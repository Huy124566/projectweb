package com.example.Ticket_Rush.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.Ticket_Rush.entity.Event;
import com.example.Ticket_Rush.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    // ==============================
    // 🔥 SEARCH (có phân trang)
    // ==============================
    public Page<Event> search(String keyword, Pageable pageable) {
        if (keyword == null) keyword = "";
        return eventRepository.search(keyword, pageable);
    }
    
    // ==============================
    // 🔥 SUGGEST (lấy 5 gợi ý đầu tiên)
    // ==============================
    public List<Event> suggest(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, 5);
        Page<Event> page = eventRepository.suggestEvents(keyword, pageable);
        return page.getContent();
    }
    
    // ==============================
    // 🔥 FILTER nâng cao
    // ==============================
    public Page<Event> filter(String keyword, String location, 
                              LocalDateTime fromDate, LocalDateTime toDate, 
                              Pageable pageable) {
        if (keyword == null) keyword = "";
        if (location == null) location = "";
        return eventRepository.filter(keyword, location, fromDate, toDate, pageable);
    }
    
    // ==============================
    // 📋 Lấy tất cả sự kiện
    // ==============================
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }
    
    // ==============================
    // 🔍 Lấy sự kiện theo ID
    // ==============================
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }
    
    // ==============================
    // ✏️ Tạo mới sự kiện (cho admin)
    // ==============================
    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }
    
    // ==============================
    // 📝 Cập nhật sự kiện
    // ==============================
    public Event updateEvent(Long id, Event eventDetails) {
        Event event = getEventById(id);
        event.setName(eventDetails.getName());
        event.setLocation(eventDetails.getLocation());
        event.setDateTime(eventDetails.getDateTime());
        event.setDescription(eventDetails.getDescription());
        event.setImageUrl(eventDetails.getImageUrl());
        event.setCategory(eventDetails.getCategory());
        return eventRepository.save(event);
    }
    
    // ==============================
    // 🗑️ Xóa sự kiện
    // ==============================
    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }
}