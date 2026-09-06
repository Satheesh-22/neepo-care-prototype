# NEEPO-CARE Frontend

React/TypeScript PWA for dispatchers, drivers and administrators. The functional slice includes role-aware sign-in, an operations overview, a trip recovery workspace, CARE plan comparisons, decision explanations, route mapping, driver telemetry capture and an IndexedDB-backed offline synchronization queue.

## Run locally

```powershell
cd frontend
npm install
npm run dev
```

The default `mock` data mode runs without the backend and demonstrates the overheating recovery flow. To use live APIs, create `frontend/.env.local`:

```text
VITE_API_MODE=live
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_WS_URL=ws://localhost:8080/ws
```

The live event client obtains a short-lived, single-use connection ticket and reconnects with exponential backoff. After reconnection, pages refresh their REST data because the WebSocket contract does not guarantee recovery of missed events. Docker uses the same-origin `/api/v1` and `/ws` reverse proxies.

## Role journeys

- Dispatcher and administrator: review the priority trip, compare recovery plans, inspect CARE explanations and open the telemetry simulator.
- Driver: follow the selected server-owned recovery instructions and transmit current vehicle/cargo observations.
- Offline: observations are stored in submission order, shown in the application header and synchronized after browser connectivity returns.

## Quality checks

```powershell
npm run lint
npm test
npm run build
npm run test:e2e
```

Shared types are imported directly from `contracts/generated/typescript/neepo-contracts.ts`; CARE scoring logic is never duplicated in the UI.
