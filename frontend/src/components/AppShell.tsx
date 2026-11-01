import AccessibilityNewRounded from "@mui/icons-material/AccessibilityNewRounded";
import AccountCircleRounded from "@mui/icons-material/AccountCircleRounded";
import AssignmentIndRounded from "@mui/icons-material/AssignmentIndRounded";
import BusinessRounded from "@mui/icons-material/BusinessRounded";
import ContrastRounded from "@mui/icons-material/ContrastRounded";
import DashboardRounded from "@mui/icons-material/DashboardRounded";
import DirectionsCarRounded from "@mui/icons-material/DirectionsCarRounded";
import FormatSizeRounded from "@mui/icons-material/FormatSizeRounded";
import InsightsRounded from "@mui/icons-material/InsightsRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import LogoutRounded from "@mui/icons-material/LogoutRounded";
import NotificationsRounded from "@mui/icons-material/NotificationsRounded";
import PeopleRounded from "@mui/icons-material/PeopleRounded";
import PersonPinRounded from "@mui/icons-material/PersonPinRounded";
import RouterRounded from "@mui/icons-material/RouterRounded";
import SensorsRounded from "@mui/icons-material/SensorsRounded";
import ShieldRounded from "@mui/icons-material/ShieldRounded";
import SyncRounded from "@mui/icons-material/SyncRounded";
import TranslateRounded from "@mui/icons-material/TranslateRounded";
import VolumeUpRounded from "@mui/icons-material/VolumeUpRounded";
import WarehouseRounded from "@mui/icons-material/WarehouseRounded";
import WifiOffRounded from "@mui/icons-material/WifiOffRounded";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Container from "@mui/material/Container";
import Divider from "@mui/material/Divider";
import FormControlLabel from "@mui/material/FormControlLabel";
import IconButton from "@mui/material/IconButton";
import Popover from "@mui/material/Popover";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import { useEffect, useState, type ReactNode } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { apiMode } from "../api";
import { DEMO_TRIP_ID } from "../api/mockApi";
import { useAuthStore } from "../auth/authStore";
import { usePreferencesStore } from "../accessibility/preferencesStore";
import { useAnnounce } from "../accessibility";
import { useTranslation } from "../i18n";
import { useOfflineQueue } from "../offline/useOfflineQueue";
import { DemoModeBar } from "./DemoModeBar";
import { NotificationSettingsModal } from "./NotificationSettingsModal";

export function AppShell({ children }: { children: ReactNode }) {
  const [online, setOnline] = useState(navigator.onLine);
  const session = useAuthStore((state) => state.session)!;
  const signOut = useAuthStore((state) => state.signOut);
  const navigate = useNavigate();
  const location = useLocation();
  const { pendingCount, synchronizing, synchronize } = useOfflineQueue();
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [a11yAnchor, setA11yAnchor] = useState<null | HTMLElement>(null);

  const { t, locale, setLocale } = useTranslation();
  const { announce } = useAnnounce();

  const highContrast = usePreferencesStore((s) => s.highContrast);
  const toggleHighContrast = usePreferencesStore((s) => s.toggleHighContrast);
  const largeText = usePreferencesStore((s) => s.largeText);
  const toggleLargeText = usePreferencesStore((s) => s.toggleLargeText);
  const soundAlerts = usePreferencesStore((s) => s.soundAlerts);
  const toggleSoundAlerts = usePreferencesStore((s) => s.toggleSoundAlerts);

  useEffect(() => {
    const update = () => setOnline(navigator.onLine);
    window.addEventListener("online", update);
    window.addEventListener("offline", update);
    return () => {
      window.removeEventListener("online", update);
      window.removeEventListener("offline", update);
    };
  }, []);

  const handleToggleLanguage = () => {
    const newLocale = locale === "en" ? "es" : "en";
    setLocale(newLocale);
    announce(
      newLocale === "es"
        ? "Idioma cambiado a español"
        : "Language changed to English",
      "polite"
    );
  };

  const role = session.user.role;
  const navItems: { label: string; path: string; icon: React.ReactNode }[] =
    role === "ADMIN"
      ? [
          { label: t("nav.dashboard"), path: "/", icon: <DashboardRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.company"), path: "/admin/company", icon: <BusinessRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.enterpriseSso"), path: "/admin/sso", icon: <ShieldRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.users"), path: "/admin/users", icon: <PeopleRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.drivers"), path: "/admin/drivers", icon: <PersonPinRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.vehicles"), path: "/admin/vehicles", icon: <DirectionsCarRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.depots"), path: "/admin/depots", icon: <WarehouseRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.assignments"), path: "/admin/assignments", icon: <AssignmentIndRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.gateways"), path: "/gateways", icon: <RouterRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.analytics"), path: "/analytics", icon: <InsightsRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.workspace"), path: `/trips/${DEMO_TRIP_ID}`, icon: <LocalShippingRounded sx={{ fontSize: 18 }} /> },
          { label: t("nav.simulator"), path: `/simulator/trips/${DEMO_TRIP_ID}`, icon: <SensorsRounded sx={{ fontSize: 18 }} /> }
        ]
      : role === "DISPATCHER"
        ? [
            { label: t("nav.dashboard"), path: "/", icon: <DashboardRounded sx={{ fontSize: 18 }} /> },
            { label: t("nav.analytics"), path: "/analytics", icon: <InsightsRounded sx={{ fontSize: 18 }} /> },
            { label: t("nav.workspace"), path: `/trips/${DEMO_TRIP_ID}`, icon: <LocalShippingRounded sx={{ fontSize: 18 }} /> },
            { label: t("nav.drivers"), path: "/admin/drivers", icon: <PersonPinRounded sx={{ fontSize: 18 }} /> },
            { label: t("nav.vehicles"), path: "/admin/vehicles", icon: <DirectionsCarRounded sx={{ fontSize: 18 }} /> },
            { label: t("nav.depots"), path: "/admin/depots", icon: <WarehouseRounded sx={{ fontSize: 18 }} /> },
            { label: t("nav.assignments"), path: "/admin/assignments", icon: <AssignmentIndRounded sx={{ fontSize: 18 }} /> },
            { label: t("nav.gateways"), path: "/gateways", icon: <RouterRounded sx={{ fontSize: 18 }} /> },
            { label: t("nav.simulator"), path: `/simulator/trips/${DEMO_TRIP_ID}`, icon: <SensorsRounded sx={{ fontSize: 18 }} /> }
          ]
        : [
            { label: t("nav.dashboard"), path: "/", icon: <DashboardRounded sx={{ fontSize: 18 }} /> },
            { label: t("nav.myTrip"), path: `/driver/trips/${DEMO_TRIP_ID}`, icon: <LocalShippingRounded sx={{ fontSize: 18 }} /> },
            { label: t("nav.profile"), path: "/driver/profile", icon: <AccountCircleRounded sx={{ fontSize: 18 }} /> }
          ];

  return (
    <Box minHeight="100vh" sx={{ display: "flex", flexDirection: "column", bgcolor: "background.default" }}>
      {/* Skip to Main Content Link for WCAG 2.1 AA Keyboard Accessibility */}
      <a href="#main-content" className="skip-link">
        {t("a11y.skipToContent")}
      </a>

      {/* Interactive Demo Mode Controller */}
      <DemoModeBar />

      {/* Top App Bar with Landmark role="banner" */}
      <Box
        component="header"
        role="banner"
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
                aria-label="NEEPO-CARE Operations Home"
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

              {/* Navigation Links with Landmark role="navigation" */}
              <Box component="nav" role="navigation" aria-label={t("a11y.mainNav")}>
                <Stack direction="row" spacing={0.5} sx={{ display: { xs: "none", md: "flex" } }}>
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
              </Box>
            </Stack>

            {/* Status & User Actions */}
            <Stack direction="row" spacing={1.5} alignItems="center">
              {/* Online / Offline / Sync Indicators */}
              {!online ? (
                <Chip
                  icon={<WifiOffRounded sx={{ fontSize: "16px !important" }} />}
                  label={t("nav.offline")}
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
                    display: { xs: "none", lg: "flex" },
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
                    {t("nav.liveStack")}
                  </Typography>
                </Box>
              )}

              {(pendingCount > 0 || synchronizing) && (
                <Chip
                  icon={<SyncRounded sx={{ fontSize: "16px !important", animation: synchronizing ? "spin 1s linear infinite" : "none" }} />}
                  label={synchronizing ? t("nav.syncing") : t("nav.queued", { count: pendingCount })}
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
                  {t("nav.syncNow")}
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

              {/* Language Switcher Button */}
              <Tooltip title={locale === "en" ? "Cambiar a Español" : "Switch to English"}>
                <Button
                  id="btn-language-toggle"
                  size="small"
                  onClick={handleToggleLanguage}
                  startIcon={<TranslateRounded sx={{ fontSize: 16 }} />}
                  sx={{
                    color: "rgba(255, 255, 255, 0.9)",
                    bgcolor: "rgba(255, 255, 255, 0.08)",
                    border: "1px solid rgba(255, 255, 255, 0.15)",
                    fontSize: "0.76rem",
                    fontWeight: 800,
                    px: 1.2,
                    minHeight: 32,
                    "&:hover": { bgcolor: "rgba(255, 255, 255, 0.16)" }
                  }}
                  aria-label={`Current language: ${locale.toUpperCase()}. Click to switch language.`}
                >
                  {locale.toUpperCase()}
                </Button>
              </Tooltip>

              {/* Accessibility & Display Settings Button */}
              <Tooltip title={t("a11y.accessibilityMenu")}>
                <IconButton
                  id="btn-open-a11y"
                  size="small"
                  onClick={(e) => setA11yAnchor(e.currentTarget)}
                  aria-label={t("a11y.accessibilityMenu")}
                  sx={{
                    color: highContrast ? "#38bdf8" : "rgba(255, 255, 255, 0.85)",
                    bgcolor: highContrast ? "rgba(56, 189, 248, 0.2)" : "rgba(255, 255, 255, 0.08)",
                    border: highContrast ? "1.5px solid #38bdf8" : "1px solid rgba(255, 255, 255, 0.15)",
                    "&:hover": { bgcolor: "rgba(255, 255, 255, 0.16)", color: "#ffffff" }
                  }}
                >
                  <AccessibilityNewRounded sx={{ fontSize: 18 }} />
                </IconButton>
              </Tooltip>

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

              {role !== "DRIVER" && (
                <Tooltip title={t("nav.notificationsTooltip")}>
                  <IconButton
                    id="btn-open-notifications"
                    size="small"
                    onClick={() => setNotificationsOpen(true)}
                    aria-label={t("nav.notificationsTooltip")}
                    sx={{
                      color: "rgba(255, 255, 255, 0.8)",
                      bgcolor: "rgba(255, 255, 255, 0.08)",
                      border: "1px solid rgba(255, 255, 255, 0.12)",
                      "&:hover": { bgcolor: "rgba(255, 255, 255, 0.15)", color: "#ffffff" }
                    }}
                  >
                    <NotificationsRounded sx={{ fontSize: 18 }} />
                  </IconButton>
                </Tooltip>
              )}

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
                {t("nav.signOut")}
              </Button>
            </Stack>
          </Stack>
        </Container>
      </Box>

      {/* Accessibility & Display Settings Popover */}
      <Popover
        open={Boolean(a11yAnchor)}
        anchorEl={a11yAnchor}
        onClose={() => setA11yAnchor(null)}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
        transformOrigin={{ vertical: "top", horizontal: "right" }}
        PaperProps={{
          sx: {
            p: 2.5,
            width: 320,
            borderRadius: 3,
            boxShadow: "0 10px 35px rgba(0, 0, 0, 0.2)",
            border: "1px solid #e2e8f0"
          }
        }}
      >
        <Stack spacing={2}>
          <Stack direction="row" alignItems="center" spacing={1}>
            <AccessibilityNewRounded sx={{ color: "primary.main", fontSize: 20 }} />
            <Typography variant="h4" fontWeight={750}>
              {t("a11y.accessibilityMenu")}
            </Typography>
          </Stack>
          <Divider />

          {/* High Contrast Mode Switch */}
          <Box>
            <FormControlLabel
              control={
                <Switch
                  id="toggle-high-contrast"
                  checked={highContrast}
                  onChange={() => {
                    toggleHighContrast();
                    announce(
                      !highContrast
                        ? "High Contrast Mode enabled"
                        : "High Contrast Mode disabled",
                      "polite"
                    );
                  }}
                  color="primary"
                />
              }
              label={
                <Stack direction="row" spacing={1} alignItems="center">
                  <ContrastRounded sx={{ fontSize: 18 }} />
                  <Typography fontWeight={700} variant="body2">
                    {t("a11y.highContrast")}
                  </Typography>
                </Stack>
              }
            />
            <Typography variant="caption" color="text.secondary" sx={{ display: "block", pl: 4 }}>
              {t("a11y.highContrastDesc")}
            </Typography>
          </Box>

          {/* Large Text Scaling Switch */}
          <Box>
            <FormControlLabel
              control={
                <Switch
                  id="toggle-large-text"
                  checked={largeText}
                  onChange={() => {
                    toggleLargeText();
                    announce(
                      !largeText
                        ? "Large text scaling enabled"
                        : "Large text scaling disabled",
                      "polite"
                    );
                  }}
                  color="primary"
                />
              }
              label={
                <Stack direction="row" spacing={1} alignItems="center">
                  <FormatSizeRounded sx={{ fontSize: 18 }} />
                  <Typography fontWeight={700} variant="body2">
                    {t("a11y.largeText")}
                  </Typography>
                </Stack>
              }
            />
            <Typography variant="caption" color="text.secondary" sx={{ display: "block", pl: 4 }}>
              {t("a11y.largeTextDesc")}
            </Typography>
          </Box>

          {/* Sound & Screen Reader Alerts Switch */}
          <Box>
            <FormControlLabel
              control={
                <Switch
                  id="toggle-sound-alerts"
                  checked={soundAlerts}
                  onChange={toggleSoundAlerts}
                  color="primary"
                />
              }
              label={
                <Stack direction="row" spacing={1} alignItems="center">
                  <VolumeUpRounded sx={{ fontSize: 18 }} />
                  <Typography fontWeight={700} variant="body2">
                    {t("a11y.soundAlerts")}
                  </Typography>
                </Stack>
              }
            />
            <Typography variant="caption" color="text.secondary" sx={{ display: "block", pl: 4 }}>
              {t("a11y.soundAlertsDesc")}
            </Typography>
          </Box>
        </Stack>
      </Popover>

      {/* Notification Settings Modal */}
      <NotificationSettingsModal
        open={notificationsOpen}
        onClose={() => setNotificationsOpen(false)}
      />

      {/* Main Content Area with Landmark role="main" and Skip Anchor */}
      <Container
        component="main"
        id="main-content"
        role="main"
        tabIndex={-1}
        maxWidth="xl"
        sx={{
          py: { xs: 2.5, md: 4 },
          flex: 1,
          outline: "none"
        }}
      >
        {children}
      </Container>
    </Box>
  );
}
