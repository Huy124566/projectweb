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
import org.springframework.web.bind.annotation.*;

import com.example.Ticket_Rush.entity.Event;
import com.example.Ticket_Rush.entity.Seat;
import com.example.Ticket_Rush.entity.SeatStatus;
import com.example.Ticket_Rush.entity.User;
import com.example.Ticket_Rush.entity.UserRole;
import com.example.Ticket_Rush.exception.NotFoundException;
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
    private final SimpMessagingTemplate messagingTemplate;

    // ==================== 🛠️ HÀM TÍNH TOÁN NỘI BỘ (Chống đệ quy, tối ưu hiệu năng) ====================
    private Map<String, Object> calculateStats() {
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
        
        return stats;
    }

    // ==================== THỐNG KÊ TỔNG QUAN ====================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(calculateStats());
    }

    private void broadcastDashboardUpdate() {
        try {
            Map<String, Object> stats = calculateStats(); // Đã sửa: Gọi hàm nội bộ an toàn
            messagingTemplate.convertAndSend("/topic/admin-stats", stats);
            System.out.println("📡 Đã gửi cập nhật Dashboard realtime đến admin");
        } catch (Exception e) {
            System.err.println("Lỗi gửi cập nhật Dashboard: " + e.getMessage());
        }
    }

    private void broadcastEventUpdate(Long eventId) {
        try {
            messagingTemplate.convertAndSend("/topic/event-updates", Map.of("id", eventId));
            System.out.println("📡 Đã gửi cập nhật sự kiện realtime cho event: " + eventId);
        } catch (Exception e) {
            System.err.println("Lỗi gửi cập nhật sự kiện: " + e.getMessage());
        }
    }

    // ==================== BIỂU ĐỒ DOANH THU ====================
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
            if (row[0] != null) {
                dates.add(row[0].toString());
                revenues.add(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
            }
        }
        data.put("dates", dates);
        data.put("revenues", revenues);
        return ResponseEntity.ok(data);
    }

    // ==================== THỐNG KÊ TRẠNG THÁI GHẾ ====================
    @GetMapping("/seats/stats")
    public ResponseEntity<Map<String, Long>> getSeatStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("available", seatRepository.countByStatus(SeatStatus.AVAILABLE));
        stats.put("locked", seatRepository.countByStatus(SeatStatus.LOCKED));
        stats.put("sold", seatRepository.countByStatus(SeatStatus.SOLD));
        return ResponseEntity.ok(stats);
    }

    // ==================== THỐNG KÊ NHÂN KHẨU HỌC ====================
    @GetMapping("/demographics")
    public ResponseEntity<Map<String, Object>> getDemographics() {
        Map<String, Object> demographics = new HashMap<>();
        
        Map<String, Long> genderStats = new HashMap<>();
        genderStats.put("male", userRepository.countByGender("male"));
        genderStats.put("female", userRepository.countByGender("female"));
        genderStats.put("other", userRepository.countByGender("other"));
        demographics.put("gender", genderStats);
        
        Map<String, Long> ageStats = new HashMap<>();
        ageStats.put("18-24", userRepository.countByAgeBetween(18, 24));
        ageStats.put("25-34", userRepository.countByAgeBetween(25, 34));
        ageStats.put("35-44", userRepository.countByAgeBetween(35, 44));
        ageStats.put("45+", userRepository.countByAgeGreaterThan(45));
        demographics.put("age", ageStats);
        
        return ResponseEntity.ok(demographics);
    }

    // ==================== QUẢN LÝ SỰ KIỆN ====================
    @GetMapping("/events")
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventRepository.findAll());
    }

    @PostMapping("/events")
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        event.setCreatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);
        broadcastDashboardUpdate();
        broadcastEventUpdate(saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event event) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện với ID: " + id));
        existing.setName(event.getName());
        existing.setLocation(event.getLocation());
        existing.setEventTime(event.getEventTime());
        existing.setDescription(event.getDescription());
        existing.setImageUrl(event.getImageUrl());
        existing.setCategory(event.getCategory());
        existing.setMinPrice(event.getMinPrice());
        Event saved = eventRepository.save(existing);
        broadcastDashboardUpdate();
        broadcastEventUpdate(saved.getId());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<String> deleteEvent(@PathVariable Long id) {
        if (!eventRepository.existsById(id)) {
            throw new NotFoundException("Không tìm thấy sự kiện với ID để xóa: " + id);
        }
        eventRepository.deleteById(id);
        broadcastDashboardUpdate();
        broadcastEventUpdate(id);
        return ResponseEntity.ok("Đã xóa sự kiện thành công");
    }

    // ==================== CẤU HÌNH GHẾ NGỒI ====================
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
        broadcastDashboardUpdate();
        broadcastEventUpdate(eventId);
        return ResponseEntity.ok("Đã tạo sơ đồ ghế thành công");
    }

    @PostMapping("/seats/{seatId}/update-price")
    public ResponseEntity<Seat> updateSeatPrice(
            @PathVariable Long seatId,
            @RequestParam BigDecimal price
    ) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ghế với ID: " + seatId));
        seat.setPrice(price);
        Seat saved = seatRepository.save(seat);
        broadcastDashboardUpdate();
        // Bắn update thêm cho cụm phân tích sự kiện nếu ghế này thuộc một event cụ thể
        if (seat.getEvent() != null) {
            broadcastEventUpdate(seat.getEvent().getId());
        }
        return ResponseEntity.ok(saved);
    }

    // ==================== THỐNG KÊ SỰ KIỆN ====================
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

    // ==================== QUẢN LÝ NGƯỜI DÙNG ====================
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + id));
        user.setRole(UserRole.valueOf(newRole));
        User saved = userRepository.save(user);
        broadcastDashboardUpdate();
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Không tìm thấy người dùng với ID để xóa: " + id);
        }
        userRepository.deleteById(id);
        broadcastDashboardUpdate();
        return ResponseEntity.ok("Đã xóa người dùng thành công");
    }
}