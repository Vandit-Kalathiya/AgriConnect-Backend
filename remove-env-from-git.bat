@echo off
REM Script to remove .env files from git tracking
REM This will keep the files locally but remove them from git

echo 🔧 Removing .env files from git tracking...
echo.

REM Remove .env files from git tracking (keeps local files)
git rm --cached Main-Backend\.env 2>NUL
if %ERRORLEVEL% EQU 0 (
    echo ✅ Removed Main-Backend\.env from tracking
) else (
    echo ⚠️  Main-Backend\.env not tracked or already removed
)

git rm --cached Contract-Farming-App\.env 2>NUL
if %ERRORLEVEL% EQU 0 (
    echo ✅ Removed Contract-Farming-App\.env from tracking
) else (
    echo ⚠️  Contract-Farming-App\.env not tracked or already removed
)

git rm --cached Generate-Agreement-App\.env 2>NUL
if %ERRORLEVEL% EQU 0 (
    echo ✅ Removed Generate-Agreement-App\.env from tracking
) else (
    echo ⚠️  Generate-Agreement-App\.env not tracked or already removed
)

git rm --cached Market-Access-App\.env 2>NUL
if %ERRORLEVEL% EQU 0 (
    echo ✅ Removed Market-Access-App\.env from tracking
) else (
    echo ⚠️  Market-Access-App\.env not tracked or already removed
)

git rm --cached Eureka-Main-Server\.env 2>NUL
if %ERRORLEVEL% EQU 0 (
    echo ✅ Removed Eureka-Main-Server\.env from tracking
) else (
    echo ⚠️  Eureka-Main-Server\.env not tracked or already removed
)

echo.
echo 📊 Checking git status...
git status

echo.
echo 📝 If files were removed, commit the changes:
echo    git commit -m "chore: Remove .env files from git tracking"
echo.
echo 🚀 Then push to remote:
echo    git push origin main
echo.
pause
