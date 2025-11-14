import React, { useEffect, useState } from 'react';
import {
  Grid,
  Typography,
  Box,
  LinearProgress,
} from '@mui/material';
import Card from '../../components/common/Card';
import Loader from '../../components/common/Loader';

interface DashboardStats {
  totalTasks: number;
  completedTasks: number;
  pendingTasks: number;
  upcomingTasks: number;
  completionPercentage: number;
}

const Dashboard: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DashboardStats>({
    totalTasks: 0,
    completedTasks: 0,
    pendingTasks: 0,
    upcomingTasks: 0,
    completionPercentage: 0,
  });

  useEffect(() => {
    // Simulate API call
    setTimeout(() => {
      setStats({
        totalTasks: 25,
        completedTasks: 15,
        pendingTasks: 8,
        upcomingTasks: 2,
        completionPercentage: 60,
      });
      setLoading(false);
    }, 1000);
  }, []);

  if (loading) {
    return <Loader message="Loading dashboard..." />;
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Dashboard
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Welcome back! Here's your onboarding progress
      </Typography>

      <Grid container spacing={3}>
        {/* Stats Cards */}
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <Box textAlign="center">
              <Typography variant="h3" color="primary" fontWeight={700}>
                {stats.totalTasks}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Total Tasks
              </Typography>
            </Box>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <Box textAlign="center">
              <Typography variant="h3" color="success.main" fontWeight={700}>
                {stats.completedTasks}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Completed
              </Typography>
            </Box>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <Box textAlign="center">
              <Typography variant="h3" color="warning.main" fontWeight={700}>
                {stats.pendingTasks}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Pending
              </Typography>
            </Box>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <Box textAlign="center">
              <Typography variant="h3" color="info.main" fontWeight={700}>
                {stats.upcomingTasks}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Upcoming
              </Typography>
            </Box>
          </Card>
        </Grid>

        {/* Progress Card */}
        <Grid item xs={12}>
          <Card title="Onboarding Progress">
            <Box>
              <Box display="flex" justifyContent="space-between" mb={1}>
                <Typography variant="body2">Overall Completion</Typography>
                <Typography variant="body2" fontWeight={600}>
                  {stats.completionPercentage}%
                </Typography>
              </Box>
              <LinearProgress
                variant="determinate"
                value={stats.completionPercentage}
                sx={{ height: 10, borderRadius: 5 }}
              />
              <Typography variant="caption" color="text.secondary" mt={1}>
                {stats.completedTasks} of {stats.totalTasks} tasks completed
              </Typography>
            </Box>
          </Card>
        </Grid>

        {/* Recent Tasks */}
        <Grid item xs={12} md={6}>
          <Card title="Recent Tasks">
            <Typography variant="body2" color="text.secondary">
              No recent tasks to display
            </Typography>
          </Card>
        </Grid>

        {/* Upcoming Meetings */}
        <Grid item xs={12} md={6}>
          <Card title="Upcoming Meetings">
            <Typography variant="body2" color="text.secondary">
              No upcoming meetings scheduled
            </Typography>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
