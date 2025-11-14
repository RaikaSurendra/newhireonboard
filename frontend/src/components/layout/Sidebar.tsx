import React from 'react';
import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  Box,
  Typography,
} from '@mui/material';
import { useNavigate, useLocation } from 'react-router-dom';

const DRAWER_WIDTH = 240;

interface MenuItem {
  text: string;
  path: string;
  icon: string;
  roles?: string[];
}

const menuItems: MenuItem[] = [
  { text: 'Dashboard', path: '/dashboard', icon: '📊' },
  { text: 'Onboarding Plans', path: '/plans', icon: '📋', roles: ['MANAGER', 'HR_MANAGER', 'ADMIN'] },
  { text: 'My Tasks', path: '/tasks', icon: '✅' },
  { text: 'Buddy Matches', path: '/matches', icon: '🤝' },
  { text: 'Messages', path: '/messages', icon: '💬' },
  { text: 'Feedback', path: '/feedback', icon: '⭐' },
  { text: 'Users', path: '/users', icon: '👥', roles: ['ADMIN', 'HR_MANAGER'] },
  { text: 'Analytics', path: '/analytics', icon: '📈', roles: ['ADMIN', 'HR_MANAGER'] },
];

interface SidebarProps {
  open: boolean;
  onClose: () => void;
  userRole?: string;
}

const Sidebar: React.FC<SidebarProps> = ({ open, onClose, userRole }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const filteredMenuItems = menuItems.filter(
    (item) => !item.roles || (userRole && item.roles.includes(userRole))
  );

  const handleNavigation = (path: string) => {
    navigate(path);
    if (window.innerWidth < 600) {
      onClose();
    }
  };

  return (
    <Drawer
      variant="permanent"
      open={open}
      sx={{
        width: DRAWER_WIDTH,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: DRAWER_WIDTH,
          boxSizing: 'border-box',
          background: 'linear-gradient(180deg, #7B3FF2 0%, #5E2BC4 100%)',
          color: 'white',
          borderRight: 'none',
        },
      }}
    >
      <Box sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 1 }}>
        <Box sx={{ 
          width: 40, 
          height: 40, 
          borderRadius: '8px', 
          bgcolor: 'rgba(255,255,255,0.2)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontWeight: 700,
          fontSize: '1.2rem'
        }}>
          OB
        </Box>
        <Typography variant="h6" sx={{ fontWeight: 700, color: 'white' }}>
          OnboardBuddy
        </Typography>
      </Box>
      <Box sx={{ overflow: 'auto', mt: 1, px: 2 }}>
        <List>
          {filteredMenuItems.map((item) => (
            <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
              <ListItemButton
                selected={location.pathname === item.path}
                onClick={() => handleNavigation(item.path)}
                sx={{
                  borderRadius: '8px',
                  color: 'rgba(255,255,255,0.8)',
                  '&:hover': {
                    backgroundColor: 'rgba(255,255,255,0.1)',
                    color: 'white',
                  },
                  '&.Mui-selected': {
                    backgroundColor: 'rgba(255,255,255,0.2)',
                    color: 'white',
                    '&:hover': {
                      backgroundColor: 'rgba(255,255,255,0.25)',
                    },
                  },
                }}
              >
                <ListItemIcon sx={{ color: 'inherit', minWidth: 40 }}>
                  <Typography fontSize="1.5rem">{item.icon}</Typography>
                </ListItemIcon>
                <ListItemText 
                  primary={item.text} 
                  primaryTypographyProps={{
                    fontSize: '0.95rem',
                    fontWeight: 500,
                  }}
                />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
        <Divider sx={{ my: 2, borderColor: 'rgba(255,255,255,0.1)' }} />
        <Box sx={{ px: 2 }}>
          <Typography variant="caption" sx={{ color: 'rgba(255,255,255,0.5)' }}>
            Version 1.0.0
          </Typography>
        </Box>
      </Box>
    </Drawer>
  );
};

export default Sidebar;
