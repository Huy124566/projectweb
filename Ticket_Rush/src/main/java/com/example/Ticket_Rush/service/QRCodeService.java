package com.example.Ticket_Rush.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
@Slf4j
public class QRCodeService {

    // Tạo QR Code dưới dạng Base64 (trả về image data URL)
    public String generateQRCodeBase64(String text, int width, int height) {
        try {
            log.info("🔲 Generating QR Code for text: {}, size: {}x{}", text, width, height);
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            byte[] qrCodeBytes = outputStream.toByteArray();
            log.info("✅ QR Code generated, size: {} bytes", qrCodeBytes.length);
            
            // Xóa ký tự xuống dòng trong Base64 (đảm bảo trình duyệt mail đọc được)
            String base64Content = Base64.getEncoder().encodeToString(qrCodeBytes)
                .replace("\n", "")
                .replace("\r", "");
            
            String result = "data:image/png;base64," + base64Content;
            log.debug("🔗 QR Code data URL length: {} characters", result.length());
            
            return result;
            
        } catch (WriterException | IOException e) {
            log.error("❌ Failed to generate QR Code: {}", e.getMessage(), e);
            return "";
        }
    }
    
    // Tạo QR Code cho vé
    public String generateTicketQRCode(Long ticketId, String eventName, String seatNumber, String orderNumber) {
        String qrText = String.format(
            "TICKET_%d_%s_%s", 
            ticketId, 
            orderNumber != null ? orderNumber : "UNKNOWN",
            seatNumber != null ? seatNumber : "UNKNOWN"
        );
        log.debug("📦 QR Text: {}", qrText);
        return generateQRCodeBase64(qrText, 250, 250);
    }
}