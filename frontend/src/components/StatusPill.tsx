import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import type { TripStatus } from "@contracts";

interface StatusConfig {
  label: string;
  bg: string;
  color: string;
  border: string;
  dotColor: string;
}

const statusConfigs: Record<TripStatus, StatusConfig> = {
  CREATED: {
    label: "Created",
    bg: "#f1f5f9",
    color: "#475569",
    border: "#cbd5e1",
    dotColor: "#94a3b8"
  },
  ASSIGNED: {
    label: "Assigned",
    bg: "#f0f9ff",
    color: "#0369a1",
    border: "#bae6fd",
    dotColor: "#0284c7"
  },
  IN_PROGRESS: {
    label: "In Transit",
    bg: "#ecfdf5",
    color: "#047857",
    border: "#a7f3d0",
    dotColor: "#10b981"
  },
  DISRUPTED: {
    label: "Disrupted",
    bg: "#fef2f2",
    color: "#b91c1c",
    border: "#fecaca",
    dotColor: "#ef4444"
  },
  RECOVERY_IN_PROGRESS: {
    label: "Recovery Active",
    bg: "#fffbeb",
    color: "#b45309",
    border: "#fde68a",
    dotColor: "#f59e0b"
  },
  COMPLETED: {
    label: "Completed",
    bg: "#ecfdf5",
    color: "#047857",
    border: "#a7f3d0",
    dotColor: "#10b981"
  },
  CANCELLED: {
    label: "Cancelled",
    bg: "#f8fafc",
    color: "#64748b",
    border: "#e2e8f0",
    dotColor: "#94a3b8"
  }
};

export function StatusPill({ status }: { status: TripStatus }) {
  const config = statusConfigs[status] ?? {
    label: status.replaceAll("_", " "),
    bg: "#f1f5f9",
    color: "#475569",
    border: "#cbd5e1",
    dotColor: "#94a3b8"
  };

  return (
    <Chip
      size="small"
      icon={
        <Box
          component="span"
          sx={{
            width: 7,
            height: 7,
            borderRadius: "50%",
            bgcolor: config.dotColor,
            ml: 0.5,
            mr: -0.5
          }}
        />
      }
      label={config.label}
      sx={{
        backgroundColor: config.bg,
        color: config.color,
        border: `1px solid ${config.border}`,
        fontWeight: 700,
        fontSize: "0.75rem",
        height: 24,
        "& .MuiChip-label": {
          px: 1
        }
      }}
    />
  );
}

