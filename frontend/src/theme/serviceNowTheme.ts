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
      primary: '#161616',
      secondary: '#525252',
    },
  },
  typography: {
    fontFamily: '"Gilroy", "Inter", "Helvetica Neue", Arial, sans-serif',
    h1: {
      fontSize: '2.5rem',
      fontWeight: 700,
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 600,
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 600,
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 600,
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 500,
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 500,
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
    borderRadius: 4,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 600,
          padding: '8px 16px',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            '&:hover fieldset': {
              borderColor: '#0F62FE',
            },
          },
        },
      },
    },
  },
});
