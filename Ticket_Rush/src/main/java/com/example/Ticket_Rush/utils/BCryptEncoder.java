package com.example.Ticket_Rush.utils;

import org.springframework.stereotype.Component;
import at.favre.lib.crypto.bcrypt.BCrypt;

@Component
public class BCryptEncoder {

    /**
     * Mã hóa mật khẩu
     * @param rawPassword Mật khẩu dạng plain text
     * @return Mật khẩu đã được mã hóa
     */
    public String encode(String rawPassword) {
        return BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray());
    }

    /**
     * Kiểm tra mật khẩu
     * @param rawPassword Mật khẩu người dùng nhập
     * @param encodedPassword Mật khẩu đã mã hóa trong database
     * @return true nếu đúng, false nếu sai
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), encodedPassword).verified;
    }
}