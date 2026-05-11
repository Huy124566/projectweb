package com.example.Ticket_Rush.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    private String location;
    
    private LocalDateTime eventTime;
    
    // ===== THÊM CÁC FIELD MỚI =====
    
    @Column(length = 1000)
    private String description;      // Mô tả sự kiện
    
    private String imageUrl;         // URL ảnh đại diện
    
    private String category;         // CONCERT, SPORTS, THEATRE, FAMILY, COMEDY
    
    private BigDecimal minPrice;     // Giá thấp nhất
    
    // ===== GETTER/SETTER cho Frontend (để dùng cả eventTime và dateTime) =====
    public LocalDateTime getDateTime() {
        return eventTime;
    }
    
    public void setDateTime(LocalDateTime dateTime) {
        this.eventTime = dateTime;
    }
    
    @PrePersist
    protected void onCreate() {
        if (minPrice == null) {
            minPrice = BigDecimal.ZERO;
        }
        if (description == null) {
            description = "";
        }
        if (imageUrl == null) {
            imageUrl = "https://source.unsplash.com/featured/300x200?concert";
        }
        if (category == null) {
            category = "CONCERT";
        }
    }
}