package com.example.Ticket_Rush.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kích hoạt broker đơn giản để gửi dữ liệu real-time xuống client qua đầu /topic
        // Ví dụ: Client subcribe vào /topic/seats để nhận cập nhật ghế
        config.enableSimpleBroker("/topic");
        
        // Tiền tố cho các tin nhắn gửi từ client lên server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Vì Frontend và Backend chung domain, dùng "*" là an toàn nhất khi không có Security
                .setAllowedOriginPatterns(
                    "https://ticketmaster.id.vn",
                    "https://www.ticketmaster.id.vn",
                    "https://*.railway.app",
                    "http://localhost:*"
                )
                // Hỗ trợ SockJS để đảm bảo kết nối ổn định trên mọi trình duyệt
                .withSockJS();
    }
}