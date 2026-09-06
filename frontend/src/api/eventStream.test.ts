import type { SystemEvent } from "@contracts";
import { describe, expect, it, vi } from "vitest";
import { deduplicateEvents } from "./eventStream";

describe("trip event stream", () => {
  it("suppresses a replayed event after reconnect", () => {
    const listener = vi.fn();
    const emit = deduplicateEvents(listener);
    const event = {
      eventId: crypto.randomUUID(),
      type: "TELEMETRY_UPDATED",
      tripId: crypto.randomUUID(),
      aggregateId: crypto.randomUUID(),
      occurredAt: new Date().toISOString(),
      schemaVersion: 1,
      payload: {}
    } satisfies SystemEvent;

    emit(event);
    emit(event);

    expect(listener).toHaveBeenCalledOnce();
  });
});
