# Development Environment

## Required

| Tool | Supported baseline |
|---|---|
| Git | 2.40 or newer |
| Java | 21 LTS |
| Maven | Wrapper included; no global Maven required |
| Node.js | 22 LTS |
| npm | 10 or newer |
| Python | 3.11 or newer, for repository validation scripts |
| Docker Desktop/Engine | 25 or newer |
| Docker Compose | v2 plugin |
| AI development assistant | Optional; Claude Code requires a supported paid/API account. The free file-handoff workflow is documented in `docs/assistant/FREE_WORKFLOW.md`. |
| Postman/Bruno | Optional API verification |
| DBeaver/pgAdmin | Optional database inspection |

## Verify

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-prerequisites.ps1
```

Linux/macOS:

```bash
chmod +x scripts/*.sh backend/mvnw
./scripts/check-prerequisites.sh
```

Expected major versions: Java 21, Node 22, Python 3.11+, Docker Compose v2. Do not begin implementation with Java 17 or a missing Compose plugin.

Claude Code is not required for the repository to build or run. Developers without paid Claude Code access can use the controlled ZIP handoff process in `docs/assistant/FREE_WORKFLOW.md`.

## IDE

Recommended: IntelliJ IDEA or VS Code. Use UTF-8, LF line endings, four-space Java indentation and two-space YAML indentation. The repository includes `.editorconfig` and `.gitattributes`.

## Secrets

Copy `.env.example` to `.env`, replace every `replace_...` value, and never commit `.env`. Do not paste real secrets into Claude prompts, screenshots, logs or frontend variables.
