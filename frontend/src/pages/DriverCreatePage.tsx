import ArrowBackRounded from "@mui/icons-material/ArrowBackRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import ContentCopyRounded from "@mui/icons-material/ContentCopyRounded";
import PersonAddRounded from "@mui/icons-material/PersonAddRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api";
import type { CreateDriverDetailedInput, DriverDetailed } from "../api/types";
import { useAuthStore } from "../auth/authStore";

export function DriverCreatePage() {
  const session = useAuthStore((state) => state.session)!;
  const navigate = useNavigate();

  const [form, setForm] = useState<CreateDriverDetailedInput>({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    employeeId: "",
    licenseNumber: "",
    licenseCategory: "HEAVY_RIGID",
    licenseExpiryDate: "",
    emergencyContactName: "",
    emergencyContactPhone: "",
    baseLocation: "San Francisco Terminal",
    employmentStatus: "FULL_TIME",
    profilePhotoUrl: "",
    maximumDailyDrivingMinutes: 600
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdDriver, setCreatedDriver] = useState<DriverDetailed | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    // Client-side validations
    const fullName = `${form.firstName.trim()} ${form.lastName.trim()}`.trim();
    if (form.emergencyContactName && form.emergencyContactName.trim().toLowerCase() === fullName.toLowerCase()) {
      setError("Emergency contact person cannot be the driver.");
      setSaving(false);
      return;
    }

    if (form.licenseExpiryDate) {
      const expiry = new Date(form.licenseExpiryDate);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (expiry < today) {
        setError("Driver license expiry date cannot be in the past.");
        setSaving(false);
        return;
      }
    }

    try {
      const res = await api.createDriver(form, session.accessToken);
      setCreatedDriver(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to register driver");
    } finally {
      setSaving(false);
    }
  };

  const inviteUrl = createdDriver?.invitationToken
    ? `${window.location.origin}/invite/accept?token=${createdDriver.invitationToken}`
    : "";

  return (
    <Box maxWidth="lg" mx="auto">
      {/* Header */}
      <Stack direction="row" alignItems="center" spacing={2} mb={3}>
        <Button
          component={Link}
          to="/admin/drivers"
          startIcon={<ArrowBackRounded />}
          variant="text"
          sx={{ color: "text.secondary" }}
        >
          Back to Directory
        </Button>
      </Stack>

      <Stack direction="row" alignItems="center" spacing={1.5} mb={3}>
        <PersonAddRounded sx={{ fontSize: 28, color: "primary.main" }} />
        <Typography variant="h5" fontWeight={800}>
          Onboard Commercial Driver
        </Typography>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {createdDriver ? (
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
              Driver Successfully Registered!
            </Typography>
            <Typography variant="body2" color="text.secondary" maxWidth="sm" mx="auto" mb={3}>
              Driver profile for <strong>{createdDriver.name}</strong> (Employee ID: {createdDriver.employeeId}) has been created in the organization directory.
            </Typography>

            {createdDriver.invitationToken && (
              <Box maxWidth="sm" mx="auto" p={3} sx={{ bgcolor: "action.hover", borderRadius: 2, textAlign: "left", mb: 4 }}>
                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  Driver Onboarding Link
                </Typography>
                <Typography variant="caption" color="text.secondary" display="block" mb={1.5}>
                  Send this one-time link to the driver so they can set their password and log in to the mobile companion:
                </Typography>
                <TextField
                  fullWidth
                  size="small"
                  value={inviteUrl}
                  InputProps={{
                    readOnly: true,
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => {
                            void navigator.clipboard.writeText(inviteUrl);
                          }}
                        >
                          <ContentCopyRounded fontSize="small" />
                        </IconButton>
                      </InputAdornment>
                    )
                  }}
                />
              </Box>
            )}

            <Stack direction="row" spacing={2} justifyContent="center">
              <Button
                variant="outlined"
                onClick={() => {
                  setCreatedDriver(null);
                  setForm({
                    firstName: "",
                    lastName: "",
                    email: "",
                    phone: "",
                    employeeId: "",
                    licenseNumber: "",
                    licenseCategory: "HEAVY_RIGID",
                    licenseExpiryDate: "",
                    emergencyContactName: "",
                    emergencyContactPhone: "",
                    baseLocation: "San Francisco Terminal",
                    employmentStatus: "FULL_TIME",
                    profilePhotoUrl: "",
                    maximumDailyDrivingMinutes: 600
                  });
                }}
              >
                Register Another Driver
              </Button>
              <Button variant="contained" component={Link} to="/admin/drivers" sx={{ fontWeight: 700 }}>
                View All Drivers
              </Button>
            </Stack>
          </CardContent>
        </Card>
      ) : (
        <Card variant="outlined" sx={{ borderRadius: 3 }}>
          <CardContent sx={{ p: { xs: 2.5, md: 3.5 } }}>
            <Box component="form" onSubmit={handleSubmit}>
              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Driver Identity & Contact
              </Typography>
              <Grid container spacing={2} mb={3}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="First Name *"
                    value={form.firstName}
                    onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                    required
                    disabled={saving}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Last Name *"
                    value={form.lastName}
                    onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                    required
                    disabled={saving}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="email"
                    label="Work Email *"
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    required
                    disabled={saving}
                    helperText="Used for authentication invitation"
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Mobile Phone *"
                    value={form.phone}
                    onChange={(e) => setForm({ ...form, phone: e.target.value })}
                    required
                    disabled={saving}
                    helperText="E.164 format (+15550100000)"
                  />
                </Grid>
              </Grid>

              <Divider sx={{ my: 3 }} />

              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Commercial License & Compliance
              </Typography>
              <Grid container spacing={2} mb={3}>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    label="Employee ID *"
                    value={form.employeeId}
                    onChange={(e) => setForm({ ...form, employeeId: e.target.value })}
                    required
                    disabled={saving}
                    helperText="Unique staff code e.g. EMP-DRV-102"
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    label="Commercial License Number *"
                    value={form.licenseNumber}
                    onChange={(e) => setForm({ ...form, licenseNumber: e.target.value })}
                    required
                    disabled={saving}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    select
                    fullWidth
                    label="License Category *"
                    value={form.licenseCategory}
                    onChange={(e) => setForm({ ...form, licenseCategory: e.target.value })}
                    disabled={saving}
                  >
                    <MenuItem value="HEAVY_RIGID">Heavy Rigid (HR)</MenuItem>
                    <MenuItem value="HEAVY_COMBINATION">Heavy Combination (HC)</MenuItem>
                    <MenuItem value="MULTI_COMBINATION">Multi Combination (MC)</MenuItem>
                    <MenuItem value="MEDIUM_RIGID">Medium Rigid (MR)</MenuItem>
                  </TextField>
                </Grid>

                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    type="date"
                    label="License Expiry Date *"
                    value={form.licenseExpiryDate}
                    onChange={(e) => setForm({ ...form, licenseExpiryDate: e.target.value })}
                    InputLabelProps={{ shrink: true }}
                    required
                    disabled={saving}
                    helperText="Must be valid and unexpired"
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    select
                    fullWidth
                    label="Employment Status"
                    value={form.employmentStatus}
                    onChange={(e) => setForm({ ...form, employmentStatus: e.target.value })}
                    disabled={saving}
                  >
                    <MenuItem value="FULL_TIME">Full-time Employee</MenuItem>
                    <MenuItem value="CONTRACTOR">Contractor / Agency</MenuItem>
                    <MenuItem value="PART_TIME">Part-time</MenuItem>
                  </TextField>
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Max Driving Limit (Minutes/Day)"
                    value={form.maximumDailyDrivingMinutes}
                    onChange={(e) => setForm({ ...form, maximumDailyDrivingMinutes: parseInt(e.target.value) || 600 })}
                    disabled={saving}
                    inputProps={{ min: 60, max: 840 }}
                    helperText="Regulatory fatigue limit (e.g. 600m)"
                  />
                </Grid>
              </Grid>

              <Divider sx={{ my: 3 }} />

              <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                Duty Station & Safety Contacts
              </Typography>
              <Grid container spacing={2} mb={4}>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    label="Base Terminal Location"
                    value={form.baseLocation}
                    onChange={(e) => setForm({ ...form, baseLocation: e.target.value })}
                    disabled={saving}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    label="Emergency Contact Name"
                    value={form.emergencyContactName}
                    onChange={(e) => setForm({ ...form, emergencyContactName: e.target.value })}
                    disabled={saving}
                    helperText="Must be different from driver"
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 4 }}>
                  <TextField
                    fullWidth
                    label="Emergency Contact Phone"
                    value={form.emergencyContactPhone}
                    onChange={(e) => setForm({ ...form, emergencyContactPhone: e.target.value })}
                    disabled={saving}
                  />
                </Grid>
              </Grid>

              <Stack direction="row" spacing={2} justifyContent="flex-end">
                <Button variant="outlined" onClick={() => navigate("/admin/drivers")} disabled={saving}>
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={saving}
                  startIcon={saving ? <CircularProgress size={18} color="inherit" /> : <PersonAddRounded />}
                  sx={{ px: 4, fontWeight: 700 }}
                >
                  {saving ? "Registering Driver…" : "Register Driver"}
                </Button>
              </Stack>
            </Box>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
