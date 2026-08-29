#!/usr/bin/env bash
set -euo pipefail

# Local-only build script. Builds all three images directly on the Docker host.
# No registry or push is required.
#
# Run this on the machine that will run docker compose up -d.

VARIANTS=("coaching" "rating")

for variant in "${VARIANTS[@]}"; do
    echo "Building $variant web variant..."
    ./gradlew ":composeApp:wasmJsBrowserProductionWebpack" -PappVariant="$variant"

    buildDir="composeApp/docker/build/$variant"
    rm -rf "$buildDir"
    mkdir -p "$buildDir"

    cp composeApp/build/kotlin-webpack/wasmJs/productionExecutable/* "$buildDir/"
    cp -r composeApp/build/generated/compose/resourceGenerator/assembledResources/wasmJsMain/composeResources "$buildDir/"
    cp "composeApp/docker/index-$variant.html" "$buildDir/index.html"
    cp composeApp/docker/serve.js "$buildDir/"

    docker build -t "handball-${variant}-web:local" -f "composeApp/docker/Dockerfile.$variant" "$buildDir"
done

echo "Building backend..."
./gradlew :server:buildFatJar

backendDir="server/build/docker"
rm -rf "$backendDir"
mkdir -p "$backendDir"
cp server/build/libs/server-all.jar "$backendDir/"
cp -r server/data "$backendDir/"

docker build -t handball-backend:local -f server/Dockerfile "$backendDir"

echo "Done. Run 'docker compose -f deploy/docker-compose.yml up -d' to start."
