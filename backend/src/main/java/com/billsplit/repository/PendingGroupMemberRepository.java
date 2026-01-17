package com.billsplit.repository;

import com.billsplit.entity.Group;
import com.billsplit.entity.PendingGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PendingGroupMemberRepository extends JpaRepository<PendingGroupMember, Long> {
    List<PendingGroupMember> findByGroup(Group group);
    Optional<PendingGroupMember> findByGroupAndEmail(Group group, String email);
    boolean existsByGroupAndEmail(Group group, String email);
    List<PendingGroupMember> findByEmail(String email);
}
