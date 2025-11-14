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
    >
      {children}
    </MuiButton>
  );
};

export default Button;
