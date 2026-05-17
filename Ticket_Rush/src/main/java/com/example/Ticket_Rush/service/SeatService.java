package com.example.Ticket_Rush.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.Ticket_Rush.entity.Event;
import com.example.Ticket_Rush.entity.Seat;
import com.example.Ticket_Rush.entity.SeatStatus;
import com.example.Ticket_Rush.entity.Ticket;
import com.example.Ticket_Rush.entity.User;
import com.example.Ticket_Rush.exception.BadRequestException;
import com.example.Ticket_Rush.exception.NotFoundException;
import com.example.Ticket_Rush.repository.EventRepository;
import com.example.Ticket_Rush.repository.SeatRepository;
import com.example.Ticket_Rush.repository.TicketRepository;
import com.example.Ticket_Rush.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final QRCodeService qrCodeService;

    // ==============================
    // 🎯 GENERATE SEATS (Ý 4: Đã thêm Validation)
    // ==============================
    @Transactional
    public void generateSeats(Long eventId, int rows, int cols, BigDecimal price) {
        if (rows <= 0 || cols <= 0) {
            throw new BadRequestException("Rows and columns must be positive numbers");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        // Tránh tạo đè dữ liệu làm hỏng sơ đồ ghế cũ
        if (!seatRepository.findByEventId(eventId).isEmpty()) {
            throw new BadRequestException("Seats have already been generated for this event");
        }

        List<Seat> seats = new ArrayList<>();
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                Seat seat = new Seat();
                seat.setEvent(event);
                seat.setRowIndex(r);
                seat.setColIndex(c);
                seat.setSeatCode((char) (64 + r) + String.valueOf(c));
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setPrice(price);
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);
    }

    public List<Seat> getSeatsByEvent(Long eventId) {
        return seatRepository.findByEventId(eventId);
    }

    // ==============================
    // 🔒 LOCK SEAT (Ý 1: Đã sửa lỗi nuốt Lock hết hạn)
    // ==============================
    @Transactional
    public Seat lockSeat(Long seatId, Long userId) {
        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new NotFoundException("Seat not found"));

        if (seat.getStatus() == SeatStatus.SOLD) {
            throw new BadRequestException("Seat already sold");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Nếu ghế đã lock bởi chính user này → trả về luôn
        if (seat.getStatus() == SeatStatus.LOCKED && 
            seat.getLockedBy() != null && 
            seat.getLockedBy().getId().equals(userId)) {
            return seat;
        }

        // Xử lý khi ghế đang bị LOCK
        if (seat.getStatus() == SeatStatus.LOCKED) {
            // Kiểm tra xem thời hạn Lock cũ đã hết hạn chưa (Ý 1)
            if (seat.getLockedUntil() != null && seat.getLockedUntil().isBefore(LocalDateTime.now())) {
                System.out.println("🔄 Lazy release expired lock for seat: " + seat.getSeatCode());
                // Khôi phục trạng thái tạm thời để chu trình bên dưới ghi đè lock mới lên
                seat.setStatus(SeatStatus.AVAILABLE);
            } else {
                throw new BadRequestException("Seat is being held by another user");
            }
        }

        // Tiến hành ghi đè Lock mới
        seat.setStatus(SeatStatus.LOCKED);
        seat.setLockedAt(LocalDateTime.now());
        seat.setLockedUntil(LocalDateTime.now().plusMinutes(5));
        seat.setLockedBy(user);

        Seat saved = seatRepository.save(seat);
        messagingTemplate.convertAndSend("/topic/seats", saved);
        return saved;
    }

    // ==============================
    // ⏰ RELEASE EXPIRED SEATS (Ý 2 & Ý 7: Tự động quét an toàn)
    // ==============================
    @Scheduled(fixedDelay = 30000) // Tự động chạy mỗi 30 giây (Ý 7)
    @Transactional
    public void releaseExpiredSeats() {
        List<Seat> expiredSeats = seatRepository.findExpiredSeats(LocalDateTime.now());
        if (expiredSeats.isEmpty()) return;

        for (Seat seat : expiredSeats) {
            // Sử dụng Pessimistic Lock bọc lại từng ghế để tránh Race Condition với luồng Checkout (Ý 2)
            Seat freshSeat = seatRepository.findByIdForUpdate(seat.getId()).orElse(null);
            if (freshSeat != null && 
                freshSeat.getStatus() == SeatStatus.LOCKED &&
                freshSeat.getLockedUntil() != null &&
                freshSeat.getLockedUntil().isBefore(LocalDateTime.now())) {
                
                freshSeat.setStatus(SeatStatus.AVAILABLE);
                freshSeat.setLockedAt(null);
                freshSeat.setLockedUntil(null);
                freshSeat.setLockedBy(null);
                
                Seat saved = seatRepository.save(freshSeat);
                messagingTemplate.convertAndSend("/topic/seats", saved);
                System.out.println("🔄 [Scheduler] Released expired seat: " + saved.getSeatCode());
            }
        }
    }

    public Seat getSeatById(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
    }

    // ==============================
    // 🔓 RELEASE SEAT CHỦ ĐỘNG (Ý 5: Đã thêm Lock bảo vệ)
    // ==============================
    @Transactional
    public Seat releaseSeat(Long seatId, Long userId) {
        // Thay vì findById thường, dùng findByIdForUpdate chống xung đột (Ý 5)
        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new NotFoundException("Seat not found"));
        
        if (seat.getLockedBy() != null && seat.getLockedBy().getId().equals(userId)) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setLockedAt(null);
            seat.setLockedUntil(null);
            seat.setLockedBy(null);
            seat = seatRepository.save(seat);
            messagingTemplate.convertAndSend("/topic/seats", seat);
            System.out.println("🔓 Released seat: " + seat.getSeatCode() + " by user " + userId);
        }
        return seat;
    }

    // ==============================
    // 💳 CHECKOUT (Ý 3: Có quản lý Rollback QR Code hợp lý)
    // ==============================
    @Transactional
    public Ticket checkout(Long seatId, Long userId) {
        System.out.println("=== CHECKOUT START ===");
        
        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));
        
        if (seat.getStatus() != SeatStatus.LOCKED) {
            throw new RuntimeException("Seat is not locked. Current status: " + seat.getStatus());
        }
        
        if (seat.getLockedBy() == null || !seat.getLockedBy().getId().equals(userId)) {
            throw new RuntimeException("You are not the owner of this seat.");
        }
        
        if (seat.getLockedUntil() != null && seat.getLockedUntil().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Seat lock has expired.");
        }
        
        final Long eventId = (seat.getEvent() != null) ? seat.getEvent().getId() : null;
        
        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setSeatId(seatId);
        ticket.setSeatNumber(seat.getSeatCode());
        ticket.setPrice(seat.getPrice());
        ticket.setStatus("BOOKED");
        ticket.setBookingDate(LocalDateTime.now());
        ticket.setOrderNumber(generateOrderNumber());
        
        if (seat.getEvent() != null) {
            ticket.setEventId(eventId);
            ticket.setEventName(seat.getEvent().getName());
            ticket.setEventVenue(seat.getEvent().getLocation());
            ticket.setEventDate(seat.getEvent().getEventTime() != null ? 
                seat.getEvent().getEventTime() : LocalDateTime.now().plusDays(30));
        } else {
            ticket.setEventDate(LocalDateTime.now().plusDays(30));
        }
        
        // Luồng QR Code (Ý 3): Nếu hệ thống của bạn coi QR là bắt buộc, hãy throw lỗi tại đây để Rollback.
        try {
            String qrCodeBase64 = qrCodeService.generateTicketQRCode(
                System.currentTimeMillis(),
                ticket.getEventName() != null ? ticket.getEventName() : "Event",
                ticket.getSeatNumber(),
                ticket.getOrderNumber()
            );
            ticket.setQrCodeUrl(qrCodeBase64);
            ticket.setQrCodeData("TICKET:" + ticket.getOrderNumber());
        } catch (Exception e) {
            // Rollback toàn bộ giao dịch mua vé nếu lỗi sinh QR code (An toàn dữ liệu tuyệt đối)
            throw new RuntimeException("Checkout failed due to QR Code generation failure", e);
        }
        
        seat.setStatus(SeatStatus.SOLD);
        seat.setLockedAt(null);
        seat.setLockedUntil(null);
        seat.setLockedBy(null);
        Seat savedSeat = seatRepository.save(seat);
        
        Ticket savedTicket = ticketRepository.save(ticket);
        System.out.println("✅ Ticket created: " + savedTicket.getId());
        
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend("/topic/seats", savedSeat);
                    
                    if (eventId != null) {
                        messagingTemplate.convertAndSend("/topic/event-updates", Map.of("id", eventId));
                    }
                    
                    try {
                        Map<String, Object> stats = new java.util.HashMap<>();
                        long totalTickets = ticketRepository.count();
                        long totalUsers = userRepository.count();
                        BigDecimal totalRevenue = ticketRepository.sumPriceByStatus("BOOKED");
                        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
                        
                        long totalSeats = seatRepository.count();
                        long soldSeats = seatRepository.countByStatus(SeatStatus.SOLD);
                        double occupancyRate = totalSeats > 0 ? (soldSeats * 100.0 / totalSeats) : 0;
                        
                        stats.put("totalSold", totalTickets);
                        stats.put("totalUsers", totalUsers);
                        stats.put("totalRevenue", totalRevenue);
                        stats.put("occupancyRate", Math.round(occupancyRate));
                        
                        messagingTemplate.convertAndSend("/topic/admin-stats", stats);
                        System.out.println("📡 Real-time stats broadcasted AFTER COMMIT successfully!");
                    } catch (Exception e) {
                        System.err.println("Failed to broadcast admin stats: " + e.getMessage());
                    }
                }
            });
        }

        return savedTicket;
    }

    private String generateOrderNumber() {
        return "TM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}