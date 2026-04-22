#!/bin/bash
# Build All Services
# Run from Backend root directory

set -e

echo "🔨 Building All AgriConnect Services..."
echo ""

SERVICES=(
    "Eureka-Main-Server"
    "Api-Gateway"
    "Market-Access-App"
    "Contract-Farming-App"
    "Generate-Agreement-App"
    "Notification-Service"
)

BUILD_FAILED=()
BUILD_SUCCESS=()

for service in "${SERVICES[@]}"; do
    echo "═══════════════════════════════════════════════════════════"
    echo "Building: $service"
    echo "═══════════════════════════════════════════════════════════"
    
    if [ ! -d "$service" ]; then
        echo "❌ Service directory not found: $service"
        BUILD_FAILED+=("$service")
        continue
    fi
    
    cd "$service"
    
    if mvn clean install -DskipTests -B -q; then
        echo "✅ $service built successfully"
        BUILD_SUCCESS+=("$service")
    else
        echo "❌ $service build failed"
        BUILD_FAILED+=("$service")
    fi
    
    cd ..
    echo ""
done

echo "═══════════════════════════════════════════════════════════"
echo "Build Summary"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "✅ Successful builds: ${#BUILD_SUCCESS[@]}"
for service in "${BUILD_SUCCESS[@]}"; do
    echo "   - $service"
done

if [ ${#BUILD_FAILED[@]} -gt 0 ]; then
    echo ""
    echo "❌ Failed builds: ${#BUILD_FAILED[@]}"
    for service in "${BUILD_FAILED[@]}"; do
        echo "   - $service"
    done
    exit 1
fi

echo ""
echo "🎉 All services built successfully!"
echo ""
echo "Next steps:"
echo "1. Test with Docker: docker-compose up --build"
echo "2. Verify Eureka: http://localhost:8761"
echo "3. Run load tests"
echo ""
