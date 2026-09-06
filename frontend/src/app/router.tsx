/* eslint-disable react-refresh/only-export-components */
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import { Suspense, lazy, type ReactNode } from "react";
import { Navigate, Outlet, createBrowserRouter } from "react-router-dom";
import { useAuthStore } from "../auth/authStore";
import { AppShell } from "../components/AppShell";
import { LoginPage } from "../pages/LoginPage";
import { NotFoundPage } from "../pages/NotFoundPage";

const DashboardPage = lazy(() => import("../pages/DashboardPage").then((module) => ({ default: module.DashboardPage })));
const TripWorkspacePage = lazy(() => import("../pages/TripWorkspacePage").then((module) => ({ default: module.TripWorkspacePage })));
const DriverTripPage = lazy(() => import("../pages/DriverTripPage").then((module) => ({ default: module.DriverTripPage })));

function LoadingView() {
  return <Stack alignItems="center" py={10}><CircularProgress aria-label="Loading workspace" /></Stack>;
}

function Deferred({ children }: { children: ReactNode }) {
  return <Suspense fallback={<LoadingView />}>{children}</Suspense>;
}

function AuthenticatedLayout() {
  const session = useAuthStore((state) => state.session);
  return session ? <AppShell><Outlet /></AppShell> : <Navigate to="/login" replace />;
}

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  {
    element: <AuthenticatedLayout />,
    children: [
      { path: "/", element: <Deferred><DashboardPage /></Deferred> },
      { path: "/trips/:tripId", element: <Deferred><TripWorkspacePage /></Deferred> },
      { path: "/driver/trips/:tripId", element: <Deferred><DriverTripPage /></Deferred> },
      { path: "/simulator/trips/:tripId", element: <Deferred><DriverTripPage /></Deferred> }
    ]
  },
  { path: "*", element: <NotFoundPage /> }
]);
