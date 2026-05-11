package com.example.Ticket_Rush.dto;

import lombok.Data;

@Data
public class LoginOtpRequest {
    private String email;
    private String otp;
}