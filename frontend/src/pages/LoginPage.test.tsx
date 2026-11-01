import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "@mui/material/styles";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it } from "vitest";
import { theme } from "../app/theme";
import { useAuthStore } from "../auth/authStore";
import { LoginPage } from "./LoginPage";

describe("login page", () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.setState({ session: null });
  });

  const renderComponent = () => {
    const client = new QueryClient({ defaultOptions: { mutations: { retry: false }, queries: { retry: false } } });
    return render(
      <ThemeProvider theme={theme}>
        <QueryClientProvider client={client}>
          <MemoryRouter initialEntries={["/login"]}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/" element={<div>Protected operations workspace</div>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>
    );
  };

  it("starts a demo session and enters the protected workspace", async () => {
    renderComponent();

    await userEvent.click(screen.getByRole("button", { name: /sign in to operations/i }));

    expect(await screen.findByText("Protected operations workspace")).toBeInTheDocument();
    expect(useAuthStore.getState().session?.user.role).toBe("DISPATCHER");
  });

  it("discovers enterprise SSO and allows federated sign in", async () => {
    const user = userEvent.setup();
    renderComponent();

    const emailInput = screen.getByLabelText(/email address/i);
    await user.clear(emailInput);
    await user.type(emailInput, "dispatcher@neepo.com");

    const matchingElements = await screen.findAllByText(/NEEPO Enterprise Solutions/i);
    expect(matchingElements.length).toBeGreaterThan(0);

    const ssoSignInBtn = await screen.findByRole("button", { name: /or sign in with oidc sso/i });
    await user.click(ssoSignInBtn);

    expect(await screen.findByText("Protected operations workspace")).toBeInTheDocument();
    expect(useAuthStore.getState().session?.user.email).toBe("dispatcher@neepo.com");
  });
});
