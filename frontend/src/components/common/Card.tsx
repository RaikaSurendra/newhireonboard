import React from 'react';
import { Card as MuiCard, CardContent, CardHeader, CardActions, CardProps } from '@mui/material';

interface CustomCardProps extends CardProps {
  title?: string;
  subtitle?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
}

const Card: React.FC<CustomCardProps> = ({ title, subtitle, actions, children, ...props }) => {
  return (
    <MuiCard {...props}>
      {(title || subtitle) && (
        <CardHeader
          title={title}
          subheader={subtitle}
          titleTypographyProps={{ variant: 'h6', fontWeight: 600 }}
          subheaderTypographyProps={{ variant: 'body2' }}
        />
      )}
      <CardContent>{children}</CardContent>
      {actions && <CardActions>{actions}</CardActions>}
    </MuiCard>
  );
};

export default Card;
