package com.example.Ticket_Rush.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    // Lưu OTP tạm: key = email, value = otp
    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    
    // Lưu thời gian hết hạn: key = email, value = thời gian (millis)
    private final Map<String, Long> otpExpiry = new ConcurrentHashMap<>();
    
    // Lưu dữ liệu tạm khi đăng ký (trước khi verify OTP)
    private final Map<String, TempRegisterData> tempRegisterData = new ConcurrentHashMap<>();
    
    private final Random random = new Random();
    private static final long EXPIRY_MS = 5 * 60 * 1000; // 5 phút

    /**
     * Lớp lưu thông tin đăng ký tạm thời
     */
    public static class TempRegisterData {
        private final String username;
        private final String password;
        private final String gender;
        private final String dateOfBirth;
        
        public TempRegisterData(String username, String password, String gender, String dateOfBirth) {
            this.username = username;
            this.password = password;
            this.gender = gender;
            this.dateOfBirth = dateOfBirth;
        }
        
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getGender() { return gender; }
        public String getDateOfBirth() { return dateOfBirth; }
    }

    /**
     * Tạo OTP 6 số và lưu vào bộ nhớ tạm
     */
    public String generateAndSaveOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1000000));
        otpStorage.put(email, otp);
        otpExpiry.put(email, System.currentTimeMillis() + EXPIRY_MS);
        return otp;
    }

    /**
     * Lưu dữ liệu đăng ký tạm thời (trước khi xác thực OTP)
     */
    public void saveTempRegisterData(String email, String username, String password, String gender, String dateOfBirth) {
        tempRegisterData.put(email, new TempRegisterData(username, password, gender, dateOfBirth));
    }
    
    /**
     * Lấy dữ liệu đăng ký tạm thời
     */
    public TempRegisterData getTempRegisterData(String email) {
        return tempRegisterData.get(email);
    }
    
    /**
     * Xóa dữ liệu đăng ký tạm thời
     */
    public void clearTempRegisterData(String email) {
        tempRegisterData.remove(email);
    }

    /**
     * Xác thực OTP
     */
    public boolean verifyOtp(String email, String otp) {
        String savedOtp = otpStorage.get(email);
        Long expiryTime = otpExpiry.get(email);
        
        if (savedOtp == null || expiryTime == null) {
            return false; // Không có OTP
        }
        
        // Kiểm tra hết hạn
        if (System.currentTimeMillis() > expiryTime) {
            clearOtp(email); // Xóa OTP hết hạn
            return false;
        }
        
        // Kiểm tra đúng OTP
        return savedOtp.equals(otp);
    }

    /**
     * Xóa OTP sau khi xác thực thành công
     */
    public void clearOtp(String email) {
        otpStorage.remove(email);
        otpExpiry.remove(email);
    }
    
    /**
     * Xóa tất cả dữ liệu liên quan đến email (OTP + temp data)
     */
    public void clearAll(String email) {
        clearOtp(email);
        clearTempRegisterData(email);
    }
}