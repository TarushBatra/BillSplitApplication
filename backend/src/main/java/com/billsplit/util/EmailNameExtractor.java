package com.billsplit.util;

public class EmailNameExtractor {
    
    /**
     * Extracts a name from an email address.
     * Examples:
     * - "john.doe@example.com" -> "John Doe"
     * - "jane_smith@gmail.com" -> "Jane Smith"
     * - "bob123@test.com" -> "Bob"
     * - "firstname.lastname@domain.com" -> "Firstname Lastname"
     */
    public static String extractNameFromEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        
        // Get the part before @
        String localPart = email.split("@")[0].trim();
        
        // Remove numbers and special characters except dots, underscores, and hyphens
        localPart = localPart.replaceAll("[^a-zA-Z._-]", "");
        
        if (localPart.isEmpty()) {
            return null;
        }
        
        // Split by common separators
        String[] parts = localPart.split("[._-]+");
        
        if (parts.length == 0) {
            return capitalize(localPart);
        }
        
        // Capitalize each part and join with space
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (name.length() > 0) {
                    name.append(" ");
                }
                name.append(capitalize(part));
            }
        }
        
        String result = name.toString().trim();
        return result.isEmpty() ? null : result;
    }
    
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + 
               (str.length() > 1 ? str.substring(1).toLowerCase() : "");
    }
}
