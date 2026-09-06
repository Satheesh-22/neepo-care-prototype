import path from "node:path";
import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";
import { defineConfig } from "vitest/config";

const directory = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: "autoUpdate",
      includeAssets: ["neepo-mark.svg"],
      manifest: {
        name: "NEEPO-CARE Operations",
        short_name: "NEEPO-CARE",
        description: "Explainable disruption recovery for logistics operations",
        theme_color: "#071b2a",
        background_color: "#f4f7f5",
        display: "standalone",
        start_url: "/",
        icons: [
          { src: "/neepo-mark.svg", sizes: "any", type: "image/svg+xml", purpose: "any" },
          { src: "/neepo-mark.svg", sizes: "any", type: "image/svg+xml", purpose: "maskable" }
        ]
      }
    })
  ],
  resolve: {
    alias: {
      "@contracts": path.resolve(directory, "../contracts/generated/typescript/neepo-contracts.ts")
    }
  },
  server: {
    port: 5173,
    fs: { allow: [path.resolve(directory, "..")] }
  },
  build: { chunkSizeWarningLimit: 600 },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
    exclude: ["e2e/**", "node_modules/**", "dist/**"],
    css: true
  }
});
