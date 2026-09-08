/* eslint-disable react-refresh/only-export-components */
import type { UserRole } from "@contracts";
import CircularProgress from "@mui/material/CircularProgress";
import Stack from "@mui/material/Stack";
import { Suspense, lazy, type ComponentType, type ReactNode } from "react";
import { Navigate, Outlet, createBrowserRouter } from "react-router-dom";
import { useAuthStore } from "../auth/authStore";
import { AppShell } from "../components/AppShell";
import { RouteErrorBoundary } from "../components/RouteErrorBoundary";
import { AcceptInvitationPage } from "../pages/AcceptInvitationPage";
import { ForbiddenPage } from "../pages/ForbiddenPage";
import { LoginPage } from "../pages/LoginPage";
import { NotFoundPage } from "../pages/NotFoundPage";

function lazyWithRetry<T extends ComponentType<object>>(factory: () => Promise<{ default: T }>) {
  return lazy(() =>
    factory().catch((error) => {
      const isDynamicImportError =
        error instanceof Error &&
        (error.message.includes("dynamically imported module") ||
          error.message.includes("Failed to fetch") ||
          error.name === "TypeError");

      if (typeof window !== "undefined" && isDynamicImportError) {
        const reloadKey = `chunk_reload_${window.location.pathname}`;
        if (!sessionStorage.getItem(reloadKey)) {
          sessionStorage.setItem(reloadKey, "true");
          window.location.reload();
          return new Promise<{ default: T }>(() => {});
        }
      }
      throw error;
    })
  );
}

const DashboardPage = lazyWithRetry(() => import("../pages/DashboardPage").then((module) => ({ default: module.DashboardPage })));
const TripWorkspacePage = lazyWithRetry(() => import("../pages/TripWorkspacePage").then((module) => ({ default: module.TripWorkspacePage })));
const DriverTripPage = lazyWithRetry(() => import("../pages/DriverTripPage").then((module) => ({ default: module.DriverTripPage })));

// Admin & Onboarding Pages
const CompanyProfilePage = lazyWithRetry(() => import("../pages/CompanyProfilePage").then((m) => ({ default: m.CompanyProfilePage })));
const UserAdminPage = lazyWithRetry(() => import("../pages/UserAdminPage").then((m) => ({ default: m.UserAdminPage })));
const DriverListPage = lazyWithRetry(() => import("../pages/DriverListPage").then((m) => ({ default: m.DriverListPage })));
const DriverCreatePage = lazyWithRetry(() => import("../pages/DriverCreatePage").then((m) => ({ default: m.DriverCreatePage })));
const VehicleListPage = lazyWithRetry(() => import("../pages/VehicleListPage").then((m) => ({ default: m.VehicleListPage })));
const VehicleCreatePage = lazyWithRetry(() => import("../pages/VehicleCreatePage").then((m) => ({ default: m.VehicleCreatePage })));
const AssignmentAdminPage = lazyWithRetry(() => import("../pages/AssignmentAdminPage").then((m) => ({ default: m.AssignmentAdminPage })));
const DriverProfilePage = lazyWithRetry(() => import("../pages/DriverProfilePage").then((m) => ({ default: m.DriverProfilePage })));

function LoadingView() {
  return <Stack alignItems="center" py={10}><CircularProgress aria-label="Loading workspace" /></Stack>;
}

function Deferred({ children }: { children: ReactNode }) {
  return <Suspense fallback={<LoadingView />}>{children}</Suspense>;
}

function RoleGuard({ allowedRoles, children }: { allowedRoles: readonly UserRole[]; children: ReactNode }) {
  const session = useAuthStore((state) => state.session);
  if (!session) return <Navigate to="/login" replace />;
  if (!allowedRoles.includes(session.user.role)) {
    return <Navigate to="/forbidden" replace />;
  }
  return <>{children}</>;
}

function AuthenticatedLayout() {
  const session = useAuthStore((state) => state.session);
  return session ? <AppShell><Outlet /></AppShell> : <Navigate to="/login" replace />;
}

export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage />, errorElement: <RouteErrorBoundary /> },
  { path: "/invite/accept", element: <AcceptInvitationPage />, errorElement: <RouteErrorBoundary /> },
  {
    element: <AuthenticatedLayout />,
    errorElement: <RouteErrorBoundary />,
    children: [
      { path: "/", element: <Deferred><DashboardPage /></Deferred>, errorElement: <RouteErrorBoundary /> },
      { path: "/forbidden", element: <ForbiddenPage /> },
      { path: "/trips/:tripId", element: <Deferred><TripWorkspacePage /></Deferred>, errorElement: <RouteErrorBoundary /> },
      { path: "/driver/trips/:tripId", element: <Deferred><DriverTripPage /></Deferred>, errorElement: <RouteErrorBoundary /> },
      { path: "/simulator/trips/:tripId", element: <Deferred><DriverTripPage /></Deferred>, errorElement: <RouteErrorBoundary /> },

      // Company Profile (Admin only for edits; visible to permitted)
      {
        path: "/admin/company",
        element: (
          <RoleGuard allowedRoles={["ADMIN", "DISPATCHER"]}>
            <Deferred><CompanyProfilePage /></Deferred>
          </RoleGuard>
        )
      },

      // User Administration (Admin only)
      {
        path: "/admin/users",
        element: (
          <RoleGuard allowedRoles={["ADMIN"]}>
            <Deferred><UserAdminPage /></Deferred>
          </RoleGuard>
        )
      },

      // Driver Onboarding & Management
      {
        path: "/admin/drivers",
        element: (
          <RoleGuard allowedRoles={["ADMIN", "DISPATCHER"]}>
            <Deferred><DriverListPage /></Deferred>
          </RoleGuard>
        )
      },
      {
        path: "/admin/drivers/new",
        element: (
          <RoleGuard allowedRoles={["ADMIN"]}>
            <Deferred><DriverCreatePage /></Deferred>
          </RoleGuard>
        )
      },

      // Vehicle Registration & Management
      {
        path: "/admin/vehicles",
        element: (
          <RoleGuard allowedRoles={["ADMIN", "DISPATCHER"]}>
            <Deferred><VehicleListPage /></Deferred>
          </RoleGuard>
        )
      },
      {
        path: "/admin/vehicles/new",
        element: (
          <RoleGuard allowedRoles={["ADMIN"]}>
            <Deferred><VehicleCreatePage /></Deferred>
          </RoleGuard>
        )
      },

      // Driver-Vehicle Allocations
      {
        path: "/admin/assignments",
        element: (
          <RoleGuard allowedRoles={["ADMIN", "DISPATCHER"]}>
            <Deferred><AssignmentAdminPage /></Deferred>
          </RoleGuard>
        )
      },

      // Driver Self Profile
      {
        path: "/driver/profile",
        element: (
          <RoleGuard allowedRoles={["DRIVER", "ADMIN", "DISPATCHER"]}>
            <Deferred><DriverProfilePage /></Deferred>
          </RoleGuard>
        )
      }
    ]
  },
  { path: "*", element: <NotFoundPage /> }
]);
