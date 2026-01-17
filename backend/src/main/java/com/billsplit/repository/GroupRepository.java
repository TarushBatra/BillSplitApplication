package com.billsplit.repository;

import com.billsplit.entity.Group;
import com.billsplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    @Query("SELECT g FROM Group g JOIN g.members gm WHERE gm.user = :user")
    List<Group> findByUser(@Param("user") User user);
    
    @Query("SELECT g FROM Group g WHERE g.createdBy = :user")
    List<Group> findByCreatedBy(@Param("user") User user);
}

