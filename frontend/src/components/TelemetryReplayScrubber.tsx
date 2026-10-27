import FastForwardRounded from "@mui/icons-material/FastForwardRounded";
import FastRewindRounded from "@mui/icons-material/FastRewindRounded";
import PauseRounded from "@mui/icons-material/PauseRounded";
import PlayArrowRounded from "@mui/icons-material/PlayArrowRounded";
import ReplayRounded from "@mui/icons-material/ReplayRounded";
import SpeedRounded from "@mui/icons-material/SpeedRounded";
import ThermostatRounded from "@mui/icons-material/ThermostatRounded";
import WarningAmberRounded from "@mui/icons-material/WarningAmberRounded";
import AcUnitRounded from "@mui/icons-material/AcUnitRounded";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import ButtonGroup from "@mui/material/ButtonGroup";
import Chip from "@mui/material/Chip";
import IconButton from "@mui/material/IconButton";
import Slider from "@mui/material/Slider";
import Stack from "@mui/material/Stack";
import Tooltip from "@mui/material/Tooltip";
import Typography from "@mui/material/Typography";
import { useEffect, useRef, useState } from "react";
import type { TelemetryHistoryPoint } from "../api/types";

interface TelemetryReplayScrubberProps {
  points: readonly TelemetryHistoryPoint[];
  currentIndex: number;
  onIndexChange: (index: number) => void;
  isLoading?: boolean;
}

export function TelemetryReplayScrubber({
  points,
  currentIndex,
  onIndexChange,
  isLoading = false
}: TelemetryReplayScrubberProps) {
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [playbackSpeed, setPlaybackSpeed] = useState<1 | 2 | 5>(1);

  const totalPoints = points.length;
  const currentPoint = points[currentIndex] ?? null;
  const currentIndexRef = useRef(currentIndex);
  useEffect(() => {
    currentIndexRef.current = currentIndex;
  }, [currentIndex]);

  // Auto-play interval
  useEffect(() => {
    if (!isPlaying || totalPoints <= 1) return;

    const intervalMs = Math.round(1000 / playbackSpeed);
    const timer = setInterval(() => {
      if (currentIndexRef.current >= totalPoints - 1) {
        setIsPlaying(false);
      } else {
        onIndexChange(currentIndexRef.current + 1);
      }
    }, intervalMs);

    return () => clearInterval(timer);
  }, [isPlaying, playbackSpeed, totalPoints, onIndexChange]);

  const handlePlayToggle = () => {
    if (currentIndex >= totalPoints - 1) {
      onIndexChange(0);
      setIsPlaying(true);
    } else {
      setIsPlaying((prev) => !prev);
    }
  };

  const handleStepBack = () => {
    setIsPlaying(false);
    onIndexChange(Math.max(0, currentIndex - 1));
  };

  const handleStepForward = () => {
    setIsPlaying(false);
    onIndexChange(Math.min(totalPoints - 1, currentIndex + 1));
  };

  const handleReset = () => {
    setIsPlaying(false);
    onIndexChange(0);
  };

  if (totalPoints === 0 && !isLoading) {
    return (
      <Box sx={{ p: 2, bgcolor: "#f8fafc", borderRadius: 3, border: "1px dashed #cbd5e1", textAlign: "center" }}>
        <Typography variant="body2" color="text.secondary">
          No historical telemetry breadcrumbs available for this trip.
        </Typography>
      </Box>
    );
  }

  const isOverheating = (currentPoint?.engineTemperatureCelsius ?? 0) > 100;
  const isColdChainOk =
    (currentPoint?.cargoTemperatureCelsius ?? 4) >= 2 &&
    (currentPoint?.cargoTemperatureCelsius ?? 4) <= 8;

  const formatTime = (isoString?: string) => {
    if (!isoString) return "--:--:--";
    try {
      const d = new Date(isoString);
      return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false });
    } catch {
      return isoString;
    }
  };

  return (
    <Box
      aria-label="Telemetry historical route replay scrubber"
      sx={{
        p: 2.5,
        borderRadius: 3.5,
        border: "1px solid #e2e8f0",
        bgcolor: "#ffffff",
        boxShadow: "0 4px 16px -2px rgba(15, 23, 42, 0.05)"
      }}
    >
      {/* Header & Status Indicator */}
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "flex-start", sm: "center" }} spacing={1.5} mb={2}>
        <Stack direction="row" spacing={1.2} alignItems="center">
          <Typography variant="subtitle2" fontWeight={800} color="#0f172a" textTransform="uppercase" letterSpacing="0.05em">
            Telemetry Replay Scrubber
          </Typography>
          <Chip
            size="small"
            label={`${totalPoints} Observations`}
            sx={{ bgcolor: "#f1f5f9", fontWeight: 700, fontSize: "0.7rem" }}
          />
        </Stack>

        {currentPoint && (
          <Stack direction="row" spacing={1} flexWrap="wrap">
            {isOverheating ? (
              <Chip
                icon={<WarningAmberRounded sx={{ "&&": { color: "#b91c1c" } }} />}
                label={`Engine Anomaly: ${currentPoint.engineTemperatureCelsius?.toFixed(1)}°C`}
                size="small"
                sx={{ bgcolor: "#fee2e2", color: "#991b1b", fontWeight: 800, fontSize: "0.75rem" }}
              />
            ) : (
              <Chip
                icon={<ThermostatRounded sx={{ "&&": { color: "#0d5b63" } }} />}
                label={`Engine: ${currentPoint.engineTemperatureCelsius?.toFixed(1)}°C`}
                size="small"
                sx={{ bgcolor: "#e6fffa", color: "#0d5b63", fontWeight: 700, fontSize: "0.75rem" }}
              />
            )}

            <Chip
              icon={<AcUnitRounded sx={{ "&&": { color: isColdChainOk ? "#0284c7" : "#b91c1c" } }} />}
              label={`Cargo: ${currentPoint.cargoTemperatureCelsius?.toFixed(1)}°C (${isColdChainOk ? "Compliant" : "Breach"})`}
              size="small"
              sx={{
                bgcolor: isColdChainOk ? "#f0f9ff" : "#fee2e2",
                color: isColdChainOk ? "#0369a1" : "#991b1b",
                fontWeight: 700,
                fontSize: "0.75rem"
              }}
            />

            <Chip
              icon={<SpeedRounded sx={{ "&&": { color: "#475569" } }} />}
              label={`${currentPoint.speedKph?.toFixed(0) ?? 0} km/h`}
              size="small"
              sx={{ bgcolor: "#f8fafc", color: "#334155", fontWeight: 700, fontSize: "0.75rem" }}
            />
          </Stack>
        )}
      </Stack>

      {/* Scrubber Timeline Slider */}
      <Box sx={{ px: 1, my: 1.5 }}>
        <Slider
          value={currentIndex}
          min={0}
          max={Math.max(0, totalPoints - 1)}
          step={1}
          onChange={(_, val) => {
            setIsPlaying(false);
            onIndexChange(val as number);
          }}
          valueLabelDisplay="auto"
          valueLabelFormat={(index) => formatTime(points[index]?.recordedAt)}
          aria-label="Trip telemetry timeline scrubber"
          sx={{
            color: isOverheating ? "#dc2626" : "#0d5b63",
            height: 6,
            "& .MuiSlider-thumb": {
              width: 18,
              height: 18,
              transition: "0.15s cubic-bezier(.4, 0, .2, 1)",
              "&:hover, &.Mui-focusVisible": {
                boxShadow: isOverheating ? "0 0 0 8px rgba(220, 38, 38, 0.16)" : "0 0 0 8px rgba(13, 91, 99, 0.16)"
              }
            },
            "& .MuiSlider-rail": {
              bgcolor: "#cbd5e1",
              opacity: 1
            }
          }}
        />
      </Box>

      {/* Control Bar & Playback Speed Buttons */}
      <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems="center" spacing={2} pt={0.5}>
        <Stack direction="row" spacing={1} alignItems="center">
          <Tooltip title="Reset to Trip Start">
            <span>
              <IconButton size="small" onClick={handleReset} disabled={currentIndex === 0} aria-label="Reset to start">
                <ReplayRounded fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>

          <Tooltip title="Step Backward">
            <span>
              <IconButton size="small" onClick={handleStepBack} disabled={currentIndex === 0} aria-label="Step backward">
                <FastRewindRounded fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>

          <Button
            variant="contained"
            size="small"
            onClick={handlePlayToggle}
            startIcon={isPlaying ? <PauseRounded /> : <PlayArrowRounded />}
            aria-label={isPlaying ? "Pause replay" : "Play replay"}
            sx={{
              minWidth: 95,
              borderRadius: 2.5,
              bgcolor: "#0d5b63",
              fontWeight: 700,
              textTransform: "none",
              "&:hover": { bgcolor: "#0a454b" }
            }}
          >
            {isPlaying ? "Pause" : currentIndex >= totalPoints - 1 ? "Replay" : "Play"}
          </Button>

          <Tooltip title="Step Forward">
            <span>
              <IconButton size="small" onClick={handleStepForward} disabled={currentIndex >= totalPoints - 1} aria-label="Step forward">
                <FastForwardRounded fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>

          <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ ml: 1 }}>
            {formatTime(currentPoint?.recordedAt)} ({currentIndex + 1} of {totalPoints})
          </Typography>
        </Stack>

        {/* Speed Multiplier Controls */}
        <Stack direction="row" spacing={1} alignItems="center">
          <Typography variant="caption" color="text.secondary" fontWeight={700}>
            Speed:
          </Typography>
          <ButtonGroup size="small" variant="outlined" aria-label="Playback speed selector">
            {([1, 2, 5] as const).map((speed) => (
              <Button
                key={speed}
                onClick={() => setPlaybackSpeed(speed)}
                aria-pressed={playbackSpeed === speed}
                sx={{
                  px: 1.5,
                  py: 0.25,
                  fontSize: "0.75rem",
                  fontWeight: 700,
                  bgcolor: playbackSpeed === speed ? "#0d5b63" : "transparent",
                  color: playbackSpeed === speed ? "#ffffff" : "#475569",
                  borderColor: "#cbd5e1",
                  "&:hover": {
                    bgcolor: playbackSpeed === speed ? "#0a454b" : "#f1f5f9",
                    borderColor: "#94a3b8"
                  }
                }}
              >
                {speed}×
              </Button>
            ))}
          </ButtonGroup>
        </Stack>
      </Stack>
    </Box>
  );
}
