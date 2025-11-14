import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Grid,
  Chip,
  TextField,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Loader from '../../components/common/Loader';
import { buddyMatchApi, BuddyMatch, CreateMatchRequest } from '../../api/buddyMatchApi';
import userApi from '../../api/userApi';

const BuddyMatches: React.FC = () => {
  const [matches, setMatches] = useState<BuddyMatch[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [users, setUsers] = useState<any[]>([]);
  const [newMatch, setNewMatch] = useState<CreateMatchRequest>({
    buddyUserId: 0,
    newEmployeeId: 0,
  });

  useEffect(() => {
    loadMatches();
    loadUsers();
  }, [filterStatus]);

  const loadMatches = async () => {
    try {
      const response = await buddyMatchApi.getAll({ status: filterStatus || undefined });
      const matchesData = response?.data?.data || [];
      setMatches(Array.isArray(matchesData) ? matchesData : []);
    } catch (error) {
      console.error('Failed to load matches:', error);
      setMatches([]);
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = async () => {
    try {
      const response = await userApi.getUsers();
      const usersData = response?.data?.data || [];
      setUsers(Array.isArray(usersData) ? usersData : []);
    } catch (error) {
      console.error('Failed to load users:', error);
    }
  };

  const handleCreateMatch = async () => {
    try {
      await buddyMatchApi.create(newMatch);
      setCreateDialogOpen(false);
      setNewMatch({ buddyUserId: 0, newEmployeeId: 0 });
      loadMatches();
    } catch (error) {
      console.error('Failed to create match:', error);
    }
  };

  const handleAcceptMatch = async (id: number) => {
    try {
      await buddyMatchApi.accept(id);
      loadMatches();
    } catch (error) {
      console.error('Failed to accept match:', error);
    }
  };

  const handleCompleteMatch = async (id: number) => {
    try {
      await buddyMatchApi.complete(id);
      loadMatches();
    } catch (error) {
      console.error('Failed to complete match:', error);
    }
  };

  const getStatusColor = (status: string) => {
    const colors: Record<string, 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning'> = {
      PENDING: 'warning',
      SUGGESTED: 'info',
      ACCEPTED: 'primary',
      ACTIVE: 'success',
      COMPLETED: 'default',
      ENDED: 'error',
    };
    return colors[status] || 'default';
  };

  if (loading) {
    return <Loader message="Loading buddy matches..." />;
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>
            🤝 Buddy Matches
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage buddy-new employee pairings
          </Typography>
        </Box>
        <Button variant="contained" onClick={() => setCreateDialogOpen(true)}>
          + Create Match
        </Button>
      </Box>

      <Box mb={3}>
        <TextField
          select
          label="Filter by Status"
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value)}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value="">All Statuses</MenuItem>
          <MenuItem value="PENDING">Pending</MenuItem>
          <MenuItem value="ACCEPTED">Accepted</MenuItem>
          <MenuItem value="ACTIVE">Active</MenuItem>
          <MenuItem value="COMPLETED">Completed</MenuItem>
          <MenuItem value="ENDED">Ended</MenuItem>
        </TextField>
      </Box>

      {matches.length === 0 ? (
        <Card>
          <Box textAlign="center" py={4}>
            <Typography variant="h6" color="text.secondary">
              No buddy matches found
            </Typography>
            <Typography variant="body2" color="text.secondary" mt={1}>
              Create your first match to get started
            </Typography>
          </Box>
        </Card>
      ) : (
        <Grid container spacing={3}>
          {matches.map((match) => (
            <Grid item xs={12} md={6} key={match.id}>
              <Card>
                <Box display="flex" justifyContent="space-between" alignItems="start" mb={2}>
                  <Box>
                    <Typography variant="h6" fontWeight={600}>
                      Match #{match.id}
                    </Typography>
                    <Chip
                      label={match.status}
                      color={getStatusColor(match.status)}
                      size="small"
                      sx={{ mt: 1 }}
                    />
                  </Box>
                  {match.matchScore && (
                    <Chip
                      label={`Score: ${(match.matchScore * 100).toFixed(0)}%`}
                      color="primary"
                      variant="outlined"
                      size="small"
                    />
                  )}
                </Box>

                <Box mb={2}>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>Buddy:</strong> {match.buddyName}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>New Employee:</strong> {match.employeeName}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <strong>Matched:</strong> {new Date(match.matchedAt).toLocaleDateString()}
                  </Typography>
                  {match.acceptedAt && (
                    <Typography variant="body2" color="text.secondary" gutterBottom>
                      <strong>Accepted:</strong> {new Date(match.acceptedAt).toLocaleDateString()}
                    </Typography>
                  )}
                </Box>

                <Box display="flex" gap={1}>
                  {match.status === 'PENDING' && (
                    <Button
                      size="small"
                      variant="contained"
                      onClick={() => handleAcceptMatch(match.id)}
                    >
                      Accept
                    </Button>
                  )}
                  {(match.status === 'ACCEPTED' || match.status === 'ACTIVE') && (
                    <Button
                      size="small"
                      variant="contained"
                      color="success"
                      onClick={() => handleCompleteMatch(match.id)}
                    >
                      Complete
                    </Button>
                  )}
                </Box>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Create Match Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Buddy Match</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField
              select
              label="Buddy"
              value={newMatch.buddyUserId}
              onChange={(e) => setNewMatch({ ...newMatch, buddyUserId: Number(e.target.value) })}
              fullWidth
            >
              <MenuItem value={0}>Select Buddy</MenuItem>
              {users.filter(u => u.role === 'BUDDY').map((user) => (
                <MenuItem key={user.id} value={user.id}>
                  {user.name} ({user.email})
                </MenuItem>
              ))}
            </TextField>

            <TextField
              select
              label="New Employee"
              value={newMatch.newEmployeeId}
              onChange={(e) => setNewMatch({ ...newMatch, newEmployeeId: Number(e.target.value) })}
              fullWidth
            >
              <MenuItem value={0}>Select Employee</MenuItem>
              {users.filter(u => u.role === 'NEW_EMPLOYEE').map((user) => (
                <MenuItem key={user.id} value={user.id}>
                  {user.name} ({user.email})
                </MenuItem>
              ))}
            </TextField>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreateMatch}
            disabled={!newMatch.buddyUserId || !newMatch.newEmployeeId}
          >
            Create Match
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default BuddyMatches;
