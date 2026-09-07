import AcUnitRounded from "@mui/icons-material/AcUnitRounded";
import AddRounded from "@mui/icons-material/AddRounded";
import BuildRounded from "@mui/icons-material/BuildRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import EditRounded from "@mui/icons-material/EditRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import SearchRounded from "@mui/icons-material/SearchRounded";
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
import FormControlLabel from "@mui/material/FormControlLabel";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import type { UpdateVehicleInput, VehicleDetailed } from "../api/types";
import { useAuthStore } from "../auth/authStore";

export function VehicleListPage() {
  const session = useAuthStore((state) => state.session)!;
  const isAdmin = session.user.role === "ADMIN";
  const queryClient = useQueryClient();

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: vehicles = [], isPending: loading, error: queryError } = useQuery({
    queryKey: ["vehicles"],
    queryFn: () => api.listVehicles(session.accessToken)
  });

  // Status Change Dialog
  const [statusDialogVehicle, setStatusDialogVehicle] = useState<VehicleDetailed | null>(null);
  const [newStatus, setNewStatus] = useState<string>("AVAILABLE");
  const [statusChanging, setStatusChanging] = useState(false);

  // Edit Vehicle Dialog
  const [editOpen, setEditOpen] = useState(false);
  const [selectedVehicle, setSelectedVehicle] = useState<VehicleDetailed | null>(null);
  const [editForm, setEditForm] = useState<UpdateVehicleInput>({
    registrationNumber: "",
    vin: "",
    make: "",
    model: "",
    year: 2025,
    vehicleType: "REFRIGERATED_TRUCK",
    heightMeters: 3.8,
    widthMeters: 2.5,
    grossWeightKg: 18000,
    payloadCapacityKg: 8500,
    maximumEngineTemperatureCelsius: 105.0,
    refrigerationSupported: true,
    cargoTemperatureMinCelsius: -25.0,
    cargoTemperatureMaxCelsius: 10.0,
    deviceIdentifier: ""
  });
  const [savingEdit, setSavingEdit] = useState(false);

  const handleStatusSubmit = async () => {
    if (!statusDialogVehicle) return;
    setStatusChanging(true);
    setError(null);
    try {
      await api.updateVehicleStatus(statusDialogVehicle.id, newStatus, session.accessToken);
      setSuccess(`Vehicle ${statusDialogVehicle.registrationNumber} status updated to ${newStatus}.`);
      setStatusDialogVehicle(null);
      await queryClient.invalidateQueries({ queryKey: ["vehicles"] });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update vehicle status");
    } finally {
      setStatusChanging(false);
    }
  };

  const handleEditSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedVehicle) return;
    setSavingEdit(true);
    setError(null);
    try {
      await api.updateVehicle(selectedVehicle.id, editForm, session.accessToken);
      setSuccess(`Vehicle ${selectedVehicle.registrationNumber} updated successfully.`);
      setEditOpen(false);
      await queryClient.invalidateQueries({ queryKey: ["vehicles"] });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update vehicle details");
    } finally {
      setSavingEdit(false);
    }
  };

  const filteredVehicles = vehicles.filter((v) => {
    const query = search.toLowerCase();
    const matchesSearch =
      v.registrationNumber.toLowerCase().includes(query) ||
      (v.vin && v.vin.toLowerCase().includes(query)) ||
      (v.make && v.make.toLowerCase().includes(query)) ||
      (v.model && v.model.toLowerCase().includes(query)) ||
      v.vehicleType.toLowerCase().includes(query);
    const matchesStatus = statusFilter === "ALL" || v.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <Box>
      {/* Header */}
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ sm: "center" }} spacing={2} mb={3}>
        <Box>
          <Stack direction="row" alignItems="center" spacing={1.5}>
            <LocalShippingRounded sx={{ fontSize: 28, color: "primary.main" }} />
            <Typography variant="h5" fontWeight={800}>
              Fleet & Vehicles
            </Typography>
          </Stack>
          <Typography variant="body2" color="text.secondary">
            Manage heavy transport assets, refrigeration telematics, thermal limits, and maintenance readiness.
          </Typography>
        </Box>

        <Stack direction="row" spacing={1.5}>
          <Button
            variant="outlined"
            startIcon={loading ? <CircularProgress size={16} /> : <RefreshRounded />}
            onClick={() => void queryClient.invalidateQueries({ queryKey: ["vehicles"] })}
            disabled={loading}
          >
            Refresh
          </Button>
          {isAdmin && (
            <Button
              component={Link}
              to="/admin/vehicles/new"
              variant="contained"
              startIcon={<AddRounded />}
              sx={{ fontWeight: 700 }}
            >
              Register Vehicle
            </Button>
          )}
        </Stack>
      </Stack>

      {(error || queryError) && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error ?? (queryError instanceof Error ? queryError.message : "Failed to load vehicles")}
        </Alert>
      )}

      {success && (
        <Alert severity="success" icon={<CheckCircleRounded />} sx={{ mb: 3 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {/* Filters */}
      <Card variant="outlined" sx={{ mb: 3, borderRadius: 3 }}>
        <CardContent sx={{ p: 2 }}>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems="center">
            <TextField
              size="small"
              placeholder="Search by registration, VIN, make, model…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              sx={{ flex: 1 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchRounded fontSize="small" sx={{ color: "text.secondary" }} />
                  </InputAdornment>
                )
              }}
            />
            <TextField
              select
              size="small"
              label="Operational Status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              sx={{ minWidth: 170 }}
            >
              <MenuItem value="ALL">All Statuses</MenuItem>
              <MenuItem value="AVAILABLE">AVAILABLE</MenuItem>
              <MenuItem value="ASSIGNED">ASSIGNED</MenuItem>
              <MenuItem value="IN_TRANSIT">IN_TRANSIT</MenuItem>
              <MenuItem value="MAINTENANCE">MAINTENANCE</MenuItem>
              <MenuItem value="OUT_OF_SERVICE">OUT_OF_SERVICE</MenuItem>
            </TextField>
          </Stack>
        </CardContent>
      </Card>

      {/* Vehicles Table */}
      <Card variant="outlined" sx={{ borderRadius: 3, overflow: "hidden" }}>
        <TableContainer>
          <Table>
            <TableHead sx={{ bgcolor: "action.hover" }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>Vehicle / Registration</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Make & Model</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Type & Capacity</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Refrigeration & Temp</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Assigned Driver</TableCell>
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
              ) : filteredVehicles.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <Typography variant="body2" color="text.secondary">
                      No vehicles found matching the filters.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                filteredVehicles.map((v) => (
                  <TableRow key={v.id} hover>
                    <TableCell>
                      <Typography variant="subtitle2" fontWeight={700}>
                        {v.registrationNumber}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
                        VIN: {v.vin ?? "—"}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight={600}>
                        {v.make ? `${v.make} ${v.model ?? ""}` : "—"}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {v.year ? `Year ${v.year}` : ""}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{v.vehicleType}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        Cap: {v.payloadCapacityKg.toLocaleString()} kg
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {v.refrigerationSupported ? (
                        <Stack direction="row" spacing={0.5} alignItems="center">
                          <Chip
                            icon={<AcUnitRounded sx={{ fontSize: "14px !important" }} />}
                            label="Reefer"
                            size="small"
                            color="info"
                            variant="outlined"
                            sx={{ fontSize: "0.72rem", fontWeight: 700 }}
                          />
                          <Typography variant="caption" color="text.secondary">
                            {v.cargoTemperatureMinCelsius ?? -25}°C to {v.cargoTemperatureMaxCelsius ?? 10}°C
                          </Typography>
                        </Stack>
                      ) : (
                        <Typography variant="caption" color="text.secondary">
                          Ambient Standard
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      {v.assignedDriverName ? (
                        <Chip
                          label={v.assignedDriverName}
                          size="small"
                          color="primary"
                          variant="outlined"
                          sx={{ fontWeight: 600 }}
                        />
                      ) : (
                        <Typography variant="caption" color="text.secondary">
                          Unassigned
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={v.status}
                        size="small"
                        color={
                          v.status === "AVAILABLE"
                            ? "success"
                            : v.status === "ASSIGNED"
                            ? "primary"
                            : v.status === "IN_TRANSIT"
                            ? "info"
                            : v.status === "MAINTENANCE"
                            ? "warning"
                            : "error"
                        }
                        sx={{ fontWeight: 700 }}
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                        <Tooltip title="Update Status / Maintenance">
                          <IconButton
                            size="small"
                            onClick={() => {
                              setStatusDialogVehicle(v);
                              setNewStatus(v.status);
                            }}
                          >
                            <BuildRounded fontSize="small" />
                          </IconButton>
                        </Tooltip>

                        {isAdmin && (
                          <Tooltip title="Edit Specifications">
                            <IconButton
                              size="small"
                              onClick={() => {
                                setSelectedVehicle(v);
                                setEditForm({
                                  registrationNumber: v.registrationNumber,
                                  vin: v.vin ?? "",
                                  make: v.make ?? "",
                                  model: v.model ?? "",
                                  year: v.year ?? 2025,
                                  vehicleType: v.vehicleType,
                                  heightMeters: v.heightMeters,
                                  widthMeters: v.widthMeters,
                                  grossWeightKg: v.grossWeightKg,
                                  payloadCapacityKg: v.payloadCapacityKg,
                                  maximumEngineTemperatureCelsius: v.maximumEngineTemperatureCelsius,
                                  refrigerationSupported: v.refrigerationSupported,
                                  cargoTemperatureMinCelsius: v.cargoTemperatureMinCelsius ?? -25.0,
                                  cargoTemperatureMaxCelsius: v.cargoTemperatureMaxCelsius ?? 10.0,
                                  deviceIdentifier: v.deviceIdentifier ?? ""
                                });
                                setEditOpen(true);
                              }}
                            >
                              <EditRounded fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      {/* Status Transition Dialog */}
      <Dialog open={Boolean(statusDialogVehicle)} onClose={() => setStatusDialogVehicle(null)} maxWidth="xs" fullWidth>
        <DialogTitle fontWeight={700}>Update Operational Status</DialogTitle>
        <DialogContent dividers>
          <Box pt={1}>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Update status for <strong>{statusDialogVehicle?.registrationNumber}</strong>:
            </Typography>
            <TextField
              select
              fullWidth
              label="Status"
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value)}
              disabled={statusChanging}
            >
              <MenuItem value="AVAILABLE">AVAILABLE — Ready for driver assignment</MenuItem>
              <MenuItem value="MAINTENANCE">MAINTENANCE — Workshop inspection required</MenuItem>
              <MenuItem value="OUT_OF_SERVICE">OUT_OF_SERVICE — Decommissioned or out of duty</MenuItem>
            </TextField>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setStatusDialogVehicle(null)} disabled={statusChanging}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={() => void handleStatusSubmit()}
            disabled={statusChanging}
            startIcon={statusChanging ? <CircularProgress size={16} color="inherit" /> : null}
          >
            {statusChanging ? "Updating…" : "Update Status"}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Vehicle Dialog */}
      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle fontWeight={700}>Edit Vehicle Specifications</DialogTitle>
        <DialogContent dividers>
          <Box component="form" id="edit-vehicle-form" onSubmit={handleEditSubmit} sx={{ pt: 1 }}>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Registration Number *"
                  value={editForm.registrationNumber}
                  onChange={(e) => setEditForm({ ...editForm, registrationNumber: e.target.value })}
                  required
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="VIN"
                  value={editForm.vin}
                  onChange={(e) => setEditForm({ ...editForm, vin: e.target.value })}
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 4 }}>
                <TextField
                  fullWidth
                  label="Make"
                  value={editForm.make}
                  onChange={(e) => setEditForm({ ...editForm, make: e.target.value })}
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 4 }}>
                <TextField
                  fullWidth
                  label="Model"
                  value={editForm.model}
                  onChange={(e) => setEditForm({ ...editForm, model: e.target.value })}
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 4 }}>
                <TextField
                  fullWidth
                  type="number"
                  label="Year"
                  value={editForm.year}
                  onChange={(e) => setEditForm({ ...editForm, year: parseInt(e.target.value) || 2025 })}
                  disabled={savingEdit}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  type="number"
                  label="Payload Capacity (kg) *"
                  value={editForm.payloadCapacityKg}
                  onChange={(e) => setEditForm({ ...editForm, payloadCapacityKg: parseFloat(e.target.value) || 0 })}
                  required
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  type="number"
                  label="Gross Weight (kg) *"
                  value={editForm.grossWeightKg}
                  onChange={(e) => setEditForm({ ...editForm, grossWeightKg: parseFloat(e.target.value) || 0 })}
                  required
                  disabled={savingEdit}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  type="number"
                  label="Height (Meters) *"
                  value={editForm.heightMeters}
                  onChange={(e) => setEditForm({ ...editForm, heightMeters: parseFloat(e.target.value) || 0 })}
                  required
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  type="number"
                  label="Width (Meters) *"
                  value={editForm.widthMeters}
                  onChange={(e) => setEditForm({ ...editForm, widthMeters: parseFloat(e.target.value) || 0 })}
                  required
                  disabled={savingEdit}
                />
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  type="number"
                  label="Max Engine Temp (°C) *"
                  value={editForm.maximumEngineTemperatureCelsius}
                  onChange={(e) => setEditForm({ ...editForm, maximumEngineTemperatureCelsius: parseFloat(e.target.value) || 105 })}
                  required
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Telematics Device ID"
                  value={editForm.deviceIdentifier}
                  onChange={(e) => setEditForm({ ...editForm, deviceIdentifier: e.target.value })}
                  disabled={savingEdit}
                />
              </Grid>

              <Grid size={{ xs: 12 }}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={editForm.refrigerationSupported}
                      onChange={(e) => setEditForm({ ...editForm, refrigerationSupported: e.target.checked })}
                    />
                  }
                  label="Refrigeration / Cold Chain Support (Reefer Unit)"
                />
              </Grid>

              {editForm.refrigerationSupported && (
                <>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      type="number"
                      label="Min Cargo Temp (°C)"
                      value={editForm.cargoTemperatureMinCelsius}
                      onChange={(e) => setEditForm({ ...editForm, cargoTemperatureMinCelsius: parseFloat(e.target.value) || -25 })}
                      disabled={savingEdit}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      type="number"
                      label="Max Cargo Temp (°C)"
                      value={editForm.cargoTemperatureMaxCelsius}
                      onChange={(e) => setEditForm({ ...editForm, cargoTemperatureMaxCelsius: parseFloat(e.target.value) || 10 })}
                      disabled={savingEdit}
                    />
                  </Grid>
                </>
              )}
            </Grid>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setEditOpen(false)} disabled={savingEdit}>
            Cancel
          </Button>
          <Button
            type="submit"
            form="edit-vehicle-form"
            variant="contained"
            disabled={savingEdit}
            startIcon={savingEdit ? <CircularProgress size={16} color="inherit" /> : null}
          >
            {savingEdit ? "Saving…" : "Save Changes"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
