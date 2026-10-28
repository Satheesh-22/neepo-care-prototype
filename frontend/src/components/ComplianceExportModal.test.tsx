import { ThemeProvider } from "@mui/material/styles";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { theme } from "../app/theme";
import { useAuthStore } from "../auth/authStore";
import { ComplianceExportModal } from "./ComplianceExportModal";

const DEMO_TRIP_ID = "00000000-0000-4000-8600-000000000001";

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
        <ComplianceExportModal
          open={open}
          onClose={onClose}
          tripId={DEMO_TRIP_ID}
          tripReferenceCode="TRIP-DEMO-001"
        />
      </QueryClientProvider>
    </ThemeProvider>
  );
}

describe("ComplianceExportModal", () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({
      session: {
        accessToken: "test-admin-jwt",
        expiresAt: "2026-08-25T13:00:00Z",
        user: {
          id: "00000000-0000-4000-8100-000000000001",
          name: "Compliance Admin",
          email: "compliance@neepocare.com",
          role: "ADMIN",
          phone: "+1-555-0100",
          createdAt: "2026-08-25T12:00:00Z",
          updatedAt: "2026-08-25T12:00:00Z"
        }
      }
    });
  });

  it("renders modal with regulatory compliance header and tabs", () => {
    renderModal(true);

    expect(screen.getByText("Regulatory Compliance & Audit Evidence")).toBeInTheDocument();
    expect(screen.getByText(/FDA 21 CFR Part 11 & EU GDP/i)).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /generate export/i })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /tamper verifier/i })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: /audit ledger/i })).toBeInTheDocument();
  });

  it("generates a compliance package and displays SHA-256 digest and payload preview", async () => {
    const user = userEvent.setup();
    renderModal(true);

    const generateBtn = screen.getByRole("button", { name: /generate & seal compliance package/i });
    expect(generateBtn).toBeInTheDocument();
    await user.click(generateBtn);

    await waitFor(() => {
      expect(screen.getByText("Evidence Package Certified & Registered in Ledger")).toBeInTheDocument();
      expect(screen.getByText("SHA-256 TAMPER-EVIDENT DIGEST")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: /download/i })).toBeInTheDocument();
    });
  });

  it("allows switching to Tamper Verifier tab and running integrity verification", async () => {
    const user = userEvent.setup();
    renderModal(true);

    // Switch to Verifier tab
    const verifierTab = screen.getByRole("tab", { name: /tamper verifier/i });
    await user.click(verifierTab);

    expect(screen.getByLabelText(/Manifest ID \(UUID\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Payload Content to Verify/i)).toBeInTheDocument();

    const manifestInput = screen.getByLabelText(/Manifest ID \(UUID\)/i);
    const payloadInput = screen.getByLabelText(/Payload Content to Verify/i);

    await user.type(manifestInput, "00000000-0000-4000-8900-000000000001");
    await user.type(payloadInput, "sample payload data for testing");

    const verifyBtn = screen.getByRole("button", { name: /verify cryptographic integrity/i });
    await user.click(verifyBtn);

    await waitFor(() => {
      expect(screen.getByText("TAMPER DETECTED: DIGEST MISMATCH")).toBeInTheDocument();
      expect(screen.getByText(/Expected Ledger Digest:/i)).toBeInTheDocument();
      expect(screen.getByText(/Computed Payload Digest:/i)).toBeInTheDocument();
    });
  });

  it("allows switching to Audit Ledger tab and displays existing manifests", async () => {
    const user = userEvent.setup();
    renderModal(true);

    const ledgerTab = screen.getByRole("tab", { name: /audit ledger/i });
    await user.click(ledgerTab);

    await waitFor(() => {
      expect(screen.getAllByText("FULL TRIP DOSSIER").length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText("COLD CHAIN EXCURSION")).toBeInTheDocument();
    });
  });
});
