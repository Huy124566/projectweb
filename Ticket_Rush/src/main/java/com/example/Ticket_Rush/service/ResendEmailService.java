package com.example.Ticket_Rush.service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
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

    private final QRCodeService qrCodeService;
    
    // Bỏ final để tránh xung đột với constructor
    private OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    private ObjectMapper objectMapper = new ObjectMapper();

    // Constructor
    public ResendEmailService(QRCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    // ========== PHƯƠNG THỨC GỬI EMAIL CƠ BẢN ==========
    
    private void sendEmail(String to, String subject, String htmlContent) {
        log.info("📧 Sending email to: {}", to);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("❌ RESEND_API_KEY is missing!");
            throw new RuntimeException("Resend API Key is missing");
        }
        
        try {
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
                .addHeader("Accept", "application/json")  // Thêm header Accept
                .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful()) {
                    log.info("✅ Email sent successfully to: {}", to);
                } else {
                    log.error("❌ Failed to send email. Status: {}, Response: {}", 
                        response.code(), responseBody);
                    throw new RuntimeException("Failed to send email: " + responseBody);
                }
            }
        } catch (IOException e) {
            log.error("❌ IO Error: {}", e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    // ========== GỬI OTP EMAIL (dùng Switch Expression Java 17+) ==========
    
    public void sendOtpEmail(String to, String otp, String purpose) {
        log.info("🚀 sendOtpEmail to: {}, purpose: {}", to, purpose);
        
        // Switch expression Java 17+
        String subject = switch (purpose) {
            case "REGISTER" -> "Verify Your Email - TicketRush";
            case "FORGOT_PASSWORD" -> "Reset Your Password - TicketRush";
            default -> "Login Verification - TicketRush";
        };
        
        String purposeText = switch (purpose) {
            case "REGISTER" -> "Email Verification";
            case "FORGOT_PASSWORD" -> "Password Reset";
            default -> "Login";
        };

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f7fa;">
                <div style="max-width: 500px; margin: 0 auto; background: white; border-radius: 20px; overflow: hidden;">
                    <div style="background: linear-gradient(135deg, #0d5bd7, #0a4ab3); color: white; padding: 25px; text-align: center;">
                        <h2 style="margin: 0;">🎫 TicketRush</h2>
                    </div>
                    <div style="padding: 25px;">
                        <p>Hello,</p>
                        <p>You requested a verification code for <strong>%s</strong>.</p>
                        <div style="font-size: 42px; font-weight: bold; color: #0d5bd7; text-align: center; padding: 20px; background: #f8f9fc; border-radius: 12px; margin: 20px 0;">
                            %s
                        </div>
                        <p style="text-align: center;">This code expires in <strong>5 minutes</strong>.</p>
                        <p style="text-align: center; font-size: 12px; color: #888;">If you didn't request this, please ignore this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """, purposeText, otp);

        sendEmail(to, subject, htmlContent);
    }

    // ========== GỬI TICKET CONFIRMATION EMAIL (CÓ QR CODE) ==========
    
    public void sendTicketConfirmation(String to, Ticket ticket) {
        log.info("🚀 sendTicketConfirmation to: {}, ticketId: {}", to, ticket.getId());
        
        String eventDate = ticket.getEventDate() != null 
            ? ticket.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
            : "Date TBA";
        String eventTime = ticket.getEventDate() != null 
            ? ticket.getEventDate().format(DateTimeFormatter.ofPattern("HH:mm")) 
            : "Time TBA";
        
        // Generate QR Code Base64 (đã bao gồm tiền tố data:image/png;base64,)
        String qrBase64 = qrCodeService.generateTicketQRCode(
            ticket.getId(),
            ticket.getEventName(),
            ticket.getSeatNumber(),
            ticket.getOrderNumber()
        );
        
        // QR HTML - đã có tiền tố trong qrBase64, không cần thêm nữa
        String qrHtml = (qrBase64 != null && !qrBase64.isEmpty()) 
            ? String.format("""
                <div style="text-align: center; margin: 25px 0;">
                    <img src="%s" alt="QR Code" 
                         style="display: block; margin: 0 auto; width: 200px; height: 200px; border: 1px solid #eef2f6; border-radius: 12px; padding: 10px;">
                    <div style="font-size: 12px; color: #888; margin-top: 10px;">🔍 Scan this QR code at the venue entrance</div>
                </div>
                """, qrBase64)
            : "";
        
        String subject = "🎫 Ticket Confirmation - " + ticket.getEventName();
        
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
                        
                        <div style="background: #f8f9fc; padding: 15px; border-radius: 12px; margin: 15px 0;">
                            <p><strong>🎤 Event:</strong> %s</p>
                            <p><strong>🆔 Order:</strong> %s</p>
                            <p><strong>💺 Seat:</strong> %s</p>
                            <p><strong>📍 Venue:</strong> %s</p>
                            <p><strong>📅 Date:</strong> %s</p>
                            <p><strong>⏰ Time:</strong> %s</p>
                        </div>
                        
                        %s
                        
                        <p style="text-align: center; margin-top: 20px;">Thank you for choosing TicketRush! 🎉</p>
                    </div>
                    <div style="text-align: center; padding: 15px; color: #888; font-size: 12px; border-top: 1px solid #eef2f6;">
                        <p>&copy; 2025 TicketRush</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            ticket.getEventName(),
            ticket.getEventName(),
            ticket.getOrderNumber(),
            ticket.getSeatNumber(),
            ticket.getEventVenue() != null ? ticket.getEventVenue() : "Venue TBA",
            eventDate,
            eventTime,
            qrHtml
        );

        sendEmail(to, subject, htmlContent);
    }
}