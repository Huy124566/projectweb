package com.example.Ticket_Rush.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String gender;

    private LocalDate dateOfBirth;
    
    private String phone;
    
    @Column(columnDefinition = "TEXT")
    private String avatar;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (role == null) {
            role = UserRole.ROLE_USER;
        }
        if (avatar == null || avatar.isEmpty()) {
            // Tạo avatar hình người giống Facebook từ DiceBear
            this.avatar = generateDefaultAvatar(username);
        }
        if (phone == null) {
            phone = "";
        }
    }
    
    // Tạo avatar mặc định dạng hình người (giống phong cách Facebook)
    private String generateDefaultAvatar(String name) {
        String seed = (name != null && !name.isEmpty()) ? name : "user";
        // DiceBear API - avataaars style tạo hình người
        return "https://api.dicebear.com/7.x/avataaars/svg?seed=" + seed + "&backgroundColor=b6e3f4&hairColor= brown&skinColor=light";
    }
    
    // Cập nhật avatar
    public void updateAvatar(String newAvatar) {
        this.avatar = newAvatar;
    }
}