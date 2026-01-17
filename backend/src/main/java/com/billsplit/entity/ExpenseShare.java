package com.billsplit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_shares", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"expense_id", "user_id"}))
public class ExpenseShare {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull
    @DecimalMin(value = "0.00", message = "Amount owed must be non-negative")
    @Column(name = "amount_owed", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountOwed;
    
    // Constructors
    public ExpenseShare() {}
    
    public ExpenseShare(Expense expense, User user, BigDecimal amountOwed) {
        this.expense = expense;
        this.user = user;
        this.amountOwed = amountOwed;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Expense getExpense() {
        return expense;
    }
    
    public void setExpense(Expense expense) {
        this.expense = expense;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public BigDecimal getAmountOwed() {
        return amountOwed;
    }
    
    public void setAmountOwed(BigDecimal amountOwed) {
        this.amountOwed = amountOwed;
    }
}

