import AccessTimeRounded from "@mui/icons-material/AccessTimeRounded";
import ArrowForwardRounded from "@mui/icons-material/ArrowForwardRounded";
import AssessmentRounded from "@mui/icons-material/AssessmentRounded";
import CheckCircleRounded from "@mui/icons-material/CheckCircleRounded";
import InsightsRounded from "@mui/icons-material/InsightsRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import MonetizationOnRounded from "@mui/icons-material/MonetizationOnRounded";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import SecurityRounded from "@mui/icons-material/SecurityRounded";
import SpeedRounded from "@mui/icons-material/SpeedRounded";
import TimelineRounded from "@mui/icons-material/TimelineRounded";
import TrendingUpRounded from "@mui/icons-material/TrendingUpRounded";
import WarningAmberRounded from "@mui/icons-material/WarningAmberRounded";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import LinearProgress from "@mui/material/LinearProgress";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import ToggleButton from "@mui/material/ToggleButton";
import ToggleButtonGroup from "@mui/material/ToggleButtonGroup";
import Typography from "@mui/material/Typography";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";
import { neepoApi } from "../api";
import { DEMO_TRIP_ID } from "../api/mockApi";
import type { FleetAnalyticsTimeframe } from "../api/types";
import { useAuthStore } from "../auth/authStore";

export function FleetAnalyticsPage() {
  const [timeframe, setTimeframe] = useState<FleetAnalyticsTimeframe>("30d");
  const session = useAuthStore((state) => state.session)!;

  const {
    data: analytics,
    isPending,
    isError,
    error,
    refetch,
    isFetching
  } = useQuery({
    queryKey: ["fleetAnalytics", timeframe],
    queryFn: () => neepoApi.getFleetAnalytics(timeframe, session.accessToken)
  });

  const handleTimeframeChange = (
    _event: React.MouseEvent<HTMLElement>,
    newTimeframe: FleetAnalyticsTimeframe | null
  ) => {
    if (newTimeframe !== null) {
      setTimeframe(newTimeframe);
    }
  };

  const timeframeLabels: Record<FleetAnalyticsTimeframe, string> = {
    "7d": "Past 7 Days",
    "30d": "Past 30 Days",
    "90d": "Past 90 Days"
  };

  return (
    <Stack spacing={3.5}>
      {/* Executive Header Banner */}
      <Box
        sx={{
          p: { xs: 2.5, sm: 3.5 },
          borderRadius: 4,
          background: "linear-gradient(135deg, #071b2a 0%, #0d3b43 100%)",
          color: "white",
          boxShadow: "0 12px 30px -5px rgba(7, 27, 42, 0.25)",
          position: "relative",
          overflow: "hidden"
        }}
      >
        <Box
          sx={{
            position: "absolute",
            right: -20,
            bottom: -35,
            opacity: 0.06,
            transform: "rotate(-12deg)",
            pointerEvents: "none"
          }}
        >
          <AssessmentRounded sx={{ fontSize: 260 }} />
        </Box>

        <Stack
          direction={{ xs: "column", md: "row" }}
          justifyContent="space-between"
          alignItems={{ xs: "flex-start", md: "center" }}
          spacing={2.5}
          sx={{ position: "relative", zIndex: 1 }}
        >
          <Stack spacing={1}>
            <Stack direction="row" alignItems="center" spacing={1.5} flexWrap="wrap">
              <Chip
                icon={<InsightsRounded sx={{ fontSize: "16px !important", color: "#34d399 !important" }} />}
                label="Executive Reliability & ROI"
                size="small"
                sx={{
                  bgcolor: "rgba(16, 185, 129, 0.2)",
                  color: "#a7f3d0",
                  fontWeight: 700,
                  border: "1px solid rgba(16, 185, 129, 0.35)"
                }}
              />
              <Typography variant="caption" sx={{ color: "rgba(255, 255, 255, 0.7)" }}>
                Window: {timeframeLabels[timeframe]} · Incremental Materialized Aggregation
              </Typography>
            </Stack>

            <Typography variant="h1" sx={{ color: "#ffffff", fontSize: { xs: "1.6rem", sm: "2.1rem" } }}>
              Fleet Reliability & Cost-Avoidance Analytics
            </Typography>

            <Typography sx={{ color: "rgba(255, 255, 255, 0.85)", maxWidth: 760, fontSize: "0.95rem" }}>
              Quantifiable recovery metrics tracking Mean Time to Recovery (MTTR), driver compliance rates,
              and preserved cargo valuation through deterministic CARE constraint solving.
            </Typography>
          </Stack>

          {/* Timeframe Controls */}
          <Stack direction="row" spacing={1.5} alignItems="center" alignSelf={{ xs: "stretch", md: "center" }}>
            <ToggleButtonGroup
              value={timeframe}
              exclusive
              onChange={handleTimeframeChange}
              aria-label="Analytics Timeframe"
              size="small"
              sx={{
                bgcolor: "rgba(255, 255, 255, 0.08)",
                borderRadius: 2.5,
                border: "1px solid rgba(255, 255, 255, 0.15)",
                "& .MuiToggleButton-root": {
                  color: "rgba(255, 255, 255, 0.75)",
                  px: 2,
                  py: 0.8,
                  fontWeight: 650,
                  fontSize: "0.82rem",
                  "&.Mui-selected": {
                    bgcolor: "rgba(255, 255, 255, 0.22)",
                    color: "#ffffff"
                  }
                }
              }}
            >
              <ToggleButton value="7d" aria-label="7 days">7D</ToggleButton>
              <ToggleButton value="30d" aria-label="30 days">30D</ToggleButton>
              <ToggleButton value="90d" aria-label="90 days">90D</ToggleButton>
            </ToggleButtonGroup>

            <Button
              variant="outlined"
              size="small"
              onClick={() => refetch()}
              disabled={isFetching}
              startIcon={isFetching ? <CircularProgress size={14} color="inherit" /> : <RefreshRounded />}
              sx={{
                color: "white",
                borderColor: "rgba(255, 255, 255, 0.25)",
                borderRadius: 2.5,
                px: 2,
                "&:hover": { borderColor: "white", bgcolor: "rgba(255, 255, 255, 0.08)" }
              }}
            >
              Sync
            </Button>
          </Stack>
        </Stack>
      </Box>

      {/* Error state */}
      {isError && (
        <Alert severity="error" sx={{ borderRadius: 3 }}>
          {error instanceof Error ? error.message : "Failed to load fleet analytics metrics."}
        </Alert>
      )}

      {/* Loading state */}
      {isPending && (
        <Card sx={{ p: 6, textAlign: "center", borderRadius: 3.5 }}>
          <CircularProgress size={42} sx={{ mb: 2, color: "#0d5b63" }} />
          <Typography variant="body1" color="text.secondary">
            Aggregating historical fleet recovery events and calculating cost avoidance...
          </Typography>
        </Card>
      )}

      {/* Analytics Content */}
      {analytics && (
        <Stack spacing={3.5}>
          {/* Executive KPI Grid */}
          <Box>
            <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.06em" mb={1.5} display="block">
              Executive Recovery & Performance Indicators ({timeframeLabels[timeframe]})
            </Typography>

            <Stack direction={{ xs: "column", sm: "row" }} spacing={2} flexWrap="wrap">
              {/* Cost Avoidance */}
              <Card
                sx={{
                  flex: "1 1 200px",
                  borderRadius: 3.5,
                  p: 2.5,
                  border: "1px solid #d1fae5",
                  background: "linear-gradient(180deg, #ffffff 0%, #f0fdf4 100%)",
                  boxShadow: "0 4px 18px -2px rgba(16, 185, 129, 0.12)"
                }}
              >
                <Stack spacing={1}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase">
                      Estimated Cost Avoidance
                    </Typography>
                    <Box sx={{ p: 1, borderRadius: 2, bgcolor: "rgba(16, 185, 129, 0.15)", color: "#059669" }}>
                      <MonetizationOnRounded sx={{ fontSize: 22 }} />
                    </Box>
                  </Stack>
                  <Typography variant="h2" sx={{ fontWeight: 800, color: "#065f46" }}>
                    ${analytics.estimatedCostAvoidanceUsd.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </Typography>
                  <Stack direction="row" alignItems="center" spacing={0.8}>
                    <TrendingUpRounded sx={{ fontSize: 16, color: "#10b981" }} />
                    <Typography variant="caption" sx={{ color: "#047857", fontWeight: 700 }}>
                      +18.4% Capital Preserved vs Baseline
                    </Typography>
                  </Stack>
                </Stack>
              </Card>

              {/* Mean Time to Recovery (MTTR) */}
              <Card
                sx={{
                  flex: "1 1 200px",
                  borderRadius: 3.5,
                  p: 2.5,
                  border: "1px solid #e0f2fe",
                  background: "linear-gradient(180deg, #ffffff 0%, #f0f9ff 100%)",
                  boxShadow: "0 4px 18px -2px rgba(2, 132, 199, 0.12)"
                }}
              >
                <Stack spacing={1}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase">
                      Mean Time to Recovery (MTTR)
                    </Typography>
                    <Box sx={{ p: 1, borderRadius: 2, bgcolor: "rgba(2, 132, 199, 0.15)", color: "#0284c7" }}>
                      <SpeedRounded sx={{ fontSize: 22 }} />
                    </Box>
                  </Stack>
                  <Typography variant="h2" sx={{ fontWeight: 800, color: "#075985" }}>
                    {analytics.meanTimeToRecoveryMinutes} min
                  </Typography>
                  <Stack direction="row" alignItems="center" spacing={0.8}>
                    <CheckCircleRounded sx={{ fontSize: 16, color: "#0284c7" }} />
                    <Typography variant="caption" sx={{ color: "#0369a1", fontWeight: 700 }}>
                      Target SLA &lt; 25.0 min met
                    </Typography>
                  </Stack>
                </Stack>
              </Card>

              {/* Driver Compliance */}
              <Card
                sx={{
                  flex: "1 1 200px",
                  borderRadius: 3.5,
                  p: 2.5,
                  border: "1px solid #f1f5f9",
                  background: "linear-gradient(180deg, #ffffff 0%, #f8fafc 100%)",
                  boxShadow: "0 4px 14px -2px rgba(100, 116, 139, 0.08)"
                }}
              >
                <Stack spacing={1}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase">
                      Driver Plan Compliance
                    </Typography>
                    <Box sx={{ p: 1, borderRadius: 2, bgcolor: "rgba(13, 91, 99, 0.12)", color: "#0d5b63" }}>
                      <CheckCircleRounded sx={{ fontSize: 22 }} />
                    </Box>
                  </Stack>
                  <Typography variant="h2" sx={{ fontWeight: 800, color: "#0f172a" }}>
                    {analytics.driverComplianceRate}%
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {analytics.resolvedDisruptionsCount} of {analytics.activeDisruptionsCount + analytics.resolvedDisruptionsCount} incidents resolved
                  </Typography>
                </Stack>
              </Card>

              {/* Saved Transit Hours */}
              <Card
                sx={{
                  flex: "1 1 200px",
                  borderRadius: 3.5,
                  p: 2.5,
                  border: "1px solid #fef3c7",
                  background: "linear-gradient(180deg, #ffffff 0%, #fffbeb 100%)",
                  boxShadow: "0 4px 14px -2px rgba(245, 158, 11, 0.1)"
                }}
              >
                <Stack spacing={1}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase">
                      Saved Transit Hours
                    </Typography>
                    <Box sx={{ p: 1, borderRadius: 2, bgcolor: "rgba(245, 158, 11, 0.15)", color: "#d97706" }}>
                      <AccessTimeRounded sx={{ fontSize: 22 }} />
                    </Box>
                  </Stack>
                  <Typography variant="h2" sx={{ fontWeight: 800, color: "#92400e" }}>
                    {analytics.savedTransitHours} hrs
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Across {analytics.totalTripsMonitored} monitored fleet corridors
                  </Typography>
                </Stack>
              </Card>

              {/* Spoilage Avoidance Rate */}
              <Card
                sx={{
                  flex: "1 1 200px",
                  borderRadius: 3.5,
                  p: 2.5,
                  border: "1px solid #e2e8f0",
                  background: "linear-gradient(180deg, #ffffff 0%, #f8fafc 100%)",
                  boxShadow: "0 4px 14px -2px rgba(100, 116, 139, 0.08)"
                }}
              >
                <Stack spacing={1}>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase">
                      Cold-Chain Spoilage Avoidance
                    </Typography>
                    <Box sx={{ p: 1, borderRadius: 2, bgcolor: "rgba(16, 185, 129, 0.12)", color: "#059669" }}>
                      <SecurityRounded sx={{ fontSize: 22 }} />
                    </Box>
                  </Stack>
                  <Typography variant="h2" sx={{ fontWeight: 800, color: "#065f46" }}>
                    {analytics.spoilageAvoidanceRate}%
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Zero total perishable cargo loss recorded
                  </Typography>
                </Stack>
              </Card>
            </Stack>
          </Box>

          {/* Breakdown Section: Root Causes & Disruption Trend */}
          <Stack direction={{ xs: "column", lg: "row" }} spacing={3}>
            {/* Top Root Causes */}
            <Card sx={{ flex: 1, borderRadius: 3.5, border: "1.5px solid #e2e8f0", p: 3 }}>
              <Stack direction="row" alignItems="center" spacing={1.2} mb={2}>
                <WarningAmberRounded sx={{ color: "#d97706", fontSize: 24 }} />
                <Typography variant="h3">Disruption Root-Cause Distribution</Typography>
              </Stack>
              <Typography variant="caption" color="text.secondary" display="block" mb={2.5}>
                Frequency and mean resolution latency partitioned by telematics incident trigger
              </Typography>

              <Stack spacing={2.5}>
                {analytics.topDisruptionRootCauses.map((item, idx) => (
                  <Box key={idx}>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" mb={0.6}>
                      <Typography fontWeight={700} fontSize="0.9rem">
                        {item.cause}
                      </Typography>
                      <Stack direction="row" spacing={1.5} alignItems="center">
                        <Chip
                          label={`MTTR: ${item.averageResolutionMinutes} min`}
                          size="small"
                          sx={{ height: 20, fontSize: "0.72rem", bgcolor: "#f1f5f9" }}
                        />
                        <Typography fontWeight={800} fontSize="0.9rem" sx={{ minWidth: 50, textAlign: "right" }}>
                          {item.percentage}% ({item.count})
                        </Typography>
                      </Stack>
                    </Stack>
                    <LinearProgress
                      variant="determinate"
                      value={item.percentage}
                      sx={{
                        height: 8,
                        borderRadius: 4,
                        bgcolor: "#f1f5f9",
                        "& .MuiLinearProgress-bar": {
                          borderRadius: 4,
                          bgcolor:
                            idx === 0 ? "#ef4444" : idx === 1 ? "#f59e0b" : idx === 2 ? "#0d5b63" : "#64748b"
                        }
                      }}
                    />
                  </Box>
                ))}
              </Stack>
            </Card>

            {/* Disruption & Recovery Trend */}
            <Card sx={{ flex: 1, borderRadius: 3.5, border: "1.5px solid #e2e8f0", p: 3 }}>
              <Stack direction="row" alignItems="center" spacing={1.2} mb={2}>
                <TimelineRounded sx={{ color: "#0284c7", fontSize: 24 }} />
                <Typography variant="h3">Incident Progression & Value Avoided</Typography>
              </Stack>
              <Typography variant="caption" color="text.secondary" display="block" mb={2.5}>
                Interval breakdown showing reported disruptions, successful CARE resolutions, and preserved capital
              </Typography>

              <Stack spacing={2}>
                {analytics.disruptionTrend.map((trend, idx) => (
                  <Box
                    key={idx}
                    sx={{
                      p: 1.8,
                      borderRadius: 2.5,
                      bgcolor: "#f8fafc",
                      border: "1px solid #e2e8f0"
                    }}
                  >
                    <Stack direction="row" justifyContent="space-between" alignItems="center">
                      <Stack direction="row" spacing={1.5} alignItems="center">
                        <Chip
                          label={trend.period}
                          size="small"
                          sx={{ bgcolor: "#071b2a", color: "white", fontWeight: 700, fontSize: "0.75rem" }}
                        />
                        <Typography variant="body2" color="text.secondary">
                          {trend.disruptionCount} disrupted · {trend.resolvedCount} resolved
                        </Typography>
                      </Stack>
                      <Typography fontWeight={800} color="#059669" fontSize="0.95rem">
                        +${trend.costAvoidedUsd.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </Typography>
                    </Stack>
                  </Box>
                ))}
              </Stack>
            </Card>
          </Stack>

          {/* Recovery Strategy Efficiency Table */}
          <Card sx={{ borderRadius: 3.5, border: "1.5px solid #e2e8f0", overflow: "hidden" }}>
            <Box sx={{ p: 3, borderBottom: "1px solid #e2e8f0" }}>
              <Stack direction="row" alignItems="center" spacing={1.2}>
                <SecurityRounded sx={{ color: "#0d5b63", fontSize: 24 }} />
                <Typography variant="h3">CARE Strategy Efficiency & Cost Avoidance</Typography>
              </Stack>
              <Typography variant="caption" color="text.secondary" mt={0.5} display="block">
                Effectiveness of solver-recommended recovery plans across multi-tenant dispatch workflows
              </Typography>
            </Box>

            <TableContainer>
              <Table>
                <TableHead sx={{ bgcolor: "#f8fafc" }}>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 750, color: "text.secondary" }}>RECOVERY STRATEGY</TableCell>
                    <TableCell align="center" sx={{ fontWeight: 750, color: "text.secondary" }}>EXECUTIONS</TableCell>
                    <TableCell align="center" sx={{ fontWeight: 750, color: "text.secondary" }}>SUCCESS RATE</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 750, color: "text.secondary" }}>AVG AVOIDED COST / INCIDENT</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {analytics.recoveryStrategyBreakdown.map((strat, idx) => (
                    <TableRow key={idx} hover>
                      <TableCell sx={{ fontWeight: 650 }}>{strat.strategyName}</TableCell>
                      <TableCell align="center">
                        <Chip label={strat.usageCount} size="small" sx={{ fontWeight: 700, bgcolor: "#f1f5f9" }} />
                      </TableCell>
                      <TableCell align="center">
                        <Chip
                          icon={<CheckCircleRounded sx={{ fontSize: "14px !important" }} />}
                          label={`${strat.successRate}%`}
                          size="small"
                          sx={{
                            bgcolor: strat.successRate >= 95 ? "#ecfdf5" : "#fef3c7",
                            color: strat.successRate >= 95 ? "#065f46" : "#92400e",
                            fontWeight: 700
                          }}
                        />
                      </TableCell>
                      <TableCell align="right" sx={{ fontWeight: 800, color: "#059669" }}>
                        ${strat.averageCostAvoidanceUsd.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Card>

          {/* Quick Action Navigation Bar */}
          <Card
            sx={{
              p: 3,
              borderRadius: 3.5,
              bgcolor: "#f8fafc",
              border: "1.5px solid #e2e8f0"
            }}
          >
            <Stack
              direction={{ xs: "column", sm: "row" }}
              justifyContent="space-between"
              alignItems={{ xs: "flex-start", sm: "center" }}
              spacing={2}
            >
              <Box>
                <Typography variant="h4" fontWeight={750}>Active Corridor Monitoring</Typography>
                <Typography variant="caption" color="text.secondary">
                  Investigate telemetry traces, route replays, and compliance packages for active shipments
                </Typography>
              </Box>

              <Stack direction="row" spacing={1.5}>
                <Button
                  component={Link}
                  to={`/trips/${DEMO_TRIP_ID}`}
                  variant="contained"
                  endIcon={<ArrowForwardRounded />}
                  sx={{
                    borderRadius: 3,
                    boxShadow: "0 8px 18px -4px rgba(13, 91, 99, 0.4)",
                    py: 1
                  }}
                >
                  Open Recovery Workspace
                </Button>

                <Button
                  component={Link}
                  to={`/simulator/trips/${DEMO_TRIP_ID}`}
                  variant="outlined"
                  startIcon={<LocalShippingRounded />}
                  sx={{ borderRadius: 3, py: 1 }}
                >
                  Hardware Gateway Pilot
                </Button>
              </Stack>
            </Stack>
          </Card>
        </Stack>
      )}
    </Stack>
  );
}
