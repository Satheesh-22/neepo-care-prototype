# Shared Generated Contracts

These Java records/enums and TypeScript types freeze cross-module names before independent development.

- Java package: `com.neepo.contracts`
- TypeScript entry point: `typescript/neepo-contracts.ts`
- Source of truth: `../openapi.yaml` and `../domain-enums.yaml`

They are deliberately outside the backend and frontend source trees. During the first relevant milestone, each member must either:

1. generate models from OpenAPI into the module build, or
2. copy these baseline contracts into a dedicated shared module without renaming fields.

Do not hand-edit only one language. Every contract change must update OpenAPI, enums, Java, TypeScript, persistence impact and consumer tests in the same reviewed pull request.

Numeric Java scoring values use `BigDecimal` for deterministic calculations. TypeScript receives JSON numbers for display and must not reimplement decision arithmetic.
