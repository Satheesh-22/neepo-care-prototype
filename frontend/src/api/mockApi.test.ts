import { describe, expect, it } from "vitest";
import { DEMO_TRIP_ID, mockApi } from "./mockApi";

describe("mock API boundary", () => {
  it("creates the requested role session", async () => {
    const session = await mockApi.login({
      email: "driver@neepo.local",
      password: "demo-password",
      demoRole: "DRIVER"
    });

    expect(session.user.role).toBe("DRIVER");
    expect(session.accessToken).toContain("driver");
  });

  it("returns ordered recovery alternatives for the demo trip", async () => {
    const plans = await mockApi.listRecoveryPlans(DEMO_TRIP_ID, "demo-token");

    expect(plans).toHaveLength(2);
    expect(plans[0].status).toBe("SELECTED");
    expect(plans[1].evaluation.feasible).toBe(false);
  });

  it("accepts telemetry idempotently by event identifier", async () => {
    const eventId = crypto.randomUUID();
    const observation = {
      eventId,
      tripId: DEMO_TRIP_ID,
      vehicleId: crypto.randomUUID(),
      recordedAt: new Date().toISOString(),
      receivedAt: new Date().toISOString(),
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      location: { latitude: 13.03, longitude: 77.59 },
      engineTemperatureCelsius: 101,
      confidence: 0.98,
      observationStatus: "FRESH" as const,
      source: "SIMULATOR" as const,
      networkState: "ONLINE" as const
    };

    expect((await mockApi.ingestTelemetry(observation, "demo-token")).duplicate).toBe(false);
    expect((await mockApi.ingestTelemetry(observation, "demo-token")).duplicate).toBe(true);
  });

  it("returns emergency and safely parked adaptive intervals", async () => {
    const observedAt = new Date().toISOString();
    const emergency = await mockApi.evaluateTelemetryPolicy(DEMO_TRIP_ID, {
      eventId: crypto.randomUUID(), observedAt, nearDecisionPoint: false, safelyParked: false
    }, "demo-token");
    const parked = await mockApi.evaluateTelemetryPolicy(DEMO_TRIP_ID, {
      eventId: crypto.randomUUID(), observedAt, nearDecisionPoint: false, safelyParked: true
    }, "demo-token");

    expect(emergency).toMatchObject({ policyState: "BREAKDOWN_OR_EMERGENCY", intervalSeconds: 3 });
    expect(parked).toMatchObject({ policyState: "SAFELY_PARKED", intervalSeconds: 120 });
  });

  it("transitions the trip to in progress when started", async () => {
    const started = await mockApi.startTrip(DEMO_TRIP_ID, "demo-token");
    expect(started.status).toBe("IN_PROGRESS");
    expect(started.startedAt).toBeDefined();
  });

  it("returns time-series telemetry history for replay", async () => {
    const history = await mockApi.getTelemetryHistory(DEMO_TRIP_ID);
    expect(history.length).toBeGreaterThanOrEqual(9);
    expect(history[0].speedKph).toBe(0);
    expect(history.some((pt) => (pt.engineTemperatureCelsius ?? 0) > 100)).toBe(true);

    const filtered = await mockApi.getTelemetryHistory(DEMO_TRIP_ID, { limit: 3 });
    expect(filtered).toHaveLength(3);
  });
});

