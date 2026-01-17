package com.billsplit.controller;

import com.billsplit.dto.AuthRequest;
import com.billsplit.dto.AuthResponse;
import com.billsplit.dto.RegisterRequest;
import com.billsplit.dto.UpdatePasswordRequest;
import com.billsplit.dto.UpdateProfileRequest;
import com.billsplit.entity.User;
import com.billsplit.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse response = authService.login(authRequest);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<User> getCurrentUser() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password for a user (for testing)")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestParam String email, @RequestParam String newPassword) {
        authService.resetPassword(email, newPassword);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successfully");
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update user profile")
    public ResponseEntity<User> updateProfile(@Valid @RequestBody UpdateProfileRequest updateRequest) {
        User updatedUser = authService.updateProfile(updateRequest);
        return ResponseEntity.ok(updatedUser);
    }
    
    @PutMapping("/password")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update user password")
    public ResponseEntity<Map<String, String>> updatePassword(@Valid @RequestBody UpdatePasswordRequest updateRequest) {
        authService.updatePassword(updateRequest);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password updated successfully");
        return ResponseEntity.ok(response);
    }
}

