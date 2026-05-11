package com.example.Ticket_Rush.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.Ticket_Rush.service.SeatService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SeatScheduler {

    private final SeatService seatService;

    @Scheduled(fixedRate = 30000) // mỗi 30 giây
    public void releaseSeats() {
    	System.out.println("Scheduler running...");
        seatService.releaseExpiredSeats();
    }
}