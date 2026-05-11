package com.example.Ticket_Rush.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String message;
    private String token;
    private Long id;
    private String email;
    private String username;
    private String role;
    private String avatar;
    private String phone;
    private String gender;
    private String dateOfBirth;
    private String createdAt;
}