import { create } from "zustand";

interface AnnounceState {
  politeMessage: string;
  assertiveMessage: string;
  announce: (message: string, priority?: "polite" | "assertive") => void;
  clear: () => void;
}

export const useAnnounceStore = create<AnnounceState>((set) => ({
  politeMessage: "",
  assertiveMessage: "",
  announce: (message: string, priority: "polite" | "assertive" = "polite") => {
    if (priority === "assertive") {
      set({ assertiveMessage: message });
    } else {
      set({ politeMessage: message });
    }
  },
  clear: () => set({ politeMessage: "", assertiveMessage: "" })
}));

export function announce(message: string, priority: "polite" | "assertive" = "polite") {
  useAnnounceStore.getState().announce(message, priority);
}

export function useAnnounce() {
  const announceFn = useAnnounceStore((s) => s.announce);
  return { announce: announceFn };
}
