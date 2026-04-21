# CI/CD Deployment Feature Flag Guide

## Overview

The CI/CD pipeline uses a **deployment configuration file** (`.deployment-config`) that controls whether the pipeline performs a full deployment to EC2 or stops after building and pushing Docker images.

**Key Benefit:** The deployment flag is version-controlled in your repo, so you can change it with a simple commit and push.

---

## How It Works

### When `ENABLE_DEPLOYMENT=true` (Full CI/CD)

1. ✅ **Build** — Compile all 6 microservices
2. ✅ **Docker Push** — Build & push images to Docker Hub with tags: `latest`, `sha-<commit>`, `build-<number>`
3. ✅ **Deploy** — SSH into EC2, pull images, rolling restart, health checks, cleanup
4. ✅ **Summary** — Pipeline report with deployment details

### When `ENABLE_DEPLOYMENT=false` (CI Only)

1. ✅ **Build** — Compile all 6 microservices
2. ✅ **Docker Push** — Build & push images to Docker Hub
3. ⏭️ **Deploy** — **SKIPPED** (no EC2 deployment)
4. ✅ **Summary** — Pipeline report showing deployment was skipped

---

## Setup Instructions

### The `.deployment-config` File

The deployment flag is controlled by a file in your repo root: `.deployment-config`

**File location:** `Backend/.deployment-config`

**File content:**

```bash
# Deployment Configuration
# Set ENABLE_DEPLOYMENT to control deployment behavior:
#   true  = Full CI/CD (build → docker push → EC2 deploy)
#   false = CI only (build → docker push, skip EC2 deploy)

ENABLE_DEPLOYMENT=false
```

### How to Change Deployment Behavior

**To disable deployment (recommended for development):**

1. Edit `.deployment-config`
2. Set `ENABLE_DEPLOYMENT=false`
3. Commit and push

**To enable deployment:**

1. Edit `.deployment-config`
2. Set `ENABLE_DEPLOYMENT=true`
3. Commit and push

The pipeline automatically reads this file on every run.

---

## Use Cases

### 🔒 Disable Deployment (Recommended for Development)

**When to use:**

- You're actively developing and don't want every push to trigger a production deployment
- You want to build and test Docker images without deploying
- You're working on features that aren't ready for production
- You want to accumulate multiple commits before deploying

**How:**

```
ENABLE_DEPLOYMENT=false
```

**Result:**

- Every push to `main` builds images and pushes to Docker Hub
- No EC2 deployment happens
- You can manually deploy later by setting the flag to `true` and re-running the workflow

---

### 🚀 Enable Deployment (Production Mode)

**When to use:**

- You're ready to deploy to production
- You want automatic deployments on every push to `main`
- You've tested locally and want the changes live

**How:**

```
ENABLE_DEPLOYMENT=true
```

**Result:**

- Full CI/CD pipeline runs
- Images are built, pushed, and deployed to EC2
- Health checks ensure all services are running
- Nginx is reloaded automatically

---

## Changing the Flag

### Via GitHub UI

1. Go to: **Settings** → **Secrets and variables** → **Actions** → **Variables**
2. Click the pencil icon next to `ENABLE_DEPLOYMENT`
3. Change the value to `true` or `false`
4. Click **Update variable**

### Re-running a Workflow with Different Flag

1. Change the `ENABLE_DEPLOYMENT` variable (as above)
2. Go to **Actions** tab
3. Select the workflow run you want to re-run
4. Click **Re-run all jobs**
5. The workflow will use the new flag value

---

## Pipeline Summary

The pipeline summary now shows the deployment mode at the top:

### When Disabled:

```
🔒 Deployment Mode: DISABLED (CI only — images pushed to Docker Hub)

| Stage                          | Result          |
|--------------------------------|-----------------|
| Build & Package (6 services)   | ✅ Passed       |
| Docker Build & Push (6 images) | ✅ Passed       |
| Deploy to EC2 + Nginx reload   | ⏭️ Skipped (ENABLE_DEPLOYMENT=false) |
```

### When Enabled:

```
🚀 Deployment Mode: ENABLED (Full CI/CD)

| Stage                          | Result          |
|--------------------------------|-----------------|
| Build & Package (6 services)   | ✅ Passed       |
| Docker Build & Push (6 images) | ✅ Passed       |
| Deploy to EC2 + Nginx reload   | ✅ Passed       |
```

---

## Best Practices

### 1. **Default to Disabled**

Keep `ENABLE_DEPLOYMENT=false` during active development to avoid accidental deployments.

### 2. **Enable for Releases**

When you're ready to release:

1. Set `ENABLE_DEPLOYMENT=true`
2. Push to `main` or re-run the last workflow
3. Verify deployment succeeded
4. Optionally set back to `false` after deployment

### 3. **Manual Deployment Workflow**

If you want to deploy a specific commit without enabling auto-deployment:

1. Keep `ENABLE_DEPLOYMENT=false` as default
2. When ready to deploy, temporarily set to `true`
3. Go to **Actions** → select the commit's workflow run → **Re-run all jobs**
4. Set back to `false` after deployment completes

### 4. **Scheduled Deployments**

For scheduled deployments (e.g., deploy every Friday):

- Keep `ENABLE_DEPLOYMENT=false` normally
- Use GitHub Actions scheduled workflows or manually enable on deployment day

---

## Troubleshooting

### Deployment Still Running When Flag is False

- **Cause:** The variable wasn't created or has a typo
- **Fix:** Verify the variable name is exactly `ENABLE_DEPLOYMENT` (case-sensitive)

### Deployment Not Running When Flag is True

- **Cause 1:** You're on a PR (deployments only run on direct pushes to `main`)
- **Cause 2:** Docker push job failed
- **Fix:** Check the workflow logs for errors in the build or docker-push stages

### How to Check Current Flag Value

1. Go to: **Settings** → **Secrets and variables** → **Actions** → **Variables**
2. Look for `ENABLE_DEPLOYMENT` and its current value

---

## Related Documentation

- **Main CI/CD Pipeline:** `.github/workflows/ci.yml`
- **Kafka Feature Flags:** See `Api-Gateway/.env.example` → `FEATURE_KAFKA_ENABLED`
- **Redis Feature Flags:** See `Api-Gateway/.env.example` → `CACHE_LOCAL_ENABLED`
- **Production Checklist:** `PRODUCTION_CHECKLIST.md`

---

## Summary

The `ENABLE_DEPLOYMENT` flag gives you full control over when deployments happen:

- **`false`** (default) → Safe development mode, images built but not deployed
- **`true`** → Production mode, full CI/CD with automatic EC2 deployment

This allows you to:

- ✅ Push code frequently without deploying
- ✅ Build and test Docker images in the pipeline
- ✅ Deploy only when you're ready
- ✅ Avoid accidental production deployments during development
