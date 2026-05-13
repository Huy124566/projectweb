package com.example.Ticket_Rush.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.Ticket_Rush.entity.Ticket;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendTicket(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    public void sendOtpEmail(String to, String otp, String purpose) {
        String subject;
        if (purpose.equals("REGISTER")) {
            subject = "Verify Your Email - TicketRush";
        } else if (purpose.equals("FORGOT_PASSWORD")) {
            subject = "Reset Your Password - TicketRush";
        } else {
            subject = "Login Verification - TicketRush";
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
            """, purpose.equals("REGISTER") ? "Email Verification" : (purpose.equals("FORGOT_PASSWORD") ? "Password Reset" : "Login"), otp);
        sendTicket(to, subject, htmlContent);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private String formatDateReadable(LocalDateTime dateTime) {
        if (dateTime == null) return "Date TBA";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy");
        return dateTime.format(formatter);
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) return "Time TBA";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
    }

    public void sendTicketConfirmation(String to, Ticket ticket) {
        String subject = "🎫 Ticket Confirmation - " + ticket.getEventName();

        String eventDateReadable = formatDateReadable(ticket.getEventDate());
        String eventTime = formatTime(ticket.getEventDate());
        
        String eventName = escapeHtml(ticket.getEventName());
        String eventVenue = escapeHtml(ticket.getEventVenue() != null ? ticket.getEventVenue() : "Venue TBA");
        String seatNumber = escapeHtml(ticket.getSeatNumber());
        String orderNumber = escapeHtml(ticket.getOrderNumber());

        String statusBadge = "<span style=\"background: #e8f0fe; color: #0d5bd7; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;\">✅ Upcoming</span>";

        // 🔥 DÙNG QUICKCHART API (THAY THẾ GOOGLE CHART) 🔥
        String rawData = "TICKET_" + ticket.getOrderNumber();
        String encodedData = URLEncoder.encode(rawData, StandardCharsets.UTF_8);
        String qrCodeUrl = String.format("https://quickchart.io/qr?text=%s&size=200&margin=1", encodedData);

        // Log để debug (copy link test trên trình duyệt)
        System.out.println("📧 Sending ticket email to: " + to);
        System.out.println("📦 Order: " + ticket.getOrderNumber());
        System.out.println("🔗 QR URL (test in browser): " + qrCodeUrl);

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f7fa;">
                <div style="max-width: 550px; margin: 0 auto; background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1);">
                    
                    <!-- HEADER -->
                    <div style="background: linear-gradient(135deg, #0d5bd7, #0a4ab3); color: white; padding: 25px; text-align: center;">
                        <h2 style="margin: 0;">🎫 TicketRush</h2>
                        <p style="margin: 5px 0 0; opacity: 0.9;">Your ticket is ready!</p>
                    </div>
                    
                    <!-- CONTENT -->
                    <div style="padding: 25px;">
                        <div style="text-align: center; margin-bottom: 15px;">%s</div>
                        <div style="font-size: 22px; font-weight: bold; color: #1a2a3a; margin-bottom: 15px; text-align: center;">🎉 %s</div>
                        
                        <div style="background: #f8f9fc; padding: 15px; border-radius: 12px; margin: 15px 0;">
                            <p style="margin: 10px 0; color: #4a5a6a;"><strong style="color: #1a2a3a; width: 80px; display: inline-block;">📍 Venue:</strong> %s</p>
                            <p style="margin: 10px 0; color: #4a5a6a;"><strong style="color: #1a2a3a; width: 80px; display: inline-block;">📅 Date:</strong> %s</p>
                            <p style="margin: 10px 0; color: #4a5a6a;"><strong style="color: #1a2a3a; width: 80px; display: inline-block;">⏰ Time:</strong> %s</p>
                            <p style="margin: 10px 0; color: #4a5a6a;"><strong style="color: #1a2a3a; width: 80px; display: inline-block;">💺 Seat:</strong> %s</p>
                            <p style="margin: 10px 0; color: #4a5a6a;"><strong style="color: #1a2a3a; width: 80px; display: inline-block;">🆔 Order:</strong> %s</p>
                        </div>
                        
                        <!-- QR CODE -->
                        <div style="text-align: center; margin: 25px 0;">
                            <img src="%s" alt="QR Code" style="display: block; margin: 0 auto; width: 180px; height: 180px; border: 1px solid #eef2f6; border-radius: 12px; padding: 10px;">
                            <div style="font-size: 12px; color: #888; text-align: center; margin-top: 10px;">🔍 Scan this QR code at the venue entrance</div>
                        </div>
                        
                        <p style="text-align: center; margin-top: 20px;">Thank you for choosing TicketRush!<br>Have a great experience! 🎉</p>
                    </div>
                    
                    <!-- FOOTER -->
                    <div style="text-align: center; padding: 15px; color: #888; font-size: 12px; border-top: 1px solid #eef2f6;">
                        <p>&copy; 2025 TicketRush. All rights reserved.</p>
                        <p style="font-size: 10px;">This is an automated confirmation, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            statusBadge,
            eventName,
            eventVenue,
            eventDateReadable,
            eventTime,
            seatNumber,
            orderNumber,
            qrCodeUrl
        );

        sendTicket(to, subject, htmlContent);
    }
}