package com.example.Ticket_Rush.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Ticket_Rush.entity.Seat;
import com.example.Ticket_Rush.entity.SeatStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdForUpdate(@Param("id") Long id);

    List<Seat> findByStatusAndLockedUntilBefore(SeatStatus status, LocalDateTime time);
    
    @Query("SELECT s FROM Seat s WHERE s.status = 'LOCKED' AND s.lockedUntil < :now")
    List<Seat> findExpiredSeats(@Param("now") LocalDateTime now);
    
    // ==================== ADMIN STATS METHODS ====================
    
    /**
     * Đếm số ghế theo trạng thái
     */
    long countByStatus(SeatStatus status);
    
    /**
     * Đếm số ghế theo event
     */
    long countByEventId(Long eventId);
    
    /**
     * Đếm số ghế theo event và trạng thái
     */
    long countByEventIdAndStatus(Long eventId, SeatStatus status);
    
    /**
     * Lấy tất cả ghế của event (cho seat map)
     */
    List<Seat> findByEventIdOrderByRowIndexAscColIndexAsc(Long eventId);
    
    /**
     * Cập nhật giá ghế theo loại (VIP, Regular, etc.)
     */
    @Query("UPDATE Seat s SET s.price = :price WHERE s.event.id = :eventId AND s.rowIndex <= :vipRows")
    int updateVipSeatsPrice(@Param("eventId") Long eventId, @Param("vipRows") int vipRows, @Param("price") java.math.BigDecimal price);
    
    /**
     * Thống kê số ghế theo từng trạng thái cho 1 event
     */
    @Query("SELECT s.status, COUNT(s) FROM Seat s WHERE s.event.id = :eventId GROUP BY s.status")
    List<Object[]> countSeatsByStatusForEvent(@Param("eventId") Long eventId);
}