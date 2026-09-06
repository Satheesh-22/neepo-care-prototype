import type { ApiError, DecisionExplanation, RecoveryPlan, TelemetryObservation, TelemetryPolicyDecision, TelemetryPolicyRequest, Trip, User, UserRole, UUID } from "@contracts";

export interface LoginRequest {
  email: string;
  password: string;
  demoRole?: UserRole;
}

export interface Session {
  accessToken: string;
  expiresAt: string;
  user: User;
}

export interface TelemetryAccepted {
  eventId: UUID;
  duplicate: boolean;
  acceptedAt: string;
}

export interface WebSocketTicket {
  ticket: string;
  expiresAt: string;
}

export interface NeepoApi {
  login(request: LoginRequest): Promise<Session>;
  getTrip(tripId: UUID, accessToken: string): Promise<Trip>;
  startTrip(tripId: UUID, accessToken: string): Promise<Trip>;
  listRecoveryPlans(tripId: UUID, accessToken: string): Promise<readonly RecoveryPlan[]>;
  getDecisionExplanation(tripId: UUID, accessToken: string): Promise<DecisionExplanation | null>;
  generateRecoveryPlans(tripId: UUID, accessToken: string): Promise<readonly RecoveryPlan[]>;
  ingestTelemetry(observation: TelemetryObservation, accessToken: string): Promise<TelemetryAccepted>;
  evaluateTelemetryPolicy(tripId: UUID, request: TelemetryPolicyRequest, accessToken: string): Promise<TelemetryPolicyDecision>;
  issueEventTicket(accessToken: string): Promise<WebSocketTicket>;
}

export class ApiClientError extends Error {
  constructor(readonly status: number, readonly response?: ApiError) {
    super(response?.error.message ?? "The request could not be completed.");
    this.name = "ApiClientError";
  }
}
