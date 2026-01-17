package com.billsplit.repository;

import com.billsplit.entity.Expense;
import com.billsplit.entity.Group;
import com.billsplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    List<Expense> findByGroup(Group group);
    
    List<Expense> findByPaidBy(User user);
    
    @Query("SELECT e FROM Expense e WHERE e.group = :group ORDER BY e.createdAt DESC")
    List<Expense> findByGroupOrderByCreatedAtDesc(@Param("group") Group group);
}

