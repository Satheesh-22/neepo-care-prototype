import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "@mui/material/styles";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it } from "vitest";
import { theme } from "../app/theme";
import { useAuthStore } from "../auth/authStore";
import { HardwareGatewayPage } from "./HardwareGatewayPage";

describe("HardwareGatewayPage", () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({
      session: {
        accessToken: "test-token",
        expiresAt: new Date(Date.now() + 3600000).toISOString(),
        user: {
          id: "00000000-0000-4000-8100-000000000001",
          name: "Test Dispatcher",
          email: "dispatcher@neepo.local",
          phone: "+15550100001",
          role: "DISPATCHER",
          createdAt: "2026-08-25T12:00:00Z",
          updatedAt: "2026-08-25T12:00:00Z"
        }
      }
    });
  });

  const renderComponent = () => {
    const client = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 }
      }
    });

    return render(
      <ThemeProvider theme={theme}>
        <QueryClientProvider client={client}>
          <MemoryRouter>
            <HardwareGatewayPage />
          </MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>
    );
  };

  it("renders header banner and baseline hardware gateway KPIs", async () => {
    renderComponent();

    expect(screen.getByText("Hardware Gateway Fleet Pilot")).toBeInTheDocument();
    expect(screen.getByText("v1.1 Hardware Pilot")).toBeInTheDocument();
    expect(screen.getByText("Offline Flash Resilience")).toBeInTheDocument();

    // Check KPI cards
    expect(screen.getByText("Active Edge Gateways")).toBeInTheDocument();
    expect(screen.getByText("SPI Flash Buffer Capacity")).toBeInTheDocument();
    expect(screen.getByText("Cellular Link Health")).toBeInTheDocument();
    expect(screen.getByText("LTE Cat-4")).toBeInTheDocument();
    expect(screen.getByText("ATECC608A")).toBeInTheDocument();

    // Gateway serials from mockApi
    expect(await screen.findByText("NEEPO-GW-2026-001")).toBeInTheDocument();
    expect(screen.getByText("NEEPO-GW-2026-002")).toBeInTheDocument();
  });

  it("filters gateways by status tab", async () => {
    const user = userEvent.setup();
    renderComponent();

    expect(await screen.findByText("NEEPO-GW-2026-001")).toBeInTheDocument();
    expect(screen.getByText("NEEPO-GW-2026-002")).toBeInTheDocument();

    // Filter by ACTIVE
    const activeChip = screen.getByRole("button", { name: "ACTIVE" });
    await user.click(activeChip);

    expect(screen.getByText("NEEPO-GW-2026-001")).toBeInTheDocument();
    expect(screen.queryByText("NEEPO-GW-2026-002")).not.toBeInTheDocument();

    // Filter by OFFLINE
    const offlineChip = screen.getByRole("button", { name: "OFFLINE" });
    await user.click(offlineChip);

    expect(screen.queryByText("NEEPO-GW-2026-001")).not.toBeInTheDocument();
    expect(screen.getByText("NEEPO-GW-2026-002")).toBeInTheDocument();
  });

  it("opens OTA config and diagnostics modal and performs simulated batch replay", async () => {
    const user = userEvent.setup();
    renderComponent();

    expect(await screen.findByText("NEEPO-GW-2026-001")).toBeInTheDocument();

    const otaButtons = screen.getAllByRole("button", { name: /OTA & Diagnostics/i });
    await user.click(otaButtons[0]);

    // Modal opens
    expect(await screen.findByText(/NEEPO-GW-2026-001 Diagnostics & OTA/i)).toBeInTheDocument();
    expect(await screen.findByText("Active Over-The-Air (OTA) Configuration Profile")).toBeInTheDocument();
    expect(screen.getByText("/api/v1/telemetry")).toBeInTheDocument();
    expect(screen.getByText("105°C")).toBeInTheDocument();
    expect(screen.getByText("-20°C")).toBeInTheDocument();

    // Execute simulated batch replay
    const replayButton = screen.getByRole("button", { name: /Simulate Offline Replay Batch/i });
    await user.click(replayButton);

    expect(await screen.findByText(/SPI Flash Replay Succeeded/i)).toBeInTheDocument();
    expect(screen.getByText(/Batch size: 2/i)).toBeInTheDocument();
  });

  it("opens register gateway dialog and validates required fields", async () => {
    const user = userEvent.setup();
    renderComponent();

    const registerButton = screen.getByRole("button", { name: /Register Gateway/i });
    await user.click(registerButton);

    expect(screen.getByText("Register New Hardware Edge Gateway")).toBeInTheDocument();
    expect(screen.getByLabelText(/Device Serial Number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/HMAC Shared Secret/i)).toBeInTheDocument();
  });
});
