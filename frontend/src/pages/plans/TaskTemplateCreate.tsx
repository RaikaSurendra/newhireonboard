import React, { useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  MenuItem,
  Grid,
  Alert,
  FormControl,
  InputLabel,
  Select,
  Chip,
  OutlinedInput,
} from '@mui/material';
import { useNavigate, useParams } from 'react-router-dom';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import { taskApi, CreateTaskTemplateRequest } from '../../api/taskApi';

const priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
const taskTypes = ['ADMINISTRATIVE', 'TECHNICAL', 'COMPLIANCE', 'SOCIAL', 'PROJECT', 'REVIEW'];
const ownerTypes = ['MANAGER_OWNED', 'NEW_HIRE_OWNED', 'INTERNAL_TEAM_OWNED', 'INTERNAL_EMPLOYEE_OWNED', 'EXTERNAL_TEAM_OWNED', 'SHARED_OWNERSHIP'];
const assigneeTypes = ['NEW_EMPLOYEE', 'BUDDY', 'MANAGER', 'HR_TEAM', 'IT_TEAM', 'TEAM_MEMBER', 'TEAM', 'EXTERNAL_VENDOR'];
const executionModes = ['SEQUENTIAL', 'PARALLEL'];

const TaskTemplateCreate: React.FC = () => {
  const navigate = useNavigate();
  const { planId } = useParams<{ planId: string }>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState<CreateTaskTemplateRequest>({
    name: '',
    description: '',
    priority: 'MEDIUM',
    dayOffset: 0,
    estimatedDuration: 1,
    taskType: 'ADMINISTRATIVE',
    ownerType: 'MANAGER_OWNED',
    assigneeType: 'NEW_EMPLOYEE',
    executionMode: 'PARALLEL',
    category: '',
    tags: [],
  });

  const handleChange = (field: keyof CreateTaskTemplateRequest, value: any) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await taskApi.createTemplate(Number(planId), formData);
      navigate(`/plans/${planId}`);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to create task template');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Box mb={3}>
        <Typography variant="h4" fontWeight={700} gutterBottom>
          Create Task Template
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Add a new task template to the onboarding plan
        </Typography>
      </Box>

      <Card>
        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            {/* Basic Information */}
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom>
                Basic Information
              </Typography>
            </Grid>

            <Grid item xs={12} md={8}>
              <TextField
                fullWidth
                label="Task Name"
                required
                value={formData.name}
                onChange={(e) => handleChange('name', e.target.value)}
                placeholder="e.g., Complete IT Setup"
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                select
                label="Priority"
                required
                value={formData.priority}
                onChange={(e) => handleChange('priority', e.target.value)}
              >
                {priorities.map((priority) => (
                  <MenuItem key={priority} value={priority}>
                    {priority}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                required
                multiline
                rows={3}
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                placeholder="Describe what needs to be done..."
              />
            </Grid>

            {/* Task Classification */}
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Task Classification
              </Typography>
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                select
                label="Task Type"
                required
                value={formData.taskType}
                onChange={(e) => handleChange('taskType', e.target.value)}
              >
                {taskTypes.map((type) => (
                  <MenuItem key={type} value={type}>
                    {type}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                select
                label="Owner Type"
                required
                value={formData.ownerType}
                onChange={(e) => handleChange('ownerType', e.target.value)}
              >
                {ownerTypes.map((type) => (
                  <MenuItem key={type} value={type}>
                    {type.replace(/_/g, ' ')}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                select
                label="Assignee Type"
                required
                value={formData.assigneeType}
                onChange={(e) => handleChange('assigneeType', e.target.value)}
              >
                {assigneeTypes.map((type) => (
                  <MenuItem key={type} value={type}>
                    {type.replace(/_/g, ' ')}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            {/* Timing & Execution */}
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Timing & Execution
              </Typography>
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Day Offset"
                type="number"
                required
                value={formData.dayOffset}
                onChange={(e) => handleChange('dayOffset', parseInt(e.target.value))}
                helperText="Days after onboarding start (negative for pre-boarding)"
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                label="Estimated Duration (days)"
                type="number"
                required
                value={formData.estimatedDuration}
                onChange={(e) => handleChange('estimatedDuration', parseInt(e.target.value))}
                InputProps={{ inputProps: { min: 1 } }}
              />
            </Grid>

            <Grid item xs={12} md={4}>
              <TextField
                fullWidth
                select
                label="Execution Mode"
                required
                value={formData.executionMode}
                onChange={(e) => handleChange('executionMode', e.target.value)}
              >
                {executionModes.map((mode) => (
                  <MenuItem key={mode} value={mode}>
                    {mode}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>

            {formData.executionMode === 'SEQUENTIAL' && (
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Sequence Order"
                  type="number"
                  value={formData.sequenceOrder || ''}
                  onChange={(e) => handleChange('sequenceOrder', parseInt(e.target.value))}
                  helperText="Order in sequence (1, 2, 3...)"
                />
              </Grid>
            )}

            {formData.executionMode === 'PARALLEL' && (
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Parallel Group"
                  value={formData.parallelGroup || ''}
                  onChange={(e) => handleChange('parallelGroup', e.target.value)}
                  helperText="Group ID for parallel tasks"
                />
              </Grid>
            )}

            {/* Additional Details */}
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Additional Details
              </Typography>
            </Grid>

            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Category"
                value={formData.category}
                onChange={(e) => handleChange('category', e.target.value)}
                placeholder="e.g., IT Setup, Training"
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Tags</InputLabel>
                <Select
                  multiple
                  value={formData.tags || []}
                  onChange={(e) => handleChange('tags', e.target.value)}
                  input={<OutlinedInput label="Tags" />}
                  renderValue={(selected) => (
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                      {(selected as string[]).map((value) => (
                        <Chip key={value} label={value} size="small" />
                      ))}
                    </Box>
                  )}
                >
                  {['compliance', 'security', 'training', 'setup', 'mandatory', 'optional'].map((tag) => (
                    <MenuItem key={tag} value={tag}>
                      {tag}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            {/* Actions */}
            <Grid item xs={12}>
              <Box display="flex" gap={2} justifyContent="flex-end" mt={2}>
                <Button
                  variant="outlined"
                  onClick={() => navigate(`/plans/${planId}`)}
                  disabled={loading}
                >
                  Cancel
                </Button>
                <Button
                  variant="contained"
                  type="submit"
                  loading={loading}
                >
                  Create Template
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      </Card>
    </Box>
  );
};

export default TaskTemplateCreate;
