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
    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    @Value("${sendgrid.api.key:}")
    private String sendgridApiKey;
    
    @Value("${sendgrid.from.email:}")
    private String fromEmail;
    
    @Value("${sendgrid.from.name:BillSplit}")
    private String fromName;
    
    @Value("${sendgrid.reply.to:}")
    private String replyToEmail;
    
    @Value("${app.url:http://localhost:3000}")
    private String appUrl;
    
    @Value("${app.name:BillSplit}")
    private String appName;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Email validation pattern (RFC 5322 compliant)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    private boolean isEmailConfigured() {
        return sendgridApiKey != null && !sendgridApiKey.trim().isEmpty() 
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
     * Converts plain text to basic HTML format
     * @param text Plain text content
     * @return HTML formatted content
     */
    private String convertTextToHtml(String text) {
        if (text == null) {
            return "";
        }
        // Escape HTML and convert newlines to <br>
        String escaped = text.replace("&", "&amp;")
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("\n", "<br>");
        return "<html><body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">" +
               escaped +
               "</body></html>";
    }
    
    /**
     * Creates a professional HTML email template
     * @param title Email title
     * @param content Main content (HTML)
     * @param actionText Optional action button text
     * @param actionUrl Optional action button URL
     * @return Complete HTML email
     */
    private String createHtmlEmailTemplate(String title, String content, String actionText, String actionUrl) {
        String buttonHtml = "";
        if (actionText != null && actionUrl != null && !actionText.isEmpty() && !actionUrl.isEmpty()) {
            buttonHtml = "<div style=\"text-align: center; margin: 30px 0;\">" +
                        "<a href=\"" + actionUrl + "\" style=\"background-color: #10b981; color: white; " +
                        "padding: 12px 30px; text-decoration: none; border-radius: 6px; " +
                        "display: inline-block; font-weight: bold;\">" + actionText + "</a>" +
                        "</div>";
        }
        
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head><meta charset=\"UTF-8\"></head>" +
               "<body style=\"margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;\">" +
               "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #f5f5f5; padding: 20px;\">" +
               "<tr><td align=\"center\">" +
               "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\">" +
               "<tr><td style=\"padding: 30px; background: linear-gradient(135deg, #10b981 0%, #059669 100%); border-radius: 8px 8px 0 0;\">" +
               "<h1 style=\"margin: 0; color: #ffffff; font-size: 24px; font-weight: 600;\">" + appName + "</h1>" +
               "</td></tr>" +
               "<tr><td style=\"padding: 30px;\">" +
               "<h2 style=\"margin: 0 0 20px 0; color: #111827; font-size: 20px; font-weight: 600;\">" + title + "</h2>" +
               "<div style=\"color: #374151; font-size: 16px; line-height: 1.6;\">" + content + "</div>" +
               buttonHtml +
               "</td></tr>" +
               "<tr><td style=\"padding: 20px 30px; background-color: #f9fafb; border-top: 1px solid #e5e7eb; border-radius: 0 0 8px 8px; text-align: center;\">" +
               "<p style=\"margin: 0; color: #6b7280; font-size: 14px;\">" +
               "This email was sent from " + appName + ". " +
               "If you have any questions, please contact us at " + (replyToEmail != null && !replyToEmail.isEmpty() ? replyToEmail : fromEmail) + "." +
               "</p>" +
               "<p style=\"margin: 10px 0 0 0; color: #9ca3af; font-size: 12px;\">" +
               "<a href=\"" + appUrl + "\" style=\"color: #10b981; text-decoration: none;\">Visit " + appName + "</a>" +
               "</p>" +
               "</td></tr>" +
               "</table>" +
               "</td></tr>" +
               "</table>" +
               "</body>" +
               "</html>";
    }
    
    /**
     * Sends an email using Resend API with both text and HTML content
     * @param to Recipient email address
     * @param subject Email subject
     * @param text Plain text email body
     * @param html HTML email body (optional, falls back to text if not provided)
     * @return true if email was sent successfully, false otherwise
     */
    private boolean sendEmail(String to, String subject, String text, String html) {
        if (!isEmailConfigured()) {
            logger.warn("Email not configured. SENDGRID_API_KEY: {}, SENDGRID_FROM_EMAIL: {}", 
                    sendgridApiKey != null && !sendgridApiKey.isEmpty() ? "SET" : "NOT SET",
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
            headers.setBearerAuth(sendgridApiKey.trim());
            
            // Prepare request body for SendGrid API
            Map<String, Object> requestBody = new HashMap<>();
            
            // From email
            Map<String, String> fromMap = new HashMap<>();
            fromMap.put("email", fromEmail.trim());
            fromMap.put("name", fromName);
            requestBody.put("from", fromMap);
            
            // To email
            List<Map<String, Object>> personalizations = new java.util.ArrayList<>();
            Map<String, String> toMap = new HashMap<>();
            toMap.put("email", sanitizedEmail);
            List<Map<String, String>> toList = new java.util.ArrayList<>();
            toList.add(toMap);
            Map<String, Object> personalization = new HashMap<>();
            personalization.put("to", toList);
            personalizations.add(personalization);
            requestBody.put("personalizations", personalizations);
            
            // Subject
            requestBody.put("subject", subject);
            
            // Content (both text and HTML)
            List<Map<String, String>> content = new java.util.ArrayList<>();
            
            // Text content
            Map<String, String> textContent = new HashMap<>();
            textContent.put("type", "text/plain");
            textContent.put("value", text);
            content.add(textContent);
            
            // HTML content
            Map<String, String> htmlContent = new HashMap<>();
            htmlContent.put("type", "text/html");
            htmlContent.put("value", html != null && !html.trim().isEmpty() ? html : convertTextToHtml(text));
            content.add(htmlContent);
            
            requestBody.put("content", content);
            
            // Reply-to if configured
            if (replyToEmail != null && !replyToEmail.trim().isEmpty()) {
                requestBody.put("reply_to", replyToEmail.trim());
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            logger.info("Sending email via SendGrid API to {} (from: {})", sanitizedEmail, fromEmail);
            
            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                SENDGRID_API_URL,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Email sent successfully to {} via SendGrid API", sanitizedEmail);
                return true;
            } else {
                logger.error("Failed to send email to {}. SendGrid API returned status: {}", 
                        sanitizedEmail, response.getStatusCode());
                return false;
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("SendGrid API error (4xx) sending email to {}: Status: {}, Response: {}", 
                    sanitizedEmail, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            logger.error("SendGrid API error (5xx) sending email to {}: Status: {}, Response: {}", 
                    sanitizedEmail, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (Exception e) {
            logger.error("Failed to send email to {} via SendGrid API: {} - {}", 
                    sanitizedEmail, e.getClass().getSimpleName(), e.getMessage(), e);
            return false;
        }
    }
    
    public void sendGroupInvitation(String toEmail, String groupName, String inviterName) {
        logger.info("Attempting to send group invitation email to: {}", toEmail);
        
        String safeGroupName = safeString(groupName, "Unknown Group");
        String safeInviterName = safeString(inviterName, "Someone");
        
        String subject = "You're invited to join \"" + safeGroupName + "\" on " + appName;
        
        String text = "Hello,\n\n" + safeInviterName + " has invited you to join the group \"" + safeGroupName + "\" on " + appName + ".\n\n" +
                "To accept this invitation:\n" +
                "1. Visit: " + appUrl + "\n" +
                "2. Register or login with this email: " + toEmail + "\n" +
                "3. Go to your Dashboard to see and accept pending invitations\n\n" +
                "Best regards,\n" + appName + " Team";
        
        String htmlContent = "<p>Hello,</p>" +
                "<p><strong>" + safeInviterName + "</strong> has invited you to join the group <strong>\"" + safeGroupName + "\"</strong> on " + appName + ".</p>" +
                "<p>To accept this invitation:</p>" +
                "<ol>" +
                "<li>Visit: <a href=\"" + appUrl + "\">" + appUrl + "</a></li>" +
                "<li>Register or login with this email: <strong>" + toEmail + "</strong></li>" +
                "<li>Go to your Dashboard to see and accept pending invitations</li>" +
                "</ol>";
        
        String html = createHtmlEmailTemplate("Group Invitation", htmlContent, "Go to Dashboard", appUrl);
        
        boolean sent = sendEmail(toEmail, subject, text, html);
        if (sent) {
            logger.info("Group invitation email sent successfully to: {} via SendGrid", toEmail);
        } else {
            logger.error("Failed to send group invitation email to: {}. Check SendGrid configuration and logs above.", toEmail);
        }
    }
    
    public void sendExpenseNotification(String toEmail, String groupName, String expenseDescription, String amount) {
        String safeGroupName = safeString(groupName, "Unknown Group");
        String safeDescription = safeString(expenseDescription, "Expense");
        String safeAmount = safeString(amount, "0.00");
        
        String subject = "New expense in \"" + safeGroupName + "\"";
        
        String text = "Hello,\n\nA new expense has been added to the group \"" + safeGroupName + "\":\n\n" +
                "Description: " + safeDescription + "\n" +
                "Amount: $" + safeAmount + "\n\n" +
                "View details: " + appUrl + "\n\n" +
                "Best regards,\n" + appName + " Team";
        
        String htmlContent = "<p>Hello,</p>" +
                "<p>A new expense has been added to the group <strong>\"" + safeGroupName + "\"</strong>:</p>" +
                "<div style=\"background-color: #f3f4f6; padding: 15px; border-radius: 6px; margin: 20px 0;\">" +
                "<p style=\"margin: 5px 0;\"><strong>Description:</strong> " + safeDescription + "</p>" +
                "<p style=\"margin: 5px 0;\"><strong>Amount:</strong> <span style=\"color: #10b981; font-size: 18px; font-weight: bold;\">$" + safeAmount + "</span></p>" +
                "</div>";
        
        String groupUrl = appUrl + "/groups"; // Link to groups page
        String html = createHtmlEmailTemplate("New Expense Added", htmlContent, "View Group", groupUrl);
        
        sendEmail(toEmail, subject, text, html);
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
                
                // Convert settlement summary to HTML
                String htmlSummary = settlementSummary.toString()
                    .replace("\n", "<br>")
                    .replace("• ", "• ");
                
                String text = "Hello " + safeUserName + ",\n\n" +
                        settlementSummary.toString() + "\n" +
                        "View details: " + appUrl + "\n\n" +
                        "Best regards,\n" + appName + " Team";
                
                String htmlContent = "<p>Hello <strong>" + safeUserName + "</strong>,</p>" +
                        "<p>Here's the settlement summary for the group <strong>\"" + safeGroupName + "\"</strong>:</p>" +
                        "<div style=\"background-color: #f3f4f6; padding: 15px; border-radius: 6px; margin: 20px 0;\">" +
                        htmlSummary +
                        "</div>";
                
                String groupUrl = appUrl + "/groups"; // Link to groups page
                String html = createHtmlEmailTemplate("Settlement Summary", htmlContent, "View Details", groupUrl);
                
                sendEmail(sanitizedEmail, subject, text, html);
            }
        } catch (Exception e) {
            logger.error("Failed to send settlement notifications for group {}: {}", groupId, e.getMessage(), e);
        }
    }
    
    public void sendInvitationRejectionNotification(String toEmail, String groupName, String rejecterName) {
        String safeGroupName = safeString(groupName, "Unknown Group");
        String safeRejecterName = safeString(rejecterName, "Someone");
        
        String subject = "Invitation Rejected: " + safeGroupName;
        
        String text = "Hello,\n\n" + safeRejecterName + " has rejected your invitation to join the group \"" + safeGroupName + "\" on " + appName + ".\n\n" +
                "View your groups: " + appUrl + "\n\n" +
                "Best regards,\n" + appName + " Team";
        
        String htmlContent = "<p>Hello,</p>" +
                "<p><strong>" + safeRejecterName + "</strong> has rejected your invitation to join the group <strong>\"" + safeGroupName + "\"</strong> on " + appName + ".</p>";
        
        String html = createHtmlEmailTemplate("Invitation Rejected", htmlContent, "View Groups", appUrl);
        
        sendEmail(toEmail, subject, text, html);
    }
}
