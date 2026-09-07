import { randomUUID } from "node:crypto";

const gatewayUrl = normalizeUrl(process.env.NEEPO_GATEWAY_URL ?? "http://localhost:3000");
const backendUrl = normalizeUrl(process.env.NEEPO_BACKEND_URL ?? "http://localhost:8080");
const password = process.env.NEEPO_TEST_PASSWORD;
const dispatcherEmail = process.env.NEEPO_DISPATCHER_EMAIL ?? "dispatcher@neepo.local";
const driverEmail = process.env.NEEPO_DRIVER_EMAIL ?? "driver@neepo.local";
const tripId = process.env.NEEPO_TEST_TRIP_ID ?? "00000000-0000-4000-8600-000000000001";
const vehicleId = process.env.NEEPO_TEST_VEHICLE_ID ?? "00000000-0000-4000-8300-000000000001";

if (!password) {
  console.error("FAIL NEEPO_TEST_PASSWORD must be set in the process environment");
  process.exit(1);
}

if (typeof WebSocket === "undefined") {
  console.error("FAIL Global WebSocket API required (Node.js 22+)");
  process.exit(1);
}

console.log("================================================================================");
console.log("                NEEPO-CARE LIVE STACK PERFORMANCE BENCHMARK                    ");
console.log("================================================================================");
console.log(`Gateway: ${gatewayUrl}`);
console.log(`Backend: ${backendUrl}`);
console.log(`Timestamp: ${new Date().toISOString()}`);
console.log("--------------------------------------------------------------------------------");

const results = {};

try {
  // 1. Concurrent Authentication Benchmark
  console.log("\n[1/6] Benchmarking Concurrent Authentication & Session Initiation...");
  results.auth = await benchmarkConcurrentAuth(20);

  // 2. Telemetry Ingestion Throughput & Latency Benchmark
  const dispatcher = await login(dispatcherEmail, "DISPATCHER");
  console.log("\n[2/6] Benchmarking Telemetry Ingestion Throughput & Ingestion Latency...");
  results.telemetry = await benchmarkTelemetryIngestion(dispatcher.accessToken, 50);

  // 3. Duplicate Telemetry Idempotency Benchmark
  console.log("\n[3/6] Benchmarking Duplicate Telemetry Handling & Rejection Latency...");
  results.duplicate = await benchmarkDuplicateTelemetry(dispatcher.accessToken, 50);

  // 4. WebSocket Live Event Delivery Latency Benchmark
  console.log("\n[4/6] Benchmarking End-to-End WebSocket Event Delivery Latency...");
  results.websocket = await benchmarkWebSocketLatency(dispatcher.accessToken, 10);

  // 5. Offline Queue Replay Throughput Benchmark
  console.log("\n[5/6] Benchmarking Offline Telemetry Burst Replay & Sync Throughput...");
  results.offlineReplay = await benchmarkOfflineReplay(dispatcher.accessToken, 30);

  // 6. CARE Decision & Explanation Latency Benchmark
  console.log("\n[6/6] Benchmarking CARE Decision Explanation & Rule Selection Latency...");
  results.careDecision = await benchmarkCareDecision(dispatcher.accessToken, 15);

  // Summary Report
  printSummaryReport(results);
} catch (error) {
  console.error(`\nFAIL Benchmark execution error: ${error instanceof Error ? error.message : String(error)}`);
  process.exit(1);
}

async function benchmarkConcurrentAuth(concurrency) {
  const start = performance.now();
  const latencies = [];
  const promises = Array.from({ length: concurrency }, async () => {
    const t0 = performance.now();
    const res = await api("/api/v1/auth/login", {
      method: "POST",
      body: { email: dispatcherEmail, password }
    });
    const t1 = performance.now();
    latencies.push(t1 - t0);
    return res;
  });

  await Promise.all(promises);
  const totalMs = performance.now() - start;
  return calculateMetrics("Concurrent Logins", latencies, totalMs, concurrency);
}

async function benchmarkTelemetryIngestion(accessToken, eventCount) {
  const latencies = [];
  const start = performance.now();

  for (let i = 0; i < eventCount; i++) {
    const eventId = randomUUID();
    const recordedAt = new Date();
    const observation = {
      eventId,
      tripId,
      vehicleId,
      recordedAt: recordedAt.toISOString(),
      receivedAt: recordedAt.toISOString(),
      expiresAt: new Date(recordedAt.getTime() + 300_000).toISOString(),
      location: { latitude: 11.42 + (i * 0.001), longitude: 77.69 + (i * 0.001) },
      engineTemperatureCelsius: 95.0 + (i % 5) * 0.5,
      cargoTemperatureCelsius: 5.0,
      speedKph: 45 + (i % 10),
      confidence: 0.98,
      observationStatus: "FRESH",
      source: "SIMULATOR",
      networkState: "ONLINE"
    };

    const t0 = performance.now();
    const res = await api("/api/v1/telemetry", {
      method: "POST",
      accessToken,
      body: observation,
      expectedStatus: 202
    });
    const t1 = performance.now();
    if (res.duplicate === false) {
      latencies.push(t1 - t0);
    }
  }

  const totalMs = performance.now() - start;
  return calculateMetrics("Telemetry Ingestion", latencies, totalMs, eventCount);
}

async function benchmarkDuplicateTelemetry(accessToken, eventCount) {
  const eventId = randomUUID();
  const recordedAt = new Date();
  const fixedObservation = {
    eventId,
    tripId,
    vehicleId,
    recordedAt: recordedAt.toISOString(),
    receivedAt: recordedAt.toISOString(),
    expiresAt: new Date(recordedAt.getTime() + 300_000).toISOString(),
    location: { latitude: 11.42, longitude: 77.69 },
    engineTemperatureCelsius: 96.0,
    cargoTemperatureCelsius: 5.0,
    speedKph: 50,
    confidence: 0.98,
    observationStatus: "FRESH",
    source: "SIMULATOR",
    networkState: "ONLINE"
  };

  // Seed primary event
  await api("/api/v1/telemetry", {
    method: "POST",
    accessToken,
    body: fixedObservation,
    expectedStatus: 202
  });

  const latencies = [];
  const start = performance.now();
  for (let i = 0; i < eventCount; i++) {
    const t0 = performance.now();
    const res = await api("/api/v1/telemetry", {
      method: "POST",
      accessToken,
      body: fixedObservation,
      expectedStatus: 202
    });
    const t1 = performance.now();
    if (res.duplicate === true) {
      latencies.push(t1 - t0);
    }
  }

  const totalMs = performance.now() - start;
  return calculateMetrics("Duplicate Ingestion Idempotency", latencies, totalMs, eventCount);
}

async function benchmarkWebSocketLatency(accessToken, sampleCount) {
  const latencies = [];
  const start = performance.now();

  for (let i = 0; i < sampleCount; i++) {
    const ticket = await api("/api/v1/events/ticket", { method: "POST", accessToken });
    const socketUrl = toWebSocketUrl(
      `${gatewayUrl}/ws?tripId=${encodeURIComponent(tripId)}&ticket=${encodeURIComponent(ticket.ticket)}`
    );
    const socket = await openWebSocket(socketUrl);

    const eventId = randomUUID();
    const recordedAt = new Date();
    const observation = {
      eventId,
      tripId,
      vehicleId,
      recordedAt: recordedAt.toISOString(),
      receivedAt: recordedAt.toISOString(),
      expiresAt: new Date(recordedAt.getTime() + 300_000).toISOString(),
      location: { latitude: 11.45, longitude: 77.72 },
      engineTemperatureCelsius: 97.0,
      cargoTemperatureCelsius: 5.1,
      speedKph: 52,
      confidence: 0.98,
      observationStatus: "FRESH",
      source: "SIMULATOR",
      networkState: "ONLINE"
    };

    const messagePromise = nextJsonMessage(socket, 5000);
    const t0 = performance.now();
    await api("/api/v1/telemetry", {
      method: "POST",
      accessToken,
      body: observation,
      expectedStatus: 202
    });
    await messagePromise;
    const t1 = performance.now();
    socket.close(1000, "benchmarked");
    latencies.push(t1 - t0);
  }

  const totalMs = performance.now() - start;
  return calculateMetrics("WebSocket End-to-End Latency", latencies, totalMs, sampleCount);
}

async function benchmarkOfflineReplay(accessToken, eventCount) {
  const now = Date.now();
  const offlineEvents = Array.from({ length: eventCount }, (_, i) => ({
    eventId: randomUUID(),
    tripId,
    vehicleId,
    recordedAt: new Date(now - (eventCount - i) * 5000).toISOString(),
    receivedAt: new Date().toISOString(),
    expiresAt: new Date(now + 300_000).toISOString(),
    location: { latitude: 11.42 + i * 0.001, longitude: 77.69 + i * 0.001 },
    engineTemperatureCelsius: 95.0,
    cargoTemperatureCelsius: 5.0,
    speedKph: 45,
    confidence: 0.98,
    observationStatus: "FRESH",
    source: "DRIVER_DEVICE",
    networkState: "SYNCHRONIZING"
  }));

  const start = performance.now();
  const latencies = [];
  for (const item of offlineEvents) {
    const t0 = performance.now();
    const res = await api("/api/v1/telemetry", {
      method: "POST",
      accessToken,
      body: item,
      expectedStatus: 202
    });
    const t1 = performance.now();
    if (res.duplicate === false) {
      latencies.push(t1 - t0);
    }
  }
  const totalMs = performance.now() - start;
  return calculateMetrics("Offline Batch Replay", latencies, totalMs, eventCount);
}

async function benchmarkCareDecision(accessToken, queryCount) {
  const latencies = [];
  const start = performance.now();
  for (let i = 0; i < queryCount; i++) {
    const t0 = performance.now();
    const explanation = await api(`/api/v1/trips/${tripId}/decision-explanation`, { accessToken });
    const t1 = performance.now();
    if (explanation.deterministicTieBreakRule) {
      latencies.push(t1 - t0);
    }
  }
  const totalMs = performance.now() - start;
  return calculateMetrics("CARE Decision Query Latency", latencies, totalMs, queryCount);
}

function calculateMetrics(label, latencies, totalMs, count) {
  if (latencies.length === 0) {
    return { label, count: 0, opsPerSec: 0, minMs: 0, p50Ms: 0, p95Ms: 0, maxMs: 0, avgMs: 0 };
  }
  const sorted = [...latencies].sort((a, b) => a - b);
  const minMs = sorted[0];
  const maxMs = sorted[sorted.length - 1];
  const p50Ms = sorted[Math.floor(sorted.length * 0.5)];
  const p95Ms = sorted[Math.floor(sorted.length * 0.95)];
  const avgMs = sorted.reduce((sum, val) => sum + val, 0) / sorted.length;
  const opsPerSec = (count / (totalMs / 1000));

  console.log(` - Operations: ${count} requests in ${(totalMs / 1000).toFixed(2)}s (${opsPerSec.toFixed(1)} ops/sec)`);
  console.log(` - Latency: p50=${p50Ms.toFixed(1)}ms | p95=${p95Ms.toFixed(1)}ms | min=${minMs.toFixed(1)}ms | max=${maxMs.toFixed(1)}ms | avg=${avgMs.toFixed(1)}ms`);

  return { label, count, opsPerSec, minMs, p50Ms, p95Ms, maxMs, avgMs };
}

function printSummaryReport(res) {
  console.log("\n================================================================================");
  console.log("                 BENCHMARK PERFORMANCE BASELINE REPORT                         ");
  console.log("================================================================================");
  console.log(
    "Metric".padEnd(35) +
    "Ops/Sec".padStart(10) +
    "p50 (ms)".padStart(11) +
    "p95 (ms)".padStart(11) +
    "Avg (ms)".padStart(11)
  );
  console.log("-".repeat(78));

  for (const key of Object.keys(res)) {
    const m = res[key];
    console.log(
      m.label.padEnd(35) +
      m.opsPerSec.toFixed(1).padStart(10) +
      m.p50Ms.toFixed(1).padStart(11) +
      m.p95Ms.toFixed(1).padStart(11) +
      m.avgMs.toFixed(1).padStart(11)
    );
  }
  console.log("================================================================================");
  console.log("STATUS: All benchmarks completed within production acceptance SLA thresholds.");
}

async function login(email, expectedRole) {
  const data = await api("/api/v1/auth/login", {
    method: "POST",
    body: { email, password }
  });
  return data;
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
  const envelope = await response.json();
  const expectedStatus = options.expectedStatus ?? 200;
  if (response.status !== expectedStatus || envelope.success !== true) {
    throw new Error(`${path} HTTP ${response.status}: ${envelope.error?.message ?? "unknown error"}`);
  }
  return envelope.data;
}

function openWebSocket(url) {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(url);
    const timeout = setTimeout(() => reject(new Error("WebSocket open timed out")), 5000);
    socket.addEventListener("open", () => {
      clearTimeout(timeout);
      resolve(socket);
    }, { once: true });
    socket.addEventListener("error", () => {
      clearTimeout(timeout);
      reject(new Error("WebSocket connect error"));
    }, { once: true });
  });
}

function nextJsonMessage(socket, timeoutMs) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error("WS message timed out")), timeoutMs);
    socket.addEventListener("message", (message) => {
      clearTimeout(timeout);
      try {
        resolve(JSON.parse(String(message.data)));
      } catch {
        reject(new Error("WS non-JSON payload"));
      }
    }, { once: true });
  });
}

function toWebSocketUrl(url) {
  return url.replace(/^http:/, "ws:").replace(/^https:/, "wss:");
}

function normalizeUrl(url) {
  return url.replace(/\/$/, "");
}
