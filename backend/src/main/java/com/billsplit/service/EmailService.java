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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;
    
    @Value("${app.url:http://localhost:3000}")
    private String appUrl;
    
    // Email validation pattern (RFC 5322 compliant)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    private boolean isEmailConfigured() {
        return mailSender != null && fromEmail != null && !fromEmail.trim().isEmpty();
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
    
    public void sendGroupInvitation(String toEmail, String groupName, String inviterName) {
        if (!isEmailConfigured()) {
            logger.warn("Email not configured. MAIL_USERNAME: {}, mailSender: {}", 
                    fromEmail != null && !fromEmail.isEmpty() ? "SET" : "NOT SET", 
                    mailSender != null ? "AVAILABLE" : "NULL");
            return;
        }
        
        // Validate and sanitize inputs
        String sanitizedEmail = toEmail != null ? toEmail.trim() : null;
        if (!isValidEmail(sanitizedEmail)) {
            logger.warn("Invalid email address for group invitation: {}", toEmail);
            return;
        }
        
        String safeGroupName = safeString(groupName, "Unknown Group");
        String safeInviterName = safeString(inviterName, "Someone");
        
        logger.info("Attempting to send invitation email to {} for group {}", sanitizedEmail, safeGroupName);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(sanitizedEmail);
            message.setSubject("Invitation to join group: " + safeGroupName);
            message.setText("Hello,\n\n" + safeInviterName + " has invited you to join the group \"" + safeGroupName + "\" on BillSplit.\n\n" +
                    "To accept this invitation:\n" +
                    "1. Visit: " + appUrl + "\n" +
                    "2. Register or login with this email: " + sanitizedEmail + "\n" +
                    "3. Go to your Dashboard to see and accept pending invitations\n\n" +
                    "Best regards,\nBillSplit Team");
            
            // Gmail requires the "from" address to match the authenticated email
            // If fromEmail is set, use it; otherwise use MAIL_USERNAME
            String fromAddress = fromEmail != null && !fromEmail.trim().isEmpty() ? fromEmail.trim() : null;
            if (fromAddress == null) {
                logger.error("Cannot send email: fromEmail is not configured. Set MAIL_USERNAME in environment variables.");
                return;
            }
            message.setFrom(fromAddress);
            
            logger.info("Sending email from {} to {} via SMTP", fromAddress, sanitizedEmail);
            mailSender.send(message);
            logger.info("Group invitation email sent successfully to {} (from: {})", sanitizedEmail, fromAddress);
        } catch (org.springframework.mail.MailAuthenticationException e) {
            logger.error("Email authentication failed. Check MAIL_USERNAME and MAIL_PASSWORD. Error: {}", e.getMessage(), e);
        } catch (org.springframework.mail.MailSendException e) {
            logger.error("Failed to send email to {}. SMTP error: {}. Check: 1) MAIL_USERNAME matches authenticated email, 2) App password is correct, 3) Gmail security settings. Full error: {}", 
                    sanitizedEmail, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Failed to send group invitation email to {}: {} - {}", sanitizedEmail, e.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
    
    public void sendExpenseNotification(String toEmail, String groupName, String expenseDescription, String amount) {
        if (!isEmailConfigured()) {
            logger.info("Email not configured. Skipping expense notification email to {}", toEmail);
            return;
        }
        
        // Validate and sanitize inputs
        String sanitizedEmail = toEmail != null ? toEmail.trim() : null;
        if (!isValidEmail(sanitizedEmail)) {
            logger.warn("Invalid email address for expense notification: {}", toEmail);
            return;
        }
        
        String safeGroupName = safeString(groupName, "Unknown Group");
        String safeDescription = safeString(expenseDescription, "Expense");
        String safeAmount = safeString(amount, "0.00");
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(sanitizedEmail);
            message.setSubject("New expense added to group: " + safeGroupName);
            message.setText("Hello,\n\nA new expense has been added to the group \"" + safeGroupName + "\":\n\n" +
                    "Description: " + safeDescription + "\n" +
                    "Amount: $" + safeAmount + "\n\n" +
                    "View details: " + appUrl + "\n\n" +
                    "Best regards,\nBillSplit Team");
            message.setFrom(fromEmail);
            
            mailSender.send(message);
            logger.info("Expense notification email sent successfully to {}", sanitizedEmail);
        } catch (Exception e) {
            logger.error("Failed to send expense notification email to {}: {}", sanitizedEmail, e.getMessage(), e);
        }
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
                
                String safeGroupName = safeString(group != null ? group.getName() : null, "Unknown Group");
                String safeUserName = safeString(userName, "there");
                
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(sanitizedEmail);
                    message.setSubject("Settlement Summary: " + safeGroupName);
                    message.setText("Hello " + safeUserName + ",\n\n" +
                            settlementSummary.toString() + "\n" +
                            "View details: " + appUrl + "\n\n" +
                            "Best regards,\nBillSplit Team");
                    message.setFrom(fromEmail);
                    
                    mailSender.send(message);
                    logger.info("Settlement notification email sent successfully to {}", sanitizedEmail);
                } catch (Exception e) {
                    logger.error("Failed to send settlement notification to {}: {}", 
                            sanitizedEmail, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to send settlement notifications for group {}: {}", groupId, e.getMessage(), e);
        }
    }
    
    public void sendInvitationRejectionNotification(String toEmail, String groupName, String rejecterName) {
        if (!isEmailConfigured()) {
            logger.info("Email not configured. Skipping rejection notification email to {}", toEmail);
            return;
        }
        
        // Validate and sanitize inputs
        String sanitizedEmail = toEmail != null ? toEmail.trim() : null;
        if (!isValidEmail(sanitizedEmail)) {
            logger.warn("Invalid email address for rejection notification: {}", toEmail);
            return;
        }
        
        String safeGroupName = safeString(groupName, "Unknown Group");
        String safeRejecterName = safeString(rejecterName, "Someone");
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(sanitizedEmail);
            message.setSubject("Invitation Rejected: " + safeGroupName);
            message.setText("Hello,\n\n" + safeRejecterName + " has rejected your invitation to join the group \"" + safeGroupName + "\" on BillSplit.\n\n" +
                    "View your groups: " + appUrl + "\n\n" +
                    "Best regards,\nBillSplit Team");
            message.setFrom(fromEmail);
            
            mailSender.send(message);
            logger.info("Invitation rejection notification email sent successfully to {}", sanitizedEmail);
        } catch (Exception e) {
            logger.error("Failed to send invitation rejection notification email to {}: {}", sanitizedEmail, e.getMessage(), e);
        }
    }
}

