package com.example.Ticket_Rush.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ticket_Rush.entity.Event;
import com.example.Ticket_Rush.entity.Seat;
import com.example.Ticket_Rush.entity.SeatStatus;
import com.example.Ticket_Rush.entity.Ticket;
import com.example.Ticket_Rush.repository.SeatRepository;
import com.example.Ticket_Rush.repository.TicketRepository;
import com.example.Ticket_Rush.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SeatRepository seatRepository;
    private final QRCodeService qrCodeService;

    public List<Ticket> getTicketsByUser(Long userId) {
        return ticketRepository.findByUserIdOrderByEventDateDesc(userId);
    }

    public Ticket getById(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));
    }

    public List<Ticket> getUpcomingTickets(Long userId) {
        return ticketRepository.findByUserIdAndEventDateAfter(userId, LocalDateTime.now());
    }

    public List<Ticket> getPastTickets(Long userId) {
        return ticketRepository.findByUserIdAndEventDateBefore(userId, LocalDateTime.now());
    }

    @Transactional
    public Ticket createTicket(Long userId, Long seatId) {
        // Kiểm tra user tồn tại
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Lấy thông tin ghế
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
        
        // Lấy event từ seat (quan hệ ManyToOne)
        Event event = seat.getEvent();
        if (event == null) {
            throw new RuntimeException("Event not found for this seat");
        }

        // Kiểm tra trạng thái ghế phải là LOCKED
        if (seat.getStatus() != SeatStatus.LOCKED) {
            throw new RuntimeException("Seat is not available. Current status: " + seat.getStatus());
        }

        // Tạo vé mới
        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setEventId(event.getId());
        ticket.setSeatId(seatId);
        
        // Tạo seatNumber từ seatCode hoặc rowIndex + colIndex
        String seatNumber = seat.getSeatCode() != null ? seat.getSeatCode() : 
                           (seat.getRowIndex() + "-" + seat.getColIndex());
        ticket.setSeatNumber(seatNumber);
        
        ticket.setPrice(seat.getPrice());
        ticket.setStatus("BOOKED");
        ticket.setEventDate(event.getDateTime()); // event.getDateTime() từ Event entity
        ticket.setBookingDate(LocalDateTime.now());
        ticket.setOrderNumber(generateOrderNumber());
        ticket.setEventName(event.getName());
        ticket.setEventVenue(event.getLocation());
        
        // Tạo dữ liệu QR Code
        ticket.generateQRCodeData();
        
        // Tạo ảnh QR Code Base64 (tạm thời dùng ID tương lai)
        Long tempId = System.currentTimeMillis();
        String qrCodeBase64 = qrCodeService.generateTicketQRCode(
            tempId,
            event.getName(),
            seatNumber,
            ticket.getOrderNumber()
        );
        ticket.setQrCodeUrl(qrCodeBase64);

        // Cập nhật trạng thái ghế thành BOOKED hoặc SOLD
        seat.setStatus(SeatStatus.BOOKED);  // Nếu có BOOKED, hoặc dùng SOLD
        seatRepository.save(seat);

        // Lưu vé
        Ticket savedTicket = ticketRepository.save(ticket);
        
        // Cập nhật lại QR Code với ID thật sau khi save
        String realQRCode = qrCodeService.generateTicketQRCode(
            savedTicket.getId(),
            event.getName(),
            seatNumber,
            savedTicket.getOrderNumber()
        );
        savedTicket.setQrCodeUrl(realQRCode);
        savedTicket.generateQRCodeData(); // Regenerate data với ID thật
        
        return ticketRepository.save(savedTicket);
    }

    @Transactional
    public void cancelTicket(Long ticketId) {
        Ticket ticket = getById(ticketId);
        
        if (ticket.getEventDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot cancel past event ticket");
        }
        
        ticket.setStatus("CANCELLED");
        ticketRepository.save(ticket);
        
        // Giải phóng ghế
        Seat seat = seatRepository.findById(ticket.getSeatId())
                .orElseThrow(() -> new RuntimeException("Seat not found"));
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setLockedAt(null);
        seat.setLockedUntil(null);
        seat.setLockedBy(null);
        seatRepository.save(seat);
    }
    
    // Refresh QR Code cho vé
    public String refreshQRCode(Long ticketId) {
        Ticket ticket = getById(ticketId);
        String newQRCode = qrCodeService.generateTicketQRCode(
            ticketId,
            ticket.getEventName(),
            ticket.getSeatNumber(),
            ticket.getOrderNumber()
        );
        ticket.setQrCodeUrl(newQRCode);
        ticket.generateQRCodeData();
        ticketRepository.save(ticket);
        return newQRCode;
    }

    private String generateOrderNumber() {
        return "TM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}