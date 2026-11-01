import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "@mui/material/styles";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it } from "vitest";
import { theme } from "../app/theme";
import { useAuthStore } from "../auth/authStore";
import { EnterpriseSsoPage } from "./EnterpriseSsoPage";

describe("EnterpriseSsoPage", () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({
      session: {
        accessToken: "test-admin-token",
        expiresAt: new Date(Date.now() + 3600000).toISOString(),
        user: {
          id: "00000000-0000-4000-8100-000000000001",
          name: "Test Admin",
          email: "admin@neepo.local",
          phone: "+15550100000",
          role: "ADMIN",
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
            <EnterpriseSsoPage />
          </MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>
    );
  };

  it("renders enterprise SSO configuration header and baseline federation status", async () => {
    renderComponent();

    expect(
      screen.getByText("Enterprise SSO & Identity Federation")
    ).toBeInTheDocument();

    // KPI cards
    expect(screen.getByText(/Federation Status/i)).toBeInTheDocument();
    expect(screen.getByText(/SSO Enforcement/i)).toBeInTheDocument();
    expect(screen.getByText(/JIT Provisioning/i)).toBeInTheDocument();
    expect(screen.getByText(/Protocol Type/i)).toBeInTheDocument();

    // Default mock config domain
    expect(await screen.findByDisplayValue("neepo.com")).toBeInTheDocument();
    expect(screen.getByDisplayValue("neepo-care-enterprise-client-id")).toBeInTheDocument();
    expect(screen.getByDisplayValue("https://auth.neepo-enterprise.com/oidc")).toBeInTheDocument();
  });

  it("renders audit log ledger with recorded security events", async () => {
    renderComponent();

    expect(screen.getByText("SSO Audit & Provisioning Log")).toBeInTheDocument();

    expect(await screen.findByText("LOGIN SUCCESS")).toBeInTheDocument();
    expect(screen.getByText("USER PROVISIONED (JIT)")).toBeInTheDocument();
    expect(screen.getByText("CONFIG UPDATED")).toBeInTheDocument();

    expect(screen.getByText("dispatcher@neepo.com")).toBeInTheDocument();
    expect(screen.getByText("driver.lead@neepo.com")).toBeInTheDocument();
  });

  it("tests connection when clicking test connection button and displays latency", async () => {
    const user = userEvent.setup();
    renderComponent();

    const testButtons = screen.getAllByRole("button", { name: /test connection|test idp discovery/i });
    await user.click(testButtons[0]);

    expect(
      await screen.findByText("Identity Provider Connection Verified")
    ).toBeInTheDocument();
    expect(screen.getByText(/Round-trip Latency:/i)).toBeInTheDocument();
  });
});
