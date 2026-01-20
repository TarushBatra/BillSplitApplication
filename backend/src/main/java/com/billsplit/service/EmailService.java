package com.billsplit.service;

import com.billsplit.dto.SettlementTransaction;
import com.billsplit.entity.Group;
import com.billsplit.entity.GroupMember;
import com.billsplit.repository.GroupMemberRepository;
import com.billsplit.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    @Value("${resend.api.key:}")
    private String resendApiKey;
    
    @Value("${resend.from.email:}")
    private String fromEmail;
    
    @Value("${app.url:http://localhost:3000}")
    private String appUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Email validation pattern (RFC 5322 compliant)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    private boolean isEmailConfigured() {
        return resendApiKey != null && !resendApiKey.trim().isEmpty() 
            && fromEmail != null && !fromEmail.trim().isEmpty();
    }
    
    /**
     * Validates email format
     * @param email Email address to validate
     * @return true if email format is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Safely gets a string value with fallback
     * @param value String value
     * @param fallback Fallback value if null or empty
     * @return Non-null string value
     */
    private String safeString(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback != null ? fallback : "";
        }
        return value.trim();
    }
    
    /**
     * Sends an email using Resend API
     * @param to Recipient email address
     * @param subject Email subject
     * @param text Email body text
     * @return true if email was sent successfully, false otherwise
     */
    private boolean sendEmail(String to, String subject, String text) {
        if (!isEmailConfigured()) {
            logger.warn("Email not configured. RESEND_API_KEY: {}, RESEND_FROM_EMAIL: {}", 
                    resendApiKey != null && !resendApiKey.isEmpty() ? "SET" : "NOT SET",
                    fromEmail != null && !fromEmail.isEmpty() ? "SET" : "NOT SET");
            return false;
        }
        
        // Validate and sanitize inputs
        String sanitizedEmail = to != null ? to.trim() : null;
        if (!isValidEmail(sanitizedEmail)) {
            logger.warn("Invalid email address: {}", to);
            return false;
        }
        
        try {
            // Prepare request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey.trim());
            
            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", fromEmail.trim());
            requestBody.put("to", sanitizedEmail);
            requestBody.put("subject", subject);
            requestBody.put("text", text);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            logger.info("Sending email via Resend API to {} (from: {})", sanitizedEmail, fromEmail);
            
            // Make API call
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                RESEND_API_URL,
                HttpMethod.POST,
                request,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Email sent successfully to {} via Resend API", sanitizedEmail);
                return true;
            } else {
                logger.error("Failed to send email to {}. Resend API returned status: {}", 
                        sanitizedEmail, response.getStatusCode());
                return false;
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("Resend API error (4xx) sending email to {}: Status: {}, Response: {}", 
                    sanitizedEmail, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            logger.error("Resend API error (5xx) sending email to {}: Status: {}, Response: {}", 
                    sanitizedEmail, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (Exception e) {
            logger.error("Failed to send email to {} via Resend API: {} - {}", 
                    sanitizedEmail, e.getClass().getSimpleName(), e.getMessage(), e);
            return false;
        }
    }
    
    public void sendGroupInvitation(String toEmail, String groupName, String inviterName) {
        String safeGroupName = safeString(groupName, "Unknown Group");
        String safeInviterName = safeString(inviterName, "Someone");
        
        String subject = "Invitation to join group: " + safeGroupName;
        String text = "Hello,\n\n" + safeInviterName + " has invited you to join the group \"" + safeGroupName + "\" on BillSplit.\n\n" +
                "To accept this invitation:\n" +
                "1. Visit: " + appUrl + "\n" +
                "2. Register or login with this email: " + toEmail + "\n" +
                "3. Go to your Dashboard to see and accept pending invitations\n\n" +
                "Best regards,\nBillSplit Team";
        
        sendEmail(toEmail, subject, text);
    }
    
    public void sendExpenseNotification(String toEmail, String groupName, String expenseDescription, String amount) {
        String safeGroupName = safeString(groupName, "Unknown Group");
        String safeDescription = safeString(expenseDescription, "Expense");
        String safeAmount = safeString(amount, "0.00");
        
        String subject = "New expense added to group: " + safeGroupName;
        String text = "Hello,\n\nA new expense has been added to the group \"" + safeGroupName + "\":\n\n" +
                "Description: " + safeDescription + "\n" +
                "Amount: $" + safeAmount + "\n\n" +
                "View details: " + appUrl + "\n\n" +
                "Best regards,\nBillSplit Team";
        
        sendEmail(toEmail, subject, text);
    }
    
    public void sendSettlementNotifications(Long groupId, List<SettlementTransaction> transactions) {
        if (!isEmailConfigured()) {
            logger.info("Email not configured. Skipping settlement notifications for group {}", groupId);
            return;
        }
        
        if (transactions == null || transactions.isEmpty()) {
            logger.info("No settlement transactions to notify for group {}", groupId);
            return;
        }
        
        try {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));
            
            List<GroupMember> members = groupMemberRepository.findByGroupWithUser(group);
            
            if (members.isEmpty()) {
                logger.warn("No members found for group {}", groupId);
                return;
            }
            
            // Build settlement summary
            StringBuilder settlementSummary = new StringBuilder();
            settlementSummary.append("Settlement Summary for group \"").append(group.getName()).append("\":\n\n");
            
            for (SettlementTransaction transaction : transactions) {
                String fromName = transaction.getFromUserName() != null ? 
                    transaction.getFromUserName() : "Unknown";
                String toName = transaction.getToUserName() != null ? 
                    transaction.getToUserName() : "Unknown";
                
                settlementSummary.append("• ").append(fromName)
                               .append(" owes ").append(toName)
                               .append(" $").append(transaction.getAmount() != null ? 
                                   String.format("%.2f", transaction.getAmount()) : "0.00")
                               .append("\n");
            }
            
            String safeGroupName = safeString(group != null ? group.getName() : null, "Unknown Group");
            
            // Send email to each group member
            for (GroupMember member : members) {
                // Null safety checks
                if (member.getUser() == null) {
                    logger.warn("Skipping member {} - user is null", member.getId());
                    continue;
                }
                
                String userEmail = member.getUser().getEmail();
                String userName = member.getUser().getName();
                
                // Validate and sanitize email
                String sanitizedEmail = userEmail != null ? userEmail.trim() : null;
                if (!isValidEmail(sanitizedEmail)) {
                    logger.warn("Skipping member {} - invalid email address: {}", member.getId(), userEmail);
                    continue;
                }
                
                String safeUserName = safeString(userName, "there");
                
                String subject = "Settlement Summary: " + safeGroupName;
                String text = "Hello " + safeUserName + ",\n\n" +
                        settlementSummary.toString() + "\n" +
                        "View details: " + appUrl + "\n\n" +
                        "Best regards,\nBillSplit Team";
                
                sendEmail(sanitizedEmail, subject, text);
            }
        } catch (Exception e) {
            logger.error("Failed to send settlement notifications for group {}: {}", groupId, e.getMessage(), e);
        }
    }
    
    public void sendInvitationRejectionNotification(String toEmail, String groupName, String rejecterName) {
        String safeGroupName = safeString(groupName, "Unknown Group");
        String safeRejecterName = safeString(rejecterName, "Someone");
        
        String subject = "Invitation Rejected: " + safeGroupName;
        String text = "Hello,\n\n" + safeRejecterName + " has rejected your invitation to join the group \"" + safeGroupName + "\" on BillSplit.\n\n" +
                "View your groups: " + appUrl + "\n\n" +
                "Best regards,\nBillSplit Team";
        
        sendEmail(toEmail, subject, text);
    }
}
