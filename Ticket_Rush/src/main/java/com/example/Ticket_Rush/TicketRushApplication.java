package com.example.Ticket_Rush;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
@EnableScheduling
public class TicketRushApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketRushApplication.class, args);
	}

}
