package com.example.Ticket_Rush.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ticket_Rush.entity.Event;
import com.example.Ticket_Rush.entity.Seat;
import com.example.Ticket_Rush.entity.SeatStatus;
import com.example.Ticket_Rush.entity.User;
import com.example.Ticket_Rush.entity.UserRole;
import com.example.Ticket_Rush.repository.EventRepository;
import com.example.Ticket_Rush.repository.SeatRepository;
import com.example.Ticket_Rush.repository.TicketRepository;
import com.example.Ticket_Rush.repository.UserRepository;
import com.example.Ticket_Rush.service.SeatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminController {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SeatService seatService;
    private final SimpMessagingTemplate messagingTemplate; // 🔥 THÊM DÒNG NÀY

    // ==================== DASHBOARD STATS ====================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
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
        
        return ResponseEntity.ok(stats);
    }

    // 🔥 GỬI REALTIME UPDATE SAU KHI THAY ĐỔI
    private void broadcastDashboardUpdate() {
        try {
            // Lấy dữ liệu mới nhất
            Map<String, Object> stats = getDashboardStats().getBody();
            messagingTemplate.convertAndSend("/topic/admin-stats", stats);
            System.out.println("📡 Real-time update sent to all admins");
        } catch (Exception e) {
            System.err.println("Failed to broadcast update: " + e.getMessage());
        }
    }

    // ==================== REVENUE OVER TIME ====================
    @GetMapping("/revenue/timeline")
    public ResponseEntity<Map<String, Object>> getRevenueTimeline(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        List<Object[]> results = ticketRepository.findRevenueByDateRange(from, to);
        Map<String, Object> data = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        
        for (Object[] row : results) {
            dates.add(row[0].toString());
            revenues.add((BigDecimal) row[1]);
        }
        data.put("dates", dates);
        data.put("revenues", revenues);
        return ResponseEntity.ok(data);
    }

    // ==================== SEAT STATUS STATS ====================
    @GetMapping("/seats/stats")
    public ResponseEntity<Map<String, Long>> getSeatStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("available", seatRepository.countByStatus(SeatStatus.AVAILABLE));
        stats.put("locked", seatRepository.countByStatus(SeatStatus.LOCKED));
        stats.put("sold", seatRepository.countByStatus(SeatStatus.SOLD));
        return ResponseEntity.ok(stats);
    }

    // ==================== DEMOGRAPHIC STATS ====================
    @GetMapping("/demographics")
    public ResponseEntity<Map<String, Object>> getDemographics() {
        Map<String, Object> demographics = new HashMap<>();
        
        long maleCount = userRepository.countByGender("male");
        long femaleCount = userRepository.countByGender("female");
        long otherCount = userRepository.countByGender("other");
        
        Map<String, Long> genderStats = new HashMap<>();
        genderStats.put("male", maleCount);
        genderStats.put("female", femaleCount);
        genderStats.put("other", otherCount);
        demographics.put("gender", genderStats);
        
        Map<String, Long> ageStats = new HashMap<>();
        ageStats.put("18-24", userRepository.countByAgeBetween(18, 24));
        ageStats.put("25-34", userRepository.countByAgeBetween(25, 34));
        ageStats.put("35-44", userRepository.countByAgeBetween(35, 44));
        ageStats.put("45+", userRepository.countByAgeGreaterThan(45));
        demographics.put("age", ageStats);
        
        return ResponseEntity.ok(demographics);
    }

    // ==================== EVENT MANAGEMENT ====================
    @GetMapping("/events")
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventRepository.findAll());
    }

    @PostMapping("/events")
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        event.setCreatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);
        broadcastDashboardUpdate(); // 🔥 GỬI REALTIME
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event event) {
        Event existing = eventRepository.findById(id).orElseThrow();
        existing.setName(event.getName());
        existing.setLocation(event.getLocation());
        existing.setEventTime(event.getEventTime());
        existing.setDescription(event.getDescription());
        existing.setImageUrl(event.getImageUrl());
        existing.setCategory(event.getCategory());
        existing.setMinPrice(event.getMinPrice());
        Event saved = eventRepository.save(existing);
        broadcastDashboardUpdate(); // 🔥 GỬI REALTIME
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<String> deleteEvent(@PathVariable Long id) {
        eventRepository.deleteById(id);
        broadcastDashboardUpdate(); // 🔥 GỬI REALTIME
        return ResponseEntity.ok("Event deleted");
    }

    // ==================== SEAT CONFIGURATION ====================
    @GetMapping("/events/{eventId}/seats")
    public ResponseEntity<List<Seat>> getEventSeats(@PathVariable Long eventId) {
        return ResponseEntity.ok(seatRepository.findByEventId(eventId));
    }

    @PostMapping("/events/{eventId}/generate-seats")
    public ResponseEntity<String> generateSeats(
            @PathVariable Long eventId,
            @RequestParam int rows,
            @RequestParam int cols,
            @RequestParam BigDecimal defaultPrice
    ) {
        seatService.generateSeats(eventId, rows, cols, defaultPrice);
        broadcastDashboardUpdate(); // 🔥 GỬI REALTIME
        return ResponseEntity.ok("Seats generated successfully");
    }

    @PostMapping("/seats/{seatId}/update-price")
    public ResponseEntity<Seat> updateSeatPrice(
            @PathVariable Long seatId,
            @RequestParam BigDecimal price
    ) {
        Seat seat = seatRepository.findById(seatId).orElseThrow();
        seat.setPrice(price);
        Seat saved = seatRepository.save(seat);
        broadcastDashboardUpdate(); // 🔥 GỬI REALTIME
        return ResponseEntity.ok(saved);
    }

    // ==================== TICKET STATS PER EVENT ====================
    @GetMapping("/events/{eventId}/stats")
    public ResponseEntity<Map<String, Object>> getEventStats(@PathVariable Long eventId) {
        Map<String, Object> stats = new HashMap<>();
        long totalSeats = seatRepository.countByEventId(eventId);
        long soldSeats = seatRepository.countByEventIdAndStatus(eventId, SeatStatus.SOLD);
        BigDecimal revenue = ticketRepository.sumPriceByEventId(eventId);
        
        stats.put("totalSeats", totalSeats);
        stats.put("soldSeats", soldSeats);
        stats.put("availableSeats", totalSeats - soldSeats);
        stats.put("occupancyRate", totalSeats > 0 ? (soldSeats * 100.0 / totalSeats) : 0);
        stats.put("revenue", revenue != null ? revenue : BigDecimal.ZERO);
        
        return ResponseEntity.ok(stats);
    }

    // ==================== USER MANAGEMENT ====================
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        User user = userRepository.findById(id).orElseThrow();
        user.setRole(UserRole.valueOf(newRole));
        User saved = userRepository.save(user);
        broadcastDashboardUpdate(); // 🔥 GỬI REALTIME
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        broadcastDashboardUpdate(); // 🔥 GỬI REALTIME
        return ResponseEntity.ok("User deleted");
    }
}