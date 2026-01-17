package com.billsplit.dto;

import com.billsplit.entity.Expense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ExpenseRequest {
    
    @NotNull
    private Long groupId;
    
    @NotBlank
    @Size(max = 200)
    private String description;
    
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @NotNull
    private Expense.SplitType splitType;
    
    private Long paidByUserId;
    
    private String paidByPendingMemberEmail;
    
    private List<ExpenseShareRequest> shares;
    
    private List<PendingShareRequest> pendingShares;
    
    public ExpenseRequest() {}
    
    public ExpenseRequest(Long groupId, String description, BigDecimal amount, 
                         Expense.SplitType splitType, List<ExpenseShareRequest> shares) {
        this.groupId = groupId;
        this.description = description;
        this.amount = amount;
        this.splitType = splitType;
        this.shares = shares;
    }
    
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public Expense.SplitType getSplitType() {
        return splitType;
    }
    
    public void setSplitType(Expense.SplitType splitType) {
        this.splitType = splitType;
    }
    
    public List<ExpenseShareRequest> getShares() {
        return shares;
    }
    
    public void setShares(List<ExpenseShareRequest> shares) {
        this.shares = shares;
    }
    
    public static class ExpenseShareRequest {
        private Long userId;
        private BigDecimal amountOwed;
        
        public ExpenseShareRequest() {}
        
        public ExpenseShareRequest(Long userId, BigDecimal amountOwed) {
            this.userId = userId;
            this.amountOwed = amountOwed;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public BigDecimal getAmountOwed() {
            return amountOwed;
        }
        
        public void setAmountOwed(BigDecimal amountOwed) {
            this.amountOwed = amountOwed;
        }
    }
    
    public static class PendingShareRequest {
        private String email;
        private BigDecimal amountOwed;
        
        public PendingShareRequest() {}
        
        public PendingShareRequest(String email, BigDecimal amountOwed) {
            this.email = email;
            this.amountOwed = amountOwed;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public BigDecimal getAmountOwed() {
            return amountOwed;
        }
        
        public void setAmountOwed(BigDecimal amountOwed) {
            this.amountOwed = amountOwed;
        }
    }
    
    public Long getPaidByUserId() {
        return paidByUserId;
    }
    
    public void setPaidByUserId(Long paidByUserId) {
        this.paidByUserId = paidByUserId;
    }
    
    public String getPaidByPendingMemberEmail() {
        return paidByPendingMemberEmail;
    }
    
    public void setPaidByPendingMemberEmail(String paidByPendingMemberEmail) {
        this.paidByPendingMemberEmail = paidByPendingMemberEmail;
    }
    
    public List<PendingShareRequest> getPendingShares() {
        return pendingShares;
    }
    
    public void setPendingShares(List<PendingShareRequest> pendingShares) {
        this.pendingShares = pendingShares;
    }
}

