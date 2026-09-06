import DashboardRounded from "@mui/icons-material/DashboardRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import LogoutRounded from "@mui/icons-material/LogoutRounded";
import SensorsRounded from "@mui/icons-material/SensorsRounded";
import SyncRounded from "@mui/icons-material/SyncRounded";
import WifiOffRounded from "@mui/icons-material/WifiOffRounded";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Container from "@mui/material/Container";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useEffect, useState, type ReactNode } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { apiMode } from "../api";
import { DEMO_TRIP_ID } from "../api/mockApi";
import { useAuthStore } from "../auth/authStore";
import { useOfflineQueue } from "../offline/useOfflineQueue";

export function AppShell({ children }: { children: ReactNode }) {
  const [online, setOnline] = useState(navigator.onLine);
  const session = useAuthStore((state) => state.session)!;
  const signOut = useAuthStore((state) => state.signOut);
  const navigate = useNavigate();
  const location = useLocation();
  const { pendingCount, synchronizing, synchronize } = useOfflineQueue();

  useEffect(() => {
    const update = () => setOnline(navigator.onLine);
    window.addEventListener("online", update);
    window.addEventListener("offline", update);
    return () => {
      window.removeEventListener("online", update);
      window.removeEventListener("offline", update);
    };
  }, []);

  const isDriver = session.user.role === "DRIVER";
  const navItems = [
    { label: "Dashboard", path: "/", icon: <DashboardRounded sx={{ fontSize: 18 }} /> },
    {
      label: isDriver ? "My Trip" : "Workspace",
      path: isDriver ? `/driver/trips/${DEMO_TRIP_ID}` : `/trips/${DEMO_TRIP_ID}`,
      icon: <LocalShippingRounded sx={{ fontSize: 18 }} />
    },
    ...(!isDriver
      ? [{ label: "Simulator", path: `/simulator/trips/${DEMO_TRIP_ID}`, icon: <SensorsRounded sx={{ fontSize: 18 }} /> }]
      : [])
  ];

  return (
    <Box minHeight="100vh" sx={{ display: "flex", flexDirection: "column", bgcolor: "background.default" }}>
      {/* Top App Bar */}
      <Box
        component="header"
        sx={{
          bgcolor: "#071b2a",
          color: "white",
          borderBottom: "1px solid rgba(255, 255, 255, 0.08)",
          position: "sticky",
          top: 0,
          zIndex: 1100,
          backdropFilter: "blur(12px)"
        }}
      >
        <Container maxWidth="xl">
          <Stack
            direction="row"
            alignItems="center"
            justifyContent="space-between"
            sx={{ height: 68 }}
            spacing={2}
          >
            {/* Logo and Brand */}
            <Stack direction="row" alignItems="center" spacing={3}>
              <Stack
                direction="row"
                alignItems="center"
                spacing={1.5}
                component={Link}
                to="/"
                sx={{ textDecoration: "none", color: "inherit" }}
              >
                <Box
                  sx={{
                    width: 42,
                    height: 42,
                    borderRadius: "11px",
                    bgcolor: "#ffffff",
                    border: "1.5px solid rgba(255, 255, 255, 0.2)",
                    display: "grid",
                    placeItems: "center",
                    p: 0.3,
                    boxShadow: "0 2px 10px rgba(0, 0, 0, 0.2)",
                    overflow: "hidden"
                  }}
                >
                  <Box
                    component="img"
                    src="/neepo-logo.jpg"
                    alt="NEEPO-CARE Logo"
                    sx={{ width: "100%", height: "100%", objectFit: "cover", borderRadius: "8px" }}
                  />
                </Box>
                <Box>
                  <Typography fontWeight={800} letterSpacing="-0.02em" sx={{ fontSize: "1.05rem", lineHeight: 1.1 }}>
                    NEEPO-CARE
                  </Typography>
                  <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.6)", fontSize: "0.7rem" }}>
                    Operations Control
                  </Typography>
                </Box>
              </Stack>

              {/* Navigation Links */}
              <Stack direction="row" spacing={0.5} sx={{ display: { xs: "none", sm: "flex" } }}>
                {navItems.map((item) => {
                  const active = location.pathname === item.path;
                  return (
                    <Button
                      key={item.path}
                      component={Link}
                      to={item.path}
                      startIcon={item.icon}
                      size="small"
                      sx={{
                        color: active ? "#ffffff" : "rgba(255, 255, 255, 0.7)",
                        bgcolor: active ? "rgba(13, 91, 99, 0.4)" : "transparent",
                        border: active ? "1px solid rgba(255, 255, 255, 0.15)" : "1px solid transparent",
                        px: 1.5,
                        py: 0.7,
                        fontSize: "0.82rem",
                        "&:hover": {
                          bgcolor: "rgba(255, 255, 255, 0.08)",
                          color: "#ffffff"
                        }
                      }}
                    >
                      {item.label}
                    </Button>
                  );
                })}
              </Stack>
            </Stack>

            {/* Status & User Actions */}
            <Stack direction="row" spacing={1.5} alignItems="center">
              {/* Online / Offline / Sync Indicators */}
              {!online ? (
                <Chip
                  icon={<WifiOffRounded sx={{ fontSize: "16px !important" }} />}
                  label="Offline"
                  size="small"
                  sx={{
                    bgcolor: "rgba(239, 68, 68, 0.2)",
                    color: "#fca5a5",
                    border: "1px solid rgba(239, 68, 68, 0.4)",
                    fontWeight: 700
                  }}
                />
              ) : (
                <Box
                  sx={{
                    display: { xs: "none", md: "flex" },
                    alignItems: "center",
                    gap: 1,
                    px: 1.2,
                    py: 0.5,
                    borderRadius: 2,
                    bgcolor: "rgba(16, 185, 129, 0.1)",
                    border: "1px solid rgba(16, 185, 129, 0.2)"
                  }}
                >
                  <Box className="live-pulse-dot" />
                  <Typography variant="caption" sx={{ color: "#34d399", fontWeight: 700, fontSize: "0.72rem" }}>
                    Live Stack
                  </Typography>
                </Box>
              )}

              {(pendingCount > 0 || synchronizing) && (
                <Chip
                  icon={<SyncRounded sx={{ fontSize: "16px !important", animation: synchronizing ? "spin 1s linear infinite" : "none" }} />}
                  label={synchronizing ? "Syncing…" : `${pendingCount} queued`}
                  size="small"
                  sx={{
                    bgcolor: "rgba(245, 158, 11, 0.2)",
                    color: "#fcd34d",
                    border: "1px solid rgba(245, 158, 11, 0.4)",
                    fontWeight: 700
                  }}
                />
              )}

              {online && pendingCount > 0 && (
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => void synchronize()}
                  disabled={synchronizing}
                  sx={{
                    borderColor: "rgba(255,255,255,0.3)",
                    color: "white",
                    fontSize: "0.75rem",
                    py: 0.4,
                    minHeight: 30
                  }}
                >
                  Sync Now
                </Button>
              )}

              <Chip
                label={apiMode}
                size="small"
                sx={{
                  bgcolor: "rgba(255, 255, 255, 0.08)",
                  color: "rgba(255, 255, 255, 0.8)",
                  border: "1px solid rgba(255, 255, 255, 0.12)",
                  fontSize: "0.7rem",
                  textTransform: "uppercase"
                }}
              />

              {/* User badge */}
              <Box
                sx={{
                  display: { xs: "none", sm: "flex" },
                  alignItems: "center",
                  gap: 1,
                  pl: 1.5,
                  borderLeft: "1px solid rgba(255,255,255,0.12)"
                }}
              >
                <Box textAlign="right">
                  <Typography fontWeight={700} sx={{ fontSize: "0.82rem", lineHeight: 1.2 }}>
                    {session.user.name}
                  </Typography>
                  <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.5)", fontSize: "0.68rem" }}>
                    {session.user.role}
                  </Typography>
                </Box>
              </Box>

              <Button
                variant="outlined"
                color="inherit"
                size="small"
                startIcon={<LogoutRounded sx={{ fontSize: 16 }} />}
                onClick={() => {
                  signOut();
                  navigate("/login");
                }}
                sx={{
                  borderColor: "rgba(255,255,255,0.2)",
                  color: "rgba(255,255,255,0.9)",
                  minHeight: 34,
                  fontSize: "0.8rem",
                  "&:hover": {
                    borderColor: "rgba(255,255,255,0.4)",
                    bgcolor: "rgba(255,255,255,0.06)"
                  }
                }}
              >
                Sign out
              </Button>
            </Stack>
          </Stack>
        </Container>
      </Box>

      {/* Main Content Area */}
      <Container component="main" maxWidth="xl" sx={{ py: { xs: 2.5, md: 4 }, flex: 1 }}>
        {children}
      </Container>
    </Box>
  );
}

