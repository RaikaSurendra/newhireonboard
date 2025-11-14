import React from 'react';
import { Card as MuiCard, CardContent, Typography, Box } from '@mui/material';

interface CardProps {
  title?: string;
  children: React.ReactNode;
  action?: React.ReactNode;
  sx?: any;
  onClick?: () => void;
}

const Card: React.FC<CardProps> = ({ title, children, action, sx, onClick }) => {
  return (
    <MuiCard
      onClick={onClick}
      sx={{
        borderRadius: '12px',
        boxShadow: '0 2px 12px rgba(0, 0, 0, 0.08)',
        transition: 'transform 0.2s, box-shadow 0.2s',
        '&:hover': {
          transform: 'translateY(-2px)',
          boxShadow: '0 4px 20px rgba(0, 0, 0, 0.12)',
        },
        ...sx,
      }}
    >
      {title && (
        <Box
          sx={{
            p: 2.5,
            borderBottom: '1px solid',
            borderColor: 'divider',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <Typography variant="h6" fontWeight={600} sx={{ color: '#2D2D2D' }}>
            {title}
          </Typography>
          {action}
        </Box>
      )}
      <CardContent sx={{ p: 3 }}>{children}</CardContent>
    </MuiCard>
  );
};

export default Card;
