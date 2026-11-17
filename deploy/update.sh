#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
git fetch origin
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$(git rev-parse HEAD)" != "$(git rev-parse "origin/$BRANCH")" ]; then
  git pull --ff-only
  cd backend
  # Remove this block once the Dockerfile builds the jar itself
  docker run --rm -v "$PWD":/build -v "$HOME/.m2":/root/.m2 -w /build \
    maven:3-eclipse-temurin-21 mvn package -DskipTests
  docker compose up -d --build
fi
