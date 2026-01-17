package com.billsplit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    
    @NotBlank
    @Size(min = 2, max = 50)
    private String name;
    
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;
    
    public UpdateProfileRequest() {}
    
    public UpdateProfileRequest(String name, String email) {
        this.name = name;
        this.email = email;
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
