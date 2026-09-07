import AutoAwesomeRounded from "@mui/icons-material/AutoAwesomeRounded";
import ExpandLessRounded from "@mui/icons-material/ExpandLessRounded";
import ExpandMoreRounded from "@mui/icons-material/ExpandMoreRounded";
import PlayCircleOutlineRounded from "@mui/icons-material/PlayCircleOutlineRounded";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Collapse from "@mui/material/Collapse";
import Container from "@mui/material/Container";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { DEMO_TRIP_ID } from "../api/mockApi";

export function DemoModeBar() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  const scenarios = [
    {
      label: "1. Normal Transit",
      desc: "Nominal engine temp (85°C), cold-chain (4°C), in-transit",
      path: `/driver/trips/${DEMO_TRIP_ID}`
    },
    {
      label: "2. Overheat Trigger",
      desc: "Engine temp spikes to 106°C, disruption detected",
      path: `/simulator/trips/${DEMO_TRIP_ID}`
    },
    {
      label: "3. CARE Plan Selection",
      desc: "Evaluates alternatives with deterministic utility scoring",
      path: `/trips/${DEMO_TRIP_ID}`
    },
    {
      label: "4. Driver Safe Parking",
      desc: "Executes next safe action and policy slows to 120s",
      path: `/driver/trips/${DEMO_TRIP_ID}`
    }
  ];

  return (
    <Box
      sx={{
        bgcolor: "#03111b",
        borderBottom: "1px solid rgba(255, 255, 255, 0.08)",
        color: "white"
      }}
    >
      <Container maxWidth="xl">
        <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ py: 0.8 }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <AutoAwesomeRounded sx={{ fontSize: 16, color: "#34d399" }} />
            <Typography variant="caption" sx={{ fontWeight: 800, color: "#a7f3d0", letterSpacing: "0.03em" }}>
              DEMO PRESENTATION CONTROLLER
            </Typography>
            <Chip
              label="Interactive Guide"
              size="small"
              sx={{
                bgcolor: "rgba(16, 185, 129, 0.15)",
                color: "#34d399",
                fontSize: "0.65rem",
                height: 18,
                fontWeight: 700
              }}
            />
          </Stack>

          <Button
            size="small"
            onClick={() => setOpen(!open)}
            endIcon={open ? <ExpandLessRounded /> : <ExpandMoreRounded />}
            sx={{
              color: "rgba(255, 255, 255, 0.8)",
              fontSize: "0.72rem",
              py: 0.2,
              minHeight: 26,
              "&:hover": { bgcolor: "rgba(255, 255, 255, 0.08)" }
            }}
          >
            {open ? "Hide Scenarios" : "Explore Scenarios"}
          </Button>
        </Stack>

        <Collapse in={open}>
          <Box sx={{ pb: 1.5, pt: 0.5 }}>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} flexWrap="wrap">
              {scenarios.map((sc) => (
                <Box
                  key={sc.label}
                  onClick={() => navigate(sc.path)}
                  sx={{
                    flex: 1,
                    minWidth: { xs: "100%", sm: 200 },
                    p: 1.2,
                    borderRadius: 2,
                    bgcolor: "rgba(255, 255, 255, 0.05)",
                    border: "1px solid rgba(255, 255, 255, 0.1)",
                    cursor: "pointer",
                    transition: "all 0.15s ease",
                    "&:hover": {
                      bgcolor: "rgba(13, 91, 99, 0.3)",
                      borderColor: "rgba(52, 211, 153, 0.4)"
                    }
                  }}
                >
                  <Stack direction="row" spacing={1} alignItems="center">
                    <PlayCircleOutlineRounded sx={{ fontSize: 16, color: "#34d399" }} />
                    <Typography variant="caption" fontWeight={800} color="#ffffff">
                      {sc.label}
                    </Typography>
                  </Stack>
                  <Typography variant="caption" color="rgba(255, 255, 255, 0.65)" display="block" fontSize="0.7rem" mt={0.3}>
                    {sc.desc}
                  </Typography>
                </Box>
              ))}
            </Stack>
          </Box>
        </Collapse>
      </Container>
    </Box>
  );
}
