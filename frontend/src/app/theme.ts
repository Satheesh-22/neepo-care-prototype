import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
  palette: {
    mode: "light",
    primary: {
      main: "#0d5b63",
      dark: "#083a3f",
      light: "#e0f2f1",
      contrastText: "#ffffff"
    },
    secondary: {
      main: "#0284c7",
      light: "#e0f2fe",
      dark: "#0369a1",
      contrastText: "#ffffff"
    },
    background: {
      default: "#f8fafc",
      paper: "#ffffff"
    },
    text: {
      primary: "#0f172a",
      secondary: "#475569"
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
      fontSize: "clamp(1.85rem, 3.5vw, 2.75rem)",
      fontWeight: 800,
      letterSpacing: "-0.035em",
      lineHeight: 1.18
    },
    h2: {
      fontSize: "clamp(1.4rem, 2.2vw, 1.95rem)",
      fontWeight: 750,
      letterSpacing: "-0.025em",
      lineHeight: 1.25
    },
    h3: {
      fontSize: "1.18rem",
      fontWeight: 700,
      letterSpacing: "-0.015em",
      lineHeight: 1.35
    },
    h4: {
      fontSize: "1.02rem",
      fontWeight: 650,
      letterSpacing: "-0.01em"
    },
    body1: {
      fontSize: "0.938rem",
      lineHeight: 1.55,
      color: "#1e293b"
    },
    body2: {
      fontSize: "0.85rem",
      lineHeight: 1.5,
      color: "#475569"
    },
    caption: {
      fontSize: "0.75rem",
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
          borderRadius: 16,
          backgroundColor: "#ffffff",
          boxShadow: "0 2px 8px -2px rgba(15, 23, 42, 0.04), 0 1px 3px -1px rgba(15, 23, 42, 0.02)",
          transition: "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)",
          "&:hover": {
            boxShadow: "0 8px 24px -4px rgba(15, 23, 42, 0.07), 0 2px 8px -2px rgba(15, 23, 42, 0.03)"
          }
        }
      }
    },
    MuiPaper: {
      styleOverrides: {
        rounded: {
          borderRadius: 16
        }
      }
    },
    MuiButton: {
      defaultProps: {
        disableElevation: true
      },
      styleOverrides: {
        root: {
          minHeight: 40,
          borderRadius: 12,
          padding: "8px 18px",
          fontWeight: 700,
          fontSize: "0.875rem",
          transition: "all 0.15s ease-in-out",
          "&:focus-visible": {
            outline: "2.5px solid #0d5b63",
            outlineOffset: 2
          }
        },
        containedPrimary: {
          backgroundColor: "#0d5b63",
          color: "#ffffff",
          "&:hover": {
            backgroundColor: "#083a3f"
          }
        },
        outlinedPrimary: {
          borderColor: "#0d5b63",
          color: "#0d5b63",
          "&:hover": {
            backgroundColor: "rgba(13, 91, 99, 0.04)",
            borderColor: "#083a3f"
          }
        }
      }
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 700,
          borderRadius: 8
        }
      }
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          fontWeight: 500,
          fontSize: "0.875rem"
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
          },
          "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
            borderColor: "#0d5b63",
            borderWidth: 2
          }
        }
      }
    }
  }
});
