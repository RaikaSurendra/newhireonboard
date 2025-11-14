import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Grid,
  Chip,
  TextField,
  InputAdornment,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Loader from '../../components/common/Loader';
import { planApi, Plan } from '../../api/planApi';

const PlanList: React.FC = () => {
  const navigate = useNavigate();
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    loadPlans();
  }, []);

  const loadPlans = async () => {
    try {
      const response = await planApi.getAll();
      console.log('Plans API response:', response);
      // Backend returns { success: true, data: [...] }
      // Axios wraps this in response.data, so we need response.data.data
      const plansData = response?.data?.data || response?.data || [];
      console.log('Plans data:', plansData);
      setPlans(Array.isArray(plansData) ? plansData : []);
    } catch (error) {
      console.error('Failed to load plans:', error);
      setPlans([]);
    } finally {
      setLoading(false);
    }
  };

  const filteredPlans = plans.filter((plan) =>
    plan.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    plan.department.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return <Loader message="Loading plans..." />;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>
            Onboarding Plans
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage and create onboarding plans for your organization
          </Typography>
        </Box>
        <Button
          variant="contained"
          onClick={() => navigate('/plans/create')}
        >
          + Create Plan
        </Button>
      </Box>

      <Box mb={3}>
        <TextField
          fullWidth
          placeholder="Search plans..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">🔍</InputAdornment>
            ),
          }}
        />
      </Box>

      <Grid container spacing={3}>
        {filteredPlans.length === 0 ? (
          <Grid item xs={12}>
            <Card>
              <Box textAlign="center" py={4}>
                <Typography variant="h6" color="text.secondary" gutterBottom>
                  No plans found
                </Typography>
                <Typography variant="body2" color="text.secondary" mb={2}>
                  Create your first onboarding plan to get started
                </Typography>
                <Button
                  variant="contained"
                  onClick={() => navigate('/plans/create')}
                >
                  Create Plan
                </Button>
              </Box>
            </Card>
          </Grid>
        ) : (
          filteredPlans.map((plan) => (
            <Grid item xs={12} md={6} lg={4} key={plan.id}>
              <Card
                sx={{
                  cursor: 'pointer',
                  transition: 'transform 0.2s',
                  '&:hover': {
                    transform: 'translateY(-4px)',
                    boxShadow: 3,
                  },
                }}
                onClick={() => navigate(`/plans/${plan.id}`)}
              >
                <Box>
                  <Box display="flex" justifyContent="space-between" alignItems="start" mb={2}>
                    <Typography variant="h6" fontWeight={600}>
                      {plan.name}
                    </Typography>
                    <Chip
                      label={plan.isActive ? 'Active' : 'Draft'}
                      color={plan.isActive ? 'success' : 'default'}
                      size="small"
                    />
                  </Box>

                  <Typography variant="body2" color="text.secondary" mb={2}>
                    {plan.description}
                  </Typography>

                  <Box display="flex" gap={1} flexWrap="wrap" mb={2}>
                    <Chip label={plan.department} size="small" variant="outlined" />
                    <Chip label={`${plan.durationDays} days`} size="small" variant="outlined" />
                    <Chip label={`v${plan.version}`} size="small" variant="outlined" />
                  </Box>

                  <Box display="flex" justifyContent="space-between" alignItems="center">
                    <Typography variant="caption" color="text.secondary">
                      {plan.templateCount || 0} templates
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Used {plan.usageCount || 0} times
                    </Typography>
                  </Box>
                </Box>
              </Card>
            </Grid>
          ))
        )}
      </Grid>
    </Box>
  );
};

export default PlanList;
