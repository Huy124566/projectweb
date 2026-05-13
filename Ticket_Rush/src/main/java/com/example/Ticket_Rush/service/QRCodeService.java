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

    public String generateQRCodeBase64(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            byte[] qrCodeBytes = outputStream.toByteArray();
            log.info("✅ QR Code generated, size: {} bytes", qrCodeBytes.length);
            
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeBytes);
            
        } catch (WriterException | IOException e) {
            log.error("❌ Failed to generate QR Code: {}", e.getMessage());
            return null;
        }
    }
    
    public String generateTicketQRCode(Long ticketId, String eventName, String seatNumber, String orderNumber) {
        String qrText = String.format("TICKET_%d_%s_%s", ticketId, orderNumber, seatNumber);
        String base64 = generateQRCodeBase64(qrText, 200, 200);
        // Trả về chuỗi có tiền tố data:image/png;base64,
        return base64; // generateQRCodeBase64 đã trả về "data:image/png;base64,..."
    }
}