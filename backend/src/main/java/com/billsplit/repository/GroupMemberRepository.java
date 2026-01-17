package com.billsplit.repository;

import com.billsplit.entity.Group;
import com.billsplit.entity.GroupMember;
import com.billsplit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    
    Optional<GroupMember> findByGroupAndUser(Group group, User user);
    
    List<GroupMember> findByGroup(Group group);
    
    List<GroupMember> findByUser(User user);
    
    boolean existsByGroupAndUser(Group group, User user);
    
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group = :group AND gm.role = 'ADMIN'")
    List<GroupMember> findAdminsByGroup(@Param("group") Group group);
    
    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.user WHERE gm.group = :group")
    List<GroupMember> findByGroupWithUser(@Param("group") Group group);
}

