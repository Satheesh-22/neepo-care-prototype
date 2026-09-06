import { openDB } from "idb";

export interface QueuedRequest {
  id: string;
  path: string;
  method: "POST" | "PUT" | "PATCH";
  body: unknown;
  queuedAt: string;
}

const database = openDB("neepo-care-offline", 1, {
  upgrade(db) {
    db.createObjectStore("requests", { keyPath: "id" });
  }
});

export async function queueRequest(request: QueuedRequest) {
  return (await database).put("requests", request);
}

export async function pendingRequests(): Promise<QueuedRequest[]> {
  const requests = await (await database).getAll("requests") as QueuedRequest[];
  return requests.sort((left, right) => left.queuedAt.localeCompare(right.queuedAt));
}

export async function removeQueuedRequest(id: string) {
  return (await database).delete("requests", id);
}
