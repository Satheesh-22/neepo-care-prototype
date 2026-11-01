import AddRounded from "@mui/icons-material/AddRounded";
import BusinessRounded from "@mui/icons-material/BusinessRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import DeleteRounded from "@mui/icons-material/DeleteRounded";
import DirectionsCarRounded from "@mui/icons-material/DirectionsCarRounded";
import EditRounded from "@mui/icons-material/EditRounded";
import HubRounded from "@mui/icons-material/HubRounded";
import LocationOnRounded from "@mui/icons-material/LocationOnRounded";
import PeopleRounded from "@mui/icons-material/PeopleRounded";
import SearchRounded from "@mui/icons-material/SearchRounded";
import WarehouseRounded from "@mui/icons-material/WarehouseRounded";
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
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import InputLabel from "@mui/material/InputLabel";
import LinearProgress from "@mui/material/LinearProgress";
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
import type { CreateDepotRequest, DepotResponse, DepotStatus, UpdateDepotRequest } from "../api/types";
import { useAuthStore } from "../auth/authStore";
import { useTranslation } from "../i18n";
import { useAnnounce } from "../accessibility";

export const DepotsPage = () => {
  const session = useAuthStore((state) => state.session);
  const token = session?.accessToken ?? "";
  const isAdmin = session?.user.role === "ADMIN";
  const queryClient = useQueryClient();
  const { t, locale } = useTranslation();
  const { announce } = useAnnounce();

  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingDepot, setEditingDepot] = useState<DepotResponse | null>(null);
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; message: string } | null>(null);

  // Form State
  const [formData, setFormData] = useState<CreateDepotRequest>({
    code: "",
    name: "",
    address: "",
    city: "",
    state: "",
    postalCode: "",
    latitude: 47.6062,
    longitude: -122.3321,
    capacity: 50,
    status: "ACTIVE",
    contactPhone: "",
    contactEmail: ""
  });

  const { data: depots = [], isLoading, error } = useQuery({
    queryKey: ["depots", token],
    queryFn: () => neepoApi.listDepots(token),
    enabled: Boolean(token)
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateDepotRequest) => neepoApi.createDepot(req, token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["depots"] });
      setIsDialogOpen(false);
      setFeedback({ type: "success", message: "Depot created successfully." });
      announce(locale === "es" ? "Instalación creada con éxito" : "Facility created successfully", "polite");
    },
    onError: (err: Error) => {
      setFeedback({ type: "error", message: err.message || "Failed to create depot." });
      announce(locale === "es" ? "Error al crear instalación" : "Failed to create facility", "assertive");
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, req }: { id: string; req: UpdateDepotRequest }) => neepoApi.updateDepot(id, req, token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["depots"] });
      setIsDialogOpen(false);
      setEditingDepot(null);
      setFeedback({ type: "success", message: "Depot updated successfully." });
      announce(locale === "es" ? "Instalación actualizada con éxito" : "Facility updated successfully", "polite");
    },
    onError: (err: Error) => {
      setFeedback({ type: "error", message: err.message || "Failed to update depot." });
      announce(locale === "es" ? "Error al actualizar instalación" : "Failed to update facility", "assertive");
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => neepoApi.deleteDepot(id, token),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["depots"] });
      setFeedback({ type: "success", message: "Depot deleted successfully." });
      announce(locale === "es" ? "Instalación eliminada con éxito" : "Facility deleted successfully", "polite");
    },
    onError: (err: Error) => {
      setFeedback({ type: "error", message: err.message || "Failed to delete depot." });
      announce(locale === "es" ? "Error al eliminar instalación" : "Failed to delete facility", "assertive");
    }
  });

  const filteredDepots = useMemo(() => {
    return depots.filter((depot) => {
      const matchesSearch =
        depot.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        depot.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
        depot.city.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesStatus = statusFilter === "ALL" || depot.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [depots, searchTerm, statusFilter]);

  // Aggregate Metrics
  const totalDepots = depots.length;
  const activeFacilities = depots.filter((d) => d.status === "ACTIVE").length;
  const totalCapacity = depots.reduce((sum, d) => sum + d.capacity, 0);
  const totalStationedVehicles = depots.reduce((sum, d) => sum + d.assignedVehiclesCount, 0);

  const handleOpenCreate = () => {
    setEditingDepot(null);
    setFormData({
      code: "",
      name: "",
      address: "",
      city: "",
      state: "",
      postalCode: "",
      latitude: 47.6062,
      longitude: -122.3321,
      capacity: 50,
      status: "ACTIVE",
      contactPhone: "",
      contactEmail: ""
    });
    setIsDialogOpen(true);
  };

  const handleOpenEdit = (depot: DepotResponse) => {
    setEditingDepot(depot);
    setFormData({
      code: depot.code,
      name: depot.name,
      address: depot.address,
      city: depot.city,
      state: depot.state ?? "",
      postalCode: depot.postalCode ?? "",
      latitude: depot.latitude,
      longitude: depot.longitude,
      capacity: depot.capacity,
      status: depot.status,
      contactPhone: depot.contactPhone ?? "",
      contactEmail: depot.contactEmail ?? ""
    });
    setIsDialogOpen(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingDepot) {
      updateMutation.mutate({ id: editingDepot.id, req: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  return (
    <Box sx={{ p: { xs: 2, md: 4 }, maxWidth: 1400, mx: "auto" }}>
      {/* Header Banner */}
      <Box sx={{ mb: 4, display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: 2 }}>
        <Box>
          <Stack direction="row" alignItems="center" spacing={1.5} sx={{ mb: 0.5 }}>
            <WarehouseRounded sx={{ fontSize: 32, color: "primary.main" }} />
            <Typography variant="h4" fontWeight={700}>
              {t("depots.title")}
            </Typography>
          </Stack>
          <Typography variant="body1" color="text.secondary">
            {t("depots.subtitle")}
          </Typography>
        </Box>
        {isAdmin && (
          <Button
            variant="contained"
            startIcon={<AddRounded />}
            onClick={handleOpenCreate}
            sx={{ px: 3, py: 1.2, borderRadius: 2 }}
          >
            {t("depots.addDepot")}
          </Button>
        )}
      </Box>

      {/* Feedback Alert */}
      {feedback && (
        <Alert
          severity={feedback.type}
          onClose={() => setFeedback(null)}
          sx={{ mb: 3, borderRadius: 2 }}
        >
          {feedback.message}
        </Alert>
      )}

      {/* KPI Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card sx={{ p: 2.5, borderRadius: 3, boxShadow: 1 }}>
            <Stack direction="row" alignItems="center" spacing={2}>
              <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "primary.50", color: "primary.main" }}>
                <HubRounded sx={{ fontSize: 28 }} />
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" fontWeight={600}>
                  {t("depots.totalDepots").toUpperCase()}
                </Typography>
                <Typography variant="h4" fontWeight={700}>
                  {totalDepots}
                </Typography>
              </Box>
            </Stack>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card sx={{ p: 2.5, borderRadius: 3, boxShadow: 1 }}>
            <Stack direction="row" alignItems="center" spacing={2}>
              <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "success.50", color: "success.main" }}>
                <CheckCircleRounded sx={{ fontSize: 28 }} />
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" fontWeight={600}>
                  {t("depots.activeFacilities").toUpperCase()}
                </Typography>
                <Typography variant="h4" fontWeight={700} color="success.main">
                  {activeFacilities}
                </Typography>
              </Box>
            </Stack>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card sx={{ p: 2.5, borderRadius: 3, boxShadow: 1 }}>
            <Stack direction="row" alignItems="center" spacing={2}>
              <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "info.50", color: "info.main" }}>
                <BusinessRounded sx={{ fontSize: 28 }} />
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" fontWeight={600}>
                  {t("depots.totalCapacity").toUpperCase()}
                </Typography>
                <Typography variant="h4" fontWeight={700}>
                  {totalCapacity} <Typography component="span" variant="body2" color="text.secondary">{t("depots.vehicleUnit")}</Typography>
                </Typography>
              </Box>
            </Stack>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card sx={{ p: 2.5, borderRadius: 3, boxShadow: 1 }}>
            <Stack direction="row" alignItems="center" spacing={2}>
              <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "warning.50", color: "warning.main" }}>
                <DirectionsCarRounded sx={{ fontSize: 28 }} />
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" fontWeight={600}>
                  {t("depots.stationedFleet").toUpperCase()}
                </Typography>
                <Typography variant="h4" fontWeight={700}>
                  {totalStationedVehicles} <Typography component="span" variant="body2" color="text.secondary">{t("depots.vehicleUnit")}</Typography>
                </Typography>
              </Box>
            </Stack>
          </Card>
        </Grid>
      </Grid>

      {/* Filter and Search Bar */}
      <Card sx={{ p: 2.5, mb: 3, borderRadius: 3, boxShadow: 1 }}>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems="center" justifyContent="space-between">
          <TextField
            size="small"
            placeholder={t("depots.searchPlaceholder")}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchRounded sx={{ color: "text.secondary" }} />
                </InputAdornment>
              )
            }}
            sx={{ width: { xs: "100%", sm: 380 } }}
          />

          <Stack direction="row" spacing={1} sx={{ width: { xs: "100%", sm: "auto" } }}>
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel id="status-filter-label">Facility Status</InputLabel>
              <Select
                labelId="status-filter-label"
                value={statusFilter}
                label="Facility Status"
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <MenuItem value="ALL">All Facilities</MenuItem>
                <MenuItem value="ACTIVE">Active Only</MenuItem>
                <MenuItem value="MAINTENANCE">Maintenance</MenuItem>
                <MenuItem value="INACTIVE">Inactive</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </Stack>
      </Card>

      {/* Depots Ledger Table */}
      <Card sx={{ borderRadius: 3, boxShadow: 1, overflow: "hidden" }}>
        {isLoading ? (
          <Box sx={{ p: 6, display: "flex", justifyContent: "center" }}>
            <CircularProgress />
          </Box>
        ) : error ? (
          <Box sx={{ p: 4 }}>
            <Alert severity="error">Failed to load facility depots.</Alert>
          </Box>
        ) : filteredDepots.length === 0 ? (
          <Box sx={{ p: 6, textAlign: "center" }}>
            <WarehouseRounded sx={{ fontSize: 48, color: "text.disabled", mb: 1.5 }} />
            <Typography variant="h6" color="text.secondary">
              No facility depots match your criteria
            </Typography>
            <Typography variant="body2" color="text.disabled">
              Try adjusting your search terms or create a new depot facility.
            </Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table sx={{ minWidth: 800 }}>
              <TableHead sx={{ bgcolor: "grey.50" }}>
                <TableRow>
                  <TableCell sx={{ fontWeight: 700 }}>Depot Code</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Facility Name & Address</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Location (Coordinates)</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Capacity Utilization</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Stationed Resources</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                  {isAdmin && <TableCell align="right" sx={{ fontWeight: 700 }}>Actions</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredDepots.map((depot) => {
                  const utilizationPercent = Math.min(
                    100,
                    Math.round((depot.assignedVehiclesCount / Math.max(1, depot.capacity)) * 100)
                  );
                  return (
                    <TableRow key={depot.id} hover>
                      <TableCell>
                        <Chip
                          label={depot.code}
                          size="small"
                          sx={{ fontWeight: 700, fontFamily: "monospace", bgcolor: "primary.50", color: "primary.main" }}
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="subtitle2" fontWeight={600}>
                          {depot.name}
                        </Typography>
                        <Typography variant="caption" color="text.secondary" display="block">
                          {depot.address}, {depot.city}, {depot.state} {depot.postalCode}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Stack direction="row" alignItems="center" spacing={0.5}>
                          <LocationOnRounded sx={{ fontSize: 16, color: "text.secondary" }} />
                          <Typography variant="body2" color="text.secondary">
                            {depot.latitude.toFixed(4)}, {depot.longitude.toFixed(4)}
                          </Typography>
                        </Stack>
                      </TableCell>
                      <TableCell sx={{ minWidth: 160 }}>
                        <Stack spacing={0.5}>
                          <Stack direction="row" justifyContent="space-between">
                            <Typography variant="caption" fontWeight={600}>
                              {depot.assignedVehiclesCount} / {depot.capacity}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {utilizationPercent}%
                            </Typography>
                          </Stack>
                          <LinearProgress
                            variant="determinate"
                            value={utilizationPercent}
                            color={utilizationPercent > 85 ? "warning" : "primary"}
                            sx={{ height: 6, borderRadius: 1 }}
                          />
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={1.5} alignItems="center">
                          <Stack direction="row" spacing={0.5} alignItems="center" title="Assigned Vehicles">
                            <DirectionsCarRounded sx={{ fontSize: 16, color: "text.secondary" }} />
                            <Typography variant="body2">{depot.assignedVehiclesCount}</Typography>
                          </Stack>
                          <Stack direction="row" spacing={0.5} alignItems="center" title="Assigned Drivers">
                            <PeopleRounded sx={{ fontSize: 16, color: "text.secondary" }} />
                            <Typography variant="body2">{depot.assignedDriversCount}</Typography>
                          </Stack>
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={depot.status}
                          size="small"
                          color={
                            depot.status === "ACTIVE"
                              ? "success"
                              : depot.status === "MAINTENANCE"
                              ? "warning"
                              : "default"
                          }
                          sx={{ fontWeight: 600 }}
                        />
                      </TableCell>
                      {isAdmin && (
                        <TableCell align="right">
                          <IconButton
                            size="small"
                            onClick={() => handleOpenEdit(depot)}
                            title="Edit facility settings"
                          >
                            <EditRounded fontSize="small" />
                          </IconButton>
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => {
                              if (window.confirm(`Delete facility ${depot.code}?`)) {
                                deleteMutation.mutate(depot.id);
                              }
                            }}
                            title="Delete depot"
                          >
                            <DeleteRounded fontSize="small" />
                          </IconButton>
                        </TableCell>
                      )}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      {/* Create / Edit Depot Modal */}
      <Dialog
        open={isDialogOpen}
        onClose={() => setIsDialogOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: 3 } }}
      >
        <form onSubmit={handleSubmit}>
          <DialogTitle sx={{ fontWeight: 700 }}>
            {editingDepot ? t("depots.editTitle") : t("depots.registerTitle")}
          </DialogTitle>
          <DialogContent dividers>
            <Stack spacing={2.5} sx={{ mt: 1 }}>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    label="Depot Code"
                    fullWidth
                    required
                    placeholder="e.g. DEPOT-SEA-01"
                    value={formData.code}
                    onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <FormControl fullWidth required>
                    <InputLabel id="depot-status-select-label">Status</InputLabel>
                    <Select
                      labelId="depot-status-select-label"
                      value={formData.status}
                      label="Status"
                      onChange={(e) => setFormData({ ...formData, status: e.target.value as DepotStatus })}
                    >
                      <MenuItem value="ACTIVE">ACTIVE</MenuItem>
                      <MenuItem value="MAINTENANCE">MAINTENANCE</MenuItem>
                      <MenuItem value="INACTIVE">INACTIVE</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
              </Grid>

              <TextField
                label="Facility Name"
                fullWidth
                required
                placeholder="e.g. Seattle North Cold Chain Hub"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />

              <TextField
                label="Street Address"
                fullWidth
                required
                placeholder="e.g. 1000 Logistics Way"
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              />

              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 5 }}>
                  <TextField
                    label="City"
                    fullWidth
                    required
                    value={formData.city}
                    onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <TextField
                    label="State"
                    fullWidth
                    value={formData.state ?? ""}
                    onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 6, sm: 4 }}>
                  <TextField
                    label="Postal Code"
                    fullWidth
                    value={formData.postalCode ?? ""}
                    onChange={(e) => setFormData({ ...formData, postalCode: e.target.value })}
                  />
                </Grid>
              </Grid>

              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    label="Capacity (Vehicles)"
                    type="number"
                    fullWidth
                    required
                    value={formData.capacity}
                    onChange={(e) => setFormData({ ...formData, capacity: parseInt(e.target.value, 10) || 1 })}
                  />
                </Grid>
                <Grid size={{ xs: 6, sm: 4 }}>
                  <TextField
                    label="Latitude"
                    type="number"
                    fullWidth
                    required
                    value={formData.latitude}
                    onChange={(e) => setFormData({ ...formData, latitude: parseFloat(e.target.value) || 0 })}
                  />
                </Grid>
                <Grid size={{ xs: 6, sm: 4 }}>
                  <TextField
                    label="Longitude"
                    type="number"
                    fullWidth
                    required
                    value={formData.longitude}
                    onChange={(e) => setFormData({ ...formData, longitude: parseFloat(e.target.value) || 0 })}
                  />
                </Grid>
              </Grid>

              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    label="Contact Phone"
                    fullWidth
                    value={formData.contactPhone ?? ""}
                    onChange={(e) => setFormData({ ...formData, contactPhone: e.target.value })}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    label="Contact Email"
                    fullWidth
                    type="email"
                    value={formData.contactEmail ?? ""}
                    onChange={(e) => setFormData({ ...formData, contactEmail: e.target.value })}
                  />
                </Grid>
              </Grid>
            </Stack>
          </DialogContent>
          <DialogActions sx={{ p: 2.5 }}>
            <Button onClick={() => setIsDialogOpen(false)} color="inherit">
              {t("common.cancel")}
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={createMutation.isPending || updateMutation.isPending}
            >
              {createMutation.isPending || updateMutation.isPending ? "Saving..." : editingDepot ? "Update Facility" : "Register Facility"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};
