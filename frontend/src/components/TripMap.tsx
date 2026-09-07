import GpsFixedRounded from "@mui/icons-material/GpsFixedRounded";
import LayersRounded from "@mui/icons-material/LayersRounded";
import LocalShippingRounded from "@mui/icons-material/LocalShippingRounded";
import NavigationRounded from "@mui/icons-material/NavigationRounded";
import WarningAmberRounded from "@mui/icons-material/WarningAmberRounded";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { useState } from "react";
import { CircleMarker, MapContainer, Polyline, Popup, TileLayer, Tooltip, useMap } from "react-leaflet";

const originCoords: [number, number] = [12.9716, 77.5946]; // Origin: Bengaluru Depot
const vehicleCoords: [number, number] = [13.0358, 77.597]; // Current: Stopped (Overheating Incident)
const mechanicCoords: [number, number] = [13.045, 77.595]; // Mobile Mechanic: Sri Balaji Auto Assist
const safeStopCoords: [number, number] = [13.0512, 77.592]; // Safe Stop: Sri Krishna Safe Hub
const destinationCoords: [number, number] = [13.0827, 77.5892]; // Destination: North Hub

const nominalRoute: [number, number][] = [
  originCoords,
  vehicleCoords,
  [13.06, 77.594],
  destinationCoords
];

const recoveryDetour: [number, number][] = [
  vehicleCoords,
  safeStopCoords
];

function MapController({ center, zoom }: { center: [number, number]; zoom: number }) {
  const map = useMap();
  map.setView(center, zoom, { animate: true });
  return null;
}

export function TripMap() {
  const [mapCenter, setMapCenter] = useState<[number, number]>(vehicleCoords);
  const [mapZoom, setMapZoom] = useState<number>(12);
  const [viewMode, setViewMode] = useState<"VEHICLE" | "FULL_ROUTE">("VEHICLE");

  const handleCenterVehicle = () => {
    setMapCenter(vehicleCoords);
    setMapZoom(13);
    setViewMode("VEHICLE");
  };

  const handleFullRoute = () => {
    setMapCenter([13.027, 77.593]);
    setMapZoom(11);
    setViewMode("FULL_ROUTE");
  };

  return (
    <Box
      aria-label="Trip route map"
      sx={{
        borderRadius: 4,
        overflow: "hidden",
        border: "1px solid #e2e8f0",
        bgcolor: "#ffffff",
        boxShadow: "0 4px 20px -2px rgba(15, 23, 42, 0.06)"
      }}
    >
      {/* Map Header & Legend */}
      <Box sx={{ p: 2, borderBottom: "1px solid #e2e8f0", bgcolor: "#f8fafc" }}>
        <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "flex-start", sm: "center" }} spacing={1.5}>
          <Stack direction="row" spacing={1.2} alignItems="center">
            <NavigationRounded sx={{ color: "#0d5b63", fontSize: 20 }} />
            <Box>
              <Typography variant="caption" fontWeight={800} color="#0f172a" textTransform="uppercase" letterSpacing="0.05em">
                Live Route & Incident Map
              </Typography>
              <Typography variant="caption" color="text.secondary" display="block" fontSize="0.7rem">
                Real-time PostGIS Geo-Corridor · KA-01-EQ-4040
              </Typography>
            </Box>
          </Stack>

          {/* Legend Items */}
          <Stack direction="row" spacing={0.8} flexWrap="wrap">
            <Chip
              size="small"
              label="Depot Origin"
              sx={{ height: 22, fontSize: "0.68rem", bgcolor: "#e0f2fe", color: "#0369a1", fontWeight: 700 }}
            />
            <Chip
              size="small"
              label="🚨 Incident (106°C)"
              sx={{ height: 22, fontSize: "0.68rem", bgcolor: "#fee2e2", color: "#b91c1c", fontWeight: 800 }}
            />
            <Chip
              size="small"
              label="🅿️ Safe Stop Hub"
              sx={{ height: 22, fontSize: "0.68rem", bgcolor: "#ecfdf5", color: "#047857", fontWeight: 700 }}
            />
            <Chip
              size="small"
              label="🔧 Mechanic Partner"
              sx={{ height: 22, fontSize: "0.68rem", bgcolor: "#fef3c7", color: "#b45309", fontWeight: 700 }}
            />
          </Stack>
        </Stack>
      </Box>

      {/* Map Canvas & Overlay Controls */}
      <Box sx={{ height: { xs: 300, sm: 380 }, width: "100%", position: "relative" }}>
        <MapContainer
          center={mapCenter}
          zoom={mapZoom}
          scrollWheelZoom={false}
          style={{ width: "100%", height: "100%" }}
        >
          <MapController center={mapCenter} zoom={mapZoom} />

          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          {/* Nominal Route Track */}
          <Polyline positions={nominalRoute} pathOptions={{ color: "#071b2a", weight: 6, opacity: 0.25 }} />
          <Polyline positions={nominalRoute} pathOptions={{ color: "#0d5b63", weight: 4, opacity: 0.9 }} />

          {/* CARE Recovery Detour Route */}
          <Polyline
            positions={recoveryDetour}
            pathOptions={{ color: "#10b981", weight: 4, dashArray: "6, 8", opacity: 0.95 }}
          />

          {/* Origin: Depot Marker */}
          <CircleMarker
            center={originCoords}
            radius={8}
            pathOptions={{ color: "#ffffff", fillColor: "#0284c7", fillOpacity: 1, weight: 2.5 }}
          >
            <Tooltip direction="bottom">Depot Origin (Bengaluru)</Tooltip>
            <Popup>
              <Box p={0.5}>
                <Typography fontWeight={700} fontSize="0.85rem">Bengaluru Central Depot</Typography>
                <Typography variant="caption" color="text.secondary">Origin: Dispatched at 11:00 UTC</Typography>
              </Box>
            </Popup>
          </CircleMarker>

          {/* Incident: Stopped Overheating Vehicle */}
          <CircleMarker
            center={vehicleCoords}
            radius={11}
            pathOptions={{ color: "#ffffff", fillColor: "#ef4444", fillOpacity: 1, weight: 3 }}
          >
            <Tooltip permanent direction="top" offset={[0, -10]}>
              🚨 Stopped (106°C) · KA-01-EQ-4040
            </Tooltip>
            <Popup>
              <Box p={0.5}>
                <Stack direction="row" spacing={1} alignItems="center" mb={0.5}>
                  <WarningAmberRounded sx={{ color: "#ef4444", fontSize: 18 }} />
                  <Typography fontWeight={800} fontSize="0.9rem">Vehicle KA-01-EQ-4040</Typography>
                </Stack>
                <Typography variant="caption" color="error.main" fontWeight={700} display="block">
                  Engine Temperature: 106°C (Threshold Exceeded)
                </Typography>
                <Typography variant="caption" color="text.secondary" display="block">
                  Speed: 0 km/h · Heading: North-East (42°)
                </Typography>
                <Typography variant="caption" color="text.secondary" display="block">
                  Status: Disrupted · Recovery Action Pending
                </Typography>
              </Box>
            </Popup>
          </CircleMarker>

          {/* Assigned Mobile Mechanic Marker */}
          <CircleMarker
            center={mechanicCoords}
            radius={8}
            pathOptions={{ color: "#ffffff", fillColor: "#f59e0b", fillOpacity: 1, weight: 2.5 }}
          >
            <Tooltip direction="right" offset={[8, 0]}>
              🔧 Sri Balaji Mobile Assist
            </Tooltip>
            <Popup>
              <Box p={0.5}>
                <Typography fontWeight={700} fontSize="0.85rem">Sri Balaji Auto Assist</Typography>
                <Typography variant="caption" color="text.secondary">Confirmed Mobile Mechanic · ETA 18 mins</Typography>
              </Box>
            </Popup>
          </CircleMarker>

          {/* Safe Stop Location Marker */}
          <CircleMarker
            center={safeStopCoords}
            radius={9}
            pathOptions={{ color: "#ffffff", fillColor: "#10b981", fillOpacity: 1, weight: 2.5 }}
          >
            <Tooltip direction="right" offset={[8, 0]}>
              🅿️ Sri Krishna Safe Hub (1.8 km)
            </Tooltip>
            <Popup>
              <Box p={0.5}>
                <Typography fontWeight={700} fontSize="0.85rem">Sri Krishna Safe Hub</Typography>
                <Typography variant="caption" color="text.secondary">
                  Designated Safe Parking Area with Cold-Chain Facility
                </Typography>
              </Box>
            </Popup>
          </CircleMarker>

          {/* Destination: North Hub */}
          <CircleMarker
            center={destinationCoords}
            radius={8}
            pathOptions={{ color: "#ffffff", fillColor: "#0d5b63", fillOpacity: 1, weight: 2.5 }}
          >
            <Tooltip direction="top">North Hub Destination</Tooltip>
            <Popup>
              <Box p={0.5}>
                <Typography fontWeight={700} fontSize="0.85rem">North Medical Logistics Hub</Typography>
                <Typography variant="caption" color="text.secondary">Delivery Deadline: 18:30 UTC</Typography>
              </Box>
            </Popup>
          </CircleMarker>
        </MapContainer>

        {/* Floating Map Actions Bar */}
        <Box
          sx={{
            position: "absolute",
            bottom: 14,
            right: 14,
            zIndex: 400,
            display: "flex",
            gap: 1
          }}
        >
          <Button
            size="small"
            variant="contained"
            startIcon={<GpsFixedRounded sx={{ fontSize: 16 }} />}
            onClick={handleCenterVehicle}
            sx={{
              bgcolor: viewMode === "VEHICLE" ? "#071b2a" : "#ffffff",
              color: viewMode === "VEHICLE" ? "#ffffff" : "#0f172a",
              "&:hover": { bgcolor: viewMode === "VEHICLE" ? "#0f2e46" : "#f1f5f9" },
              boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
              fontSize: "0.75rem",
              borderRadius: 2
            }}
          >
            Vehicle Focus
          </Button>

          <Button
            size="small"
            variant="contained"
            startIcon={<LayersRounded sx={{ fontSize: 16 }} />}
            onClick={handleFullRoute}
            sx={{
              bgcolor: viewMode === "FULL_ROUTE" ? "#071b2a" : "#ffffff",
              color: viewMode === "FULL_ROUTE" ? "#ffffff" : "#0f172a",
              "&:hover": { bgcolor: viewMode === "FULL_ROUTE" ? "#0f2e46" : "#f1f5f9" },
              boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
              fontSize: "0.75rem",
              borderRadius: 2
            }}
          >
            Full Route
          </Button>
        </Box>

        {/* Selected-Trip Floating Summary Chip */}
        <Box
          sx={{
            position: "absolute",
            top: 14,
            left: 14,
            zIndex: 400,
            bgcolor: "rgba(255, 255, 255, 0.95)",
            backdropFilter: "blur(8px)",
            p: 1.2,
            borderRadius: 2.5,
            border: "1px solid rgba(226, 232, 240, 0.9)",
            boxShadow: "0 4px 14px rgba(15, 23, 42, 0.08)",
            display: { xs: "none", sm: "block" }
          }}
        >
          <Stack direction="row" spacing={1.5} alignItems="center">
            <LocalShippingRounded sx={{ color: "#0d5b63", fontSize: 20 }} />
            <Box>
              <Typography variant="caption" fontWeight={800} color="#0f172a" display="block">
                KA-01-EQ-4040 · Stopped at KM 14.2
              </Typography>
              <Typography variant="caption" color="text.secondary" fontSize="0.7rem">
                Detour to Sri Krishna Safe Hub: 1.8 km (4 mins)
              </Typography>
            </Box>
          </Stack>
        </Box>
      </Box>
    </Box>
  );
}
