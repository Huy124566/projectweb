package com.example.Ticket_Rush.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ticket_Rush.dto.AuthResponse;
import com.example.Ticket_Rush.dto.LoginRequest;
import com.example.Ticket_Rush.dto.LoginResponse;
import com.example.Ticket_Rush.dto.OtpRequest;
import com.example.Ticket_Rush.dto.RegisterRequest;
import com.example.Ticket_Rush.entity.User;
import com.example.Ticket_Rush.entity.UserRole;
import com.example.Ticket_Rush.repository.UserRepository;
import com.example.Ticket_Rush.service.AuthService;
import com.example.Ticket_Rush.service.JwtService;
import com.example.Ticket_Rush.service.OtpService;
import com.example.Ticket_Rush.service.ResendEmailService;
import com.example.Ticket_Rush.service.UserService;
import com.example.Ticket_Rush.utils.BCryptEncoder;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final ResendEmailService emailService;
    private final UserService userService;
    private final JwtService jwtService;
    private final BCryptEncoder bcryptEncoder;
    private final UserRepository userRepository;
    // ========== API CŨ ==========
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(buildLoginResponse(response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ========== API OTP ==========
    
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Email không được để trống");
        }
        
        String otp = otpService.generateAndSaveOtp(email);
        
        try {
            emailService.sendOtpEmail(email, otp, "REGISTER");
            return ResponseEntity.ok(Map.of("message", "✅ Mã OTP đã gửi đến " + email));
        } catch (Exception e) {
            otpService.clearOtp(email);
            return ResponseEntity.internalServerError().body("❌ Gửi email thất bại: " + e.getMessage());
        }
    }
    
    @PostMapping("/register-otp")
    public ResponseEntity<?> registerWithOtp(@RequestBody OtpRequest request) {
        if (!otpService.verifyOtp(request.getEmail(), request.getOtp())) {
            return ResponseEntity.badRequest().body("❌ Mã OTP không đúng hoặc đã hết hạn");
        }
        
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("❌ Email '" + request.getEmail() + "' đã được đăng ký");
        }
        
        if (userService.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("❌ Username '" + request.getUsername() + "' đã tồn tại, vui lòng chọn tên khác");
        }
        
        try {
            userService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getGender(),
                request.getDateOfBirth()
            );
            
            otpService.clearAll(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "✅ Đăng ký thành công!"));
            
        } catch (Exception e) {
            e.printStackTrace();
            
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            
            String errorMessage = rootCause.getMessage();
            
            if (errorMessage != null && errorMessage.contains("Duplicate entry")) {
                if (errorMessage.contains("username")) {
                    return ResponseEntity.badRequest().body("❌ Username '" + request.getUsername() + "' đã tồn tại. Vui lòng chọn tên khác.");
                } else if (errorMessage.contains("email")) {
                    return ResponseEntity.badRequest().body("❌ Email '" + request.getEmail() + "' đã được đăng ký.");
                }
                return ResponseEntity.badRequest().body("❌ Thông tin đã tồn tại trong hệ thống.");
            }
            
            return ResponseEntity.badRequest().body("❌ Đăng ký thất bại: " + errorMessage);
        }
    }
    
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Email không được để trống");
        }
        
        otpService.clearOtp(email);
        String otp = otpService.generateAndSaveOtp(email);
        
        try {
            emailService.sendOtpEmail(email, otp, "REGISTER");
            return ResponseEntity.ok(Map.of("message", "✅ Đã gửi lại mã OTP mới"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("❌ Gửi lại thất bại: " + e.getMessage());
        }
    }
    
    // ========== API QUÊN MẬT KHẨU ==========
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Email không được để trống");
        }
        
        if (!userService.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("❌ Email chưa được đăng ký");
        }
        
        String otp = otpService.generateAndSaveOtp(email);
        
        try {
            emailService.sendOtpEmail(email, otp, "FORGOT_PASSWORD");
            return ResponseEntity.ok(Map.of("message", "✅ Mã OTP đã gửi đến email của bạn"));
        } catch (Exception e) {
            otpService.clearOtp(email);
            return ResponseEntity.internalServerError().body("❌ Gửi email thất bại: " + e.getMessage());
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Email không được để trống");
        }
        if (otp == null || otp.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Mã OTP không được để trống");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Mật khẩu mới không được để trống");
        }
        
        if (!otpService.verifyOtp(email, otp)) {
            return ResponseEntity.badRequest().body("❌ Mã OTP không đúng hoặc đã hết hạn");
        }
        
        try {
            userService.updatePassword(email, newPassword);
            otpService.clearAll(email);
            
            return ResponseEntity.ok(Map.of(
                "message", "✅ Đổi mật khẩu thành công!",
                "success", true
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Đổi mật khẩu thất bại: " + e.getMessage());
        }
    }
    
    // ========== API ĐĂNG NHẬP BẰNG OTP ==========

    // ========== API OTP ==========

@PostMapping("/send-login-otp")
public ResponseEntity<?> sendLoginOtp(@RequestBody Map<String, String> request) {
    System.out.println("=== 🔵 sendLoginOtp START ===");
    
    String email = request.get("email");
    System.out.println("📧 Email received: " + email);
    
    if (email == null || email.trim().isEmpty()) {
        System.out.println("❌ Email is empty");
        return ResponseEntity.badRequest().body("❌ Email không được để trống");
    }
    
    System.out.println("🔍 Checking if email exists...");
    if (!userService.existsByEmail(email)) {
        System.out.println("❌ Email NOT registered: " + email);
        return ResponseEntity.badRequest().body("❌ Email chưa được đăng ký");
    }
    
    System.out.println("✅ Email exists, generating OTP...");
    String otp = otpService.generateAndSaveOtp(email);
    System.out.println("🔐 OTP generated: " + otp);
    
    System.out.println("📧 Attempting to send email...");
    try {
        emailService.sendOtpEmail(email, otp, "LOGIN");
        System.out.println("✅ Email sent successfully!");
        return ResponseEntity.ok(Map.of("message", "✅ Mã OTP đã gửi đến email của bạn"));
    } catch (Exception e) {
        System.out.println("❌ Email ERROR: " + e.getMessage());
        e.printStackTrace();
        otpService.clearOtp(email);
        return ResponseEntity.internalServerError().body("❌ Gửi email thất bại: " + e.getMessage());
    } finally {
        System.out.println("=== 🔵 sendLoginOtp END ===");
    }
}

    @PostMapping("/login-otp")
    public ResponseEntity<?> loginWithOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Email không được để trống");
        }
        if (otp == null || otp.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("❌ Mã OTP không được để trống");
        }
        
        if (!otpService.verifyOtp(email, otp)) {
            return ResponseEntity.badRequest().body("❌ Mã OTP không đúng hoặc đã hết hạn");
        }
        
        User user = userService.getByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        otpService.clearAll(email);
        
        return ResponseEntity.ok(buildLoginResponse(user, token));
    }
    
    // ========== API ĐỔI MẬT KHẨU ==========
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        
        if (email == null || currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body("Password must be at least 6 characters");
        }
        
        try {
            User user = userService.getByEmail(email)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
            
            if (!bcryptEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body("WRONG_PASSWORD");
            }
            
            user.setPassword(bcryptEncoder.encode(newPassword));
            userService.updateUserDirect(user);
            
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // ========== PRIVATE METHODS ==========
    
    private LoginResponse buildLoginResponse(AuthResponse response) {
        LoginResponse result = new LoginResponse();
        result.setMessage(response.getMessage());
        result.setToken(response.getToken());
        result.setId(response.getId());
        result.setEmail(response.getEmail());
        result.setUsername(response.getUsername());
        result.setRole(response.getRole() != null ? response.getRole().name() : "USER");
        result.setAvatar(response.getAvatar() != null ? response.getAvatar() : "");
        result.setPhone(response.getPhone() != null ? response.getPhone() : "");
        result.setGender(response.getGender() != null ? response.getGender() : "");
        result.setDateOfBirth(response.getDateOfBirth() != null ? response.getDateOfBirth().toString() : "");
        result.setCreatedAt(response.getCreatedAt() != null ? response.getCreatedAt().toString() : "");
        return result;
    }
    
    private LoginResponse buildLoginResponse(User user, String token) {
        LoginResponse result = new LoginResponse();
        result.setMessage("✅ Đăng nhập thành công!");
        result.setToken(token);
        result.setId(user.getId());
        result.setEmail(user.getEmail());
        result.setUsername(user.getUsername());
        result.setRole(user.getRole() != null ? user.getRole().name() : "USER");
        result.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        result.setPhone(user.getPhone() != null ? user.getPhone() : "");
        result.setGender(user.getGender() != null ? user.getGender() : "");
        result.setDateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "");
        result.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        return result;
    }
    
 // Thêm API này vào AuthController.java
    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody RegisterRequest request) {
        try {
            // Kiểm tra nếu chưa có admin nào
            long adminCount = userRepository.countByRole(UserRole.ROLE_ADMIN);
            if (adminCount > 0) {
                return ResponseEntity.badRequest().body("❌ Admin already exists! Only one admin allowed.");
            }
            
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest().body("❌ Username already exists");
            }
            
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body("❌ Email already exists");
            }
            
            User admin = new User();
            admin.setUsername(request.getUsername());
            admin.setEmail(request.getEmail());
            admin.setPassword(bcryptEncoder.encode(request.getPassword()));
            admin.setRole(UserRole.ROLE_ADMIN);
            admin.setPhone(request.getPhone() != null ? request.getPhone() : "");
            admin.setAvatar("");
            admin.setCreatedAt(java.time.LocalDateTime.now());
            
            userRepository.save(admin);
            
            return ResponseEntity.ok(Map.of(
                "message", "✅ Admin account created successfully!",
                "username", admin.getUsername(),
                "email", admin.getEmail(),
                "role", admin.getRole().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to create admin: " + e.getMessage());
        }
    }
}