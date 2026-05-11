package com.example.Ticket_Rush.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.multipart.MultipartFile;

import com.example.Ticket_Rush.entity.Event;
import com.example.Ticket_Rush.service.EventService;
import com.example.Ticket_Rush.service.ImageUploadService;  // Thêm import

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EventController {

    private final EventService eventService;
    private final ImageUploadService imageUploadService;  // Thêm dependency

    // 🔥 SEARCH
    @GetMapping("/search")
    public ResponseEntity<Page<Event>> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventTime").descending());
        Page<Event> events = eventService.search(keyword, pageable);
        return ResponseEntity.ok(events);
    }
    
    // 🔥 SUGGEST
    @GetMapping("/suggest")
    public ResponseEntity<List<Event>> suggest(
            @RequestParam(required = false, defaultValue = "") String keyword
    ) {
        List<Event> suggestions = eventService.suggest(keyword);
        return ResponseEntity.ok(suggestions);
    }
    
    // 🔥 FILTER
    @GetMapping("/filter")
    public ResponseEntity<Page<Event>> filter(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventTime").ascending());
        Page<Event> events = eventService.filter(keyword, location, fromDate, toDate, pageable);
        return ResponseEntity.ok(events);
    }
    
    // 📋 Lấy tất cả
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }
    
    // 🔍 Lấy theo ID
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }
    
    // ✏️ Tạo mới (CÓ UPLOAD ẢNH) - XÓA method JSON cũ
    @PostMapping
    public ResponseEntity<Event> createEvent(
            @RequestParam String name,
            @RequestParam String location,
            @RequestParam String eventTime,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam Double minPrice
    ) {
        Event event = new Event();
        event.setName(name);
        event.setLocation(location);
        event.setEventTime(LocalDateTime.parse(eventTime));
        event.setCategory(category);
        event.setDescription(description != null ? description : "");
        event.setMinPrice(BigDecimal.valueOf(minPrice));
        
        // Upload ảnh nếu có
        if (image != null && !image.isEmpty()) {
            String imageUrl = imageUploadService.uploadEventImage(image);
            event.setImageUrl(imageUrl);
        }
        
        return ResponseEntity.ok(eventService.createEvent(event));
    }
    
    // 📝 Cập nhật (giữ nguyên JSON - không upload ảnh ở đây)
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event event) {
        return ResponseEntity.ok(eventService.updateEvent(id, event));
    }
    
    // 🗑️ Xóa
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok("Event deleted successfully");
    }
}