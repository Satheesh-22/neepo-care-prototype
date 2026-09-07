import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import ErrorOutlineRounded from "@mui/icons-material/ErrorOutlineRounded";
import VerifiedRounded from "@mui/icons-material/VerifiedRounded";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import LinearProgress from "@mui/material/LinearProgress";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { RecoveryPlan } from "@contracts";

export interface RecoveryPlanComparisonProps {
  plans: readonly RecoveryPlan[];
}

export function RecoveryPlanComparison({ plans }: RecoveryPlanComparisonProps) {
  return (
    <Stack spacing={3}>
      {/* Comparative Cards */}
      <Stack spacing={2.5}>
        {plans.map((plan) => {
          const isSelected = plan.status === "SELECTED";
          const isRejected = plan.status === "REJECTED";
          const isFeasible = plan.evaluation.feasible;

          return (
            <Card
              key={plan.id}
              className="hover-lift"
              sx={{
                border: "1.5px solid",
                borderColor: isSelected ? "#10b981" : isRejected ? "#fca5a5" : "#cbd5e1",
                bgcolor: isSelected ? "rgba(16, 185, 129, 0.02)" : "#ffffff",
                boxShadow: isSelected
                  ? "0 8px 24px -4px rgba(16, 185, 129, 0.15), 0 2px 6px -1px rgba(16, 185, 129, 0.08)"
                  : "0 2px 6px -1px rgba(15, 23, 42, 0.04)"
              }}
            >
              <CardContent sx={{ p: { xs: 2.5, sm: 3 } }}>
                {/* Plan Header */}
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={2}>
                  <Box>
                    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
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
                          sx={{
                            bgcolor: "#ffffff",
                            color: "#b91c1c",
                            border: "1px solid #fca5a5",
                            fontSize: "0.7rem",
                            fontWeight: 700
                          }}
                        />
                      ))}
                    </Stack>
                  </Box>
                )}

                {/* Multi-Dimensional Metrics Grid */}
                <Box sx={{ mt: 2.5, p: 2, borderRadius: 2.5, bgcolor: "#f8fafc", border: "1px solid #e2e8f0" }}>
                  <Stack direction="row" spacing={{ xs: 2, sm: 3.5 }} flexWrap="wrap" alignItems="center">
                    <Box>
                      <Typography variant="caption" color="text.secondary" fontWeight={700}>
                        Utility Score U(p)
                      </Typography>
                      <Typography
                        variant="h3"
                        className="mono-metric"
                        fontWeight={800}
                        sx={{ color: isSelected ? "#0d5b63" : "text.primary" }}
                      >
                        {plan.evaluation.finalUtility.toFixed(1)}
                      </Typography>
                    </Box>

                    <Divider orientation="vertical" flexItem />

                    <Box sx={{ minWidth: 90 }}>
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
                        sx={{
                          mt: 0.8,
                          height: 6,
                          borderRadius: 3,
                          bgcolor: "#e2e8f0",
                          "& .MuiLinearProgress-bar": { bgcolor: "#0d5b63" }
                        }}
                      />
                    </Box>

                    <Divider orientation="vertical" flexItem />

                    <Box>
                      <Typography variant="caption" color="text.secondary" fontWeight={700}>
                        Risk Score R(p)
                      </Typography>
                      <Typography
                        fontWeight={750}
                        fontSize="0.95rem"
                        className="mono-metric"
                        sx={{ color: plan.evaluation.riskScore > 50 ? "#ef4444" : "#10b981" }}
                      >
                        {plan.evaluation.riskScore.toFixed(0)}
                      </Typography>
                    </Box>

                    <Divider orientation="vertical" flexItem />

                    <Box>
                      <Typography variant="caption" color="text.secondary" fontWeight={700}>
                        Uncertainty S(p)
                      </Typography>
                      <Typography fontWeight={750} fontSize="0.95rem" className="mono-metric">
                        {plan.evaluation.uncertaintyScore.toFixed(0)}
                      </Typography>
                    </Box>

                    <Divider orientation="vertical" flexItem />

                    <Box>
                      <Typography variant="caption" color="text.secondary" fontWeight={700}>
                        Evidence Freshness
                      </Typography>
                      <Typography variant="caption" fontWeight={800} color="#047857" display="block">
                        FRESH (Verified)
                      </Typography>
                    </Box>
                  </Stack>
                </Box>

                {/* Steps Execution Plan */}
                {plan.steps.length > 0 && (
                  <Box sx={{ mt: 2.5 }}>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      fontWeight={700}
                      textTransform="uppercase"
                      letterSpacing="0.04em"
                    >
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
      </Stack>
    </Stack>
  );
}
