# 🧪 BillSplit Feature Test Plan - Splitwise Comparison

This document outlines a comprehensive test plan to verify all features work correctly like Splitwise.

---

## 📋 Test Categories

### 1. Authentication & User Management
- [ ] User Registration
- [ ] User Login
- [ ] User Profile Update
- [ ] Password Change
- [ ] JWT Token Refresh
- [ ] Logout

### 2. Group Management
- [ ] Create Group
- [ ] Edit Group (name, image)
- [ ] Delete Group (admin only)
- [ ] Invite Member (with account)
- [ ] Invite Member (without account)
- [ ] Accept Invitation
- [ ] Reject Invitation
- [ ] Remove Member (admin only)
- [ ] Remove Pending Member
- [ ] Leave Group
- [ ] View Group Members
- [ ] View Pending Members

### 3. Expense Management
- [ ] Add Expense - Equal Split
- [ ] Add Expense - Custom Split
- [ ] Add Expense with Pending Member
- [ ] Delete Expense
- [ ] View Expense History
- [ ] View Expense Details
- [ ] Expense with Different Payer

### 4. Balance Calculations
- [ ] View Balances Tab
- [ ] View "Who Owes Whom"
- [ ] "All Settled Up" Status (when balance = 0)
- [ ] Balance Updates After Expense
- [ ] Balance Updates After Settlement
- [ ] Balance Updates After Expense Deletion
- [ ] Floating Point Precision (e.g., $0.01 should show "All settled up")

### 5. Settlement System
- [ ] Calculate Optimal Settlements
- [ ] View Settlement Transactions
- [ ] Record Settlement (with message/image)
- [ ] View Settlement History
- [ ] Delete Settlement (admin only)
- [ ] Settlement Updates Balances
- [ ] Settlement Algorithm Correctness

### 6. Email Notifications
- [ ] Group Invitation Email
- [ ] Settlement Notification Email
- [ ] Invitation Rejection Email

### 7. UI/UX
- [ ] Navigation Between Pages
- [ ] Modal Open/Close
- [ ] Toast Notifications
- [ ] Loading States
- [ ] Error Messages
- [ ] Responsive Design

---

## 🐛 Potential Issues to Test

### Critical Issues:
1. **Balance Calculation Bug**: Floating point precision (e.g., $0.01 showing as "You owe $0.01" instead of "All settled up")
2. **Settlement Algorithm Bug**: Debtor balance update might be incorrect
3. **Expense Deletion**: Might not properly update balances
4. **Invitation Flow**: Pending members might not work correctly
5. **UI Refresh**: Data might not update after operations

### Edge Cases:
- Empty groups
- Single member groups
- Expenses with 0 amount
- Settlements with 0 amount
- Multiple expenses in quick succession
- Concurrent operations

---

## ✅ Test Execution

Follow the reproduction steps to test all features systematically.
