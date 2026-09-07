import AddRounded from "@mui/icons-material/AddRounded";
import BadgeRounded from "@mui/icons-material/BadgeRounded";
import BlockRounded from "@mui/icons-material/BlockRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import EditRounded from "@mui/icons-material/EditRounded";
import MailRounded from "@mui/icons-material/MailRounded";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import SearchRounded from "@mui/icons-material/SearchRounded";
import WarningRounded from "@mui/icons-material/WarningRounded";
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
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
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
import type { DriverDetailed, UpdateDriverInput } from "../api/types";
import { useAuthStore } from "../auth/authStore";

export function DriverListPage() {
  const session = useAuthStore((state) => state.session)!;
  const isAdmin = session.user.role === "ADMIN";
  const queryClient = useQueryClient();

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: drivers = [], isPending: loading, error: queryError } = useQuery({
    queryKey: ["drivers"],
    queryFn: () => api.listDrivers(session.accessToken)
  });

  // Edit State
  const [editOpen, setEditOpen] = useState(false);
  const [selectedDriver, setSelectedDriver] = useState<DriverDetailed | null>(null);
  const [editForm, setEditForm] = useState<UpdateDriverInput>({
    firstName: "",
    lastName: "",
    phone: "",
    employeeId: "",
    licenseNumber: "",
    licenseCategory: "HEAVY_RIGID",
    licenseExpiryDate: "",
    emergencyContactName: "",
    emergencyContactPhone: "",
    baseLocation: "",
    employmentStatus: "FULL_TIME",
    maximumDailyDrivingMinutes: 600
  });
  const [savingEdit, setSavingEdit] = useState(false);

  // Invite Token Dialog
  const [invitedToken, setInvitedToken] = useState<string | null>(null);

  const handleEditSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedDriver) return;
    setSavingEdit(true);
    setError(null);
    try {
      await api.updateDriver(selectedDriver.id, editForm, session.accessToken);
      setSuccess(`Driver ${selectedDriver.name} updated successfully.`);
      setEditOpen(false);
      await queryClient.invalidateQueries({ queryKey: ["drivers"] });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update driver");
    } finally {
      setSavingEdit(false);
    }
  };

  const handleSuspendToggle = async (driver: DriverDetailed) => {
    setError(null);
    try {
      if (driver.verificationState === "SUSPENDED") {
        await api.reactivateDriver(driver.id, session.accessToken);
        setSuccess(`Driver ${driver.name} reactivated.`);
      } else {
        await api.suspendDriver(driver.id, session.accessToken);
        setSuccess(`Driver ${driver.name} suspended.`);
      }
      await queryClient.invalidateQueries({ queryKey: ["drivers"] });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to toggle driver status");
    }
  };

  const handleReinvite = async (driver: DriverDetailed) => {
    setError(null);
    try {
      const res = await api.inviteDriver(driver.id, session.accessToken);
      setInvitedToken(res.token);
      setSuccess(`New invitation token generated for ${driver.email}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to generate driver invitation");
    }
  };

  const filteredDrivers = drivers.filter((d) => {
    const query = search.toLowerCase();
    const matchesSearch =
      d.name.toLowerCase().includes(query) ||
      d.email.toLowerCase().includes(query) ||
      (d.employeeId && d.employeeId.toLowerCase().includes(query)) ||
      d.licenseNumber.toLowerCase().includes(query) ||
      d.phone.includes(query);
    const matchesStatus = statusFilter === "ALL" || d.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <Box>
      {/* Header */}
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ sm: "center" }} spacing={2} mb={3}>
        <Box>
          <Stack direction="row" alignItems="center" spacing={1.5}>
            <BadgeRounded sx={{ fontSize: 28, color: "primary.main" }} />
            <Typography variant="h5" fontWeight={800}>
              Driver Directory
            </Typography>
          </Stack>
          <Typography variant="body2" color="text.secondary">
            Manage certified commercial drivers, license credentials, active assignments, and shift availability.
          </Typography>
        </Box>

        <Stack direction="row" spacing={1.5}>
          <Button
            variant="outlined"
            startIcon={loading ? <CircularProgress size={16} /> : <RefreshRounded />}
            onClick={() => void queryClient.invalidateQueries({ queryKey: ["drivers"] })}
            disabled={loading}
          >
            Refresh
          </Button>
          {isAdmin && (
            <Button
              component={Link}
              to="/admin/drivers/new"
              variant="contained"
              startIcon={<AddRounded />}
              sx={{ fontWeight: 700 }}
            >
              Onboard Driver
            </Button>
          )}
        </Stack>
      </Stack>

      {(error || queryError) && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error ?? (queryError instanceof Error ? queryError.message : "Failed to load drivers directory")}
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
              placeholder="Search by driver name, employee ID, license or email…"
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
              label="Driver Status"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              sx={{ minWidth: 160 }}
            >
              <MenuItem value="ALL">All Statuses</MenuItem>
              <MenuItem value="AVAILABLE">AVAILABLE</MenuItem>
              <MenuItem value="ASSIGNED">ASSIGNED</MenuItem>
              <MenuItem value="DRIVING">DRIVING</MenuItem>
              <MenuItem value="OFF_DUTY">OFF_DUTY</MenuItem>
              <MenuItem value="REST_REQUIRED">REST_REQUIRED</MenuItem>
            </TextField>
          </Stack>
        </CardContent>
      </Card>

      {/* Drivers Table */}
      <Card variant="outlined" sx={{ borderRadius: 3, overflow: "hidden" }}>
        <TableContainer>
          <Table>
            <TableHead sx={{ bgcolor: "action.hover" }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>Driver Details</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Employee ID</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>License & Expiry</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Assigned Vehicle</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                <TableCell sx={{ fontWeight: 700 }}>Verification</TableCell>
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
              ) : filteredDrivers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 6 }}>
                    <Typography variant="body2" color="text.secondary">
                      No drivers match your criteria.
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : (
                filteredDrivers.map((d) => {
                  const isSuspended = d.verificationState === "SUSPENDED";
                  return (
                    <TableRow key={d.id} hover>
                      <TableCell>
                        <Typography variant="subtitle2" fontWeight={700}>
                          {d.name}
                        </Typography>
                        <Typography variant="caption" color="text.secondary" display="block">
                          {d.email} • {d.phone}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight={600}>
                          {d.employeeId ?? "—"}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {d.employmentStatus}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{d.licenseNumber}</Typography>
                        <Stack direction="row" spacing={0.5} alignItems="center" mt={0.5}>
                          <Chip label={d.licenseCategory} size="small" variant="outlined" sx={{ fontSize: "0.7rem" }} />
                          {d.licenseExpired ? (
                            <Chip
                              icon={<WarningRounded sx={{ fontSize: "14px !important" }} />}
                              label="Expired"
                              size="small"
                              color="error"
                              sx={{ fontWeight: 700, fontSize: "0.7rem" }}
                            />
                          ) : (
                            <Typography variant="caption" color="text.secondary">
                              Exp: {d.licenseExpiryDate ?? "—"}
                            </Typography>
                          )}
                        </Stack>
                      </TableCell>
                      <TableCell>
                        {d.assignedVehicleRegistration ? (
                          <Chip
                            label={d.assignedVehicleRegistration}
                            size="small"
                            color="info"
                            variant="outlined"
                            sx={{ fontWeight: 700 }}
                          />
                        ) : (
                          <Typography variant="caption" color="text.secondary">
                            None (Available)
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={d.status}
                          size="small"
                          color={
                            d.status === "AVAILABLE"
                              ? "success"
                              : d.status === "ASSIGNED"
                              ? "primary"
                              : d.status === "DRIVING"
                              ? "info"
                              : "default"
                          }
                          sx={{ fontWeight: 700 }}
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={d.verificationState}
                          size="small"
                          color={isSuspended ? "error" : "success"}
                          variant={isSuspended ? "filled" : "outlined"}
                          sx={{ fontWeight: 700 }}
                        />
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                          {isAdmin && (
                            <Tooltip title="Re-issue Invitation Token">
                              <IconButton size="small" onClick={() => void handleReinvite(d)}>
                                <MailRounded fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          {isAdmin && (
                            <Tooltip title="Edit Profile">
                              <IconButton
                                size="small"
                                onClick={() => {
                                  setSelectedDriver(d);
                                  setEditForm({
                                    firstName: d.firstName ?? "",
                                    lastName: d.lastName ?? "",
                                    phone: d.phone,
                                    employeeId: d.employeeId ?? "",
                                    licenseNumber: d.licenseNumber,
                                    licenseCategory: d.licenseCategory,
                                    licenseExpiryDate: d.licenseExpiryDate ?? "",
                                    emergencyContactName: d.emergencyContactName ?? "",
                                    emergencyContactPhone: d.emergencyContactPhone ?? "",
                                    baseLocation: d.baseLocation ?? "",
                                    employmentStatus: d.employmentStatus,
                                    maximumDailyDrivingMinutes: d.maximumDailyDrivingMinutes
                                  });
                                  setEditOpen(true);
                                }}
                              >
                                <EditRounded fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          {isAdmin && (
                            <Tooltip title={isSuspended ? "Reactivate Driver" : "Suspend Driver"}>
                              <IconButton
                                size="small"
                                color={isSuspended ? "success" : "error"}
                                onClick={() => void handleSuspendToggle(d)}
                              >
                                <BlockRounded fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                        </Stack>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Card>

      {/* Re-invite Dialog */}
      <Dialog open={Boolean(invitedToken)} onClose={() => setInvitedToken(null)} maxWidth="sm" fullWidth>
        <DialogTitle fontWeight={700}>Driver Invitation Token Generated</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={2} py={1}>
            <Alert severity="success" icon={<MailRounded />}>
              A fresh one-time invitation token has been created. Send this link to the driver to complete their onboarding:
            </Alert>
            <TextField
              fullWidth
              size="small"
              value={`${window.location.origin}/invite/accept?token=${invitedToken ?? ""}`}
              InputProps={{ readOnly: true }}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button
            variant="contained"
            onClick={() => {
              void navigator.clipboard.writeText(`${window.location.origin}/invite/accept?token=${invitedToken ?? ""}`);
              setInvitedToken(null);
              setSuccess("Invitation link copied to clipboard.");
            }}
          >
            Copy & Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Driver Dialog */}
      <Dialog open={editOpen} onClose={() => setEditOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle fontWeight={700}>Edit Driver Credentials</DialogTitle>
        <DialogContent dividers>
          <Box component="form" id="edit-driver-form" onSubmit={handleEditSubmit} sx={{ pt: 1 }}>
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="First Name *"
                  value={editForm.firstName}
                  onChange={(e) => setEditForm({ ...editForm, firstName: e.target.value })}
                  required
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Last Name *"
                  value={editForm.lastName}
                  onChange={(e) => setEditForm({ ...editForm, lastName: e.target.value })}
                  required
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Employee ID *"
                  value={editForm.employeeId}
                  onChange={(e) => setEditForm({ ...editForm, employeeId: e.target.value })}
                  required
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Phone *"
                  value={editForm.phone}
                  onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                  required
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="License Number *"
                  value={editForm.licenseNumber}
                  onChange={(e) => setEditForm({ ...editForm, licenseNumber: e.target.value })}
                  required
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  type="date"
                  label="License Expiry Date"
                  value={editForm.licenseExpiryDate}
                  onChange={(e) => setEditForm({ ...editForm, licenseExpiryDate: e.target.value })}
                  InputLabelProps={{ shrink: true }}
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Emergency Contact Name"
                  value={editForm.emergencyContactName}
                  onChange={(e) => setEditForm({ ...editForm, emergencyContactName: e.target.value })}
                  disabled={savingEdit}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <TextField
                  fullWidth
                  label="Emergency Contact Phone"
                  value={editForm.emergencyContactPhone}
                  onChange={(e) => setEditForm({ ...editForm, emergencyContactPhone: e.target.value })}
                  disabled={savingEdit}
                />
              </Grid>
            </Grid>
          </Box>
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setEditOpen(false)} disabled={savingEdit}>
            Cancel
          </Button>
          <Button
            type="submit"
            form="edit-driver-form"
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
