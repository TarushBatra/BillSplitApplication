import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navbar from './components/Navbar';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import GroupDetail from './pages/GroupDetail';
import CreateGroup from './pages/CreateGroup';
import AddExpense from './pages/AddExpense';
import Settlements from './pages/Settlements';
import Profile from './pages/Profile';

function App() {
  return (
    <AuthProvider>
      <Router>
        <div className="min-h-screen bg-gray-50">
          <Navbar />
          <main className="container mx-auto px-4 py-8">
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/" element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              } />
              <Route path="/groups/create" element={
                <ProtectedRoute>
                  <CreateGroup />
                </ProtectedRoute>
              } />
              <Route path="/groups/:groupId" element={
                <ProtectedRoute>
                  <GroupDetail />
                </ProtectedRoute>
              } />
              <Route path="/groups/:groupId/expenses/add" element={
                <ProtectedRoute>
                  <AddExpense />
                </ProtectedRoute>
              } />
              <Route path="/groups/:groupId/settlements" element={
                <ProtectedRoute>
                  <Settlements />
                </ProtectedRoute>
              } />
              <Route path="/profile" element={
                <ProtectedRoute>
                  <Profile />
                </ProtectedRoute>
              } />
            </Routes>
          </main>
          <ToastContainer
            position="top-right"
            autoClose={5000}
            hideProgressBar={false}
            newestOnTop={false}
            closeOnClick
            rtl={false}
            pauseOnFocusLoss
            draggable
            pauseOnHover
          />
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;

