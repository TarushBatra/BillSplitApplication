package com.billsplit.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false)
    private String name;
    
    @NotBlank
    @Email
    @Size(max = 100)
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @JsonIgnore
    @NotBlank
    @Size(max = 100)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GroupMember> groupMemberships = new ArrayList<>();
    
    @JsonIgnore
    @OneToMany(mappedBy = "paidBy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Expense> expenses = new ArrayList<>();
    
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExpenseShare> expenseShares = new ArrayList<>();
    
    // Constructors
    public User() {}
    
    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<GroupMember> getGroupMemberships() {
        return groupMemberships;
    }
    
    public void setGroupMemberships(List<GroupMember> groupMemberships) {
        this.groupMemberships = groupMemberships;
    }
    
    public List<Expense> getExpenses() {
        return expenses;
    }
    
    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }
    
    public List<ExpenseShare> getExpenseShares() {
        return expenseShares;
    }
    
    public void setExpenseShares(List<ExpenseShare> expenseShares) {
        this.expenseShares = expenseShares;
    }
}

