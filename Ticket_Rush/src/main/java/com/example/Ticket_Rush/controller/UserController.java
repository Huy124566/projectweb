package com.example.Ticket_Rush.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.Ticket_Rush.entity.User;
import com.example.Ticket_Rush.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin("*")
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates
    ) {
        try {
            User user = userService.getById(id);
            
            // Cập nhật các field được gửi lên
            if (updates.containsKey("username")) {
                user.setUsername((String) updates.get("username"));
            }
            if (updates.containsKey("email")) {
                user.setEmail((String) updates.get("email"));
            }
            if (updates.containsKey("phone")) {
                user.setPhone((String) updates.get("phone"));
            }
            // 🔥 THÊM: Xử lý gender
            if (updates.containsKey("gender")) {
                user.setGender((String) updates.get("gender"));
            }
            // 🔥 THÊM: Xử lý dateOfBirth
            if (updates.containsKey("dateOfBirth")) {
                String dob = (String) updates.get("dateOfBirth");
                if (dob != null && !dob.isEmpty()) {
                    user.setDateOfBirth(java.time.LocalDate.parse(dob));
                }
            }
            if (updates.containsKey("password")) {
                user.setPassword((String) updates.get("password"));
            }
            if (updates.containsKey("avatar")) {
                user.setAvatar((String) updates.get("avatar"));
            }
            
            User updated = userService.updateUserDirect(user);
            return ResponseEntity.ok(updated);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Long>> getUserStats(@PathVariable Long id) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("ticketCount", userService.getTicketCount(id));
        stats.put("attendedCount", userService.getAttendedCount(id));
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<Map<String, String>> updateAvatar(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String avatarBase64 = request.get("avatar");
            if (avatarBase64 == null || avatarBase64.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Avatar data is required"));
            }
            User updated = userService.updateAvatar(id, avatarBase64);
            return ResponseEntity.ok(Map.of(
                "avatar", updated.getAvatar(),
                "message", "Avatar updated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}