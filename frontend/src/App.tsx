import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import { CssBaseline } from '@mui/material';
import { serviceNowTheme } from './theme/serviceNowTheme';
import Layout from './components/layout/Layout';
import Login from './pages/auth/Login';
import Dashboard from './pages/dashboard/Dashboard';
import PlanList from './pages/plans/PlanList';
import PlanCreate from './pages/plans/PlanCreate';
import PlanDetails from './pages/plans/PlanDetails';
import TaskTemplateCreate from './pages/plans/TaskTemplateCreate';
import MyTasks from './pages/tasks/MyTasks';
import UserList from './pages/users/UserList';
import BuddyMatches from './pages/matches/BuddyMatches';
import Messages from './pages/messages/Messages';
import Feedback from './pages/feedback/Feedback';

function App() {
  // Get user from localStorage on every render to ensure it's up to date
  const getUserFromStorage = () => {
    const stored = localStorage.getItem('user');
    try {
      return stored ? JSON.parse(stored) : null;
    } catch (e) {
      console.error('Error parsing user from localStorage:', e);
      return null;
    }
  };

  const user = getUserFromStorage();
  const isAuthenticated = !!localStorage.getItem('authToken');

  return (
    <ThemeProvider theme={serviceNowTheme}>
      <CssBaseline />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          
          {isAuthenticated ? (
            <>
              <Route
                path="/*"
                element={
                  <Layout user={user}>
                    <Routes>
                      <Route path="/dashboard" element={<Dashboard />} />
                      <Route path="/users" element={<UserList />} />
                      <Route path="/plans" element={<PlanList />} />
                      <Route path="/plans/create" element={<PlanCreate />} />
                      <Route path="/plans/:planId" element={<PlanDetails />} />
                      <Route path="/plans/:planId/templates/create" element={<TaskTemplateCreate />} />
                      <Route path="/tasks" element={<MyTasks />} />
                      <Route path="/matches" element={<BuddyMatches />} />
                      <Route path="/messages" element={<Messages />} />
                      <Route path="/feedback" element={<Feedback />} />
                      <Route path="/" element={<Navigate to="/dashboard" replace />} />
                    </Routes>
                  </Layout>
                }
              />
            </>
          ) : (
            <Route path="*" element={<Navigate to="/login" replace />} />
          )}
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default App;
