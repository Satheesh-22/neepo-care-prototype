import { randomUUID } from "node:crypto";

const gatewayUrl = normalizeUrl(process.env.NEEPO_GATEWAY_URL ?? "http://localhost:3000");
const backendUrl = normalizeUrl(process.env.NEEPO_BACKEND_URL ?? "http://localhost:8080");
const password = process.env.NEEPO_TEST_PASSWORD;
const dispatcherEmail = process.env.NEEPO_DISPATCHER_EMAIL ?? "dispatcher@neepo.local";
const driverEmail = process.env.NEEPO_DRIVER_EMAIL ?? "driver@neepo.local";
const tripId = process.env.NEEPO_TEST_TRIP_ID ?? "00000000-0000-4000-8600-000000000001";
const vehicleId = process.env.NEEPO_TEST_VEHICLE_ID ?? "00000000-0000-4000-8300-000000000001";
const routeId = process.env.NEEPO_TEST_ROUTE_ID ?? "00000000-0000-4000-8500-000000000001";
let passedChecks = 0;

if (!password) {
  fail("NEEPO_TEST_PASSWORD must be set in the process environment");
}

if (typeof WebSocket === "undefined") {
  fail("This validator requires Node.js 22 or newer with the global WebSocket API");
}

try {
  await validateHealth();
  await expectUnauthorizedTripRead();

  const dispatcher = await login(dispatcherEmail, "DISPATCHER");
  const driver = await login(driverEmail, "DRIVER");
  await validateAssignedTrip(dispatcher.accessToken, "dispatcher");
  await validateAssignedTrip(driver.accessToken, "driver");
  await validateTelemetryRoundTrip(dispatcher.accessToken);
  await validateOfflineTelemetryReplay(dispatcher.accessToken, tripId, vehicleId);
  await validateAutomaticCareLifecycle(dispatcher.accessToken);

  console.log(`Live-stack validation passed: ${passedChecks} checks`);
} catch (error) {
  fail(error instanceof Error ? error.message : String(error));
}

async function validateHealth() {
  const frontend = await fetch(`${gatewayUrl}/health`);
  assert(frontend.ok, `frontend health returned HTTP ${frontend.status}`);
  pass("frontend health");

  const backend = await fetch(`${backendUrl}/actuator/health`);
  const body = await parseJson(backend, "backend health");
  assert(backend.ok && body.status === "UP", `backend health returned HTTP ${backend.status}`);
  pass("backend health");
}

async function expectUnauthorizedTripRead() {
  const response = await fetch(`${gatewayUrl}/api/v1/trips/${tripId}`);
  assert(response.status === 401, `unauthenticated trip read returned HTTP ${response.status}, expected 401`);
  pass("unauthenticated API rejection");
}

async function login(email, expectedRole) {
  const data = await api("/api/v1/auth/login", {
    method: "POST",
    body: { email, password }
  });
  assert(data.accessToken, `${expectedRole.toLowerCase()} login did not return an access token`);
  assert(data.user?.role === expectedRole, `${email} returned role ${data.user?.role ?? "missing"}`);
  pass(`${expectedRole.toLowerCase()} authentication`);
  return data;
}

async function validateAssignedTrip(accessToken, actor) {
  const trip = await api(`/api/v1/trips/${tripId}`, { accessToken });
  assert(trip.id === tripId, `${actor} received an unexpected trip`);
  pass(`${actor} assigned-trip access`);
}

async function validateTelemetryRoundTrip(accessToken) {
  const ticket = await api("/api/v1/events/ticket", { method: "POST", accessToken });
  assert(ticket.ticket, "event ticket response did not contain a ticket");

  const socketUrl = toWebSocketUrl(
    `${gatewayUrl}/ws?tripId=${encodeURIComponent(tripId)}&ticket=${encodeURIComponent(ticket.ticket)}`
  );
  const socket = await openWebSocket(socketUrl);
  pass("one-use WebSocket ticket authentication");

  const eventId = randomUUID();
  const recordedAt = new Date();
  const observation = {
    eventId,
    tripId,
    vehicleId,
    recordedAt: recordedAt.toISOString(),
    receivedAt: new Date(recordedAt.getTime() + 1_000).toISOString(),
    expiresAt: new Date(recordedAt.getTime() + 300_000).toISOString(),
    location: { latitude: 11.42, longitude: 77.69 },
    engineTemperatureCelsius: 96.5,
    cargoTemperatureCelsius: 5.2,
    speedKph: 42,
    confidence: 0.98,
    observationStatus: "FRESH",
    source: "SIMULATOR",
    networkState: "ONLINE"
  };

  const eventPromise = nextJsonMessage(socket, 10_000);
  const accepted = await api("/api/v1/telemetry", {
    method: "POST",
    accessToken,
    body: observation,
    expectedStatus: 202
  });
  assert(accepted.eventId === eventId && accepted.duplicate === false, "new telemetry was not accepted once");
  pass("telemetry ingestion count assertion (INT-03)");

  const event = await eventPromise;
  assert(event.type === "TELEMETRY_UPDATED", `received unexpected event type ${event.type ?? "missing"}`);
  assert(event.tripId === tripId, "WebSocket event contains an unexpected trip ID");
  assert(event.aggregateId === eventId, "WebSocket event does not match accepted telemetry");
  socket.close(1000, "validated");
  pass("post-commit telemetry WebSocket delivery (INT-03)");

  const duplicate = await api("/api/v1/telemetry", {
    method: "POST",
    accessToken,
    body: observation,
    expectedStatus: 202
  });
  assert(duplicate.eventId === eventId && duplicate.duplicate === true, "duplicate telemetry was not idempotent");
  pass("duplicate telemetry event count assertion (INT-04)");

  await expectWebSocketRejected(socketUrl);
  pass("consumed WebSocket ticket rejection");
}

async function validateOfflineTelemetryReplay(accessToken, replayTripId, replayVehicleId) {
  const offlineCount = 3;
  const now = Date.now();
  const offlineEvents = Array.from({ length: offlineCount }, (_, index) => {
    const recordedAt = new Date(now - (offlineCount - index) * 10_000);
    return {
      eventId: randomUUID(),
      tripId: replayTripId,
      vehicleId: replayVehicleId,
      recordedAt: recordedAt.toISOString(),
      receivedAt: new Date().toISOString(),
      expiresAt: new Date(now + 300_000).toISOString(),
      location: { latitude: 11.42 + index * 0.005, longitude: 77.69 + index * 0.005 },
      engineTemperatureCelsius: 95.0 + index * 0.5,
      cargoTemperatureCelsius: 5.0,
      speedKph: 40 + index * 5,
      confidence: 0.98,
      observationStatus: "FRESH",
      source: "DRIVER_DEVICE",
      networkState: "SYNCHRONIZING"
    };
  });

  let replayedCount = 0;
  for (const item of offlineEvents) {
    const res = await api("/api/v1/telemetry", {
      method: "POST",
      accessToken,
      body: item,
      expectedStatus: 202
    });
    if (res.eventId === item.eventId && res.duplicate === false) {
      replayedCount += 1;
    }
  }

  assert(replayedCount === offlineCount, `expected ${offlineCount} offline events replayed, but got ${replayedCount}`);
  pass("offline telemetry queue replay and synchronization count (INT-15, INT-16)");
}

async function validateAutomaticCareLifecycle(accessToken) {
  const suffix = randomUUID().replaceAll("-", "").slice(0, 10);
  const phoneDigits = (BigInt(`0x${suffix}`) % 1_000_000_000n).toString().padStart(9, "0");
  const generatedPassword = `${randomUUID()}Aa1!`;
  const user = await api("/api/v1/users", {
    method: "POST",
    accessToken,
    expectedStatus: 201,
    body: {
      name: `Acceptance Driver ${suffix}`,
      email: `acceptance-${suffix}@neepo.local`,
      phone: `9${phoneDigits}`,
      role: "DRIVER",
      password: generatedPassword
    }
  });
  const vehicle = await api("/api/v1/vehicles", {
    method: "POST",
    accessToken,
    expectedStatus: 201,
    body: {
      registrationNumber: `AC-${suffix}`,
      vehicleType: "REFRIGERATED_TRUCK",
      heightMeters: 3.8,
      widthMeters: 2.5,
      grossWeightKg: 14_000,
      payloadCapacityKg: 7_000,
      maximumEngineTemperatureCelsius: 105,
      refrigerationSupported: true
    }
  });
  const driver = await api("/api/v1/drivers", {
    method: "POST",
    accessToken,
    expectedStatus: 201,
    body: {
      userId: user.id,
      licenseNumber: `AC-LIC-${suffix}`,
      maximumDailyDrivingMinutes: 540
    }
  });
  const cargo = await api("/api/v1/cargo", {
    method: "POST",
    accessToken,
    expectedStatus: 201,
    body: {
      referenceCode: `AC-CARGO-${suffix}`,
      cargoType: "REFRIGERATED",
      description: "Automated acceptance temperature-sensitive cargo",
      weightKg: 2_500,
      minimumTemperatureCelsius: 2,
      maximumTemperatureCelsius: 8,
      hazardous: false
    }
  });
  const trip = await api("/api/v1/trips", {
    method: "POST",
    accessToken,
    expectedStatus: 201,
    body: {
      referenceCode: `AC-TRIP-${suffix}`,
      vehicleId: vehicle.id,
      driverId: driver.id,
      cargoId: cargo.id,
      routeId,
      deliveryDeadline: new Date(Date.now() + 20 * 60 * 60 * 1_000).toISOString(),
      maximumWaitingMinutes: 180,
      acceptedEtaThresholdMinutes: 90
    }
  });
  pass("acceptance trip resource provisioning");

  await api(`/api/v1/trips/${trip.id}/assign`, {
    method: "POST",
    accessToken,
    body: {
      vehicleId: vehicle.id,
      driverId: driver.id,
      assignedAt: new Date(Date.now() - 30_000).toISOString()
    }
  });
  const started = await api(`/api/v1/trips/${trip.id}/start`, { method: "POST", accessToken });
  assert(started.status === "IN_PROGRESS", "acceptance trip did not enter IN_PROGRESS");
  pass("acceptance trip assignment and start");

  await refreshDemoProviders(accessToken);
  const eventId = randomUUID();
  const recordedAt = new Date();
  await api("/api/v1/telemetry", {
    method: "POST",
    accessToken,
    expectedStatus: 202,
    body: telemetryObservation(eventId, trip.id, vehicle.id, recordedAt, 112)
  });
  const disrupted = await pollApi(
    `/api/v1/trips/${trip.id}`,
    accessToken,
    value => value.status === "DISRUPTED",
    "automatic overheating disruption"
  );
  assert(disrupted.status === "DISRUPTED", "overheating telemetry did not disrupt the trip");
  pass("automatic overheating disruption detection count (INT-05)");

  const plans = await pollApi(
    `/api/v1/trips/${trip.id}/recovery-plans`,
    accessToken,
    value => Array.isArray(value) && value.length >= 3 && value.some(plan => plan.status === "SELECTED"),
    "automatic recovery plan generation"
  );
  assert(plans.length >= 3, "CARE generated fewer than three recovery alternatives");
  const selected = plans.find(plan => plan.status === "SELECTED");
  assert(selected, "CARE did not select a recovery plan");
  pass("automatic recovery plan generation count (INT-06)");

  const rejected = plans.filter(plan => plan.status === "REJECTED");
  assert(
    rejected.some(plan => plan.evaluation?.violatedHardConstraints?.length > 0),
    "recovery alternatives did not preserve hard-constraint rejection evidence"
  );
  pass("hard-constraint rejection evidence");

  const staleProviderId = "00000000-0000-4000-8700-000000000003";
  const staleProviderPlans = plans.filter(plan => plan.providerId === staleProviderId);
  assert(
    staleProviderPlans.length > 0 && staleProviderPlans.every(plan => plan.status === "REJECTED"),
    "candidate recovery plans for expired/stale provider were not rejected"
  );
  assert(
    staleProviderPlans.some(plan => plan.evaluation?.explanationCodes?.includes("PROVIDER_AVAILABILITY_EXPIRED")),
    "stale provider rejection omitted the PROVIDER_AVAILABILITY_EXPIRED explanation code"
  );
  pass("expired provider availability rejection (INT-08)");

  const explanationStart = Date.now();
  const explanation = await api(`/api/v1/trips/${trip.id}/decision-explanation`, { accessToken });
  const decisionLatencyMs = Date.now() - explanationStart;
  assert(decisionLatencyMs >= 0 && decisionLatencyMs < 5000, "decision explanation latency was invalid or exceeded threshold");
  assert(explanation.selectedPlanId === selected.id, "decision explanation does not identify the selected plan");
  assert(
    explanation.deterministicTieBreakRule === "FINAL_UTILITY_DESC_THEN_PLAN_UUID_ASC",
    "decision explanation omitted or had unexpected deterministic tie-break rule"
  );
  assert(
    Array.isArray(explanation.selectionExplanationCodes) && explanation.selectionExplanationCodes.includes("PLAN_SELECTED_HIGHEST_UTILITY"),
    "decision explanation omitted highest utility selection code"
  );
  pass("deterministic tie-break rule and decision explanation with latency assertion (INT-09, INT-10)");

  const emergencyPolicy = await api(`/api/v1/trips/${trip.id}/telemetry-policy`, {
    method: "POST",
    accessToken,
    body: {
      eventId: randomUUID(),
      observedAt: new Date().toISOString(),
      nearDecisionPoint: false,
      safelyParked: false
    }
  });
  assert(
    emergencyPolicy.policyState === "BREAKDOWN_OR_EMERGENCY" && emergencyPolicy.intervalSeconds === 3,
    "active overheating did not select the three-second emergency telemetry interval"
  );
  pass("adaptive emergency telemetry policy");

  const parkedPolicy = await api(`/api/v1/trips/${trip.id}/telemetry-policy`, {
    method: "POST",
    accessToken,
    body: {
      eventId: randomUUID(),
      observedAt: new Date().toISOString(),
      nearDecisionPoint: false,
      safelyParked: true
    }
  });
  assert(
    parkedPolicy.policyState === "SAFELY_PARKED" && parkedPolicy.intervalSeconds === 120,
    "safe-parking confirmation did not select the 120-second telemetry interval"
  );
  pass("adaptive safely parked telemetry policy");

  const invalidationCode = selected.providerId ? "PROVIDER_CANCELLED" : "ROUTE_BLOCKED";
  const invalidated = await api(`/api/v1/recovery-plans/${selected.id}/invalidate`, {
    method: "POST",
    accessToken,
    expectedStatus: 202,
    body: {
      eventId: randomUUID(),
      explanationCode: invalidationCode,
      observedAt: new Date().toISOString(),
      details: "Automated acceptance invalidation"
    }
  });
  assert(invalidated.status === "INVALIDATED", "selected recovery plan was not invalidated");
  const replanned = await pollApi(
    `/api/v1/trips/${trip.id}/recovery-plans`,
    accessToken,
    value => Array.isArray(value) && value.some(plan => plan.status === "SELECTED" && plan.id !== selected.id),
    "automatic recovery re-planning"
  );
  assert(
    replanned.some(plan => plan.status === "SELECTED" && plan.id !== selected.id),
    "CARE did not select a replacement recovery plan"
  );
  pass("invalidation and automatic re-planning");
}

async function refreshDemoProviders(accessToken) {
  const providerIds = [
    "00000000-0000-4000-8700-000000000001",
    "00000000-0000-4000-8700-000000000002",
    "00000000-0000-4000-8700-000000000003",
    "00000000-0000-4000-8700-000000000004",
    "00000000-0000-4000-8700-000000000005"
  ];
  const observedAt = new Date();
  await Promise.all(providerIds.map((providerId, index) => {
    const isStaleProvider = providerId === "00000000-0000-4000-8700-000000000003";
    const expiresAt = isStaleProvider
      ? new Date(observedAt.getTime() - 60_000).toISOString()
      : new Date(observedAt.getTime() + 10 * 60 * 1_000).toISOString();
    return api(`/api/v1/providers/${providerId}/availability`, {
      method: "POST",
      accessToken,
      expectedStatus: 201,
      body: {
        status: "AVAILABLE",
        observedAt: isStaleProvider ? new Date(observedAt.getTime() - 120_000).toISOString() : observedAt.toISOString(),
        expiresAt,
        estimatedArrivalMinutes: 20 + index * 10,
        confidence: 0.98,
        requiredResourceAvailable: true
      }
    });
  }));
}

function telemetryObservation(eventId, observationTripId, observationVehicleId, recordedAt, engineTemperature) {
  return {
    eventId,
    tripId: observationTripId,
    vehicleId: observationVehicleId,
    recordedAt: recordedAt.toISOString(),
    receivedAt: recordedAt.toISOString(),
    expiresAt: new Date(recordedAt.getTime() + 300_000).toISOString(),
    location: { latitude: 11.42, longitude: 77.69 },
    engineTemperatureCelsius: engineTemperature,
    cargoTemperatureCelsius: 5.2,
    speedKph: 42,
    confidence: 0.98,
    observationStatus: "FRESH",
    source: "SIMULATOR",
    networkState: "ONLINE"
  };
}

async function pollApi(path, accessToken, predicate, label, timeoutMs = 10_000) {
  const deadline = Date.now() + timeoutMs;
  let value;
  do {
    value = await api(path, { accessToken });
    if (predicate(value)) return value;
    await new Promise(resolve => setTimeout(resolve, 200));
  } while (Date.now() < deadline);
  throw new Error(`${label} timed out`);
}

async function api(path, options = {}) {
  const headers = { Accept: "application/json" };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  if (options.accessToken) headers.Authorization = `Bearer ${options.accessToken}`;

  const response = await fetch(`${gatewayUrl}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });
  const envelope = await parseJson(response, path);
  const expectedStatus = options.expectedStatus ?? 200;
  if (response.status !== expectedStatus || envelope.success !== true) {
    const code = envelope.error?.code ?? "UNKNOWN_ERROR";
    const message = envelope.error?.message ?? "request failed";
    throw new Error(`${path} returned HTTP ${response.status} (${code}: ${message})`);
  }
  return envelope.data;
}

function openWebSocket(url) {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(url);
    const timeout = setTimeout(() => reject(new Error("WebSocket connection timed out")), 10_000);
    socket.addEventListener("open", () => {
      clearTimeout(timeout);
      resolve(socket);
    }, { once: true });
    socket.addEventListener("error", () => {
      clearTimeout(timeout);
      reject(new Error("WebSocket authentication failed"));
    }, { once: true });
  });
}

function expectWebSocketRejected(url) {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(url);
    const timeout = setTimeout(() => reject(new Error("consumed WebSocket ticket rejection timed out")), 10_000);
    socket.addEventListener("open", () => {
      clearTimeout(timeout);
      socket.close();
      reject(new Error("a consumed WebSocket ticket was accepted again"));
    }, { once: true });
    socket.addEventListener("error", () => {
      clearTimeout(timeout);
      resolve();
    }, { once: true });
  });
}

function nextJsonMessage(socket, timeoutMs) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error("telemetry WebSocket event timed out")), timeoutMs);
    socket.addEventListener("message", (message) => {
      clearTimeout(timeout);
      try {
        resolve(JSON.parse(String(message.data)));
      } catch {
        reject(new Error("WebSocket returned a non-JSON event"));
      }
    }, { once: true });
  });
}

async function parseJson(response, label) {
  try {
    return await response.json();
  } catch {
    throw new Error(`${label} returned a non-JSON response`);
  }
}

function toWebSocketUrl(url) {
  return url.replace(/^http:/, "ws:").replace(/^https:/, "wss:");
}

function normalizeUrl(url) {
  return url.replace(/\/$/, "");
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function pass(label) {
  passedChecks += 1;
  console.log(`PASS ${label}`);
}

function fail(message) {
  console.error(`FAIL ${message}`);
  process.exit(1);
}
