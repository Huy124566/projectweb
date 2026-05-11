package com.example.Ticket_Rush.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.Ticket_Rush.dto.UserUpdateRequest;
import com.example.Ticket_Rush.entity.User;
import com.example.Ticket_Rush.entity.UserRole;
import com.example.Ticket_Rush.repository.TicketRepository;
import com.example.Ticket_Rush.repository.UserRepository;
import com.example.Ticket_Rush.utils.BCryptEncoder;  // ← THÊM IMPORT

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final BCryptEncoder bcryptEncoder;  // ← THÊM DÒNG NÀY

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public Optional<User> getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(String username, String email, String password, String gender, String dateOfBirth) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        
        // 🔥 SỬA: MÃ HÓA MẬT KHẨU TRƯỚC KHI LƯU
        user.setPassword(bcryptEncoder.encode(password));
        
        user.setGender(gender);
        if (dateOfBirth != null && !dateOfBirth.isEmpty()) {
            user.setDateOfBirth(LocalDate.parse(dateOfBirth));
        }
        user.setCreatedAt(LocalDateTime.now());
        user.setPhone("");
        user.setAvatar("https://i.pravatar.cc/150");
        user.setRole(UserRole.ROLE_USER);
        return userRepository.save(user);
    }

    public User updateUser(Long id, UserUpdateRequest request) {
        User user = getById(id);
        
        if (request.username != null && !request.username.isEmpty()) {
            user.setUsername(request.username);
        }
        if (request.email != null && !request.email.isEmpty()) {
            user.setEmail(request.email);
        }
        if (request.phone != null) {
            user.setPhone(request.phone);
        }
        if (request.password != null && !request.password.isEmpty()) {
            user.setPassword(bcryptEncoder.encode(request.password));
        }
        if (request.avatar != null) {
            user.setAvatar(request.avatar);
        }
      
        if (request.gender != null && !request.gender.isEmpty()) {
            user.setGender(request.gender);
        }
        if (request.dateOfBirth != null && !request.dateOfBirth.isEmpty()) {
            user.setDateOfBirth(java.time.LocalDate.parse(request.dateOfBirth));
        }
        
        return userRepository.save(user);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Long getTicketCount(Long userId) {
        Long count = ticketRepository.countByUserId(userId);
        return count != null ? count : 0L;
    }
    
    public Long getAttendedCount(Long userId) {
        Long count = ticketRepository.countByUserIdAndStatus(userId, "ATTENDED");
        return count != null ? count : 0L;
    }

    public User updateAvatar(Long userId, String avatarBase64) {
        User user = getById(userId);
        user.setAvatar(avatarBase64);
        return userRepository.save(user);
    }
 
    public void updatePassword(String email, String newPassword) {
        User user = getByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // 🔥 SỬA: MÃ HÓA MẬT KHẨU MỚI
        user.setPassword(bcryptEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
 // Thêm vào UserService.java
    public User updateUserDirect(User user) {
        return userRepository.save(user);
    }
}