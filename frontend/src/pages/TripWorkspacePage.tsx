import AutoAwesomeRounded from "@mui/icons-material/AutoAwesomeRounded";
import InfoOutlined from "@mui/icons-material/InfoOutlined";
import PsychologyRounded from "@mui/icons-material/PsychologyRounded";
import SpeedRounded from "@mui/icons-material/SpeedRounded";
import VerifiedRounded from "@mui/icons-material/VerifiedRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { neepoApi } from "../api";
import { useAuthStore } from "../auth/authStore";
import { RecoveryPlanComparison } from "../components/RecoveryPlanComparison";
import { StatusPill } from "../components/StatusPill";
import { TripMap } from "../components/TripMap";

export function TripWorkspacePage() {
  const { tripId = "" } = useParams();
  const session = useAuthStore((state) => state.session)!;
  const queryClient = useQueryClient();

  const trip = useQuery({
    queryKey: ["trip", tripId],
    queryFn: () => neepoApi.getTrip(tripId, session.accessToken),
    enabled: Boolean(tripId)
  });

  const plans = useQuery({
    queryKey: ["plans", tripId],
    queryFn: () => neepoApi.listRecoveryPlans(tripId, session.accessToken),
    enabled: Boolean(tripId)
  });

  const explanation = useQuery({
    queryKey: ["explanation", tripId],
    queryFn: () => neepoApi.getDecisionExplanation(tripId, session.accessToken),
    enabled: Boolean(tripId)
  });

  const generate = useMutation({
    mutationFn: () => neepoApi.generateRecoveryPlans(tripId, session.accessToken),
    onSuccess: (generated) => queryClient.setQueryData(["plans", tripId], generated)
  });

  if (trip.isPending) {
    return (
      <Stack alignItems="center" justifyContent="center" minHeight="50vh" spacing={2}>
        <CircularProgress size={40} />
        <Typography color="text.secondary">Loading trip workspace…</Typography>
      </Stack>
    );
  }

  if (trip.isError) {
    return (
      <Alert severity="error" sx={{ mt: 3 }}>
        {trip.error.message}
      </Alert>
    );
  }

  return (
    <Stack spacing={3.5}>
      {/* Workspace Header */}
      <Stack direction={{ xs: "column", md: "row" }} justifyContent="space-between" alignItems={{ xs: "flex-start", md: "center" }} spacing={2}>
        <Box>
          <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap">
            <Typography variant="h2" fontWeight={800}>
              {trip.data?.referenceCode}
            </Typography>
            {trip.data && <StatusPill status={trip.data.status} />}
            <Chip
              label="Live Disruption Recovery"
              size="small"
              sx={{ bgcolor: "#f1f5f9", fontWeight: 700, fontSize: "0.75rem" }}
            />
          </Stack>
          <Typography color="text.secondary" mt={0.5} fontSize="0.95rem">
            Explainable decision intelligence · Hard safety validation · Deterministic scoring
          </Typography>
        </Box>

        {session.user.role !== "DRIVER" && (
          <Button
            variant="contained"
            size="large"
            startIcon={generate.isPending ? <CircularProgress size={18} color="inherit" /> : <AutoAwesomeRounded />}
            onClick={() => generate.mutate()}
            disabled={generate.isPending}
            sx={{
              py: 1.2,
              px: 2.5,
              borderRadius: 3,
              boxShadow: "0 6px 16px -2px rgba(13, 91, 99, 0.4)"
            }}
          >
            {generate.isPending ? "Evaluating Alternatives…" : "Generate Recovery Plans"}
          </Button>
        )}
      </Stack>

      {generate.isError && (
        <Alert severity="error">
          {generate.error.message}
        </Alert>
      )}

      {/* Map Overview */}
      <TripMap />

      {/* Recovery Journey Timeline */}
      <Card sx={{ border: "1px solid #e2e8f0", bgcolor: "#ffffff" }}>
        <CardContent sx={{ p: { xs: 2.5, sm: 3 } }}>
          <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.05em" display="block" mb={1.5}>
            Decision & Recovery Lifecycle
          </Typography>

          <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems={{ xs: "flex-start", sm: "center" }}>
            <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "#fef2f2", border: "1px solid #fca5a5", flex: 1, width: "100%" }}>
              <Typography variant="caption" color="error.main" fontWeight={800} display="block">1. Disruption Ingestion</Typography>
              <Typography variant="body2" fontWeight={650} color="#991b1b">Engine Overheat 106°C</Typography>
            </Box>

            <Typography sx={{ display: { xs: "none", sm: "block" }, color: "text.secondary" }}>→</Typography>

            <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "#f0f9ff", border: "1px solid #bae6fd", flex: 1, width: "100%" }}>
              <Typography variant="caption" color="info.main" fontWeight={800} display="block">2. Context Snapshot</Typography>
              <Typography variant="body2" fontWeight={650} color="#075985">Temporal Freshness Verified</Typography>
            </Box>

            <Typography sx={{ display: { xs: "none", sm: "block" }, color: "text.secondary" }}>→</Typography>

            <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "#ecfdf5", border: "1px solid #a7f3d0", flex: 1, width: "100%" }}>
              <Typography variant="caption" color="success.main" fontWeight={800} display="block">3. Deterministic Selection</Typography>
              <Typography variant="body2" fontWeight={650} color="#065f46">Safe Parking Mobile Mechanic</Typography>
            </Box>

            <Typography sx={{ display: { xs: "none", sm: "block" }, color: "text.secondary" }}>→</Typography>

            <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "#f8fafc", border: "1px solid #e2e8f0", flex: 1, width: "100%" }}>
              <Typography variant="caption" color="text.secondary" fontWeight={800} display="block">4. Driver Execution</Typography>
              <Typography variant="body2" fontWeight={650} color="#334155">Stabilize & Safe Stop</Typography>
            </Box>
          </Stack>
        </CardContent>
      </Card>

      {/* Alternatives & Explainability Grid */}
      <Stack direction={{ xs: "column", lg: "row" }} spacing={3} alignItems="flex-start">
        {/* Left Column: Recovery Alternatives */}
        <Box flex={1.6} width="100%">
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h3">Recovery alternatives</Typography>
            <Typography variant="caption" color="text.secondary">
              Evaluated with hard safety constraints
            </Typography>
          </Stack>

          {plans.isPending && (
            <Stack alignItems="center" py={4}>
              <CircularProgress size={32} />
            </Stack>
          )}

          {plans.data && plans.data.length > 0 ? (
            <RecoveryPlanComparison plans={plans.data} />
          ) : (
            !plans.isPending && (
              <Alert severity="info" sx={{ borderRadius: 3 }}>
                No plans generated yet. Click <strong>Generate Recovery Plans</strong> above to evaluate options.
              </Alert>
            )
          )}
        </Box>

        {/* Right Column: Decision Explanation */}
        <Card
          sx={{
            flex: 1,
            width: "100%",
            position: { lg: "sticky" },
            top: { lg: 84 },
            border: "1px solid #e2e8f0"
          }}
        >
          <CardContent sx={{ p: { xs: 2.5, sm: 3 } }}>
            <Stack direction="row" spacing={1.2} alignItems="center" mb={2}>
              <PsychologyRounded sx={{ color: "#0d5b63", fontSize: 26 }} />
              <Typography variant="h3">Why CARE Selected This</Typography>
            </Stack>

            {explanation.isPending && (
              <Stack alignItems="center" py={3}>
                <CircularProgress size={26} />
              </Stack>
            )}

            {explanation.data ? (
              <Stack spacing={2.5}>
                <Alert severity="success" icon={<SpeedRounded fontSize="inherit" />} sx={{ fontWeight: 600 }}>
                  Optimal Utility: U(p) = Benefit - λ(Risk) - μ(Uncertainty)
                </Alert>

                <Box>
                  <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.04em">
                    Selection Evidence Codes
                  </Typography>
                  <Stack spacing={1} mt={1}>
                    {explanation.data.selectionExplanationCodes.map((code) => (
                      <Stack
                        key={code}
                        direction="row"
                        spacing={1}
                        alignItems="center"
                        sx={{
                          p: 1.2,
                          borderRadius: 2,
                          bgcolor: "#f0fdf4",
                          border: "1px solid #bbf7d0"
                        }}
                      >
                        <VerifiedRounded sx={{ fontSize: 18, color: "#16a34a" }} />
                        <Typography variant="body2" fontWeight={650} color="#166534">
                          {code.replaceAll("_", " ")}
                        </Typography>
                      </Stack>
                    ))}
                  </Stack>
                </Box>

                <Divider />

                <Box>
                  <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.04em">
                    Deterministic Tie-Break Rule
                  </Typography>
                  <Box
                    sx={{
                      mt: 1,
                      p: 1.5,
                      borderRadius: 2,
                      bgcolor: "#071b2a",
                      color: "#a7f3d0",
                      fontFamily: "'JetBrains Mono', monospace",
                      fontSize: "0.78rem",
                      lineHeight: 1.4,
                      wordBreak: "break-all"
                    }}
                  >
                    {explanation.data.deterministicTieBreakRule}
                  </Box>
                </Box>

                <Box sx={{ p: 1.5, borderRadius: 2, bgcolor: "#f8fafc", border: "1px solid #e2e8f0" }}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <InfoOutlined sx={{ fontSize: 16, color: "text.secondary" }} />
                    <Typography variant="caption" color="text.secondary">
                      Evaluator Version 1.0.0 · Cryptographic input hash verified
                    </Typography>
                  </Stack>
                </Box>
              </Stack>
            ) : (
              !explanation.isPending && (
                <Typography color="text.secondary" mt={2} fontSize="0.9rem">
                  Decision transparency trail will be generated once recovery plans are evaluated.
                </Typography>
              )
            )}
          </CardContent>
        </Card>
      </Stack>
    </Stack>
  );
}
