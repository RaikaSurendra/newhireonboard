import React from 'react';
import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
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
        },
      }}
    >
      <Toolbar />
      <Box sx={{ overflow: 'auto', mt: 2 }}>
        <List>
          {filteredMenuItems.map((item) => (
            <ListItem key={item.path} disablePadding>
              <ListItemButton
                selected={location.pathname === item.path}
                onClick={() => handleNavigation(item.path)}
                sx={{
                  '&.Mui-selected': {
                    backgroundColor: 'primary.light',
                    color: 'primary.contrastText',
                    '&:hover': {
                      backgroundColor: 'primary.main',
                    },
                  },
                }}
              >
                <ListItemIcon>
                  <Typography fontSize="1.5rem">{item.icon}</Typography>
                </ListItemIcon>
                <ListItemText primary={item.text} />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
        <Divider sx={{ my: 2 }} />
        <Box sx={{ px: 2 }}>
          <Typography variant="caption" color="text.secondary">
            Version 1.0.0
          </Typography>
        </Box>
      </Box>
    </Drawer>
  );
};

export default Sidebar;
