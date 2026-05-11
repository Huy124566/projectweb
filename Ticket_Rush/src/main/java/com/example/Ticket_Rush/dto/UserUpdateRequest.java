package com.example.Ticket_Rush.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    public String username;
    public String email;
    public String phone;
    public String password;
    public String avatar;
    public String gender;        // ← THÊM
    public String dateOfBirth;   // ← THÊM
}