# GitHub Actions Pipeline Behavior

## Quick Reference

| Event | Build | Docker Push | Deploy |
|-------|-------|-------------|--------|
| Push to `feature/*` | ❌ No | ❌ No | ❌ No |
| Push to `dev` | ❌ No | ❌ No | ❌ No |
| Push to any branch except `main` | ❌ No | ❌ No | ❌ No |
| **Pull Request → `main`** | ✅ **Yes** | ❌ No | ❌ No |
| **Push/Merge → `main`** | ✅ **Yes** | ✅ **Yes** | ✅ **Yes*** |
| Manual workflow dispatch on `main` | ✅ Yes | ✅ Yes | ✅ Yes* |
| Manual workflow dispatch on other branch | ✅ Yes | ❌ No | ❌ No |

\* Deploy job also checks `.deployment-config` file - if `ENABLE_DEPLOYMENT=false`, deployment is skipped even on main.

---

## Visual Flow

```
┌─────────────────────────────────────────────────────────────┐
│ Developer pushes to feature/xyz branch                      │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
                    ❌ No pipeline runs


┌─────────────────────────────────────────────────────────────┐
│ Developer opens PR: feature/xyz → main                      │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
                  ✅ Build job runs
                     (6 services)
                           │
                           ▼
                  ⏭️ Docker push skipped
                  ⏭️ Deploy skipped


┌─────────────────────────────────────────────────────────────┐
│ PR merged to main (or direct push to main)                  │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
                  ✅ Build job runs
                     (6 services)
                           │
                           ▼
                  ✅ Docker push runs
                     (6 images to Docker Hub)
                     Tags: latest, sha-xxx, build-N
                           │
                           ▼
                  ✅ Deploy job runs*
                     (SSH to EC2, rolling restart)
                           │
                           ▼
                  ✅ Pipeline summary
```

\* If `ENABLE_DEPLOYMENT=true` in `.deployment-config`

---

## Configuration Files

### `.github/workflows/ci.yml`
Controls **when** the pipeline triggers and **which jobs** run based on event type and branch.

### `.deployment-config`
Controls **whether** deployment to EC2 happens (even when the deploy job would normally run).

```bash
ENABLE_DEPLOYMENT=true   # Full CI/CD
ENABLE_DEPLOYMENT=false  # CI only (skip EC2 deploy)
```

---

## Why This Strategy?

✅ **Safety** - Feature branches can't accidentally deploy to production

✅ **Fast feedback** - PRs get build verification before merge

✅ **Resource efficiency** - Docker builds only happen when needed

✅ **Clear separation** - Build verification vs production deployment

✅ **Flexibility** - Can disable deployment independently via `.deployment-config`

---

For detailed documentation, see: `docs/CI_CD_BRANCH_STRATEGY.md`
