package com.example.Ticket_Rush.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Ticket_Rush.entity.Ticket;
import com.example.Ticket_Rush.service.ResendEmailService;
import com.example.Ticket_Rush.service.TicketService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TicketController {

    private final TicketService ticketService;
    private final ResendEmailService emailService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Ticket>> getTicketsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ticketService.getTicketsByUser(userId));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getById(ticketId));
    }

    @GetMapping("/user/{userId}/upcoming")
    public ResponseEntity<List<Ticket>> getUpcomingTickets(@PathVariable Long userId) {
        return ResponseEntity.ok(ticketService.getUpcomingTickets(userId));
    }

    @GetMapping("/user/{userId}/past")
    public ResponseEntity<List<Ticket>> getPastTickets(@PathVariable Long userId) {
        return ResponseEntity.ok(ticketService.getPastTickets(userId));
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<String> cancelTicket(@PathVariable Long ticketId) {
        ticketService.cancelTicket(ticketId);
        return ResponseEntity.ok("Ticket cancelled successfully");
    }
    
    @GetMapping("/{ticketId}/qrcode")
    public ResponseEntity<String> getTicketQRCode(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.getById(ticketId);
        if (ticket.getQrCodeUrl() == null || ticket.getQrCodeUrl().isEmpty()) {
            String qrCode = ticketService.refreshQRCode(ticketId);
            return ResponseEntity.ok(qrCode);
        }
        return ResponseEntity.ok(ticket.getQrCodeUrl());
    }
    
    @PostMapping("/{ticketId}/refresh-qrcode")
    public ResponseEntity<String> refreshQRCode(@PathVariable Long ticketId) {
        String qrCode = ticketService.refreshQRCode(ticketId);
        return ResponseEntity.ok(qrCode);
    }
    
    // API GỬI VÉ QUA EMAIL (dùng Resend API)
    @PostMapping("/{ticketId}/send-email")
    public ResponseEntity<?> sendTicketEmail(@PathVariable Long ticketId, @RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            
            Ticket ticket = ticketService.getById(ticketId);
            
            // Nếu QR Code bị null, tạo mới
            if (ticket.getQrCodeUrl() == null || ticket.getQrCodeUrl().isEmpty()) {
                String newQRCode = ticketService.refreshQRCode(ticketId);
                ticket.setQrCodeUrl(newQRCode);
            }
            
            // Gửi email xác nhận vé
            emailService.sendTicketConfirmation(email, ticket);
            
            return ResponseEntity.ok(Map.of(
                "message", "✅ Ticket sent to " + email,
                "success", true
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}