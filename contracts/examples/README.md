# Contract Examples

These deterministic JSON fixtures use the fixed Phase 10 trip and vehicle IDs. They are development/test inputs, not proof that the planned endpoints are implemented.

- `telemetry-normal.json`: normal travel observation
- `telemetry-overheating.json`: exceeds the seeded 105°C limit
- `provider-cancellation.json`: invalidates a selected Workshop B plan after its plan UUID is known

Tests should replace timestamps with controlled clocks where freshness behavior is under evaluation.
