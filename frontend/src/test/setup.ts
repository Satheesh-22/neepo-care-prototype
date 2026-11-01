import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// Mock indexedDB for jsdom environment
if (typeof window !== "undefined" && !window.indexedDB) {
  (window as unknown as { indexedDB: unknown }).indexedDB = {
    open: () => ({} as IDBOpenDBRequest)
  };
}

afterEach(() => cleanup());
