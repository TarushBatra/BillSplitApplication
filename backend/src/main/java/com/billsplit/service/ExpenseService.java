package com.billsplit.service;

import com.billsplit.dto.ExpenseRequest;
import com.billsplit.entity.*;
import com.billsplit.repository.ExpenseRepository;
import com.billsplit.repository.ExpenseShareRepository;
import com.billsplit.repository.GroupMemberRepository;
import com.billsplit.repository.PendingGroupMemberRepository;
import com.billsplit.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ExpenseService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    @Autowired
    private ExpenseShareRepository expenseShareRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private PendingGroupMemberRepository pendingGroupMemberRepository;
    
    @Autowired
    private EmailService emailService;
    
    public Expense createExpense(ExpenseRequest expenseRequest) {
        User currentUser = authService.getCurrentUser();
        Group group = groupMemberRepository.findByUser(currentUser).stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(expenseRequest.getGroupId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setDescription(expenseRequest.getDescription());
        expense.setAmount(expenseRequest.getAmount());
        expense.setSplitType(expenseRequest.getSplitType());
        
        // Determine who paid
        User paidByUser = currentUser;
        String pendingPaidByNote = null;
        
        if (expenseRequest.getPaidByUserId() != null) {
            // Check if the user is a member of the group
            GroupMember payerMember = groupMemberRepository.findByGroupAndUser(group, 
                    userRepository.findById(expenseRequest.getPaidByUserId())
                            .orElseThrow(() -> new RuntimeException("Payer user not found")))
                    .orElseThrow(() -> new RuntimeException("Payer is not a member of this group"));
            paidByUser = payerMember.getUser();
        } else if (expenseRequest.getPaidByPendingMemberEmail() != null) {
            // Pending member paid - use current user as proxy but note it in description
            String normalizedEmail = expenseRequest.getPaidByPendingMemberEmail().trim().toLowerCase();
            PendingGroupMember pendingPayer = pendingGroupMemberRepository.findByGroupAndEmail(group, normalizedEmail)
                    .orElseThrow(() -> new RuntimeException("Pending member not found"));
            paidByUser = currentUser; // Use current user as proxy
            pendingPaidByNote = " (Paid by: " + (pendingPayer.getName() != null ? pendingPayer.getName() : pendingPayer.getEmail()) + " - Pending)";
        }
        
        expense.setPaidBy(paidByUser);
        
        // Append pending payer note to description if applicable
        if (pendingPaidByNote != null) {
            expense.setDescription(expenseRequest.getDescription() + pendingPaidByNote);
        }
        
        Expense savedExpense = expenseRepository.save(expense);
        
        // Create expense shares
        if (expenseRequest.getSplitType() == Expense.SplitType.EQUAL) {
            createEqualShares(savedExpense, group, expenseRequest.getPaidByPendingMemberEmail() != null);
        } else {
            createCustomShares(savedExpense, expenseRequest.getShares(), expenseRequest.getPendingShares());
        }
        
        // Send expense notification emails to all group members (except the payer)
        try {
            List<GroupMember> allMembers = groupMemberRepository.findByGroupWithUser(group);
            for (GroupMember member : allMembers) {
                if (member.getUser() != null && !member.getUser().getId().equals(paidByUser.getId())) {
                    emailService.sendExpenseNotification(
                            member.getUser().getEmail(),
                            group.getName(),
                            expenseRequest.getDescription(),
                            expenseRequest.getAmount().toString()
                    );
                }
            }
        } catch (Exception e) {
            // Log error but don't fail expense creation if email fails
            logger.error("Failed to send expense notification emails: {}", e.getMessage(), e);
        }
        
        return savedExpense;
    }
    
    private void createEqualShares(Expense expense, Group group, boolean paidByPendingMember) {
        List<GroupMember> members = groupMemberRepository.findByGroup(group);
        List<PendingGroupMember> pendingMembers = pendingGroupMemberRepository.findByGroup(group);
        
        int totalParticipants = members.size() + pendingMembers.size();
        if (totalParticipants == 0) {
            throw new RuntimeException("Cannot split expense with no participants");
        }
        
        // Calculate base amount per person (with more precision)
        BigDecimal amountPerPerson = expense.getAmount().divide(
                BigDecimal.valueOf(totalParticipants), 4, RoundingMode.HALF_UP);
        
        // Round to 2 decimal places for storage
        BigDecimal roundedAmountPerPerson = amountPerPerson.setScale(2, RoundingMode.HALF_UP);
        
        // Calculate total if all participants pay the rounded amount
        BigDecimal totalRoundedAll = roundedAmountPerPerson.multiply(BigDecimal.valueOf(totalParticipants));
        
        // Calculate rounding error (difference between expense amount and rounded total)
        BigDecimal roundingError = expense.getAmount().subtract(totalRoundedAll);
        
        // Distribute rounding error: give it to the last participant (member if members exist, otherwise last pending)
        // First, calculate what members should pay (rounded amount, with rounding error if they're the last group)
        BigDecimal totalForMembers = roundedAmountPerPerson.multiply(BigDecimal.valueOf(members.size()));
        BigDecimal totalForPending = roundedAmountPerPerson.multiply(BigDecimal.valueOf(pendingMembers.size()));
        
        // Distribute rounding error to the last group (members if they exist, otherwise pending)
        if (members.size() > 0) {
            // Members get the rounding error (distributed to last member)
            totalForMembers = totalForMembers.add(roundingError);
        } else if (pendingMembers.size() > 0) {
            // Pending members get the rounding error (distributed to last pending)
            totalForPending = totalForPending.add(roundingError);
        }
        
        // Create shares for all actual members
        int memberIndex = 0;
        BigDecimal remainingForMembers = totalForMembers;
        BigDecimal membersTotal = BigDecimal.ZERO;
        for (GroupMember member : members) {
            BigDecimal shareAmount;
            
            if (memberIndex == members.size() - 1) {
                // Last member gets the remainder to ensure exact total
                shareAmount = remainingForMembers;
            } else {
                shareAmount = roundedAmountPerPerson;
                remainingForMembers = remainingForMembers.subtract(shareAmount);
            }
            
            membersTotal = membersTotal.add(shareAmount);
            ExpenseShare share = new ExpenseShare(expense, member.getUser(), shareAmount);
            expenseShareRepository.save(share);
            memberIndex++;
        }
        
        // Store pending members' shares in description for frontend calculation
        BigDecimal pendingTotal = BigDecimal.ZERO;
        if (!pendingMembers.isEmpty()) {
            StringBuilder pendingSharesStr = new StringBuilder();
            if (expense.getDescription().contains("(Paid by:")) {
                pendingSharesStr.append(" (Pending shares: ");
            } else {
                pendingSharesStr.append(" (Pending shares: ");
            }
            
            int pendingIndex = 0;
            BigDecimal remainingForPending = totalForPending;
            for (PendingGroupMember pending : pendingMembers) {
                if (pendingIndex > 0) pendingSharesStr.append(", ");
                
                BigDecimal pendingShareAmount;
                if (pendingIndex == pendingMembers.size() - 1) {
                    // Last pending member gets the remainder to ensure exact total
                    pendingShareAmount = remainingForPending;
                } else {
                    // Use the same rounded amount per person
                    pendingShareAmount = roundedAmountPerPerson;
                    remainingForPending = remainingForPending.subtract(pendingShareAmount);
                }
                
                pendingTotal = pendingTotal.add(pendingShareAmount);
                pendingSharesStr.append(pending.getEmail())
                        .append(":")
                        .append(pendingShareAmount.setScale(2, RoundingMode.HALF_UP));
                pendingIndex++;
            }
            pendingSharesStr.append(")");
            
            expense.setDescription(expense.getDescription() + pendingSharesStr.toString());
            expenseRepository.save(expense);
        }
        
        // Validate: membersTotal + pendingTotal should equal expense amount
        BigDecimal totalShares = membersTotal.add(pendingTotal);
        BigDecimal difference = expense.getAmount().subtract(totalShares).abs();
        if (difference.compareTo(new BigDecimal("0.01")) > 0) {
            logger.error("VALIDATION ERROR: Expense {} shares don't sum correctly. Expected: {}, Got: {}, Difference: {}", 
                expense.getId(), expense.getAmount(), totalShares, difference);
        }
    }
    
    private void createCustomShares(Expense expense, List<ExpenseRequest.ExpenseShareRequest> shareRequests, 
                                   List<ExpenseRequest.PendingShareRequest> pendingShareRequests) {
        if ((shareRequests == null || shareRequests.isEmpty()) && 
            (pendingShareRequests == null || pendingShareRequests.isEmpty())) {
            throw new RuntimeException("Custom shares must be provided for custom split type");
        }
        
        BigDecimal totalActualShares = shareRequests != null ? shareRequests.stream()
                .map(ExpenseRequest.ExpenseShareRequest::getAmountOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        
        BigDecimal totalPendingShares = pendingShareRequests != null ? pendingShareRequests.stream()
                .map(ExpenseRequest.PendingShareRequest::getAmountOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        
        BigDecimal totalShares = totalActualShares.add(totalPendingShares);
        
        if (totalShares.compareTo(expense.getAmount()) != 0) {
            throw new RuntimeException("Sum of custom shares must equal the expense amount");
        }
        
        // Create shares for actual members
        if (shareRequests != null) {
        for (ExpenseRequest.ExpenseShareRequest shareRequest : shareRequests) {
            User user = userRepository.findById(shareRequest.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
                // Include payer's share in custom split
                ExpenseShare share = new ExpenseShare(expense, user, shareRequest.getAmountOwed());
                expenseShareRepository.save(share);
            }
        }
        
        // Store pending member shares in description for frontend
        if (pendingShareRequests != null && !pendingShareRequests.isEmpty()) {
            StringBuilder pendingSharesStr = new StringBuilder();
            if (expense.getDescription().contains("(Paid by:")) {
                pendingSharesStr.append(" (Pending shares: ");
            } else {
                pendingSharesStr.append(" (Pending shares: ");
            }
            
            for (int i = 0; i < pendingShareRequests.size(); i++) {
                if (i > 0) pendingSharesStr.append(", ");
                pendingSharesStr.append(pendingShareRequests.get(i).getEmail())
                        .append(":")
                        .append(pendingShareRequests.get(i).getAmountOwed());
            }
            pendingSharesStr.append(")");
            
            expense.setDescription(expense.getDescription() + pendingSharesStr.toString());
            expenseRepository.save(expense);
        }
    }
    
    public List<Expense> getGroupExpenses(Long groupId) {
        User currentUser = authService.getCurrentUser();
        Group group = groupMemberRepository.findByUser(currentUser).stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        List<Expense> expenses = expenseRepository.findByGroupOrderByCreatedAtDesc(group);
        // Eagerly load paidBy and deletedBy users to ensure they're serialized
        expenses.forEach(expense -> {
            if (expense.getPaidBy() != null) {
                expense.getPaidBy().getName(); // Trigger lazy loading
            }
            if (expense.getDeletedBy() != null) {
                expense.getDeletedBy().getName(); // Trigger lazy loading
            }
        });
        return expenses;
    }
    
    public Expense getExpenseById(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
    }
    
    public void deleteExpense(Long expenseId) {
        User currentUser = authService.getCurrentUser();
        Expense expense = getExpenseById(expenseId);
        
        // Check if already deleted
        if (expense.getDeletedAt() != null) {
            throw new RuntimeException("Expense is already deleted");
        }
        
        // Check if user is the one who paid or is a group admin
        boolean isPaidBy = expense.getPaidBy().getId().equals(currentUser.getId());
        boolean isAdmin = groupMemberRepository.findByGroupAndUser(expense.getGroup(), currentUser)
                .map(member -> member.getRole() == GroupMember.GroupRole.ADMIN)
                .orElse(false);
        
        if (!isPaidBy && !isAdmin) {
            throw new RuntimeException("Only the person who paid or group admin can delete expense");
        }
        
        // Soft delete
        expense.setDeletedAt(LocalDateTime.now());
        expense.setDeletedBy(currentUser);
        expenseRepository.save(expense);
    }
    
    public void permanentlyDeleteExpense(Long expenseId) {
        User currentUser = authService.getCurrentUser();
        Expense expense = getExpenseById(expenseId);
        
        // Check if expense is soft deleted
        if (expense.getDeletedAt() == null) {
            throw new RuntimeException("Expense must be soft deleted before permanent deletion");
        }
        
        // Check if current user is the group creator
        boolean isGroupCreator = expense.getGroup().getCreatedBy().getId().equals(currentUser.getId());
        if (!isGroupCreator) {
            throw new RuntimeException("Only the group creator can permanently delete expenses");
        }
        
        // Hard delete
        expenseRepository.delete(expense);
    }
    
    public List<ExpenseShare> getExpenseShares(Long expenseId) {
        Expense expense = getExpenseById(expenseId);
        return expenseShareRepository.findByExpense(expense);
    }
}

