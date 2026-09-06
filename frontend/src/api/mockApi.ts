import type { DecisionExplanation, PlanEvaluation, RecoveryPlan, Trip, User, UserRole } from "@contracts";
import type { LoginRequest, NeepoApi } from "./types";

export const DEMO_TRIP_ID = "00000000-0000-4000-8600-000000000001";
const now = "2026-08-24T13:00:00Z";

const trip: Trip = {
  id: DEMO_TRIP_ID,
  companyId: "00000000-0000-4000-8000-000000000001",
  vehicleId: "00000000-0000-4000-8300-000000000001",
  driverId: "00000000-0000-4000-8200-000000000001",
  cargoId: "00000000-0000-4000-8400-000000000001",
  routeId: "00000000-0000-4000-8500-000000000001",
  referenceCode: "TRIP-DEMO-001",
  status: "DISRUPTED",
  deliveryDeadline: "2026-08-24T18:30:00Z",
  maximumWaitingMinutes: 60,
  acceptedEtaThresholdMinutes: 30,
  startedAt: "2026-08-24T11:00:00Z",
  createdAt: "2026-08-24T08:00:00Z",
  updatedAt: now
};

const evaluation = (planId: string, feasible: boolean, utility: number): PlanEvaluation => ({
  planId,
  feasible,
  violatedHardConstraints: feasible ? [] : ["CARGO_TEMPERATURE_WINDOW_EXCEEDED"],
  warnings: feasible ? ["PROVIDER_DATA_AGING"] : [],
  softConstraintResults: feasible ? { deliveryContinuity: 86, safety: 94 } : {},
  riskScore: feasible ? 12 : 78,
  uncertaintyScore: feasible ? 8 : 18,
  benefitScore: feasible ? 88 : 42,
  lambdaCoefficient: 0.35,
  muCoefficient: 0.2,
  finalUtility: utility,
  confidence: feasible ? 0.95 : 0.91,
  freshnessSummary: feasible ? { "vehicle.health": "FRESH" } : { "cargo.condition": "FRESH" },
  explanationCodes: [feasible ? "PLAN_SELECTED_HIGHEST_UTILITY" : "PLAN_REJECTED_HARD_CONSTRAINT"],
  evaluatedAt: now,
  evaluatorVersion: "plan-evaluator-1.0.0",
  inputHash: "a".repeat(64)
});

const selectedId = "10000000-0000-4000-9000-000000000001";
const rejectedId = "10000000-0000-4000-9000-000000000003";
const disruptionId = "20000000-0000-4000-9000-000000000002";
const plans: readonly RecoveryPlan[] = [
  {
    id: selectedId,
    tripId: DEMO_TRIP_ID,
    disruptionId,
    type: "SAFE_PARKING_MOBILE_MECHANIC",
    status: "SELECTED",
    providerId: "00000000-0000-4000-8700-000000000002",
    steps: [
      {
        sequence: 1,
        actionCode: "STOP_AND_STABILIZE",
        instruction: "Stop safely and stabilize the overheating vehicle.",
        expectedStartAt: now,
        expectedCompleteAt: "2026-08-24T13:10:00Z",
        requiredResources: ["SAFE_STOPPING_LOCATION"]
      },
      {
        sequence: 2,
        actionCode: "WAIT_FOR_MOBILE_MECHANIC",
        instruction: "Remain parked while the confirmed mobile mechanic arrives.",
        providerId: "00000000-0000-4000-8700-000000000002",
        expectedStartAt: "2026-08-24T13:10:00Z",
        expectedCompleteAt: "2026-08-24T13:40:00Z",
        requiredResources: ["MOBILE_MECHANIC_CONFIRMATION"]
      }
    ],
    evaluation: evaluation(selectedId, true, 82.2),
    generatedAt: now,
    selectedAt: now
  },
  {
    id: rejectedId,
    tripId: DEMO_TRIP_ID,
    disruptionId,
    type: "TOW_REPAIR_OR_TRANSFER",
    status: "REJECTED",
    steps: [],
    evaluation: evaluation(rejectedId, false, 11.1),
    generatedAt: now
  }
];

const explanation: DecisionExplanation = {
  tripId: DEMO_TRIP_ID,
  disruptionId,
  selectedPlanId: selectedId,
  rejectedPlanIds: [rejectedId],
  selectionExplanationCodes: ["PLAN_SELECTED_HIGHEST_UTILITY"],
  rejectionExplanationCodes: { [rejectedId]: ["CARGO_TEMPERATURE_WINDOW_EXCEEDED"] },
  deterministicTieBreakRule: "FINAL_UTILITY_DESC_THEN_PLAN_UUID_ASC",
  explainedAt: now
};

const demoUser = (email: string, role: UserRole): User => ({
  id: "00000000-0000-4000-8100-000000000002",
  name: role === "DRIVER" ? "Demo Driver" : role === "ADMIN" ? "Demo Administrator" : "Demo Dispatcher",
  email,
  phone: "+919999999999",
  role,
  createdAt: now,
  updatedAt: now
});

const delay = async () => new Promise((resolve) => window.setTimeout(resolve, 80));
const acceptedTelemetry = new Set<string>();

export const mockApi: NeepoApi = {
  async login(request: LoginRequest) {
    await delay();
    const role = request.demoRole ?? "DISPATCHER";
    return {
      accessToken: `demo-${role.toLowerCase()}-token`,
      expiresAt: "2026-08-25T13:00:00Z",
      user: demoUser(request.email, role)
    };
  },
  async getTrip(tripId) {
    await delay();
    if (tripId !== DEMO_TRIP_ID) throw new Error("Trip not found in demonstration data.");
    return trip;
  },
  async startTrip(tripId) {
    await delay();
    if (tripId !== DEMO_TRIP_ID) throw new Error("Trip not found in demonstration data.");
    return {
      ...trip,
      status: "IN_PROGRESS",
      startedAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
  },
  async listRecoveryPlans(tripId) {
    await delay();
    return tripId === DEMO_TRIP_ID ? plans : [];
  },
  async getDecisionExplanation(tripId) {
    await delay();
    return tripId === DEMO_TRIP_ID ? explanation : null;
  },
  async generateRecoveryPlans(tripId) {
    await delay();
    return tripId === DEMO_TRIP_ID ? plans : [];
  },
  async ingestTelemetry(observation) {
    await delay();
    const duplicate = acceptedTelemetry.has(observation.eventId);
    acceptedTelemetry.add(observation.eventId);
    return { eventId: observation.eventId, duplicate, acceptedAt: new Date().toISOString() };
  },
  async evaluateTelemetryPolicy(tripId, request) {
    await delay();
    return {
      tripId,
      eventId: request.eventId,
      policyState: request.safelyParked ? "SAFELY_PARKED" : "BREAKDOWN_OR_EMERGENCY",
      intervalSeconds: request.safelyParked ? 120 : 3,
      decidedAt: request.observedAt,
      explanationCode: request.safelyParked ? "SAFELY_PARKED" : "BREAKDOWN_OR_EMERGENCY"
    };
  },
  async issueEventTicket() {
    await delay();
    return { ticket: crypto.randomUUID(), expiresAt: new Date(Date.now() + 30_000).toISOString() };
  }
};
