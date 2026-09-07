import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import LockResetRounded from "@mui/icons-material/LockResetRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CircularProgress from "@mui/material/CircularProgress";
import Container from "@mui/material/Container";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api } from "../api";

export function AcceptInvitationPage() {
  const [searchParams] = useSearchParams();
  const initialToken = searchParams.get("token") ?? "";

  const [token, setToken] = useState(initialToken);
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [completed, setCompleted] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!token.trim()) {
      setError("Invitation token is required.");
      return;
    }
    if (password.length < 8) {
      setError("Password must be at least 8 characters long.");
      return;
    }
    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await api.acceptInvitation({ token: token.trim(), password });
      setCompleted(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to activate account. The invitation may be invalid or expired.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      minHeight="100vh"
      display="flex"
      alignItems="center"
      justifyContent="center"
      sx={{ bgcolor: "#071b2a", p: 2 }}
    >
      <Container maxWidth="sm">
        <Card variant="outlined" sx={{ borderRadius: 3, bgcolor: "background.paper", boxShadow: "0 8px 32px rgba(0,0,0,0.3)" }}>
          <CardContent sx={{ p: { xs: 3, sm: 4.5 } }}>
            {completed ? (
              <Stack spacing={2.5} alignItems="center" textAlign="center" py={2}>
                <Box
                  sx={{
                    width: 64,
                    height: 64,
                    borderRadius: "50%",
                    bgcolor: "rgba(16, 185, 129, 0.1)",
                    color: "success.main",
                    display: "grid",
                    placeItems: "center"
                  }}
                >
                  <CheckCircleRounded sx={{ fontSize: 38 }} />
                </Box>
                <Typography variant="h5" fontWeight={800}>
                  Account Successfully Activated!
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Your password has been securely created. You can now log in to the NEEPO-CARE operations platform.
                </Typography>
                <Button
                  component={Link}
                  to="/login"
                  variant="contained"
                  size="large"
                  fullWidth
                  sx={{ mt: 2, fontWeight: 700, py: 1.2 }}
                >
                  Proceed to Sign In
                </Button>
              </Stack>
            ) : (
              <Box component="form" onSubmit={handleSubmit}>
                <Stack direction="row" alignItems="center" spacing={1.5} mb={1}>
                  <LockResetRounded sx={{ fontSize: 28, color: "primary.main" }} />
                  <Typography variant="h5" fontWeight={800}>
                    Complete Account Setup
                  </Typography>
                </Stack>
                <Typography variant="body2" color="text.secondary" mb={3}>
                  Welcome to NEEPO-CARE. Create a secure password to complete your organization onboarding.
                </Typography>

                {error && (
                  <Alert severity="error" sx={{ mb: 2.5 }} onClose={() => setError(null)}>
                    {error}
                  </Alert>
                )}

                <Stack spacing={2.5}>
                  <TextField
                    fullWidth
                    label="Invitation Token *"
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    required
                    disabled={loading || Boolean(initialToken)}
                    helperText="One-time onboarding security token"
                  />

                  <TextField
                    fullWidth
                    type="password"
                    label="New Password *"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    disabled={loading}
                    helperText="Minimum 8 characters with letters and symbols"
                  />

                  <TextField
                    fullWidth
                    type="password"
                    label="Confirm Password *"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    disabled={loading}
                  />

                  <Button
                    type="submit"
                    variant="contained"
                    size="large"
                    fullWidth
                    disabled={loading}
                    startIcon={loading ? <CircularProgress size={18} color="inherit" /> : null}
                    sx={{ mt: 2, py: 1.2, fontWeight: 700 }}
                  >
                    {loading ? "Activating Account…" : "Activate Account & Set Password"}
                  </Button>
                </Stack>
              </Box>
            )}
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
}
