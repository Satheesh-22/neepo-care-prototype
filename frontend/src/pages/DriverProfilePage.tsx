import AccessTimeRounded from "@mui/icons-material/AccessTimeRounded";
import BadgeRounded from "@mui/icons-material/BadgeRounded";
import ContactPhoneRounded from "@mui/icons-material/ContactPhoneRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import Grid from "@mui/material/Grid";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../api";
import { useAuthStore } from "../auth/authStore";

export function DriverProfilePage() {
  const session = useAuthStore((state) => state.session)!;
  const queryClient = useQueryClient();

  const { data: driver, isPending: loading, error: queryError } = useQuery({
    queryKey: ["driverProfile"],
    queryFn: () => api.getMyDriverProfile(session.accessToken)
  });

  if (loading) {
    return (
      <Box textAlign="center" py={10}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box maxWidth="lg" mx="auto">
      {/* Header */}
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Stack direction="row" alignItems="center" spacing={1.5}>
          <BadgeRounded sx={{ fontSize: 28, color: "primary.main" }} />
          <Typography variant="h5" fontWeight={800}>
            Driver Operational Profile
          </Typography>
        </Stack>
        <Button
          variant="outlined"
          startIcon={<RefreshRounded />}
          onClick={() => void queryClient.invalidateQueries({ queryKey: ["driverProfile"] })}
          size="small"
        >
          Refresh
        </Button>
      </Stack>

      {queryError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {queryError instanceof Error ? queryError.message : "Failed to load driver profile"}
        </Alert>
      )}

      {driver && (
        <Grid container spacing={3}>
          {/* Main Credentials */}
          <Grid size={{ xs: 12, md: 8 }}>
            <Card variant="outlined" sx={{ borderRadius: 3 }}>
              <CardContent sx={{ p: { xs: 2.5, md: 3.5 } }}>
                <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ sm: "center" }} mb={3}>
                  <Box>
                    <Typography variant="h6" fontWeight={800}>
                      {driver.name}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {driver.email} • {driver.phone}
                    </Typography>
                  </Box>
                  <Stack direction="row" spacing={1} mt={{ xs: 1, sm: 0 }}>
                    <Chip label={driver.verificationState} color="success" size="small" sx={{ fontWeight: 700 }} />
                    <Chip label={driver.status} color="primary" size="small" sx={{ fontWeight: 700 }} />
                  </Stack>
                </Stack>

                <Divider sx={{ my: 2.5 }} />

                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  Commercial License & Fleet Credentials
                </Typography>
                <Grid container spacing={2} mt={0.5}>
                  <Grid size={{ xs: 6, sm: 4 }}>
                    <Typography variant="caption" color="text.secondary">
                      Employee ID
                    </Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {driver.employeeId ?? "—"}
                    </Typography>
                  </Grid>
                  <Grid size={{ xs: 6, sm: 4 }}>
                    <Typography variant="caption" color="text.secondary">
                      License Number
                    </Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {driver.licenseNumber}
                    </Typography>
                  </Grid>
                  <Grid size={{ xs: 6, sm: 4 }}>
                    <Typography variant="caption" color="text.secondary">
                      Category
                    </Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {driver.licenseCategory}
                    </Typography>
                  </Grid>
                  <Grid size={{ xs: 6, sm: 4 }}>
                    <Typography variant="caption" color="text.secondary">
                      License Expiry
                    </Typography>
                    <Typography variant="body2" fontWeight={600} color={driver.licenseExpired ? "error.main" : "text.primary"}>
                      {driver.licenseExpiryDate ?? "—"}
                    </Typography>
                  </Grid>
                  <Grid size={{ xs: 6, sm: 4 }}>
                    <Typography variant="caption" color="text.secondary">
                      Employment
                    </Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {driver.employmentStatus}
                    </Typography>
                  </Grid>
                  <Grid size={{ xs: 6, sm: 4 }}>
                    <Typography variant="caption" color="text.secondary">
                      Base Location
                    </Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {driver.baseLocation ?? "—"}
                    </Typography>
                  </Grid>
                </Grid>

                <Divider sx={{ my: 2.5 }} />

                <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                  Emergency Contact
                </Typography>
                <Stack direction="row" spacing={1.5} alignItems="center" mt={1}>
                  <ContactPhoneRounded sx={{ color: "text.secondary" }} />
                  <Box>
                    <Typography variant="body2" fontWeight={600}>
                      {driver.emergencyContactName ?? "Not specified"}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {driver.emergencyContactPhone ?? "No contact number"}
                    </Typography>
                  </Box>
                </Stack>
              </CardContent>
            </Card>
          </Grid>

          {/* Assigned Vehicle & Hours */}
          <Grid size={{ xs: 12, md: 4 }}>
            <Stack spacing={3}>
              <Card variant="outlined" sx={{ borderRadius: 3 }}>
                <CardContent sx={{ p: 2.5 }}>
                  <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                    <LocalShippingRounded sx={{ color: "primary.main" }} />
                    <Typography variant="subtitle2" fontWeight={700}>
                      Assigned Vehicle
                    </Typography>
                  </Stack>

                  {driver.assignedVehicleRegistration ? (
                    <Box p={2} sx={{ bgcolor: "action.hover", borderRadius: 2 }}>
                      <Typography variant="h6" fontWeight={800} color="primary.main">
                        {driver.assignedVehicleRegistration}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Active Vehicle Allocation
                      </Typography>
                    </Box>
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      No vehicle currently allocated.
                    </Typography>
                  )}
                </CardContent>
              </Card>

              <Card variant="outlined" sx={{ borderRadius: 3 }}>
                <CardContent sx={{ p: 2.5 }}>
                  <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                    <AccessTimeRounded sx={{ color: "primary.main" }} />
                    <Typography variant="subtitle2" fontWeight={700}>
                      Driving Time Budget
                    </Typography>
                  </Stack>

                  <Typography variant="caption" color="text.secondary">
                    Remaining Driving Minutes Today
                  </Typography>
                  <Typography variant="h4" fontWeight={800} color="success.main" my={0.5}>
                    {driver.remainingDrivingMinutes} / {driver.maximumDailyDrivingMinutes} m
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Compliant with maximum daily driving limit.
                  </Typography>
                </CardContent>
              </Card>
            </Stack>
          </Grid>
        </Grid>
      )}
    </Box>
  );
}
