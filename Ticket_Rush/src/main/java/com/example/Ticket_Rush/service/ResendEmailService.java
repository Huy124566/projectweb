package com.example.Ticket_Rush.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.Ticket_Rush.entity.Ticket;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
@Slf4j
public class ResendEmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Value("${resend.api.key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendOtpEmail(String to, String otp, String purpose) {
        try {
            String subject;
            String purposeText;
            
            switch (purpose) {
                case "REGISTER":
                    subject = "Verify Your Email - TicketRush";
                    purposeText = "Email Verification";
                    break;
                case "FORGOT_PASSWORD":
                    subject = "Reset Your Password - TicketRush";
                    purposeText = "Password Reset";
                    break;
                default:
                    subject = "Login Verification - TicketRush";
                    purposeText = "Login";
                    break;
            }

            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f7fa;">
                    <div style="max-width: 500px; margin: 0 auto; background: white; border-radius: 20px; overflow: hidden;">
                        <div style="background: linear-gradient(135deg, #0d5bd7, #0a4ab3); color: white; padding: 25px; text-align: center;">
                            <h2 style="margin: 0;">🎫 TicketRush</h2>
                        </div>
                        <div style="padding: 20px;">
                            <p>Hello,</p>
                            <p>You requested a verification code for <strong>%s</strong>.</p>
                            <div style="font-size: 42px; font-weight: bold; color: #0d5bd7; text-align: center; padding: 20px; background: #f8f9fc; border-radius: 12px; margin: 20px 0;">
                                %s
                            </div>
                            <p style="text-align: center;">This code expires in <strong>5 minutes</strong>.</p>
                            <p style="text-align: center; font-size: 14px; color: #888;">If you didn't request this, please ignore this email.</p>
                        </div>
                        <div style="text-align: center; padding: 15px; color: #888; font-size: 12px; border-top: 1px solid #eef2f6;">
                            <p>&copy; 2025 TicketRush</p>
                        </div>
                    </div>
                </body>
                </html>
                """, purposeText, otp);

            Map<String, Object> emailData = Map.of(
                "from", "TicketRush <onboarding@resend.dev>",
                "to", to,
                "subject", subject,
                "html", htmlContent
            );

            String jsonBody = objectMapper.writeValueAsString(emailData);
            RequestBody body = RequestBody.create(jsonBody, JSON);
            
            Request request = new Request.Builder()
                .url(RESEND_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful()) {
                    log.info("✅ Email sent successfully to: {}", to);
                    log.debug("Response: {}", responseBody);
                } else {
                    log.error("❌ Failed to send email. Status: {}, Response: {}", 
                        response.code(), responseBody);
                    throw new RuntimeException("Failed to send email: " + responseBody);
                }
            }

        } catch (IOException e) {
            log.error("❌ IO Error sending email: {}", e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error: {}", e.getMessage());
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }


    public void sendTicketConfirmation(String to, Ticket ticket) {
    try {
        String subject = "🎫 Your Ticket Confirmation - TicketRush";
        
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f7fa;">
                <div style="max-width: 550px; margin: 0 auto; background: white; border-radius: 20px; overflow: hidden;">
                    <div style="background: linear-gradient(135deg, #0d5bd7, #0a4ab3); color: white; padding: 25px; text-align: center;">
                        <h2 style="margin: 0;">🎫 TicketRush</h2>
                        <p style="margin: 5px 0 0; opacity: 0.9;">Your ticket is ready!</p>
                    </div>
                    <div style="padding: 25px;">
                        <p>Hello,</p>
                        <p>Your ticket for <strong>%s</strong> has been confirmed.</p>
                        <p><strong>Order Number:</strong> %s</p>
                        <p><strong>Seat:</strong> %s</p>
                        <p style="text-align: center; margin-top: 20px;">Thank you for choosing TicketRush!</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            ticket.getEventName(),
            ticket.getOrderNumber(),
            ticket.getSeatNumber()
        );

        Map<String, Object> emailData = Map.of(
            "from", "TicketRush <onboarding@resend.dev>",
            "to", to,
            "subject", subject,
            "html", htmlContent
        );

        String jsonBody = objectMapper.writeValueAsString(emailData);
        RequestBody body = RequestBody.create(jsonBody, JSON);
        
        Request request = new Request.Builder()
            .url(RESEND_API_URL)
            .post(body)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("✅ Ticket email sent successfully to: {}", to);
            } else {
                log.error("❌ Failed to send ticket email");
            }
        }
    } catch (Exception e) {
        log.error("❌ Error sending ticket email: {}", e.getMessage());
        throw new RuntimeException("Failed to send ticket email", e);
    }
}
}