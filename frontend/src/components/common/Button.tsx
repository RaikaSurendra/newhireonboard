import React from 'react';
import { Button as MuiButton, ButtonProps as MuiButtonProps, CircularProgress } from '@mui/material';

interface ButtonProps extends MuiButtonProps {
  loading?: boolean;
}

const Button: React.FC<ButtonProps> = ({ loading, children, disabled, ...props }) => {
  return (
    <MuiButton
      {...props}
      disabled={disabled || loading}
      startIcon={loading ? <CircularProgress size={20} color="inherit" /> : props.startIcon}
      sx={{
        borderRadius: '8px',
        textTransform: 'none',
        fontWeight: 600,
        padding: '10px 24px',
        boxShadow: 'none',
        '&:hover': {
          boxShadow: props.variant === 'contained' ? '0 4px 12px rgba(123, 63, 242, 0.3)' : 'none',
        },
        ...props.sx,
      }}
    >
      {children}
    </MuiButton>
  );
};

export default Button;
