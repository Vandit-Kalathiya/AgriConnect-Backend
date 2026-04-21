# CI/CD Deployment Flag — Quick Reference

## TL;DR

Control EC2 deployment by editing `.deployment-config` file in your repo.

---

## The File

**Location:** `Backend/.deployment-config`

**Content:**

```bash
ENABLE_DEPLOYMENT=false  # or true
```

**To change:** Edit the file, commit, and push.

---

## Behavior

| `ENABLE_DEPLOYMENT` | What Happens                                   |
| ------------------- | ---------------------------------------------- |
| `false` (default)   | ✅ Build → ✅ Docker Push → ⏭️ Skip EC2 Deploy |
| `true`              | ✅ Build → ✅ Docker Push → ✅ EC2 Deploy      |

---

## Common Workflows

### Development Mode (Recommended)

```bash
# Edit .deployment-config
ENABLE_DEPLOYMENT=false

# Commit and push
git add .deployment-config
git commit -m "Disable deployment"
git push
```

- Push code freely
- Images built and pushed to Docker Hub
- No EC2 deployment

### Deploy to Production

```bash
# Edit .deployment-config
ENABLE_DEPLOYMENT=true

# Commit and push
git add .deployment-config
git commit -m "Enable deployment"
git push

# After deployment, optionally disable again
ENABLE_DEPLOYMENT=false
git add .deployment-config
git commit -m "Disable deployment"
git push
```

---

## Quick Links

- **Full Guide:** `docs/DEPLOYMENT_FLAG_GUIDE.md`
- **CI/CD Pipeline:** `.github/workflows/ci.yml`
- **Production Checklist:** `docs/PRODUCTION_CHECKLIST.md`

---

## Troubleshooting

**Deployment not running?**

- Check `.deployment-config` file exists in repo root
- Verify value is exactly `ENABLE_DEPLOYMENT=true` (lowercase `true`)
- Ensure you committed and pushed the file
- Ensure you're pushing to `main` branch (not PR)

**Want to check current value?**

- Open `.deployment-config` file in your repo
- Or check the pipeline summary after a run
