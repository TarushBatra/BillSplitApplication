import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      api.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      fetchUser();
    } else {
      setLoading(false);
    }
  }, []);

  const fetchUser = async () => {
    try {
      const response = await api.get('/auth/me');
      setUser(response.data);
    } catch (error) {
      console.error('Failed to fetch user:', error);
      localStorage.removeItem('token');
      delete api.defaults.headers.common['Authorization'];
    } finally {
      setLoading(false);
    }
  };

  const login = async (email, password) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      const { accessToken, userId, name, email: userEmail } = response.data;
      
      localStorage.setItem('token', accessToken);
      api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
      
      setUser({ id: userId, name, email: userEmail });
      return { success: true };
    } catch (error) {
      const apiError = error.response?.data;
      return {
        success: false,
        error: apiError?.error || apiError?.message || error.message || 'Login failed',
      };
    }
  };

  const register = async (name, email, password) => {
    try {
      const response = await api.post('/auth/register', { name, email, password });
      const { accessToken, userId, name: userName, email: userEmail } = response.data;
      
      localStorage.setItem('token', accessToken);
      api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
      
      setUser({ id: userId, name: userName, email: userEmail });
      return { success: true };
    } catch (error) {
      const apiError = error.response?.data;
      return {
        success: false,
        error: apiError?.error || apiError?.message || error.message || 'Registration failed',
      };
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    delete api.defaults.headers.common['Authorization'];
    setUser(null);
  };

  const updateProfile = async (name, email) => {
    try {
      const response = await api.put('/auth/profile', { name, email });
      setUser(response.data);
      return { success: true };
    } catch (error) {
      const apiError = error.response?.data;
      throw new Error(apiError?.error || apiError?.message || error.message || 'Failed to update profile');
    }
  };

  const value = {
    user,
    login,
    register,
    logout,
    updateProfile,
    fetchUser,
    loading
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

