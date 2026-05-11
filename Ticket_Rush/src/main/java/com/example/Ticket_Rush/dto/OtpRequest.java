package com.example.Ticket_Rush.dto;

import lombok.Data;

@Data
public class OtpRequest {
    private String email;
    private String otp;
    private String password;
    private String username;
    private String gender;
    private String dateOfBirth;
}