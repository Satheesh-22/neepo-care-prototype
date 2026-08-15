# Repeatable Demonstration Data

Flyway migration `V4__seed_repeatable_demo_data.sql` creates the complete starting state. IDs are fixed so automated tests and simulator fixtures can refer to stable records.

## Scenario records

| Record | Stable ID | Purpose |
|---|---|---|
| Company | `00000000-0000-4000-8000-000000000001` | Demo tenant |
| Dispatcher | `00000000-0000-4000-8100-000000000001` | Dispatcher role |
| Driver user | `00000000-0000-4000-8100-000000000002` | Driver role |
| Driver | `00000000-0000-4000-8200-000000000001` | 360 minutes remaining |
| Refrigerated truck | `00000000-0000-4000-8300-000000000001` | 3.8 m high, 14,000 kg, 105°C limit |
| Cargo | `00000000-0000-4000-8400-000000000001` | Diagnostic reagents requiring 2–8°C |
| Route | `00000000-0000-4000-8500-000000000001` | Erode to Bengaluru |
| Trip | `00000000-0000-4000-8600-000000000001` | Assigned demo trip |

## Provider behavior

- Workshop A is nearest and freshly available, but its access restriction is 3.5 m. The 3.8 m truck must be rejected with `ROAD_HEIGHT_LIMIT_EXCEEDED`.
- Workshop B is compatible and freshly available. It is the expected initial selection before cancellation.
- Workshop C is compatible but its availability expired before seed completion. It must not be treated as confirmed.
- Mobile Mechanic D is the expected feasible fallback after Workshop B cancels.
- Towing Service E enables a third normal recovery alternative.
- Two verified safe stops allow the safe-parking and mobile-mechanic plan.

## Reset

For local demonstration only, remove the Docker volume and recreate the stack:

```bash
docker compose down -v
docker compose up --build -d postgres redis backend
```

Deleting the volume destroys local data. Never run this reset against a shared or production database.

## Authentication note

Phase 10 intentionally does not commit passwords or reusable password hashes. The authentication milestone must create development credentials securely through a loader or environment-driven setup.
