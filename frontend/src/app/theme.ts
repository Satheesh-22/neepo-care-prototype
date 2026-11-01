import { createTheme, type Theme } from "@mui/material/styles";

export interface ThemeOptionsConfig {
  highContrast?: boolean;
  largeText?: boolean;
}

export function createAppTheme(config: ThemeOptionsConfig = {}): Theme {
  const { highContrast = false, largeText = false } = config;

  const fontScale = largeText ? 1.15 : 1.0;

  return createTheme({
    palette: {
      mode: "light",
      primary: {
        main: highContrast ? "#042f2e" : "#0d5b63",
        dark: "#083a3f",
        light: highContrast ? "#ccfbf1" : "#e0f2f1",
        contrastText: "#ffffff"
      },
      secondary: {
        main: highContrast ? "#0369a1" : "#0284c7",
        light: "#e0f2fe",
        dark: "#075985",
        contrastText: "#ffffff"
      },
      background: {
        default: highContrast ? "#ffffff" : "#f8fafc",
        paper: "#ffffff"
      },
      text: {
        primary: highContrast ? "#000000" : "#0f172a",
        secondary: highContrast ? "#1e293b" : "#475569"
      },
      success: {
        main: highContrast ? "#047857" : "#10b981",
        light: "#ecfdf5",
        dark: "#065f46"
      },
      warning: {
        main: highContrast ? "#b45309" : "#f59e0b",
        light: "#fffbeb",
        dark: "#78350f"
      },
      error: {
        main: highContrast ? "#b91c1c" : "#ef4444",
        light: "#fef2f2",
        dark: "#7f1d1d"
      },
      info: {
        main: highContrast ? "#0369a1" : "#0284c7",
        light: "#f0f9ff",
        dark: "#0c4a6e"
      },
      divider: highContrast ? "#0f172a" : "#e2e8f0"
    },
    shape: {
      borderRadius: highContrast ? 8 : 16
    },
    typography: {
      fontFamily: '"Plus Jakarta Sans", "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      h1: {
        fontSize: `calc(clamp(1.85rem, 3.5vw, 2.75rem) * ${fontScale})`,
        fontWeight: 800,
        letterSpacing: "-0.035em",
        lineHeight: 1.18,
        color: highContrast ? "#000000" : undefined
      },
      h2: {
        fontSize: `calc(clamp(1.4rem, 2.2vw, 1.95rem) * ${fontScale})`,
        fontWeight: 750,
        letterSpacing: "-0.025em",
        lineHeight: 1.25,
        color: highContrast ? "#000000" : undefined
      },
      h3: {
        fontSize: `calc(1.18rem * ${fontScale})`,
        fontWeight: 700,
        letterSpacing: "-0.015em",
        lineHeight: 1.35,
        color: highContrast ? "#000000" : undefined
      },
      h4: {
        fontSize: `calc(1.02rem * ${fontScale})`,
        fontWeight: 650,
        letterSpacing: "-0.01em",
        color: highContrast ? "#000000" : undefined
      },
      body1: {
        fontSize: `calc(0.938rem * ${fontScale})`,
        lineHeight: 1.55,
        color: highContrast ? "#000000" : "#1e293b"
      },
      body2: {
        fontSize: `calc(0.85rem * ${fontScale})`,
        lineHeight: 1.5,
        color: highContrast ? "#1e293b" : "#475569"
      },
      caption: {
        fontSize: `calc(0.75rem * ${fontScale})`,
        fontWeight: 600,
        letterSpacing: "0.02em",
        color: highContrast ? "#1e293b" : undefined
      },
      button: {
        fontSize: `calc(0.875rem * ${fontScale})`,
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
            border: highContrast ? "2px solid #0f172a" : "1px solid #e2e8f0",
            borderRadius: highContrast ? 10 : 16,
            backgroundColor: "#ffffff",
            boxShadow: highContrast
              ? "none"
              : "0 2px 8px -2px rgba(15, 23, 42, 0.04), 0 1px 3px -1px rgba(15, 23, 42, 0.02)",
            transition: "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)",
            "&:hover": {
              boxShadow: highContrast
                ? "0 0 0 1px #0f172a"
                : "0 8px 24px -4px rgba(15, 23, 42, 0.07), 0 2px 8px -2px rgba(15, 23, 42, 0.03)"
            }
          }
        }
      },
      MuiPaper: {
        styleOverrides: {
          rounded: {
            borderRadius: highContrast ? 10 : 16,
            border: highContrast ? "1.5px solid #0f172a" : undefined
          }
        }
      },
      MuiButton: {
        defaultProps: {
          disableElevation: true
        },
        styleOverrides: {
          root: {
            minHeight: largeText ? 44 : 40,
            borderRadius: highContrast ? 8 : 12,
            padding: largeText ? "10px 22px" : "8px 18px",
            fontWeight: 700,
            border: highContrast ? "2px solid transparent" : undefined,
            transition: "all 0.15s ease-in-out",
            "&:focus-visible": {
              outline: highContrast ? "3px solid #0284c7" : "2.5px solid #0d5b63",
              outlineOffset: highContrast ? 3 : 2
            }
          },
          containedPrimary: {
            backgroundColor: highContrast ? "#042f2e" : "#0d5b63",
            color: "#ffffff",
            border: highContrast ? "2px solid #042f2e" : undefined,
            "&:hover": {
              backgroundColor: "#083a3f"
            }
          },
          outlinedPrimary: {
            borderColor: highContrast ? "#042f2e" : "#0d5b63",
            borderWidth: highContrast ? 2 : 1,
            color: highContrast ? "#042f2e" : "#0d5b63",
            "&:hover": {
              backgroundColor: "rgba(13, 91, 99, 0.04)",
              borderColor: "#083a3f",
              borderWidth: highContrast ? 2 : 1
            }
          }
        }
      },
      MuiChip: {
        styleOverrides: {
          root: {
            fontWeight: 700,
            borderRadius: highContrast ? 6 : 8,
            border: highContrast ? "1.5px solid currentColor" : undefined
          }
        }
      },
      MuiAlert: {
        styleOverrides: {
          root: {
            borderRadius: highContrast ? 8 : 12,
            fontWeight: 600,
            fontSize: `calc(0.875rem * ${fontScale})`,
            border: highContrast ? "2px solid currentColor" : undefined
          },
          standardSuccess: {
            backgroundColor: "#ecfdf5",
            color: "#065f46",
            border: highContrast ? "2px solid #065f46" : "1px solid #a7f3d0"
          },
          standardWarning: {
            backgroundColor: "#fffbeb",
            color: "#92400e",
            border: highContrast ? "2px solid #92400e" : "1px solid #fde68a"
          },
          standardError: {
            backgroundColor: "#fef2f2",
            color: "#991b1b",
            border: highContrast ? "2px solid #991b1b" : "1px solid #fecaca"
          },
          standardInfo: {
            backgroundColor: "#f0f9ff",
            color: "#075985",
            border: highContrast ? "2px solid #075985" : "1px solid #bae6fd"
          }
        }
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            borderRadius: highContrast ? 8 : 12,
            backgroundColor: "#ffffff",
            "& .MuiOutlinedInput-notchedOutline": {
              borderColor: highContrast ? "#0f172a" : undefined,
              borderWidth: highContrast ? 2 : 1
            },
            "&:hover .MuiOutlinedInput-notchedOutline": {
              borderColor: highContrast ? "#000000" : "#0d5b63"
            },
            "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
              borderColor: highContrast ? "#0284c7" : "#0d5b63",
              borderWidth: 2
            }
          }
        }
      }
    }
  });
}

export const theme = createAppTheme();
