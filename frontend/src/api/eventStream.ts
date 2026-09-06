import type { SystemEvent } from "@contracts";
import { apiMode, neepoApi } from ".";

export type EventConnectionState = "connecting" | "connected" | "reconnecting" | "closed";

export interface EventStreamSubscription {
  close(): void;
}

export function deduplicateEvents(onEvent: (event: SystemEvent) => void) {
  const seen = new Set<string>();
  return (event: SystemEvent) => {
    if (seen.has(event.eventId)) return;
    seen.add(event.eventId);
    onEvent(event);
  };
}

export function subscribeToTripEvents(
  tripId: string,
  accessToken: string,
  onEvent: (event: SystemEvent) => void,
  onState: (state: EventConnectionState) => void
): EventStreamSubscription {
  const emit = deduplicateEvents(onEvent);
  if (apiMode === "mock") return mockSubscription(tripId, emit, onState);

  let socket: WebSocket | undefined;
  let retry: number | undefined;
  let stopped = false;
  let attempts = 0;
  const baseUrl = import.meta.env.VITE_WS_URL ?? `${location.protocol === "https:" ? "wss" : "ws"}://${location.host}/ws`;

  const connect = async () => {
    onState(attempts === 0 ? "connecting" : "reconnecting");
    try {
      const ticket = await neepoApi.issueEventTicket(accessToken);
      if (stopped) return;
      socket = new WebSocket(`${baseUrl}?tripId=${encodeURIComponent(tripId)}&ticket=${encodeURIComponent(ticket.ticket)}`);
    } catch {
      attempts += 1;
      onState("reconnecting");
      retry = window.setTimeout(() => void connect(), Math.min(1_000 * 2 ** attempts, 30_000));
      return;
    }
    socket.onopen = () => { attempts = 0; onState("connected"); };
    socket.onmessage = (message) => {
      try { emit(JSON.parse(String(message.data)) as SystemEvent); } catch { /* Ignore malformed external events. */ }
    };
    socket.onclose = () => {
      if (stopped) return;
      attempts += 1;
      onState("reconnecting");
      retry = window.setTimeout(() => void connect(), Math.min(1_000 * 2 ** attempts, 30_000));
    };
  };
  void connect();
  return { close: () => { stopped = true; if (retry) clearTimeout(retry); socket?.close(); onState("closed"); } };
}

function mockSubscription(
  tripId: string,
  onEvent: (event: SystemEvent) => void,
  onState: (state: EventConnectionState) => void
): EventStreamSubscription {
  onState("connected");
  const timer = window.setInterval(() => onEvent({
    eventId: crypto.randomUUID(),
    type: "TELEMETRY_UPDATED",
    tripId,
    aggregateId: crypto.randomUUID(),
    occurredAt: new Date().toISOString(),
    schemaVersion: 1,
    payload: { source: "mock-event-stream" }
  }), 15_000);
  return { close: () => { clearInterval(timer); onState("closed"); } };
}
