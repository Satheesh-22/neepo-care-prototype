# Git and Branch Workflow

## Branches

| Branch | Purpose | Owner |
|---|---|---|
| `main` | Stable demonstration releases | Team |
| `develop` | Reviewed integration | Team |
| `feature/backend-platform` | Backend and persistence | Member 1 |
| `feature/care-engine` | CARE decision engine | Member 2 |
| `feature/frontend-pwa` | Frontend, PWA and simulator | Member 3 |

## Initialize after extracting the ZIP

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\initialize-git.ps1
```

Linux/macOS:

```bash
chmod +x scripts/initialize-git.sh
./scripts/initialize-git.sh
```

The script creates local branches only. Add your private GitHub remote and push them:

```bash
git remote add origin https://github.com/YOUR_ORG/neepo-care-prototype.git
git push -u origin main
git push -u origin develop
git push -u origin feature/backend-platform
git push -u origin feature/care-engine
git push -u origin feature/frontend-pwa
```

## Protection

In GitHub, protect `main` and `develop`: require pull requests, at least one review, required CI checks, resolved conversations and no force pushes.

## Milestone integration

Each milestone is one focused branch change and pull request. A shared contract change requires all module owners to approve. Merge feature work into `develop`; merge `develop` into `main` only for a verified demo release.
