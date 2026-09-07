import AddCircleRounded from "@mui/icons-material/AddCircleRounded";
import ArrowBackRounded from "@mui/icons-material/ArrowBackRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import FormControlLabel from "@mui/material/FormControlLabel";
import Grid from "@mui/material/Grid";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api";
import type { CreateVehicleDetailedInput, VehicleDetailed } from "../api/types";
import { useAuthStore } from "../auth/authStore";

export function VehicleCreatePage() {
  const session = useAuthStore((state) => state.session)!;
  const navigate = useNavigate();

  const [form, setForm] = useState<CreateVehicleDetailedInput>({
    registrationNumber: "",
    vin: "",
    make: "Volvo",
    model: "FH Electric Reefer",
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
    deviceIdentifier: "DEV-OBD-002",
    status: "AVAILABLE"
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdVehicle, setCreatedVehicle] = useState<VehicleDetailed | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    if (form.payloadCapacityKg <= 0 || form.grossWeightKg <= 0) {
      setError("Payload capacity and gross weight must be positive numbers.");
      setSaving(false);
      return;
    }

    if (form.refrigerationSupported) {
      if (
        form.cargoTemperatureMinCelsius !== undefined &&
        form.cargoTemperatureMaxCelsius !== undefined &&
        form.cargoTemperatureMinCelsius > form.cargoTemperatureMaxCelsius
      ) {
        setError("Minimum cargo temperature cannot exceed maximum cargo temperature.");
        setSaving(false);
        return;
      }
    }

    try {
      const res = await api.createVehicle(form, session.accessToken);
      setCreatedVehicle(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to register vehicle");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box maxWidth="lg" mx="auto">
      {/* Header */}
      <Stack direction="row" alignItems="center" spacing={2} mb={3}>
        <Button
          component={Link}
          to="/admin/vehicles"
          startIcon={<ArrowBackRounded />}
          variant="text"
          sx={{ color: "text.secondary" }}
        >
          Back to Vehicles
        </Button>
      </Stack>

      <Stack direction="row" alignItems="center" spacing={1.5} mb={3}>
        <LocalShippingRounded sx={{ fontSize: 28, color: "primary.main" }} />
        <Typography variant="h5" fontWeight={800}>
          Register Fleet Vehicle
        </Typography>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {createdVehicle ? (
        <Card variant="outlined" sx={{ borderRadius: 3 }}>
          <CardContent sx={{ p: 4, textAlign: "center" }}>
            <Box
              sx={{
                width: 64,
                height: 64,
                borderRadius: "50%",
                bgcolor: "rgba(16, 185, 129, 0.1)",
                color: "success.main",
                display: "grid",
                placeItems: "center",
                mx: "auto",
                mb: 2
              }}
            >
              <CheckCircleRounded sx={{ fontSize: 36 }} />
            </Box>

            <Typography variant="h6" fontWeight={800} gutterBottom>
              Vehicle Successfully Registered!
            </Typography>
            <Typography variant="body2" color="text.secondary" maxWidth="sm" mx="auto" mb={3}>
              Vehicle <strong>{createdVehicle.registrationNumber}</strong> ({createdVehicle.make} {createdVehicle.model}) is now ready for operational driver assignment and trip dispatch.
            </Typography>

            <Stack direction="row" spacing={2} justifyContent="center">
              <Button
                variant="outlined"
                onClick={() => {
                  setCreatedVehicle(null);
                  setForm({
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
                    deviceIdentifier: "",
                    status: "AVAILABLE"
                  });
                }}
              >
                Register Another Vehicle
              </Button>
              <Button variant="contained" component={Link} to="/admin/vehicles" sx={{ fontWeight: 700 }}>
                View All Vehicles
              </Button>
            </Stack>
          </CardContent>
        </Card>
      ) : (
        <Card variant="outlined" sx={{ borderRadius: 3 }}>
          <CardContent sx={{ p: { xs: 2.5, md: 3.5 } }}>
            <Box component="form" onSubmit={handleSubmit}>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Vehicle Identity & Registry
              </Typography>
              <Grid container spacing={2} mb={3}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="License Registration Number *"
                    value={form.registrationNumber}
                    onChange={(e) => setForm({ ...form, registrationNumber: e.target.value })}
                    required
                    disabled={saving}
                    helperText="Official license plate e.g. CA-94105-REEFER"
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="VIN (Vehicle Identification Number)"
                    value={form.vin}
                    onChange={(e) => setForm({ ...form, vin: e.target.value })}
                    disabled={saving}
                    helperText="17-character chassis number"
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    label="Make"
                    value={form.make}
                    onChange={(e) => setForm({ ...form, make: e.target.value })}
                    disabled={saving}
                    helperText="e.g. Volvo, Scania, Peterbilt"
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    label="Model"
                    value={form.model}
                    onChange={(e) => setForm({ ...form, model: e.target.value })}
                    disabled={saving}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Manufacturing Year"
                    value={form.year}
                    onChange={(e) => setForm({ ...form, year: parseInt(e.target.value) || 2025 })}
                    disabled={saving}
                  />
                </Grid>
              </Grid>

              <Divider sx={{ my: 3 }} />

              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Physical Specifications & Cargo Capacity
              </Typography>
              <Grid container spacing={2} mb={3}>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    select
                    fullWidth
                    label="Vehicle Classification *"
                    value={form.vehicleType}
                    onChange={(e) => setForm({ ...form, vehicleType: e.target.value })}
                    disabled={saving}
                  >
                    <MenuItem value="REFRIGERATED_TRUCK">Refrigerated Truck (Cold Chain)</MenuItem>
                    <MenuItem value="HEAVY_RIGID_VAN">Heavy Rigid Van</MenuItem>
                    <MenuItem value="SEMI_TRAILER">Semi-Trailer / Prime Mover</MenuItem>
                    <MenuItem value="LIGHT_COMMERCIAL">Light Commercial Vehicle (LCV)</MenuItem>
                    <MenuItem value="FLATBED">Flatbed Cargo</MenuItem>
                  </TextField>
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Payload Capacity (kg) *"
                    value={form.payloadCapacityKg}
                    onChange={(e) => setForm({ ...form, payloadCapacityKg: parseFloat(e.target.value) || 0 })}
                    required
                    disabled={saving}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Gross Vehicle Weight (kg) *"
                    value={form.grossWeightKg}
                    onChange={(e) => setForm({ ...form, grossWeightKg: parseFloat(e.target.value) || 0 })}
                    required
                    disabled={saving}
                  />
                </Grid>

                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Height (Meters) *"
                    value={form.heightMeters}
                    onChange={(e) => setForm({ ...form, heightMeters: parseFloat(e.target.value) || 0 })}
                    required
                    disabled={saving}
                    helperText="For bridge clearance checks"
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Width (Meters) *"
                    value={form.widthMeters}
                    onChange={(e) => setForm({ ...form, widthMeters: parseFloat(e.target.value) || 0 })}
                    required
                    disabled={saving}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Engine Overheat Threshold (°C) *"
                    value={form.maximumEngineTemperatureCelsius}
                    onChange={(e) => setForm({ ...form, maximumEngineTemperatureCelsius: parseFloat(e.target.value) || 105 })}
                    required
                    disabled={saving}
                    helperText="Alerts trigger beyond this limit"
                  />
                </Grid>
              </Grid>

              <Divider sx={{ my: 3 }} />

              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Cold Chain & Telematics Configuration
              </Typography>
              <Grid container spacing={2} mb={4}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="OBD / Telematics Device ID"
                    value={form.deviceIdentifier}
                    onChange={(e) => setForm({ ...form, deviceIdentifier: e.target.value })}
                    disabled={saving}
                    helperText="Hardware MAC or Gateway Identifier"
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    select
                    fullWidth
                    label="Initial Status"
                    value={form.status}
                    onChange={(e) => setForm({ ...form, status: e.target.value as CreateVehicleDetailedInput["status"] })}
                    disabled={saving}
                  >
                    <MenuItem value="AVAILABLE">AVAILABLE — Ready for Assignment</MenuItem>
                    <MenuItem value="MAINTENANCE">MAINTENANCE — In Workshop Inspection</MenuItem>
                  </TextField>
                </Grid>

                <Grid size={{ xs: 12 }}>
                  <FormControlLabel
                    control={
                      <Switch
                        checked={form.refrigerationSupported}
                        onChange={(e) => setForm({ ...form, refrigerationSupported: e.target.checked })}
                      />
                    }
                    label="Refrigeration / Temperature Controlled Cargo (Reefer)"
                  />
                </Grid>

                {form.refrigerationSupported && (
                  <>
                    <Grid size={{ xs: 12, sm: 6 }}>
                      <TextField
                        fullWidth
                        type="number"
                        label="Min Permitted Cargo Temp (°C)"
                        value={form.cargoTemperatureMinCelsius}
                        onChange={(e) => setForm({ ...form, cargoTemperatureMinCelsius: parseFloat(e.target.value) || -25 })}
                        disabled={saving}
                        helperText="Deep freeze lower boundary"
                      />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                      <TextField
                        fullWidth
                        type="number"
                        label="Max Permitted Cargo Temp (°C)"
                        value={form.cargoTemperatureMaxCelsius}
                        onChange={(e) => setForm({ ...form, cargoTemperatureMaxCelsius: parseFloat(e.target.value) || 10 })}
                        disabled={saving}
                        helperText="Upper safe temperature threshold"
                      />
                    </Grid>
                  </>
                )}
              </Grid>

              <Stack direction="row" spacing={2} justifyContent="flex-end">
                <Button variant="outlined" onClick={() => navigate("/admin/vehicles")} disabled={saving}>
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={saving}
                  startIcon={saving ? <CircularProgress size={18} color="inherit" /> : <AddCircleRounded />}
                  sx={{ px: 4, fontWeight: 700 }}
                >
                  {saving ? "Registering Vehicle…" : "Register Vehicle"}
                </Button>
              </Stack>
            </Box>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
