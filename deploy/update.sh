#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
git fetch origin
BRANCH=$(git rev-parse --abbrev-ref HEAD)
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse "origin/$BRANCH")
if [ "$LOCAL" != "$REMOTE" ]; then
  if [ "$(git merge-base "$LOCAL" "$REMOTE")" != "$LOCAL" ]; then
    echo "Local branch is not behind origin/$BRANCH; refusing update." >&2
    exit 1
  fi
  git pull --ff-only
  docker compose --env-file backend/.env -f backend/docker-compose.yml build
  docker compose --env-file backend/.env -f backend/docker-compose.yml up -d
fi
