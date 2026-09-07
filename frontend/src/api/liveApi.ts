import type { ApiResult } from "@contracts";
import { ApiClientError, type LoginRequest, type NeepoApi, type Session, type UserAdminItem } from "./types";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...init.headers
    }
  });
  if (response.status === 204) {
    return undefined as unknown as T;
  }
  const result = (await response.json()) as ApiResult<T>;
  if (!response.ok || !result.success) {
    throw new ApiClientError(response.status, result.success ? undefined : result);
  }
  return result.data;
}

const authorized = (token: string): HeadersInit => ({ Authorization: `Bearer ${token}` });

export const liveApi: NeepoApi = {
  login: (credentials: LoginRequest) => request<Session>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email: credentials.email, password: credentials.password })
  }),
  getTrip: (tripId, token) => request(`/trips/${tripId}`, { headers: authorized(token) }),
  startTrip: (tripId, token) => request(`/trips/${tripId}/start`, {
    method: "POST",
    headers: authorized(token)
  }),
  listRecoveryPlans: (tripId, token) =>
    request(`/trips/${tripId}/recovery-plans`, { headers: authorized(token) }),
  getDecisionExplanation: async (tripId, token) => {
    try {
      return await request(`/trips/${tripId}/decision-explanation`, { headers: authorized(token) });
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 404) return null;
      throw error;
    }
  },
  generateRecoveryPlans: (tripId, token) => request(`/trips/${tripId}/recovery-plans/generate`, {
    method: "POST",
    headers: authorized(token)
  }),
  ingestTelemetry: (observation, token) => request("/telemetry", {
    method: "POST",
    headers: authorized(token),
    body: JSON.stringify(observation)
  }),
  evaluateTelemetryPolicy: (tripId, policy, token) => request(`/trips/${tripId}/telemetry-policy`, {
    method: "POST",
    headers: authorized(token),
    body: JSON.stringify(policy)
  }),
  issueEventTicket: (token) => request("/events/ticket", {
    method: "POST",
    headers: authorized(token)
  }),

  // Company Profile
  getCompany: (token) => request("/company", { headers: authorized(token) }),
  updateCompany: (input, token) => request("/company", {
    method: "PATCH",
    headers: authorized(token),
    body: JSON.stringify(input)
  }),
  getCompanyAudit: (token) => request("/company/audit", { headers: authorized(token) }),

  // User Administration
  listUsers: async (page, size, token) => {
    const res = await request<{ items: readonly UserAdminItem[]; totalElements: number }>(
      `/users?page=${page}&size=${size}`,
      { headers: authorized(token) }
    );
    return { items: res.items, total: res.totalElements };
  },
  createUser: (input, token) => request("/users", {
    method: "POST",
    headers: authorized(token),
    body: JSON.stringify(input)
  }),
  inviteUser: (input, token) => request("/users/invitations", {
    method: "POST",
    headers: authorized(token),
    body: JSON.stringify(input)
  }),
  acceptInvitation: (input) => request("/users/invitations/accept", {
    method: "POST",
    body: JSON.stringify(input)
  }),
  revokeInvitation: (invitationId, token) => request(`/users/invitations/${invitationId}/revoke`, {
    method: "POST",
    headers: authorized(token)
  }),
  updateUser: (userId, input, token) => request(`/users/${userId}`, {
    method: "PATCH",
    headers: authorized(token),
    body: JSON.stringify(input)
  }),
  suspendUser: (userId, token) => request(`/users/${userId}/suspend`, {
    method: "POST",
    headers: authorized(token)
  }),
  reactivateUser: (userId, token) => request(`/users/${userId}/reactivate`, {
    method: "POST",
    headers: authorized(token)
  }),
  deleteUser: (userId, token) => request(`/users/${userId}`, {
    method: "DELETE",
    headers: authorized(token)
  }),

  // Driver Onboarding
  listDrivers: (token) => request("/drivers", { headers: authorized(token) }),
  createDriver: (input, token) => request("/drivers", {
    method: "POST",
    headers: authorized(token),
    body: JSON.stringify(input)
  }),
  getDriver: (driverId, token) => request(`/drivers/${driverId}`, { headers: authorized(token) }),
  getMyDriverProfile: (token) => request("/drivers/me", { headers: authorized(token) }),
  updateDriver: (driverId, input, token) => request(`/drivers/${driverId}`, {
    method: "PATCH",
    headers: authorized(token),
    body: JSON.stringify(input)
  }),
  inviteDriver: (driverId, token) => request(`/drivers/${driverId}/invite`, {
    method: "POST",
    headers: authorized(token)
  }),
  suspendDriver: (driverId, token) => request(`/drivers/${driverId}/suspend`, {
    method: "POST",
    headers: authorized(token)
  }),
  reactivateDriver: (driverId, token) => request(`/drivers/${driverId}/reactivate`, {
    method: "POST",
    headers: authorized(token)
  }),

  // Vehicle Management
  listVehicles: (token) => request("/vehicles", { headers: authorized(token) }),
  createVehicle: (input, token) => request("/vehicles", {
    method: "POST",
    headers: authorized(token),
    body: JSON.stringify(input)
  }),
  getVehicle: (vehicleId, token) => request(`/vehicles/${vehicleId}`, { headers: authorized(token) }),
  updateVehicle: (vehicleId, input, token) => request(`/vehicles/${vehicleId}`, {
    method: "PATCH",
    headers: authorized(token),
    body: JSON.stringify(input)
  }),
  updateVehicleStatus: (vehicleId, status, token) => request(`/vehicles/${vehicleId}/status`, {
    method: "POST",
    headers: authorized(token),
    body: JSON.stringify({ status })
  }),

  // Driver-Vehicle Assignments
  listAssignments: (token) => request("/assignments", { headers: authorized(token) }),
  createAssignment: (input, token) => request("/assignments", {
    method: "POST",
    headers: authorized(token),
    body: JSON.stringify(input)
  }),
  releaseAssignment: (assignmentId, token) => request(`/assignments/${assignmentId}/release`, {
    method: "POST",
    headers: authorized(token)
  })
};
