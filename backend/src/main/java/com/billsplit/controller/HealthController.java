package com.billsplit.controller;

import com.billsplit.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {
    
    @Autowired(required = false)
    private EmailService emailService;
    
    @Value("${resend.api.key:}")
    private String resendApiKey;
    
    @Value("${resend.from.email:}")
    private String resendFromEmail;
    
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "BillSplit Backend");
        
        // Email configuration status (Resend)
        boolean emailConfigured = resendApiKey != null && !resendApiKey.trim().isEmpty() 
            && resendFromEmail != null && !resendFromEmail.trim().isEmpty();
        response.put("email_configured", String.valueOf(emailConfigured));
        response.put("resend_api_key_set", resendApiKey != null && !resendApiKey.trim().isEmpty() ? "YES" : "NO");
        response.put("resend_from_email_set", resendFromEmail != null && !resendFromEmail.trim().isEmpty() ? "YES" : "NO");
        response.put("email_provider", "Resend API");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test-email")
    public ResponseEntity<Map<String, String>> testEmail(@RequestParam(required = false) String to) {
        Map<String, String> response = new HashMap<>();
        
        if (to == null || to.trim().isEmpty()) {
            response.put("error", "Please provide 'to' parameter with email address");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (emailService == null) {
            response.put("error", "EmailService not available");
            return ResponseEntity.status(500).body(response);
        }
        
        try {
            emailService.sendGroupInvitation(to.trim(), "Test Group", "System Test");
            response.put("status", "success");
            response.put("message", "Test email sent to " + to);
            response.put("note", "Check your inbox and spam folder");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to send test email: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
