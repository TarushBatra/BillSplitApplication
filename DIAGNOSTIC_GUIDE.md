# 🔍 BillSplit Diagnostic Guide

## How to Report Issues

Since I cannot see runtime logs, please provide the following information:

### 1. What Feature Isn't Working?
- [ ] Expense creation
- [ ] Balance calculation
- [ ] Settlement calculation
- [ ] Expense deletion
- [ ] Pending members
- [ ] Other: _______________

### 2. Specific Values
Please provide exact values:
- **Expected**: What should the value be?
- **Actual**: What is the value showing?
- **Example**: "Balance shows $50.00 but should be $75.00"

### 3. Steps to Reproduce
Exact steps that cause the issue:
1. Step 1
2. Step 2
3. Step 3

### 4. Screenshots/Logs
- Browser console errors (F12 → Console)
- Backend logs (`.cursor/debug.log`)
- Network tab showing API responses

## Common Issues Checklist

### Issue: Balances are Wrong
- [ ] Check if expense shares sum to expense amount
- [ ] Check if deleted expenses are excluded
- [ ] Check if settlements are applied correctly
- [ ] Compare frontend balance with backend API response

### Issue: Settlement Calculations are Wrong
- [ ] Check if balances are correct first
- [ ] Verify settlement algorithm is using correct balances
- [ ] Check if settlements account for existing settlements

### Issue: Expense Shares Don't Sum Correctly
- [ ] Check rounding for equal splits
- [ ] Verify pending members are included correctly
- [ ] Check if custom splits sum to expense amount

## Quick Validation Tests

### Test 1: Simple Equal Split
1. Create group with 2 members
2. Add expense: $100, Equal split
3. **Check**: Each member should owe $50.00
4. **Check**: Total shares = $100.00

### Test 2: Rounding Test
1. Create group with 3 members
2. Add expense: $100, Equal split
3. **Check**: Shares should be $33.33, $33.33, $33.34
4. **Check**: Total = $100.00

### Test 3: Balance Test
1. You pay $100 expense (equal split, 2 people)
2. **Check**: Your balance = $100 - $50 = $50 (you are owed $50)
3. **Check**: Other person's balance = $0 - $50 = -$50 (they owe $50)

## Debug Information Location

- **Frontend Logs**: Browser Console (F12 → Console)
- **Backend Logs**: `.cursor/debug.log` in project root
- **API Responses**: Network tab (F12 → Network)
