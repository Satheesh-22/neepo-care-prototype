import { ThemeProvider } from "@mui/material/styles";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { theme } from "../app/theme";
import type { TelemetryHistoryPoint } from "../api/types";
import { TelemetryReplayScrubber } from "./TelemetryReplayScrubber";

const mockPoints: TelemetryHistoryPoint[] = [
  {
    eventId: "00000000-0000-4000-8800-000000000001",
    recordedAt: "2026-08-24T12:45:00Z",
    latitude: 12.9716,
    longitude: 77.5946,
    engineTemperatureCelsius: 82.0,
    cargoTemperatureCelsius: 4.0,
    speedKph: 0.0,
    confidence: 0.99,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  },
  {
    eventId: "00000000-0000-4000-8800-000000000002",
    recordedAt: "2026-08-24T12:55:00Z",
    latitude: 13.0270,
    longitude: 77.5968,
    engineTemperatureCelsius: 104.5,
    cargoTemperatureCelsius: 4.2,
    speedKph: 35.0,
    confidence: 0.97,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  }
];

function renderScrubber(
  points: TelemetryHistoryPoint[] = mockPoints,
  currentIndex = 0,
  onIndexChange = vi.fn()
) {
  return render(
    <ThemeProvider theme={theme}>
      <TelemetryReplayScrubber
        points={points}
        currentIndex={currentIndex}
        onIndexChange={onIndexChange}
      />
    </ThemeProvider>
  );
}

describe("TelemetryReplayScrubber", () => {
  it("renders observation count and normal engine temperature", () => {
    renderScrubber(mockPoints, 0);

    expect(screen.getByText("Telemetry Replay Scrubber")).toBeInTheDocument();
    expect(screen.getByText("2 Observations")).toBeInTheDocument();
    expect(screen.getByText(/Engine: 82.0°C/i)).toBeInTheDocument();
    expect(screen.getByText(/0 km\/h/i)).toBeInTheDocument();
  });

  it("renders anomaly alert badge when engine temperature exceeds 100°C", () => {
    renderScrubber(mockPoints, 1);

    expect(screen.getByText(/Engine Anomaly: 104.5°C/i)).toBeInTheDocument();
    expect(screen.getByText(/35 km\/h/i)).toBeInTheDocument();
  });

  it("triggers onIndexChange on step forward and backward controls", async () => {
    const user = userEvent.setup();
    const onIndexChange = vi.fn();
    renderScrubber(mockPoints, 0, onIndexChange);

    const stepForwardBtn = screen.getByRole("button", { name: /step forward/i });
    await user.click(stepForwardBtn);

    expect(onIndexChange).toHaveBeenCalledWith(1);
  });

  it("allows selecting playback speed", async () => {
    const user = userEvent.setup();
    renderScrubber(mockPoints, 0);

    const speed2xBtn = screen.getByRole("button", { name: "2×" });
    await user.click(speed2xBtn);
    expect(speed2xBtn).toHaveAttribute("aria-pressed", "true");

    const speed5xBtn = screen.getByRole("button", { name: "5×" });
    await user.click(speed5xBtn);
    expect(speed5xBtn).toHaveAttribute("aria-pressed", "true");
  });

  it("renders empty state message when no telemetry points are available", () => {
    renderScrubber([], 0);

    expect(
      screen.getByText(/No historical telemetry breadcrumbs available for this trip/i)
    ).toBeInTheDocument();
  });
});
