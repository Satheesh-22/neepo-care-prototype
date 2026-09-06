import type { Session } from "../api/types";
import { create } from "zustand";
import { persist } from "zustand/middleware";

interface AuthState {
  session: Session | null;
  setSession: (session: Session) => void;
  signOut: () => void;
}

export const useAuthStore = create<AuthState>()(persist(
  (set) => ({
    session: null,
    setSession: (session) => set({ session }),
    signOut: () => set({ session: null })
  }),
  { name: "neepo-care-session" }
));
