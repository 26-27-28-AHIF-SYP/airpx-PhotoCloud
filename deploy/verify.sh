#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
WEB_PORT="${WEB_PORT:-8081}"
API_HEALTH_PATH="${API_HEALTH_PATH:-/api}"
COMPOSE=(docker compose --env-file "$REPO_DIR/backend/.env" -f "$REPO_DIR/backend/docker-compose.yml")
failures=0

check() {
  local description="$1"
  shift
  if "$@"; then
    printf 'PASS %s\n' "$description"
  else
    printf 'FAIL %s\n' "$description"
    failures=$((failures + 1))
  fi
}

containers_running() {
  local services
  services="$("${COMPOSE[@]}" ps --status running --services)" || return 1
  for service in db app web; do
    grep -Fxq "$service" <<<"$services" || return 1
  done
}

app_healthy() {
  local id
  id="$("${COMPOSE[@]}" ps -q app)"
  [[ -n "$id" ]] || return 1
  [[ "$(docker inspect --format '{{.State.Health.Status}}' "$id")" == healthy ]]
}

database_unpublished() {
  local id published
  id="$("${COMPOSE[@]}" ps -q db)"
  [[ -n "$id" ]] || return 1
  published="$(docker inspect --format '{{range $port, $bindings := .NetworkSettings.Ports}}{{if $bindings}}{{range $bindings}}{{printf "%s:%s " .HostIp .HostPort}}{{end}}{{end}}{{end}}' "$id")"
  [[ -z "$published" ]]
}

service_loopback_only() {
  local service="$1" id bindings binding
  id="$("${COMPOSE[@]}" ps -q "$service")"
  [[ -n "$id" ]] || return 1
  bindings="$(docker inspect --format '{{range $port, $items := .NetworkSettings.Ports}}{{range $items}}{{printf "%s:%s\n" .HostIp .HostPort}}{{end}}{{end}}' "$id")"
  [[ -n "$bindings" ]] || return 1
  while IFS= read -r binding; do
    [[ "$binding" == 127.0.0.1:* ]] || return 1
  done <<<"$bindings"
}

api_through_web_healthy() {
  curl --fail --silent --show-error --max-time 10 \
    "http://127.0.0.1:${WEB_PORT}${API_HEALTH_PATH}" \
    | grep -Eq '"status"[[:space:]]*:[[:space:]]*"up"'
}

update_timer_active() {
  systemctl is-enabled --quiet airpx-update.timer \
    && systemctl is-active --quiet airpx-update.timer
}

tailscale_mapping_matches() {
  local status
  status="$(tailscale serve status 2>&1)" || return 1
  grep -Fq "127.0.0.1:${WEB_PORT}" <<<"$status"
}

check "db, app, and web containers are running" containers_running
check "app container is healthy" app_healthy
check "Postgres has no published host port" database_unpublished
check "app binds to 127.0.0.1 only" service_loopback_only app
check "web binds to 127.0.0.1 only" service_loopback_only web
check "API health endpoint succeeds through web" api_through_web_healthy
check "automatic update timer is enabled and active" update_timer_active

if command -v tailscale >/dev/null 2>&1; then
  check "Tailscale Serve maps to 127.0.0.1:${WEB_PORT}" tailscale_mapping_matches
else
  echo "WARN SKIP Tailscale Serve mapping (tailscale command not installed)"
fi

if ((failures > 0)); then
  printf '%s verification check(s) failed.\n' "$failures" >&2
  exit 1
fi
echo "All verification checks passed."
