package com.example.Ticket_Rush.service;

import org.springframework.stereotype.Service;

import com.example.Ticket_Rush.dto.AuthResponse;
import com.example.Ticket_Rush.dto.LoginRequest;
import com.example.Ticket_Rush.dto.RegisterRequest;
import com.example.Ticket_Rush.entity.User;
import com.example.Ticket_Rush.entity.UserRole;
import com.example.Ticket_Rush.repository.UserRepository;
import com.example.Ticket_Rush.utils.BCryptEncoder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptEncoder bcryptEncoder;
    private final JwtService jwtService;

    // ================= REGISTER =================
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("USERNAME_EXISTS");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("EMAIL_EXISTS");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(bcryptEncoder.encode(request.getPassword()));
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setRole(UserRole.ROLE_USER);
        user.setPhone("");
        user.setAvatar("");

        userRepository.save(user);

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getAvatar(),
                null,
                "REGISTER_SUCCESS",
                user.getPhone(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getCreatedAt()
        );
    }

    // ================= LOGIN =================
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        if (!bcryptEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("WRONG_PASSWORD");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getAvatar(),
                token,
                "LOGIN_SUCCESS",
                user.getPhone(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getCreatedAt()
        );
    }
}