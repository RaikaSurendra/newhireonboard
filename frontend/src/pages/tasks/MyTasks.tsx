import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Grid,
  Chip,
  LinearProgress,
  Card as MuiCard,
  CardContent,
  CardActions,
  Tabs,
  Tab,
} from '@mui/material';
import { CheckCircle, Schedule, Block } from '@mui/icons-material';
import Button from '../../components/common/Button';
import Loader from '../../components/common/Loader';
import { taskApi, Task } from '../../api/taskApi';

const MyTasks: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [activeTab, setActiveTab] = useState(0);

  useEffect(() => {
    loadTasks();
  }, []);

  const loadTasks = async () => {
    try {
      const response = await taskApi.getMyTasks();
      // Backend returns { success: true, data: [...] }
      // Axios wraps this in response.data, so we need response.data.data
      const tasksData = response?.data?.data || [];
      setTasks(Array.isArray(tasksData) ? tasksData : []);
    } catch (error) {
      console.error('Failed to load tasks:', error);
      setTasks([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCompleteTask = async (taskId: number) => {
    try {
      await taskApi.completeTask(taskId);
      loadTasks();
    } catch (error) {
      console.error('Failed to complete task:', error);
    }
  };

  const filteredTasks = tasks.filter((task) => {
    if (activeTab === 0) return task.status === 'PENDING' || task.status === 'IN_PROGRESS';
    if (activeTab === 1) return task.status === 'COMPLETED';
    if (activeTab === 2) return task.status === 'BLOCKED';
    return true;
  });

  const stats = {
    total: tasks.length,
    pending: tasks.filter(t => t.status === 'PENDING').length,
    inProgress: tasks.filter(t => t.status === 'IN_PROGRESS').length,
    completed: tasks.filter(t => t.status === 'COMPLETED').length,
    blocked: tasks.filter(t => t.status === 'BLOCKED').length,
  };

  const completionPercentage = stats.total > 0 ? (stats.completed / stats.total) * 100 : 0;

  if (loading) {
    return <Loader message="Loading your tasks..." />;
  }

  return (
    <Box>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        My Tasks
      </Typography>
      <Typography variant="body2" color="text.secondary" mb={3}>
        Manage your onboarding tasks and track progress
      </Typography>

      {/* Stats */}
      <Grid container spacing={3} mb={3}>
        <Grid item xs={12} sm={6} md={3}>
          <MuiCard>
            <CardContent>
              <Box textAlign="center">
                <Typography variant="h3" color="primary" fontWeight={700}>
                  {stats.total}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Total Tasks
                </Typography>
              </Box>
            </CardContent>
          </MuiCard>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <MuiCard>
            <CardContent>
              <Box textAlign="center">
                <Typography variant="h3" color="warning.main" fontWeight={700}>
                  {stats.pending}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Pending
                </Typography>
              </Box>
            </CardContent>
          </MuiCard>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <MuiCard>
            <CardContent>
              <Box textAlign="center">
                <Typography variant="h3" color="info.main" fontWeight={700}>
                  {stats.inProgress}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  In Progress
                </Typography>
              </Box>
            </CardContent>
          </MuiCard>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <MuiCard>
            <CardContent>
              <Box textAlign="center">
                <Typography variant="h3" color="success.main" fontWeight={700}>
                  {stats.completed}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Completed
                </Typography>
              </Box>
            </CardContent>
          </MuiCard>
        </Grid>
      </Grid>

      {/* Progress Bar */}
      <MuiCard sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" justifyContent="space-between" mb={1}>
            <Typography variant="body2" fontWeight={600}>
              Overall Progress
            </Typography>
            <Typography variant="body2" fontWeight={600}>
              {completionPercentage.toFixed(0)}%
            </Typography>
          </Box>
          <LinearProgress
            variant="determinate"
            value={completionPercentage}
            sx={{ height: 10, borderRadius: 5 }}
          />
          <Typography variant="caption" color="text.secondary" mt={1}>
            {stats.completed} of {stats.total} tasks completed
          </Typography>
        </CardContent>
      </MuiCard>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={activeTab} onChange={(_, newValue) => setActiveTab(newValue)}>
          <Tab label={`Active (${stats.pending + stats.inProgress})`} />
          <Tab label={`Completed (${stats.completed})`} />
          <Tab label={`Blocked (${stats.blocked})`} />
        </Tabs>
      </Box>

      {/* Task List */}
      <Grid container spacing={3}>
        {filteredTasks.length === 0 ? (
          <Grid item xs={12}>
            <MuiCard>
              <CardContent>
                <Box textAlign="center" py={4}>
                  <Typography variant="h6" color="text.secondary">
                    No tasks in this category
                  </Typography>
                </Box>
              </CardContent>
            </MuiCard>
          </Grid>
        ) : (
          filteredTasks.map((task) => (
            <Grid item xs={12} md={6} key={task.id}>
              <MuiCard
                sx={{
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  border: task.status === 'BLOCKED' ? '2px solid' : 'none',
                  borderColor: 'error.main',
                }}
              >
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box display="flex" justifyContent="space-between" alignItems="start" mb={2}>
                    <Typography variant="h6" fontWeight={600}>
                      {task.title}
                    </Typography>
                    <Chip
                      label={task.priority}
                      size="small"
                      color={
                        task.priority === 'URGENT' ? 'error' :
                        task.priority === 'HIGH' ? 'warning' :
                        task.priority === 'MEDIUM' ? 'info' : 'default'
                      }
                    />
                  </Box>

                  <Typography variant="body2" color="text.secondary" mb={2}>
                    {task.description}
                  </Typography>

                  <Box display="flex" gap={1} flexWrap="wrap" mb={2}>
                    <Chip
                      icon={
                        task.status === 'COMPLETED' ? <CheckCircle /> :
                        task.status === 'BLOCKED' ? <Block /> :
                        <Schedule />
                      }
                      label={task.status}
                      size="small"
                      color={
                        task.status === 'COMPLETED' ? 'success' :
                        task.status === 'BLOCKED' ? 'error' :
                        task.status === 'IN_PROGRESS' ? 'info' : 'default'
                      }
                    />
                    {task.dueDate && (
                      <Chip
                        label={`Due: ${new Date(task.dueDate).toLocaleDateString()}`}
                        size="small"
                        variant="outlined"
                      />
                    )}
                  </Box>
                </CardContent>

                <CardActions>
                  {task.status === 'PENDING' && (
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => taskApi.updateTaskStatus(task.id, 'IN_PROGRESS')}
                    >
                      Start Task
                    </Button>
                  )}
                  {task.status === 'IN_PROGRESS' && (
                    <Button
                      size="small"
                      variant="contained"
                      color="success"
                      onClick={() => handleCompleteTask(task.id)}
                    >
                      Complete
                    </Button>
                  )}
                  {task.status === 'BLOCKED' && (
                    <Typography variant="caption" color="error">
                      ⚠️ Waiting for previous task to complete
                    </Typography>
                  )}
                </CardActions>
              </MuiCard>
            </Grid>
          ))
        )}
      </Grid>
    </Box>
  );
};

export default MyTasks;
