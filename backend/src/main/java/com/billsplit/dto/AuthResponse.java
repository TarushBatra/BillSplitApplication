package com.billsplit.dto;

public class AuthResponse {
    
    private String accessToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String name;
    private String email;
    
    public AuthResponse() {}
    
    public AuthResponse(String accessToken, Long userId, String name, String email) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.name = name;
        this.email = email;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
}

