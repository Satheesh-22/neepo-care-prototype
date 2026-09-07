import BusinessRounded from "@mui/icons-material/BusinessRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import HistoryRounded from "@mui/icons-material/HistoryRounded";
import SaveRounded from "@mui/icons-material/SaveRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import Grid from "@mui/material/Grid";
import MenuItem from "@mui/material/MenuItem";
import Skeleton from "@mui/material/Skeleton";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { api } from "../api";
import type { UpdateCompanyInput } from "../api/types";
import { useAuthStore } from "../auth/authStore";

export function CompanyProfilePage() {
  const session = useAuthStore((state) => state.session)!;
  const isAdmin = session.user.role === "ADMIN";
  const queryClient = useQueryClient();

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: profile, isPending: loading, error: queryError } = useQuery({
    queryKey: ["companyProfile"],
    queryFn: () => api.getCompany(session.accessToken)
  });

  const { data: auditEvents = [] } = useQuery({
    queryKey: ["companyAudit"],
    queryFn: () => api.getCompanyAudit(session.accessToken),
    enabled: isAdmin
  });

  const [customFormData, setCustomFormData] = useState<UpdateCompanyInput | null>(null);

  const formData: UpdateCompanyInput = customFormData ?? {
    legalName: profile?.legalName ?? profile?.name ?? "",
    tradingName: profile?.tradingName ?? "",
    registrationNumber: profile?.registrationNumber ?? "",
    industry: profile?.industry ?? "",
    address: profile?.address ?? "",
    city: profile?.city ?? "",
    stateProvince: profile?.stateProvince ?? "",
    country: profile?.country ?? "",
    postalCode: profile?.postalCode ?? "",
    contactPerson: profile?.contactPerson ?? "",
    businessEmail: profile?.businessEmail ?? "",
    phoneNumber: profile?.phoneNumber ?? "",
    timezone: profile?.timezone ?? "UTC",
    measurementUnits: profile?.measurementUnits ?? "METRIC",
    cargoTemperaturePolicy: profile?.cargoTemperaturePolicy ?? "STRICT_COLD_CHAIN",
    logoReference: profile?.logoReference ?? "",
    dataRetentionDays: profile?.dataRetentionDays ?? 90
  };

  const setFormData = (updated: UpdateCompanyInput) => {
    setCustomFormData(updated);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!isAdmin) return;

    setSaving(true);
    setError(null);
    setSuccess(null);

    try {
      await api.updateCompany(formData, session.accessToken);
      setSuccess("Company profile updated successfully.");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["companyProfile"] }),
        queryClient.invalidateQueries({ queryKey: ["companyAudit"] })
      ]);
      setCustomFormData(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update company profile");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Stack spacing={3}>
        <Skeleton variant="rectangular" height={60} sx={{ borderRadius: 2 }} />
        <Skeleton variant="rectangular" height={400} sx={{ borderRadius: 2 }} />
      </Stack>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ sm: "center" }} spacing={2} mb={3}>
        <Box>
          <Stack direction="row" alignItems="center" spacing={1.5}>
            <BusinessRounded sx={{ fontSize: 28, color: "primary.main" }} />
            <Typography variant="h5" fontWeight={800}>
              Company Profile
            </Typography>
          </Stack>
          <Typography variant="body2" color="text.secondary">
            Manage organization details, registration numbers, measurement standards, and cold-chain compliance policies.
          </Typography>
        </Box>
        {profile && (
          <Stack direction="row" spacing={1} alignItems="center">
            <Chip label={`Code: ${profile.code}`} variant="outlined" size="small" />
            <Chip
              label={isAdmin ? "Administrator Access" : "Read Only"}
              color={isAdmin ? "primary" : "default"}
              size="small"
            />
          </Stack>
        )}
      </Stack>

      {(error || queryError) && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error ?? (queryError instanceof Error ? queryError.message : "Failed to load company profile")}
        </Alert>
      )}

      {success && (
        <Alert severity="success" icon={<CheckCircleRounded />} sx={{ mb: 3 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Main Profile Form */}
        <Grid size={{ xs: 12, lg: 8 }}>
          <Card variant="outlined" sx={{ borderRadius: 3 }}>
            <CardContent sx={{ p: { xs: 2.5, md: 3.5 } }}>
              <Box component="form" onSubmit={handleSubmit}>
                <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                  Organization Information
                </Typography>
                <Typography variant="caption" color="text.secondary" display="block" mb={2}>
                  Core legal entity details recorded for regulatory compliance and carrier contracts.
                </Typography>

                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Legal Name *"
                      value={formData.legalName}
                      onChange={(e) => setFormData({ ...formData, legalName: e.target.value })}
                      disabled={!isAdmin || saving}
                      required
                      helperText="Official registered company name"
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Trading Name / Brand"
                      value={formData.tradingName}
                      onChange={(e) => setFormData({ ...formData, tradingName: e.target.value })}
                      disabled={!isAdmin || saving}
                      helperText="Operating display name"
                    />
                  </Grid>

                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Registration Number *"
                      value={formData.registrationNumber}
                      onChange={(e) => setFormData({ ...formData, registrationNumber: e.target.value })}
                      disabled={!isAdmin || saving}
                      required
                      helperText="Unique commercial/corporate register ID"
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Industry"
                      value={formData.industry}
                      onChange={(e) => setFormData({ ...formData, industry: e.target.value })}
                      disabled={!isAdmin || saving}
                      helperText="e.g. Cold Chain Logistics, Pharma"
                    />
                  </Grid>

                  <Grid size={{ xs: 12 }}>
                    <TextField
                      fullWidth
                      label="Address"
                      value={formData.address}
                      onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                      disabled={!isAdmin || saving}
                    />
                  </Grid>

                  <Grid size={{ xs: 12, sm: 3 }}>
                    <TextField
                      fullWidth
                      label="City"
                      value={formData.city}
                      onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                      disabled={!isAdmin || saving}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 3 }}>
                    <TextField
                      fullWidth
                      label="State / Province"
                      value={formData.stateProvince}
                      onChange={(e) => setFormData({ ...formData, stateProvince: e.target.value })}
                      disabled={!isAdmin || saving}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 3 }}>
                    <TextField
                      fullWidth
                      label="Country"
                      value={formData.country}
                      onChange={(e) => setFormData({ ...formData, country: e.target.value })}
                      disabled={!isAdmin || saving}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 3 }}>
                    <TextField
                      fullWidth
                      label="Postal Code"
                      value={formData.postalCode}
                      onChange={(e) => setFormData({ ...formData, postalCode: e.target.value })}
                      disabled={!isAdmin || saving}
                    />
                  </Grid>
                </Grid>

                <Divider sx={{ my: 3 }} />

                <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                  Contact & Communication
                </Typography>
                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField
                      fullWidth
                      label="Contact Person"
                      value={formData.contactPerson}
                      onChange={(e) => setFormData({ ...formData, contactPerson: e.target.value })}
                      disabled={!isAdmin || saving}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField
                      fullWidth
                      type="email"
                      label="Business Email *"
                      value={formData.businessEmail}
                      onChange={(e) => setFormData({ ...formData, businessEmail: e.target.value })}
                      disabled={!isAdmin || saving}
                      required
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField
                      fullWidth
                      label="Phone Number"
                      value={formData.phoneNumber}
                      onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                      disabled={!isAdmin || saving}
                      helperText="E.164 format (+15550100000)"
                    />
                  </Grid>
                </Grid>

                <Divider sx={{ my: 3 }} />

                <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                  Operational Policies & Standards
                </Typography>
                <Grid container spacing={2}>
                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField
                      select
                      fullWidth
                      label="Timezone"
                      value={formData.timezone}
                      onChange={(e) => setFormData({ ...formData, timezone: e.target.value })}
                      disabled={!isAdmin || saving}
                    >
                      <MenuItem value="UTC">UTC (Coordinated Universal Time)</MenuItem>
                      <MenuItem value="America/New_York">Eastern Time (US & Canada)</MenuItem>
                      <MenuItem value="America/Chicago">Central Time (US & Canada)</MenuItem>
                      <MenuItem value="America/Los_Angeles">Pacific Time (US & Canada)</MenuItem>
                      <MenuItem value="Europe/London">London (GMT/BST)</MenuItem>
                      <MenuItem value="Asia/Kolkata">India (IST)</MenuItem>
                    </TextField>
                  </Grid>

                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField
                      select
                      fullWidth
                      label="Measurement Units"
                      value={formData.measurementUnits}
                      onChange={(e) => setFormData({ ...formData, measurementUnits: e.target.value })}
                      disabled={!isAdmin || saving}
                    >
                      <MenuItem value="METRIC">Metric (kg, km, °C)</MenuItem>
                      <MenuItem value="IMPERIAL">Imperial (lbs, miles, °F)</MenuItem>
                    </TextField>
                  </Grid>

                  <Grid size={{ xs: 12, sm: 4 }}>
                    <TextField
                      select
                      fullWidth
                      label="Cargo Temperature Policy"
                      value={formData.cargoTemperaturePolicy}
                      onChange={(e) => setFormData({ ...formData, cargoTemperaturePolicy: e.target.value })}
                      disabled={!isAdmin || saving}
                    >
                      <MenuItem value="STRICT_COLD_CHAIN">Strict Cold Chain (Pharma/Frozen)</MenuItem>
                      <MenuItem value="PERISHABLE_STANDARD">Perishable Standard (Produce/Dairy)</MenuItem>
                      <MenuItem value="AMBIENT_TOLERANT">Ambient Tolerant</MenuItem>
                    </TextField>
                  </Grid>

                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      label="Logo URL / Reference"
                      value={formData.logoReference}
                      onChange={(e) => setFormData({ ...formData, logoReference: e.target.value })}
                      disabled={!isAdmin || saving}
                      helperText="Path or URL to company badge"
                    />
                  </Grid>

                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      type="number"
                      label="Telemetry Retention (Days)"
                      value={formData.dataRetentionDays}
                      onChange={(e) => setFormData({ ...formData, dataRetentionDays: parseInt(e.target.value) || 90 })}
                      disabled={!isAdmin || saving}
                      inputProps={{ min: 30, max: 365 }}
                      helperText="Audit storage window (30-365 days)"
                    />
                  </Grid>
                </Grid>

                {isAdmin && (
                  <Box mt={4} textAlign="right">
                    <Button
                      type="submit"
                      variant="contained"
                      size="large"
                      startIcon={saving ? <CircularProgress size={18} color="inherit" /> : <SaveRounded />}
                      disabled={saving}
                      sx={{ px: 4, py: 1.2, fontWeight: 700 }}
                    >
                      {saving ? "Saving Changes…" : "Save Company Profile"}
                    </Button>
                  </Box>
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Sidebar: Metadata & Audit Trail */}
        <Grid size={{ xs: 12, lg: 4 }}>
          <Stack spacing={3}>
            <Card variant="outlined" sx={{ borderRadius: 3 }}>
              <CardContent sx={{ p: 2.5 }}>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  Organization Summary
                </Typography>
                <Stack spacing={1.5} mt={1}>
                  <Box>
                    <Typography variant="caption" color="text.secondary">
                      Company ID
                    </Typography>
                    <Typography variant="body2" sx={{ fontFamily: "monospace", wordBreak: "break-all" }}>
                      {profile?.id}
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" color="text.secondary">
                      Created At
                    </Typography>
                    <Typography variant="body2">
                      {profile?.createdAt ? new Date(profile.createdAt).toLocaleString() : "—"}
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="caption" color="text.secondary">
                      Last Updated
                    </Typography>
                    <Typography variant="body2">
                      {profile?.updatedAt ? new Date(profile.updatedAt).toLocaleString() : "—"}
                    </Typography>
                  </Box>
                </Stack>
              </CardContent>
            </Card>

            {isAdmin && (
              <Card variant="outlined" sx={{ borderRadius: 3 }}>
                <CardContent sx={{ p: 2.5 }}>
                  <Stack direction="row" alignItems="center" spacing={1} mb={2}>
                    <HistoryRounded sx={{ fontSize: 20, color: "primary.main" }} />
                    <Typography variant="subtitle2" fontWeight={700}>
                      Audit History
                    </Typography>
                  </Stack>

                  {auditEvents.length === 0 ? (
                    <Typography variant="caption" color="text.secondary">
                      No recent audit modifications recorded for this company.
                    </Typography>
                  ) : (
                    <Stack spacing={1.5}>
                      {auditEvents.slice(0, 5).map((item) => (
                        <Box
                          key={item.id}
                          sx={{
                            p: 1.5,
                            borderRadius: 1.5,
                            bgcolor: "action.hover",
                            border: "1px solid",
                            borderColor: "divider"
                          }}
                        >
                          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={0.5}>
                            <Typography variant="caption" fontWeight={700}>
                              {item.eventType}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {new Date(item.occurredAt).toLocaleTimeString()}
                            </Typography>
                          </Stack>
                          <Typography variant="caption" color="text.secondary" sx={{ display: "block", fontFamily: "monospace" }}>
                            Trace: {item.traceId.slice(0, 8)}…
                          </Typography>
                        </Box>
                      ))}
                    </Stack>
                  )}
                </CardContent>
              </Card>
            )}
          </Stack>
        </Grid>
      </Grid>
    </Box>
  );
}
