# 🧪 BillSplit Feature Testing Guide

This guide helps you systematically test all features and identify any remaining issues.

## 📋 What to Test

### Critical Test Cases

1. **Equal Split Expenses**
   - Test with 2 members, $100 expense → Should be $50.00 each
   - Test with 3 members, $100 expense → Should be $33.33, $33.33, $33.34
   - Test with 2 members + 1 pending, $100 expense → Should sum to exactly $100.00
   - Verify all shares sum to the expense amount exactly

2. **Balance Calculations**
   - Add an expense where you pay
   - Add an expense where someone else pays
   - Verify your balance = (total you paid) - (total you owe)
   - Verify "All settled up!" appears when balance is effectively zero

3. **Settlement Calculations**
   - Create expenses that create debts
   - Go to "Settle up" tab
   - Verify settlement transactions are optimal (minimal number)
   - Verify settlement amounts are correct

4. **Settlement Recording**
   - Record a settlement
   - Verify balances update correctly
   - Verify settlement appears in history

5. **Expense Deletion**
   - Delete an expense
   - Verify balances update (deleted expense should not affect balances)

6. **Pending Members**
   - Invite someone without an account
   - Add expense with pending member
   - Verify pending member's balance is calculated correctly
   - Accept invitation and verify balances update

## 🐛 Common Issues to Check

1. **Rounding Errors**: Do all expense shares sum exactly to the expense amount?
2. **Balance Mismatches**: Do frontend and backend show the same balances?
3. **Settlement Calculations**: Are settlement transactions optimal and correct?
4. **Deleted Expenses**: Do deleted expenses still affect balances?
5. **Pending Members**: Are pending member balances calculated correctly?

## 📊 Debug Information

After testing, check:
- Browser console for any errors
- Backend logs (`.cursor/debug.log`) for calculation details
- Network tab for API responses

## ✅ Success Criteria

All features work correctly when:
- ✅ All expense shares sum exactly to expense amounts
- ✅ Balances are calculated correctly (paid - owed)
- ✅ Settlements are optimal and correct
- ✅ Deleted expenses don't affect balances
- ✅ Pending members are handled correctly
- ✅ "All settled up!" appears when appropriate
- ✅ Frontend and backend calculations match
