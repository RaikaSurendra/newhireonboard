import React, { useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  MenuItem,
  Alert,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import { planApi, CreatePlanRequest } from '../../api/planApi';

const departments = [
  'Engineering',
  'Product',
  'Design',
  'Marketing',
  'Sales',
  'HR',
  'Finance',
  'Operations',
  'Customer Success',
];

const PlanCreate: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [formData, setFormData] = useState<CreatePlanRequest>({
    name: '',
    description: '',
    department: '',
    durationDays: 90,
  });

  const handleChange = (field: keyof CreatePlanRequest, value: string | number) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await planApi.create(formData);
      console.log('Create plan response:', response);
      // Backend returns { success: true, planId: X, message: "..." }
      const planId = (response as any)?.data?.planId || (response as any)?.data?.data?.id;
      if (planId) {
        // Navigate to task template creation page
        navigate(`/plans/${planId}/templates/create`);
      } else {
        setError('Plan created but could not get plan ID');
      }
    } catch (err: any) {
      console.error('Create plan error:', err);
      setError(err.response?.data?.error || 'Failed to create plan');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Box mb={3}>
        <Typography variant="h4" fontWeight={700} gutterBottom>
          Create Onboarding Plan
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Create a new onboarding plan with task templates
        </Typography>
      </Box>

      <Card>
        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        <form onSubmit={handleSubmit}>
          <Box display="flex" flexDirection="column" gap={3}>
            <TextField
              fullWidth
              label="Plan Name"
              required
              value={formData.name}
              onChange={(e) => handleChange('name', e.target.value)}
              placeholder="e.g., Engineering 90-Day Onboarding"
              helperText="A descriptive name for this onboarding plan"
            />

            <TextField
              fullWidth
              label="Description"
              required
              multiline
              rows={4}
              value={formData.description}
              onChange={(e) => handleChange('description', e.target.value)}
              placeholder="Describe the purpose and goals of this onboarding plan..."
              helperText="Provide details about what this plan covers"
            />

            <TextField
              fullWidth
              select
              label="Department"
              required
              value={formData.department}
              onChange={(e) => handleChange('department', e.target.value)}
              helperText="Select the department this plan is for"
            >
              {departments.map((dept) => (
                <MenuItem key={dept} value={dept}>
                  {dept}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              fullWidth
              label="Duration (Days)"
              type="number"
              required
              value={formData.durationDays}
              onChange={(e) => handleChange('durationDays', parseInt(e.target.value))}
              InputProps={{ inputProps: { min: 1, max: 365 } }}
              helperText="Expected duration of the onboarding process"
            />

            <Box display="flex" gap={2} justifyContent="flex-end">
              <Button
                variant="outlined"
                onClick={() => navigate('/plans')}
                disabled={loading}
              >
                Cancel
              </Button>
              <Button
                variant="contained"
                type="submit"
                loading={loading}
              >
                Create Plan
              </Button>
            </Box>
          </Box>
        </form>
      </Card>
    </Box>
  );
};

export default PlanCreate;
