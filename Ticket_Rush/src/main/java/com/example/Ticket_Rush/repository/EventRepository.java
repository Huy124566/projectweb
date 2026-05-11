package com.example.Ticket_Rush.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.Ticket_Rush.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    // 🔥 SEARCH (fix keyword rỗng)
    @Query("""
        SELECT e FROM Event e
        WHERE (:keyword = '' 
               OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(e.location) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Event> search(@Param("keyword") String keyword, Pageable pageable);


    // 🔥 SUGGEST (dùng Page cho chuẩn)
    @Query("""
        SELECT e FROM Event e
        WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Event> suggestEvents(@Param("keyword") String keyword, Pageable pageable);


    // 🔥 FILTER chuẩn (fix empty string)
    @Query("""
        SELECT e FROM Event e
        WHERE (:keyword = '' 
               OR LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:location = '' 
               OR LOWER(e.location) LIKE LOWER(CONCAT('%', :location, '%')))
          AND (:fromDate IS NULL OR e.eventTime >= :fromDate)
          AND (:toDate IS NULL OR e.eventTime <= :toDate)
    """)
    Page<Event> filter(
        @Param("keyword") String keyword,
        @Param("location") String location,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable
    );
}