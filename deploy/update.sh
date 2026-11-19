#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

log() { printf '%s %s\n' "$(date --iso-8601=seconds)" "$*"; }

exec 9>"$REPO_DIR/.git/airpx-update.lock"
flock -n 9 || { log "Another update is running; exiting."; exit 0; }

CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
BRANCH="${AIRPX_UPDATE_BRANCH:-$CURRENT_BRANCH}"
if [[ "$CURRENT_BRANCH" == HEAD ]]; then
  log "Detached HEAD; refusing update."
  exit 1
fi
if [[ "$CURRENT_BRANCH" != "$BRANCH" ]]; then
  log "Checked out branch $CURRENT_BRANCH does not match configured branch $BRANCH; refusing update."
  exit 1
fi

if ! git fetch origin; then
  log "Fetch failed; leaving the running stack unchanged."
  exit 1
fi
if [[ -n "$(git status --porcelain)" ]]; then
  log "Working tree is dirty; refusing update."
  exit 1
fi

LOCAL="$(git rev-parse HEAD)"
REMOTE="$(git rev-parse "origin/$BRANCH")"
if [[ "$LOCAL" == "$REMOTE" ]]; then
  exit 0
fi
if ! git merge-base --is-ancestor "$LOCAL" "$REMOTE"; then
  if git merge-base --is-ancestor "$REMOTE" "$LOCAL"; then
    exit 0
  fi
  log "Local branch and origin/$BRANCH have diverged; refusing update."
  exit 1
fi

rollback_build_failure() {
  status=$?
  if [[ $status -ne 0 && "${BUILDING:-0}" == 1 ]]; then
    trap - EXIT
    git reset --hard "$LOCAL"
    log "Image build failed; checkout restored to $LOCAL so this commit will not be retried."
  fi
  exit "$status"
}

if ! git pull --ff-only origin "$BRANCH"; then
  log "Fast-forward pull failed; leaving the running stack unchanged."
  exit 1
fi
UPDATED="$(git rev-parse HEAD)"
BUILDING=1
trap rollback_build_failure EXIT
docker compose --env-file backend/.env -f backend/docker-compose.yml build
BUILDING=0
trap - EXIT
docker compose --env-file backend/.env -f backend/docker-compose.yml up -d
log "Updated from $LOCAL to $UPDATED."
