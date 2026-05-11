package com.example.Ticket_Rush.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // Gửi email với nội dung HTML
    public void sendTicket(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);  // true = gửi dạng HTML
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
            <head>
                <meta charset="UTF-8">
                <style>
                    .container { font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; border: 1px solid #eef2f6; border-radius: 20px; }
                    .header { background: linear-gradient(135deg, #0d5bd7, #0a4ab3); color: white; padding: 25px; text-align: center; border-radius: 15px 15px 0 0; }
                    .otp-code { font-size: 42px; font-weight: bold; color: #0d5bd7; text-align: center; padding: 25px; letter-spacing: 8px; background: #f8f9fc; border-radius: 12px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #888; font-size: 12px; border-top: 1px solid #eef2f6; margin-top: 20px; }
                    h2 { margin: 0; }
                    p { color: #555; line-height: 1.6; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🎫 TicketRush</h2>
                    </div>
                    <div style="padding: 10px 20px;">
                        <p>Hello,</p>
                        <p>You requested a verification code for <strong>%s</strong>.</p>
                        <div class="otp-code">
                            %s
                        </div>
                        <p style="text-align: center;">This code will expire in <strong>5 minutes</strong>.</p>
                        <p style="text-align: center; font-size: 14px; color: #888;">If you didn't request this, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 TicketRush. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            purpose.equals("REGISTER") ? "Email Verification" : (purpose.equals("FORGOT_PASSWORD") ? "Password Reset" : "Login"),
            otp
        );
        
        sendTicket(to, subject, htmlContent);
    }
}