import { useEffect } from "react";
import { useAnnounceStore } from "./announceStore";

/**
 * Visually hidden ARIA live regions for screen-reader notifications.
 * Polite: status changes, non-blocking completions.
 * Assertive: immediate cold-chain alarms, critical sensor dropouts, collision/overheating alerts.
 */
export function LiveAnnouncer() {
  const politeMessage = useAnnounceStore((s) => s.politeMessage);
  const assertiveMessage = useAnnounceStore((s) => s.assertiveMessage);
  const clear = useAnnounceStore((s) => s.clear);

  useEffect(() => {
    if (politeMessage || assertiveMessage) {
      const timer = setTimeout(() => {
        clear();
      }, 7000);
      return () => clearTimeout(timer);
    }
  }, [politeMessage, assertiveMessage, clear]);

  return (
    <div
      id="a11y-live-region"
      style={{
        position: "absolute",
        width: 1,
        height: 1,
        padding: 0,
        margin: -1,
        overflow: "hidden",
        clip: "rect(0, 0, 0, 0)",
        whiteSpace: "nowrap",
        border: 0
      }}
    >
      <div
        id="a11y-polite"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        {politeMessage}
      </div>
      <div
        id="a11y-assertive"
        role="alert"
        aria-live="assertive"
        aria-atomic="true"
      >
        {assertiveMessage}
      </div>
    </div>
  );
}
