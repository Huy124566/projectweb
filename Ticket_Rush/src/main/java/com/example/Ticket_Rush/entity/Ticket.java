package com.example.Ticket_Rush.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    
    private Long eventId;
    
    private Long seatId;
    
    private String seatNumber;
    
    private BigDecimal price;
    
    private String status;  // BOOKED, CANCELLED, ATTENDED
    
    private LocalDateTime eventDate;
    
    private LocalDateTime bookingDate;
    
    private String orderNumber;
    
    private String eventName;
    
    private String eventVenue;
    
    @Column(columnDefinition = "LONGTEXT")  // ← SỬA TỪ TEXT THÀNH LONGTEXT
    private String qrCodeData;
    
    @Column(columnDefinition = "LONGTEXT")  // ← THÊM DÒNG NÀY
    private String qrCodeUrl;
    
    @PrePersist
    protected void onCreate() {
        if (bookingDate == null) {
            bookingDate = LocalDateTime.now();
        }
        if (status == null) {
            status = "BOOKED";
        }
    }
    
    public void generateQRCodeData() {
        this.qrCodeData = String.format(
            "TICKET:%d|EVENT:%d|SEAT:%s|DATE:%s|USER:%d|ORDER:%s",
            id, eventId, seatNumber, eventDate, userId, orderNumber
        );
    }
}