package com.billsplit.controller;

import com.billsplit.dto.SettleUpRequest;
import com.billsplit.dto.SettlementTransaction;
import com.billsplit.entity.Settlement;
import com.billsplit.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/settlements")
@Tag(name = "Settlements", description = "Settlement management APIs")
@PreAuthorize("hasRole('USER')")
public class SettlementController {
    
    @Autowired
    private SettlementService settlementService;
    
    @GetMapping("/group/{groupId}")
    @Operation(summary = "Calculate optimal settlements for a group")
    public ResponseEntity<List<SettlementTransaction>> calculateSettlements(@PathVariable Long groupId) {
        List<SettlementTransaction> transactions = settlementService.calculateSettlements(groupId);
        return ResponseEntity.ok(transactions);
    }
    
    @PostMapping("/group/{groupId}/process")
    @Operation(summary = "Process settlements and send notifications")
    public ResponseEntity<Void> processSettlements(@PathVariable Long groupId) {
        settlementService.processSettlements(groupId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/group/{groupId}/settle")
    @Operation(summary = "Record a settlement")
    public ResponseEntity<Settlement> recordSettlement(@PathVariable Long groupId, @Valid @RequestBody SettleUpRequest settleUpRequest) {
        Settlement settlement = settlementService.recordSettlement(groupId, settleUpRequest);
        return ResponseEntity.ok(settlement);
    }
    
    @GetMapping("/group/{groupId}/history")
    @Operation(summary = "Get settlement history for a group")
    public ResponseEntity<List<Settlement>> getSettlementHistory(@PathVariable Long groupId) {
        List<Settlement> settlements = settlementService.getSettlementHistory(groupId);
        return ResponseEntity.ok(settlements);
    }
    
    @DeleteMapping("/group/{groupId}/{settlementId}")
    @Operation(summary = "Delete a settlement (admin only)")
    public ResponseEntity<Void> deleteSettlement(@PathVariable Long groupId, @PathVariable Long settlementId) {
        settlementService.deleteSettlement(groupId, settlementId);
        return ResponseEntity.ok().build();
    }
}

