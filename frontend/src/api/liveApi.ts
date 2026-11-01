import type { ApiResult } from "@contracts";
import {
  ApiClientError,
  type CompanySsoConfigResponse,
  type ComplianceExportFormat,
  type ComplianceExportManifest,
  type ComplianceExportType,
  type DepotResponse,
  type GatewayBatchSyncResponse,
  type GatewayHeartbeatResponse,
  type GatewayOtaConfigResponse,
  type HardwareGatewayItem,
  type LoginRequest,
  type NeepoApi,
  type ScimStatusResponse,
  type ScimTokenResponse,
  type Session,
  type SsoAuditLogItem,
  type SsoDiscoveryResponse,
  type SsoLoginResponse,
  type SsoTestResultResponse,
  type UserAdminItem
} from "./types";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";

interface BackendManifestResponse {
  id: string;
  companyId: string;
  tripId?: string | null;
  exportType: ComplianceExportType;
  exportFormat: ComplianceExportFormat;
  sha256Digest: string;
  recordCount: number;
  exportedAt: string;
  exportedByUserId?: string | null;
  regulatoryContext?: string;
  metadataJson?: string;
}

interface BackendPackageResponse {
  manifest: BackendManifestResponse;
  contentType: string;
  filename: string;
  payloadContent: string;
}

interface BackendVerificationResponse {
  manifestId: string;
  valid: boolean;
  calculatedSha256Hex: string;
  manifestSha256Hex: string;
  verifiedAt: string;
  message: string;
}

function mapManifest(m: BackendManifestResponse): ComplianceExportManifest {
  let metadata: Record<string, unknown> = {};
  if (m.metadataJson) {
    try {
      metadata = JSON.parse(m.metadataJson);
    } catch {
      metadata = { raw: m.metadataJson };
    }
  }
  if (m.regulatoryContext) {
    metadata.regulatoryContext = m.regulatoryContext;
  }
  return {
    id: m.id,
    companyId: m.companyId,
    tripId: m.tripId ?? null,
    exportType: m.exportType,
    format: m.exportFormat,
    sha256Digest: m.sha256Digest,
    recordCount: m.recordCount,
    generatedByUserId: m.exportedByUserId ?? null,
    metadata,
    createdAt: m.exportedAt
  };
}

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
  getTelemetryHistory: (tripId, options, token) => {
    const params = new URLSearchParams();
    if (options?.from) params.set("from", options.from);
    if (options?.to) params.set("to", options.to);
    if (options?.limit) params.set("limit", String(options.limit));
    const qs = params.toString() ? `?${params.toString()}` : "";
    return request(`/trips/${tripId}/telemetry/history${qs}`, {
      headers: token ? authorized(token) : undefined
    });
  },
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
  }),

  // Notification Channels & Alerting
  listNotificationChannels: (token) => request("/notifications/channels", { headers: authorized(token) }),
  createNotificationChannel: (input, token) => request("/notifications/channels", {
    method: "POST",
    headers: authorized(token),
    body: JSON.stringify(input)
  }),
  deleteNotificationChannel: (channelId, token) => request(`/notifications/channels/${channelId}`, {
    method: "DELETE",
    headers: authorized(token)
  }),
  testNotificationChannel: (channelId, token) => request(`/notifications/channels/${channelId}/test`, {
    method: "POST",
    headers: authorized(token)
  }),
  listNotificationLogs: (token, limit = 50) => request(`/notifications/logs?limit=${encodeURIComponent(limit)}`, {
    headers: authorized(token)
  }),

  // Compliance & Audit Evidence APIs
  generateComplianceExport: async (tripId, exportType, exportFormat, token) => {
    const raw = await request<BackendPackageResponse>(
      `/compliance/exports/trip/${tripId}?type=${exportType}&format=${exportFormat}`,
      {
        method: "POST",
        headers: authorized(token)
      }
    );
    return {
      manifest: mapManifest(raw.manifest),
      rawPayload: raw.payloadContent,
      mediaType: raw.contentType,
      filename: raw.filename
    };
  },
  listComplianceExports: async (limit = 50, token) => {
    const raw = await request<readonly BackendManifestResponse[]>(
      `/compliance/exports?limit=${encodeURIComponent(limit)}`,
      { headers: authorized(token) }
    );
    return raw.map(mapManifest);
  },
  getComplianceExportManifest: async (manifestId, token) => {
    const raw = await request<BackendManifestResponse>(
      `/compliance/exports/${manifestId}`,
      { headers: authorized(token) }
    );
    return mapManifest(raw);
  },
  verifyComplianceExport: async (manifestId, verificationRequest, token) => {
    const raw = await request<BackendVerificationResponse>(
      `/compliance/exports/${manifestId}/verify`,
      {
        method: "POST",
        headers: authorized(token),
        body: JSON.stringify({
          payloadContent: verificationRequest.payload
        })
      }
    );
    return {
      manifestId: raw.manifestId,
      valid: raw.valid,
      expectedDigest: raw.manifestSha256Hex,
      computedDigest: raw.calculatedSha256Hex,
      verifiedAt: raw.verifiedAt,
      message: raw.message
    };
  },
  getFleetAnalytics: async (timeframe, token) => {
    const query = timeframe ? `?timeframe=${encodeURIComponent(timeframe)}` : "";
    return request(
      `/analytics/fleet${query}`,
      { headers: authorized(token) }
    );
  },
  listGateways: async (token) => {
    return request<readonly HardwareGatewayItem[]>("/gateways", {
      headers: authorized(token)
    });
  },
  getGateway: async (gatewayId, token) => {
    return request<HardwareGatewayItem>(`/gateways/${gatewayId}`, {
      headers: authorized(token)
    });
  },
  registerGateway: async (input, token) => {
    return request<HardwareGatewayItem>("/gateways", {
      method: "POST",
      headers: authorized(token),
      body: JSON.stringify(input)
    });
  },
  getGatewayOtaConfig: async (gatewayId, token) => {
    return request<GatewayOtaConfigResponse>(`/gateways/${gatewayId}/config`, {
      headers: authorized(token)
    });
  },
  syncGatewayBatch: async (gatewayId, input, token) => {
    return request<GatewayBatchSyncResponse>(`/gateways/${gatewayId}/sync`, {
      method: "POST",
      headers: authorized(token),
      body: JSON.stringify(input)
    });
  },
  sendGatewayHeartbeat: async (gatewayId, input, token) => {
    return request<GatewayHeartbeatResponse>(`/gateways/${gatewayId}/heartbeat`, {
      method: "POST",
      headers: authorized(token),
      body: JSON.stringify(input)
    });
  },

  discoverSso: async ({ email, domain }) => {
    const params = new URLSearchParams();
    if (email) params.set("email", email);
    if (domain) params.set("domain", domain);
    const query = params.toString() ? `?${params.toString()}` : "";
    return request<SsoDiscoveryResponse>(`/auth/sso/discover${query}`);
  },
  loginSso: async (req) => {
    return request<SsoLoginResponse>("/auth/sso/callback", {
      method: "POST",
      body: JSON.stringify(req)
    });
  },
  getCompanySsoConfig: async (token) => {
    return request<CompanySsoConfigResponse>("/admin/sso", {
      headers: authorized(token)
    });
  },
  updateCompanySsoConfig: async (input, token) => {
    return request<CompanySsoConfigResponse>("/admin/sso", {
      method: "PUT",
      headers: authorized(token),
      body: JSON.stringify(input)
    });
  },
  listSsoAuditLogs: async (token, limit = 50) => {
    return request<readonly SsoAuditLogItem[]>(`/admin/sso/audit-logs?limit=${limit}`, {
      headers: authorized(token)
    });
  },
  testSsoConfiguration: async (token) => {
    return request<SsoTestResultResponse>("/admin/sso/test", {
      method: "POST",
      headers: authorized(token)
    });
  },

  // Fleet Depots & Partitioning
  listDepots: async (token) => {
    return request<readonly DepotResponse[]>("/depots", {
      headers: authorized(token)
    });
  },
  getDepot: async (depotId, token) => {
    return request<DepotResponse>(`/depots/${depotId}`, {
      headers: authorized(token)
    });
  },
  createDepot: async (input, token) => {
    return request<DepotResponse>("/admin/depots", {
      method: "POST",
      headers: authorized(token),
      body: JSON.stringify(input)
    });
  },
  updateDepot: async (depotId, input, token) => {
    return request<DepotResponse>(`/admin/depots/${depotId}`, {
      method: "PUT",
      headers: authorized(token),
      body: JSON.stringify(input)
    });
  },
  deleteDepot: async (depotId, token) => {
    return request<void>(`/admin/depots/${depotId}`, {
      method: "DELETE",
      headers: authorized(token)
    });
  },

  // SCIM 2.0 User Provisioning
  getScimStatus: async (token) => {
    return request<ScimStatusResponse>("/admin/scim/status", {
      headers: authorized(token)
    });
  },
  generateScimToken: async (token) => {
    return request<ScimTokenResponse>("/admin/scim/token", {
      method: "POST",
      headers: authorized(token)
    });
  },
  revokeScimToken: async (token) => {
    return request<void>("/admin/scim/token", {
      method: "DELETE",
      headers: authorized(token)
    });
  }
};


