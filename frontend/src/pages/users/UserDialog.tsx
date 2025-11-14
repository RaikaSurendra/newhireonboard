import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  MenuItem,
  Box,
  Alert,
  CircularProgress,
} from '@mui/material';
import userApi, { User } from '../../api/userApi';

interface UserDialogProps {
  open: boolean;
  user: User | null;
  onClose: () => void;
  onSave: () => void;
}

const UserDialog: React.FC<UserDialogProps> = ({ open, user, onClose, onSave }) => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    role: 'NEW_EMPLOYEE',
    department: '',
    phone: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user) {
      setFormData({
        name: user.name,
        email: user.email,
        role: user.role,
        department: user.department || '',
        phone: user.phone || '',
      });
    } else {
      setFormData({
        name: '',
        email: '',
        role: 'NEW_EMPLOYEE',
        department: '',
        phone: '',
      });
    }
    setError('');
  }, [user, open]);

  const handleChange = (field: string) => (event: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [field]: event.target.value,
    });
  };

  const handleSubmit = async () => {
    setError('');
    
    // Validation
    if (!formData.name.trim()) {
      setError('Name is required');
      return;
    }
    if (!formData.email.trim()) {
      setError('Email is required');
      return;
    }

    try {
      setLoading(true);
      
      if (user) {
        // Update existing user
        await userApi.updateUser(user.id, formData);
      } else {
        // Note: Create user would need a register endpoint or admin create endpoint
        setError('User creation not yet implemented. Use registration page.');
        setLoading(false);
        return;
      }
      
      onSave();
    } catch (err: any) {
      console.error('Error saving user:', err);
      setError(err.response?.data?.error || 'Failed to save user');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {user ? 'Edit User' : 'Add User'}
      </DialogTitle>
      
      <DialogContent>
        <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
          {error && (
            <Alert severity="error" onClose={() => setError('')}>
              {error}
            </Alert>
          )}

          <TextField
            label="Name"
            value={formData.name}
            onChange={handleChange('name')}
            fullWidth
            required
            disabled={loading}
          />

          <TextField
            label="Email"
            type="email"
            value={formData.email}
            onChange={handleChange('email')}
            fullWidth
            required
            disabled={loading || !!user} // Can't change email for existing users
            helperText={user ? "Email cannot be changed" : ""}
          />

          <TextField
            select
            label="Role"
            value={formData.role}
            onChange={handleChange('role')}
            fullWidth
            required
            disabled={loading}
          >
            <MenuItem value="NEW_EMPLOYEE">New Employee</MenuItem>
            <MenuItem value="BUDDY">Buddy</MenuItem>
            <MenuItem value="MANAGER">Manager</MenuItem>
            <MenuItem value="HR_MANAGER">HR Manager</MenuItem>
            <MenuItem value="ADMIN">Admin</MenuItem>
          </TextField>

          <TextField
            label="Department"
            value={formData.department}
            onChange={handleChange('department')}
            fullWidth
            disabled={loading}
          />

          <TextField
            label="Phone"
            value={formData.phone}
            onChange={handleChange('phone')}
            fullWidth
            disabled={loading}
            placeholder="+1234567890"
          />
        </Box>
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose} disabled={loading}>
          Cancel
        </Button>
        <Button 
          onClick={handleSubmit} 
          variant="contained" 
          disabled={loading}
          startIcon={loading ? <CircularProgress size={20} /> : null}
        >
          {loading ? 'Saving...' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default UserDialog;
