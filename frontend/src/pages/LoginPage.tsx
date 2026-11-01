import AdminPanelSettingsRounded from "@mui/icons-material/AdminPanelSettingsRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import LockRounded from "@mui/icons-material/LockRounded";
import ShieldRounded from "@mui/icons-material/ShieldRounded";
import VisibilityOffRounded from "@mui/icons-material/VisibilityOffRounded";
import VisibilityRounded from "@mui/icons-material/VisibilityRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Container from "@mui/material/Container";
import Divider from "@mui/material/Divider";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import InputLabel from "@mui/material/InputLabel";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import type { UserRole } from "@contracts";
import { apiMode, neepoApi } from "../api";
import { useAuthStore } from "../auth/authStore";

export function LoginPage() {
  const [email, setEmail] = useState("dispatcher@neepo.local");
  const [password, setPassword] = useState("ChangeMe123!");
  const [showPassword, setShowPassword] = useState(false);
  const [role, setRole] = useState<UserRole>("DISPATCHER");
  const session = useAuthStore((state) => state.session);
  const setSession = useAuthStore((state) => state.setSession);
  const navigate = useNavigate();

  const emailDomain = email && email.includes("@") && email.split("@")[1]?.includes(".")
    ? email.split("@")[1].toLowerCase()
    : null;

  const { data: ssoDiscovery = null, isFetching: discoveringSso } = useQuery({
    queryKey: ["ssoDiscovery", emailDomain],
    queryFn: () => neepoApi.discoverSso({ domain: emailDomain! }),
    enabled: Boolean(emailDomain)
  });

  const login = useMutation({
    mutationFn: neepoApi.login,
    onSuccess: (result) => {
      setSession(result);
      navigate("/");
    }
  });

  const ssoLogin = useMutation({
    mutationFn: () =>
      neepoApi.loginSso({
        email,
        idpToken: "idp_demo_oidc_saml_assertion_token",
        domain: ssoDiscovery?.domain ?? (email.split("@")[1] || "neepo.com")
      }),
    onSuccess: (result) => {
      setSession(result);
      navigate("/");
    }
  });

  if (session) return <Navigate to="/" replace />;

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (ssoDiscovery?.enforceSso) {
      ssoLogin.mutate();
    } else {
      login.mutate({ email, password, demoRole: role });
    }
  };

  const setPreset = (presetEmail: string, presetRole: UserRole) => {
    setEmail(presetEmail);
    setPassword("ChangeMe123!");
    setRole(presetRole);
  };

  return (
    <Box
      minHeight="100vh"
      sx={{
        display: "grid",
        placeItems: "center",
        p: { xs: 2, sm: 3 },
        background: "radial-gradient(ellipse at top left, rgba(13, 91, 99, 0.12) 0%, rgba(248, 250, 252, 0.8) 50%, #f8fafc 100%)",
        position: "relative"
      }}
    >
      <Container maxWidth="sm">
        <Stack spacing={3.5} alignItems="center">
          {/* Header Brand */}
          <Stack spacing={1.5} alignItems="center">
            <Box
              sx={{
                width: 96,
                height: 96,
                borderRadius: "20px",
                bgcolor: "#ffffff",
                border: "1px solid #e2e8f0",
                display: "grid",
                placeItems: "center",
                p: 0.8,
                boxShadow: "0 14px 30px -6px rgba(7, 27, 42, 0.15)",
                overflow: "hidden"
              }}
            >
              <Box
                component="img"
                src="/neepo-logo.jpg"
                alt="NEEPO-CARE Logo"
                sx={{ width: "100%", height: "100%", objectFit: "cover", borderRadius: "14px" }}
              />
            </Box>
            <Box textAlign="center">
              <Typography variant="h2" fontWeight={800} letterSpacing="-0.03em" sx={{ lineHeight: 1.1 }}>
                NEEPO-CARE
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Delivering Care, Connecting Everywhere
              </Typography>
            </Box>
          </Stack>

          <Box textAlign="center" maxWidth={460}>
            <Typography variant="h1" sx={{ fontSize: { xs: "1.8rem", sm: "2.3rem" } }}>
              Decisions that keep moving.
            </Typography>
            <Typography color="text.secondary" mt={1} fontSize="0.95rem">
              Deterministic recovery generation, transparent constraint validation, and real-time fleet guidance.
            </Typography>
          </Box>

          {/* Login Card */}
          <Card
            sx={{
              width: "100%",
              borderRadius: 4,
              border: "1px solid #e2e8f0",
              boxShadow: "0 20px 40px -10px rgba(15, 23, 42, 0.08), 0 1px 3px rgba(0, 0, 0, 0.05)"
            }}
          >
            <CardContent sx={{ p: { xs: 3, sm: 4.5 } }}>
              {/* Quick Preset Selector */}
              <Box mb={3}>
                <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.05em">
                  Quick Demo Accounts
                </Typography>
                <Stack direction={{ xs: "column", sm: "row" }} spacing={1} mt={1}>
                  <Button
                    type="button"
                    variant={email === "dispatcher@neepo.local" ? "contained" : "outlined"}
                    size="small"
                    startIcon={<AdminPanelSettingsRounded />}
                    onClick={() => setPreset("dispatcher@neepo.local", "DISPATCHER")}
                    sx={{ flex: 1, py: 0.8, fontSize: "0.8rem" }}
                  >
                    Dispatcher
                  </Button>
                  <Button
                    type="button"
                    variant={email === "driver@neepo.local" ? "contained" : "outlined"}
                    size="small"
                    startIcon={<LocalShippingRounded />}
                    onClick={() => setPreset("driver@neepo.local", "DRIVER")}
                    sx={{ flex: 1, py: 0.8, fontSize: "0.8rem" }}
                  >
                    Driver
                  </Button>
                  <Button
                    type="button"
                    variant={email === "dispatcher@neepo.com" ? "contained" : "outlined"}
                    size="small"
                    color="secondary"
                    startIcon={<ShieldRounded />}
                    onClick={() => setPreset("dispatcher@neepo.com", "DISPATCHER")}
                    sx={{ flex: 1, py: 0.8, fontSize: "0.8rem" }}
                  >
                    Enterprise SSO
                  </Button>
                </Stack>
              </Box>

              <Divider sx={{ my: 2.5 }}>
                <Chip
                  label={ssoDiscovery?.ssoAvailable ? "Enterprise SSO Detected" : "Sign in credentials"}
                  color={ssoDiscovery?.ssoAvailable ? "primary" : "default"}
                  size="small"
                  sx={{ fontSize: "0.72rem", fontWeight: 700 }}
                />
              </Divider>

              <Stack component="form" onSubmit={submit} spacing={2.5}>
                {apiMode === "mock" && (
                  <Alert severity="info" sx={{ fontSize: "0.85rem" }}>
                    Demonstration mode active. You can test without live backend connectivity.
                  </Alert>
                )}

                {/* SSO Discovery Notice */}
                {ssoDiscovery?.ssoAvailable && (
                  <Alert
                    severity={ssoDiscovery.enforceSso ? "warning" : "info"}
                    icon={<ShieldRounded fontSize="inherit" />}
                    sx={{ fontSize: "0.85rem" }}
                  >
                    <Typography variant="subtitle2" fontWeight={700}>
                      {ssoDiscovery.companyName} ({ssoDiscovery.idpType} SSO)
                    </Typography>
                    <Typography variant="caption" display="block">
                      {ssoDiscovery.enforceSso
                        ? "Enterprise SSO is ENFORCED for your organization domain. Password login is disabled."
                        : "Single Sign-On is configured for this domain. You can sign in via your identity provider or password."}
                    </Typography>
                  </Alert>
                )}

                {login.isError && (
                  <Alert severity="error" sx={{ fontSize: "0.85rem" }}>
                    {login.error.message || "Invalid credentials. Please verify your email and password."}
                  </Alert>
                )}

                {ssoLogin.isError && (
                  <Alert severity="error" sx={{ fontSize: "0.85rem" }}>
                    {ssoLogin.error.message || "SSO Federation error. Please verify your corporate credentials."}
                  </Alert>
                )}

                <TextField
                  label="Email address"
                  type="email"
                  required
                  fullWidth
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  autoComplete="username"
                  placeholder="name@neepo.local or user@neepo.com"
                  InputProps={{
                    endAdornment: discoveringSso ? (
                      <InputAdornment position="end">
                        <CircularProgress size={16} />
                      </InputAdornment>
                    ) : undefined
                  }}
                />

                {!ssoDiscovery?.enforceSso && (
                  <TextField
                    label="Password"
                    type={showPassword ? "text" : "password"}
                    required={!ssoDiscovery?.enforceSso}
                    fullWidth
                    inputProps={{ minLength: 8 }}
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    autoComplete="current-password"
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton
                            aria-label="toggle password visibility"
                            onClick={() => setShowPassword(!showPassword)}
                            edge="end"
                            size="small"
                          >
                            {showPassword ? <VisibilityOffRounded fontSize="small" /> : <VisibilityRounded fontSize="small" />}
                          </IconButton>
                        </InputAdornment>
                      )
                    }}
                  />
                )}

                {apiMode === "mock" && !ssoDiscovery?.enforceSso && (
                  <FormControl fullWidth>
                    <InputLabel id="demo-role-label">Demo Role</InputLabel>
                    <Select
                      labelId="demo-role-label"
                      label="Demo Role"
                      value={role}
                      onChange={(event) => setRole(event.target.value as UserRole)}
                    >
                      <MenuItem value="DISPATCHER">Dispatcher</MenuItem>
                      <MenuItem value="DRIVER">Driver</MenuItem>
                      <MenuItem value="ADMIN">Administrator</MenuItem>
                    </Select>
                  </FormControl>
                )}

                {/* Primary Action Button */}
                {ssoDiscovery?.enforceSso ? (
                  <Button
                    type="button"
                    variant="contained"
                    color="secondary"
                    size="large"
                    disabled={ssoLogin.isPending}
                    onClick={() => ssoLogin.mutate()}
                    startIcon={ssoLogin.isPending ? <CircularProgress size={18} color="inherit" /> : <ShieldRounded />}
                    sx={{
                      mt: 1,
                      py: 1.4,
                      fontSize: "0.95rem",
                      fontWeight: 750,
                      borderRadius: 3,
                      boxShadow: "0 8px 20px -4px rgba(100, 50, 180, 0.4)"
                    }}
                  >
                    {ssoLogin.isPending ? "Connecting to IdP…" : `Sign In with ${ssoDiscovery.idpType} SSO`}
                  </Button>
                ) : (
                  <Button
                    type="submit"
                    variant="contained"
                    size="large"
                    disabled={login.isPending}
                    startIcon={login.isPending ? <CircularProgress size={18} color="inherit" /> : <LockRounded />}
                    sx={{
                      mt: 1,
                      py: 1.4,
                      fontSize: "0.95rem",
                      fontWeight: 750,
                      borderRadius: 3,
                      boxShadow: "0 8px 20px -4px rgba(13, 91, 99, 0.4)"
                    }}
                  >
                    {login.isPending ? "Authenticating…" : "Sign In to Operations"}
                  </Button>
                )}

                {/* Optional SSO Secondary Action */}
                {ssoDiscovery?.ssoAvailable && !ssoDiscovery.enforceSso && (
                  <Button
                    type="button"
                    variant="outlined"
                    color="secondary"
                    size="medium"
                    disabled={ssoLogin.isPending}
                    onClick={() => ssoLogin.mutate()}
                    startIcon={ssoLogin.isPending ? <CircularProgress size={16} /> : <ShieldRounded />}
                    sx={{ fontWeight: 650, borderRadius: 2.5 }}
                  >
                    {ssoLogin.isPending ? "Verifying SSO…" : `Or Sign In with ${ssoDiscovery.idpType} SSO (${ssoDiscovery.companyName})`}
                  </Button>
                )}
              </Stack>

              <Box sx={{ mt: 3, p: 1.5, borderRadius: 2.5, bgcolor: "#f8fafc", border: "1px solid #e2e8f0", textAlign: "center" }}>
                <Typography variant="caption" color="text.secondary" display="block">
                  Demo Password: <strong style={{ color: "#0f172a" }}>ChangeMe123!</strong> • Enterprise SSO:{" "}
                  <strong style={{ color: "#0f172a" }}>dispatcher@neepo.com</strong>
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Stack>
      </Container>
    </Box>
  );
}
