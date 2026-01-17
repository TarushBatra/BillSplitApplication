package com.billsplit.service;

import com.billsplit.dto.AuthRequest;
import com.billsplit.dto.AuthResponse;
import com.billsplit.dto.RegisterRequest;
import com.billsplit.dto.UpdatePasswordRequest;
import com.billsplit.dto.UpdateProfileRequest;
import com.billsplit.entity.User;
import com.billsplit.repository.UserRepository;
import com.billsplit.security.JwtTokenProvider;
import com.billsplit.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    public AuthResponse register(RegisterRequest registerRequest) {
        String normalizedEmail = registerRequest.getEmail().trim().toLowerCase();
        
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email is already taken!");
        }
        
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        
        User savedUser = userRepository.save(user);
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        registerRequest.getPassword()
                )
        );
        
        String jwt = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        return new AuthResponse(jwt, savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }
    
    public AuthResponse login(AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getEmail().trim().toLowerCase(),
                            authRequest.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            return new AuthResponse(jwt, user.getId(), user.getName(), user.getEmail());
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new RuntimeException("Invalid email or password");
        }
    }
    
    public User getCurrentUser() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public void resetPassword(String email, String newPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + normalizedEmail));
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    public User updateProfile(UpdateProfileRequest updateRequest) {
        User user = getCurrentUser();
        String normalizedEmail = updateRequest.getEmail().trim().toLowerCase();
        
        // Check if email is already taken by another user
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail) && 
            userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email is already taken!");
        }
        
        user.setName(updateRequest.getName());
        user.setEmail(normalizedEmail);
        
        return userRepository.save(user);
    }
    
    public void updatePassword(UpdatePasswordRequest updateRequest) {
        User user = getCurrentUser();
        
        // Verify current password
        if (!passwordEncoder.matches(updateRequest.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Update to new password
        user.setPasswordHash(passwordEncoder.encode(updateRequest.getNewPassword()));
        userRepository.save(user);
    }
}

