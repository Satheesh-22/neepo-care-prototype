import type { TelemetryObservation } from "@contracts";
import { neepoApi } from "../api";
import { ApiClientError, type TelemetryAccepted } from "../api/types";
import { pendingRequests, queueRequest, removeQueuedRequest } from "./requestQueue";

export type TelemetrySubmission =
  | { state: "accepted"; result: TelemetryAccepted }
  | { state: "queued"; eventId: string };

const telemetryPath = "/telemetry";

export async function submitTelemetry(
  observation: TelemetryObservation,
  accessToken: string,
  online = navigator.onLine
): Promise<TelemetrySubmission> {
  if (!online || observation.networkState === "OFFLINE") {
    await enqueueTelemetry(observation);
    return { state: "queued", eventId: observation.eventId };
  }

  try {
    return { state: "accepted", result: await neepoApi.ingestTelemetry(observation, accessToken) };
  } catch (error) {
    if (error instanceof ApiClientError) throw error;
    await enqueueTelemetry(observation);
    return { state: "queued", eventId: observation.eventId };
  }
}

export async function synchronizeTelemetry(accessToken: string): Promise<number> {
  let synchronized = 0;
  for (const request of await pendingRequests()) {
    if (request.path !== telemetryPath) continue;
    await neepoApi.ingestTelemetry(request.body as TelemetryObservation, accessToken);
    await removeQueuedRequest(request.id);
    synchronized += 1;
  }
  return synchronized;
}

async function enqueueTelemetry(observation: TelemetryObservation) {
  await queueRequest({
    id: observation.eventId,
    path: telemetryPath,
    method: "POST",
    body: observation,
    queuedAt: new Date().toISOString()
  });
  window.dispatchEvent(new Event("neepo:queue-change"));
}
