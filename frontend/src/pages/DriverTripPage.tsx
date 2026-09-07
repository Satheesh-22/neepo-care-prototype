import CloudDoneRounded from "@mui/icons-material/CloudDoneRounded";
import CloudOffRounded from "@mui/icons-material/CloudOffRounded";
import NavigationRounded from "@mui/icons-material/NavigationRounded";
import PlayArrowRounded from "@mui/icons-material/PlayArrowRounded";
import SendRounded from "@mui/icons-material/SendRounded";
import SensorsRounded from "@mui/icons-material/SensorsRounded";
import SpeedRounded from "@mui/icons-material/SpeedRounded";
import ThermostatRounded from "@mui/icons-material/ThermostatRounded";
import WarningAmberRounded from "@mui/icons-material/WarningAmberRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Checkbox from "@mui/material/Checkbox";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import FormControl from "@mui/material/FormControl";
import FormControlLabel from "@mui/material/FormControlLabel";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Slider from "@mui/material/Slider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { NetworkState, SystemEvent, TelemetryObservation } from "@contracts";
import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { neepoApi } from "../api";
import { subscribeToTripEvents, type EventConnectionState } from "../api/eventStream";
import { useAuthStore } from "../auth/authStore";
import { StatusPill } from "../components/StatusPill";
import { TelemetryGauges } from "../components/TelemetryGauges";
import { submitTelemetry } from "../offline/telemetrySync";

const vehicleId = "00000000-0000-4000-8300-000000000001";

export function DriverTripPage() {
  const { tripId = "" } = useParams();
  const session = useAuthStore((state) => state.session)!;
  const queryClient = useQueryClient();
  const [engineTemperature, setEngineTemperature] = useState(96);
  const [cargoTemperature, setCargoTemperature] = useState(4);
  const [speed, setSpeed] = useState(0);
  const [networkState, setNetworkState] = useState<NetworkState>("ONLINE");
  const [safelyParked, setSafelyParked] = useState(false);
  const [connection, setConnection] = useState<EventConnectionState>("connecting");
  const [latestEvent, setLatestEvent] = useState<SystemEvent | null>(null);

  const trip = useQuery({
    queryKey: ["trip", tripId],
    queryFn: () => neepoApi.getTrip(tripId, session.accessToken),
    enabled: Boolean(tripId)
  });

  const plans = useQuery({
    queryKey: ["plans", tripId],
    queryFn: () => neepoApi.listRecoveryPlans(tripId, session.accessToken),
    enabled: Boolean(tripId)
  });

  const selectedPlan = useMemo(
    () => plans.data?.find((plan) => plan.status === "SELECTED"),
    [plans.data]
  );

  useEffect(() => {
    const subscription = subscribeToTripEvents(
      tripId,
      session.accessToken,
      setLatestEvent,
      (state) => {
        setConnection(state);
        if (state === "connected") {
          void queryClient.invalidateQueries({ queryKey: ["trip", tripId] });
          void queryClient.invalidateQueries({ queryKey: ["plans", tripId] });
        }
      }
    );
    return subscription.close;
  }, [queryClient, session.accessToken, tripId]);

  const startTripMutation = useMutation({
    mutationFn: async () => {
      return await neepoApi.startTrip(tripId, session.accessToken);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["trip", tripId] });
    }
  });

  const transmit = useMutation({
    mutationFn: async () => {
      if (trip.data?.status === "ASSIGNED") {
        await neepoApi.startTrip(tripId, session.accessToken);
        void queryClient.invalidateQueries({ queryKey: ["trip", tripId] });
      }
      const observation = createObservation(
        tripId,
        engineTemperature,
        cargoTemperature,
        speed,
        networkState,
        session.user.role === "DRIVER" ? "DRIVER_DEVICE" : "SIMULATOR"
      );
      const submission = await submitTelemetry(observation, session.accessToken);
      if (submission.state === "queued") return { submission };
      const policy = await neepoApi.evaluateTelemetryPolicy(
        tripId,
        {
          eventId: crypto.randomUUID(),
          observedAt: observation.receivedAt,
          nearDecisionPoint: false,
          safelyParked
        },
        session.accessToken
      );
      return { submission, policy };
    }
  });

  if (trip.isPending) {
    return (
      <Stack alignItems="center" justifyContent="center" minHeight="50vh">
        <CircularProgress size={40} />
      </Stack>
    );
  }

  if (trip.isError) {
    return (
      <Alert severity="error" sx={{ mt: 3 }}>
        {trip.error.message}
      </Alert>
    );
  }

  const isDriver = session.user.role === "DRIVER";

  // Engine temperature status calculations
  const isEngineCritical = engineTemperature > 105;
  const isEngineWarning = engineTemperature >= 95 && engineTemperature <= 105;
  const engineColor = isEngineCritical ? "#ef4444" : isEngineWarning ? "#f59e0b" : "#10b981";

  // Cargo temperature status (Safe between 2°C and 8°C)
  const isCargoSafe = cargoTemperature >= 2 && cargoTemperature <= 8;
  const cargoColor = isCargoSafe ? "#10b981" : "#ef4444";

  return (
    <Stack spacing={3}>
      {/* Header */}
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "flex-start", sm: "center" }} spacing={2}>
        <Box>
          <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap">
            <Typography variant="h2" fontWeight={800}>
              {isDriver ? "Driver Command Console" : "Telemetry Simulator"}
            </Typography>
            {trip.data && <StatusPill status={trip.data.status} />}
          </Stack>
          <Typography color="text.secondary" mt={0.5} fontSize="0.95rem">
            Vehicle KA-01-EQ-4040 · Live onboard instrument controls & telemetry reporting
          </Typography>
        </Box>

        <Stack direction="row" spacing={1} alignItems="center">
          <Chip
            icon={connection === "connected" ? <CloudDoneRounded sx={{ fontSize: "16px !important", color: "#047857 !important" }} /> : <CloudOffRounded sx={{ fontSize: "16px !important" }} />}
            label={connection === "connected" ? "Live Connected" : connection}
            size="small"
            sx={{
              bgcolor: connection === "connected" ? "#ecfdf5" : "#fffbeb",
              color: connection === "connected" ? "#047857" : "#b45309",
              border: `1px solid ${connection === "connected" ? "#a7f3d0" : "#fde68a"}`,
              fontWeight: 700
            }}
          />
        </Stack>
      </Stack>

      {/* Real-time Telemetry Visualization Gauges */}
      <TelemetryGauges
        engineTemperature={engineTemperature}
        cargoTemperature={cargoTemperature}
        speed={speed}
        networkState={networkState}
        observationFreshness="FRESH"
        safelyParked={safelyParked}
      />

      {/* Trip Assigned Banner */}
      {trip.data?.status === "ASSIGNED" && (
        <Card sx={{ bgcolor: "#ecfdf5", border: "1.5px solid #a7f3d0" }}>
          <CardContent sx={{ p: 2.5 }}>
            <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "flex-start", sm: "center" }} spacing={2}>
              <Box>
                <Typography variant="caption" color="success.dark" fontWeight={750} textTransform="uppercase" letterSpacing="0.04em">
                  Trip Assigned & Ready to Dispatch
                </Typography>
                <Typography variant="body1" fontWeight={700} color="#065f46" mt={0.2}>
                  Start the trip to begin onboard telemetry synchronization and active CARE monitoring.
                </Typography>
              </Box>
              <Button
                variant="contained"
                startIcon={startTripMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <PlayArrowRounded />}
                disabled={startTripMutation.isPending}
                onClick={() => startTripMutation.mutate()}
                sx={{
                  bgcolor: "#059669",
                  "&:hover": { bgcolor: "#047857" },
                  borderRadius: 2.5,
                  px: 3,
                  py: 1,
                  fontWeight: 700,
                  whiteSpace: "nowrap"
                }}
              >
                {startTripMutation.isPending ? "Starting Trip…" : "Start Trip"}
              </Button>
            </Stack>
          </CardContent>
        </Card>
      )}

      {/* Next Safe Action Banner */}
      {selectedPlan && (
        <Card sx={{ bgcolor: "#fffbeb", border: "1.5px solid #fde68a" }}>
          <CardContent sx={{ p: 2.5 }}>
            <Stack direction="row" spacing={2} alignItems="center">
              <Box sx={{ width: 44, height: 44, borderRadius: 2.5, bgcolor: "#fef3c7", display: "grid", placeItems: "center", flexShrink: 0 }}>
                <WarningAmberRounded sx={{ fontSize: 26, color: "#d97706" }} />
              </Box>
              <Box flex={1}>
                <Typography variant="caption" color="warning.dark" fontWeight={700} textTransform="uppercase" letterSpacing="0.04em">
                  Next safe action:
                </Typography>
                <Typography variant="body1" fontWeight={700} color="#78350f" mt={0.2}>
                  {selectedPlan.steps[0]?.instruction}
                </Typography>
              </Box>
            </Stack>
          </CardContent>
        </Card>
      )}

      {/* Main Controls Grid */}
      <Stack direction={{ xs: "column", lg: "row" }} spacing={3} alignItems="flex-start">
        {/* Left Column: Instruments & Telemetry Observation */}
        <Card sx={{ flex: 1.5, width: "100%", border: "1px solid #e2e8f0" }}>
          <CardContent sx={{ p: { xs: 2.5, sm: 3.5 } }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
              <Stack direction="row" spacing={1} alignItems="center">
                <SensorsRounded sx={{ color: "#0d5b63" }} />
                <Typography variant="h3">Telemetry Controls</Typography>
              </Stack>
              <Typography variant="caption" color="text.secondary">
                Simulate onboard sensor signals
              </Typography>
            </Stack>

            {/* Quick Presets */}
            <Box sx={{ my: 2.5, p: 2, borderRadius: 3, bgcolor: "#f8fafc", border: "1px solid #e2e8f0" }}>
              <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.04em" display="block" mb={1}>
                Quick Test Scenarios
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap">
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => { setEngineTemperature(85); setCargoTemperature(4); setSpeed(60); setSafelyParked(false); }}
                  sx={{ borderRadius: 2, fontSize: "0.78rem" }}
                >
                  🟢 Normal Cruise (85°C)
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  color="warning"
                  onClick={() => { setEngineTemperature(106); setSpeed(0); setSafelyParked(false); }}
                  sx={{ borderRadius: 2, fontSize: "0.78rem" }}
                >
                  🔴 Overheat Trigger (106°C)
                </Button>
                <Button
                  size="small"
                  variant="outlined"
                  color="success"
                  onClick={() => { setEngineTemperature(70); setSpeed(0); setSafelyParked(true); }}
                  sx={{ borderRadius: 2, fontSize: "0.78rem" }}
                >
                  🅿️ Safely Parked (70°C)
                </Button>
              </Stack>
            </Box>

            <Stack spacing={3}>
              {/* Engine Temp Gauge */}
              <Box sx={{ p: 2, borderRadius: 2.5, bgcolor: "#ffffff", border: `1.5px solid ${engineColor}`, transition: "all 0.2s ease" }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <ThermostatRounded sx={{ color: engineColor }} />
                    <Typography fontWeight={750} fontSize="0.95rem">Engine Coolant Temperature</Typography>
                  </Stack>
                  <Chip
                    label={`${engineTemperature} °C ${isEngineCritical ? "CRITICAL (>105°C)" : isEngineWarning ? "WARNING" : "NORMAL"}`}
                    size="small"
                    sx={{ bgcolor: `${engineColor}15`, color: engineColor, fontWeight: 800, border: `1px solid ${engineColor}40` }}
                  />
                </Stack>
                <Slider
                  aria-label="Engine temperature"
                  value={engineTemperature}
                  min={60}
                  max={150}
                  valueLabelDisplay="auto"
                  onChange={(_, next) => setEngineTemperature(next as number)}
                  sx={{ color: engineColor }}
                />
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="caption" color="text.secondary">60°C (Cold)</Typography>
                  <Typography variant="caption" color="text.secondary">Safe Limit: 95°C</Typography>
                  <Typography variant="caption" color="text.secondary">150°C (Max)</Typography>
                </Stack>
              </Box>

              {/* Cargo Temp Gauge */}
              <Box sx={{ p: 2, borderRadius: 2.5, bgcolor: "#ffffff", border: `1.5px solid ${cargoColor}`, transition: "all 0.2s ease" }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <ThermostatRounded sx={{ color: cargoColor }} />
                    <Typography fontWeight={750} fontSize="0.95rem">Refrigerated Cargo Temperature</Typography>
                  </Stack>
                  <Chip
                    label={`${cargoTemperature} °C ${isCargoSafe ? "SAFE (2-8°C)" : "OUT OF RANGE"}`}
                    size="small"
                    sx={{ bgcolor: `${cargoColor}15`, color: cargoColor, fontWeight: 800, border: `1px solid ${cargoColor}40` }}
                  />
                </Stack>
                <Slider
                  aria-label="Cargo temperature"
                  value={cargoTemperature}
                  min={-20}
                  max={30}
                  valueLabelDisplay="auto"
                  onChange={(_, next) => setCargoTemperature(next as number)}
                  sx={{ color: cargoColor }}
                />
              </Box>

              {/* Speed Slider */}
              <Box sx={{ p: 2, borderRadius: 2.5, bgcolor: "#ffffff", border: "1px solid #e2e8f0" }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <SpeedRounded sx={{ color: "#0d5b63" }} />
                    <Typography fontWeight={750} fontSize="0.95rem">Vehicle Speed</Typography>
                  </Stack>
                  <Typography fontWeight={800} sx={{ color: "#0d5b63" }}>{speed} km/h</Typography>
                </Stack>
                <Slider
                  aria-label="Speed"
                  value={speed}
                  min={0}
                  max={120}
                  valueLabelDisplay="auto"
                  onChange={(_, next) => setSpeed(next as number)}
                  sx={{ color: "#0d5b63" }}
                />
              </Box>

              {/* Network State & Safe Parking */}
              <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems="center">
                <FormControl fullWidth>
                  <InputLabel id="network-state-label">Reported network state</InputLabel>
                  <Select
                    labelId="network-state-label"
                    label="Reported network state"
                    value={networkState}
                    onChange={(event) => setNetworkState(event.target.value as NetworkState)}
                  >
                    <MenuItem value="ONLINE">ONLINE</MenuItem>
                    <MenuItem value="WEAK">WEAK</MenuItem>
                    <MenuItem value="OFFLINE">OFFLINE</MenuItem>
                    <MenuItem value="SYNCHRONIZING">SYNCHRONIZING</MenuItem>
                  </Select>
                </FormControl>

                <FormControlLabel
                  control={<Checkbox checked={safelyParked} onChange={(event) => setSafelyParked(event.target.checked)} />}
                  label={<Typography fontWeight={600} fontSize="0.9rem">Vehicle is safely parked</Typography>}
                  sx={{ whiteSpace: "nowrap" }}
                />
              </Stack>

              {/* Transmit Action */}
              <Button
                variant="contained"
                size="large"
                startIcon={transmit.isPending ? <CircularProgress size={18} color="inherit" /> : <SendRounded />}
                disabled={transmit.isPending}
                onClick={() => transmit.mutate()}
                sx={{
                  py: 1.4,
                  fontSize: "0.95rem",
                  borderRadius: 3,
                  boxShadow: "0 8px 18px -4px rgba(13, 91, 99, 0.4)"
                }}
              >
                {transmit.isPending
                  ? "Transmitting…"
                  : networkState === "OFFLINE"
                    ? "Save observation offline"
                    : "Transmit observation"}
              </Button>

              {/* Feedback Alerts */}
              {transmit.data?.submission.state === "accepted" && (
                <Alert severity="success" sx={{ borderRadius: 2.5 }}>
                  Observation accepted by server{transmit.data.submission.result.duplicate ? " (Idempotent duplicate recognized)" : ""}.
                </Alert>
              )}

              {transmit.data?.submission.state === "queued" && (
                <Alert severity="info" sx={{ borderRadius: 2.5 }}>
                  Observation saved securely on this device and queued for synchronization.
                </Alert>
              )}

              {transmit.data?.policy && (
                <Alert severity="info" sx={{ borderRadius: 2.5 }}>
                  Adaptive Telemetry Cadence: <strong>{transmit.data.policy.policyState.replaceAll("_", " ")} · next upload in {transmit.data.policy.intervalSeconds} seconds</strong>
                </Alert>
              )}

              {transmit.isError && (
                <Alert severity="error" sx={{ borderRadius: 2.5 }}>
                  {transmit.error.message}
                </Alert>
              )}
            </Stack>
          </CardContent>
        </Card>

        {/* Right Column: Tactical Instructions & Connection */}
        <Stack spacing={3} flex={1} width="100%">
          {/* Recovery Instructions Card */}
          <Card sx={{ border: "1px solid #e2e8f0" }}>
            <CardContent sx={{ p: 3 }}>
              <Stack direction="row" spacing={1} alignItems="center" mb={2}>
                <NavigationRounded sx={{ color: "#0d5b63" }} />
                <Typography variant="h3">Driver Action Checklist</Typography>
              </Stack>

              {selectedPlan ? (
                <Stack spacing={2}>
                  {selectedPlan.steps.map((step) => (
                    <Box
                      key={step.sequence}
                      sx={{
                        p: 1.5,
                        borderRadius: 2.5,
                        bgcolor: step.sequence === 1 ? "#ecfdf5" : "#f8fafc",
                        border: `1px solid ${step.sequence === 1 ? "#a7f3d0" : "#e2e8f0"}`
                      }}
                    >
                      <Stack direction="row" spacing={1.5} alignItems="flex-start">
                        <Box
                          sx={{
                            width: 24,
                            height: 24,
                            borderRadius: "50%",
                            bgcolor: step.sequence === 1 ? "#10b981" : "#071b2a",
                            color: "white",
                            display: "grid",
                            placeItems: "center",
                            fontSize: "0.75rem",
                            fontWeight: 700,
                            flexShrink: 0
                          }}
                        >
                          {step.sequence}
                        </Box>
                        <Box>
                          <Typography fontWeight={750} fontSize="0.88rem">
                            {step.actionCode.replaceAll("_", " ")}
                          </Typography>
                          <Typography variant="body2" color="text.secondary" mt={0.3}>
                            {step.instruction}
                          </Typography>
                        </Box>
                      </Stack>
                    </Box>
                  ))}
                </Stack>
              ) : (
                <Typography color="text.secondary" fontSize="0.9rem">
                  No active recovery plan assigned. Drive according to route schedule.
                </Typography>
              )}
            </CardContent>
          </Card>

          {/* Real-time Event Feed */}
          <Card sx={{ border: "1px solid #e2e8f0" }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h3" mb={2}>Operations Link Status</Typography>
              <Stack spacing={2}>
                <Stack direction="row" justifyContent="space-between" alignItems="center" p={1.5} borderRadius={2} bgcolor="#f8fafc">
                  <Typography variant="caption" color="text.secondary" fontWeight={700}>STREAM PROTOCOL</Typography>
                  <Typography variant="caption" fontWeight={700}>WebSocket / Single-Use Ticket</Typography>
                </Stack>

                {latestEvent ? (
                  <Box p={1.5} borderRadius={2} bgcolor="#f0f9ff" border="1px solid #bae6fd">
                    <Typography variant="caption" color="info.dark" fontWeight={700}>LATEST EVENT DISPATCH</Typography>
                    <Typography fontWeight={700} fontSize="0.85rem" mt={0.3}>
                      {latestEvent.type.replaceAll("_", " ")}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" display="block">
                      {new Date(latestEvent.occurredAt).toLocaleTimeString()}
                    </Typography>
                  </Box>
                ) : (
                  <Typography variant="caption" color="text.secondary">
                    Listening for server-side disruption and plan events…
                  </Typography>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Stack>
      </Stack>
    </Stack>
  );
}

function createObservation(
  tripId: string,
  engine: number,
  cargo: number,
  speed: number,
  networkState: NetworkState,
  source: "DRIVER_DEVICE" | "SIMULATOR"
): TelemetryObservation {
  const recordedAt = new Date();
  return {
    eventId: crypto.randomUUID(),
    tripId,
    vehicleId,
    recordedAt: recordedAt.toISOString(),
    receivedAt: recordedAt.toISOString(),
    expiresAt: new Date(recordedAt.getTime() + 5 * 60_000).toISOString(),
    location: { latitude: 13.0358, longitude: 77.597 },
    engineTemperatureCelsius: engine,
    cargoTemperatureCelsius: cargo,
    speedKph: speed,
    confidence: 0.98,
    observationStatus: "FRESH",
    source,
    networkState
  };
}
