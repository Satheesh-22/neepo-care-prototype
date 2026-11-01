import AdminPanelSettingsRounded from "@mui/icons-material/AdminPanelSettingsRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import ContentCopyRounded from "@mui/icons-material/ContentCopyRounded";
import ErrorOutlineRounded from "@mui/icons-material/ErrorOutlineRounded";
import HistoryRounded from "@mui/icons-material/HistoryRounded";
import KeyRounded from "@mui/icons-material/KeyRounded";
import NetworkCheckRounded from "@mui/icons-material/NetworkCheckRounded";
import PersonAddRounded from "@mui/icons-material/PersonAddRounded";
import SaveRounded from "@mui/icons-material/SaveRounded";
import SecurityRounded from "@mui/icons-material/SecurityRounded";
import ShieldRounded from "@mui/icons-material/ShieldRounded";
import SpeedRounded from "@mui/icons-material/SpeedRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Dialog from "@mui/material/Dialog";
import DialogContent from "@mui/material/DialogContent";
import DialogTitle from "@mui/material/DialogTitle";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import FormControlLabel from "@mui/material/FormControlLabel";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import Switch from "@mui/material/Switch";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import type { SsoAuditLogItem, SsoIdpType, UpdateCompanySsoConfigRequest, UserRole } from "@contracts";
import { neepoApi } from "../api";
import { useAuthStore } from "../auth/authStore";

export function EnterpriseSsoPage() {
  const session = useAuthStore((state) => state.session)!;
  const token = session.accessToken;
  const queryClient = useQueryClient();

  const [saving, setSaving] = useState(false);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [selectedAuditLog, setSelectedAuditLog] = useState<SsoAuditLogItem | null>(null);

  const { data: ssoConfig, isLoading: loadingConfig, error: configError } = useQuery({
    queryKey: ["companySsoConfig"],
    queryFn: () => neepoApi.getCompanySsoConfig(token)
  });

  const { data: auditLogs = [], isLoading: loadingLogs } = useQuery({
    queryKey: ["ssoAuditLogs"],
    queryFn: () => neepoApi.listSsoAuditLogs(token, 50)
  });

  const [idpType, setIdpType] = useState<SsoIdpType>("OIDC");
  const [idpIssuerUrl, setIdpIssuerUrl] = useState("");
  const [clientId, setClientId] = useState("");
  const [clientSecret, setClientSecret] = useState("");
  const [ssoDomain, setSsoDomain] = useState("");
  const [enabled, setEnabled] = useState(true);
  const [enforceSso, setEnforceSso] = useState(false);
  const [jitProvisioningEnabled, setJitProvisioningEnabled] = useState(true);
  const [defaultRole, setDefaultRole] = useState<UserRole>("DISPATCHER");
  const [metadataText, setMetadataText] = useState("{}");
  const [formInitialized, setFormInitialized] = useState(false);

  // Synchronize initial configuration into form state once loaded
  if (ssoConfig && !formInitialized) {
    setIdpType(ssoConfig.idpType);
    setIdpIssuerUrl(ssoConfig.idpIssuerUrl);
    setClientId(ssoConfig.clientId);
    setSsoDomain(ssoConfig.ssoDomain);
    setEnabled(ssoConfig.enabled);
    setEnforceSso(ssoConfig.enforceSso);
    setJitProvisioningEnabled(ssoConfig.jitProvisioningEnabled);
    setDefaultRole(ssoConfig.defaultRole);
    setMetadataText(JSON.stringify(ssoConfig.metadata ?? {}, null, 2));
    setFormInitialized(true);
  }

  const testMutation = useMutation({
    mutationFn: () => neepoApi.testSsoConfiguration(token)
  });

  const saveMutation = useMutation({
    mutationFn: (payload: UpdateCompanySsoConfigRequest) =>
      neepoApi.updateCompanySsoConfig(payload, token),
    onSuccess: (updated) => {
      setSaving(false);
      setSuccessMsg("Enterprise SSO configuration successfully updated and activated.");
      setErrorMsg(null);
      queryClient.setQueryData(["companySsoConfig"], updated);
      queryClient.invalidateQueries({ queryKey: ["ssoAuditLogs"] });
    },
    onError: (err: unknown) => {
      setSaving(false);
      setErrorMsg(err instanceof Error ? err.message : "Failed to update SSO configuration.");
      setSuccessMsg(null);
    }
  });

  const handleSave = (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setErrorMsg(null);
    setSuccessMsg(null);

    let parsedMetadata: Record<string, unknown> = {};
    try {
      if (metadataText.trim()) {
        parsedMetadata = JSON.parse(metadataText);
      }
    } catch {
      setErrorMsg("Invalid JSON metadata format. Please check your syntax.");
      setSaving(false);
      return;
    }

    const payload: UpdateCompanySsoConfigRequest = {
      idpType,
      idpIssuerUrl: idpIssuerUrl.trim(),
      clientId: clientId.trim(),
      clientSecret: clientSecret.trim() ? clientSecret.trim() : undefined,
      ssoDomain: ssoDomain.trim().toLowerCase(),
      enabled,
      enforceSso,
      jitProvisioningEnabled,
      defaultRole,
      metadata: parsedMetadata
    };

    saveMutation.mutate(payload);
  };

  const getEventBadge = (eventType: string) => {
    switch (eventType) {
      case "LOGIN_SUCCESS":
        return <Chip label="LOGIN SUCCESS" size="small" color="success" sx={{ fontWeight: 700, fontSize: "0.72rem" }} />;
      case "LOGIN_FAILED":
        return <Chip label="LOGIN FAILED" size="small" color="error" sx={{ fontWeight: 700, fontSize: "0.72rem" }} />;
      case "USER_PROVISIONED":
        return <Chip label="USER PROVISIONED (JIT)" size="small" color="primary" sx={{ fontWeight: 700, fontSize: "0.72rem" }} />;
      case "SSO_ENFORCED":
        return <Chip label="SSO ENFORCED" size="small" color="warning" sx={{ fontWeight: 700, fontSize: "0.72rem" }} />;
      case "CONFIG_UPDATED":
        return <Chip label="CONFIG UPDATED" size="small" color="secondary" sx={{ fontWeight: 700, fontSize: "0.72rem" }} />;
      default:
        return <Chip label={eventType} size="small" />;
    }
  };

  return (
    <Box sx={{ p: { xs: 2, md: 3 }, maxWidth: 1400, mx: "auto" }}>
      {/* Page Header */}
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "flex-start", sm: "center" }} spacing={2} mb={3}>
        <Box>
          <Stack direction="row" alignItems="center" spacing={1.5}>
            <ShieldRounded sx={{ fontSize: 32, color: "primary.main" }} />
            <Typography variant="h4" fontWeight={800} letterSpacing="-0.02em">
              Enterprise SSO & Identity Federation
            </Typography>
          </Stack>
          <Typography variant="body2" color="text.secondary" mt={0.5}>
            Configure SAML 2.0 / OpenID Connect (OIDC) Single Sign-On, Just-in-Time user provisioning, and enterprise enforcement.
          </Typography>
        </Box>
        <Stack direction="row" spacing={1.5}>
          <Button
            variant="outlined"
            startIcon={testMutation.isPending ? <CircularProgress size={16} /> : <NetworkCheckRounded />}
            onClick={() => testMutation.mutate()}
            disabled={testMutation.isPending}
            sx={{ fontWeight: 600, textTransform: "none" }}
          >
            Test Connection
          </Button>
          <Button
            variant="contained"
            startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveRounded />}
            onClick={handleSave}
            disabled={saving}
            sx={{ fontWeight: 700, textTransform: "none" }}
          >
            Save Configuration
          </Button>
        </Stack>
      </Stack>

      {/* Alerts */}
      {successMsg && (
        <Alert severity="success" onClose={() => setSuccessMsg(null)} sx={{ mb: 3 }}>
          {successMsg}
        </Alert>
      )}
      {errorMsg && (
        <Alert severity="error" onClose={() => setErrorMsg(null)} sx={{ mb: 3 }}>
          {errorMsg}
        </Alert>
      )}
      {configError && (
        <Alert severity="info" sx={{ mb: 3 }}>
          No enterprise SSO profile currently exists for your company. Configure and save to activate enterprise identity federation.
        </Alert>
      )}

      {/* Test Connection Result Banner */}
      {testMutation.data && (
        <Alert
          severity={testMutation.data.success ? "success" : "error"}
          icon={testMutation.data.success ? <CheckCircleRounded fontSize="inherit" /> : <ErrorOutlineRounded fontSize="inherit" />}
          onClose={() => testMutation.reset()}
          sx={{ mb: 3 }}
        >
          <Stack spacing={0.5}>
            <Typography variant="subtitle2" fontWeight={700}>
              {testMutation.data.success ? "Identity Provider Connection Verified" : "Identity Provider Connection Failed"}
            </Typography>
            <Typography variant="body2">
              {testMutation.data.message}
            </Typography>
            <Stack direction="row" spacing={2} mt={0.5} alignItems="center">
              <Chip
                icon={<SpeedRounded sx={{ fontSize: 14 }} />}
                label={`Round-trip Latency: ${testMutation.data.latencyMs} ms`}
                size="small"
                variant="outlined"
              />
              <Typography variant="caption" color="text.secondary">
                Tested at: {new Date(testMutation.data.testedAt).toLocaleTimeString()}
              </Typography>
            </Stack>
          </Stack>
        </Alert>
      )}

      {/* Summary KPI Cards */}
      <Grid container spacing={2.5} mb={3.5}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2, height: "100%" }}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase">
                  Federation Status
                </Typography>
                <KeyRounded sx={{ color: enabled ? "success.main" : "text.disabled", fontSize: 22 }} />
              </Stack>
              <Typography variant="h5" fontWeight={800}>
                {enabled ? "ACTIVE" : "DISABLED"}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Domain: {ssoDomain || "Not Configured"}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2, height: "100%" }}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase">
                  SSO Enforcement
                </Typography>
                <SecurityRounded sx={{ color: enforceSso ? "warning.main" : "text.secondary", fontSize: 22 }} />
              </Stack>
              <Typography variant="h5" fontWeight={800} color={enforceSso ? "warning.dark" : "text.primary"}>
                {enforceSso ? "STRICT SSO" : "OPTIONAL"}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {enforceSso ? "Password login blocked" : "Password & SSO permitted"}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2, height: "100%" }}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase">
                  JIT Provisioning
                </Typography>
                <PersonAddRounded sx={{ color: jitProvisioningEnabled ? "info.main" : "text.disabled", fontSize: 22 }} />
              </Stack>
              <Typography variant="h5" fontWeight={800}>
                {jitProvisioningEnabled ? "ENABLED" : "RESTRICTED"}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Default Role: {defaultRole}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card variant="outlined" sx={{ borderRadius: 2, height: "100%" }}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase">
                  Protocol Type
                </Typography>
                <AdminPanelSettingsRounded sx={{ color: "primary.main", fontSize: 22 }} />
              </Stack>
              <Typography variant="h5" fontWeight={800}>
                {idpType}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {idpType === "OIDC" ? "OpenID Connect 1.0" : "SAML 2.0 Web Browser SSO"}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Main Form & Configuration */}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 7 }}>
          <Card variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 3 }}>
              <Typography variant="h6" fontWeight={800} mb={0.5}>
                Identity Provider (IdP) Configuration
              </Typography>
              <Typography variant="body2" color="text.secondary" mb={3}>
                Set up connection parameters to your corporate IdP (Okta, Microsoft Entra ID, PingIdentity, or Google Workspace).
              </Typography>

              {loadingConfig && <CircularProgress size={24} sx={{ mb: 2 }} />}

              <Box component="form" onSubmit={handleSave}>
                <Grid container spacing={2.5}>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <FormControl fullWidth size="small">
                      <InputLabel id="idp-type-label">Protocol Architecture</InputLabel>
                      <Select
                        labelId="idp-type-label"
                        value={idpType}
                        label="Protocol Architecture"
                        onChange={(e) => setIdpType(e.target.value as SsoIdpType)}
                      >
                        <MenuItem value="OIDC">OpenID Connect (OIDC 1.0)</MenuItem>
                        <MenuItem value="SAML2">SAML 2.0 Web SSO</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>

                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      size="small"
                      label="Corporate SSO Domain"
                      placeholder="e.g. neepo.com"
                      value={ssoDomain}
                      onChange={(e) => setSsoDomain(e.target.value)}
                      helperText="Users with this email domain will use this IdP"
                      required
                    />
                  </Grid>

                  <Grid size={{ xs: 12 }}>
                    <TextField
                      fullWidth
                      size="small"
                      label="IdP Issuer / Metadata URL"
                      placeholder="https://auth.company.com/oauth2/default"
                      value={idpIssuerUrl}
                      onChange={(e) => setIdpIssuerUrl(e.target.value)}
                      helperText="Issuer endpoint used to fetch discovery JSON or SAML metadata"
                      required
                    />
                  </Grid>

                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      size="small"
                      label="Client ID / Application ID"
                      value={clientId}
                      onChange={(e) => setClientId(e.target.value)}
                      required
                    />
                  </Grid>

                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      fullWidth
                      size="small"
                      type="password"
                      label="Client Secret (Optional update)"
                      placeholder="••••••••••••••••"
                      value={clientSecret}
                      onChange={(e) => setClientSecret(e.target.value)}
                      helperText="Leave empty to retain existing secret"
                    />
                  </Grid>

                  <Grid size={{ xs: 12 }}>
                    <Divider sx={{ my: 1 }} />
                    <Typography variant="subtitle2" fontWeight={700} mb={1.5}>
                      Provisioning & Access Policy
                    </Typography>
                  </Grid>

                  <Grid size={{ xs: 12, sm: 6 }}>
                    <FormControl fullWidth size="small">
                      <InputLabel id="default-role-label">Default JIT User Role</InputLabel>
                      <Select
                        labelId="default-role-label"
                        value={defaultRole}
                        label="Default JIT User Role"
                        onChange={(e) => setDefaultRole(e.target.value as UserRole)}
                      >
                        <MenuItem value="DISPATCHER">Dispatcher (Standard)</MenuItem>
                        <MenuItem value="DRIVER">Driver</MenuItem>
                        <MenuItem value="ADMIN">Administrator</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>

                  <Grid size={{ xs: 12, sm: 6 }}>
                    <Stack spacing={1}>
                      <FormControlLabel
                        control={<Switch checked={enabled} onChange={(e) => setEnabled(e.target.checked)} color="primary" />}
                        label={<Typography variant="body2" fontWeight={600}>Enable Enterprise SSO</Typography>}
                      />
                      <FormControlLabel
                        control={<Switch checked={enforceSso} onChange={(e) => setEnforceSso(e.target.checked)} color="warning" />}
                        label={<Typography variant="body2" fontWeight={600}>Enforce SSO (Block Password Login)</Typography>}
                      />
                      <FormControlLabel
                        control={<Switch checked={jitProvisioningEnabled} onChange={(e) => setJitProvisioningEnabled(e.target.checked)} color="primary" />}
                        label={<Typography variant="body2" fontWeight={600}>Just-In-Time (JIT) Auto-Provisioning</Typography>}
                      />
                    </Stack>
                  </Grid>

                  <Grid size={{ xs: 12 }}>
                    <TextField
                      fullWidth
                      multiline
                      rows={4}
                      size="small"
                      label="Custom Federation Metadata (JSON)"
                      value={metadataText}
                      onChange={(e) => setMetadataText(e.target.value)}
                      helperText="Optional JSON parameters (e.g. redirectUri, claimsMapping, signingCertificate)"
                    />
                  </Grid>

                  <Grid size={{ xs: 12 }}>
                    <Stack direction="row" spacing={2} justifyContent="flex-end" mt={2}>
                      <Button
                        variant="outlined"
                        onClick={() => testMutation.mutate()}
                        disabled={testMutation.isPending}
                        startIcon={testMutation.isPending ? <CircularProgress size={16} /> : <NetworkCheckRounded />}
                      >
                        Test IdP Discovery
                      </Button>
                      <Button
                        type="submit"
                        variant="contained"
                        disabled={saving}
                        startIcon={saving ? <CircularProgress size={16} color="inherit" /> : <SaveRounded />}
                      >
                        Save Settings
                      </Button>
                    </Stack>
                  </Grid>
                </Grid>
              </Box>
            </CardContent>
          </Card>

          {/* Automated SCIM 2.0 User Provisioning Card */}
          <Card variant="outlined" sx={{ borderRadius: 2, mt: 3 }}>
            <CardContent sx={{ p: 3 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Box>
                  <Typography variant="h6" fontWeight={800}>
                    Automated SCIM 2.0 User Provisioning
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    RFC 7643 / RFC 7644 user lifecycle synchronization with Okta, Microsoft Entra ID, and PingFederate.
                  </Typography>
                </Box>
                <Chip
                  label={enabled ? "SCIM READY" : "DISABLED"}
                  color={enabled ? "success" : "default"}
                  size="small"
                  sx={{ fontWeight: 700 }}
                />
              </Stack>

              <Stack spacing={2.5} sx={{ mt: 2.5 }}>
                <TextField
                  size="small"
                  label="SCIM 2.0 Base URL"
                  fullWidth
                  value={`${window.location.origin}/api/v1/scim/v2`}
                  InputProps={{
                    readOnly: true,
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          size="small"
                          onClick={() => navigator.clipboard.writeText(`${window.location.origin}/api/v1/scim/v2`)}
                          title="Copy SCIM Base URL"
                        >
                          <ContentCopyRounded fontSize="small" />
                        </IconButton>
                      </InputAdornment>
                    )
                  }}
                  helperText="Configure this endpoint in your Identity Provider SCIM provisioning settings."
                />

                <Alert severity="info" sx={{ borderRadius: 2 }}>
                  <Typography variant="subtitle2" fontWeight={700}>
                    SCIM Authentication
                  </Typography>
                  <Typography variant="body2">
                    Use HTTP Header Bearer Token authentication. When generated, configure the Bearer Token in your IdP application settings.
                  </Typography>
                </Alert>

                <Stack direction="row" spacing={2} justifyContent="flex-end">
                  <Button
                    variant="outlined"
                    startIcon={<KeyRounded />}
                    onClick={async () => {
                      try {
                        const res = await neepoApi.generateScimToken(token);
                        alert(`SCIM Token generated: ${res.token}\n\nPlease copy and store this token safely in your IdP. It will not be shown again.`);
                      } catch (e: unknown) {
                        alert(e instanceof Error ? e.message : "Failed to generate SCIM token.");
                      }
                    }}
                  >
                    Generate SCIM Token
                  </Button>
                </Stack>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* Audit Log Trail */}
        <Grid size={{ xs: 12, lg: 5 }}>
          <Card variant="outlined" sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 3 }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
                <Stack direction="row" spacing={1} alignItems="center">
                  <HistoryRounded sx={{ color: "text.secondary", fontSize: 22 }} />
                  <Typography variant="h6" fontWeight={800}>
                    SSO Audit & Provisioning Log
                  </Typography>
                </Stack>
                <Chip label={`${auditLogs.length} Events`} size="small" variant="outlined" />
              </Stack>
              <Typography variant="body2" color="text.secondary" mb={2}>
                Immutable security ledger tracking enterprise logins, JIT registrations, and enforcement blocks.
              </Typography>

              {loadingLogs ? (
                <Stack alignItems="center" py={4}>
                  <CircularProgress size={24} />
                </Stack>
              ) : auditLogs.length === 0 ? (
                <Alert severity="info">No SSO security events recorded yet.</Alert>
              ) : (
                <TableContainer sx={{ maxHeight: 520 }}>
                  <Table size="small" stickyHeader>
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 700 }}>Event</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>User Email</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Time</TableCell>
                        <TableCell sx={{ fontWeight: 700 }} align="right">Details</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {auditLogs.map((log) => (
                        <TableRow key={log.id} hover>
                          <TableCell>{getEventBadge(log.eventType)}</TableCell>
                          <TableCell>
                            <Typography variant="caption" fontWeight={600} display="block">
                              {log.userEmail}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {log.ipAddress || "Internal"}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Typography variant="caption" color="text.secondary">
                              {new Date(log.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })}
                            </Typography>
                          </TableCell>
                          <TableCell align="right">
                            <Button
                              size="small"
                              variant="text"
                              onClick={() => setSelectedAuditLog(log)}
                              sx={{ fontSize: "0.72rem", p: 0.5 }}
                            >
                              View
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Audit Log Details Dialog */}
      <Dialog
        open={Boolean(selectedAuditLog)}
        onClose={() => setSelectedAuditLog(null)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 800 }}>
          SSO Security Audit Event Details
        </DialogTitle>
        <DialogContent dividers>
          {selectedAuditLog && (
            <Stack spacing={2}>
              <Box>
                <Typography variant="caption" color="text.secondary" textTransform="uppercase" fontWeight={700}>
                  Event ID
                </Typography>
                <Typography variant="body2" fontFamily="monospace">
                  {selectedAuditLog.id}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" textTransform="uppercase" fontWeight={700}>
                  Event Type
                </Typography>
                <Box mt={0.5}>{getEventBadge(selectedAuditLog.eventType)}</Box>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" textTransform="uppercase" fontWeight={700}>
                  User Email & Identity Provider
                </Typography>
                <Typography variant="body2">
                  {selectedAuditLog.userEmail} ({selectedAuditLog.idpType})
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" textTransform="uppercase" fontWeight={700}>
                  Client Network Context
                </Typography>
                <Typography variant="body2">
                  IP: {selectedAuditLog.ipAddress || "127.0.0.1"} • UA: {selectedAuditLog.userAgent || "Unknown"}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" textTransform="uppercase" fontWeight={700}>
                  Event Metadata Payload
                </Typography>
                <Box
                  component="pre"
                  sx={{
                    p: 1.5,
                    borderRadius: 1,
                    bgcolor: "grey.100",
                    fontFamily: "monospace",
                    fontSize: "0.78rem",
                    overflowX: "auto"
                  }}
                >
                  {JSON.stringify(selectedAuditLog.details ?? {}, null, 2)}
                </Box>
              </Box>
            </Stack>
          )}
        </DialogContent>
      </Dialog>
    </Box>
  );
}
