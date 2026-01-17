package com.billsplit.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pending_group_members", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "email"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PendingGroupMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    @Column(name = "name", length = 100)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Nullable - set if user has an account
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;
    
    @CreationTimestamp
    @Column(name = "invited_at", nullable = false, updatable = false)
    private LocalDateTime invitedAt;
    
    // Constructors
    public PendingGroupMember() {}
    
    public PendingGroupMember(Group group, String email, String name, User invitedBy) {
        this.group = group;
        this.email = email;
        this.name = name;
        this.invitedBy = invitedBy;
    }
    
    public PendingGroupMember(Group group, String email, String name, User user, User invitedBy) {
        this.group = group;
        this.email = email;
        this.name = name;
        this.user = user;
        this.invitedBy = invitedBy;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Group getGroup() {
        return group;
    }
    
    public void setGroup(Group group) {
        this.group = group;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public User getInvitedBy() {
        return invitedBy;
    }
    
    public void setInvitedBy(User invitedBy) {
        this.invitedBy = invitedBy;
    }
    
    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }
    
    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }
}
