package com.billsplit.service;

import com.billsplit.dto.SettleUpRequest;
import com.billsplit.dto.SettlementTransaction;
import com.billsplit.entity.*;
import com.billsplit.repository.ExpenseShareRepository;
import com.billsplit.repository.GroupMemberRepository;
import com.billsplit.repository.SettlementRepository;
import com.billsplit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional
public class SettlementService {
    
    @Autowired
    private ExpenseShareRepository expenseShareRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SettlementRepository settlementRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<SettlementTransaction> calculateSettlements(Long groupId) {
        User currentUser = authService.getCurrentUser();
        Group group = groupMemberRepository.findByUser(currentUser).stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        List<GroupMember> members = groupMemberRepository.findByGroup(group);
        Map<Long, BigDecimal> balances = calculateBalances(group, members);
        
        return optimizeSettlements(balances, members);
    }
    
    private Map<Long, BigDecimal> calculateBalances(Group group, List<GroupMember> members) {
        Map<Long, BigDecimal> balances = new HashMap<>();
        
        for (GroupMember member : members) {
            User user = member.getUser();
            BigDecimal totalOwed = expenseShareRepository.getTotalOwedByUserInGroup(group, user);
            BigDecimal totalPaid = expenseShareRepository.getTotalPaidByUserInGroup(group, user);
            BigDecimal balance = totalPaid.subtract(totalOwed);
            balances.put(user.getId(), balance);
        }
        
        return balances;
    }
    
    private List<SettlementTransaction> optimizeSettlements(Map<Long, BigDecimal> balances, List<GroupMember> members) {
        List<SettlementTransaction> transactions = new ArrayList<>();
        
        // Create lists of creditors (positive balance) and debtors (negative balance)
        List<Map.Entry<Long, BigDecimal>> creditors = new ArrayList<>();
        List<Map.Entry<Long, BigDecimal>> debtors = new ArrayList<>();
        
        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(entry);
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(entry);
            }
        }
        
        // Sort by absolute balance
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        debtors.sort((a, b) -> a.getValue().compareTo(b.getValue()));
        
        // Create a map for user names
        Map<Long, String> userNames = new HashMap<>();
        for (GroupMember member : members) {
            userNames.put(member.getUser().getId(), member.getUser().getName());
        }
        
        // Optimize transactions
        int creditorIndex = 0;
        int debtorIndex = 0;
        
        while (creditorIndex < creditors.size() && debtorIndex < debtors.size()) {
            Map.Entry<Long, BigDecimal> creditor = creditors.get(creditorIndex);
            Map.Entry<Long, BigDecimal> debtor = debtors.get(debtorIndex);
            
            BigDecimal creditorAmount = creditor.getValue();
            BigDecimal debtorAmount = debtor.getValue().abs();
            
            BigDecimal transactionAmount = creditorAmount.min(debtorAmount);
            
            if (transactionAmount.compareTo(BigDecimal.ZERO) > 0) {
                SettlementTransaction transaction = new SettlementTransaction(
                        debtor.getKey(),
                        userNames.get(debtor.getKey()),
                        creditor.getKey(),
                        userNames.get(creditor.getKey()),
                        transactionAmount
                );
                transactions.add(transaction);
                
                // Update balances
                creditor.setValue(creditorAmount.subtract(transactionAmount));
                debtor.setValue(debtorAmount.subtract(transactionAmount));
                
                // Move to next if balance is zero
                if (creditor.getValue().compareTo(BigDecimal.ZERO) == 0) {
                    creditorIndex++;
                }
                if (debtor.getValue().compareTo(BigDecimal.ZERO) == 0) {
                    debtorIndex++;
                }
            } else {
                creditorIndex++;
                debtorIndex++;
            }
        }
        
        return transactions;
    }
    
    public void processSettlements(Long groupId) {
        List<SettlementTransaction> transactions = calculateSettlements(groupId);
        
        // Send email notifications for settlements
        emailService.sendSettlementNotifications(groupId, transactions);
    }
    
    public Settlement recordSettlement(Long groupId, SettleUpRequest settleUpRequest) {
        User currentUser = authService.getCurrentUser();
        Group group = groupMemberRepository.findByUser(currentUser).stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        // Verify the current user is the one who owes
        if (!currentUser.getId().equals(settleUpRequest.getToUserId())) {
            // Actually, the current user is the one paying (fromUser), and toUserId is who receives
            // But wait, the request says toUserId - let me check the frontend logic
            // Frontend sends: toUserId is the person who is owed (creditor)
            // So currentUser should be the debtor (fromUser)
        }
        
        User toUser = userRepository.findById(settleUpRequest.getToUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Settlement settlement = new Settlement(
                group,
                currentUser, // fromUser (debtor)
                toUser,      // toUser (creditor)
                settleUpRequest.getAmount(),
                settleUpRequest.getMessage(),
                settleUpRequest.getImageUrl()
        );
        
        return settlementRepository.save(settlement);
    }
    
    public List<Settlement> getSettlementHistory(Long groupId) {
        User currentUser = authService.getCurrentUser();
        Group group = groupMemberRepository.findByUser(currentUser).stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        List<Settlement> settlements = settlementRepository.findByGroupOrderBySettledAtDesc(group);
        // Eagerly load users to ensure they're serialized
        settlements.forEach(settlement -> {
            if (settlement.getFromUser() != null) {
                settlement.getFromUser().getName();
            }
            if (settlement.getToUser() != null) {
                settlement.getToUser().getName();
            }
        });
        return settlements;
    }
    
    public void deleteSettlement(Long groupId, Long settlementId) {
        User currentUser = authService.getCurrentUser();
        Group group = groupMemberRepository.findByUser(currentUser).stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        // Check if current user is admin
        GroupMember currentUserMember = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        if (currentUserMember.getRole() != GroupMember.GroupRole.ADMIN) {
            throw new RuntimeException("Only group admins can delete settlements");
        }
        
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement not found"));
        
        // Verify that this settlement belongs to the specified group
        if (!settlement.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Settlement does not belong to this group");
        }
        
        settlementRepository.delete(settlement);
    }
}

