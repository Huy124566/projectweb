package com.example.Ticket_Rush.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ticket_Rush.dto.LockRequest;
import com.example.Ticket_Rush.entity.Seat;
import com.example.Ticket_Rush.entity.Ticket;
import com.example.Ticket_Rush.service.SeatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SeatController { 

    private final SeatService seatService;

    // 🔹 generate seat map
    @PostMapping("/generate")
    public ResponseEntity<String> generate(
            @RequestParam Long eventId,
            @RequestParam int rows,
            @RequestParam int cols,
            @RequestParam BigDecimal price
    ) {
        seatService.generateSeats(eventId, rows, cols, price);
        return ResponseEntity.ok("Generated!");
    }

    // 🔹 get seats by event
    @GetMapping("/{eventId}")
    public ResponseEntity<List<Seat>> getSeats(@PathVariable Long eventId) {
        List<Seat> seats = seatService.getSeatsByEvent(eventId);
        return ResponseEntity.ok(seats);
    }

    // 🔹 lock seat (FIXED) - trả về lỗi chi tiết
    @PostMapping("/lock")
    public ResponseEntity<?> lockSeat(@RequestBody LockRequest req) {
        try {
            Seat seat = seatService.lockSeat(req.getSeatId(), req.getUserId());
            return ResponseEntity.ok(seat);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 🔹 checkout - trả về lỗi chi tiết
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestParam Long seatId, @RequestParam Long userId) {
        try {
            Ticket ticket = seatService.checkout(seatId, userId);
            return ResponseEntity.ok(ticket);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 🔹 release seat
    @PostMapping("/release")
    public ResponseEntity<?> releaseSeat(@RequestBody Map<String, Long> request) {
        try {
            Long seatId = request.get("seatId");
            Long userId = request.get("userId");
            Seat seat = seatService.releaseSeat(seatId, userId);
            return ResponseEntity.ok(seat);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // 🔹 get seat by id (thêm nếu cần)
    @GetMapping("/detail/{seatId}")
    public ResponseEntity<Seat> getSeatById(@PathVariable Long seatId) {
        Seat seat = seatService.getSeatById(seatId);
        return ResponseEntity.ok(seat);
    }
}