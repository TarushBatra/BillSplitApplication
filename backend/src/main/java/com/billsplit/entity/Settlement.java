package com.billsplit.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Settlement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @CreationTimestamp
    @Column(name = "settled_at", nullable = false, updatable = false)
    private LocalDateTime settledAt;
    
    // Constructors
    public Settlement() {}
    
    public Settlement(Group group, User fromUser, User toUser, BigDecimal amount, String message, String imageUrl) {
        this.group = group;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
        this.message = message;
        this.imageUrl = imageUrl;
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
    
    public User getFromUser() {
        return fromUser;
    }
    
    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }
    
    public User getToUser() {
        return toUser;
    }
    
    public void setToUser(User toUser) {
        this.toUser = toUser;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public LocalDateTime getSettledAt() {
        return settledAt;
    }
    
    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }
}
