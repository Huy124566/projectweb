package com.example.Ticket_Rush.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Ticket_Rush.entity.Ticket;
import com.example.Ticket_Rush.service.TicketService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TicketController {

    private final TicketService ticketService;

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
    
    // 🆕 Endpoint lấy QR Code của vé
    @GetMapping("/{ticketId}/qrcode")
    public ResponseEntity<String> getTicketQRCode(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.getById(ticketId);
        if (ticket.getQrCodeUrl() == null) {
            String qrCode = ticketService.refreshQRCode(ticketId);
            return ResponseEntity.ok(qrCode);
        }
        return ResponseEntity.ok(ticket.getQrCodeUrl());
    }
    
    // 🆕 Refresh QR Code
    @PostMapping("/{ticketId}/refresh-qrcode")
    public ResponseEntity<String> refreshQRCode(@PathVariable Long ticketId) {
        String qrCode = ticketService.refreshQRCode(ticketId);
        return ResponseEntity.ok(qrCode);
    }
}