import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { toast } from 'react-toastify';
import { useAuth } from '../contexts/AuthContext';

const GroupDetail = () => {
  const { groupId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [group, setGroup] = useState(null);
  const [members, setMembers] = useState([]);
  const [pendingMembers, setPendingMembers] = useState([]);
  const [expenses, setExpenses] = useState([]);
  const [settlements, setSettlements] = useState([]);
  const [balances, setBalances] = useState({});
  const [activeTab, setActiveTab] = useState('activity');
  const [loading, setLoading] = useState(true);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviting, setInviting] = useState(false);
  const [showMembersModal, setShowMembersModal] = useState(false);
  const [showEditGroupModal, setShowEditGroupModal] = useState(false);
  const [editGroupName, setEditGroupName] = useState('');
  const [editGroupImageUrl, setEditGroupImageUrl] = useState('');
  const [updatingGroup, setUpdatingGroup] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const [showSettleUpModal, setShowSettleUpModal] = useState(false);
  const [selectedSettlement, setSelectedSettlement] = useState(null);
  const [settleUpMessage, setSettleUpMessage] = useState('');
  const [settleUpImageUrl, setSettleUpImageUrl] = useState('');
  const [settleUpAmount, setSettleUpAmount] = useState('');
  const [settlingUp, setSettlingUp] = useState(false);
  const [settlementHistory, setSettlementHistory] = useState([]);
  const [isGroupCreator, setIsGroupCreator] = useState(false);

  useEffect(() => {
    fetchGroupData();
  }, [groupId]);

  // Refresh when returning to page
  useEffect(() => {
    const handleFocus = () => {
      if (groupId) {
        fetchGroupData();
      }
    };
    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [groupId]);

  const fetchGroupData = async () => {
    if (!groupId || groupId === 'undefined') {
      toast.error('Invalid group ID');
      setLoading(false);
      return;
    }

    try {
      const [groupRes, membersRes, pendingRes, expensesRes] = await Promise.all([
        api.get(`/groups/${groupId}`),
        api.get(`/groups/${groupId}/members`),
        api.get(`/groups/${groupId}/pending-members`).catch(() => ({ data: [] })),
        api.get(`/expenses/group/${groupId}`)
      ]);

      setGroup(groupRes.data);
      const membersData = Array.isArray(membersRes.data) ? membersRes.data : [];
      setMembers(membersData);
      const pendingData = Array.isArray(pendingRes.data) ? pendingRes.data : [];
      setPendingMembers(pendingData);
      
      // Check if current user is admin and group creator
      const currentUserMember = membersData.find(m => m.user?.id === user?.id);
      setIsAdmin(currentUserMember?.role === 'ADMIN');
      setIsGroupCreator(groupRes.data?.createdBy?.id === user?.id);
      
      const expensesData = Array.isArray(expensesRes.data) ? expensesRes.data : [];
      setExpenses(expensesData.sort((a, b) => {
        const dateA = new Date(a.createdAt || 0);
        const dateB = new Date(b.createdAt || 0);
        return dateB - dateA;
      }));

      // Fetch settlement history
      let settlementHistoryData = [];
      try {
        const historyRes = await api.get(`/settlements/group/${groupId}/history`);
        settlementHistoryData = Array.isArray(historyRes.data) ? historyRes.data : [];
        setSettlementHistory(settlementHistoryData);
      } catch (err) {
        setSettlementHistory([]);
      }
      
      // Calculate balances - pass settlement history directly to avoid stale state
      await calculateBalances(expensesData, membersData, Array.isArray(pendingRes.data) ? pendingRes.data : [], settlementHistoryData);
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to fetch group data';
      toast.error(errorMessage);
      if (error.response?.status === 404) {
        setGroup(null);
      }
    } finally {
      setLoading(false);
    }
  };

  const calculateBalances = async (expensesData, membersData, pendingMembersData, settlementHistoryData = null) => {
    try {
      const balanceMap = {};
      
      // Add actual members to balance map
      const seenUserIds = new Set();
      membersData.forEach(member => {
        const userId = member.user?.id;
        if (userId && !seenUserIds.has(userId)) {
          seenUserIds.add(userId);
          balanceMap[userId] = { 
            name: member.user.name, 
            balance: 0, 
            paid: 0, 
            owed: 0,
            isPending: false,
            email: member.user.email
          };
        }
      });

      // Add pending members to balance map
      pendingMembersData.forEach(pending => {
        const pendingKey = `pending-${pending.email}`;
        balanceMap[pendingKey] = {
          name: pending.name || pending.email,
          balance: 0,
          paid: 0,
          owed: 0,
          isPending: true,
          email: pending.email
        };
      });

      // Calculate paid amounts (exclude deleted expenses)
      expensesData.forEach(expense => {
        if (expense.deletedAt) return;
        
        const description = expense.description || '';
        const pendingPaidByMatch = description.match(/\(Paid by:\s*([^)]+)\s*-\s*Pending\)/i);
        
        if (pendingPaidByMatch) {
          const pendingName = pendingPaidByMatch[1].trim();
          const pendingMember = pendingMembersData.find(p => 
            (p.name && p.name === pendingName) || p.email === pendingName
          );
          if (pendingMember) {
            const pendingKey = `pending-${pendingMember.email}`;
            if (balanceMap[pendingKey]) {
              balanceMap[pendingKey].paid += parseFloat(expense.amount || 0);
            }
          }
        } else {
          const paidBy = expense.paidBy?.id;
          if (paidBy && balanceMap[paidBy]) {
            balanceMap[paidBy].paid += parseFloat(expense.amount || 0);
          }
        }
      });

      // Fetch shares and calculate owed amounts
      const processedExpenseIds = new Set(); // Track processed expenses to avoid duplicates
      
      for (const expense of expensesData) {
        if (expense.deletedAt) continue;
      
      // Skip if we've already processed this expense
      if (processedExpenseIds.has(expense.id)) {
        continue;
      }
      processedExpenseIds.add(expense.id);
      
      const description = expense.description || '';
      
      try {
        const sharesRes = await api.get(`/expenses/${expense.id}/shares`);
        const shares = Array.isArray(sharesRes.data) ? sharesRes.data : [];
        
        const expenseAmount = parseFloat(expense.amount || 0);
        
        // Check splitType case-insensitively
        const splitType = (expense.splitType || '').toUpperCase();
        
        if (splitType === 'EQUAL') {
          // For EQUAL splits, use the actual share amounts from ExpenseShare records
          // This ensures we use the exact amounts calculated by the backend (including rounding adjustments)
          
          // Apply actual share amounts from ExpenseShare records
          shares.forEach(share => {
            const userId = share.user?.id;
            const amountOwed = parseFloat(share.amountOwed || 0);
            if (userId && balanceMap[userId] && amountOwed > 0) {
              const beforeOwed = balanceMap[userId].owed;
              balanceMap[userId].owed += amountOwed;
              // #region agent log
              fetch('http://127.0.0.1:7242/ingest/9a5b9856-af02-44c8-b291-90bdfd33f3ee',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'GroupDetail.js:197',message:'Share applied to member',data:{expenseId:expense.id,userId,amountOwed,beforeOwed,afterOwed:balanceMap[userId].owed},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'N'})}).catch(()=>{});
              // #endregion
            }
          });
          
          // Handle pending members from description (if any)
          // Use the actual amounts stored in the description (from backend calculation)
          const pendingSharesMatch = description.match(/\(Pending shares:\s*([^)]+)\)/i);
          if (pendingSharesMatch) {
            const pendingSharesStr = pendingSharesMatch[1];
            const pendingSharePairs = pendingSharesStr.split(',').map(s => s.trim());
            
            pendingSharePairs.forEach(pair => {
              const [email, amountStr] = pair.split(':').map(s => s.trim());
              const amount = parseFloat(amountStr || 0);
              if (email && amount > 0) {
                const pendingKey = `pending-${email}`;
                if (balanceMap[pendingKey]) {
                  const beforeOwed = balanceMap[pendingKey].owed;
                  balanceMap[pendingKey].owed += amount;
                  // #region agent log
                  fetch('http://127.0.0.1:7242/ingest/9a5b9856-af02-44c8-b291-90bdfd33f3ee',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'GroupDetail.js:214',message:'Pending share applied (EQUAL)',data:{expenseId:expense.id,email,amount,beforeOwed,afterOwed:balanceMap[pendingKey].owed},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'N'})}).catch(()=>{});
                  // #endregion
                }
              }
            });
          }
        } else if (splitType === 'CUSTOM') {
          const pendingSharesMatch = description.match(/\(Pending shares:\s*([^)]+)\)/i);
          if (pendingSharesMatch) {
            const pendingSharesStr = pendingSharesMatch[1];
            const pendingSharePairs = pendingSharesStr.split(',').map(s => s.trim());
            pendingSharePairs.forEach(pair => {
              const [email, amountStr] = pair.split(':').map(s => s.trim());
              const amount = parseFloat(amountStr || 0);
              if (email && amount > 0) {
                const pendingKey = `pending-${email}`;
                if (balanceMap[pendingKey]) {
                  balanceMap[pendingKey].owed += amount;
                }
              }
            });
          }
          
          shares.forEach(share => {
            const userId = share.user?.id;
            if (userId && balanceMap[userId]) {
              const amountOwed = parseFloat(share.amountOwed || 0);
              balanceMap[userId].owed += amountOwed;
            }
          });
        }
      } catch (error) {
        console.error('Failed to fetch shares for expense:', expense.id, error);
        }
      }

      // Calculate final balances
      let totalPaid = 0;
      let totalOwed = 0;
      Object.keys(balanceMap).forEach(key => {
        const beforeBalance = balanceMap[key].balance;
        balanceMap[key].balance = balanceMap[key].paid - balanceMap[key].owed;
        totalPaid += balanceMap[key].paid;
        totalOwed += balanceMap[key].owed;
        // #region agent log
        fetch('http://127.0.0.1:7242/ingest/9a5b9856-af02-44c8-b291-90bdfd33f3ee',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'GroupDetail.js:263',message:'Balance calculated',data:{key,name:balanceMap[key].name,paid:balanceMap[key].paid,owed:balanceMap[key].owed,balance:balanceMap[key].balance,isPending:balanceMap[key].isPending},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'M'})}).catch(()=>{});
        // #endregion
      });
      
      // Validation: Total paid should equal sum of all expense amounts (excluding deleted)
      const totalExpenseAmount = expensesData.filter(e => !e.deletedAt).reduce((sum, e) => sum + parseFloat(e.amount || 0), 0);
      const balance = totalPaid - totalOwed;
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/9a5b9856-af02-44c8-b291-90bdfd33f3ee',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'GroupDetail.js:272',message:'Balance validation',data:{totalPaid,totalOwed,totalExpenseAmount,netBalance:balance},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'P'})}).catch(()=>{});
      // #endregion
      
      // Adjust balances based on settlements
      // Use passed settlementHistoryData if provided, otherwise fall back to state
      const settlementsToApply = settlementHistoryData !== null ? settlementHistoryData : settlementHistory;
      settlementsToApply.forEach(settlement => {
        const fromUserId = settlement.fromUser?.id;
        const toUserId = settlement.toUser?.id;
        const amount = parseFloat(settlement.amount || 0);
        
        if (fromUserId && balanceMap[fromUserId]) {
          balanceMap[fromUserId].balance += amount;
          // #region agent log
          fetch('http://127.0.0.1:7242/ingest/9a5b9856-af02-44c8-b291-90bdfd33f3ee',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'GroupDetail.js:283',message:'Settlement applied to debtor',data:{fromUserId,amount,newBalance:balanceMap[fromUserId].balance},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
          // #endregion
        }
        if (toUserId && balanceMap[toUserId]) {
          balanceMap[toUserId].balance -= amount;
          // #region agent log
          fetch('http://127.0.0.1:7242/ingest/9a5b9856-af02-44c8-b291-90bdfd33f3ee',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'GroupDetail.js:286',message:'Settlement applied to creditor',data:{toUserId,amount,newBalance:balanceMap[toUserId].balance},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'E'})}).catch(()=>{});
          // #endregion
        }
      });

      setBalances(balanceMap);
      
      // Calculate settlements
      const allSettlements = calculateSettlementsFromBalances(balanceMap);
      setSettlements(allSettlements);
      
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/9a5b9856-af02-44c8-b291-90bdfd33f3ee',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'GroupDetail.js:290',message:'Balance calculation complete',data:{expenseCount:expensesData.filter(e=>!e.deletedAt).length,memberCount:Object.keys(balanceMap).filter(k=>!k.startsWith('pending-')).length,pendingCount:Object.keys(balanceMap).filter(k=>k.startsWith('pending-')).length,settlementCount:allSettlements.length},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'Q'})}).catch(()=>{});
      // #endregion
    } catch (error) {
      console.error('Error calculating balances:', error);
      // #region agent log
      fetch('http://127.0.0.1:7242/ingest/9a5b9856-af02-44c8-b291-90bdfd33f3ee',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'GroupDetail.js:295',message:'Balance calculation ERROR',data:{error:error.message,stack:error.stack},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'Q'})}).catch(()=>{});
      // #endregion
      // Ensure balances are set even if there's an error
      setBalances({});
      setSettlements([]);
    }
  };
  
  const calculateSettlementsFromBalances = (balanceMap) => {
    const settlements = [];
    // Create working balances as mutable entries
    const workingBalances = [];
    Object.entries(balanceMap).forEach(([key, balanceInfo]) => {
      workingBalances.push({
        key,
        ...balanceInfo,
        balance: balanceInfo.balance
      });
    });
    
    // Separate creditors (positive balance) and debtors (negative balance)
    const creditors = workingBalances.filter(b => b.balance > 0.01);
    const debtors = workingBalances.filter(b => b.balance < -0.01);
    
    // Sort by absolute balance (largest first for creditors, most negative first for debtors)
    creditors.sort((a, b) => b.balance - a.balance);
    debtors.sort((a, b) => a.balance - b.balance);
    
    // Greedy algorithm: match debtors with creditors
    let creditorIndex = 0;
    let debtorIndex = 0;
    
    while (creditorIndex < creditors.length && debtorIndex < debtors.length) {
      const creditor = creditors[creditorIndex];
      const debtor = debtors[debtorIndex];
      
      // Skip if balance is effectively zero
      if (Math.abs(creditor.balance) < 0.01) {
        creditorIndex++;
        continue;
      }
      if (Math.abs(debtor.balance) < 0.01) {
        debtorIndex++;
        continue;
      }
      
      const creditorAmount = creditor.balance;
      const debtorAmount = Math.abs(debtor.balance);
      
      // Transaction amount is the minimum of what creditor is owed and what debtor owes
      const transactionAmount = Math.min(creditorAmount, debtorAmount);
      
      if (transactionAmount > 0.01) {
        const isPendingFrom = debtor.isPending || debtor.key.startsWith('pending-');
        const isPendingTo = creditor.isPending || creditor.key.startsWith('pending-');
        
        const fromUserId = isPendingFrom ? null : (debtor.key.startsWith('pending-') ? null : parseInt(debtor.key));
        const toUserId = isPendingTo ? null : (creditor.key.startsWith('pending-') ? null : parseInt(creditor.key));
        
        settlements.push({
          fromUserId: fromUserId,
          fromUserName: debtor.name,
          fromUserEmail: debtor.email,
          fromUserIsPending: isPendingFrom,
          toUserId: toUserId,
          toUserName: creditor.name,
          toUserEmail: creditor.email,
          toUserIsPending: isPendingTo,
          amount: parseFloat(transactionAmount.toFixed(2))
        });
        
        // Update balances
        creditor.balance -= transactionAmount;
        debtor.balance += transactionAmount; // Add because balance is negative
        
        // Round to avoid floating point precision issues
        creditor.balance = parseFloat(creditor.balance.toFixed(2));
        debtor.balance = parseFloat(debtor.balance.toFixed(2));
        
        // Move to next if balance is effectively zero
        if (Math.abs(creditor.balance) < 0.01) {
          creditorIndex++;
        }
        if (Math.abs(debtor.balance) < 0.01) {
          debtorIndex++;
        }
      } else {
        // If transaction amount is too small, move to next
        creditorIndex++;
        debtorIndex++;
      }
    }
    
    return settlements;
  };

  useEffect(() => {
    if (activeTab === 'settle' && Object.keys(balances).length > 0) {
      const allSettlements = calculateSettlementsFromBalances(balances);
      setSettlements(allSettlements);
    }
  }, [activeTab, balances]);

  const handleInviteMember = async (e) => {
    e.preventDefault();
    if (!inviteEmail.trim()) {
      toast.error('Please enter an email address');
      return;
    }

    setInviting(true);
    try {
      await api.post(`/groups/${groupId}/members`, null, {
        params: { email: inviteEmail.trim() }
      });
      toast.success(`Invitation sent to ${inviteEmail}`);
      setInviteEmail('');
      setShowInviteModal(false);
      await fetchGroupData();
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to invite member';
      toast.error(errorMessage);
    } finally {
      setInviting(false);
    }
  };

  const handleUpdateGroup = async (e) => {
    e.preventDefault();
    setUpdatingGroup(true);
    try {
      const updateData = {};
      if (editGroupName.trim()) {
        updateData.name = editGroupName.trim();
      }
      if (editGroupImageUrl !== undefined) {
        updateData.imageUrl = editGroupImageUrl.trim() || null;
      }
      
      const response = await api.put(`/groups/${groupId}`, updateData);
      setGroup(response.data);
      setShowEditGroupModal(false);
      toast.success('Group updated successfully!');
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to update group';
      toast.error(errorMessage);
    } finally {
      setUpdatingGroup(false);
    }
  };

  const handleRemoveMember = async (userId, memberName) => {
    if (!window.confirm(`Are you sure you want to remove ${memberName} from this group?`)) {
      return;
    }

    try {
      await api.delete(`/groups/${groupId}/members/${userId}`);
      toast.success(`${memberName} has been removed from the group`);
      await fetchGroupData();
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to remove member';
      toast.error(errorMessage);
    }
  };

  const handleRemovePendingMember = async (pendingMemberId, pendingMemberName) => {
    if (!window.confirm(`Are you sure you want to remove ${pendingMemberName} from this group?`)) {
      return;
    }

    try {
      await api.delete(`/groups/${groupId}/pending-members/${pendingMemberId}`);
      toast.success(`${pendingMemberName} has been removed from the group`);
      await fetchGroupData();
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to remove pending member';
      toast.error(errorMessage);
    }
  };

  const handleDeleteSettlement = async (settlementId, settlementAmount) => {
    if (!window.confirm(`Are you sure you want to delete this settlement of $${parseFloat(settlementAmount || 0).toFixed(2)}? This action cannot be undone.`)) {
      return;
    }

    try {
      await api.delete(`/settlements/group/${groupId}/${settlementId}`);
      toast.success('Settlement deleted successfully');
      await fetchGroupData();
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to delete settlement';
      toast.error(errorMessage);
    }
  };

  const handleLeaveGroup = async () => {
    if (!window.confirm('Are you sure you want to leave this group? You will no longer have access to it.')) {
      return;
    }

    try {
      await api.post(`/groups/${groupId}/leave`);
      toast.success('You have left the group');
      navigate('/');
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to leave group';
      toast.error(errorMessage);
    }
  };

  const handleDeleteGroup = async () => {
    const confirmMessage = `Are you sure you want to delete "${group?.name}"? This action cannot be undone. All expenses, settlements, and member data will be permanently deleted.`;
    if (!window.confirm(confirmMessage)) {
      return;
    }

    // Double confirmation for safety
    if (!window.confirm('This is your final warning. Deleting this group will permanently remove all data. Are you absolutely sure?')) {
      return;
    }

    try {
      await api.delete(`/groups/${groupId}`);
      toast.success('Group deleted successfully');
      navigate('/');
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to delete group';
      toast.error(errorMessage);
    }
  };

  const openEditModal = () => {
    setEditGroupName(group?.name || '');
    setEditGroupImageUrl(group?.imageUrl || '');
    setShowEditGroupModal(true);
  };

  const openSettleUpModal = (settlement) => {
    setSelectedSettlement(settlement);
    setSettleUpAmount(settlement.amount?.toFixed(2) || '');
    setSettleUpMessage('');
    setSettleUpImageUrl('');
    setShowSettleUpModal(true);
  };

  const handleSettleUp = async (e) => {
    e.preventDefault();
    if (!selectedSettlement) return;
    
    setSettlingUp(true);
    try {
      const settleUpData = {
        toUserId: selectedSettlement.toUserId,
        amount: parseFloat(settleUpAmount),
        message: settleUpMessage.trim() || null,
        imageUrl: settleUpImageUrl.trim() || null
      };
      
      await api.post(`/settlements/group/${groupId}/settle`, settleUpData);
      toast.success('Settlement recorded successfully!');
      setShowSettleUpModal(false);
      setSelectedSettlement(null);
      setSettleUpMessage('');
      setSettleUpImageUrl('');
      setSettleUpAmount('');
      await fetchGroupData();
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to record settlement';
      toast.error(errorMessage);
    } finally {
      setSettlingUp(false);
    }
  };

  const getCurrentUserBalance = () => {
    if (!user?.id) return null;
    const userBalance = balances[user.id];
    if (!userBalance) return null;
    return userBalance.balance;
  };

  const formatBalance = (balance) => {
    if (balance === null || balance === undefined) return 'No balance';
    const amount = parseFloat(balance);
    const EPSILON = 0.01; // Tolerance for floating point comparison
    if (Math.abs(amount) < EPSILON) return 'All settled up!';
    if (amount > 0) return `You are owed $${amount.toFixed(2)}`;
    return `You owe $${Math.abs(amount).toFixed(2)}`;
  };

  const getBalanceColor = (balance) => {
    if (balance === null || balance === undefined) return 'text-gray-500';
    const amount = parseFloat(balance);
    const EPSILON = 0.01; // Tolerance for floating point comparison
    if (Math.abs(amount) < EPSILON) return 'text-green-600'; // All settled up - green
    if (amount > 0) return 'text-green-600'; // You are owed - green
    return 'text-red-600'; // You owe - red
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-gray-50">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600"></div>
      </div>
    );
  }

  if (!group) {
    return (
      <div className="text-center py-12 bg-gray-50 min-h-screen">
        <h2 className="text-2xl font-bold text-gray-900 mb-4">Group not found</h2>
        <Link to="/" className="text-green-600 hover:text-green-700 font-medium">
          ← Back to Dashboard
        </Link>
      </div>
    );
  }

  const currentUserBalance = getCurrentUserBalance();
  const memberCount = members.length + pendingMembers.length;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header with Group Info */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-6xl mx-auto px-4 py-6">
          <div className="flex items-center justify-between mb-4">
            <Link to="/" className="text-gray-600 hover:text-gray-900">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </Link>
            <div className="flex items-center space-x-3">
              <button
                onClick={() => setShowMembersModal(true)}
                className="text-gray-600 hover:text-gray-900 font-medium flex items-center space-x-1"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
                <span>Members</span>
              </button>
              <button
                onClick={() => setShowInviteModal(true)}
                className="text-green-600 hover:text-green-700 font-medium"
              >
                + Invite
              </button>
              {isAdmin && (
                <button
                  onClick={handleDeleteGroup}
                  className="text-red-600 hover:text-red-700 font-medium flex items-center space-x-1"
                  title="Delete group"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  <span>Delete Group</span>
                </button>
              )}
              {!isAdmin && (
                <button
                  onClick={handleLeaveGroup}
                  className="text-red-600 hover:text-red-700 font-medium"
                  title="Leave group"
                >
                  Leave Group
                </button>
              )}
            </div>
          </div>
          
          <div className="flex items-center space-x-4 mb-4">
            <div className="relative">
              {group.imageUrl ? (
                <img 
                  src={group.imageUrl} 
                  alt={group.name}
                  className="w-16 h-16 rounded-xl object-cover"
                  onError={(e) => {
                    e.target.style.display = 'none';
                    e.target.nextSibling.style.display = 'flex';
                  }}
                />
              ) : null}
              <div 
                className={`w-16 h-16 bg-gradient-to-br from-green-400 to-green-600 rounded-xl flex items-center justify-center text-white font-bold text-2xl ${group.imageUrl ? 'hidden' : ''}`}
              >
                {group.name?.charAt(0)?.toUpperCase() || 'G'}
              </div>
              {isAdmin && (
                <button
                  onClick={openEditModal}
                  className="absolute -bottom-1 -right-1 w-6 h-6 bg-green-600 rounded-full flex items-center justify-center text-white hover:bg-green-700 transition-colors shadow-md"
                  title="Edit group"
                >
                  <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 4.732z" />
                  </svg>
                </button>
              )}
            </div>
            <div className="flex-1">
              <div className="flex items-center space-x-2">
                <h1 className="text-3xl font-bold text-gray-900">{group.name}</h1>
                {isAdmin && (
                  <button
                    onClick={openEditModal}
                    className="text-gray-400 hover:text-gray-600 transition-colors"
                    title="Edit group name"
                  >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 4.732z" />
                    </svg>
                  </button>
                )}
              </div>
              <p className="text-gray-600 mt-1">
                {memberCount} {memberCount === 1 ? 'person' : 'people'} • Created {group.createdAt ? new Date(group.createdAt).toLocaleDateString('en-US', { month: 'short', year: 'numeric' }) : 'N/A'}
              </p>
            </div>
          </div>

          {/* Balance Summary */}
          {currentUserBalance !== null && (
            <div className={`text-lg font-semibold ${getBalanceColor(currentUserBalance)}`}>
              {formatBalance(currentUserBalance)}
            </div>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-6xl mx-auto px-4">
          <div className="flex space-x-8">
            <button
              onClick={() => setActiveTab('activity')}
              className={`py-4 px-2 border-b-2 font-medium transition-colors ${
                activeTab === 'activity'
                  ? 'border-green-600 text-green-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              Activity
            </button>
            <button
              onClick={() => setActiveTab('balances')}
              className={`py-4 px-2 border-b-2 font-medium transition-colors ${
                activeTab === 'balances'
                  ? 'border-green-600 text-green-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              Balances
            </button>
            <button
              onClick={() => {
                setActiveTab('settle');
                if (Object.keys(balances).length > 0) {
                  const allSettlements = calculateSettlementsFromBalances(balances);
                  setSettlements(allSettlements);
                }
              }}
              className={`py-4 px-2 border-b-2 font-medium transition-colors ${
                activeTab === 'settle'
                  ? 'border-green-600 text-green-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              Settle up
            </button>
            <button
              onClick={() => setActiveTab('totals')}
              className={`py-4 px-2 border-b-2 font-medium transition-colors ${
                activeTab === 'totals'
                  ? 'border-green-600 text-green-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              Totals
            </button>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-6xl mx-auto px-4 py-6">
        {/* Activity Tab */}
        {activeTab === 'activity' && (
          <div className="space-y-4">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Recent Activity</h2>
              <Link
                to={`/groups/${groupId}/expenses/add`}
                className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors font-medium"
              >
                + Add Expense
              </Link>
            </div>

            {expenses.length === 0 ? (
              <div className="bg-white rounded-lg shadow-sm p-12 text-center">
                <div className="text-gray-400 text-5xl mb-4">💰</div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">No expenses yet</h3>
                <p className="text-gray-600 mb-6">Start splitting expenses with your group</p>
                <Link
                  to={`/groups/${groupId}/expenses/add`}
                  className="inline-block bg-green-600 text-white px-6 py-3 rounded-lg hover:bg-green-700 transition-colors font-medium"
                >
                  Add Your First Expense
                </Link>
              </div>
            ) : (
              <div className="space-y-3">
                {expenses.map((expense) => {
                  const isDeleted = expense.deletedAt != null;
                  
                  let paidBy = expense.paidBy?.name || 'Unknown';
                  let paidByInitial = paidBy.charAt(0).toUpperCase();
                  const description = expense.description || '';
                  
                  const pendingPaidByMatch = description.match(/\(Paid by:\s*([^)]+)\s*-\s*Pending\)/i);
                  if (pendingPaidByMatch) {
                    paidBy = pendingPaidByMatch[1].trim();
                    paidByInitial = paidBy.charAt(0).toUpperCase();
                  }
                  
                  const cleanDescription = description.replace(/\s*\(Paid by:\s*[^)]+\s*-\s*Pending\)/i, '').trim();
                  
                  const deletedBy = expense.deletedBy?.name || 'Unknown';
                  const date = expense.createdAt ? new Date(expense.createdAt) : new Date();
                  const deleteDate = expense.deletedAt ? new Date(expense.deletedAt) : null;
                  const isToday = date.toDateString() === new Date().toDateString();
                  const isYesterday = date.toDateString() === new Date(Date.now() - 86400000).toDateString();
                  
                  let dateLabel = '';
                  if (isToday) dateLabel = 'Today';
                  else if (isYesterday) dateLabel = 'Yesterday';
                  else dateLabel = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });

                  const canDelete = !isDeleted && (
                    expense.paidBy?.id === user?.id || 
                    members.find(m => m.user?.id === user?.id && m.role === 'ADMIN')
                  );
                  const canPermanentlyDelete = isDeleted && isGroupCreator;

                  return (
                    <div 
                      key={expense.id} 
                      className={`bg-white rounded-lg shadow-sm p-4 hover:shadow-md transition-shadow ${
                        isDeleted ? 'opacity-60 border-l-4 border-red-300' : ''
                      }`}
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex items-start space-x-4 flex-1">
                          <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
                            isDeleted ? 'bg-red-100' : 'bg-green-100'
                          }`}>
                            {isDeleted ? (
                              <svg className="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                              </svg>
                            ) : (
                              <span className={`font-semibold text-lg ${
                                isDeleted ? 'text-red-600' : 'text-green-600'
                              }`}>
                                {paidByInitial}
                              </span>
                            )}
                          </div>
                          <div className="flex-1 min-w-0">
                            {isDeleted ? (
                              <>
                                <div className="flex items-center space-x-2 mb-1">
                                  <p className="font-semibold text-gray-900">{deletedBy}</p>
                                  <span className="text-gray-500">deleted expense</span>
                                </div>
                                <div className="flex items-center space-x-2 mb-1 line-through">
                                  <p className="font-semibold text-gray-500">{paidBy}</p>
                                  <span className="text-gray-400">paid</span>
                                  <p className="font-semibold text-gray-500">${parseFloat(expense.amount || 0).toFixed(2)}</p>
                                </div>
                                <p className="text-gray-500 mb-1 line-through">{cleanDescription || expense.description || 'No description'}</p>
                                <div className="flex items-center space-x-2 text-sm text-gray-400">
                                  <span>Deleted {deleteDate ? (deleteDate.toDateString() === new Date().toDateString() ? 'today' : deleteDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })) : ''}</span>
                                </div>
                              </>
                            ) : (
                              <>
                                <div className="flex items-center space-x-2 mb-1">
                                  <p className="font-semibold text-gray-900">{paidBy}</p>
                                  <span className="text-gray-500">paid</span>
                                  <p className="font-semibold text-gray-900">${parseFloat(expense.amount || 0).toFixed(2)}</p>
                                </div>
                                <p className="text-gray-700 mb-1">{cleanDescription || 'No description'}</p>
                                <div className="flex items-center space-x-2 text-sm text-gray-500">
                                  <span>{dateLabel}</span>
                                  {expense.splitType && (
                                    <>
                                      <span>•</span>
                                      <span className="capitalize">{expense.splitType.toLowerCase()} split</span>
                                    </>
                                  )}
                                </div>
                              </>
                            )}
                          </div>
                        </div>
                        {canDelete && (
                          <button
                            onClick={async () => {
                              if (window.confirm('Are you sure you want to delete this expense?')) {
                                try {
                                  await api.delete(`/expenses/${expense.id}`);
                                  toast.success('Expense deleted successfully');
                                  setLoading(true);
                                  await fetchGroupData();
                                  setLoading(false);
                                } catch (error) {
                                  const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to delete expense';
                                  toast.error(errorMessage);
                                  setLoading(false);
                                }
                              }
                            }}
                            className="ml-4 text-red-600 hover:text-red-700 transition-colors"
                            title="Delete expense"
                          >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                          </button>
                        )}
                        {canPermanentlyDelete && (
                          <button
                            onClick={async () => {
                              if (window.confirm('Are you sure you want to permanently delete this expense? This action cannot be undone.')) {
                                try {
                                  await api.delete(`/expenses/${expense.id}/permanent`);
                                  toast.success('Expense permanently deleted');
                                  setLoading(true);
                                  await fetchGroupData();
                                  setLoading(false);
                                } catch (error) {
                                  const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to permanently delete expense';
                                  toast.error(errorMessage);
                                  setLoading(false);
                                }
                              }
                            }}
                            className="ml-4 text-red-800 hover:text-red-900 transition-colors"
                            title="Permanently delete expense"
                          >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
            
            {/* Settlement History Section */}
            {settlementHistory.length > 0 && (
              <div className="mt-8">
                <h2 className="text-xl font-semibold text-gray-900 mb-4">Settlement History</h2>
                <div className="space-y-3">
                  {settlementHistory.map((settlement) => {
                    const date = settlement.settledAt ? new Date(settlement.settledAt) : new Date();
                    const isToday = date.toDateString() === new Date().toDateString();
                    const isYesterday = date.toDateString() === new Date(Date.now() - 86400000).toDateString();
                    
                    let dateLabel = '';
                    if (isToday) dateLabel = 'Today';
                    else if (isYesterday) dateLabel = 'Yesterday';
                    else dateLabel = date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
                    
                    const isCurrentUser = settlement.fromUser?.id === user?.id;
                    
                    // Check if current user is admin - use direct check like pending members
                    const currentUserMember = members.find(m => m.user?.id === user?.id);
                    const canDeleteSettlement = currentUserMember?.role === 'ADMIN' || isAdmin;
                    
                    return (
                      <div key={settlement.id} className="bg-white rounded-lg shadow-sm p-4 hover:shadow-md transition-shadow border-l-4 border-green-500">
                        <div className="flex items-start justify-between">
                          <div className="flex items-start space-x-4 flex-1 min-w-0">
                            <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center flex-shrink-0">
                              <span className="font-semibold text-lg text-green-600">✓</span>
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center space-x-2 mb-1">
                                <p className="font-semibold text-gray-900">
                                  {settlement.fromUser?.name}
                                  {isCurrentUser && ' (You)'}
                                </p>
                                <span className="text-gray-500">settled up with</span>
                                <p className="font-semibold text-gray-900">{settlement.toUser?.name}</p>
                              </div>
                              <p className="text-lg font-bold text-green-600 mb-1">
                                ${parseFloat(settlement.amount || 0).toFixed(2)}
                              </p>
                              {settlement.message && (
                                <p className="text-gray-600 mb-1 italic">"{settlement.message}"</p>
                              )}
                              {settlement.imageUrl && (
                                <div className="mt-2">
                                  <img 
                                    src={settlement.imageUrl} 
                                    alt="Settlement proof" 
                                    className="max-w-xs rounded-lg border border-gray-200"
                                    onError={(e) => {
                                      e.target.style.display = 'none';
                                    }}
                                  />
                                </div>
                              )}
                              <div className="flex items-center space-x-2 text-sm text-gray-500 mt-1">
                                <span>{dateLabel}</span>
                                <span>•</span>
                                <span>{date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })}</span>
                              </div>
                            </div>
                          </div>
                          {canDeleteSettlement && (
                            <button
                              onClick={() => handleDeleteSettlement(settlement.id, settlement.amount)}
                              className="ml-4 flex-shrink-0 text-red-600 hover:text-red-700 transition-colors"
                              title="Delete settlement"
                            >
                              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                              </svg>
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Balances Tab */}
        {activeTab === 'balances' && (
          <div className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Balances</h2>
            {Object.keys(balances).length === 0 ? (
              <div className="bg-white rounded-lg shadow-sm p-12 text-center">
                <div className="text-gray-400 text-5xl mb-4">💰</div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">No balances yet</h3>
                <p className="text-gray-600">Add expenses to see balances</p>
              </div>
            ) : (() => {
              // Check if all balances are settled (all balances are 0 or very close to 0)
              const allBalances = Object.values(balances);
              const allSettled = allBalances.every(b => Math.abs(parseFloat(b.balance || 0)) < 0.01);
              
              if (allSettled) {
                return (
                  <div className="bg-white rounded-lg shadow-sm p-12 text-center">
                    <div className="text-green-400 text-6xl mb-4">✓</div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">All settled up!</h3>
                    <p className="text-gray-600">No outstanding debts in this group</p>
                  </div>
                );
              }
              
              return (
              <div className="space-y-3">
                {Object.entries(balances).map(([key, balanceInfo]) => {
                  const isCurrentUser = !balanceInfo.isPending && parseInt(key) === user?.id;
                  const balance = parseFloat(balanceInfo.balance || 0);
                  const paid = parseFloat(balanceInfo.paid || 0);
                  const owed = parseFloat(balanceInfo.owed || 0);
                  const isOwed = balance > 0.01;
                  const owes = balance < -0.01;
                  
                  if (Math.abs(balance) < 0.01 && !isCurrentUser) return null;
                  
                  return (
                    <div 
                      key={key}
                      className={`bg-white rounded-lg shadow-sm p-4 hover:shadow-md transition-shadow ${
                        balanceInfo.isPending ? 'border-l-4 border-yellow-300 bg-yellow-50' : ''
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-4 flex-1">
                          <div className={`w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0 ${
                            balanceInfo.isPending ? 'bg-yellow-200' : 'bg-green-100'
                          }`}>
                            <span className={`font-semibold text-lg ${
                              balanceInfo.isPending ? 'text-yellow-700' : 'text-green-600'
                            }`}>
                              {balanceInfo.name?.charAt(0)?.toUpperCase() || '?'}
                            </span>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center space-x-2">
                              <p className="font-semibold text-gray-900">
                                {balanceInfo.name}
                                {isCurrentUser && ' (You)'}
                                {balanceInfo.isPending && ' (Pending)'}
                              </p>
                              {balanceInfo.email && (
                                <p className="text-sm text-gray-500">({balanceInfo.email})</p>
                              )}
                            </div>
                            <div className="flex items-center space-x-4 mt-1 text-sm text-gray-600">
                              <span>Paid ${paid.toFixed(2)}</span>
                              <span>•</span>
                              <span>Owed ${owed.toFixed(2)}</span>
                            </div>
                          </div>
                        </div>
                        <div className="text-right">
                          {isOwed && (
                            <p className="text-lg font-semibold text-green-600">
                              Gets back ${balance.toFixed(2)}
                            </p>
                          )}
                          {owes && (
                            <p className="text-lg font-semibold text-red-600">
                              Owed ${Math.abs(balance).toFixed(2)}
                            </p>
                          )}
                          {!isOwed && !owes && (
                            <p className="text-lg font-semibold text-gray-500">
                              $0.00
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
              );
            })()}
          </div>
        )}

        {/* Settle Up Tab */}
        {activeTab === 'settle' && (
          <div className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Settle Up</h2>
            {settlements.length === 0 ? (
              <div className="bg-white rounded-lg shadow-sm p-12 text-center">
                <div className="text-green-400 text-6xl mb-4">✓</div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">All settled up!</h3>
                <p className="text-gray-600">No outstanding debts in this group</p>
              </div>
            ) : (
              <div className="space-y-3">
                {settlements.map((settlement, index) => {
                  const fromUserId = settlement.fromUserId;
                  const isCurrentUserOwing = fromUserId === user?.id;
                  const canSettle = isCurrentUserOwing && !settlement.fromUserIsPending && !settlement.toUserIsPending;
                  
                  return (
                    <div 
                      key={index}
                      className={`bg-white rounded-lg shadow-sm p-4 hover:shadow-md transition-shadow ${
                        settlement.fromUserIsPending || settlement.toUserIsPending ? 'border-l-4 border-yellow-300 bg-yellow-50' : ''
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-4 flex-1">
                          <div className={`w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0 ${
                            settlement.fromUserIsPending || settlement.toUserIsPending ? 'bg-yellow-200' : 'bg-green-100'
                          }`}>
                            <span className={`font-semibold text-lg ${
                              settlement.fromUserIsPending || settlement.toUserIsPending ? 'text-yellow-700' : 'text-green-600'
                            }`}>
                              {settlement.fromUserName?.charAt(0)?.toUpperCase() || '?'}
                            </span>
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="font-semibold text-gray-900">
                              {settlement.fromUserName}
                              {isCurrentUserOwing && ' (You)'}
                              {settlement.fromUserIsPending && ' (Pending)'}
                              {settlement.fromUserEmail && ` (${settlement.fromUserEmail})`}
                            </p>
                            <p className="text-sm text-gray-600 mt-1">
                              owes <span className="font-medium">{settlement.toUserName}</span>
                              {settlement.toUserIsPending && ' (Pending)'}
                              {settlement.toUserEmail && ` (${settlement.toUserEmail})`}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center space-x-4">
                          <div className="text-right">
                            <p className="text-lg font-semibold text-red-600">
                              ${parseFloat(settlement.amount || 0).toFixed(2)}
                            </p>
                          </div>
                          {canSettle && (
                            <button
                              onClick={() => openSettleUpModal(settlement)}
                              className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors font-medium"
                            >
                              Settle Up
                            </button>
                          )}
                          {!canSettle && (settlement.fromUserIsPending || settlement.toUserIsPending) && (
                            <span className="text-sm text-yellow-700 font-medium">Pending member - cannot settle yet</span>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Totals Tab */}
        {activeTab === 'totals' && (
          <div className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Totals</h2>
            <div className="bg-white rounded-lg shadow-sm p-6">
              <div className="space-y-4">
                <div className="flex justify-between items-center pb-4 border-b border-gray-200">
                  <span className="text-gray-600">Total Expenses</span>
                  <span className="text-xl font-semibold text-gray-900">
                    ${expenses
                      .filter(e => !e.deletedAt)
                      .reduce((sum, e) => sum + parseFloat(e.amount || 0), 0)
                      .toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between items-center pb-4 border-b border-gray-200">
                  <span className="text-gray-600">Total Paid by You</span>
                  <span className="text-xl font-semibold text-gray-900">
                    ${user?.id && balances[user.id] ? balances[user.id].paid.toFixed(2) : '0.00'}
                  </span>
                </div>
                {(() => {
                  const netBalance = currentUserBalance ?? 0;
                  const totalYouOwe = netBalance < -0.01 ? Math.abs(netBalance) : 0;
                  const totalYouAreOwed = netBalance > 0.01 ? netBalance : 0;
                  return (
                    <>
                      <div className="flex justify-between items-center pb-4 border-b border-gray-200">
                        <span className="text-gray-600">Total You Owe</span>
                        <span className="text-xl font-semibold text-gray-900">
                          ${totalYouOwe.toFixed(2)}
                        </span>
                      </div>
                      <div className="flex justify-between items-center pb-4 border-b border-gray-200">
                        <span className="text-gray-600">Total You Are Owed</span>
                        <span className="text-xl font-semibold text-gray-900">
                          ${totalYouAreOwed.toFixed(2)}
                        </span>
                      </div>
                    </>
                  );
                })()}
                {(() => {
                  // Calculate total settlements made by current user
                  const totalSettled = settlementHistory
                    .filter(s => s.fromUser?.id === user?.id)
                    .reduce((sum, s) => sum + parseFloat(s.amount || 0), 0);
                  
                  return totalSettled > 0 ? (
                    <div className="flex justify-between items-center pb-4 border-b border-gray-200">
                      <span className="text-gray-600">Total Settled by You</span>
                      <span className="text-xl font-semibold text-green-600">
                        ${totalSettled.toFixed(2)}
                      </span>
                    </div>
                  ) : null;
                })()}
                <div className="flex justify-between items-center">
                  <span className="text-gray-600 font-semibold">Your Balance</span>
                  <span className={`text-xl font-semibold ${getBalanceColor(currentUserBalance)}`}>
                    {currentUserBalance !== null ? (
                      Math.abs(currentUserBalance) < 0.01 ? 'All settled up!' : `$${currentUserBalance.toFixed(2)}`
                    ) : '$0.00'}
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Invite Member Modal */}
      {showInviteModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Invite Member</h2>
            <form onSubmit={handleInviteMember}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email Address
                </label>
                <input
                  type="email"
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  placeholder="Enter email address"
                  required
                />
              </div>
              <div className="flex space-x-3">
                <button
                  type="button"
                  onClick={() => {
                    setShowInviteModal(false);
                    setInviteEmail('');
                  }}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={inviting}
                  className="flex-1 px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors disabled:opacity-50"
                >
                  {inviting ? 'Sending...' : 'Send Invite'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Members Modal */}
      {showMembersModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 max-w-2xl w-full mx-4 max-h-[80vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-2xl font-bold text-gray-900">Members</h2>
              <button
                onClick={() => setShowMembersModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="space-y-3">
              {members.map((member) => {
                const isCurrentUser = member.user?.id === user?.id;
                const canRemove = isAdmin && !isCurrentUser;
                return (
                  <div key={member.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                        <span className="font-semibold text-green-600">
                          {member.user?.name?.charAt(0)?.toUpperCase() || '?'}
                        </span>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">
                          {member.user?.name}
                          {isCurrentUser && ' (You)'}
                        </p>
                        <p className="text-sm text-gray-500">{member.user?.email}</p>
                      </div>
                    </div>
                    <div className="flex items-center space-x-3">
                      <span className={`px-2 py-1 text-xs rounded-full ${
                        member.role === 'ADMIN' 
                          ? 'bg-red-100 text-red-800' 
                          : 'bg-gray-100 text-gray-800'
                      }`}>
                        {member.role}
                      </span>
                      {canRemove && (
                        <button
                          onClick={() => handleRemoveMember(member.user.id, member.user.name)}
                          className="text-red-600 hover:text-red-700"
                          title="Remove member"
                        >
                          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
              {pendingMembers.map((pending) => {
                const currentUserMember = members.find(m => m.user?.id === user?.id);
                const canRemovePending = currentUserMember?.role === 'ADMIN';
                // Check if user has an account (user field is set)
                const hasAccount = pending.user !== null && pending.user !== undefined;
                const statusText = hasAccount ? 'Pending Request' : 'Pending';
                
                return (
                  <div key={pending.id || pending.email} className="flex items-center justify-between p-3 bg-yellow-50 rounded-lg border-l-4 border-yellow-300">
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 bg-yellow-200 rounded-full flex items-center justify-center">
                        <span className="font-semibold text-yellow-700">
                          {pending.name?.charAt(0)?.toUpperCase() || pending.email?.charAt(0)?.toUpperCase() || '?'}
                        </span>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">
                          {pending.name || pending.email} ({statusText})
                        </p>
                        <p className="text-sm text-gray-500">{pending.email}</p>
                      </div>
                    </div>
                    <div className="flex items-center space-x-3">
                      {canRemovePending && (
                        <button
                          onClick={() => handleRemovePendingMember(pending.id, pending.name || pending.email)}
                          className="text-red-600 hover:text-red-700"
                          title="Remove pending member"
                        >
                          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
            <div className="mt-6 space-y-3">
              <button
                onClick={() => {
                  setShowMembersModal(false);
                  setShowInviteModal(true);
                }}
                className="w-full bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors font-medium"
              >
                + Invite Member
              </button>
              {!isAdmin && (
                <button
                  onClick={async () => {
                    setShowMembersModal(false);
                    await handleLeaveGroup();
                  }}
                  className="w-full bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors font-medium"
                >
                  Leave Group
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Edit Group Modal */}
      {showEditGroupModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Edit Group</h2>
            <form onSubmit={handleUpdateGroup}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Group Name
                </label>
                <input
                  type="text"
                  value={editGroupName}
                  onChange={(e) => setEditGroupName(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  placeholder="Enter group name"
                  required
                />
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Image URL (optional)
                </label>
                <input
                  type="url"
                  value={editGroupImageUrl}
                  onChange={(e) => setEditGroupImageUrl(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  placeholder="https://example.com/image.jpg"
                />
              </div>
              <div className="flex space-x-3">
                <button
                  type="button"
                  onClick={() => {
                    setShowEditGroupModal(false);
                    setEditGroupName('');
                    setEditGroupImageUrl('');
                  }}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updatingGroup}
                  className="flex-1 px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors disabled:opacity-50"
                >
                  {updatingGroup ? 'Updating...' : 'Update Group'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Settle Up Modal */}
      {showSettleUpModal && selectedSettlement && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Settle Up</h2>
            <div className="mb-4 p-3 bg-gray-50 rounded-lg">
              <p className="text-sm text-gray-600 mb-1">You owe</p>
              <p className="text-xl font-semibold text-gray-900">{selectedSettlement.toUserName}</p>
              <p className="text-2xl font-bold text-green-600 mt-2">${settleUpAmount}</p>
            </div>
            <form onSubmit={handleSettleUp}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Message (optional)
                </label>
                <textarea
                  value={settleUpMessage}
                  onChange={(e) => setSettleUpMessage(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  placeholder="Add a note..."
                  rows={3}
                />
              </div>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Screenshot URL (optional)
                </label>
                <input
                  type="url"
                  value={settleUpImageUrl}
                  onChange={(e) => setSettleUpImageUrl(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500"
                  placeholder="https://example.com/screenshot.jpg"
                />
              </div>
              <div className="flex space-x-3">
                <button
                  type="button"
                  onClick={() => {
                    setShowSettleUpModal(false);
                    setSelectedSettlement(null);
                    setSettleUpMessage('');
                    setSettleUpImageUrl('');
                    setSettleUpAmount('');
                  }}
                  className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={settlingUp}
                  className="flex-1 px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors disabled:opacity-50"
                >
                  {settlingUp ? 'Recording...' : 'Record Settlement'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default GroupDetail;
