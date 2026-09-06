import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, Tooltip } from "react-leaflet";

const route: [number, number][] = [
  [12.9716, 77.5946], // Origin: Bengaluru Depot
  [13.0358, 77.597],  // Current: Vehicle Stopped (Overheating)
  [13.0512, 77.592],  // Safe Stop / Provider: Sri Krishna Safe Hub
  [13.0827, 77.5892]  // Destination: North Hub
];

export function TripMap() {
  return (
    <Box
      aria-label="Trip route map"
      sx={{
        borderRadius: 4,
        overflow: "hidden",
        border: "1px solid",
        borderColor: "divider",
        bgcolor: "#ffffff",
        boxShadow: "0 4px 20px -2px rgba(15, 23, 42, 0.06)"
      }}
    >
      <Box sx={{ p: 2, borderBottom: "1px solid", borderColor: "divider", bgcolor: "#f8fafc" }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Typography variant="caption" fontWeight={700} color="text.secondary" textTransform="uppercase" letterSpacing="0.05em">
            Live Route & Incident Map
          </Typography>
          <Stack direction="row" spacing={1}>
            <Chip size="small" label="Depot" sx={{ height: 20, fontSize: "0.68rem", bgcolor: "#e2e8f0" }} />
            <Chip size="small" label="Incident" sx={{ height: 20, fontSize: "0.68rem", bgcolor: "#fee2e2", color: "#b91c1c", fontWeight: 700 }} />
            <Chip size="small" label="Safe Stop" sx={{ height: 20, fontSize: "0.68rem", bgcolor: "#dcfce7", color: "#15803d", fontWeight: 700 }} />
          </Stack>
        </Stack>
      </Box>

      <Box sx={{ height: 340, width: "100%", position: "relative" }}>
        <MapContainer center={route[1]} zoom={11} scrollWheelZoom={false} style={{ width: "100%", height: "100%" }}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          {/* Route path outline */}
          <Polyline positions={route} pathOptions={{ color: "#071b2a", weight: 6, opacity: 0.3 }} />
          {/* Route path primary */}
          <Polyline positions={route} pathOptions={{ color: "#0d5b63", weight: 4, opacity: 0.9 }} />

          {/* Origin: Depot */}
          <CircleMarker center={route[0]} radius={7} pathOptions={{ color: "#ffffff", fillColor: "#0284c7", fillOpacity: 1, weight: 2.5 }}>
            <Tooltip direction="bottom">Depot Origin</Tooltip>
          </CircleMarker>

          {/* Stopped Vehicle (Incident) */}
          <CircleMarker center={route[1]} radius={10} pathOptions={{ color: "#ffffff", fillColor: "#ef4444", fillOpacity: 1, weight: 3 }}>
            <Tooltip permanent direction="top" offset={[0, -8]}>
              🚨 Stopped (106°C)
            </Tooltip>
            <Popup>
              <Box p={0.5}>
                <Typography fontWeight={700} fontSize="0.85rem">Vehicle KA-01-EQ-4040</Typography>
                <Typography variant="caption" color="text.secondary">Status: Stopped due to Overheating</Typography>
              </Box>
            </Popup>
          </CircleMarker>

          {/* Safe Stop Location */}
          <CircleMarker center={route[2]} radius={8} pathOptions={{ color: "#ffffff", fillColor: "#10b981", fillOpacity: 1, weight: 2.5 }}>
            <Tooltip direction="right" offset={[8, 0]}>
              🅿️ Sri Krishna Safe Hub
            </Tooltip>
          </CircleMarker>

          {/* Destination */}
          <CircleMarker center={route[3]} radius={7} pathOptions={{ color: "#ffffff", fillColor: "#0d5b63", fillOpacity: 1, weight: 2.5 }}>
            <Tooltip direction="top">North Hub Destination</Tooltip>
          </CircleMarker>
        </MapContainer>
      </Box>
    </Box>
  );
}

