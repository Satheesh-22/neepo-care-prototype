import type { ApiResult } from "@contracts";
import { ApiClientError, type LoginRequest, type NeepoApi, type Session } from "./types";

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
  })
};
