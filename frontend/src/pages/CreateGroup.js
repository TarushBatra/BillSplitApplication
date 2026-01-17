import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { toast } from 'react-toastify';

const CreateGroup = () => {
  const [formData, setFormData] = useState({
    name: '',
    memberEmails: ''
  });
  const [loading, setLoading] = useState(false);
  
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const memberEmails = formData.memberEmails
        .split(',')
        .map(email => email.trim())
        .filter(email => email.length > 0);

      const response = await api.post('/groups', {
        name: formData.name,
        memberEmails: memberEmails
      });

      toast.success('Group created successfully!');
      navigate(`/groups/${response.data.id}`);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to create group');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Create New Group</h1>
        <p className="mt-2 text-gray-600">Create a group to start splitting expenses with friends</p>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
              Group Name
            </label>
            <input
              type="text"
              id="name"
              name="name"
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-green-500 focus:border-green-500"
              placeholder="Enter group name"
              value={formData.name}
              onChange={handleChange}
            />
          </div>

          <div>
            <label htmlFor="memberEmails" className="block text-sm font-medium text-gray-700 mb-2">
              Member Emails (Optional)
            </label>
            <textarea
              id="memberEmails"
              name="memberEmails"
              rows={4}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-green-500 focus:border-green-500"
              placeholder="Enter email addresses separated by commas (e.g., john@example.com, jane@example.com)"
              value={formData.memberEmails}
              onChange={handleChange}
            />
            <p className="mt-1 text-sm text-gray-500">
              Invite members by entering their email addresses. They will receive an invitation email.
            </p>
          </div>

          <div className="flex space-x-4">
            <button
              type="button"
              onClick={() => navigate('/')}
              className="flex-1 bg-gray-300 text-gray-700 py-2 px-4 rounded-md hover:bg-gray-400 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 bg-green-600 text-white py-2 px-4 rounded-md hover:bg-green-700 transition-colors disabled:opacity-50"
            >
              {loading ? 'Creating...' : 'Create Group'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateGroup;

