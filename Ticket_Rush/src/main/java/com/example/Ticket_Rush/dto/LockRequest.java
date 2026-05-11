package com.example.Ticket_Rush.dto;

import lombok.Data;

@Data
public class LockRequest {
    private Long seatId;
    private Long userId;
}
