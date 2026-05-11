package com.example.Ticket_Rush.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    // 🎯 GENERATE SEATS
    // ==============================
    @Transactional
    public void generateSeats(Long eventId, int rows, int cols, BigDecimal price) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

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

    // ==============================
    // 📋 GET SEATS
    // ==============================
    public List<Seat> getSeatsByEvent(Long eventId) {
        return seatRepository.findByEventId(eventId);
    }

    // ==============================
    // 🔒 LOCK SEAT
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

        // Nếu ghế đã lock bởi chính user này → trả về ghế hiện tại
        if (seat.getStatus() == SeatStatus.LOCKED && 
            seat.getLockedBy() != null && 
            seat.getLockedBy().getId().equals(userId)) {
            return seat;
        }

        // Nếu ghế đang lock bởi người khác
        if (seat.getStatus() == SeatStatus.LOCKED) {
            throw new BadRequestException("Seat is being held by another user");
        }

        // Lock mới
        seat.setStatus(SeatStatus.LOCKED);
        seat.setLockedAt(LocalDateTime.now());
        seat.setLockedUntil(LocalDateTime.now().plusMinutes(5));
        seat.setLockedBy(user);

        Seat saved = seatRepository.save(seat);
        messagingTemplate.convertAndSend("/topic/seats", saved);
        return saved;
    }

    // ==============================
    // ⏰ RELEASE EXPIRED SEATS
    // ==============================
    @Transactional
    public void releaseExpiredSeats() {
        List<Seat> expiredSeats = seatRepository.findExpiredSeats(LocalDateTime.now());
        if (expiredSeats.isEmpty()) return;

        for (Seat seat : expiredSeats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setLockedAt(null);
            seat.setLockedUntil(null);
            seat.setLockedBy(null);
        }

        List<Seat> savedSeats = seatRepository.saveAll(expiredSeats);
        for (Seat seat : savedSeats) {
            messagingTemplate.convertAndSend("/topic/seats", seat);
            System.out.println("🔄 Released expired seat: " + seat.getSeatCode());
        }
    }

    // ==============================
    // 🔓 RELEASE SEAT (chủ động)
    // ==============================
    public Seat getSeatById(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
    }

    @Transactional
    public Seat releaseSeat(Long seatId, Long userId) {
        Seat seat = getSeatById(seatId);
        
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
    // 💳 CHECKOUT
    // ==============================
    @Transactional
    public Ticket checkout(Long seatId, Long userId) {
        System.out.println("=== CHECKOUT START ===");
        System.out.println("SeatId: " + seatId + ", UserId: " + userId);
        
        // 1. Lấy ghế với pessimistic lock
        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));
        
        System.out.println("Seat status: " + seat.getStatus());
        System.out.println("Locked by: " + (seat.getLockedBy() != null ? seat.getLockedBy().getId() : "null"));
        System.out.println("Locked until: " + seat.getLockedUntil());
        
        // 2. KIỂM TRA QUAN TRỌNG 1: Ghế phải ở trạng thái LOCKED
        if (seat.getStatus() != SeatStatus.LOCKED) {
            throw new RuntimeException("Seat is not locked. Current status: " + seat.getStatus());
        }
        
        // 3. KIỂM TRA QUAN TRỌNG 2: Phải là chủ nhân của ghế
        if (seat.getLockedBy() == null || !seat.getLockedBy().getId().equals(userId)) {
            throw new RuntimeException("You are not the owner of this seat. Locked by user: " + 
                (seat.getLockedBy() != null ? seat.getLockedBy().getId() : "null"));
        }
        
        // 4. KIỂM TRA QUAN TRỌNG 3: Ghế chưa hết hạn lock
        if (seat.getLockedUntil() != null && seat.getLockedUntil().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Seat lock has expired. Locked until: " + seat.getLockedUntil());
        }
        
        // 5. Tạo vé
        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setSeatId(seatId);
        ticket.setSeatNumber(seat.getSeatCode());
        ticket.setPrice(seat.getPrice());
        ticket.setStatus("BOOKED");
        ticket.setBookingDate(LocalDateTime.now());
        ticket.setOrderNumber(generateOrderNumber());
        
        // Lấy event info
        if (seat.getEvent() != null) {
            ticket.setEventId(seat.getEvent().getId());
            ticket.setEventName(seat.getEvent().getName());
            ticket.setEventVenue(seat.getEvent().getLocation());
            ticket.setEventDate(seat.getEvent().getEventTime() != null ? 
                seat.getEvent().getEventTime() : LocalDateTime.now().plusDays(30));
        } else {
            ticket.setEventDate(LocalDateTime.now().plusDays(30));
        }
        
        // Tạo QR Code
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
            System.out.println("QR Code generation failed: " + e.getMessage());
        }
        
        // 6. Cập nhật ghế thành SOLD
        seat.setStatus(SeatStatus.SOLD);
        seat.setLockedAt(null);
        seat.setLockedUntil(null);
        seat.setLockedBy(null);
        seatRepository.save(seat);
        
        // 7. Lưu vé
        Ticket saved = ticketRepository.save(ticket);
        System.out.println("✅ Ticket created: " + saved.getId());
        
        // 8. Broadcast cập nhật ghế
        messagingTemplate.convertAndSend("/topic/seats", seat);
        
        return saved;
    }
    private String generateOrderNumber() {
        return "TM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
}