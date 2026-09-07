import type { DecisionExplanation, PlanEvaluation, RecoveryPlan, Trip, User, UserRole } from "@contracts";
import type {
  CompanyProfile,
  DriverDetailed,
  DriverVehicleAssignment,
  LoginRequest,
  NeepoApi,
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
  }
};
