import { useEffect, useMemo } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider } from "react-router-dom";
import { ThemeProvider } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import { router } from "./router";
import { createAppTheme } from "./theme";
import { usePreferencesStore } from "../accessibility/preferencesStore";
import { LiveAnnouncer } from "../accessibility/LiveAnnouncer";

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 15_000, retry: 1 } }
});

export function App() {
  const highContrast = usePreferencesStore((s) => s.highContrast);
  const largeText = usePreferencesStore((s) => s.largeText);
  const locale = usePreferencesStore((s) => s.locale);

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  useEffect(() => {
    if (highContrast) {
      document.body.classList.add("high-contrast");
    } else {
      document.body.classList.remove("high-contrast");
    }
  }, [highContrast]);

  const activeTheme = useMemo(
    () => createAppTheme({ highContrast, largeText }),
    [highContrast, largeText]
  );

  return (
    <ThemeProvider theme={activeTheme}>
      <CssBaseline />
      <LiveAnnouncer />
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </ThemeProvider>
  );
}
