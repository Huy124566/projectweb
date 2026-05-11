package com.example.Ticket_Rush.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ImageUploadService {

    @Value("${image.upload.dir:src/main/resources/static/images}")
    private String uploadDir;  // Lưu trực tiếp vào images

    public String uploadEventImage(MultipartFile file) {
        try {
            // Tạo tên file duy nhất
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            // Đường dẫn đầy đủ
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Lưu file
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());
            
            // Trả về URL
            return "/images/" + fileName;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }
}