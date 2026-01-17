package com.billsplit.controller;

import com.billsplit.dto.GroupInvitationDTO;
import com.billsplit.dto.GroupRequest;
import com.billsplit.dto.UpdateGroupRequest;
import com.billsplit.entity.Group;
import com.billsplit.entity.GroupMember;
import com.billsplit.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
@Tag(name = "Groups", description = "Group management APIs")
@PreAuthorize("hasRole('USER')")
public class GroupController {
    
    @Autowired
    private GroupService groupService;
    
    @PostMapping
    @Operation(summary = "Create a new group")
    public ResponseEntity<Group> createGroup(@Valid @RequestBody GroupRequest groupRequest) {
        Group group = groupService.createGroup(groupRequest);
        return ResponseEntity.ok(group);
    }
    
    @GetMapping
    @Operation(summary = "Get user's groups")
    public ResponseEntity<List<Group>> getUserGroups() {
        List<Group> groups = groupService.getUserGroups();
        return ResponseEntity.ok(groups);
    }
    
    @GetMapping("/{groupId}")
    @Operation(summary = "Get group by ID")
    public ResponseEntity<Group> getGroup(@PathVariable Long groupId) {
        Group group = groupService.getGroupById(groupId);
        return ResponseEntity.ok(group);
    }
    
    @GetMapping("/{groupId}/members")
    @Operation(summary = "Get group members")
    public ResponseEntity<List<GroupMember>> getGroupMembers(@PathVariable Long groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }
    
    @PostMapping("/{groupId}/members")
    @Operation(summary = "Add member to group")
    public ResponseEntity<Void> addMember(@PathVariable Long groupId, @RequestParam String email) {
        groupService.addMemberToGroup(groupId, email);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{groupId}/members/{userId}")
    @Operation(summary = "Remove member from group")
    public ResponseEntity<Void> removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMemberFromGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{groupId}/pending-members/{pendingMemberId}")
    @Operation(summary = "Remove pending member from group")
    public ResponseEntity<Void> removePendingMember(@PathVariable Long groupId, @PathVariable Long pendingMemberId) {
        groupService.removePendingMemberFromGroup(groupId, pendingMemberId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{groupId}/leave")
    @Operation(summary = "Leave group")
    public ResponseEntity<Void> leaveGroup(@PathVariable Long groupId) {
        groupService.leaveGroup(groupId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{groupId}")
    @Operation(summary = "Update group")
    public ResponseEntity<Group> updateGroup(@PathVariable Long groupId, @Valid @RequestBody UpdateGroupRequest updateRequest) {
        Group group = groupService.updateGroup(groupId, updateRequest);
        return ResponseEntity.ok(group);
    }
    
    @GetMapping("/{groupId}/pending-members")
    @Operation(summary = "Get pending group members")
    public ResponseEntity<List<com.billsplit.entity.PendingGroupMember>> getPendingMembers(@PathVariable Long groupId) {
        List<com.billsplit.entity.PendingGroupMember> pendingMembers = groupService.getPendingMembers(groupId);
        return ResponseEntity.ok(pendingMembers);
    }
    
    @GetMapping("/invitations")
    @Operation(summary = "Get pending invitations for current user")
    public ResponseEntity<List<GroupInvitationDTO>> getPendingInvitations() {
        List<GroupInvitationDTO> invitations = groupService.getPendingInvitations();
        return ResponseEntity.ok(invitations);
    }
    
    @PostMapping("/invitations/{invitationId}/accept")
    @Operation(summary = "Accept a group invitation")
    public ResponseEntity<Void> acceptInvitation(@PathVariable Long invitationId) {
        groupService.acceptInvitation(invitationId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/invitations/{invitationId}/reject")
    @Operation(summary = "Reject a group invitation")
    public ResponseEntity<Void> rejectInvitation(@PathVariable Long invitationId) {
        groupService.rejectInvitation(invitationId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{groupId}")
    @Operation(summary = "Delete a group (admin only)")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {
        groupService.deleteGroup(groupId);
        return ResponseEntity.ok().build();
    }
}

