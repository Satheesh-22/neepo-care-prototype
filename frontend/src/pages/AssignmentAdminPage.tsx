import AddRounded from "@mui/icons-material/AddRounded";
import AssignmentTurnedInRounded from "@mui/icons-material/AssignmentTurnedInRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import DoneAllRounded from "@mui/icons-material/DoneAllRounded";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import WarningAmberRounded from "@mui/icons-material/WarningAmberRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Grid from "@mui/material/Grid";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { api } from "../api";
import type { CreateAssignmentInput, DriverVehicleAssignment } from "../api/types";
import { useAuthStore } from "../auth/authStore";

export function AssignmentAdminPage() {
  const session = useAuthStore((state) => state.session)!;
  const queryClient = useQueryClient();

  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: assignments = [], isPending: loadingAssignments, error: assignmentsError } = useQuery({
    queryKey: ["assignments"],
    queryFn: () => api.listAssignments(session.accessToken)
  });

  const { data: drivers = [], isPending: loadingDrivers, error: driversError } = useQuery({
    queryKey: ["drivers"],
    queryFn: () => api.listDrivers(session.accessToken)
  });

  const { data: vehicles = [], isPending: loadingVehicles, error: vehiclesError } = useQuery({
    queryKey: ["vehicles"],
    queryFn: () => api.listVehicles(session.accessToken)
  });

  const loading = loadingAssignments || loadingDrivers || loadingVehicles;
  const queryError = assignmentsError || driversError || vehiclesError;

  // New Assignment Dialog
  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState<CreateAssignmentInput>({
    driverId: "",
    vehicleId: "",
    startTime: new Date().toISOString().slice(0, 16),
    endTime: "",
    notes: ""
  });
  const [creating, setCreating] = useState(false);

  // Release Confirmation Dialog
  const [releaseTarget, setReleaseTarget] = useState<DriverVehicleAssignment | null>(null);
  const [releasing, setReleasing] = useState(false);

  const handleCreateSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setError(null);

    const selectedDriver = drivers.find((d) => d.id === form.driverId);
    const selectedVehicle = vehicles.find((v) => v.id === form.vehicleId);

    if (selectedDriver?.licenseExpired) {
      setError("Cannot assign driver with an expired commercial license.");
      setCreating(false);
      return;
    }

    if (selectedDriver?.verificationState === "SUSPENDED") {
      setError("Cannot assign a suspended driver.");
      setCreating(false);
      return;
    }

    if (selectedVehicle?.status === "MAINTENANCE" || selectedVehicle?.status === "OUT_OF_SERVICE") {
      setError("Cannot assign a vehicle in maintenance or out of service.");
      setCreating(false);
      return;
    }

    try {
      await api.createAssignment(
        {
          driverId: form.driverId,
          vehicleId: form.vehicleId,
          startTime: form.startTime ? new Date(form.startTime).toISOString() : undefined,
          endTime: form.endTime ? new Date(form.endTime).toISOString() : undefined,
          notes: form.notes
        },
        session.accessToken
      );
      setSuccess("Driver-to-vehicle assignment created successfully.");
      setCreateOpen(false);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["assignments"] }),
        queryClient.invalidateQueries({ queryKey: ["drivers"] }),
        queryClient.invalidateQueries({ queryKey: ["vehicles"] })
      ]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create assignment");
    } finally {
      setCreating(false);
    }
  };

  const handleRelease = async () => {
    if (!releaseTarget) return;
    setReleasing(true);
    setError(null);
    try {
      await api.releaseAssignment(releaseTarget.id, session.accessToken);
      setSuccess(`Assignment for ${releaseTarget.driverName} and ${releaseTarget.vehicleRegistrationNumber} released.`);
      setReleaseTarget(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["assignments"] }),
        queryClient.invalidateQueries({ queryKey: ["drivers"] }),
        queryClient.invalidateQueries({ queryKey: ["vehicles"] })
      ]);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to release assignment");
    } finally {
      setReleasing(false);
    }
  };

  const availableDrivers = drivers.filter(
    (d) => d.status === "AVAILABLE" && !d.licenseExpired && d.verificationState !== "SUSPENDED"
  );
  const availableVehicles = vehicles.filter((v) => v.status === "AVAILABLE");

  return (
    <Box>
      {/* Header */}
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ sm: "center" }} spacing={2} mb={3}>
        <Box>
          <Stack direction="row" alignItems="center" spacing={1.5}>
            <AssignmentTurnedInRounded sx={{ fontSize: 28, color: "primary.main" }} />
            <Typography variant="h5" fontWeight={800}>
              Driver-to-Vehicle Allocations
            </Typography>
          </Stack>
          <Typography variant="body2" color="text.secondary">
            Assign qualified certified drivers to verified fleet vehicles with conflict checks and license validation.
          </Typography>
        </Box>

        <Stack direction="row" spacing={1.5}>
          <Button
            variant="outlined"
            startIcon={loading ? <CircularProgress size={16} /> : <RefreshRounded />}
            onClick={() => {
              void Promise.all([
                queryClient.invalidateQueries({ queryKey: ["assignments"] }),
                queryClient.invalidateQueries({ queryKey: ["drivers"] }),
                queryClient.invalidateQueries({ queryKey: ["vehicles"] })
              ]);
            }}
            disabled={loading}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddRounded />}
            onClick={() => {
              setForm({
                driverId: availableDrivers[0]?.id ?? "",
                vehicleId: availableVehicles[0]?.id ?? "",
                startTime: new Date().toISOString().slice(0, 16),
                endTime: "",
                notes: ""
              });
              setCreateOpen(true);
            }}
            sx={{ fontWeight: 700 }}
          >
            New Assignment
          </Button>
        </Stack>
      </Stack>

      {(error || queryError) && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error ?? (queryError instanceof Error ? queryError.message : "Failed to load assignment data")}
        </Alert>
      )}

      {success && (
        <Alert severity="success" icon={<CheckCircleRounded />} sx={{ mb: 3 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {/* Summary Cards */}
      <Grid container spacing={2.5} mb={3}>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Card variant="outlined" sx={{ borderRadius: 2.5 }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight={700}>
                ACTIVE ASSIGNMENTS
              </Typography>
              <Typography variant="h4" fontWeight={800} color="primary.main">
                {assignments.filter((a) => a.status === "ACTIVE").length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Card variant="outlined" sx={{ borderRadius: 2.5 }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight={700}>
                AVAILABLE DRIVERS
              </Typography>
              <Typography variant="h4" fontWeight={800} color="success.main">
                {availableDrivers.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <Card variant="outlined" sx={{ borderRadius: 2.5 }}>
            <CardContent sx={{ p: 2 }}>
              <Typography variant="caption" color="text.secondary" fontWeight={700}>
                AVAILABLE VEHICLES
              </Typography>
              <Typography variant="h4" fontWeight={800} color="info.main">
                {availableVehicles.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Assignments Table */}
      <Card variant="outlined" sx={{ borderRadius: 3, overflow: "hidden" }}>
        <TableContainer>
          <Table>
            <TableHead sx={{ bgcolor: "action.hover" }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>Driver</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Vehicle</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Start Time</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>End Time / Released</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Notes</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                <TableCell align="right" sx={{ fontWeight: 700 }}>
                  Actions
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <CircularProgress size={32} />
                  </TableCell>
                </TableRow>
              ) : assignments.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <Typography variant="body2" color="text.secondary">
                      No driver-vehicle assignments found. Click "New Assignment" to allocate assets.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                assignments.map((a) => {
                  const isActive = a.status === "ACTIVE";
                  return (
                    <TableRow key={a.id} hover>
                      <TableCell>
                        <Typography variant="subtitle2" fontWeight={700}>
                          {a.driverName}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Lic: {a.driverLicenseNumber} {a.driverEmployeeId ? `• ${a.driverEmployeeId}` : ""}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight={700}>
                          {a.vehicleRegistrationNumber}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {a.vehicleType}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(a.startTime).toLocaleString()}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" color="text.secondary">
                          {a.releasedAt ? new Date(a.releasedAt).toLocaleString() : a.endTime ? new Date(a.endTime).toLocaleString() : "Ongoing"}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {a.notes || "—"}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={a.status}
                          size="small"
                          color={isActive ? "primary" : "default"}
                          variant={isActive ? "filled" : "outlined"}
                          sx={{ fontWeight: 700 }}
                        />
                      </TableCell>
                      <TableCell align="right">
                        {isActive && (
                          <Button
                            size="small"
                            variant="outlined"
                            color="warning"
                            onClick={() => setReleaseTarget(a)}
                            sx={{ fontSize: "0.75rem", fontWeight: 700 }}
                          >
                            Release
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      {/* New Assignment Dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>Allocate Driver to Vehicle</DialogTitle>
        <DialogContent dividers>
          <Box component="form" id="create-assignment-form" onSubmit={handleCreateSubmit} sx={{ pt: 1 }}>
            <Stack spacing={2.5}>
              <TextField
                select
                fullWidth
                label="Select Driver *"
                value={form.driverId}
                onChange={(e) => setForm({ ...form, driverId: e.target.value })}
                required
                disabled={creating}
                helperText="Only verified drivers with valid unexpired licenses are eligible"
              >
                {drivers.map((d) => {
                  const isAvailable = d.status === "AVAILABLE" && !d.licenseExpired && d.verificationState !== "SUSPENDED";
                  return (
                    <MenuItem key={d.id} value={d.id} disabled={!isAvailable}>
                      {d.name} ({d.licenseNumber}) — {d.status} {d.licenseExpired ? " [LICENSE EXPIRED]" : ""}
                    </MenuItem>
                  );
                })}
              </TextField>

              <TextField
                select
                fullWidth
                label="Select Vehicle *"
                value={form.vehicleId}
                onChange={(e) => setForm({ ...form, vehicleId: e.target.value })}
                required
                disabled={creating}
                helperText="Only available vehicles not undergoing maintenance can be allocated"
              >
                {vehicles.map((v) => {
                  const isAvailable = v.status === "AVAILABLE";
                  return (
                    <MenuItem key={v.id} value={v.id} disabled={!isAvailable}>
                      {v.registrationNumber} ({v.make} {v.model}) — {v.status}
                    </MenuItem>
                  );
                })}
              </TextField>

              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="datetime-local"
                    label="Assignment Start Time"
                    value={form.startTime}
                    onChange={(e) => setForm({ ...form, startTime: e.target.value })}
                    InputLabelProps={{ shrink: true }}
                    disabled={creating}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="datetime-local"
                    label="Expected End Time (Optional)"
                    value={form.endTime}
                    onChange={(e) => setForm({ ...form, endTime: e.target.value })}
                    InputLabelProps={{ shrink: true }}
                    disabled={creating}
                  />
                </Grid>
              </Grid>

              <TextField
                fullWidth
                multiline
                rows={2}
                label="Dispatch Notes / Route Allocation"
                value={form.notes}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
                disabled={creating}
              />
            </Stack>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setCreateOpen(false)} disabled={creating}>
            Cancel
          </Button>
          <Button
            type="submit"
            form="create-assignment-form"
            variant="contained"
            disabled={creating || !form.driverId || !form.vehicleId}
            startIcon={creating ? <CircularProgress size={16} color="inherit" /> : <DoneAllRounded />}
          >
            {creating ? "Assigning…" : "Confirm Assignment"}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Release Confirmation Dialog */}
      <Dialog open={Boolean(releaseTarget)} onClose={() => setReleaseTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Release Vehicle Assignment?</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} alignItems="center" textAlign="center" pt={1}>
            <WarningAmberRounded color="warning" sx={{ fontSize: 44 }} />
            <Typography variant="body2" color="text.secondary">
              Are you sure you want to release <strong>{releaseTarget?.vehicleRegistrationNumber}</strong> from driver <strong>{releaseTarget?.driverName}</strong>? Both assets will return to the AVAILABLE pool.
            </Typography>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setReleaseTarget(null)} disabled={releasing}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color="warning"
            onClick={() => void handleRelease()}
            disabled={releasing}
            startIcon={releasing ? <CircularProgress size={16} color="inherit" /> : null}
          >
            {releasing ? "Releasing…" : "Confirm Release"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
