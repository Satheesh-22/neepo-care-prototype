import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "@mui/material/styles";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it } from "vitest";
import { theme } from "../app/theme";
import { useAuthStore } from "../auth/authStore";
import { AcceptInvitationPage } from "./AcceptInvitationPage";
import { AssignmentAdminPage } from "./AssignmentAdminPage";
import { CompanyProfilePage } from "./CompanyProfilePage";
import { DriverCreatePage } from "./DriverCreatePage";
import { DriverListPage } from "./DriverListPage";
import { DriverProfilePage } from "./DriverProfilePage";
import { ForbiddenPage } from "./ForbiddenPage";
import { UserAdminPage } from "./UserAdminPage";
import { VehicleCreatePage } from "./VehicleCreatePage";
import { VehicleListPage } from "./VehicleListPage";

function renderWithProviders(ui: React.ReactElement, initialRoute = "/") {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false }
    }
  });

  return render(
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={[initialRoute]}>
          {ui}
        </MemoryRouter>
      </QueryClientProvider>
    </ThemeProvider>
  );
}

describe("Onboarding and Administration Pages", () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({
      session: {
        accessToken: "test-admin-jwt",
        expiresAt: "2026-08-25T13:00:00Z",
        user: {
          id: "00000000-0000-4000-8100-000000000001",
          name: "Admin User",
          email: "admin@neepocare.com",
          role: "ADMIN",
          phone: "+1-555-0100",
          createdAt: "2026-08-25T12:00:00Z",
          updatedAt: "2026-08-25T12:00:00Z"
        }
      }
    });
  });

  it("renders Company Profile Page with organization settings", async () => {
    renderWithProviders(<CompanyProfilePage />);

    expect(await screen.findByRole("heading", { name: /Company Profile/i })).toBeInTheDocument();
    expect(await screen.findByDisplayValue(/Neepo Cold Chain Logistics Inc\./i)).toBeInTheDocument();
    expect(screen.getByText(/Administrator Access/i)).toBeInTheDocument();
  });

  it("renders User Administration Page and opens invite modal", async () => {
    renderWithProviders(<UserAdminPage />);

    expect(await screen.findByText(/User Administration/i)).toBeInTheDocument();
    expect(await screen.findByText(/admin@neepo.local/i)).toBeInTheDocument();

    const inviteBtn = screen.getByRole("button", { name: /Invite User/i });
    await userEvent.click(inviteBtn);

    expect(await screen.findByText(/Invite Organization Member/i)).toBeInTheDocument();
  });

  it("renders Driver List Page and displays driver records", async () => {
    renderWithProviders(<DriverListPage />);

    expect(await screen.findByText(/Driver Directory/i)).toBeInTheDocument();
    expect(await screen.findByText(/Demo Driver/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Onboard Driver/i })).toBeInTheDocument();
  });

  it("renders Driver Registration Page and validates required fields", async () => {
    renderWithProviders(<DriverCreatePage />, "/admin/drivers/new");

    expect(await screen.findByText(/Onboard Commercial Driver/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/First Name \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Work Email \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Commercial License Number \*/i)).toBeInTheDocument();
  });

  it("renders Vehicle List Page and displays fleet vehicles", async () => {
    renderWithProviders(<VehicleListPage />);

    expect(await screen.findByText(/Fleet & Vehicles/i)).toBeInTheDocument();
    expect(await screen.findByText(/CA-94105-REEFER/i)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Register Vehicle/i })).toBeInTheDocument();
  });

  it("renders Vehicle Registration Page with cold-chain specifications", async () => {
    renderWithProviders(<VehicleCreatePage />, "/admin/vehicles/new");

    expect(await screen.findByText(/Register Fleet Vehicle/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/License Registration Number \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Payload Capacity \(kg\) \*/i)).toBeInTheDocument();
  });

  it("renders Assignment Admin Page and displays allocations", async () => {
    renderWithProviders(<AssignmentAdminPage />);

    expect(await screen.findByText(/Driver-to-Vehicle Allocations/i)).toBeInTheDocument();
    expect(await screen.findByText(/ACTIVE ASSIGNMENTS/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /New Assignment/i })).toBeInTheDocument();
  });

  it("renders Driver Profile Page for authenticated driver", async () => {
    useAuthStore.setState({
      session: {
        accessToken: "test-driver-jwt",
        expiresAt: "2026-08-25T13:00:00Z",
        user: {
          id: "00000000-0000-4000-8200-000000000001",
          name: "Demo Driver",
          email: "driver@example.com",
          role: "DRIVER",
          phone: "+1-555-0102",
          createdAt: "2026-08-25T12:00:00Z",
          updatedAt: "2026-08-25T12:00:00Z"
        }
      }
    });

    renderWithProviders(<DriverProfilePage />);

    expect(await screen.findByText(/Driver Operational Profile/i)).toBeInTheDocument();
    expect(await screen.findByText(/Driving Time Budget/i)).toBeInTheDocument();
  });

  it("renders Accept Invitation Page with token parameter", async () => {
    renderWithProviders(
      <Routes>
        <Route path="/invite/accept" element={<AcceptInvitationPage />} />
      </Routes>,
      "/invite/accept?token=sample-mock-invite-token-12345"
    );

    expect(await screen.findByText(/Activate Account & Set Password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/New Password \*/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Confirm Password \*/i)).toBeInTheDocument();
  });

  it("renders Forbidden 403 Page with access denied notification", () => {
    renderWithProviders(<ForbiddenPage />);

    expect(screen.getByText(/403 — Access Forbidden/i)).toBeInTheDocument();
    expect(screen.getByText(/Return to Dashboard/i)).toBeInTheDocument();
  });
});
