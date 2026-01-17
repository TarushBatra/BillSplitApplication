import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../services/api';
import { toast } from 'react-toastify';

const Settlements = () => {
  const { groupId } = useParams();
  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    fetchSettlements();
  }, [groupId]);

  const fetchSettlements = async () => {
    try {
      const response = await api.get(`/settlements/group/${groupId}`);
      setSettlements(response.data);
    } catch (error) {
      toast.error('Failed to fetch settlements');
    } finally {
      setLoading(false);
    }
  };

  const handleProcessSettlements = async () => {
    setProcessing(true);
    try {
      await api.post(`/settlements/group/${groupId}/process`);
      toast.success('Settlement notifications sent successfully!');
    } catch (error) {
      toast.error('Failed to process settlements');
    } finally {
      setProcessing(false);
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
    <div className="max-w-4xl mx-auto">
      <div className="mb-8">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Settlements</h1>
            <p className="mt-2 text-gray-600">Optimal transactions to settle all debts</p>
          </div>
          <Link
            to={`/groups/${groupId}`}
            className="bg-gray-600 text-white px-4 py-2 rounded-md hover:bg-gray-700 transition-colors"
          >
            Back to Group
          </Link>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        {settlements.length === 0 ? (
          <div className="text-center py-12">
            <div className="text-gray-400 text-6xl mb-4">✅</div>
            <h3 className="text-xl font-semibold text-gray-900 mb-2">All settled up!</h3>
            <p className="text-gray-600">No outstanding debts in this group</p>
          </div>
        ) : (
          <>
            <div className="mb-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-4">
                Settlement Transactions ({settlements.length})
              </h2>
              <div className="space-y-4">
                {settlements.map((settlement, index) => (
                  <div key={index} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                    <div className="flex items-center space-x-4">
                      <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
                        <span className="text-red-600 font-semibold">
                          {settlement.fromUserName.charAt(0)}
                        </span>
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">{settlement.fromUserName}</p>
                        <p className="text-sm text-gray-500">owes</p>
                      </div>
                    </div>
                    
                    <div className="text-center">
                      <p className="text-2xl font-bold text-gray-900">
                        ${settlement.amount}
                      </p>
                    </div>
                    
                    <div className="flex items-center space-x-4">
                      <div>
                        <p className="font-medium text-gray-900">{settlement.toUserName}</p>
                        <p className="text-sm text-gray-500">should receive</p>
                      </div>
                      <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                        <span className="text-green-600 font-semibold">
                          {settlement.toUserName.charAt(0)}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="border-t pt-6">
              <div className="flex justify-between items-center">
                <div>
                  <h3 className="font-medium text-gray-900">Process Settlements</h3>
                  <p className="text-sm text-gray-500">
                    Send email notifications to all group members
                  </p>
                </div>
                <button
                  onClick={handleProcessSettlements}
                  disabled={processing}
                  className="bg-green-600 text-white px-6 py-2 rounded-md hover:bg-green-700 transition-colors disabled:opacity-50"
                >
                  {processing ? 'Processing...' : 'Send Notifications'}
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default Settlements;

