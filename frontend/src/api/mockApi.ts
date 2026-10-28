import type { DecisionExplanation, PlanEvaluation, RecoveryPlan, Trip, User, UserRole } from "@contracts";
import type {
  CompanyProfile,
  ComplianceExportManifest,
  CreateNotificationChannelRequest,
  DriverDetailed,
  DriverVehicleAssignment,
  LoginRequest,
  NeepoApi,
  NotificationChannel,
  NotificationDeliveryLog,
  NotificationTestResult,
  TelemetryHistoryPoint,
  UserAdminItem,
  VehicleDetailed
} from "./types";

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

const demoTelemetryHistory: TelemetryHistoryPoint[] = [
  {
    eventId: "00000000-0000-4000-8800-000000000001",
    recordedAt: "2026-08-24T12:45:00Z",
    latitude: 12.9716,
    longitude: 77.5946,
    engineTemperatureCelsius: 82.0,
    cargoTemperatureCelsius: 4.0,
    speedKph: 0.0,
    confidence: 0.99,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  },
  {
    eventId: "00000000-0000-4000-8800-000000000002",
    recordedAt: "2026-08-24T12:47:00Z",
    latitude: 12.9820,
    longitude: 77.5950,
    engineTemperatureCelsius: 85.5,
    cargoTemperatureCelsius: 4.0,
    speedKph: 38.4,
    confidence: 0.99,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  },
  {
    eventId: "00000000-0000-4000-8800-000000000003",
    recordedAt: "2026-08-24T12:49:00Z",
    latitude: 12.9940,
    longitude: 77.5955,
    engineTemperatureCelsius: 88.2,
    cargoTemperatureCelsius: 4.1,
    speedKph: 46.1,
    confidence: 0.98,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  },
  {
    eventId: "00000000-0000-4000-8800-000000000004",
    recordedAt: "2026-08-24T12:51:00Z",
    latitude: 13.0060,
    longitude: 77.5960,
    engineTemperatureCelsius: 91.8,
    cargoTemperatureCelsius: 4.1,
    speedKph: 52.0,
    confidence: 0.98,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  },
  {
    eventId: "00000000-0000-4000-8800-000000000005",
    recordedAt: "2026-08-24T12:53:00Z",
    latitude: 13.0180,
    longitude: 77.5965,
    engineTemperatureCelsius: 95.4,
    cargoTemperatureCelsius: 4.1,
    speedKph: 48.7,
    confidence: 0.97,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  },
  {
    eventId: "00000000-0000-4000-8800-000000000006",
    recordedAt: "2026-08-24T12:55:00Z",
    latitude: 13.0270,
    longitude: 77.5968,
    engineTemperatureCelsius: 101.5,
    cargoTemperatureCelsius: 4.2,
    speedKph: 41.2,
    confidence: 0.97,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  },
  {
    eventId: "00000000-0000-4000-8800-000000000007",
    recordedAt: "2026-08-24T12:57:00Z",
    latitude: 13.0320,
    longitude: 77.5969,
    engineTemperatureCelsius: 104.8,
    cargoTemperatureCelsius: 4.2,
    speedKph: 24.5,
    confidence: 0.96,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  },
  {
    eventId: "00000000-0000-4000-8800-000000000008",
    recordedAt: "2026-08-24T12:59:00Z",
    latitude: 13.0358,
    longitude: 77.5970,
    engineTemperatureCelsius: 106.2,
    cargoTemperatureCelsius: 4.2,
    speedKph: 0.0,
    confidence: 0.98,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  },
  {
    eventId: "00000000-0000-4000-8800-000000000009",
    recordedAt: "2026-08-24T13:00:00Z",
    latitude: 13.0358,
    longitude: 77.5970,
    engineTemperatureCelsius: 105.8,
    cargoTemperatureCelsius: 4.3,
    speedKph: 0.0,
    confidence: 0.98,
    observationStatus: "FRESH",
    source: "VEHICLE_DEVICE",
    networkState: "ONLINE"
  }
];


const demoUser = (email: string, role: UserRole): User => ({
  id: "00000000-0000-4000-8100-000000000002",
  name: role === "DRIVER" ? "Demo Driver" : role === "ADMIN" ? "Demo Administrator" : "Demo Dispatcher",
  email,
  phone: "+15550100001",
  role,
  createdAt: now,
  updatedAt: now
});

let mockCompany: CompanyProfile = {
  id: "00000000-0000-4000-8000-000000000001",
  code: "NEEPO-CORP",
  name: "Neepo Cold Chain Logistics Inc.",
  legalName: "Neepo Cold Chain Logistics Inc.",
  tradingName: "Neepo Logistics",
  registrationNumber: "REG-DEMO-001",
  industry: "Cold Chain Transport & Pharmaceuticals",
  address: "100 Logistics Blvd, Suite 400",
  city: "San Francisco",
  stateProvince: "CA",
  country: "USA",
  postalCode: "94105",
  contactPerson: "Operations Lead",
  businessEmail: "ops@neepo.local",
  phoneNumber: "+15550100000",
  timezone: "UTC",
  measurementUnits: "METRIC",
  cargoTemperaturePolicy: "STRICT_COLD_CHAIN",
  logoReference: "/neepo-logo.jpg",
  dataRetentionDays: 90,
  createdAt: now,
  updatedAt: now
};

const mockUsers: UserAdminItem[] = [
  {
    id: "00000000-0000-4000-8100-000000000001",
    name: "Demo Administrator",
    email: "admin@neepo.local",
    phone: "+15550100000",
    role: "ADMIN",
    status: "ACTIVE",
    createdAt: now,
    updatedAt: now,
    invitationStatus: "ACTIVE"
  },
  {
    id: "00000000-0000-4000-8100-000000000002",
    name: "Demo Dispatcher",
    email: "dispatcher@neepo.local",
    phone: "+15550100001",
    role: "DISPATCHER",
    status: "ACTIVE",
    createdAt: now,
    updatedAt: now,
    invitationStatus: "ACTIVE"
  },
  {
    id: "00000000-0000-4000-8100-000000000003",
    name: "Demo Driver",
    email: "driver@neepo.local",
    phone: "+15550100002",
    role: "DRIVER",
    status: "ACTIVE",
    createdAt: now,
    updatedAt: now,
    invitationStatus: "ACTIVE"
  }
];

const mockDrivers: DriverDetailed[] = [
  {
    id: "00000000-0000-4000-8200-000000000001",
    userId: "00000000-0000-4000-8100-000000000003",
    employeeId: "EMP-DRV-001",
    firstName: "Demo",
    lastName: "Driver",
    name: "Demo Driver",
    email: "driver@neepo.local",
    phone: "+15550100002",
    licenseNumber: "DL-DEMO-001",
    licenseCategory: "HEAVY_RIGID",
    licenseExpiryDate: "2027-08-24",
    licenseExpired: false,
    emergencyContactName: "Support Team",
    emergencyContactPhone: "+15550100099",
    baseLocation: "San Francisco Central Hub",
    employmentStatus: "FULL_TIME",
    verificationState: "VERIFIED",
    status: "ASSIGNED",
    maximumDailyDrivingMinutes: 600,
    remainingDrivingMinutes: 480,
    assignedVehicleId: "00000000-0000-4000-8300-000000000001",
    assignedVehicleRegistration: "CA-94105-REEFER",
    createdAt: now,
    updatedAt: now
  }
];

const mockVehicles: VehicleDetailed[] = [
  {
    id: "00000000-0000-4000-8300-000000000001",
    registrationNumber: "CA-94105-REEFER",
    vin: "1HGCR2F8XEA123456",
    make: "Volvo",
    model: "FH Electric Reefer",
    year: 2025,
    vehicleType: "REFRIGERATED_TRUCK",
    status: "ASSIGNED",
    heightMeters: 3.8,
    widthMeters: 2.5,
    grossWeightKg: 18000,
    payloadCapacityKg: 8500,
    maximumEngineTemperatureCelsius: 105.0,
    refrigerationSupported: true,
    cargoTemperatureMinCelsius: -25.0,
    cargoTemperatureMaxCelsius: 10.0,
    deviceIdentifier: "DEV-OBD-001",
    currentEngineHealthPercent: 94.0,
    assignedDriverId: "00000000-0000-4000-8200-000000000001",
    assignedDriverName: "Demo Driver",
    createdAt: now,
    updatedAt: now
  }
];

const mockAssignments: DriverVehicleAssignment[] = [
  {
    id: "30000000-0000-4000-9000-000000000001",
    companyId: "00000000-0000-4000-8000-000000000001",
    driverId: "00000000-0000-4000-8200-000000000001",
    driverName: "Demo Driver",
    driverLicenseNumber: "DL-DEMO-001",
    driverEmployeeId: "EMP-DRV-001",
    vehicleId: "00000000-0000-4000-8300-000000000001",
    vehicleRegistrationNumber: "CA-94105-REEFER",
    vehicleType: "REFRIGERATED_TRUCK",
    startTime: now,
    notes: "Assigned for cold chain transit.",
    status: "ACTIVE",
    createdAt: now
  }
];

const mockNotificationChannels: NotificationChannel[] = [
  {
    id: "00000000-0000-4000-8500-000000000001",
    companyId: "00000000-0000-4000-8000-000000000001",
    name: "Operations Center Slack Webhook",
    channelType: "WEBHOOK",
    targetEndpoint: "https://hooks.slack.com/services/NEEPO/OPS/ALERT_CHANNEL",
    secretKey: "neep...2026",
    minimumSeverity: "MEDIUM",
    active: true,
    createdAt: "2026-08-24T10:00:00Z"
  },
  {
    id: "00000000-0000-4000-8500-000000000002",
    companyId: "00000000-0000-4000-8000-000000000001",
    name: "Fleet Emergency PagerDuty",
    channelType: "WEBHOOK",
    targetEndpoint: "https://events.pagerduty.com/v2/enqueue",
    secretKey: "neep...2026",
    minimumSeverity: "HIGH",
    active: true,
    createdAt: "2026-08-24T10:30:00Z"
  }
];

const mockNotificationLogs: NotificationDeliveryLog[] = [
  {
    id: "00000000-0000-4000-8500-000000000101",
    channelId: "00000000-0000-4000-8500-000000000001",
    companyId: "00000000-0000-4000-8000-000000000001",
    disruptionType: "SYSTEM_STARTUP",
    severity: "LOW",
    status: "DELIVERED",
    signature: "a1b2c3d4e5f67890abcdef1234567890abcdef1234567890abcdef1234567890",
    payload: '{"event":"SYSTEM_STARTUP","status":"READY"}',
    deliveredAt: "2026-08-24T12:00:00Z"
  },
  {
    id: "00000000-0000-4000-8500-000000000102",
    channelId: "00000000-0000-4000-8500-000000000001",
    companyId: "00000000-0000-4000-8000-000000000001",
    disruptionType: "VEHICLE_OVERHEATING",
    severity: "HIGH",
    status: "DELIVERED",
    signature: "7e5d89f023b1c4e98f01a34567890abcdef1234567890abcdef1234567890ab",
    payload: '{"event":"DISRUPTION_DETECTED","type":"VEHICLE_OVERHEATING","severity":"HIGH","tempCelsius":106.2}',
    deliveredAt: "2026-08-24T12:59:30Z"
  }
];

async function computeSha256(text: string): Promise<string> {
  if (typeof crypto !== "undefined" && crypto.subtle && typeof crypto.subtle.digest === "function") {
    try {
      const encoder = new TextEncoder();
      const data = encoder.encode(text);
      const hashBuffer = await crypto.subtle.digest("SHA-256", data);
      const hashArray = Array.from(new Uint8Array(hashBuffer));
      return hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");
    } catch {
      // Fall through to fallback
    }
  }
  let hash = 0;
  for (let i = 0; i < text.length; i++) {
    hash = ((hash << 5) - hash) + text.charCodeAt(i);
    hash |= 0;
  }
  const hex = Math.abs(hash).toString(16).padStart(8, "0");
  return (hex + hex + hex + hex + hex + hex + hex + hex).substring(0, 64);
}

const mockComplianceManifests: ComplianceExportManifest[] = [
  {
    id: "00000000-0000-4000-8900-000000000001",
    companyId: "00000000-0000-4000-8000-000000000001",
    tripId: DEMO_TRIP_ID,
    exportType: "FULL_TRIP_DOSSIER",
    format: "JSON_LD",
    sha256Digest: "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
    recordCount: 42,
    generatedByUserId: "00000000-0000-4000-8100-000000000001",
    metadata: {
      regulatoryStandard: "FDA 21 CFR Part 11 / EU GDP Guidelines",
      systemVersion: "neepo-care-1.1.0"
    },
    createdAt: "2026-08-24T13:05:00Z"
  },
  {
    id: "00000000-0000-4000-8900-000000000002",
    companyId: "00000000-0000-4000-8000-000000000001",
    tripId: DEMO_TRIP_ID,
    exportType: "COLD_CHAIN_EXCURSION",
    format: "CSV",
    sha256Digest: "4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
    recordCount: 15,
    generatedByUserId: "00000000-0000-4000-8100-000000000001",
    metadata: {
      regulatoryStandard: "FDA 21 CFR Part 11 / EU GDP Guidelines",
      systemVersion: "neepo-care-1.1.0"
    },
    createdAt: "2026-08-24T12:50:00Z"
  }
];

const delay = async () =>
  new Promise((resolve) => window.setTimeout(resolve, typeof window !== "undefined" && (window as unknown as { __VITEST_ENVIRONMENT__?: unknown }).__VITEST_ENVIRONMENT__ ? 0 : 80));
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
  async getTelemetryHistory(tripId, options) {
    await delay();
    if (tripId !== DEMO_TRIP_ID) return [];
    let items = demoTelemetryHistory;
    if (options?.from) {
      items = items.filter((pt) => pt.recordedAt >= options.from!);
    }
    if (options?.to) {
      items = items.filter((pt) => pt.recordedAt <= options.to!);
    }
    if (options?.limit && options.limit > 0) {
      items = items.slice(0, options.limit);
    }
    return items;
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
  },

  // Company Profile
  async getCompany() {
    await delay();
    return mockCompany;
  },
  async updateCompany(input) {
    await delay();
    mockCompany = {
      ...mockCompany,
      ...input,
      updatedAt: new Date().toISOString()
    };
    return mockCompany;
  },
  async getCompanyAudit() {
    await delay();
    return [
      {
        id: crypto.randomUUID(),
        traceId: crypto.randomUUID(),
        eventType: "COMPANY_PROFILE_UPDATED",
        aggregateType: "COMPANY",
        aggregateId: mockCompany.id,
        occurredAt: new Date().toISOString(),
        details: JSON.stringify({ updatedBy: "admin@neepo.local" }),
        createdAt: new Date().toISOString()
      }
    ];
  },

  // Users
  async listUsers(page, size) {
    await delay();
    const start = page * size;
    return { items: mockUsers.slice(start, start + size), total: mockUsers.length };
  },
  async createUser(input) {
    await delay();
    const u: UserAdminItem = {
      id: crypto.randomUUID(),
      name: input.name,
      email: input.email,
      phone: input.phone,
      role: input.role,
      status: "ACTIVE",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    mockUsers.push(u);
    return u;
  },
  async inviteUser(input) {
    await delay();
    const token = crypto.randomUUID().replace(/-/g, "");
    const u: UserAdminItem = {
      id: crypto.randomUUID(),
      name: input.name,
      email: input.email,
      phone: input.phone,
      role: input.role,
      status: "INVITED",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      invitationStatus: "PENDING"
    };
    mockUsers.push(u);
    return { token, email: input.email, role: input.role };
  },
  async acceptInvitation(_input) {
    void _input;
    await delay();
    const found = mockUsers.find((u) => u.status === "INVITED") ?? mockUsers[0];
    found.status = "ACTIVE";
    found.invitationStatus = "ACCEPTED";
    return {
      id: found.id,
      name: found.name,
      email: found.email,
      phone: found.phone,
      role: found.role,
      createdAt: found.createdAt,
      updatedAt: new Date().toISOString()
    };
  },
  async revokeInvitation(_invitationId) {
    void _invitationId;
    await delay();
  },
  async updateUser(userId, input) {
    await delay();
    const found = mockUsers.find((u) => u.id === userId);
    if (found) {
      found.name = input.name;
      found.phone = input.phone;
      if (input.role) found.role = input.role;
      found.updatedAt = new Date().toISOString();
      return found;
    }
    throw new Error("User not found");
  },
  async suspendUser(userId) {
    await delay();
    const found = mockUsers.find((u) => u.id === userId);
    if (found) {
      found.status = "SUSPENDED";
      found.updatedAt = new Date().toISOString();
      return found;
    }
    throw new Error("User not found");
  },
  async reactivateUser(userId) {
    await delay();
    const found = mockUsers.find((u) => u.id === userId);
    if (found) {
      found.status = "ACTIVE";
      found.updatedAt = new Date().toISOString();
      return found;
    }
    throw new Error("User not found");
  },
  async deleteUser(userId) {
    await delay();
    const idx = mockUsers.findIndex((u) => u.id === userId);
    if (idx >= 0) mockUsers.splice(idx, 1);
  },

  // Drivers
  async listDrivers() {
    await delay();
    return mockDrivers;
  },
  async createDriver(input) {
    await delay();
    const d: DriverDetailed = {
      id: crypto.randomUUID(),
      userId: crypto.randomUUID(),
      employeeId: input.employeeId,
      firstName: input.firstName,
      lastName: input.lastName,
      name: `${input.firstName} ${input.lastName}`.trim(),
      email: input.email,
      phone: input.phone,
      licenseNumber: input.licenseNumber,
      licenseCategory: input.licenseCategory ?? "HEAVY_RIGID",
      licenseExpiryDate: input.licenseExpiryDate,
      licenseExpired: false,
      emergencyContactName: input.emergencyContactName,
      emergencyContactPhone: input.emergencyContactPhone,
      baseLocation: input.baseLocation,
      employmentStatus: input.employmentStatus ?? "FULL_TIME",
      profilePhotoUrl: input.profilePhotoUrl,
      verificationState: "VERIFIED",
      status: "AVAILABLE",
      maximumDailyDrivingMinutes: input.maximumDailyDrivingMinutes ?? 600,
      remainingDrivingMinutes: input.maximumDailyDrivingMinutes ?? 600,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      invitationToken: crypto.randomUUID().replace(/-/g, "")
    };
    mockDrivers.push(d);
    return d;
  },
  async getDriver(driverId) {
    await delay();
    const found = mockDrivers.find((d) => d.id === driverId);
    if (found) return found;
    throw new Error("Driver not found");
  },
  async getMyDriverProfile() {
    await delay();
    return mockDrivers[0];
  },
  async updateDriver(driverId, input) {
    await delay();
    const found = mockDrivers.find((d) => d.id === driverId);
    if (found) {
      Object.assign(found, input, {
        name: `${input.firstName} ${input.lastName}`.trim(),
        updatedAt: new Date().toISOString()
      });
      return found;
    }
    throw new Error("Driver not found");
  },
  async inviteDriver() {
    await delay();
    return { token: crypto.randomUUID().replace(/-/g, "") };
  },
  async suspendDriver(driverId) {
    await delay();
    const found = mockDrivers.find((d) => d.id === driverId);
    if (found) {
      found.verificationState = "SUSPENDED";
      found.status = "OFF_DUTY";
      found.updatedAt = new Date().toISOString();
      return found;
    }
    throw new Error("Driver not found");
  },
  async reactivateDriver(driverId) {
    await delay();
    const found = mockDrivers.find((d) => d.id === driverId);
    if (found) {
      found.verificationState = "VERIFIED";
      found.status = "AVAILABLE";
      found.updatedAt = new Date().toISOString();
      return found;
    }
    throw new Error("Driver not found");
  },

  // Vehicles
  async listVehicles() {
    await delay();
    return mockVehicles;
  },
  async createVehicle(input) {
    await delay();
    const v: VehicleDetailed = {
      id: crypto.randomUUID(),
      registrationNumber: input.registrationNumber,
      vin: input.vin,
      make: input.make,
      model: input.model,
      year: input.year,
      vehicleType: input.vehicleType,
      status: input.status ?? "AVAILABLE",
      heightMeters: input.heightMeters,
      widthMeters: input.widthMeters,
      grossWeightKg: input.grossWeightKg,
      payloadCapacityKg: input.payloadCapacityKg,
      maximumEngineTemperatureCelsius: input.maximumEngineTemperatureCelsius,
      refrigerationSupported: input.refrigerationSupported,
      cargoTemperatureMinCelsius: input.cargoTemperatureMinCelsius,
      cargoTemperatureMaxCelsius: input.cargoTemperatureMaxCelsius,
      deviceIdentifier: input.deviceIdentifier,
      currentEngineHealthPercent: 100,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    mockVehicles.push(v);
    return v;
  },
  async getVehicle(vehicleId) {
    await delay();
    const found = mockVehicles.find((v) => v.id === vehicleId);
    if (found) return found;
    throw new Error("Vehicle not found");
  },
  async updateVehicle(vehicleId, input) {
    await delay();
    const found = mockVehicles.find((v) => v.id === vehicleId);
    if (found) {
      Object.assign(found, input, { updatedAt: new Date().toISOString() });
      return found;
    }
    throw new Error("Vehicle not found");
  },
  async updateVehicleStatus(vehicleId, status) {
    await delay();
    const found = mockVehicles.find((v) => v.id === vehicleId);
    if (found) {
      found.status = status as VehicleDetailed["status"];
      found.updatedAt = new Date().toISOString();
      return found;
    }
    throw new Error("Vehicle not found");
  },

  // Assignments
  async listAssignments() {
    await delay();
    return mockAssignments;
  },
  async createAssignment(input) {
    await delay();
    const driver = mockDrivers.find((d) => d.id === input.driverId) ?? mockDrivers[0];
    const vehicle = mockVehicles.find((v) => v.id === input.vehicleId) ?? mockVehicles[0];
    const a: DriverVehicleAssignment = {
      id: crypto.randomUUID(),
      companyId: mockCompany.id,
      driverId: input.driverId,
      driverName: driver.name,
      driverLicenseNumber: driver.licenseNumber,
      driverEmployeeId: driver.employeeId,
      vehicleId: input.vehicleId,
      vehicleRegistrationNumber: vehicle.registrationNumber,
      vehicleType: vehicle.vehicleType,
      startTime: input.startTime ?? new Date().toISOString(),
      endTime: input.endTime,
      notes: input.notes,
      status: "ACTIVE",
      createdAt: new Date().toISOString()
    };
    mockAssignments.unshift(a);
    driver.status = "ASSIGNED";
    driver.assignedVehicleId = vehicle.id;
    driver.assignedVehicleRegistration = vehicle.registrationNumber;
    vehicle.status = "ASSIGNED";
    vehicle.assignedDriverId = driver.id;
    vehicle.assignedDriverName = driver.name;
    return a;
  },
  async releaseAssignment(assignmentId) {
    await delay();
    const found = mockAssignments.find((a) => a.id === assignmentId);
    if (found) {
      found.status = "RELEASED";
      found.releasedAt = new Date().toISOString();
      const driver = mockDrivers.find((d) => d.id === found.driverId);
      if (driver) {
        driver.status = "AVAILABLE";
        driver.assignedVehicleId = undefined;
        driver.assignedVehicleRegistration = undefined;
      }
      const vehicle = mockVehicles.find((v) => v.id === found.vehicleId);
      if (vehicle) {
        vehicle.status = "AVAILABLE";
        vehicle.assignedDriverId = undefined;
        vehicle.assignedDriverName = undefined;
      }
      return found;
    }
    throw new Error("Assignment not found");
  },

  // Notification Channels & Alerting
  async listNotificationChannels() {
    await delay();
    return mockNotificationChannels;
  },
  async createNotificationChannel(input: CreateNotificationChannelRequest) {
    await delay();
    const maskedSecret = input.secretKey
      ? input.secretKey.length > 8
        ? `${input.secretKey.substring(0, 4)}...${input.secretKey.substring(input.secretKey.length - 4)}`
        : "********"
      : "whse...2026";
    const channel: NotificationChannel = {
      id: crypto.randomUUID(),
      companyId: mockCompany.id,
      name: input.name,
      channelType: input.channelType,
      targetEndpoint: input.targetEndpoint,
      secretKey: maskedSecret,
      minimumSeverity: input.minimumSeverity,
      active: input.active ?? true,
      createdAt: new Date().toISOString()
    };
    mockNotificationChannels.unshift(channel);
    return channel;
  },
  async deleteNotificationChannel(channelId: string) {
    await delay();
    const idx = mockNotificationChannels.findIndex((c) => c.id === channelId);
    if (idx >= 0) mockNotificationChannels.splice(idx, 1);
  },
  async testNotificationChannel(channelId: string) {
    await delay();
    const channel = mockNotificationChannels.find((c) => c.id === channelId);
    if (!channel) throw new Error("Notification channel not found");
    const payload = JSON.stringify({
      event: "TEST_NOTIFICATION",
      channelId,
      channelName: channel.name,
      timestamp: new Date().toISOString(),
      message: "NEEPO-CARE test notification ping"
    });
    const signature = "mock_hmac_sha256_" + crypto.randomUUID().replace(/-/g, "").substring(0, 32);
    const result: NotificationTestResult = {
      channelId,
      status: "DELIVERED",
      message: "Test notification ping dispatched and signed successfully",
      signature,
      payload
    };
    mockNotificationLogs.unshift({
      id: crypto.randomUUID(),
      channelId,
      companyId: mockCompany.id,
      disruptionType: "TEST_PING",
      severity: channel.minimumSeverity,
      status: "DELIVERED",
      signature,
      payload,
      deliveredAt: new Date().toISOString()
    });
    return result;
  },
  async listNotificationLogs(_token: string, limit = 50) {
    await delay();
    return mockNotificationLogs.slice(0, limit);
  },

  // Compliance & Audit Evidence APIs
  async generateComplianceExport(tripId, exportType, exportFormat) {
    await delay();
    let rawPayload = "";
    let mediaType = "application/json";
    let filename = `compliance-${tripId.substring(0, 8)}.${exportFormat.toLowerCase()}`;
    const timestamp = new Date().toISOString();

    if (exportFormat === "JSON_LD") {
      mediaType = "application/ld+json";
      filename = `compliance-dossier-${tripId.substring(0, 8)}.jsonld`;
      rawPayload = JSON.stringify(
        {
          "@context": "https://neepo-care.io/schema/compliance-v1.jsonld",
          "@type": "ComplianceAuditPackage",
          tripId,
          exportType,
          regulatoryStandards: ["FDA 21 CFR Part 11", "EU GDP 2013/C 343/01"],
          generatedAt: timestamp,
          recordCount: demoTelemetryHistory.length,
          trip: {
            id: trip.id,
            referenceCode: trip.referenceCode,
            status: trip.status,
            deliveryDeadline: trip.deliveryDeadline
          },
          telemetryRecords: demoTelemetryHistory,
          recoveryPlans: plans,
          decisionExplanation: explanation
        },
        null,
        2
      );
    } else if (exportFormat === "CSV") {
      mediaType = "text/csv";
      filename = `compliance-dossier-${tripId.substring(0, 8)}.csv`;
      const lines = [
        `# NEEPO-CARE Compliance Evidence Export - FDA 21 CFR Part 11 / EU GDP`,
        `# Export Type: ${exportType}`,
        `# Trip ID: ${tripId}`,
        `# Generated At: ${timestamp}`,
        `timestamp,latitude,longitude,cargoTempC,engineTempC,speedKph,status`,
        ...demoTelemetryHistory.map(
          (t) =>
            `${t.recordedAt},${t.latitude},${t.longitude},${t.cargoTemperatureCelsius ?? ""},${t.engineTemperatureCelsius ?? ""},${t.speedKph ?? ""},${t.observationStatus}`
        )
      ];
      rawPayload = lines.join("\n");
    } else {
      mediaType = "text/html";
      filename = `compliance-dossier-${tripId.substring(0, 8)}.html`;
      rawPayload = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <title>NEEPO-CARE Regulatory Compliance Dossier</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 2rem; color: #1e293b; }
    h1 { color: #0f172a; border-bottom: 2px solid #0284c7; padding-bottom: 0.5rem; }
    .badge { background: #e0f2fe; color: #0369a1; padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.85rem; font-weight: 600; }
    table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
    th, td { border: 1px solid #cbd5e1; padding: 0.5rem; text-align: left; }
    th { background: #f8fafc; }
  </style>
</head>
<body>
  <h1>NEEPO-CARE Compliance Evidence Dossier</h1>
  <p><span class="badge">FDA 21 CFR Part 11 &amp; EU GDP Compliant</span></p>
  <p><strong>Trip:</strong> ${trip.referenceCode} (${tripId})</p>
  <p><strong>Export Type:</strong> ${exportType}</p>
  <p><strong>Exported At:</strong> ${timestamp}</p>
  <h2>Telemetry &amp; Audit Trail (${demoTelemetryHistory.length} records)</h2>
  <table>
    <thead><tr><th>Time</th><th>Lat</th><th>Lng</th><th>Cargo Temp</th><th>Status</th></tr></thead>
    <tbody>
      ${demoTelemetryHistory.map((t) => `<tr><td>${t.recordedAt}</td><td>${t.latitude}</td><td>${t.longitude}</td><td>${t.cargoTemperatureCelsius ?? "N/A"}°C</td><td>${t.observationStatus}</td></tr>`).join("")}
    </tbody>
  </table>
</body>
</html>`;
    }

    const sha256Digest = await computeSha256(rawPayload);
    const manifest: ComplianceExportManifest = {
      id: crypto.randomUUID(),
      companyId: "00000000-0000-4000-8000-000000000001",
      tripId,
      exportType,
      format: exportFormat,
      sha256Digest,
      recordCount: demoTelemetryHistory.length,
      generatedByUserId: "00000000-0000-4000-8100-000000000001",
      metadata: {
        regulatoryStandard: "FDA 21 CFR Part 11 / EU GDP Guidelines",
        systemVersion: "neepo-care-1.1.0"
      },
      createdAt: timestamp
    };
    mockComplianceManifests.unshift(manifest);

    return {
      manifest,
      rawPayload,
      mediaType,
      filename
    };
  },

  async listComplianceExports(_limit = 50) {
    await delay();
    return mockComplianceManifests.slice(0, _limit);
  },

  async getComplianceExportManifest(manifestId) {
    await delay();
    const manifest = mockComplianceManifests.find((m) => m.id === manifestId);
    if (!manifest) throw new Error("Compliance manifest not found");
    return manifest;
  },

  async verifyComplianceExport(manifestId, request) {
    await delay();
    const manifest = mockComplianceManifests.find((m) => m.id === manifestId);
    const computedDigest = await computeSha256(request.payload);
    const expectedDigest = manifest ? manifest.sha256Digest : "0".repeat(64);
    const valid = manifest !== undefined && computedDigest === expectedDigest;

    return {
      manifestId,
      valid,
      expectedDigest,
      computedDigest,
      verifiedAt: new Date().toISOString(),
      message: valid
        ? "Cryptographic verification passed. Payload bit-level integrity verified against tamper-evident manifest."
        : "Tamper detected! Computed SHA-256 digest does not match the signed manifest digest."
    };
  }
};


