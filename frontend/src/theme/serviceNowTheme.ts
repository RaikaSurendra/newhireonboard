import { createTheme } from '@mui/material/styles';

export const serviceNowTheme = createTheme({
  palette: {
    primary: {
      main: '#0F62FE',
      light: '#4589FF',
      dark: '#0043CE',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#6929C4',
      light: '#8A3FFC',
      dark: '#491D8B',
    },
    success: {
      main: '#24A148',
      light: '#42BE65',
      dark: '#198038',
    },
    warning: {
      main: '#F1C21B',
      light: '#FDD13A',
      dark: '#D2A106',
    },
    error: {
      main: '#DA1E28',
      light: '#FA4D56',
      dark: '#A2191F',
    },
    background: {
      default: '#F4F4F4',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#2D2D2D',
      secondary: '#6B6B6B',
    },
  },
  typography: {
    fontFamily: '"Gilroy", "Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontWeight: 700,
      fontSize: '2.5rem',
    },
    h2: {
      fontWeight: 700,
      fontSize: '2rem',
    },
    h3: {
      fontWeight: 600,
      fontSize: '1.75rem',
    },
    h4: {
      fontWeight: 600,
      fontSize: '1.5rem',
    },
    h5: {
      fontWeight: 600,
      fontSize: '1.25rem',
    },
    h6: {
      fontWeight: 600,
      fontSize: '1rem',
    },
    body1: {
      fontSize: '1rem',
      fontWeight: 400,
      lineHeight: 1.5,
    },
    body2: {
      fontSize: '0.875rem',
      fontWeight: 400,
      lineHeight: 1.43,
    },
    button: {
      fontWeight: 600,
      textTransform: 'none',
    },
  },
  shape: {
    borderRadius: 12,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          borderRadius: '8px',
          padding: '10px 24px',
        },
        contained: {
          boxShadow: 'none',
          '&:hover': {
            boxShadow: '0 4px 12px rgba(123, 63, 242, 0.3)',
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 2px 12px rgba(0, 0, 0, 0.08)',
          borderRadius: '12px',
          transition: 'transform 0.2s, box-shadow 0.2s',
          '&:hover': {
            transform: 'translateY(-2px)',
            boxShadow: '0 4px 20px rgba(0, 0, 0, 0.12)',
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: '8px',
          fontWeight: 500,
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            '&:hover fieldset': {
              borderColor: '#7B3FF2',
            },
          },
        },
      },
    },
  },
});
