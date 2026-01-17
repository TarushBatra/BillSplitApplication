package com.billsplit.dto;

import jakarta.validation.constraints.Size;

public class UpdateGroupRequest {
    
    @Size(min = 2, max = 100)
    private String name;
    
    @Size(max = 500)
    private String imageUrl;
    
    public UpdateGroupRequest() {}
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
