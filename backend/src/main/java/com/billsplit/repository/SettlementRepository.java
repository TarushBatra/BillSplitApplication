package com.billsplit.repository;

import com.billsplit.entity.Group;
import com.billsplit.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByGroupOrderBySettledAtDesc(Group group);
}
