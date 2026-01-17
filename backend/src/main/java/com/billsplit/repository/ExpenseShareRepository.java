package com.billsplit.repository;

import com.billsplit.entity.Expense;
import com.billsplit.entity.ExpenseShare;
import com.billsplit.entity.Group;
import com.billsplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {
    
    List<ExpenseShare> findByExpense(Expense expense);
    
    List<ExpenseShare> findByUser(User user);
    
    Optional<ExpenseShare> findByExpenseAndUser(Expense expense, User user);
    
    @Query("SELECT es FROM ExpenseShare es JOIN es.expense e WHERE e.group = :group")
    List<ExpenseShare> findByGroup(@Param("group") Group group);
    
    @Query("SELECT es FROM ExpenseShare es JOIN es.expense e WHERE e.group = :group AND es.user = :user")
    List<ExpenseShare> findByGroupAndUser(@Param("group") Group group, @Param("user") User user);
    
    @Query("SELECT COALESCE(SUM(es.amountOwed), 0) FROM ExpenseShare es JOIN es.expense e WHERE e.group = :group AND es.user = :user")
    BigDecimal getTotalOwedByUserInGroup(@Param("group") Group group, @Param("user") User user);
    
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.group = :group AND e.paidBy = :user")
    BigDecimal getTotalPaidByUserInGroup(@Param("group") Group group, @Param("user") User user);
}

