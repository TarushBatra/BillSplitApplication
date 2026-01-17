package com.billsplit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class GroupRequest {
    
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;
    
    private List<String> memberEmails;
    
    public GroupRequest() {}
    
    public GroupRequest(String name, List<String> memberEmails) {
        this.name = name;
        this.memberEmails = memberEmails;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getMemberEmails() {
        return memberEmails;
    }
    
    public void setMemberEmails(List<String> memberEmails) {
        this.memberEmails = memberEmails;
    }
}

