import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "@mui/material/styles";
import { render, screen, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createAppTheme } from "../app/theme";
import { useAuthStore } from "../auth/authStore";
import { usePreferencesStore } from "./preferencesStore";
import { LiveAnnouncer, announce } from "./index";
import { AppShell } from "../components/AppShell";

vi.mock("../offline/useOfflineQueue", () => ({
  useOfflineQueue: () => ({
    pendingCount: 0,
    synchronizing: false,
    synchronize: vi.fn()
  })
}));

describe("WCAG 2.1 AA Accessibility & Preferences", () => {
  beforeEach(() => {
    localStorage.clear();
    usePreferencesStore.setState({
      locale: "en",
      highContrast: false,
      largeText: false,
      reducedMotion: false,
      soundAlerts: true
    });
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

  const renderShell = () => {
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false, gcTime: 0 } }
    });
    const theme = createAppTheme({
      highContrast: usePreferencesStore.getState().highContrast,
      largeText: usePreferencesStore.getState().largeText
    });

    return render(
      <ThemeProvider theme={theme}>
        <QueryClientProvider client={client}>
          <MemoryRouter>
            <LiveAnnouncer />
            <AppShell>
              <div>Test Main Content</div>
            </AppShell>
          </MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>
    );
  };

  it("renders a skip-to-content link pointing to #main-content landmark", () => {
    renderShell();

    const skipLink = screen.getByRole("link", { name: /Skip to main content/i });
    expect(skipLink).toBeInTheDocument();
    expect(skipLink).toHaveAttribute("href", "#main-content");

    const mainElement = screen.getByRole("main");
    expect(mainElement).toHaveAttribute("id", "main-content");
  });

  it("provides correct ARIA landmark roles (banner, navigation, main)", () => {
    renderShell();

    expect(screen.getByRole("banner")).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: /Main Navigation/i })).toBeInTheDocument();
    expect(screen.getByRole("main")).toBeInTheDocument();
  });

  it("announces live screen-reader messages via polite and assertive channels", () => {
    renderShell();

    const politeRegion = document.getElementById("a11y-polite");
    const assertiveRegion = document.getElementById("a11y-assertive");

    expect(politeRegion).toHaveAttribute("aria-live", "polite");
    expect(assertiveRegion).toHaveAttribute("aria-live", "assertive");

    act(() => {
      announce("Telemetry status normal", "polite");
    });
    expect(politeRegion?.textContent).toBe("Telemetry status normal");

    act(() => {
      announce("CRITICAL: Engine temperature exceeded!", "assertive");
    });
    expect(assertiveRegion?.textContent).toBe("CRITICAL: Engine temperature exceeded!");
  });

  it("toggles language between English and Spanish in AppShell", async () => {
    const user = userEvent.setup();
    renderShell();

    const langToggleBtn = screen.getByRole("button", { name: /Current language: EN/i });
    expect(langToggleBtn).toBeInTheDocument();

    // Toggle to Spanish
    await user.click(langToggleBtn);
    expect(usePreferencesStore.getState().locale).toBe("es");

    // Skip to content should now be in Spanish
    expect(screen.getByRole("link", { name: /Saltar al contenido principal/i })).toBeInTheDocument();
    // Navigation should now be in Spanish
    expect(screen.getByText("Tablero")).toBeInTheDocument();
    expect(screen.getByText("Depósitos")).toBeInTheDocument();
  });

  it("opens accessibility options popover and toggles High Contrast mode", async () => {
    const user = userEvent.setup();
    renderShell();

    const a11yBtn = screen.getByRole("button", { name: /Accessibility & Display Options/i });
    await user.click(a11yBtn);

    expect(await screen.findByText(/High Contrast Mode/i)).toBeInTheDocument();
    expect(screen.getByText(/Large Text Scaling/i)).toBeInTheDocument();

    const highContrastSwitch = screen.getByLabelText(/High Contrast Mode/i);
    expect(highContrastSwitch).not.toBeChecked();

    await user.click(highContrastSwitch);
    expect(usePreferencesStore.getState().highContrast).toBe(true);
  });
});
