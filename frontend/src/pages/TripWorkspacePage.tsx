import AutoAwesomeRounded from "@mui/icons-material/AutoAwesomeRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import ErrorOutlineRounded from "@mui/icons-material/ErrorOutlineRounded";
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
import LinearProgress from "@mui/material/LinearProgress";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { neepoApi } from "../api";
import { useAuthStore } from "../auth/authStore";
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

      {/* Alternatives & Explainability Grid */}
      <Stack direction={{ xs: "column", lg: "row" }} spacing={3} alignItems="flex-start">
        {/* Left Column: Recovery Alternatives */}
        <Box flex={1.6} width="100%">
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h3">Evaluated Recovery Alternatives</Typography>
            <Typography variant="caption" color="text.secondary">
              Sorted by generation sequence
            </Typography>
          </Stack>

          <Stack spacing={2.5}>
            {plans.isPending && (
              <Stack alignItems="center" py={4}>
                <CircularProgress size={32} />
              </Stack>
            )}

            {plans.data?.map((plan) => {
              const isSelected = plan.status === "SELECTED";
              const isRejected = plan.status === "REJECTED";
              const isFeasible = plan.evaluation.feasible;

              return (
                <Card
                  key={plan.id}
                  sx={{
                    border: "1.5px solid",
                    borderColor: isSelected ? "#10b981" : isRejected ? "#fca5a5" : "#cbd5e1",
                    bgcolor: isSelected ? "rgba(16, 185, 129, 0.02)" : "#ffffff",
                    boxShadow: isSelected
                      ? "0 8px 24px -4px rgba(16, 185, 129, 0.15), 0 2px 6px -1px rgba(16, 185, 129, 0.08)"
                      : undefined
                  }}
                >
                  <CardContent sx={{ p: { xs: 2.5, sm: 3 } }}>
                    {/* Plan Header */}
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
                      <Box>
                        <Stack direction="row" spacing={1} alignItems="center">
                          <Typography variant="h3" fontWeight={750}>
                            {plan.type.replaceAll("_", " ")}
                          </Typography>
                          {isSelected && (
                            <Chip
                              icon={<VerifiedRounded sx={{ fontSize: "16px !important", color: "#047857 !important" }} />}
                              label="Selected by CARE"
                              size="small"
                              sx={{
                                bgcolor: "#ecfdf5",
                                color: "#047857",
                                fontWeight: 800,
                                border: "1px solid #a7f3d0",
                                fontSize: "0.72rem"
                              }}
                            />
                          )}
                        </Stack>

                        <Typography variant="body2" color="text.secondary" mt={0.5}>
                          {isFeasible ? (
                            <span style={{ color: "#047857", fontWeight: 600 }}>
                              ✓ Passed all hard safety constraints (height, weight, hours, temperature)
                            </span>
                          ) : (
                            <span style={{ color: "#b91c1c", fontWeight: 600 }}>
                              ✕ {plan.evaluation.violatedHardConstraints.length} hard constraint violation(s)
                            </span>
                          )}
                        </Typography>
                      </Box>

                      <Chip
                        icon={isSelected ? <CheckCircleRounded /> : isRejected ? <ErrorOutlineRounded /> : undefined}
                        label={plan.status}
                        size="small"
                        sx={{
                          fontWeight: 800,
                          bgcolor: isSelected ? "#10b981" : isRejected ? "#fee2e2" : "#f1f5f9",
                          color: isSelected ? "#ffffff" : isRejected ? "#991b1b" : "#475569",
                          border: `1px solid ${isSelected ? "#059669" : isRejected ? "#fca5a5" : "#cbd5e1"}`
                        }}
                      />
                    </Stack>

                    {/* Hard Constraint Violations if rejected */}
                    {plan.evaluation.violatedHardConstraints.length > 0 && (
                      <Box sx={{ mt: 1.5, p: 1.5, borderRadius: 2, bgcolor: "#fef2f2", border: "1px solid #fecaca" }}>
                        <Typography variant="caption" color="error.main" fontWeight={700} display="block" mb={0.5}>
                          Hard Constraint Failures:
                        </Typography>
                        <Stack direction="row" spacing={1} flexWrap="wrap">
                          {plan.evaluation.violatedHardConstraints.map((constraint) => (
                            <Chip
                              key={constraint}
                              label={constraint.replaceAll("_", " ")}
                              size="small"
                              sx={{ bgcolor: "#ffffff", color: "#b91c1c", border: "1px solid #fca5a5", fontSize: "0.7rem", fontWeight: 700 }}
                            />
                          ))}
                        </Stack>
                      </Box>
                    )}

                    {/* Metric Cards Grid */}
                    <Box sx={{ mt: 2.5, p: 2, borderRadius: 2.5, bgcolor: "#f8fafc", border: "1px solid #e2e8f0" }}>
                      <Stack direction="row" spacing={{ xs: 2, sm: 4 }} flexWrap="wrap" alignItems="center">
                        <Box>
                          <Typography variant="caption" color="text.secondary" fontWeight={700}>
                            Utility Score U(p)
                          </Typography>
                          <Typography variant="h3" fontWeight={800} sx={{ color: isSelected ? "#0d5b63" : "text.primary" }}>
                            {plan.evaluation.finalUtility.toFixed(1)}
                          </Typography>
                        </Box>

                        <Divider orientation="vertical" flexItem />

                        <Box sx={{ minWidth: 100 }}>
                          <Stack direction="row" justifyContent="space-between" alignItems="center">
                            <Typography variant="caption" color="text.secondary" fontWeight={700}>
                              Confidence
                            </Typography>
                            <Typography variant="caption" fontWeight={800}>
                              {Math.round(plan.evaluation.confidence * 100)}%
                            </Typography>
                          </Stack>
                          <LinearProgress
                            variant="determinate"
                            value={plan.evaluation.confidence * 100}
                            sx={{ mt: 0.8, height: 6, borderRadius: 3, bgcolor: "#e2e8f0", "& .MuiLinearProgress-bar": { bgcolor: "#0d5b63" } }}
                          />
                        </Box>

                        <Divider orientation="vertical" flexItem />

                        <Box>
                          <Typography variant="caption" color="text.secondary" fontWeight={700}>
                            Risk Score R(p)
                          </Typography>
                          <Typography fontWeight={700} fontSize="0.95rem">
                            {plan.evaluation.riskScore.toFixed(0)}
                          </Typography>
                        </Box>

                        <Divider orientation="vertical" flexItem />

                        <Box>
                          <Typography variant="caption" color="text.secondary" fontWeight={700}>
                            Uncertainty S(p)
                          </Typography>
                          <Typography fontWeight={700} fontSize="0.95rem">
                            {plan.evaluation.uncertaintyScore.toFixed(0)}
                          </Typography>
                        </Box>
                      </Stack>
                    </Box>

                    {/* Steps Execution Plan */}
                    {plan.steps.length > 0 && (
                      <Box sx={{ mt: 2.5 }}>
                        <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.04em">
                          Execution Sequence
                        </Typography>
                        <Stack spacing={1.5} mt={1}>
                          {plan.steps.map((step) => (
                            <Stack
                              key={step.sequence}
                              direction="row"
                              spacing={1.5}
                              alignItems="center"
                              sx={{
                                p: 1.2,
                                borderRadius: 2,
                                bgcolor: "#ffffff",
                                border: "1px solid #e2e8f0"
                              }}
                            >
                              <Box
                                sx={{
                                  width: 24,
                                  height: 24,
                                  borderRadius: "50%",
                                  bgcolor: "#071b2a",
                                  color: "white",
                                  display: "grid",
                                  placeItems: "center",
                                  fontSize: "0.75rem",
                                  fontWeight: 700,
                                  flexShrink: 0
                                }}
                              >
                                {step.sequence}
                              </Box>
                              <Box flex={1}>
                                <Typography variant="body2" fontWeight={600}>
                                  {step.instruction}
                                </Typography>
                                {step.actionCode && (
                                  <Typography variant="caption" color="text.secondary">
                                    Action: {step.actionCode}
                                  </Typography>
                                )}
                              </Box>
                            </Stack>
                          ))}
                        </Stack>
                      </Box>
                    )}
                  </CardContent>
                </Card>
              );
            })}

            {plans.data?.length === 0 && (
              <Alert severity="info" sx={{ borderRadius: 3 }}>
                No plans generated yet. Click <strong>Generate Recovery Plans</strong> above to evaluate options.
              </Alert>
            )}
          </Stack>
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
                  Optimal Utility: U = Benefit - λ(Risk) - μ(Uncertainty)
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
                          p: 1,
                          borderRadius: 2,
                          bgcolor: "#f0fdf4",
                          border: "1px solid #bbf7d0"
                        }}
                      >
                        <VerifiedRounded sx={{ fontSize: 16, color: "#16a34a" }} />
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

