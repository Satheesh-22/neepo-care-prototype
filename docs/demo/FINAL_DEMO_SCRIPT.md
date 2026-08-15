# Final Demonstration Script

1. Reset repeatable demo data.
2. Log in as the dispatcher.
3. Show the assigned refrigerated truck, driver, cargo and route.
4. Start the trip and simulator.
5. Show the normal 60-second telemetry policy.
6. Raise engine temperature to 112°C.
7. Show the detected `VEHICLE_OVERHEATING` event and freshness evidence.
8. Show at least three generated recovery plans.
9. Explain the nearest workshop rejection caused by a 3.5 m height limit.
10. Explain why expired provider availability is not confirmation.
11. Show the highest-utility feasible plan selected.
12. Trigger provider cancellation using a unique event ID.
13. Show plan invalidation followed by `REPLANNING_STARTED`.
14. Show the mobile mechanic fallback selected.
15. Show the complete recovery timeline and explanation codes.
16. Show emergency and safely-parked telemetry interval changes.
17. Compare CARE with nearest-provider and fixed-interval baselines.
18. Reset the demonstration without manual database editing.

The presenter must not claim that a mocked endpoint, unacknowledged offline event or stale provider observation is live or confirmed.
