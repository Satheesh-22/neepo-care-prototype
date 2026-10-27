import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "@mui/material/styles";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { theme } from "../app/theme";
import { useAuthStore } from "../auth/authStore";
import { NotificationSettingsModal } from "./NotificationSettingsModal";

function renderModal(open = true, onClose = vi.fn()) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false }
    }
  });

  return render(
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={queryClient}>
        <NotificationSettingsModal open={open} onClose={onClose} />
      </QueryClientProvider>
    </ThemeProvider>
  );
}

describe("NotificationSettingsModal", () => {
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

  it("renders modal with alert engine header and configured channels", async () => {
    renderModal(true);

    expect(screen.getByText("Alert & Webhook Engine")).toBeInTheDocument();
    expect(screen.getByText(/Real-time multi-channel incident dispatch/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("Operations Center Slack Webhook")).toBeInTheDocument();
      expect(screen.getByText("Fleet Emergency PagerDuty")).toBeInTheDocument();
    });
  });

  it("allows switching to Add Channel tab and filling in new webhook form", async () => {
    const user = userEvent.setup();
    renderModal(true);

    const addTab = screen.getByRole("tab", { name: /add channel/i });
    await user.click(addTab);

    expect(screen.getByLabelText(/Channel Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Target Endpoint URL/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /create channel/i })).toBeInTheDocument();

    await user.type(screen.getByLabelText(/Channel Name/i), "Security Team Webhook");
    await user.type(screen.getByLabelText(/Target Endpoint URL/i), "https://alerts.secops.internal/neepo");

    await user.click(screen.getByRole("button", { name: /create channel/i }));

    await waitFor(() => {
      expect(screen.getByText("Security Team Webhook")).toBeInTheDocument();
    });
  });

  it("allows testing a channel and renders HMAC-SHA256 signature feedback", async () => {
    const user = userEvent.setup();
    renderModal(true);

    await waitFor(() => {
      expect(screen.getByText("Operations Center Slack Webhook")).toBeInTheDocument();
    });

    const testButtons = screen.getAllByRole("button", { name: /send test ping/i });
    expect(testButtons.length).toBeGreaterThan(0);
    await user.click(testButtons[0]);

    await waitFor(() => {
      expect(screen.getByText(/X-Neepo-Signature:/i)).toBeInTheDocument();
    });
  });

  it("allows switching to Delivery Logs tab and displaying recorded audit logs", async () => {
    const user = userEvent.setup();
    renderModal(true);

    const logsTab = screen.getByRole("tab", { name: /delivery logs/i });
    await user.click(logsTab);

    await waitFor(() => {
      expect(screen.getByText("SYSTEM_STARTUP")).toBeInTheDocument();
      expect(screen.getByText("VEHICLE_OVERHEATING")).toBeInTheDocument();
    });
  });
});
