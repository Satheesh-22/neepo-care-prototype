import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { NetworkState } from "@contracts";

export interface TelemetryGaugesProps {
  engineTemperature: number;
  cargoTemperature: number;
  speed: number;
  networkState: NetworkState;
  observationFreshness?: "FRESH" | "STALE" | "EXPIRED";
  safelyParked?: boolean;
}

export function TelemetryGauges({
  engineTemperature,
  cargoTemperature,
  speed,
  networkState,
  observationFreshness = "FRESH",
  safelyParked = false
}: TelemetryGaugesProps) {
  // Engine Temperature calculations (Normal <95, Warning 95-105, Critical >105)
  const isEngineCritical = engineTemperature > 105;
  const isEngineWarning = engineTemperature >= 95 && engineTemperature <= 105;
  const engineColor = isEngineCritical ? "#ef4444" : isEngineWarning ? "#f59e0b" : "#10b981";
  const engineStatusText = isEngineCritical
    ? "OVERHEATING (>105°C)"
    : isEngineWarning
      ? "Warning (95–105°C)"
      : "Nominal (<95°C)";

  // Cargo Temperature calculations (Safe band: 2°C – 8°C)
  const isCargoSafe = cargoTemperature >= 2 && cargoTemperature <= 8;
  const cargoColor = isCargoSafe ? "#10b981" : "#ef4444";
  const cargoStatusText = isCargoSafe ? "Cold-Chain Compliant (2°C – 8°C)" : "Temperature Violation!";

  // Adaptive policy interval description
  const policyInterval = isEngineCritical && !safelyParked ? "3s" : safelyParked ? "120s" : "30s";
  const policyReason = isEngineCritical && !safelyParked
    ? "BREAKDOWN / EMERGENCY"
    : safelyParked
      ? "SAFELY PARKED"
      : "IN TRANSIT NOMINAL";

  return (
    <Stack spacing={2.5}>
      {/* Visual Telemetry Cards */}
      <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
        {/* Engine Temperature Gauge Card */}
        <Card
          sx={{
            flex: 1,
            border: "1.5px solid",
            borderColor: isEngineCritical ? "#fca5a5" : isEngineWarning ? "#fde68a" : "#e2e8f0",
            bgcolor: isEngineCritical ? "rgba(254, 242, 242, 0.4)" : "#ffffff"
          }}
        >
          <CardContent sx={{ p: 2.5, "&:last-child": { pb: 2.5 } }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.05em">
                Engine Temperature
              </Typography>
              <Chip
                size="small"
                label={engineStatusText}
                sx={{
                  bgcolor: isEngineCritical ? "#fee2e2" : isEngineWarning ? "#fef3c7" : "#ecfdf5",
                  color: isEngineCritical ? "#991b1b" : isEngineWarning ? "#92400e" : "#065f46",
                  fontWeight: 800,
                  fontSize: "0.72rem",
                  border: `1px solid ${isEngineCritical ? "#fca5a5" : isEngineWarning ? "#fde68a" : "#a7f3d0"}`
                }}
              />
            </Stack>

            <Stack direction="row" alignItems="baseline" spacing={1} mt={1}>
              <Typography
                variant="h2"
                className="mono-metric"
                sx={{ fontWeight: 800, color: engineColor, fontSize: "2.2rem" }}
              >
                {engineTemperature.toFixed(1)}
              </Typography>
              <Typography variant="h4" color="text.secondary">
                °C
              </Typography>
            </Stack>

            {/* Threshold Bar Scale */}
            <Box sx={{ mt: 2, position: "relative" }}>
              <Box
                sx={{
                  height: 10,
                  borderRadius: 5,
                  bgcolor: "#f1f5f9",
                  overflow: "hidden",
                  display: "flex"
                }}
              >
                <Box sx={{ width: "50%", bgcolor: "#10b981", opacity: 0.8 }} title="Safe: 60-95°C" />
                <Box sx={{ width: "16%", bgcolor: "#f59e0b", opacity: 0.8 }} title="Warning: 95-105°C" />
                <Box sx={{ width: "34%", bgcolor: "#ef4444", opacity: 0.8 }} title="Critical: >105°C" />
              </Box>

              {/* Marker pin */}
              <Box
                sx={{
                  position: "absolute",
                  top: -3,
                  left: `${Math.min(Math.max(((engineTemperature - 60) / 70) * 100, 2), 98)}%`,
                  transform: "translateX(-50%)",
                  width: 16,
                  height: 16,
                  borderRadius: "50%",
                  bgcolor: engineColor,
                  border: "2.5px solid #ffffff",
                  boxShadow: "0 2px 5px rgba(0,0,0,0.3)",
                  transition: "left 0.2s ease-out"
                }}
              />

              <Stack direction="row" justifyContent="space-between" mt={0.6}>
                <Typography variant="caption" color="text.secondary" fontSize="0.7rem">60°C</Typography>
                <Typography variant="caption" color="warning.main" fontWeight={700} fontSize="0.7rem">95°C</Typography>
                <Typography variant="caption" color="error.main" fontWeight={800} fontSize="0.7rem">Threshold 105°C</Typography>
                <Typography variant="caption" color="text.secondary" fontSize="0.7rem">130°C</Typography>
              </Stack>
            </Box>
          </CardContent>
        </Card>

        {/* Cargo Temperature (Cold-Chain) Gauge Card */}
        <Card
          sx={{
            flex: 1,
            border: "1.5px solid",
            borderColor: isCargoSafe ? "#e2e8f0" : "#fca5a5",
            bgcolor: isCargoSafe ? "#ffffff" : "rgba(254, 242, 242, 0.4)"
          }}
        >
          <CardContent sx={{ p: 2.5, "&:last-child": { pb: 2.5 } }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.05em">
                Cargo Chamber (Cold-Chain)
              </Typography>
              <Chip
                size="small"
                label={cargoStatusText}
                sx={{
                  bgcolor: isCargoSafe ? "#ecfdf5" : "#fee2e2",
                  color: isCargoSafe ? "#065f46" : "#991b1b",
                  fontWeight: 800,
                  fontSize: "0.72rem",
                  border: `1px solid ${isCargoSafe ? "#a7f3d0" : "#fca5a5"}`
                }}
              />
            </Stack>

            <Stack direction="row" alignItems="baseline" spacing={1} mt={1}>
              <Typography
                variant="h2"
                className="mono-metric"
                sx={{ fontWeight: 800, color: cargoColor, fontSize: "2.2rem" }}
              >
                {cargoTemperature.toFixed(1)}
              </Typography>
              <Typography variant="h4" color="text.secondary">
                °C
              </Typography>
            </Stack>

            {/* Cargo Range Bar */}
            <Box sx={{ mt: 2, position: "relative" }}>
              <Box
                sx={{
                  height: 10,
                  borderRadius: 5,
                  bgcolor: "#f1f5f9",
                  overflow: "hidden",
                  display: "flex"
                }}
              >
                <Box sx={{ width: "25%", bgcolor: "#ef4444", opacity: 0.6 }} title="Too Cold (<2°C)" />
                <Box sx={{ width: "45%", bgcolor: "#10b981", opacity: 0.9 }} title="Safe Range: 2°C - 8°C" />
                <Box sx={{ width: "30%", bgcolor: "#ef4444", opacity: 0.6 }} title="Too Warm (>8°C)" />
              </Box>

              {/* Marker pin */}
              <Box
                sx={{
                  position: "absolute",
                  top: -3,
                  left: `${Math.min(Math.max(((cargoTemperature - (-2)) / 16) * 100, 2), 98)}%`,
                  transform: "translateX(-50%)",
                  width: 16,
                  height: 16,
                  borderRadius: "50%",
                  bgcolor: cargoColor,
                  border: "2.5px solid #ffffff",
                  boxShadow: "0 2px 5px rgba(0,0,0,0.3)",
                  transition: "left 0.2s ease-out"
                }}
              />

              <Stack direction="row" justifyContent="space-between" mt={0.6}>
                <Typography variant="caption" color="text.secondary" fontSize="0.7rem">-2°C</Typography>
                <Typography variant="caption" color="success.main" fontWeight={800} fontSize="0.7rem">Safe 2°C – 8°C</Typography>
                <Typography variant="caption" color="text.secondary" fontSize="0.7rem">14°C</Typography>
              </Stack>
            </Box>
          </CardContent>
        </Card>

        {/* Speedometer & Policy Card */}
        <Card sx={{ flex: 0.9, border: "1px solid #e2e8f0" }}>
          <CardContent sx={{ p: 2.5, "&:last-child": { pb: 2.5 } }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography variant="caption" color="text.secondary" fontWeight={700} textTransform="uppercase" letterSpacing="0.05em">
                Speed & Reporting Policy
              </Typography>
              <Chip
                size="small"
                label={networkState}
                sx={{
                  bgcolor: networkState === "ONLINE" ? "#ecfdf5" : "#fffbeb",
                  color: networkState === "ONLINE" ? "#047857" : "#b45309",
                  fontWeight: 800,
                  fontSize: "0.72rem"
                }}
              />
            </Stack>

            <Stack direction="row" alignItems="baseline" spacing={1} mt={1}>
              <Typography variant="h2" className="mono-metric" sx={{ fontWeight: 800, color: "#0f172a", fontSize: "2.2rem" }}>
                {speed}
              </Typography>
              <Typography variant="h4" color="text.secondary">
                km/h
              </Typography>
            </Stack>

            <Box sx={{ mt: 1.5, p: 1.2, borderRadius: 2, bgcolor: "#f8fafc", border: "1px solid #e2e8f0" }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="caption" color="text.secondary" fontWeight={700}>
                  Policy Interval:
                </Typography>
                <Typography variant="caption" fontWeight={800} color="#0d5b63">
                  {policyInterval} ({policyReason})
                </Typography>
              </Stack>
              <Stack direction="row" justifyContent="space-between" alignItems="center" mt={0.5}>
                <Typography variant="caption" color="text.secondary" fontWeight={700}>
                  Freshness:
                </Typography>
                <Typography variant="caption" fontWeight={800} color="#047857">
                  {observationFreshness}
                </Typography>
              </Stack>
            </Box>
          </CardContent>
        </Card>
      </Stack>
    </Stack>
  );
}
