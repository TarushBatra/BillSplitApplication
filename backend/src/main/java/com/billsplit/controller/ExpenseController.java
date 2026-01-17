package com.billsplit.controller;

import com.billsplit.dto.ExpenseRequest;
import com.billsplit.entity.Expense;
import com.billsplit.entity.ExpenseShare;
import com.billsplit.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenses")
@Tag(name = "Expenses", description = "Expense management APIs")
@PreAuthorize("hasRole('USER')")
public class ExpenseController {
    
    @Autowired
    private ExpenseService expenseService;
    
    @PostMapping
    @Operation(summary = "Create a new expense")
    public ResponseEntity<Expense> createExpense(@Valid @RequestBody ExpenseRequest expenseRequest) {
        Expense expense = expenseService.createExpense(expenseRequest);
        return ResponseEntity.ok(expense);
    }
    
    @GetMapping("/group/{groupId}")
    @Operation(summary = "Get expenses for a group")
    public ResponseEntity<List<Expense>> getGroupExpenses(@PathVariable Long groupId) {
        List<Expense> expenses = expenseService.getGroupExpenses(groupId);
        return ResponseEntity.ok(expenses);
    }
    
    @GetMapping("/{expenseId}")
    @Operation(summary = "Get expense by ID")
    public ResponseEntity<Expense> getExpense(@PathVariable Long expenseId) {
        Expense expense = expenseService.getExpenseById(expenseId);
        return ResponseEntity.ok(expense);
    }
    
    @GetMapping("/{expenseId}/shares")
    @Operation(summary = "Get expense shares")
    public ResponseEntity<List<ExpenseShare>> getExpenseShares(@PathVariable Long expenseId) {
        List<ExpenseShare> shares = expenseService.getExpenseShares(expenseId);
        return ResponseEntity.ok(shares);
    }
    
    @DeleteMapping("/{expenseId}")
    @Operation(summary = "Delete expense (soft delete)")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{expenseId}/permanent")
    @Operation(summary = "Permanently delete expense (group creator only)")
    public ResponseEntity<Void> permanentlyDeleteExpense(@PathVariable Long expenseId) {
        expenseService.permanentlyDeleteExpense(expenseId);
        return ResponseEntity.ok().build();
    }
}

