import type {
  ApiError,
  CreateNotificationChannelRequest,
  DecisionExplanation,
  DisruptionSeverity,
  NotificationChannel,
  NotificationChannelType,
  NotificationDeliveryLog,
  NotificationDeliveryStatus,
  NotificationTestResult,
  RecoveryPlan,
  TelemetryObservation,
  TelemetryPolicyDecision,
  TelemetryPolicyRequest,
  Trip,
  User,
  UserRole,
  UUID
} from "@contracts";

export type {
  CreateNotificationChannelRequest,
  DisruptionSeverity,
  NotificationChannel,
  NotificationChannelType,
  NotificationDeliveryLog,
  NotificationDeliveryStatus,
  NotificationTestResult
};

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

export interface CompanyProfile {
  id: UUID;
  code: string;
  name: string;
  legalName?: string;
  tradingName?: string;
  registrationNumber?: string;
  industry?: string;
  address?: string;
  city?: string;
  stateProvince?: string;
  country?: string;
  postalCode?: string;
  contactPerson?: string;
  businessEmail?: string;
  phoneNumber?: string;
  timezone: string;
  measurementUnits: string;
  cargoTemperaturePolicy: string;
  logoReference?: string;
  dataRetentionDays: number;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateCompanyInput {
  legalName: string;
  tradingName?: string;
  registrationNumber: string;
  industry?: string;
  address?: string;
  city?: string;
  stateProvince?: string;
  country?: string;
  postalCode?: string;
  contactPerson?: string;
  businessEmail: string;
  phoneNumber?: string;
  timezone?: string;
  measurementUnits?: string;
  cargoTemperaturePolicy?: string;
  logoReference?: string;
  dataRetentionDays?: number;
}

export interface UserAdminItem {
  id: UUID;
  name: string;
  email: string;
  phone: string;
  role: UserRole;
  status: "ACTIVE" | "SUSPENDED" | "INVITED";
  lastActiveAt?: string;
  createdAt: string;
  updatedAt: string;
  invitationStatus?: string;
}

export interface CreateUserInput {
  name: string;
  email: string;
  phone: string;
  role: UserRole;
  password?: string;
}

export interface InviteUserInput {
  name: string;
  email: string;
  phone: string;
  role: UserRole;
}

export interface AcceptInvitationInput {
  token: string;
  password: string;
}

export interface UpdateUserInput {
  name: string;
  phone: string;
  role?: UserRole;
}

export interface DriverDetailed {
  id: UUID;
  userId: UUID;
  employeeId?: string;
  firstName?: string;
  lastName?: string;
  name: string;
  email: string;
  phone: string;
  licenseNumber: string;
  licenseCategory: string;
  licenseExpiryDate?: string;
  licenseExpired: boolean;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  baseLocation?: string;
  employmentStatus: string;
  profilePhotoUrl?: string;
  verificationState: string;
  status: "AVAILABLE" | "ASSIGNED" | "DRIVING" | "REST_REQUIRED" | "RESTING" | "OFF_DUTY";
  maximumDailyDrivingMinutes: number;
  remainingDrivingMinutes: number;
  lastRestCompletedAt?: string;
  lastActiveAt?: string;
  assignedVehicleId?: UUID;
  assignedVehicleRegistration?: string;
  createdAt: string;
  updatedAt: string;
  invitationToken?: string;
}

export interface CreateDriverDetailedInput {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  employeeId: string;
  licenseNumber: string;
  licenseCategory?: string;
  licenseExpiryDate?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  baseLocation?: string;
  employmentStatus?: string;
  profilePhotoUrl?: string;
  maximumDailyDrivingMinutes?: number;
}

export interface UpdateDriverInput {
  firstName: string;
  lastName: string;
  phone: string;
  employeeId: string;
  licenseNumber: string;
  licenseCategory?: string;
  licenseExpiryDate?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  baseLocation?: string;
  employmentStatus?: string;
  profilePhotoUrl?: string;
  maximumDailyDrivingMinutes?: number;
}

export interface VehicleDetailed {
  id: UUID;
  registrationNumber: string;
  vin?: string;
  make?: string;
  model?: string;
  year?: number;
  vehicleType: string;
  status: "AVAILABLE" | "ASSIGNED" | "IN_TRANSIT" | "DISRUPTED" | "RECOVERY" | "MAINTENANCE" | "OUT_OF_SERVICE";
  heightMeters: number;
  widthMeters: number;
  grossWeightKg: number;
  payloadCapacityKg: number;
  maximumEngineTemperatureCelsius: number;
  refrigerationSupported: boolean;
  cargoTemperatureMinCelsius?: number;
  cargoTemperatureMaxCelsius?: number;
  deviceIdentifier?: string;
  currentEngineHealthPercent: number;
  assignedDriverId?: UUID;
  assignedDriverName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateVehicleDetailedInput {
  registrationNumber: string;
  vin?: string;
  make?: string;
  model?: string;
  year?: number;
  vehicleType: string;
  heightMeters: number;
  widthMeters: number;
  grossWeightKg: number;
  payloadCapacityKg: number;
  maximumEngineTemperatureCelsius: number;
  refrigerationSupported: boolean;
  cargoTemperatureMinCelsius?: number;
  cargoTemperatureMaxCelsius?: number;
  deviceIdentifier?: string;
  status?: "AVAILABLE" | "MAINTENANCE" | "OUT_OF_SERVICE";
}

export interface UpdateVehicleInput {
  registrationNumber: string;
  vin?: string;
  make?: string;
  model?: string;
  year?: number;
  vehicleType: string;
  heightMeters: number;
  widthMeters: number;
  grossWeightKg: number;
  payloadCapacityKg: number;
  maximumEngineTemperatureCelsius: number;
  refrigerationSupported: boolean;
  cargoTemperatureMinCelsius?: number;
  cargoTemperatureMaxCelsius?: number;
  deviceIdentifier?: string;
}

export interface DriverVehicleAssignment {
  id: UUID;
  companyId: UUID;
  driverId: UUID;
  driverName: string;
  driverLicenseNumber: string;
  driverEmployeeId?: string;
  vehicleId: UUID;
  vehicleRegistrationNumber: string;
  vehicleType: string;
  startTime: string;
  endTime?: string;
  notes?: string;
  status: "ACTIVE" | "RELEASED" | "CANCELLED";
  assignedByUserId?: UUID;
  createdAt: string;
  releasedAt?: string;
}

export interface CreateAssignmentInput {
  driverId: UUID;
  vehicleId: UUID;
  startTime?: string;
  endTime?: string;
  notes?: string;
}

export interface AuditEventItem {
  id: UUID;
  traceId: UUID;
  actorUserId?: UUID;
  eventType: string;
  aggregateType: string;
  aggregateId: UUID;
  occurredAt: string;
  details: string;
  createdAt: string;
}

export interface TelemetryHistoryPoint {
  eventId: UUID;
  recordedAt: string;
  latitude: number;
  longitude: number;
  engineTemperatureCelsius?: number | null;
  cargoTemperatureCelsius?: number | null;
  speedKph?: number | null;
  confidence: number;
  observationStatus: "FRESH" | "AGING" | "EXPIRED" | "UNVERIFIED";
  source: "SIMULATOR" | "DRIVER_DEVICE" | "VEHICLE_DEVICE";
  networkState: "ONLINE" | "WEAK" | "OFFLINE" | "SYNCHRONIZING";
}

export interface TelemetryHistoryQueryOptions {
  from?: string;
  to?: string;
  limit?: number;
}

export interface NeepoApi {
  login(request: LoginRequest): Promise<Session>;
  getTrip(tripId: UUID, accessToken: string): Promise<Trip>;
  startTrip(tripId: UUID, accessToken: string): Promise<Trip>;
  listRecoveryPlans(tripId: UUID, accessToken: string): Promise<readonly RecoveryPlan[]>;
  getDecisionExplanation(tripId: UUID, accessToken: string): Promise<DecisionExplanation | null>;
  generateRecoveryPlans(tripId: UUID, accessToken: string): Promise<readonly RecoveryPlan[]>;
  ingestTelemetry(observation: TelemetryObservation, accessToken: string): Promise<TelemetryAccepted>;
  getTelemetryHistory(tripId: UUID, options?: TelemetryHistoryQueryOptions, accessToken?: string): Promise<readonly TelemetryHistoryPoint[]>;
  evaluateTelemetryPolicy(tripId: UUID, request: TelemetryPolicyRequest, accessToken: string): Promise<TelemetryPolicyDecision>;
  issueEventTicket(accessToken: string): Promise<WebSocketTicket>;

  // Onboarding & Admin APIs
  getCompany(accessToken: string): Promise<CompanyProfile>;
  updateCompany(input: UpdateCompanyInput, accessToken: string): Promise<CompanyProfile>;
  getCompanyAudit(accessToken: string): Promise<readonly AuditEventItem[]>;

  listUsers(page: number, size: number, accessToken: string): Promise<{ items: readonly UserAdminItem[]; total: number }>;
  createUser(input: CreateUserInput, accessToken: string): Promise<UserAdminItem>;
  inviteUser(input: InviteUserInput, accessToken: string): Promise<{ token: string; email: string; role: string }>;
  acceptInvitation(input: AcceptInvitationInput): Promise<User>;
  revokeInvitation(invitationId: string, accessToken: string): Promise<void>;
  updateUser(userId: string, input: UpdateUserInput, accessToken: string): Promise<UserAdminItem>;
  suspendUser(userId: string, accessToken: string): Promise<UserAdminItem>;
  reactivateUser(userId: string, accessToken: string): Promise<UserAdminItem>;
  deleteUser(userId: string, accessToken: string): Promise<void>;

  listDrivers(accessToken: string): Promise<readonly DriverDetailed[]>;
  createDriver(input: CreateDriverDetailedInput, accessToken: string): Promise<DriverDetailed>;
  getDriver(driverId: string, accessToken: string): Promise<DriverDetailed>;
  getMyDriverProfile(accessToken: string): Promise<DriverDetailed>;
  updateDriver(driverId: string, input: UpdateDriverInput, accessToken: string): Promise<DriverDetailed>;
  inviteDriver(driverId: string, accessToken: string): Promise<{ token: string }>;
  suspendDriver(driverId: string, accessToken: string): Promise<DriverDetailed>;
  reactivateDriver(driverId: string, accessToken: string): Promise<DriverDetailed>;

  listVehicles(accessToken: string): Promise<readonly VehicleDetailed[]>;
  createVehicle(input: CreateVehicleDetailedInput, accessToken: string): Promise<VehicleDetailed>;
  getVehicle(vehicleId: string, accessToken: string): Promise<VehicleDetailed>;
  updateVehicle(vehicleId: string, input: UpdateVehicleInput, accessToken: string): Promise<VehicleDetailed>;
  updateVehicleStatus(vehicleId: string, status: string, accessToken: string): Promise<VehicleDetailed>;

  listAssignments(accessToken: string): Promise<readonly DriverVehicleAssignment[]>;
  createAssignment(input: CreateAssignmentInput, accessToken: string): Promise<DriverVehicleAssignment>;
  releaseAssignment(assignmentId: string, accessToken: string): Promise<DriverVehicleAssignment>;

  listNotificationChannels(accessToken: string): Promise<readonly NotificationChannel[]>;
  createNotificationChannel(input: CreateNotificationChannelRequest, accessToken: string): Promise<NotificationChannel>;
  deleteNotificationChannel(channelId: string, accessToken: string): Promise<void>;
  testNotificationChannel(channelId: string, accessToken: string): Promise<NotificationTestResult>;
  listNotificationLogs(accessToken: string, limit?: number): Promise<readonly NotificationDeliveryLog[]>;
}

export class ApiClientError extends Error {
  constructor(readonly status: number, readonly response?: ApiError) {
    super(response?.error.message ?? "The request could not be completed.");
    this.name = "ApiClientError";
  }
}
