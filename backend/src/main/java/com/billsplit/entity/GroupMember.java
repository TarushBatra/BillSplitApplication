package com.billsplit.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "group_members", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GroupMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private GroupRole role;
    
    @Column(name = "joined_at", nullable = false)
    private java.time.LocalDateTime joinedAt;
    
    // Constructors
    public GroupMember() {}
    
    public GroupMember(Group group, User user, GroupRole role) {
        this.group = group;
        this.user = user;
        this.role = role;
        this.joinedAt = java.time.LocalDateTime.now();
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
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public GroupRole getRole() {
        return role;
    }
    
    public void setRole(GroupRole role) {
        this.role = role;
    }
    
    public java.time.LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(java.time.LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public enum GroupRole {
        ADMIN, MEMBER
    }
}

