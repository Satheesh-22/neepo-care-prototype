import { useCallback, useEffect, useState } from "react";
import { useAuthStore } from "../auth/authStore";
import { synchronizeTelemetry } from "./telemetrySync";
import { pendingRequests } from "./requestQueue";

export function useOfflineQueue() {
  const token = useAuthStore((state) => state.session?.accessToken);
  const [pendingCount, setPendingCount] = useState(0);
  const [synchronizing, setSynchronizing] = useState(false);

  const refresh = useCallback(async () => setPendingCount((await pendingRequests()).length), []);
  const synchronize = useCallback(async () => {
    if (!token || !navigator.onLine) return;
    setSynchronizing(true);
    try {
      await synchronizeTelemetry(token);
      await refresh();
    } finally {
      setSynchronizing(false);
    }
  }, [refresh, token]);

  useEffect(() => {
    const initialRefresh = window.setTimeout(() => void refresh().then(() => synchronize()), 0);
    const handleOnline = () => void synchronize();
    const handleQueueChange = () => void refresh();
    window.addEventListener("online", handleOnline);
    window.addEventListener("neepo:queue-change", handleQueueChange);
    return () => {
      clearTimeout(initialRefresh);
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("neepo:queue-change", handleQueueChange);
    };
  }, [refresh, synchronize]);

  return { pendingCount, synchronizing, synchronize };
}
