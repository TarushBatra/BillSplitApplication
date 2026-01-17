import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import { toast } from 'react-toastify';

const Dashboard = () => {
  const [groups, setGroups] = useState([]);
  const [invitations, setInvitations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchGroups();
    fetchInvitations();
  }, []);

  const fetchGroups = async () => {
    try {
      const response = await api.get('/groups');
      // Ensure response.data is an array
      if (Array.isArray(response.data)) {
        setGroups(response.data);
      } else {
        toast.error('Unexpected response format from server');
        setGroups([]);
      }
    } catch (error) {
      console.error('Failed to fetch groups:', error);
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to fetch groups';
      toast.error(errorMessage);
      setGroups([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchInvitations = async () => {
    try {
      const response = await api.get('/groups/invitations');
      if (Array.isArray(response.data)) {
        setInvitations(response.data);
      }
    } catch (error) {
      console.error('Failed to fetch invitations:', error);
      // Silently fail - invitations are optional
    }
  };

  const handleAcceptInvitation = async (invitationId) => {
    try {
      await api.post(`/groups/invitations/${invitationId}/accept`);
      toast.success('Invitation accepted! You have been added to the group.');
      fetchInvitations();
      fetchGroups(); // Refresh groups list
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to accept invitation';
      toast.error(errorMessage);
    }
  };

  const handleRejectInvitation = async (invitationId) => {
    try {
      await api.post(`/groups/invitations/${invitationId}/reject`);
      toast.success('Invitation rejected.');
      fetchInvitations();
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.response?.data?.message || 'Failed to reject invitation';
      toast.error(errorMessage);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-green-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-2 text-gray-600">Manage your expense groups and track balances</p>
      </div>

      {/* Pending Invitations Section */}
      {invitations.length > 0 && (
        <div className="mb-8">
          <h2 className="text-2xl font-semibold text-gray-900 mb-4">Pending Invitations</h2>
          <div className="space-y-3">
            {invitations.map((invitation) => (
              <div key={invitation.id} className="bg-yellow-50 border-l-4 border-yellow-400 rounded-lg p-4 shadow-sm">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3">
                      <div className="w-12 h-12 bg-yellow-200 rounded-full flex items-center justify-center">
                        <span className="font-semibold text-lg text-yellow-700">
                          {invitation.groupName?.charAt(0)?.toUpperCase() || '?'}
                        </span>
                      </div>
                      <div>
                        <p className="font-semibold text-gray-900">{invitation.groupName}</p>
                        <p className="text-sm text-gray-600">
                          Invited by <span className="font-medium">{invitation.inviterName}</span>
                          {invitation.invitedAt && (
                            <span className="text-gray-500">
                              {' • '}
                              {new Date(invitation.invitedAt).toLocaleDateString('en-US', { 
                                month: 'short', 
                                day: 'numeric',
                                year: 'numeric'
                              })}
                            </span>
                          )}
                        </p>
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center space-x-3">
                    <button
                      onClick={() => handleAcceptInvitation(invitation.id)}
                      className="bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 transition-colors font-medium"
                    >
                      Accept
                    </button>
                    <button
                      onClick={() => handleRejectInvitation(invitation.id)}
                      className="bg-red-600 text-white px-4 py-2 rounded-md hover:bg-red-700 transition-colors font-medium"
                    >
                      Reject
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {Array.isArray(groups) && groups.map((group) => (
          <div key={group.id} className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
            <h3 className="text-xl font-semibold text-gray-900 mb-2">{group.name}</h3>
            <p className="text-gray-600 mb-4">
              Created by {group.createdBy?.name || 'Unknown'}
            </p>
            <div className="flex space-x-2">
              <Link
                to={`/groups/${group.id}`}
                className="flex-1 bg-green-600 text-white text-center py-2 px-4 rounded-md hover:bg-green-700 transition-colors"
              >
                View Group
              </Link>
              <Link
                to={`/groups/${group.id}/settlements`}
                className="flex-1 bg-green-600 text-white text-center py-2 px-4 rounded-md hover:bg-green-700 transition-colors"
              >
                Settlements
              </Link>
            </div>
          </div>
        ))}
      </div>

      {groups.length === 0 && (
        <div className="text-center py-12">
          <div className="text-gray-400 text-6xl mb-4">📊</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">No groups yet</h3>
          <p className="text-gray-600 mb-6">Create your first group to start splitting expenses</p>
          <Link
            to="/groups/create"
            className="bg-green-600 text-white px-6 py-3 rounded-md hover:bg-green-700 transition-colors"
          >
            Create Group
          </Link>
        </div>
      )}
    </div>
  );
};

export default Dashboard;

