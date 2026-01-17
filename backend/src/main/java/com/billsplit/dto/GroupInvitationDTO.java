package com.billsplit.dto;

import java.time.LocalDateTime;

public class GroupInvitationDTO {
    private Long id;
    private Long groupId;
    private String groupName;
    private String inviterName;
    private String inviterEmail;
    private LocalDateTime invitedAt;
    
    public GroupInvitationDTO() {}
    
    public GroupInvitationDTO(Long id, Long groupId, String groupName, String inviterName, String inviterEmail, LocalDateTime invitedAt) {
        this.id = id;
        this.groupId = groupId;
        this.groupName = groupName;
        this.inviterName = inviterName;
        this.inviterEmail = inviterEmail;
        this.invitedAt = invitedAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getInviterName() {
        return inviterName;
    }
    
    public void setInviterName(String inviterName) {
        this.inviterName = inviterName;
    }
    
    public String getInviterEmail() {
        return inviterEmail;
    }
    
    public void setInviterEmail(String inviterEmail) {
        this.inviterEmail = inviterEmail;
    }
    
    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }
    
    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }
}
