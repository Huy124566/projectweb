package com.example.Ticket_Rush.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;

import com.example.Ticket_Rush.entity.UserRole;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private String avatar;
    private String token;
    private String message;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private LocalDateTime createdAt;
    
    // Constructor cũ (giữ để code hiện tại không bị lỗi)
    public AuthResponse(Long id, String username, String email, UserRole role, String avatar, String token, String message) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.avatar = avatar;
        this.token = token;
        this.message = message;
        this.phone = "";
        this.gender = "";
        this.dateOfBirth = null;
        this.createdAt = null;
    }
}