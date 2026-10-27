import AccessTimeRounded from "@mui/icons-material/AccessTimeRounded";
import ArrowForwardRounded from "@mui/icons-material/ArrowForwardRounded";
import CheckCircleOutlineRounded from "@mui/icons-material/CheckCircleOutlineRounded";
import CloudOffRounded from "@mui/icons-material/CloudOffRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import NotificationsActiveRounded from "@mui/icons-material/NotificationsActiveRounded";
import RouteRounded from "@mui/icons-material/RouteRounded";
import SensorsRounded from "@mui/icons-material/SensorsRounded";
import ShieldRounded from "@mui/icons-material/ShieldRounded";
import SpeedRounded from "@mui/icons-material/SpeedRounded";
import TimelineRounded from "@mui/icons-material/TimelineRounded";
import WarningAmberRounded from "@mui/icons-material/WarningAmberRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import Skeleton from "@mui/material/Skeleton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { neepoApi } from "../api";
import { DEMO_TRIP_ID } from "../api/mockApi";
import { useAuthStore } from "../auth/authStore";
import { MetricCard } from "../components/MetricCard";
import { NotificationSettingsModal } from "../components/NotificationSettingsModal";
import { StatusPill } from "../components/StatusPill";
import { useOfflineQueue } from "../offline/useOfflineQueue";
import { useState } from "react";

export function DashboardPage() {
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const session = useAuthStore((state) => state.session)!;
  const { pendingCount } = useOfflineQueue();

  const trip = useQuery({
    queryKey: ["trip", DEMO_TRIP_ID],
    queryFn: () => neepoApi.getTrip(DEMO_TRIP_ID, session.accessToken)
  });

  const isDriver = session.user.role === "DRIVER";
  const roleMessage = isDriver
    ? "Your vehicle telemetry is actively monitored by CARE. Review recovery instructions or transmit onboard observations."
    : session.user.role === "ADMIN"
      ? "Fleet operations, policy compliance, and decision transparency engines are running with full constraint validation."
      : "Active disruption detected in the corridor. CARE has generated deterministic recovery alternatives.";

  const workspacePath = isDriver ? `/driver/trips/${DEMO_TRIP_ID}` : `/trips/${DEMO_TRIP_ID}`;
  const actionButtonText = isDriver ? "Open driver trip" : "Open recovery workspace";

  return (
    <Stack spacing={3.5}>
      {/* Welcome & System Status Banner */}
      <Box
        sx={{
          p: { xs: 2.5, sm: 3.5 },
          borderRadius: 4,
          background: "linear-gradient(135deg, #071b2a 0%, #0d3b43 100%)",
          color: "white",
          boxShadow: "0 12px 30px -5px rgba(7, 27, 42, 0.25)",
          position: "relative",
          overflow: "hidden"
        }}
      >
        <Box
          sx={{
            position: "absolute",
            right: -30,
            bottom: -40,
            opacity: 0.06,
            transform: "rotate(-10deg)",
            pointerEvents: "none"
          }}
        >
          <LocalShippingRounded sx={{ fontSize: 280 }} />
        </Box>

        <Stack spacing={1.5} sx={{ position: "relative", zIndex: 1 }}>
          <Stack direction="row" alignItems="center" spacing={1.5} flexWrap="wrap">
            <Chip
              icon={<CheckCircleOutlineRounded sx={{ fontSize: "16px !important", color: "#34d399 !important" }} />}
              label="CARE Engine Online"
              size="small"
              sx={{
                bgcolor: "rgba(16, 185, 129, 0.2)",
                color: "#a7f3d0",
                fontWeight: 700,
                border: "1px solid rgba(16, 185, 129, 0.35)"
              }}
            />
            <Typography variant="caption" sx={{ color: "rgba(255, 255, 255, 0.65)" }}>
              Tenant: NEEPO Demo Logistics · Real-Time PostGIS Mesh
            </Typography>
          </Stack>

          <Typography variant="h1" sx={{ color: "#ffffff", fontSize: { xs: "1.6rem", sm: "2.1rem" } }}>
            Welcome, {session.user.name.split(" ")[0]}
          </Typography>
          <Typography sx={{ color: "rgba(255, 255, 255, 0.82)", maxWidth: 720, fontSize: "0.95rem" }}>
            {roleMessage}
          </Typography>
        </Stack>
      </Box>

      {/* Operations Overview Metric Cards */}
      <Box>
        <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.06em" mb={1.5} display="block">
          Operations & Decision Telemetry Overview
        </Typography>

        <Stack direction={{ xs: "column", sm: "row" }} spacing={2} flexWrap="wrap">
          <MetricCard
            label="Active Trips"
            value="1"
            detail="1 requiring recovery attention"
            icon={<LocalShippingRounded sx={{ fontSize: 24, color: "#0d5b63" }} />}
            iconBg="rgba(13, 91, 99, 0.12)"
            statusColor="#0d5b63"
            sparklineData={[1, 1, 1, 1, 1, 1]}
          />

          <MetricCard
            label="Disruptions"
            value="1"
            detail="Engine thermal limit exceeded"
            icon={<WarningAmberRounded sx={{ fontSize: 24, color: "#ef4444" }} />}
            iconBg="rgba(239, 68, 68, 0.12)"
            statusColor="#ef4444"
            sparklineData={[0, 0, 1, 1, 1]}
          />

          <MetricCard
            label="Decision Latency"
            value="12 ms"
            detail="Production SLA < 250 ms"
            icon={<SpeedRounded sx={{ fontSize: 24, color: "#10b981" }} />}
            iconBg="rgba(16, 185, 129, 0.12)"
            statusColor="#10b981"
            sparklineData={[18, 15, 14, 12, 12, 11, 12]}
          />

          <MetricCard
            label="Telemetry Freshness"
            value="100%"
            detail="Temporal window validated"
            icon={<TimelineRounded sx={{ fontSize: 24, color: "#0284c7" }} />}
            iconBg="rgba(2, 132, 199, 0.12)"
            statusColor="#0284c7"
            sparklineData={[98, 99, 100, 100, 100]}
          />

          <MetricCard
            label="Safety Constraints"
            value="100%"
            detail="Zero hard violations passed"
            icon={<ShieldRounded sx={{ fontSize: 24, color: "#0d5b63" }} />}
            iconBg="rgba(13, 91, 99, 0.12)"
            statusColor="#0d5b63"
            sparklineData={[100, 100, 100, 100, 100]}
          />

          <MetricCard
            label="Offline Queue"
            value={pendingCount.toString()}
            detail={pendingCount > 0 ? "Observations queued locally" : "All device buffers synced"}
            icon={<CloudOffRounded sx={{ fontSize: 24, color: pendingCount > 0 ? "#f59e0b" : "#64748b" }} />}
            iconBg={pendingCount > 0 ? "rgba(245, 158, 11, 0.12)" : "rgba(100, 116, 139, 0.12)"}
            statusColor={pendingCount > 0 ? "#f59e0b" : "#64748b"}
            sparklineData={pendingCount > 0 ? [0, 1, 2, pendingCount] : [0, 0, 0]}
          />
        </Stack>
      </Box>

      {/* Priority Trip Section */}
      <Box>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
          <Box>
            <Typography variant="h3">Priority trip</Typography>
            <Typography variant="caption" color="text.secondary">
              Active operational corridor monitored by CARE
            </Typography>
          </Box>
          <Chip
            size="small"
            label="WebSocket Live Sync"
            sx={{ bgcolor: "#ecfdf5", color: "#047857", fontWeight: 700, border: "1px solid #a7f3d0" }}
          />
        </Stack>

        {trip.isPending && <Skeleton variant="rounded" height={220} sx={{ borderRadius: 4 }} />}

        {trip.isError && (
          <Alert severity="error" sx={{ borderRadius: 3 }}>
            {trip.error.message}
          </Alert>
        )}

        {trip.data && (
          <Card
            sx={{
              border: "1.5px solid",
              borderColor: trip.data.status === "DISRUPTED" ? "#fca5a5" : "#e2e8f0",
              overflow: "hidden",
              boxShadow: trip.data.status === "DISRUPTED" ? "0 8px 24px -4px rgba(239, 68, 68, 0.12)" : undefined
            }}
          >
            {trip.data.status === "DISRUPTED" && (
              <Box
                sx={{
                  bgcolor: "#fef2f2",
                  px: 3,
                  py: 1.2,
                  borderBottom: "1px solid #fecaca",
                  display: "flex",
                  alignItems: "center",
                  gap: 1.2
                }}
              >
                <Box className="critical-pulse-dot" />
                <Typography variant="caption" sx={{ color: "#b91c1c", fontWeight: 800 }}>
                  Active Disruption: Engine temperature exceeded 105°C threshold. CARE recovery plan generated.
                </Typography>
              </Box>
            )}

            <CardContent sx={{ p: { xs: 3, md: 4 } }}>
              <Stack direction={{ xs: "column", lg: "row" }} justifyContent="space-between" spacing={3}>
                <Stack spacing={2} flex={1}>
                  <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap">
                    <Typography variant="h2" sx={{ fontWeight: 800 }}>
                      {trip.data.referenceCode}
                    </Typography>
                    <StatusPill status={trip.data.status} />
                    <Chip
                      icon={<RouteRounded sx={{ fontSize: "15px !important" }} />}
                      label="Erode → Bengaluru (260 km)"
                      size="small"
                      sx={{ bgcolor: "#f1f5f9", fontWeight: 650, fontSize: "0.75rem" }}
                    />
                  </Stack>

                  <Typography color="text.secondary" fontSize="0.92rem">
                    High-priority medical shipment · Temperature-sensitive diagnostic reagents (2°C – 8°C safe band)
                  </Typography>

                  <Stack direction="row" spacing={3} flexWrap="wrap" sx={{ pt: 1 }}>
                    <Box>
                      <Stack direction="row" spacing={0.8} alignItems="center">
                        <AccessTimeRounded sx={{ fontSize: 16, color: "text.secondary" }} />
                        <Typography variant="caption" color="text.secondary">Delivery Deadline</Typography>
                      </Stack>
                      <Typography fontWeight={700} fontSize="0.9rem" mt={0.3}>
                        {new Date(trip.data.deliveryDeadline).toLocaleString(undefined, {
                          month: "short",
                          day: "numeric",
                          hour: "2-digit",
                          minute: "2-digit"
                        })}
                      </Typography>
                    </Box>

                    <Divider orientation="vertical" flexItem />

                    <Box>
                      <Typography variant="caption" color="text.secondary">Max Waiting Limit</Typography>
                      <Typography fontWeight={700} fontSize="0.9rem" mt={0.3}>
                        {trip.data.maximumWaitingMinutes} minutes
                      </Typography>
                    </Box>

                    <Divider orientation="vertical" flexItem />

                    <Box>
                      <Typography variant="caption" color="text.secondary">ETA Threshold</Typography>
                      <Typography fontWeight={700} fontSize="0.9rem" mt={0.3}>
                        {trip.data.acceptedEtaThresholdMinutes} minutes
                      </Typography>
                    </Box>
                  </Stack>
                </Stack>

                {/* Actions */}
                <Stack spacing={1.5} justifyContent="center" sx={{ minWidth: { lg: 240 } }}>
                  <Button
                    component={Link}
                    to={workspacePath}
                    variant="contained"
                    size="large"
                    endIcon={<ArrowForwardRounded />}
                    sx={{
                      py: 1.3,
                      borderRadius: 3,
                      boxShadow: "0 8px 18px -4px rgba(13, 91, 99, 0.4)"
                    }}
                  >
                    {actionButtonText}
                  </Button>

                  {!isDriver && (
                    <>
                      <Button
                        component={Link}
                        to={`/simulator/trips/${trip.data.id}`}
                        variant="outlined"
                        size="large"
                        startIcon={<SensorsRounded />}
                        sx={{ py: 1.2, borderRadius: 3 }}
                      >
                        Telemetry Simulator
                      </Button>

                      <Button
                        id="btn-dashboard-notifications"
                        variant="text"
                        size="large"
                        startIcon={<NotificationsActiveRounded />}
                        onClick={() => setNotificationsOpen(true)}
                        sx={{ py: 1.1, borderRadius: 3, color: "text.secondary" }}
                      >
                        Alerts & Webhooks
                      </Button>
                    </>
                  )}
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        )}
      </Box>

      {/* Notification Settings Modal */}
      <NotificationSettingsModal
        open={notificationsOpen}
        onClose={() => setNotificationsOpen(false)}
      />
    </Stack>
  );
}
