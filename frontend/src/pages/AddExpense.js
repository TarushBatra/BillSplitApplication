import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { toast } from 'react-toastify';
import { useAuth } from '../contexts/AuthContext';

const AddExpense = () => {
  const { groupId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [formData, setFormData] = useState({
    description: '',
    amount: '',
    splitType: 'EQUAL',
    paidBy: '',
    shares: [],
    pendingShares: []
  });
  const [members, setMembers] = useState([]);
  const [pendingMembers, setPendingMembers] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchMembers = useCallback(async () => {
    try {
      const timestamp = new Date().getTime();
      const [membersRes, pendingRes] = await Promise.all([
        api.get(`/groups/${groupId}/members?t=${timestamp}`),
        api.get(`/groups/${groupId}/pending-members?t=${timestamp}`).catch(() => ({ data: [] }))
      ]);
      setMembers(Array.isArray(membersRes.data) ? membersRes.data : []);
      setPendingMembers(Array.isArray(pendingRes.data) ? pendingRes.data : []);
      
      // Set default paid by to current user
      if (!formData.paidBy && user?.id) {
        const currentUserMember = membersRes.data?.find(m => m.user?.id === user.id);
        if (currentUserMember) {
          setFormData(prev => ({ ...prev, paidBy: `user-${user.id}` }));
        }
      }
    } catch (error) {
      toast.error('Failed to fetch group members');
    }
  }, [groupId, user?.id, formData.paidBy]);

  useEffect(() => {
    fetchMembers();
  }, [groupId]);

  // Refresh on focus
  useEffect(() => {
    const handleFocus = () => {
      fetchMembers();
    };
    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [fetchMembers]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value
    });
  };

  const handleShareChange = (userId, amount) => {
    const shares = [...formData.shares];
    const existingIndex = shares.findIndex(share => share.userId === userId);
    
    if (existingIndex >= 0) {
      shares[existingIndex].amountOwed = parseFloat(amount) || 0;
    } else {
      shares.push({ userId, amountOwed: parseFloat(amount) || 0 });
    }
    
    setFormData({ ...formData, shares });
  };

  const handlePendingShareChange = (email, amount) => {
    const pendingShares = [...formData.pendingShares];
    const existingIndex = pendingShares.findIndex(share => share.email === email);
    
    if (existingIndex >= 0) {
      pendingShares[existingIndex].amountOwed = parseFloat(amount) || 0;
    } else {
      pendingShares.push({ email, amountOwed: parseFloat(amount) || 0 });
    }
    
    setFormData({ ...formData, pendingShares });
  };

  const calculateEqualShare = () => {
    const totalPeople = members.length + pendingMembers.length;
    if (totalPeople === 0) return 0;
    return (parseFloat(formData.amount) || 0) / totalPeople;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const expenseData = {
        groupId: parseInt(groupId),
        description: formData.description,
        amount: parseFloat(formData.amount),
        splitType: formData.splitType,
        shares: formData.splitType === 'CUSTOM' ? formData.shares : [],
        pendingShares: formData.splitType === 'CUSTOM' ? formData.pendingShares : []
      };

      // Handle paid by
      if (formData.paidBy) {
        if (formData.paidBy.startsWith('user-')) {
          const userId = parseInt(formData.paidBy.replace('user-', ''));
          expenseData.paidByUserId = userId;
        } else if (formData.paidBy.startsWith('pending-')) {
          const email = formData.paidBy.replace('pending-', '');
          expenseData.paidByPendingMemberEmail = email;
        }
      }

      await api.post('/expenses', expenseData);
      toast.success('Expense added successfully!');
      navigate(`/groups/${groupId}`);
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to add expense';
      toast.error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Add Expense</h1>
        <p className="mt-2 text-gray-600">Add a new expense to the group</p>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-2">
              Description
            </label>
            <input
              type="text"
              id="description"
              name="description"
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-green-500 focus:border-green-500"
              placeholder="What was this expense for?"
              value={formData.description}
              onChange={handleChange}
            />
          </div>

          <div>
            <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-2">
              Amount
            </label>
            <input
              type="number"
              id="amount"
              name="amount"
              step="0.01"
              min="0.01"
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-green-500 focus:border-green-500"
              placeholder="0.00"
              value={formData.amount}
              onChange={handleChange}
            />
          </div>

          <div>
            <label htmlFor="paidBy" className="block text-sm font-medium text-gray-700 mb-2">
              Paid By
            </label>
            <select
              id="paidBy"
              name="paidBy"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-green-500 focus:border-green-500"
              value={formData.paidBy}
              onChange={handleChange}
              required
            >
              <option value="">Select who paid</option>
              {members.map((member) => (
                <option key={member.id} value={`user-${member.user.id}`}>
                  {member.user.name} {member.user.id === user?.id && '(You)'}
                </option>
              ))}
              {pendingMembers.map((pending) => (
                <option key={pending.id || pending.email} value={`pending-${pending.email}`}>
                  {pending.name || pending.email} (Pending)
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="splitType" className="block text-sm font-medium text-gray-700 mb-2">
              Split Type
            </label>
            <select
              id="splitType"
              name="splitType"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-green-500 focus:border-green-500"
              value={formData.splitType}
              onChange={handleChange}
            >
              <option value="EQUAL">Equal Split</option>
              <option value="CUSTOM">Custom Split</option>
            </select>
          </div>

          {formData.splitType === 'EQUAL' && (
            <div className="bg-gray-50 p-4 rounded-md">
              <h3 className="font-medium text-gray-900 mb-2">Equal Split</h3>
              <p className="text-sm text-gray-600">
                Each member will owe: <span className="font-semibold">${calculateEqualShare().toFixed(2)}</span>
              </p>
            </div>
          )}

          {formData.splitType === 'CUSTOM' && (
            <div>
              <h3 className="font-medium text-gray-900 mb-4">Custom Split</h3>
              <div className="space-y-3">
                {members.map((member) => (
                  <div key={member.id} className="flex items-center justify-between">
                    <span className="text-gray-700">
                      {member.user.name} {member.user.id === user?.id && '(You)'}
                    </span>
                    <div className="flex items-center space-x-2">
                      <span className="text-gray-500">$</span>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        className="w-24 px-2 py-1 border border-gray-300 rounded text-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                        placeholder="0.00"
                        onChange={(e) => handleShareChange(member.user.id, e.target.value)}
                      />
                    </div>
                  </div>
                ))}
                {pendingMembers.map((pending) => (
                  <div key={pending.id || pending.email} className="flex items-center justify-between bg-yellow-50 p-2 rounded">
                    <span className="text-gray-700">
                      {pending.name || pending.email} <span className="text-yellow-600 text-sm">(Pending)</span>
                    </span>
                    <div className="flex items-center space-x-2">
                      <span className="text-gray-500">$</span>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        className="w-24 px-2 py-1 border border-gray-300 rounded text-sm focus:outline-none focus:ring-green-500 focus:border-green-500"
                        placeholder="0.00"
                        onChange={(e) => handlePendingShareChange(pending.email, e.target.value)}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className="flex space-x-4">
            <button
              type="button"
              onClick={() => navigate(`/groups/${groupId}`)}
              className="flex-1 bg-gray-300 text-gray-700 py-2 px-4 rounded-md hover:bg-gray-400 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 bg-green-600 text-white py-2 px-4 rounded-md hover:bg-green-700 transition-colors disabled:opacity-50"
            >
              {loading ? 'Adding...' : 'Add Expense'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddExpense;

