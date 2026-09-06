import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#0d5b63",
      dark: "#0a3a40",
      light: "#e0f2f1",
      contrastText: "#ffffff"
    },
    secondary: {
      main: "#d97706",
      light: "#fef3c7",
      dark: "#b45309",
      contrastText: "#ffffff"
    },
    background: {
      default: "#f8fafc",
      paper: "#ffffff"
    },
    text: {
      primary: "#0f172a",
      secondary: "#64748b"
    },
    success: {
      main: "#10b981",
      light: "#ecfdf5",
      dark: "#047857"
    },
    warning: {
      main: "#f59e0b",
      light: "#fffbeb",
      dark: "#b45309"
    },
    error: {
      main: "#ef4444",
      light: "#fef2f2",
      dark: "#b91c1c"
    },
    info: {
      main: "#0284c7",
      light: "#f0f9ff",
      dark: "#0369a1"
    },
    divider: "#e2e8f0"
  },
  shape: {
    borderRadius: 16
  },
  typography: {
    fontFamily: '"Plus Jakarta Sans", "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    h1: {
      fontSize: "clamp(2rem, 4vw, 3rem)",
      fontWeight: 800,
      letterSpacing: "-0.035em",
      lineHeight: 1.15
    },
    h2: {
      fontSize: "clamp(1.5rem, 2.5vw, 2.1rem)",
      fontWeight: 750,
      letterSpacing: "-0.025em",
      lineHeight: 1.25
    },
    h3: {
      fontSize: "1.2rem",
      fontWeight: 700,
      letterSpacing: "-0.015em",
      lineHeight: 1.35
    },
    h4: {
      fontSize: "1.05rem",
      fontWeight: 650,
      letterSpacing: "-0.01em"
    },
    body1: {
      fontSize: "0.95rem",
      lineHeight: 1.55
    },
    body2: {
      fontSize: "0.875rem",
      lineHeight: 1.5
    },
    caption: {
      fontSize: "0.78rem",
      fontWeight: 600,
      letterSpacing: "0.02em"
    },
    button: {
      fontWeight: 700,
      textTransform: "none",
      letterSpacing: "0.01em"
    }
  },
  components: {
    MuiCard: {
      defaultProps: {
        elevation: 0
      },
      styleOverrides: {
        root: {
          border: "1px solid #e2e8f0",
          borderRadius: 18,
          backgroundColor: "#ffffff",
          boxShadow: "0 4px 20px -2px rgba(15, 23, 42, 0.04), 0 2px 6px -1px rgba(15, 23, 42, 0.02)",
          transition: "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)",
          "&:hover": {
            boxShadow: "0 12px 28px -4px rgba(15, 23, 42, 0.08), 0 4px 12px -2px rgba(15, 23, 42, 0.03)"
          }
        }
      }
    },
    MuiPaper: {
      styleOverrides: {
        rounded: {
          borderRadius: 18
        }
      }
    },
    MuiButton: {
      defaultProps: {
        disableElevation: true
      },
      styleOverrides: {
        root: {
          minHeight: 42,
          borderRadius: 12,
          padding: "8px 18px",
          fontWeight: 700,
          transition: "all 0.15s ease-in-out"
        },
        containedPrimary: {
          backgroundColor: "#0d5b63",
          "&:hover": {
            backgroundColor: "#094349"
          }
        }
      }
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 700,
          borderRadius: 10
        }
      }
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 14,
          fontWeight: 500,
          fontSize: "0.9rem"
        },
        standardSuccess: {
          backgroundColor: "#ecfdf5",
          color: "#065f46",
          border: "1px solid #a7f3d0"
        },
        standardWarning: {
          backgroundColor: "#fffbeb",
          color: "#92400e",
          border: "1px solid #fde68a"
        },
        standardError: {
          backgroundColor: "#fef2f2",
          color: "#991b1b",
          border: "1px solid #fecaca"
        },
        standardInfo: {
          backgroundColor: "#f0f9ff",
          color: "#075985",
          border: "1px solid #bae6fd"
        }
      }
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          backgroundColor: "#ffffff",
          "&:hover .MuiOutlinedInput-notchedOutline": {
            borderColor: "#0d5b63"
          }
        }
      }
    }
  }
});
