import { create } from "zustand";
import { persist } from "zustand/middleware";

export type Locale = "en" | "es";

export interface PreferencesState {
  locale: Locale;
  highContrast: boolean;
  largeText: boolean;
  reducedMotion: boolean;
  soundAlerts: boolean;
  setLocale: (locale: Locale) => void;
  toggleHighContrast: () => void;
  toggleLargeText: () => void;
  toggleReducedMotion: () => void;
  toggleSoundAlerts: () => void;
  setHighContrast: (enabled: boolean) => void;
  setLargeText: (enabled: boolean) => void;
}

export const usePreferencesStore = create<PreferencesState>()(
  persist(
    (set) => ({
      locale: "en",
      highContrast: false,
      largeText: false,
      reducedMotion: false,
      soundAlerts: true,
      setLocale: (locale: Locale) => set({ locale }),
      toggleHighContrast: () => set((state) => ({ highContrast: !state.highContrast })),
      toggleLargeText: () => set((state) => ({ largeText: !state.largeText })),
      toggleReducedMotion: () => set((state) => ({ reducedMotion: !state.reducedMotion })),
      toggleSoundAlerts: () => set((state) => ({ soundAlerts: !state.soundAlerts })),
      setHighContrast: (highContrast: boolean) => set({ highContrast }),
      setLargeText: (largeText: boolean) => set({ largeText })
    }),
    {
      name: "neepo-preferences"
    }
  )
);

