package com.example.Ticket_Rush;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@SpringBootApplication(exclude = { org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
@EnableScheduling
public class TicketRushApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketRushApplication.class, args);
	}
	
	@PostConstruct
	public void init() {
	    // Ép toàn bộ ứng dụng chạy múi giờ Việt Nam
	    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
	}

}
