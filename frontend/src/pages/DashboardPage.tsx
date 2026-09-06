import AccessTimeRounded from "@mui/icons-material/AccessTimeRounded";
import ArrowForwardRounded from "@mui/icons-material/ArrowForwardRounded";
import CheckCircleOutlineRounded from "@mui/icons-material/CheckCircleOutlineRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import RouteRounded from "@mui/icons-material/RouteRounded";
import SensorsRounded from "@mui/icons-material/SensorsRounded";
import ShieldRounded from "@mui/icons-material/ShieldRounded";
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
import { StatusPill } from "../components/StatusPill";

export function DashboardPage() {
  const session = useAuthStore((state) => state.session)!;
  const trip = useQuery({
    queryKey: ["trip", DEMO_TRIP_ID],
    queryFn: () => neepoApi.getTrip(DEMO_TRIP_ID, session.accessToken)
  });

  const isDriver = session.user.role === "DRIVER";
  const roleMessage = isDriver
    ? "Your vehicle telemetry is monitored by CARE. Review recovery instructions or update current observations."
    : session.user.role === "ADMIN"
      ? "Fleet operations and decision transparency engines are running with full constraint compliance."
      : "Active disruption detected in the corridor. CARE has generated deterministic recovery alternatives.";

  const workspacePath = isDriver ? `/driver/trips/${DEMO_TRIP_ID}` : `/trips/${DEMO_TRIP_ID}`;

  const kpis = [
    {
      icon: <LocalShippingRounded sx={{ fontSize: 24, color: "#0d5b63" }} />,
      bg: "rgba(13, 91, 99, 0.1)",
      label: "Active Trips",
      value: "1",
      detail: "1 requiring recovery attention",
      status: "warning"
    },
    {
      icon: <ShieldRounded sx={{ fontSize: 24, color: "#10b981" }} />,
      bg: "rgba(16, 185, 129, 0.1)",
      label: "Safety Enforcement",
      value: "100%",
      detail: "All hard constraints verified",
      status: "success"
    },
    {
      icon: <TimelineRounded sx={{ fontSize: 24, color: "#0284c7" }} />,
      bg: "rgba(2, 132, 199, 0.1)",
      label: "Evidence Usability",
      value: "95%",
      detail: "Temporal freshness confirmed",
      status: "info"
    }
  ];

  return (
    <Stack spacing={3.5}>
      {/* Welcome & Incident Banner */}
      <Box
        sx={{
          p: { xs: 2.5, sm: 3.5 },
          borderRadius: 4,
          background: "linear-gradient(135deg, #071b2a 0%, #0d3b43 100%)",
          color: "white",
          boxShadow: "0 12px 30px -5px rgba(7, 27, 42, 0.2)",
          position: "relative",
          overflow: "hidden"
        }}
      >
        <Box
          sx={{
            position: "absolute",
            right: -30,
            bottom: -40,
            opacity: 0.07,
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
              sx={{ bgcolor: "rgba(16, 185, 129, 0.2)", color: "#a7f3d0", fontWeight: 700, border: "1px solid rgba(16, 185, 129, 0.3)" }}
            />
            <Typography variant="caption" sx={{ color: "rgba(255, 255, 255, 0.6)" }}>
              Tenant: NEEPO Demo Logistics
            </Typography>
          </Stack>

          <Typography variant="h1" sx={{ color: "#ffffff", fontSize: { xs: "1.6rem", sm: "2.1rem" } }}>
            Welcome, {session.user.name.split(" ")[0]}
          </Typography>
          <Typography sx={{ color: "rgba(255, 255, 255, 0.8)", maxWidth: 700, fontSize: "0.95rem" }}>
            {roleMessage}
          </Typography>
        </Stack>
      </Box>

      {/* KPI Stats Grid */}
      <Stack direction={{ xs: "column", md: "row" }} spacing={2.5}>
        {kpis.map((kpi) => (
          <Card key={kpi.label} sx={{ flex: 1, p: 0.5 }}>
            <CardContent sx={{ p: 2.5 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.04em">
                    {kpi.label}
                  </Typography>
                  <Typography variant="h2" sx={{ mt: 0.5, fontWeight: 800 }}>
                    {kpi.value}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" mt={0.5}>
                    {kpi.detail}
                  </Typography>
                </Box>
                <Box
                  sx={{
                    width: 48,
                    height: 48,
                    borderRadius: 3,
                    bgcolor: kpi.bg,
                    display: "grid",
                    placeItems: "center"
                  }}
                >
                  {kpi.icon}
                </Box>
              </Stack>
            </CardContent>
          </Card>
        ))}
      </Stack>

      {/* Priority Trip Section */}
      <Box>
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h3">Active Operational Corridor</Typography>
          <Typography variant="caption" color="text.secondary">
            Auto-refreshing via WebSocket
          </Typography>
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
              border: "1px solid",
              borderColor: trip.data.status === "DISRUPTED" ? "#fecaca" : "divider",
              overflow: "hidden"
            }}
          >
            {trip.data.status === "DISRUPTED" && (
              <Box sx={{ bgcolor: "#fef2f2", px: 3, py: 1, borderBottom: "1px solid #fecaca", display: "flex", alignItems: "center", gap: 1 }}>
                <WarningAmberRounded sx={{ fontSize: 18, color: "#ef4444" }} />
                <Typography variant="caption" sx={{ color: "#b91c1c", fontWeight: 700 }}>
                  Active Disruption: Engine temperature limit exceeded. Recovery plans ready for dispatch.
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
                    High-priority medical shipment · Temperature-sensitive diagnostic reagents (2°C – 8°C)
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
                    {isDriver ? "Open Driver Console" : "Open Recovery Workspace"}
                  </Button>

                  {!isDriver && (
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
                  )}
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        )}
      </Box>
    </Stack>
  );
}

