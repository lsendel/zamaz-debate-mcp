import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider } from 'react-redux';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider } from 'antd';
import { store } from './store';
import { useAppSelector, useAppDispatch } from './store';
import { antdTheme } from './styles';
import 'antd/dist/reset.css';

// Components
import Layout from './components/Layout';
import LoginPage from './components/LoginPage';
import DebatesPage from './components/DebatesPage';
import DebateDetailPage from './components/DebateDetailPage';
import WorkflowEditorPage from './components/WorkflowEditorPage';
import AnalyticsPage from './components/AnalyticsPage';
import SettingsPage from './components/SettingsPage';
import OrganizationManagementPage from './components/OrganizationManagementPage';
import NotificationSnackbar from './components/NotificationSnackbar';

// Actions
import { fetchOrganizations } from './store/slices/organizationSlice';

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: 1,
    },
  },
});

function AppContent() {
  const dispatch = useAppDispatch();
  const { isAuthenticated } = useAppSelector(state => state.auth);

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchOrganizations());
    }
  }, [isAuthenticated, dispatch]);

  return (
    <>
      <Routes>
        <Route path='/login' element={!isAuthenticated ? <LoginPage /> : <Navigate to='/' />} />
        <Route path='/' element={isAuthenticated ? <Layout /> : <Navigate to='/login' />}>
          <Route index element={<DebatesPage />} />
          <Route path='debates' element={<DebatesPage />} />
          <Route path='debates/:id' element={<DebateDetailPage />} />
          <Route path='workflow-editor' element={<WorkflowEditorPage />} />
          <Route path='analytics' element={<AnalyticsPage />} />
          <Route path='settings' element={<SettingsPage />} />
          <Route path='organization-management' element={<OrganizationManagementPage />} />
        </Route>
      </Routes>
      <NotificationSnackbar />
    </>
  );
}

function App() {
  return (
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <ConfigProvider theme={antdTheme}>
          <Router>
            <AppContent />
          </Router>
        </ConfigProvider>
      </QueryClientProvider>
    </Provider>
  );
}

export default App;
