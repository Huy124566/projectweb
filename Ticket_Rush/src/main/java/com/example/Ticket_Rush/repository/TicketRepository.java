package com.example.Ticket_Rush.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Ticket_Rush.entity.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    List<Ticket> findByUserId(Long userId);
    List<Ticket> findByUserIdOrderByEventDateDesc(Long userId);
    List<Ticket> findByUserIdAndEventDateAfter(Long userId, LocalDateTime date);
    List<Ticket> findByUserIdAndEventDateBefore(Long userId, LocalDateTime date);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.userId = :userId AND t.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);
    
    // Admin stats
    @Query("SELECT SUM(t.price) FROM Ticket t WHERE t.status = :status")
    BigDecimal sumPriceByStatus(@Param("status") String status);
    
    @Query("SELECT SUM(t.price) FROM Ticket t WHERE t.eventId = :eventId")
    BigDecimal sumPriceByEventId(@Param("eventId") Long eventId);
    
    @Query("SELECT FUNCTION('DATE', t.bookingDate) as date, SUM(t.price) FROM Ticket t WHERE t.bookingDate BETWEEN :from AND :to GROUP BY FUNCTION('DATE', t.bookingDate)")
    List<Object[]> findRevenueByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}