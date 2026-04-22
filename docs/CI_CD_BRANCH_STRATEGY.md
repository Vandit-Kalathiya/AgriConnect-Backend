# CI/CD Branch Strategy

## Overview

The AgriConnect CI/CD pipeline is configured to run **only on the main branch** with different behaviors for pull requests vs direct pushes.

## Branch-Specific Behavior

### ❌ Feature Branches (e.g., `feature/xyz`, `dev`, `hotfix/bug`)

**Action:** Push to any branch except `main`

**Result:** Pipeline does **NOT** run at all

**Why:** The workflow trigger is configured to only listen to `main` branch events. Feature branch pushes are ignored completely.

```yaml
on:
  push:
    branches: [main]  # Only main branch
  pull_request:
    branches: [main]  # Only PRs to main
```

---

### ✅ Pull Requests to Main

**Action:** Open or update a PR targeting the `main` branch

**Result:** **Build job ONLY** (compile & package verification)

**Jobs that run:**
- ✅ `build` - Compiles and packages all 6 microservices
  - Eureka-Main-Server
  - Api-Gateway
  - Contract-Farming-App
  - Market-Access-App
  - Generate-Agreement-App
  - Notification-Service

**Jobs that are SKIPPED:**
- ⏭️ `docker-push` - Skipped (condition not met)
- ⏭️ `deploy` - Skipped (depends on docker-push)

**Why:** This allows code review and verification that the code compiles before merging, without deploying to production or pushing Docker images.

---

### 🚀 Push/Merge to Main

**Action:** Direct push to `main` or merge a PR into `main`

**Result:** **FULL PIPELINE** (build → docker-push → deploy)

**Jobs that run:**
1. ✅ `build` - Compiles and packages all 6 microservices
2. ✅ `docker-push` - Builds and pushes Docker images to Docker Hub
   - Tags: `latest`, `sha-<commit>`, `build-<number>`
3. ✅ `deploy` - Deploys to EC2 (if `ENABLE_DEPLOYMENT=true` in `.deployment-config`)
4. ✅ `pipeline-summary` - Generates workflow summary

**Why:** Code merged to main is production-ready and should be built, containerized, and deployed.

---

## Workflow Conditions

### Build Job
```yaml
# No condition - runs on ALL triggers (push to main, PR to main, manual)
build:
  runs-on: ubuntu-latest
```

### Docker Push Job
```yaml
# Only runs on push events to main branch (not PRs)
docker-push:
  needs: build
  if: github.event_name == 'push' && github.ref == 'refs/heads/main'
```

### Deploy Job
```yaml
# Only runs on push events to main branch (not PRs)
deploy:
  needs: docker-push
  if: github.event_name == 'push' && github.ref == 'refs/heads/main'
```

---

## Deployment Control

Even when the pipeline runs on main, deployment can be controlled via `.deployment-config`:

```bash
# .deployment-config
ENABLE_DEPLOYMENT=true   # Full CI/CD (build + push + deploy)
ENABLE_DEPLOYMENT=false  # CI only (build + push, skip deploy)
```

This allows you to:
- Push Docker images to Docker Hub without deploying to EC2
- Test the pipeline without affecting production
- Control deployment independently of the branch strategy

---

## Example Workflows

### Scenario 1: Working on a Feature
```bash
git checkout -b feature/new-listing-api
# Make changes...
git add .
git commit -m "Add new listing API"
git push origin feature/new-listing-api
```
**Result:** ❌ No pipeline runs

---

### Scenario 2: Creating a Pull Request
```bash
# On GitHub: Create PR from feature/new-listing-api → main
```
**Result:** ✅ Build job runs to verify code compiles

**GitHub Actions Summary:**
```
✅ Build & Package (6 services) - Passed
⏭️ Docker Build & Push (6 images) - Skipped
⏭️ Deploy to EC2 - Skipped
```

---

### Scenario 3: Merging to Main
```bash
# On GitHub: Merge PR or directly push to main
git checkout main
git merge feature/new-listing-api
git push origin main
```
**Result:** 🚀 Full pipeline runs

**GitHub Actions Summary:**
```
✅ Build & Package (6 services) - Passed
✅ Docker Build & Push (6 images) - Passed
✅ Deploy to EC2 - Passed (if ENABLE_DEPLOYMENT=true)
```

---

## Benefits of This Strategy

1. **Fast Feedback on PRs** - Developers know immediately if their code compiles
2. **No Accidental Deployments** - Feature branches can't trigger deployments
3. **Production Safety** - Only merged code reaches production
4. **Resource Efficiency** - Docker builds and deployments only happen when needed
5. **Clear Separation** - Build verification vs production deployment are distinct stages

---

## Troubleshooting

### Pipeline didn't run on my feature branch
✅ **This is expected behavior.** Feature branches don't trigger the pipeline.

### Pipeline ran build only on my PR
✅ **This is expected behavior.** PRs only run the build job for verification.

### Docker images weren't pushed on my PR
✅ **This is expected behavior.** Docker push only happens on main branch pushes.

### Pipeline ran on main but deployment was skipped
Check `.deployment-config` - if `ENABLE_DEPLOYMENT=false`, deployment is intentionally disabled.

---

## Manual Workflow Dispatch

You can manually trigger the workflow from the GitHub Actions tab:
1. Go to Actions → CI/CD workflow
2. Click "Run workflow"
3. Select the branch (usually `main`)
4. Click "Run workflow"

**Note:** Manual runs follow the same conditional logic - docker-push and deploy will only run if triggered on the `main` branch.
