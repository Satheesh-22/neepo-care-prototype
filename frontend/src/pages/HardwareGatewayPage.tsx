import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import CloseRounded from "@mui/icons-material/CloseRounded";
import CloudSyncRounded from "@mui/icons-material/CloudSyncRounded";
import DirectionsCarRounded from "@mui/icons-material/DirectionsCarRounded";
import MemoryRounded from "@mui/icons-material/MemoryRounded";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import RouterRounded from "@mui/icons-material/RouterRounded";
import SearchRounded from "@mui/icons-material/SearchRounded";
import SecurityRounded from "@mui/icons-material/SecurityRounded";
import SensorsRounded from "@mui/icons-material/SensorsRounded";
import SignalCellularAltRounded from "@mui/icons-material/SignalCellularAltRounded";
import StorageRounded from "@mui/icons-material/StorageRounded";
import VpnKeyRounded from "@mui/icons-material/VpnKeyRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { neepoApi } from "../api";
import { DEMO_TRIP_ID } from "../api/mockApi";
import type {
  GatewayBatchSyncResponse,
  GatewayHeartbeatResponse,
  GatewayOtaConfigResponse,
  HardwareGatewayItem,
  RegisterHardwareGatewayRequest
} from "../api/types";
import { useAuthStore } from "../auth/authStore";

function formatTimestamp(isoString?: string | null): string {
  if (!isoString) return "Never";
  try {
    const d = new Date(isoString);
    return d.toLocaleString([], {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
  } catch {
    return isoString;
  }
}

export function HardwareGatewayPage() {
  const session = useAuthStore((state) => state.session)!;
  const queryClient = useQueryClient();

  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [registerModalOpen, setRegisterModalOpen] = useState(false);
  const [diagnosticsGateway, setDiagnosticsGateway] = useState<HardwareGatewayItem | null>(null);

  // Form state for registration
  const [formSerial, setFormSerial] = useState("");
  const [formVehicleId, setFormVehicleId] = useState<string>("");
  const [formImei, setFormImei] = useState("");
  const [formSecureElementId, setFormSecureElementId] = useState("");
  const [formFirmware, setFormFirmware] = useState("v1.1.0-prod");
  const [formHmacSecret, setFormHmacSecret] = useState("device_secret_key_prod_sec_456");
  const [formBufferCapacity, setFormBufferCapacity] = useState(10000);
  const [formInterval, setFormInterval] = useState(5);
  const [formError, setFormError] = useState<string | null>(null);

  // Diagnostics feedback state
  const [diagnosticMsg, setDiagnosticMsg] = useState<{
    type: "success" | "info" | "error";
    text: string;
  } | null>(null);

  // Queries
  const {
    data: gateways = [],
    isPending,
    isError,
    error,
    refetch,
    isFetching
  } = useQuery({
    queryKey: ["hardwareGateways"],
    queryFn: () => neepoApi.listGateways(session.accessToken)
  });

  const { data: vehicles = [] } = useQuery({
    queryKey: ["vehicles"],
    queryFn: () => neepoApi.listVehicles(session.accessToken)
  });

  // Diagnostics OTA Config Query
  const { data: otaConfig, isPending: otaPending } = useQuery({
    queryKey: ["gatewayOtaConfig", diagnosticsGateway?.id],
    queryFn: () =>
      diagnosticsGateway
        ? neepoApi.getGatewayOtaConfig(diagnosticsGateway.id, session.accessToken)
        : Promise.resolve(null as unknown as GatewayOtaConfigResponse),
    enabled: Boolean(diagnosticsGateway)
  });

  // Register mutation
  const registerMutation = useMutation({
    mutationFn: (req: RegisterHardwareGatewayRequest) =>
      neepoApi.registerGateway(req, session.accessToken),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["hardwareGateways"] });
      setRegisterModalOpen(false);
      resetRegisterForm();
    },
    onError: (err: Error) => {
      setFormError(err.message || "Failed to register gateway");
    }
  });

  // Heartbeat mutation
  const heartbeatMutation = useMutation({
    mutationFn: ({ gatewayId, firmwareVersion }: { gatewayId: string; firmwareVersion?: string }) =>
      neepoApi.sendGatewayHeartbeat(
        gatewayId,
        {
          firmwareVersion: firmwareVersion || "v1.1.0-prod",
          batteryVoltage: 12.6,
          signalRssi: -65,
          bufferedRecordsCount: 0
        },
        session.accessToken
      ),
    onSuccess: (data: GatewayHeartbeatResponse) => {
      queryClient.invalidateQueries({ queryKey: ["hardwareGateways"] });
      setDiagnosticMsg({
        type: "success",
        text: `Heartbeat acknowledged at ${new Date(data.serverTime).toLocaleTimeString()}. Status: ${data.status}.`
      });
    },
    onError: (err: Error) => {
      setDiagnosticMsg({
        type: "error",
        text: `Heartbeat failed: ${err.message}`
      });
    }
  });

  // Flash batch replay mutation
  const syncBatchMutation = useMutation({
    mutationFn: ({ gatewayId }: { gatewayId: string }) => {
      const now = new Date();
      const rec1EventId = crypto.randomUUID();
      const rec2EventId = crypto.randomUUID();
      const tripId = otaConfig?.activeTripId || DEMO_TRIP_ID;
      const vehicleId = diagnosticsGateway?.vehicleId || "00000000-0000-4000-8300-000000000001";

      return neepoApi.syncGatewayBatch(
        gatewayId,
        {
          tripId,
          syncMode: "OFFLINE_REPLAY",
          networkCarrier: "Verizon Telematics Cat-4",
          signalRssi: -72,
          records: [
            {
              eventId: rec1EventId,
              tripId,
              vehicleId,
              recordedAt: new Date(now.getTime() - 15000).toISOString(),
              receivedAt: now.toISOString(),
              expiresAt: new Date(now.getTime() + 300000).toISOString(),
              location: { latitude: 11.42345, longitude: 77.69123 },
              engineTemperatureCelsius: 94.2,
              cargoTemperatureCelsius: 4.2,
              speedKph: 52.0,
              confidence: 0.99,
              observationStatus: "FRESH",
              source: "VEHICLE_DEVICE",
              networkState: "SYNCHRONIZING"
            },
            {
              eventId: rec2EventId,
              tripId,
              vehicleId,
              recordedAt: new Date(now.getTime() - 10000).toISOString(),
              receivedAt: now.toISOString(),
              expiresAt: new Date(now.getTime() + 300000).toISOString(),
              location: { latitude: 11.4251, longitude: 77.6928 },
              engineTemperatureCelsius: 94.8,
              cargoTemperatureCelsius: 4.4,
              speedKph: 55.5,
              confidence: 0.99,
              observationStatus: "FRESH",
              source: "VEHICLE_DEVICE",
              networkState: "SYNCHRONIZING"
            }
          ]
        },
        session.accessToken
      );
    },
    onSuccess: (data: GatewayBatchSyncResponse) => {
      queryClient.invalidateQueries({ queryKey: ["hardwareGateways"] });
      setDiagnosticMsg({
        type: "success",
        text: `SPI Flash Replay Succeeded! Batch size: ${data.batchSize}, Accepted: ${data.acceptedCount}, Deduplicated: ${data.deduplicatedCount}. Mode: ${data.syncMode}.`
      });
    },
    onError: (err: Error) => {
      setDiagnosticMsg({
        type: "error",
        text: `Flash sync failed: ${err.message}`
      });
    }
  });

  const resetRegisterForm = () => {
    setFormSerial("");
    setFormVehicleId("");
    setFormImei("");
    setFormSecureElementId("");
    setFormFirmware("v1.1.0-prod");
    setFormHmacSecret("device_secret_key_prod_sec_456");
    setFormBufferCapacity(10000);
    setFormInterval(5);
    setFormError(null);
  };

  const handleRegisterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formSerial.trim()) {
      setFormError("Device serial is required");
      return;
    }
    setFormError(null);
    registerMutation.mutate({
      deviceSerial: formSerial.trim(),
      vehicleId: formVehicleId || undefined,
      imei: formImei.trim() || undefined,
      secureElementId: formSecureElementId.trim() || undefined,
      firmwareVersion: formFirmware.trim() || "v1.1.0-prod",
      hmacSecret: formHmacSecret.trim() || "device_secret_key_prod_sec_456",
      bufferCapacityRecords: Number(formBufferCapacity) || 10000,
      reportingIntervalSeconds: Number(formInterval) || 5,
      metadata: {
        mcu: "ESP32-S3-WROOM-1",
        cellularModem: "SIM7600G-H (LTE Cat-4)",
        flashStorage: "32MB SPI NOR (W25Q256JV)"
      }
    });
  };

  // Filtered gateways
  const filteredGateways = useMemo(() => {
    return gateways.filter((gw) => {
      const matchesSearch =
        gw.deviceSerial.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (gw.imei && gw.imei.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (gw.secureElementId && gw.secureElementId.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesStatus = statusFilter === "ALL" || gw.status.toUpperCase() === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [gateways, searchQuery, statusFilter]);

  // Aggregate stats
  const activeCount = gateways.filter((g) => g.status === "ACTIVE").length;
  const offlineCount = gateways.filter((g) => g.status === "OFFLINE").length;
  const totalCapacity = gateways.reduce((acc, g) => acc + (g.bufferCapacityRecords || 0), 0);

  const vehiclePlateMap = useMemo(() => {
    const map = new Map<string, string>();
    for (const v of vehicles) {
      map.set(v.id, v.registrationNumber);
    }
    return map;
  }, [vehicles]);

  return (
    <Stack spacing={3.5}>
      {/* Executive Header Banner */}
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
            right: -20,
            bottom: -35,
            opacity: 0.06,
            transform: "rotate(-12deg)",
            pointerEvents: "none"
          }}
        >
          <RouterRounded sx={{ fontSize: 260 }} />
        </Box>

        <Stack
          direction={{ xs: "column", md: "row" }}
          alignItems={{ xs: "flex-start", md: "center" }}
          justifyContent="space-between"
          spacing={2.5}
        >
          <Box>
            <Stack direction="row" alignItems="center" spacing={1.5} mb={0.75}>
              <Chip
                icon={<MemoryRounded sx={{ fontSize: "16px !important", color: "inherit" }} />}
                label="v1.1 Hardware Pilot"
                size="small"
                sx={{
                  bgcolor: "rgba(255, 255, 255, 0.15)",
                  color: "#e2f1f8",
                  fontWeight: 700,
                  backdropFilter: "blur(4px)"
                }}
              />
              <Chip
                icon={<CloudSyncRounded sx={{ fontSize: "16px !important", color: "inherit" }} />}
                label="Offline Flash Resilience"
                size="small"
                sx={{
                  bgcolor: "rgba(34, 197, 94, 0.2)",
                  color: "#86efac",
                  fontWeight: 700
                }}
              />
            </Stack>
            <Typography variant="h4" fontWeight={800} letterSpacing="-0.02em">
              Hardware Gateway Fleet Pilot
            </Typography>
            <Typography variant="body1" sx={{ color: "rgba(255, 255, 255, 0.8)", mt: 0.5, maxWidth: 680 }}>
              Bench-tested ESP32-S3 edge gateways, ATECC608A cryptographic authentication, SPI NOR flash
              offline queueing with zero-loss replay, and dynamic OTA policy synchronization.
            </Typography>
          </Box>

          <Stack direction="row" spacing={1.5}>
            <Button
              variant="outlined"
              startIcon={<RefreshRounded />}
              onClick={() => refetch()}
              disabled={isFetching}
              sx={{
                color: "white",
                borderColor: "rgba(255, 255, 255, 0.3)",
                "&:hover": { borderColor: "white", bgcolor: "rgba(255, 255, 255, 0.08)" }
              }}
            >
              Refresh
            </Button>
            <Button
              variant="contained"
              startIcon={<RouterRounded />}
              onClick={() => {
                resetRegisterForm();
                setRegisterModalOpen(true);
              }}
              sx={{
                bgcolor: "#00a884",
                color: "#ffffff",
                fontWeight: 700,
                "&:hover": { bgcolor: "#008f6f" }
              }}
            >
              Register Gateway
            </Button>
          </Stack>
        </Stack>
      </Box>

      {/* KPI Cards Grid */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)", md: "repeat(4, 1fr)" },
          gap: 2.5
        }}
      >
        <Card
          sx={{
            p: 2.5,
            borderRadius: 3,
            boxShadow: "0 2px 12px rgba(0,0,0,0.04)",
            border: "1px solid rgba(0,0,0,0.06)"
          }}
        >
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="body2" color="text.secondary" fontWeight={600}>
              Active Edge Gateways
            </Typography>
            <Box sx={{ p: 1, borderRadius: 2, bgcolor: "rgba(34, 197, 94, 0.12)", color: "#16a34a" }}>
              <SensorsRounded />
            </Box>
          </Stack>
          <Typography variant="h4" fontWeight={800} sx={{ mt: 1 }}>
            {activeCount} <Typography component="span" variant="body1" color="text.secondary">/ {gateways.length}</Typography>
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {activeCount} active, {offlineCount} offline ({gateways.length > 0 ? Math.round((activeCount / gateways.length) * 100) : 0}% online)
          </Typography>
        </Card>

        <Card
          sx={{
            p: 2.5,
            borderRadius: 3,
            boxShadow: "0 2px 12px rgba(0,0,0,0.04)",
            border: "1px solid rgba(0,0,0,0.06)"
          }}
        >
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="body2" color="text.secondary" fontWeight={600}>
              SPI Flash Buffer Capacity
            </Typography>
            <Box sx={{ p: 1, borderRadius: 2, bgcolor: "rgba(14, 165, 233, 0.12)", color: "#0284c7" }}>
              <StorageRounded />
            </Box>
          </Stack>
          <Typography variant="h4" fontWeight={800} sx={{ mt: 1 }}>
            {totalCapacity.toLocaleString()}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Records bufferable across fleet (32MB NOR)
          </Typography>
        </Card>

        <Card
          sx={{
            p: 2.5,
            borderRadius: 3,
            boxShadow: "0 2px 12px rgba(0,0,0,0.04)",
            border: "1px solid rgba(0,0,0,0.06)"
          }}
        >
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="body2" color="text.secondary" fontWeight={600}>
              Cellular Link Health
            </Typography>
            <Box sx={{ p: 1, borderRadius: 2, bgcolor: "rgba(168, 85, 247, 0.12)", color: "#9333ea" }}>
              <SignalCellularAltRounded />
            </Box>
          </Stack>
          <Typography variant="h4" fontWeight={800} sx={{ mt: 1 }}>
            LTE Cat-4
          </Typography>
          <Typography variant="caption" color="text.secondary">
            SIM7600G-H high-gain antenna, avg -65 dBm
          </Typography>
        </Card>

        <Card
          sx={{
            p: 2.5,
            borderRadius: 3,
            boxShadow: "0 2px 12px rgba(0,0,0,0.04)",
            border: "1px solid rgba(0,0,0,0.06)"
          }}
        >
          <Stack direction="row" alignItems="center" justifyContent="space-between">
            <Typography variant="body2" color="text.secondary" fontWeight={600}>
              Hardware Security
            </Typography>
            <Box sx={{ p: 1, borderRadius: 2, bgcolor: "rgba(234, 88, 12, 0.12)", color: "#ea580c" }}>
              <SecurityRounded />
            </Box>
          </Stack>
          <Typography variant="h4" fontWeight={800} sx={{ mt: 1 }}>
            ATECC608A
          </Typography>
          <Typography variant="caption" color="text.secondary">
            HMAC SHA-256 cryptographic signature
          </Typography>
        </Card>
      </Box>

      {/* Main Table Card */}
      <Card sx={{ borderRadius: 3, border: "1px solid rgba(0,0,0,0.06)", overflow: "hidden" }}>
        {/* Search and Filters Bar */}
        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={2}
          p={2.5}
          borderBottom="1px solid rgba(0,0,0,0.06)"
          alignItems="center"
          justifyContent="space-between"
        >
          <TextField
            size="small"
            placeholder="Search by serial number, IMEI, or secure ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            sx={{ width: { xs: "100%", sm: 360 } }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchRounded fontSize="small" sx={{ color: "text.secondary" }} />
                </InputAdornment>
              )
            }}
          />

          <Stack direction="row" spacing={1} alignItems="center">
            <Typography variant="body2" color="text.secondary" fontWeight={600}>
              Status:
            </Typography>
            {(["ALL", "ACTIVE", "OFFLINE", "REGISTERED"] as const).map((st) => (
              <Chip
                key={st}
                label={st}
                size="small"
                clickable
                color={statusFilter === st ? "primary" : "default"}
                variant={statusFilter === st ? "filled" : "outlined"}
                onClick={() => setStatusFilter(st)}
                sx={{ fontWeight: 600 }}
              />
            ))}
          </Stack>
        </Stack>

        {isPending ? (
          <Box p={6} textAlign="center">
            <CircularProgress size={36} />
            <Typography variant="body2" color="text.secondary" mt={2}>
              Loading edge gateway fleet...
            </Typography>
          </Box>
        ) : isError ? (
          <Box p={3}>
            <Alert severity="error">
              {error instanceof Error ? error.message : "Failed to load hardware gateways"}
            </Alert>
          </Box>
        ) : filteredGateways.length === 0 ? (
          <Box p={6} textAlign="center">
            <RouterRounded sx={{ fontSize: 48, color: "text.disabled", mb: 1 }} />
            <Typography variant="h6" fontWeight={700}>
              No Gateways Found
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {gateways.length === 0
                ? "No hardware gateways registered yet. Register a new gateway device to start edge pilot ingestion."
                : "No gateways match the specified filter or search query."}
            </Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead sx={{ bgcolor: "rgba(0,0,0,0.02)" }}>
                <TableRow>
                  <TableCell sx={{ fontWeight: 700 }}>Device Serial & Hardware</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Assigned Vehicle</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Firmware</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Buffer Capacity</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Last Heartbeat</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700 }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredGateways.map((gw) => {
                  const vehiclePlate = gw.vehicleId ? vehiclePlateMap.get(gw.vehicleId) || "Vehicle Assigned" : "Unassigned";

                  const statusColor =
                    gw.status === "ACTIVE"
                      ? "success"
                      : gw.status === "OFFLINE"
                      ? "error"
                      : "info";

                  return (
                    <TableRow key={gw.id} hover>
                      <TableCell>
                        <Stack direction="row" spacing={1.5} alignItems="center">
                          <Box
                            sx={{
                              width: 36,
                              height: 36,
                              borderRadius: 2,
                              bgcolor: "rgba(7, 27, 42, 0.06)",
                              display: "grid",
                              placeItems: "center",
                              color: "#071b2a"
                            }}
                          >
                            <MemoryRounded fontSize="small" />
                          </Box>
                          <Box>
                            <Typography variant="subtitle2" fontWeight={700}>
                              {gw.deviceSerial}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {gw.secureElementId || "ATECC608A"} • {gw.imei ? `IMEI: ${gw.imei.slice(-6)}` : "No IMEI"}
                            </Typography>
                          </Box>
                        </Stack>
                      </TableCell>

                      <TableCell>
                        {gw.vehicleId ? (
                          <Chip
                            icon={<DirectionsCarRounded sx={{ fontSize: 16 }} />}
                            label={vehiclePlate}
                            size="small"
                            variant="outlined"
                            sx={{ fontWeight: 600 }}
                          />
                        ) : (
                          <Typography variant="body2" color="text.disabled">
                            Unassigned
                          </Typography>
                        )}
                      </TableCell>

                      <TableCell>
                        <Chip
                          label={gw.firmwareVersion}
                          size="small"
                          sx={{ bgcolor: "rgba(0,0,0,0.05)", fontWeight: 600 }}
                        />
                      </TableCell>

                      <TableCell>
                        <Typography variant="body2" fontWeight={600}>
                          {gw.bufferCapacityRecords.toLocaleString()} recs
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {gw.reportingIntervalSeconds}s interval
                        </Typography>
                      </TableCell>

                      <TableCell>
                        <Chip
                          label={gw.status}
                          size="small"
                          color={statusColor}
                          sx={{ fontWeight: 700 }}
                        />
                      </TableCell>

                      <TableCell>
                        <Typography variant="body2">
                          {formatTimestamp(gw.lastHeartbeatAt)}
                        </Typography>
                      </TableCell>

                      <TableCell align="right">
                        <Stack direction="row" spacing={1} justifyContent="flex-end">
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={<SensorsRounded />}
                            onClick={() => {
                              setDiagnosticMsg(null);
                              setDiagnosticsGateway(gw);
                            }}
                            sx={{ textTransform: "none", fontWeight: 600 }}
                          >
                            OTA & Diagnostics
                          </Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      {/* Register Gateway Dialog */}
      <Dialog
        open={registerModalOpen}
        onClose={() => setRegisterModalOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <form onSubmit={handleRegisterSubmit}>
          <DialogTitle sx={{ fontWeight: 800 }}>
            Register New Hardware Edge Gateway
          </DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2.5} pt={1}>
              {formError && (
                <Alert severity="error">{formError}</Alert>
              )}

              <TextField
                label="Device Serial Number"
                required
                fullWidth
                placeholder="e.g. NEEPO-GW-2026-003"
                value={formSerial}
                onChange={(e) => setFormSerial(e.target.value)}
                helperText="Unique identifier etched into gateway hardware"
              />

              <FormControl fullWidth>
                <InputLabel id="vehicle-select-label">Assign to Vehicle</InputLabel>
                <Select
                  labelId="vehicle-select-label"
                  label="Assign to Vehicle"
                  value={formVehicleId}
                  onChange={(e) => setFormVehicleId(e.target.value)}
                >
                  <MenuItem value="">
                    <em>Leave Unassigned</em>
                  </MenuItem>
                  {vehicles.map((v) => (
                    <MenuItem key={v.id} value={v.id}>
                      {v.registrationNumber} ({v.make} {v.model})
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" }, gap: 2 }}>
                <TextField
                  label="Modem IMEI (15 Digits)"
                  fullWidth
                  placeholder="867530901234569"
                  value={formImei}
                  onChange={(e) => setFormImei(e.target.value)}
                />
                <TextField
                  label="Secure Element ID"
                  fullWidth
                  placeholder="ATECC608A-003C-D6E4"
                  value={formSecureElementId}
                  onChange={(e) => setFormSecureElementId(e.target.value)}
                />
              </Box>

              <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" }, gap: 2 }}>
                <TextField
                  label="Firmware Version"
                  fullWidth
                  value={formFirmware}
                  onChange={(e) => setFormFirmware(e.target.value)}
                />
                <TextField
                  label="Reporting Interval (Seconds)"
                  type="number"
                  fullWidth
                  value={formInterval}
                  onChange={(e) => setFormInterval(Number(e.target.value))}
                />
              </Box>

              <TextField
                label="HMAC Shared Secret"
                fullWidth
                type="password"
                placeholder="Secure cryptographic device secret"
                value={formHmacSecret}
                onChange={(e) => setFormHmacSecret(e.target.value)}
                helperText="Shared secret for SHA-256 HMAC signature verification"
              />

              <TextField
                label="Flash Buffer Capacity (Records)"
                type="number"
                fullWidth
                value={formBufferCapacity}
                onChange={(e) => setFormBufferCapacity(Number(e.target.value))}
                helperText="Maximum telemetry records stored in SPI NOR flash during offline periods"
              />
            </Stack>
          </DialogContent>
          <DialogActions sx={{ p: 2.5 }}>
            <Button onClick={() => setRegisterModalOpen(false)}>Cancel</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={registerMutation.isPending}
              sx={{ bgcolor: "#00a884", fontWeight: 700, "&:hover": { bgcolor: "#008f6f" } }}
            >
              {registerMutation.isPending ? <CircularProgress size={24} /> : "Register Device"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Diagnostics & OTA Drawer / Modal */}
      <Dialog
        open={Boolean(diagnosticsGateway)}
        onClose={() => setDiagnosticsGateway(null)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Box
              sx={{
                width: 38,
                height: 38,
                borderRadius: 2,
                bgcolor: "rgba(7, 27, 42, 0.08)",
                display: "grid",
                placeItems: "center",
                color: "#071b2a"
              }}
            >
              <SensorsRounded />
            </Box>
            <Box>
              <Typography variant="h6" fontWeight={800}>
                {diagnosticsGateway?.deviceSerial} Diagnostics & OTA
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Secure Hardware Gateway Config & Offline Sync Simulator
              </Typography>
            </Box>
          </Stack>
          <IconButton onClick={() => setDiagnosticsGateway(null)} size="small">
            <CloseRounded />
          </IconButton>
        </DialogTitle>

        <DialogContent dividers>
          <Stack spacing={3}>
            {diagnosticMsg && (
              <Alert
                severity={diagnosticMsg.type}
                onClose={() => setDiagnosticMsg(null)}
                sx={{ fontWeight: 600 }}
              >
                {diagnosticMsg.text}
              </Alert>
            )}

            {otaPending ? (
              <Box p={4} textAlign="center">
                <CircularProgress size={32} />
                <Typography variant="body2" color="text.secondary" mt={1}>
                  Fetching OTA device policy...
                </Typography>
              </Box>
            ) : otaConfig ? (
              <Stack spacing={2.5}>
                {/* OTA Policy Overview */}
                <Box
                  p={2.5}
                  borderRadius={3}
                  bgcolor="rgba(0,0,0,0.02)"
                  border="1px solid rgba(0,0,0,0.06)"
                >
                  <Typography variant="subtitle2" fontWeight={800} mb={1.5}>
                    Active Over-The-Air (OTA) Configuration Profile
                  </Typography>

                  <Box
                    sx={{
                      display: "grid",
                      gridTemplateColumns: { xs: "1fr", sm: "repeat(3, 1fr)" },
                      gap: 2
                    }}
                  >
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Ingestion Target
                      </Typography>
                      <Typography variant="body2" fontWeight={700}>
                        {otaConfig.ingestionEndpoint}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Reporting Rate
                      </Typography>
                      <Typography variant="body2" fontWeight={700}>
                        Every {otaConfig.reportingIntervalSeconds} seconds
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Firmware Target
                      </Typography>
                      <Typography variant="body2" fontWeight={700}>
                        {otaConfig.firmwareVersion}
                      </Typography>
                    </Box>

                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Active Vehicle ID
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {otaConfig.activeVehicleId ? otaConfig.activeVehicleId.slice(0, 8) + "..." : "None"}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Active Trip ID
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {otaConfig.activeTripId ? otaConfig.activeTripId.slice(0, 8) + "..." : "None"}
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Policy Timestamp
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {new Date(otaConfig.serverTime).toLocaleTimeString()}
                      </Typography>
                    </Box>
                  </Box>
                </Box>

                {/* Dynamic Envelope */}
                <Box
                  p={2.5}
                  borderRadius={3}
                  bgcolor="rgba(245, 158, 11, 0.05)"
                  border="1px solid rgba(245, 158, 11, 0.2)"
                >
                  <Typography variant="subtitle2" fontWeight={800} color="#b45309" mb={1}>
                    Vehicle Safety & Cold-Chain Dynamic Envelope
                  </Typography>
                  <Box
                    sx={{
                      display: "grid",
                      gridTemplateColumns: { xs: "1fr", sm: "repeat(3, 1fr)" },
                      gap: 2
                    }}
                  >
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Max Engine Temp
                      </Typography>
                      <Typography variant="h6" fontWeight={700} color="#b45309">
                        {otaConfig.maxEngineTemperatureCelsius}°C
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Cargo Min Temp
                      </Typography>
                      <Typography variant="h6" fontWeight={700} color="#0284c7">
                        {otaConfig.minCargoTemperatureCelsius}°C
                      </Typography>
                    </Box>
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Cargo Max Temp
                      </Typography>
                      <Typography variant="h6" fontWeight={700} color="#0284c7">
                        {otaConfig.maxCargoTemperatureCelsius}°C
                      </Typography>
                    </Box>
                  </Box>
                </Box>

                {/* Hardware Security Specifications */}
                <Box
                  p={2.5}
                  borderRadius={3}
                  bgcolor="rgba(14, 165, 233, 0.05)"
                  border="1px solid rgba(14, 165, 233, 0.2)"
                >
                  <Typography variant="subtitle2" fontWeight={800} color="#0369a1" mb={1}>
                    Hardware Architecture & Tamper Protection
                  </Typography>
                  <Stack spacing={1}>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <VpnKeyRounded sx={{ fontSize: 18, color: "#0284c7" }} />
                      <Typography variant="body2">
                        <strong>Microchip ATECC608A:</strong> Hardware elliptic curve and HMAC accelerator with anti-tamper casing.
                      </Typography>
                    </Stack>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <StorageRounded sx={{ fontSize: 18, color: "#0284c7" }} />
                      <Typography variant="body2">
                        <strong>Winbond 32MB SPI NOR Flash:</strong> Circular queue retaining up to {diagnosticsGateway?.bufferCapacityRecords} observations during cellular dropouts.
                      </Typography>
                    </Stack>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <CheckCircleRounded sx={{ fontSize: 18, color: "#0284c7" }} />
                      <Typography variant="body2">
                        <strong>Deduplication Idempotency:</strong> Replays do not trigger duplicate alert disruptions or corrupt telemetry sequences.
                      </Typography>
                    </Stack>
                  </Stack>
                </Box>

                {/* Diagnostics Interactive Simulator Actions */}
                <Box
                  p={2.5}
                  borderRadius={3}
                  bgcolor="#ffffff"
                  border="1px solid rgba(0,0,0,0.12)"
                  boxShadow="0 2px 8px rgba(0,0,0,0.03)"
                >
                  <Typography variant="subtitle2" fontWeight={800} mb={1}>
                    Live Edge Pilot Diagnostic Actions
                  </Typography>
                  <Typography variant="body2" color="text.secondary" mb={2}>
                    Execute hardware simulated signals directly to test backend authentication, heartbeat state updates, and SPI flash offline queue replay.
                  </Typography>

                  <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                    <Button
                      variant="contained"
                      startIcon={<SensorsRounded />}
                      disabled={heartbeatMutation.isPending}
                      onClick={() => {
                        if (diagnosticsGateway) {
                          heartbeatMutation.mutate({
                            gatewayId: diagnosticsGateway.id,
                            firmwareVersion: diagnosticsGateway.firmwareVersion
                          });
                        }
                      }}
                      sx={{ bgcolor: "#071b2a", "&:hover": { bgcolor: "#0d2b40" }, fontWeight: 700 }}
                    >
                      {heartbeatMutation.isPending ? <CircularProgress size={20} color="inherit" /> : "Send Edge Heartbeat"}
                    </Button>

                    <Button
                      variant="outlined"
                      color="primary"
                      startIcon={<CloudSyncRounded />}
                      disabled={syncBatchMutation.isPending}
                      onClick={() => {
                        if (diagnosticsGateway) {
                          syncBatchMutation.mutate({ gatewayId: diagnosticsGateway.id });
                        }
                      }}
                      sx={{ fontWeight: 700 }}
                    >
                      {syncBatchMutation.isPending ? <CircularProgress size={20} /> : "Simulate Offline Replay Batch"}
                    </Button>
                  </Stack>
                </Box>
              </Stack>
            ) : null}
          </Stack>
        </DialogContent>

        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setDiagnosticsGateway(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Stack>
  );
}
