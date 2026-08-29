#!/usr/bin/env bash
set -euo pipefail

REGISTRY="ghcr.io/impyoo"
VERSION="${1:-latest}"

echo "Building coaching variant..."
./gradlew :composeApp:wasmJsBrowserProductionWebpack -PappVariant=coaching
rm -rf composeApp/docker/build/coaching
mkdir -p composeApp/docker/build/coaching
cp composeApp/build/kotlin-webpack/wasmJs/productionExecutable/* composeApp/docker/build/coaching/
cp -r composeApp/build/generated/compose/resourceGenerator/assembledResources/wasmJsMain/composeResources composeApp/docker/build/coaching/
cp composeApp/docker/index-coaching.html composeApp/docker/build/coaching/index.html
cp composeApp/docker/serve.js composeApp/docker/build/coaching/
docker build -t "${REGISTRY}/handball-coaching-web:${VERSION}" -f composeApp/docker/Dockerfile.coaching composeApp/docker/build/coaching

echo "Building rating variant..."
./gradlew :composeApp:wasmJsBrowserProductionWebpack -PappVariant=rating
rm -rf composeApp/docker/build/rating
mkdir -p composeApp/docker/build/rating
cp composeApp/build/kotlin-webpack/wasmJs/productionExecutable/* composeApp/docker/build/rating/
cp -r composeApp/build/generated/compose/resourceGenerator/assembledResources/wasmJsMain/composeResources composeApp/docker/build/rating/
cp composeApp/docker/index-rating.html composeApp/docker/build/rating/index.html
cp composeApp/docker/serve.js composeApp/docker/build/rating/
docker build -t "${REGISTRY}/handball-rating-web:${VERSION}" -f composeApp/docker/Dockerfile.rating composeApp/docker/build/rating

echo "Building backend..."
./gradlew :server:buildFatJar
rm -rf server/build/docker
mkdir -p server/build/docker
cp server/build/libs/server-all.jar server/build/docker/
cp -r server/data server/build/docker/
docker build -t "${REGISTRY}/handball-backend:${VERSION}" -f server/Dockerfile server/build/docker

echo "Pushing images..."
docker push "${REGISTRY}/handball-coaching-web:${VERSION}"
docker push "${REGISTRY}/handball-rating-web:${VERSION}"
docker push "${REGISTRY}/handball-backend:${VERSION}"

echo "Done: ${VERSION}"
