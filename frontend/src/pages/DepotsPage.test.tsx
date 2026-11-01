import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "@mui/material/styles";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it } from "vitest";
import { theme } from "../app/theme";
import { useAuthStore } from "../auth/authStore";
import { DepotsPage } from "./DepotsPage";

describe("DepotsPage", () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({
      session: {
        accessToken: "test-token",
        expiresAt: new Date(Date.now() + 3600000).toISOString(),
        user: {
          id: "00000000-0000-4000-8100-000000000001",
          name: "Test Admin",
          email: "admin@neepo.local",
          phone: "+15550100001",
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
            <DepotsPage />
          </MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>
    );
  };

  it("renders page header and baseline KPI cards", async () => {
    renderComponent();

    expect(await screen.findByText(/Fleet Depots & Regional Hubs/i)).toBeInTheDocument();
    expect(screen.getByText(/TOTAL DEPOTS/i)).toBeInTheDocument();
    expect(screen.getByText(/ACTIVE FACILITIES/i)).toBeInTheDocument();
    expect(screen.getByText(/TOTAL CAPACITY/i)).toBeInTheDocument();
    expect(screen.getByText(/STATIONED FLEET/i)).toBeInTheDocument();
  });

  it("renders depot facilities in ledger table", async () => {
    renderComponent();

    expect(await screen.findByText(/Seattle Central Cold Chain Depot/i)).toBeInTheDocument();
    expect(screen.getByText(/Portland Distribution Hub/i)).toBeInTheDocument();
    expect(screen.getByText("DEPOT-HQ-01")).toBeInTheDocument();
    expect(screen.getByText("DEPOT-PORT-02")).toBeInTheDocument();
  });

  it("filters depots by search query", async () => {
    const user = userEvent.setup();
    renderComponent();

    expect(await screen.findByText(/Seattle Central Cold Chain Depot/i)).toBeInTheDocument();
    const searchInput = screen.getByPlaceholderText(/Search by code, facility name, or city/i);

    await user.type(searchInput, "Portland");
    expect(screen.getByText(/Portland Distribution Hub/i)).toBeInTheDocument();
    expect(screen.queryByText(/Seattle Central Cold Chain Depot/i)).not.toBeInTheDocument();
  });

  it("allows admin to open add depot dialog and fill in fields", async () => {
    const user = userEvent.setup();
    renderComponent();

    const addBtn = await screen.findByRole("button", { name: /Add Depot/i });
    await user.click(addBtn);

    expect(await screen.findByText(/Register New Facility Depot/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Depot Code/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Facility Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Street Address/i)).toBeInTheDocument();

    const cancelBtn = screen.getByRole("button", { name: /Cancel/i });
    await user.click(cancelBtn);
  });
});

