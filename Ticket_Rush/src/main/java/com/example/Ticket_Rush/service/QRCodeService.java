package com.example.Ticket_Rush.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class QRCodeService {

    // Tạo QR Code dưới dạng Base64 (trả về image data URL)
    public String generateQRCodeBase64(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            byte[] qrCodeBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeBytes);
            
        } catch (WriterException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Tạo QR Code cho vé
    public String generateTicketQRCode(Long ticketId, String eventName, String seatNumber, String orderNumber) {
        String qrText = String.format(
            "🎫 TICKETMASTER\nTicket ID: %d\nEvent: %s\nSeat: %s\nOrder: %s\nScan to verify",
            ticketId, eventName, seatNumber, orderNumber
        );
        return generateQRCodeBase64(qrText, 200, 200);
    }
}