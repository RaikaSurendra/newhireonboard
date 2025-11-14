import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Grid,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
} from '@mui/material';
import { Edit, Delete, Add } from '@mui/icons-material';
import { useNavigate, useParams } from 'react-router-dom';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Loader from '../../components/common/Loader';
import { planApi, Plan } from '../../api/planApi';
import { taskApi, TaskTemplate } from '../../api/taskApi';

const PlanDetails: React.FC = () => {
  const navigate = useNavigate();
  const { planId } = useParams<{ planId: string }>();
  const [loading, setLoading] = useState(true);
  const [plan, setPlan] = useState<Plan | null>(null);
  const [templates, setTemplates] = useState<TaskTemplate[]>([]);

  useEffect(() => {
    loadPlanDetails();
  }, [planId]);

  const loadPlanDetails = async () => {
    try {
      const [planResponse, templatesResponse] = await Promise.all([
        planApi.getById(Number(planId)),
        taskApi.getTemplatesByPlan(Number(planId)),
      ]);
      console.log('Plan response:', planResponse);
      console.log('Templates response:', templatesResponse);
      
      // Backend returns { success: true, data: {...} } or { success: true, data: [...] }
      setPlan(planResponse?.data?.data || planResponse?.data || null);
      const templatesData = templatesResponse?.data?.data || templatesResponse?.data || [];
      setTemplates(Array.isArray(templatesData) ? templatesData : []);
    } catch (error) {
      console.error('Failed to load plan details:', error);
      setPlan(null);
      setTemplates([]);
    } finally {
      setLoading(false);
    }
  };

  const handlePublish = async () => {
    try {
      await planApi.publish(Number(planId), 'Publishing plan');
      loadPlanDetails();
    } catch (error) {
      console.error('Failed to publish plan:', error);
    }
  };

  if (loading) {
    return <Loader message="Loading plan details..." />;
  }

  if (!plan) {
    return (
      <Box textAlign="center" py={4}>
        <Typography variant="h6">Plan not found</Typography>
        <Button onClick={() => navigate('/plans')} sx={{ mt: 2 }}>
          Back to Plans
        </Button>
      </Box>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="start" mb={3}>
        <Box>
          <Box display="flex" alignItems="center" gap={2} mb={1}>
            <Typography variant="h4" fontWeight={700}>
              {plan.name}
            </Typography>
            <Chip
              label={plan.isActive ? 'Active' : 'Draft'}
              color={plan.isActive ? 'success' : 'default'}
            />
            <Chip label={`v${plan.version}`} variant="outlined" />
          </Box>
          <Typography variant="body2" color="text.secondary">
            {plan.description}
          </Typography>
        </Box>
        <Box display="flex" gap={2}>
          {!plan.isActive && (
            <Button variant="contained" color="success" onClick={handlePublish}>
              Publish Plan
            </Button>
          )}
          <Button variant="outlined" onClick={() => navigate('/plans')}>
            Back to Plans
          </Button>
        </Box>
      </Box>

      {/* Plan Info */}
      <Grid container spacing={3} mb={3}>
        <Grid item xs={12} md={3}>
          <Card>
            <Box textAlign="center">
              <Typography variant="h4" color="primary" fontWeight={700}>
                {plan.durationDays}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Days Duration
              </Typography>
            </Box>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <Box textAlign="center">
              <Typography variant="h4" color="success.main" fontWeight={700}>
                {templates.length}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Task Templates
              </Typography>
            </Box>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <Box textAlign="center">
              <Typography variant="h4" color="info.main" fontWeight={700}>
                {plan.department}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Department
              </Typography>
            </Box>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <Box textAlign="center">
              <Typography variant="h4" color="warning.main" fontWeight={700}>
                {plan.usageCount || 0}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Times Used
              </Typography>
            </Box>
          </Card>
        </Grid>
      </Grid>

      {/* Task Templates */}
      <Card
        title="Task Templates"
        actions={
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={() => navigate(`/plans/${planId}/templates/create`)}
          >
            Add Template
          </Button>
        }
      >
        {templates.length === 0 ? (
          <Box textAlign="center" py={4}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              No task templates yet
            </Typography>
            <Button
              variant="contained"
              onClick={() => navigate(`/plans/${planId}/templates/create`)}
              sx={{ mt: 2 }}
            >
              Create First Template
            </Button>
          </Box>
        ) : (
          <TableContainer component={Paper} variant="outlined">
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell><strong>Task Name</strong></TableCell>
                  <TableCell><strong>Type</strong></TableCell>
                  <TableCell><strong>Owner</strong></TableCell>
                  <TableCell><strong>Assignee</strong></TableCell>
                  <TableCell><strong>Day</strong></TableCell>
                  <TableCell><strong>Priority</strong></TableCell>
                  <TableCell><strong>Execution</strong></TableCell>
                  <TableCell align="right"><strong>Actions</strong></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {templates.map((template) => (
                  <TableRow key={template.id} hover>
                    <TableCell>{template.name}</TableCell>
                    <TableCell>
                      <Chip label={template.taskType} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption">
                        {template.ownerType.replace(/_/g, ' ')}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption">
                        {template.assigneeType.replace(/_/g, ' ')}
                      </Typography>
                    </TableCell>
                    <TableCell>Day {template.dayOffset}</TableCell>
                    <TableCell>
                      <Chip
                        label={template.priority}
                        size="small"
                        color={
                          template.priority === 'URGENT' ? 'error' :
                          template.priority === 'HIGH' ? 'warning' :
                          template.priority === 'MEDIUM' ? 'info' : 'default'
                        }
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={template.executionMode}
                        size="small"
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="Edit">
                        <IconButton size="small" color="primary">
                          <Edit fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton size="small" color="error">
                          <Delete fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>
    </Box>
  );
};

export default PlanDetails;
