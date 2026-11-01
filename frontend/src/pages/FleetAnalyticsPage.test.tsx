import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "@mui/material/styles";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it } from "vitest";
import { theme } from "../app/theme";
import { useAuthStore } from "../auth/authStore";
import { FleetAnalyticsPage } from "./FleetAnalyticsPage";

describe("FleetAnalyticsPage", () => {
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
            <FleetAnalyticsPage />
          </MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>
    );
  };

  it("renders executive analytics header and baseline 30-day KPIs", async () => {
    renderComponent();

    expect(
      screen.getByText("Fleet Reliability & Cost-Avoidance Analytics")
    ).toBeInTheDocument();

    // Default 30D metrics from mock API
    expect(await screen.findByText("$148,500.00")).toBeInTheDocument();
    expect(screen.getByText("21.2 min")).toBeInTheDocument();
    expect(screen.getByText("96.8%")).toBeInTheDocument();
    expect(screen.getByText("186 hrs")).toBeInTheDocument();
    expect(screen.getByText("99.4%")).toBeInTheDocument();

    // Root-cause distribution
    expect(screen.getByText("Coolant & Thermal Limit Exceeded")).toBeInTheDocument();
    expect(screen.getByText("Corridor Blockage & Congestion")).toBeInTheDocument();

    // Recovery strategy table
    expect(screen.getByText("Safe Parking & Mobile Mechanic")).toBeInTheDocument();
    expect(screen.getByText("Authorized Workshop Repair")).toBeInTheDocument();
  });

  it("switches to 7D timeframe and renders updated interval metrics", async () => {
    const user = userEvent.setup();
    renderComponent();

    // Wait for initial load
    expect(await screen.findByText("$148,500.00")).toBeInTheDocument();

    // Click 7D toggle button
    const btn7d = screen.getByRole("button", { name: "7 days" });
    await user.click(btn7d);

    // Expect 7D metric values
    expect(await screen.findByText("$34,200.00")).toBeInTheDocument();
    expect(screen.getByText("18.5 min")).toBeInTheDocument();
    expect(screen.getByText("98.2%")).toBeInTheDocument();
    expect(screen.getByText("42.5 hrs")).toBeInTheDocument();
  });
});
