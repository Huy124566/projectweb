package com.example.Ticket_Rush.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username không được để trống")
    private String username;

    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Password không được để trống")
    private String password;

    private String gender;
    private LocalDate dateOfBirth;
    private String phone;
}