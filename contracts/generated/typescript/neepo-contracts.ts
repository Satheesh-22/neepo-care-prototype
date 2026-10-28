// Generated contract baseline. Do not rename shared fields without team approval.

export type UUID = string;
export type IsoInstant = string;

export type UserRole = "ADMIN" | "DISPATCHER" | "DRIVER";
export type DriverStatus =
  | "AVAILABLE"
  | "ASSIGNED"
  | "DRIVING"
  | "REST_REQUIRED"
  | "RESTING"
  | "OFF_DUTY";
export type VehicleStatus =
  | "AVAILABLE"
  | "ASSIGNED"
  | "IN_TRANSIT"
  | "DISRUPTED"
  | "RECOVERY"
  | "MAINTENANCE"
  | "OUT_OF_SERVICE";
export type CargoType = "GENERAL" | "REFRIGERATED" | "FROZEN" | "HAZARDOUS" | "FRAGILE";
export type ProviderAvailabilityStatus = "AVAILABLE" | "BUSY" | "UNAVAILABLE" | "CANCELLED";
export type RouteStatus = "PLANNED" | "ACTIVE" | "BLOCKED" | "COMPLETED";
export type ConstraintKind = "HARD" | "SOFT";
export type PlanInvalidationReason =
  | "PROVIDER_CANCELLED"
  | "PROVIDER_AVAILABILITY_EXPIRED"
  | "ETA_THRESHOLD_EXCEEDED"
  | "VEHICLE_CONDITION_WORSENED"
  | "ROUTE_BLOCKED"
  | "CARGO_SAFETY_THREATENED"
  | "DRIVER_TIME_INSUFFICIENT"
  | "RESOURCE_UNAVAILABLE"
  | "HIGHER_SEVERITY_DISRUPTION";
export type TelemetryPolicyState =
  | "NORMAL_TRAVEL"
  | "NEAR_DECISION_POINT"
  | "ROUTE_DEVIATION"
  | "BREAKDOWN_OR_EMERGENCY"
  | "SAFELY_PARKED"
  | "OFFLINE_BUFFERING";

export interface TelemetryPolicyRequest {
  eventId: UUID;
  observedAt: IsoInstant;
  nearDecisionPoint: boolean;
  safelyParked: boolean;
}

export interface TelemetryPolicyDecision {
  tripId: UUID;
  eventId: UUID;
  policyState: TelemetryPolicyState;
  intervalSeconds?: number | null;
  decidedAt: IsoInstant;
  explanationCode: string;
}
export type TripStatus =
  | "CREATED"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "DISRUPTED"
  | "RECOVERY_IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";
export type DisruptionType =
  | "VEHICLE_OVERHEATING"
  | "VEHICLE_BREAKDOWN"
  | "ROUTE_BLOCKED"
  | "ROUTE_DEVIATION"
  | "CARGO_TEMPERATURE_DEVIATION"
  | "DRIVER_REST_REQUIRED"
  | "PROVIDER_CANCELLED"
  | "UNSAFE_STOP_LOCATION";
export type DisruptionSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type RecoveryPlanType =
  | "WORKSHOP_REPAIR"
  | "SAFE_PARKING_MOBILE_MECHANIC"
  | "TOW_REPAIR_OR_TRANSFER"
  | "SUBSTITUTE_VEHICLE_TRANSFER"
  | "DRIVER_REST"
  | "EMERGENCY_ESCALATION";
export type RecoveryPlanStatus =
  | "GENERATED"
  | "EVALUATING"
  | "FEASIBLE"
  | "REJECTED"
  | "SELECTED"
  | "EXECUTING"
  | "INVALIDATED"
  | "COMPLETED"
  | "FAILED";
export type ProviderType =
  | "WORKSHOP"
  | "MOBILE_MECHANIC"
  | "TOWING_SERVICE"
  | "SAFE_PARKING"
  | "SUBSTITUTE_VEHICLE";
export type ObservationStatus = "FRESH" | "AGING" | "EXPIRED" | "UNVERIFIED";
export type NetworkState = "ONLINE" | "WEAK" | "OFFLINE" | "SYNCHRONIZING";
export type TelemetrySource = "SIMULATOR" | "DRIVER_DEVICE" | "VEHICLE_DEVICE";
export type SystemEventType =
  | "TELEMETRY_UPDATED"
  | "DISRUPTION_DETECTED"
  | "PLANS_GENERATED"
  | "PLAN_SELECTED"
  | "PLAN_INVALIDATED"
  | "REPLANNING_STARTED"
  | "REPLANNING_COMPLETED"
  | "TRIP_STATUS_CHANGED";

export interface GeoPoint {
  readonly latitude: number;
  readonly longitude: number;
}

export interface ObservationMetadata {
  readonly observedAt: IsoInstant;
  readonly receivedAt: IsoInstant;
  readonly expiresAt: IsoInstant;
  readonly confidence: number;
  readonly status: ObservationStatus;
  readonly source: string;
}

export interface User {
  readonly id: UUID;
  readonly name: string;
  readonly email: string;
  readonly phone: string;
  readonly role: UserRole;
  readonly createdAt: IsoInstant;
  readonly updatedAt: IsoInstant;
}

export interface Trip {
  readonly id: UUID;
  readonly companyId: UUID;
  readonly vehicleId: UUID;
  readonly driverId: UUID;
  readonly cargoId: UUID;
  readonly routeId: UUID;
  readonly referenceCode: string;
  readonly status: TripStatus;
  readonly deliveryDeadline: IsoInstant;
  readonly maximumWaitingMinutes: number;
  readonly acceptedEtaThresholdMinutes: number;
  readonly startedAt?: IsoInstant;
  readonly createdAt: IsoInstant;
  readonly updatedAt: IsoInstant;
}

export interface VehicleContext {
  readonly vehicleId: UUID;
  readonly registrationNumber: string;
  readonly vehicleType: string;
  readonly status: VehicleStatus;
  readonly heightMeters: number;
  readonly widthMeters: number;
  readonly grossWeightKg: number;
  readonly payloadCapacityKg: number;
  readonly maximumEngineTemperatureCelsius: number;
  readonly currentEngineTemperatureCelsius?: number;
  readonly engineHealthPercent: number;
  readonly refrigerationSupported: boolean;
  readonly requiredCapabilities: readonly string[];
  readonly healthObservation: ObservationMetadata;
}

export interface DriverContext {
  readonly driverId: UUID;
  readonly status: DriverStatus;
  readonly remainingDrivingMinutes: number;
  readonly maximumDailyDrivingMinutes: number;
  readonly lastRestCompletedAt?: IsoInstant;
  readonly statusObservation: ObservationMetadata;
}

export interface CargoContext {
  readonly cargoId: UUID;
  readonly cargoType: CargoType;
  readonly weightKg: number;
  readonly minimumTemperatureCelsius?: number;
  readonly maximumTemperatureCelsius?: number;
  readonly currentTemperatureCelsius?: number;
  readonly hazardous: boolean;
  readonly handlingRequirements: readonly string[];
  readonly conditionObservation: ObservationMetadata;
}

export interface RouteRestriction {
  readonly restrictionId: UUID;
  readonly restrictionCode: string;
  readonly restrictionKind: string;
  readonly limitValue?: number;
  readonly unit?: string;
  readonly blocked: boolean;
  readonly observation: ObservationMetadata;
}

export interface RouteContext {
  readonly routeId: UUID;
  readonly status: RouteStatus;
  readonly origin: GeoPoint;
  readonly destination: GeoPoint;
  readonly currentLocation: GeoPoint;
  readonly remainingDistanceKm: number;
  readonly remainingDurationMinutes: number;
  readonly restrictions: readonly RouteRestriction[];
  readonly conditionObservation: ObservationMetadata;
}

export interface ProviderContext {
  readonly providerId: UUID;
  readonly name: string;
  readonly providerType: ProviderType;
  readonly location: GeoPoint;
  readonly capabilities: readonly string[];
  readonly availabilityStatus: ProviderAvailabilityStatus;
  readonly estimatedArrivalMinutes?: number;
  readonly reliabilityScore: number;
  readonly requiredResourceAvailable: boolean;
  readonly availabilityObservation: ObservationMetadata;
}

export interface SafeStopContext {
  readonly safeStopId: UUID;
  readonly name: string;
  readonly location: GeoPoint;
  readonly maximumVehicleHeightMeters?: number;
  readonly maximumVehicleWeightKg?: number;
  readonly securityVerified: boolean;
  readonly observation: ObservationMetadata;
}

export interface TelemetryObservation {
  readonly eventId: UUID;
  readonly tripId: UUID;
  readonly vehicleId: UUID;
  readonly recordedAt: IsoInstant;
  readonly receivedAt: IsoInstant;
  readonly expiresAt: IsoInstant;
  readonly location: GeoPoint;
  readonly engineTemperatureCelsius?: number;
  readonly cargoTemperatureCelsius?: number;
  readonly speedKph?: number;
  readonly confidence: number;
  readonly observationStatus: ObservationStatus;
  readonly source: TelemetrySource;
  readonly networkState: NetworkState;
}

export interface ActiveDisruption {
  readonly disruptionId: UUID;
  readonly eventId: UUID;
  readonly type: DisruptionType;
  readonly severity: DisruptionSeverity;
  readonly detectedAt: IsoInstant;
  readonly explanationCodes: readonly string[];
  readonly evidence: Readonly<Record<string, unknown>>;
}

export interface DisruptionEvent {
  readonly id: UUID;
  readonly eventId: UUID;
  readonly tripId: UUID;
  readonly contextSnapshotId: UUID;
  readonly type: DisruptionType;
  readonly severity: DisruptionSeverity;
  readonly detectedAt: IsoInstant;
  readonly active: boolean;
  readonly explanationCodes: readonly string[];
  readonly evidence: Readonly<Record<string, unknown>>;
}

export interface RecoveryPlanStep {
  readonly sequence: number;
  readonly actionCode: string;
  readonly instruction: string;
  readonly providerId?: UUID;
  readonly expectedStartAt: IsoInstant;
  readonly expectedCompleteAt: IsoInstant;
  readonly requiredResources: readonly string[];
}

export interface PlanEvaluation {
  readonly planId: UUID;
  readonly feasible: boolean;
  readonly violatedHardConstraints: readonly string[];
  readonly warnings: readonly string[];
  readonly softConstraintResults: Readonly<Record<string, number>>;
  readonly riskScore: number;
  readonly uncertaintyScore: number;
  readonly benefitScore: number;
  readonly lambdaCoefficient: number;
  readonly muCoefficient: number;
  readonly finalUtility: number;
  readonly confidence: number;
  readonly freshnessSummary: Readonly<Record<string, string>>;
  readonly explanationCodes: readonly string[];
  readonly evaluatedAt: IsoInstant;
  readonly evaluatorVersion: string;
  readonly inputHash: string;
}

export interface RecoveryPlan {
  readonly id: UUID;
  readonly tripId: UUID;
  readonly disruptionId: UUID;
  readonly type: RecoveryPlanType;
  readonly status: RecoveryPlanStatus;
  readonly providerId?: UUID;
  readonly safeStopId?: UUID;
  readonly steps: readonly RecoveryPlanStep[];
  readonly evaluation: PlanEvaluation;
  readonly generatedAt: IsoInstant;
  readonly selectedAt?: IsoInstant;
}

export interface TripContext {
  readonly trip: Trip;
  readonly vehicle: VehicleContext;
  readonly driver: DriverContext;
  readonly cargo: CargoContext;
  readonly route: RouteContext;
  readonly providers: readonly ProviderContext[];
  readonly safeStops: readonly SafeStopContext[];
  readonly telemetryObservations: readonly TelemetryObservation[];
  readonly networkState: NetworkState;
  readonly activeDisruptions: readonly ActiveDisruption[];
  readonly currentRecoveryPlan?: RecoveryPlan;
  readonly evaluationTime: IsoInstant;
  readonly contextHash: string;
}

export interface PlanInvalidation {
  readonly id: UUID;
  readonly eventId: UUID;
  readonly planId: UUID;
  readonly reason: PlanInvalidationReason;
  readonly explanationCode: string;
  readonly observedAt: IsoInstant;
  readonly details?: string;
}

export interface DecisionExplanation {
  readonly tripId: UUID;
  readonly disruptionId: UUID;
  readonly selectedPlanId?: UUID;
  readonly rejectedPlanIds: readonly UUID[];
  readonly selectionExplanationCodes: readonly string[];
  readonly rejectionExplanationCodes: Readonly<Record<UUID, readonly string[]>>;
  readonly deterministicTieBreakRule: string;
  readonly explainedAt: IsoInstant;
}

export interface SystemEvent {
  readonly eventId: UUID;
  readonly type: SystemEventType;
  readonly tripId: UUID;
  readonly aggregateId: UUID;
  readonly occurredAt: IsoInstant;
  readonly schemaVersion: number;
  readonly payload: Readonly<Record<string, unknown>>;
}

export interface ApiSuccess<T> {
  readonly success: true;
  readonly data: T;
  readonly timestamp: IsoInstant;
  readonly traceId: UUID;
}

export interface ApiFieldError {
  readonly field: string;
  readonly message: string;
}

export interface ApiError {
  readonly success: false;
  readonly error: {
    readonly code: string;
    readonly message: string;
    readonly fieldErrors: readonly ApiFieldError[];
  };
  readonly timestamp: IsoInstant;
  readonly traceId: UUID;
}

export type ApiResult<T> = ApiSuccess<T> | ApiError;

export type NotificationChannelType = "WEBHOOK" | "SMS" | "WEBPUSH";
export type NotificationDeliveryStatus = "DELIVERED" | "SUPPRESSED_BY_SEVERITY" | "FAILED";

export interface NotificationChannel {
  readonly id: UUID;
  readonly companyId: UUID;
  readonly name: string;
  readonly channelType: NotificationChannelType;
  readonly targetEndpoint: string;
  readonly secretKey: string;
  readonly minimumSeverity: DisruptionSeverity;
  readonly active: boolean;
  readonly createdAt: IsoInstant;
}

export interface CreateNotificationChannelRequest {
  readonly name: string;
  readonly channelType: NotificationChannelType;
  readonly targetEndpoint: string;
  readonly secretKey?: string;
  readonly minimumSeverity: DisruptionSeverity;
  readonly active?: boolean;
}

export interface NotificationTestResult {
  readonly channelId: UUID;
  readonly status: NotificationDeliveryStatus;
  readonly message: string;
  readonly signature: string;
  readonly payload: string;
}

export interface NotificationDeliveryLog {
  readonly id: UUID;
  readonly channelId: UUID;
  readonly companyId: UUID;
  readonly disruptionEventId?: UUID | null;
  readonly disruptionType: string;
  readonly severity: DisruptionSeverity;
  readonly status: NotificationDeliveryStatus;
  readonly signature: string;
  readonly payload: string;
  readonly errorMessage?: string | null;
  readonly deliveredAt: IsoInstant;
}

export type ComplianceExportType = "COLD_CHAIN_EXCURSION" | "CARE_DECISION_CHAIN" | "FULL_TRIP_DOSSIER";
export type ComplianceExportFormat = "JSON_LD" | "CSV" | "PRINTABLE_HTML";

export interface ComplianceExportManifest {
  readonly id: UUID;
  readonly companyId: UUID;
  readonly tripId?: UUID | null;
  readonly exportType: ComplianceExportType;
  readonly format: ComplianceExportFormat;
  readonly sha256Digest: string;
  readonly recordCount: number;
  readonly generatedByUserId?: UUID | null;
  readonly metadata: Record<string, unknown>;
  readonly createdAt: IsoInstant;
}

export interface ComplianceExportPackage {
  readonly manifest: ComplianceExportManifest;
  readonly rawPayload: string;
  readonly mediaType: string;
  readonly filename: string;
}

export interface ComplianceVerificationRequest {
  readonly payload: string;
}

export interface ComplianceVerificationResult {
  readonly manifestId: UUID;
  readonly valid: boolean;
  readonly expectedDigest: string;
  readonly computedDigest: string;
  readonly verifiedAt: IsoInstant;
  readonly message?: string;
}

