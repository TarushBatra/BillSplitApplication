package com.billsplit.service;

import com.billsplit.dto.GroupInvitationDTO;
import com.billsplit.dto.GroupRequest;
import com.billsplit.entity.Group;
import com.billsplit.entity.GroupMember;
import com.billsplit.entity.PendingGroupMember;
import com.billsplit.entity.User;
import com.billsplit.repository.GroupMemberRepository;
import com.billsplit.repository.GroupRepository;
import com.billsplit.repository.PendingGroupMemberRepository;
import com.billsplit.repository.UserRepository;
import com.billsplit.util.EmailNameExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupService {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    
    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PendingGroupMemberRepository pendingGroupMemberRepository;
    
    public Group createGroup(GroupRequest groupRequest) {
        User currentUser = authService.getCurrentUser();
        
        Group group = new Group();
        group.setName(groupRequest.getName());
        group.setCreatedBy(currentUser);
        
        Group savedGroup = groupRepository.save(group);
        
        // Add creator as admin
        GroupMember adminMember = new GroupMember(savedGroup, currentUser, GroupMember.GroupRole.ADMIN);
        groupMemberRepository.save(adminMember);
        
        // Add other members if provided
        if (groupRequest.getMemberEmails() != null && !groupRequest.getMemberEmails().isEmpty()) {
            for (String email : groupRequest.getMemberEmails()) {
                if (!email.equals(currentUser.getEmail())) {
                    String normalizedEmail = email.trim().toLowerCase();
                    
                    // Check if already a pending member
                    if (pendingGroupMemberRepository.existsByGroupAndEmail(savedGroup, normalizedEmail)) {
                        continue;
                    }
                    
                    Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);
                    if (existingUser.isPresent()) {
                        // User exists - create pending invitation (they need to accept)
                        PendingGroupMember pendingMember = new PendingGroupMember(
                                savedGroup, normalizedEmail, existingUser.get().getName(), existingUser.get(), currentUser);
                        pendingGroupMemberRepository.save(pendingMember);
                        emailService.sendGroupInvitation(normalizedEmail, savedGroup.getName(), currentUser.getName());
                    } else {
                        // User doesn't exist - create pending member without user reference
                        String extractedName = EmailNameExtractor.extractNameFromEmail(normalizedEmail);
                        PendingGroupMember pendingMember = new PendingGroupMember(
                                savedGroup, normalizedEmail, extractedName, currentUser);
                        pendingGroupMemberRepository.save(pendingMember);
                        emailService.sendGroupInvitation(normalizedEmail, savedGroup.getName(), currentUser.getName());
                    }
                }
            }
        }
        
        return savedGroup;
    }
    
    public List<Group> getUserGroups() {
        User currentUser = authService.getCurrentUser();
        List<Group> groups = groupRepository.findByUser(currentUser);
        // Eagerly load createdBy to ensure it's serialized
        groups.forEach(group -> {
            if (group.getCreatedBy() != null) {
                group.getCreatedBy().getName(); // Trigger lazy loading
            }
        });
        return groups;
    }
    
    public Group getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }
    
    public List<GroupMember> getGroupMembers(Long groupId) {
        Group group = getGroupById(groupId);
        return groupMemberRepository.findByGroup(group);
    }
    
    public void addMemberToGroup(Long groupId, String email) {
        Group group = getGroupById(groupId);
        User currentUser = authService.getCurrentUser();
        
        // Check if current user is admin
        GroupMember currentUserMember = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        if (currentUserMember.getRole() != GroupMember.GroupRole.ADMIN) {
            throw new RuntimeException("Only group admins can add members");
        }
        
        // Normalize email to lowercase for case-insensitive lookup
        String normalizedEmail = email.trim().toLowerCase();
        
        // Check if email is already a pending member
        if (pendingGroupMemberRepository.existsByGroupAndEmail(group, normalizedEmail)) {
            throw new RuntimeException("This email has already been invited to the group");
        }
        
        // Check if user exists (has account)
        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);
        
        if (existingUser.isPresent()) {
            // Check if user is already a member
            if (groupMemberRepository.existsByGroupAndUser(group, existingUser.get())) {
                throw new RuntimeException("User is already a member of this group");
            }
            // User exists but not a member - create pending invitation (they need to accept)
            PendingGroupMember pendingMember = new PendingGroupMember(
                    group, normalizedEmail, existingUser.get().getName(), existingUser.get(), currentUser);
            pendingGroupMemberRepository.save(pendingMember);
            
            // Send invitation email to existing user
            emailService.sendGroupInvitation(normalizedEmail, group.getName(), currentUser.getName());
        } else {
            // User doesn't exist - create pending member without user reference
            String extractedName = EmailNameExtractor.extractNameFromEmail(normalizedEmail);
            PendingGroupMember pendingMember = new PendingGroupMember(
                    group, normalizedEmail, extractedName, currentUser);
            pendingGroupMemberRepository.save(pendingMember);
            
            // Send invitation email to non-existing user
            emailService.sendGroupInvitation(normalizedEmail, group.getName(), currentUser.getName());
        }
    }
    
    public List<PendingGroupMember> getPendingMembers(Long groupId) {
        Group group = getGroupById(groupId);
        List<PendingGroupMember> pendingMembers = pendingGroupMemberRepository.findByGroup(group);
        // Eagerly load invitedBy and user to ensure they're serialized
        pendingMembers.forEach(pending -> {
            if (pending.getInvitedBy() != null) {
                pending.getInvitedBy().getName(); // Trigger lazy loading
            }
            if (pending.getUser() != null) {
                pending.getUser().getName(); // Trigger lazy loading
                pending.getUser().getEmail(); // Trigger lazy loading
            }
        });
        return pendingMembers;
    }
    
    public void removeMemberFromGroup(Long groupId, Long userId) {
        Group group = getGroupById(groupId);
        User currentUser = authService.getCurrentUser();
        User userToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if current user is admin
        GroupMember currentUserMember = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        if (currentUserMember.getRole() != GroupMember.GroupRole.ADMIN) {
            throw new RuntimeException("Only group admins can remove members");
        }
        
        GroupMember memberToRemove = groupMemberRepository.findByGroupAndUser(group, userToRemove)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        groupMemberRepository.delete(memberToRemove);
    }
    
    public void removePendingMemberFromGroup(Long groupId, Long pendingMemberId) {
        Group group = getGroupById(groupId);
        User currentUser = authService.getCurrentUser();
        
        // Check if current user is admin
        GroupMember currentUserMember = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        if (currentUserMember.getRole() != GroupMember.GroupRole.ADMIN) {
            throw new RuntimeException("Only group admins can remove pending members");
        }
        
        PendingGroupMember pendingMember = pendingGroupMemberRepository.findById(pendingMemberId)
                .orElseThrow(() -> new RuntimeException("Pending member not found"));
        
        // Verify that this pending member belongs to the specified group
        if (!pendingMember.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Pending member does not belong to this group");
        }
        
        pendingGroupMemberRepository.delete(pendingMember);
    }
    
    public void leaveGroup(Long groupId) {
        Group group = getGroupById(groupId);
        User currentUser = authService.getCurrentUser();
        
        GroupMember member = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        // Check if user is the only admin
        if (member.getRole() == GroupMember.GroupRole.ADMIN) {
            List<GroupMember> admins = groupMemberRepository.findAdminsByGroup(group);
            if (admins.size() == 1) {
                throw new RuntimeException("Cannot leave group as the only admin. Transfer admin role first or delete the group.");
            }
        }
        
        groupMemberRepository.delete(member);
    }
    
    public Group updateGroup(Long groupId, com.billsplit.dto.UpdateGroupRequest updateRequest) {
        Group group = getGroupById(groupId);
        User currentUser = authService.getCurrentUser();
        
        // Check if current user is admin
        GroupMember currentUserMember = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        if (currentUserMember.getRole() != GroupMember.GroupRole.ADMIN) {
            throw new RuntimeException("Only group admins can update group");
        }
        
        if (updateRequest.getName() != null && !updateRequest.getName().trim().isEmpty()) {
            group.setName(updateRequest.getName().trim());
        }
        
        if (updateRequest.getImageUrl() != null) {
            group.setImageUrl(updateRequest.getImageUrl().trim().isEmpty() ? null : updateRequest.getImageUrl().trim());
        }
        
        return groupRepository.save(group);
    }
    
    public List<GroupInvitationDTO> getPendingInvitations() {
        User currentUser = authService.getCurrentUser();
        String normalizedEmail = currentUser.getEmail().trim().toLowerCase();
        
        List<PendingGroupMember> pendingInvitations = pendingGroupMemberRepository.findByEmail(normalizedEmail);
        
        return pendingInvitations.stream()
                .map(pending -> {
                    // Eagerly load group and inviter
                    Group group = pending.getGroup();
                    User inviter = pending.getInvitedBy();
                    if (group != null) {
                        group.getName(); // Trigger lazy loading
                    }
                    if (inviter != null) {
                        inviter.getName(); // Trigger lazy loading
                        inviter.getEmail(); // Trigger lazy loading
                    }
                    
                    return new GroupInvitationDTO(
                            pending.getId(),
                            group != null ? group.getId() : null,
                            group != null ? group.getName() : "Unknown Group",
                            inviter != null ? inviter.getName() : "Unknown",
                            inviter != null ? inviter.getEmail() : "Unknown",
                            pending.getInvitedAt()
                    );
                })
                .collect(Collectors.toList());
    }
    
    public void acceptInvitation(Long invitationId) {
        User currentUser = authService.getCurrentUser();
        String normalizedEmail = currentUser.getEmail().trim().toLowerCase();
        
        PendingGroupMember pendingMember = pendingGroupMemberRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));
        
        // Verify that this invitation belongs to the current user
        if (!pendingMember.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new RuntimeException("This invitation does not belong to you");
        }
        
        Group group = pendingMember.getGroup();
        
        // Check if user is already a member (edge case)
        if (groupMemberRepository.existsByGroupAndUser(group, currentUser)) {
            // User is already a member, just remove the pending invitation
            pendingGroupMemberRepository.delete(pendingMember);
            return;
        }
        
        // Add user as a member
        GroupMember member = new GroupMember(group, currentUser, GroupMember.GroupRole.MEMBER);
        groupMemberRepository.save(member);
        
        // Remove pending member entry
        pendingGroupMemberRepository.delete(pendingMember);
    }
    
    public void rejectInvitation(Long invitationId) {
        User currentUser = authService.getCurrentUser();
        String normalizedEmail = currentUser.getEmail().trim().toLowerCase();
        
        PendingGroupMember pendingMember = pendingGroupMemberRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));
        
        // Verify that this invitation belongs to the current user
        if (!pendingMember.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new RuntimeException("This invitation does not belong to you");
        }
        
        // Get inviter info before deleting
        User inviter = pendingMember.getInvitedBy();
        Group group = pendingMember.getGroup();
        String groupName = group != null ? group.getName() : "Unknown Group";
        String rejecterName = currentUser.getName();
        
        // Remove pending member entry
        pendingGroupMemberRepository.delete(pendingMember);
        
        // Send notification to inviter (optional - if email is configured)
        try {
            if (inviter != null && inviter.getEmail() != null) {
                emailService.sendInvitationRejectionNotification(
                        inviter.getEmail(),
                        groupName,
                        rejecterName
                );
            }
        } catch (Exception e) {
            // Log but don't fail if email sending fails
            logger.error("Failed to send rejection notification: {}", e.getMessage(), e);
        }
    }
    
    public void deleteGroup(Long groupId) {
        Group group = getGroupById(groupId);
        User currentUser = authService.getCurrentUser();
        
        // Check if current user is admin
        GroupMember currentUserMember = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));
        
        if (currentUserMember.getRole() != GroupMember.GroupRole.ADMIN) {
            throw new RuntimeException("Only group admins can delete the group");
        }
        
        // Delete the group (cascade will handle related entities)
        groupRepository.delete(group);
    }
}

