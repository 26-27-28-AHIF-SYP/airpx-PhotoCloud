#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
repo_path="$SCRIPT_DIR/.."
run_user="${SUDO_USER:-$(id -un)}"
branch=""
uninstall=false
check_only=false

usage() {
  cat <<'USAGE'
Usage: deploy/install.sh [--repo-path PATH] [--user USER] [--branch BRANCH] [--check]
       sudo deploy/install.sh [--repo-path PATH] [--user USER] [--branch BRANCH]
       sudo deploy/install.sh --uninstall

Defaults: repository containing this script, SUDO_USER (or current user),
and the currently checked out branch.
USAGE
}

while (($#)); do
  case "$1" in
    --repo-path|--user|--branch)
      (($# >= 2)) || { usage >&2; exit 2; }
      option="$1"
      value="$2"
      shift 2
      case "$option" in
        --repo-path) repo_path="$value" ;;
        --user) run_user="$value" ;;
        --branch) branch="$value" ;;
      esac
      ;;
    --uninstall) uninstall=true; shift ;;
    --check) check_only=true; shift ;;
    --help|-h) usage; exit 0 ;;
    *) printf 'Unknown option: %s\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
done

unit_dir=/etc/systemd/system
service_path="$unit_dir/airpx-update.service"
timer_path="$unit_dir/airpx-update.timer"

if [[ "$uninstall" != true && "$check_only" != true && "$EUID" -ne 0 ]]; then
  echo "Run this script with sudo." >&2
  exit 1
fi

if [[ "$uninstall" == true ]]; then
  [[ "$EUID" -eq 0 ]] || { echo "Run this script with sudo." >&2; exit 1; }
  systemctl disable --now airpx-update.timer >/dev/null 2>&1 || true
  systemctl stop airpx-update.service >/dev/null 2>&1 || true
  rm -f "$service_path" "$timer_path"
  systemctl daemon-reload
  systemctl reset-failed airpx-update.service airpx-update.timer >/dev/null 2>&1 || true
  echo "AirPx automatic updates uninstalled."
  exit 0
fi

for command_name in docker git flock systemctl systemd-analyze getent id; do
  command -v "$command_name" >/dev/null || {
    printf 'Required command not found: %s\n' "$command_name" >&2
    exit 1
  }
done

repo_path="$(cd "$repo_path" && pwd -P)"
git -C "$repo_path" rev-parse --git-dir >/dev/null
[[ -x "$repo_path/deploy/update.sh" ]] || {
  echo "Updater is missing or not executable in $repo_path/deploy." >&2
  exit 1
}
id -u "$run_user" >/dev/null 2>&1 || {
  printf 'User does not exist: %s\n' "$run_user" >&2
  exit 1
}
getent group docker >/dev/null || { echo "The docker group does not exist." >&2; exit 1; }
if ! id -nG "$run_user" | tr ' ' '\n' | grep -Fxq docker; then
  printf 'User %s must belong to the docker group.\n' "$run_user" >&2
  exit 1
fi

current_branch="$(git -C "$repo_path" branch --show-current)"
branch="${branch:-$current_branch}"
[[ -n "$branch" ]] || { echo "Cannot install for a detached HEAD; pass --branch." >&2; exit 1; }
git -C "$repo_path" check-ref-format --branch "$branch" >/dev/null
[[ "$branch" == "$current_branch" ]] || {
  printf 'Checkout branch %s does not match requested update branch %s.\n' "$current_branch" "$branch" >&2
  exit 1
}
git -C "$repo_path" show-ref --verify --quiet "refs/remotes/origin/$branch" || {
  printf 'Remote branch origin/%s is not available in this checkout.\n' "$branch" >&2
  exit 1
}

systemd_value() {
  local value="$1"
  [[ "$value" != *$'\n'* ]] || { echo "Systemd values cannot contain newlines." >&2; return 1; }
  value="${value//\\/\\\\}"
  value="${value// /\\x20}"
  value="${value//$'\t'/\\x09}"
  value="${value//%/%%}"
  printf '%s' "$value"
}

systemd_command() {
  local value="$1"
  [[ "$value" != *$'\n'* ]] || { echo "Systemd values cannot contain newlines." >&2; return 1; }
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//%/%%}"
  printf '"%s"' "$value"
}

render_template() {
  local template="$1" output="$2" content
  content="$(<"$template")"
  content="${content//@USER@/$quoted_user}"
  content="${content//@GROUP@/$quoted_group}"
  content="${content//@REPO_PATH@/$quoted_repo}"
  content="${content//@UPDATE_SCRIPT@/$quoted_script}"
  content="${content//@BRANCH_ENV@/$quoted_branch_env}"
  content="${content//@BRANCH@/$quoted_branch}"
  printf '%s\n' "$content" > "$output"
}

quoted_user="$(systemd_value "$run_user")"
quoted_group="$(systemd_value "$(id -gn "$run_user")")"
quoted_repo="$(systemd_value "$repo_path")"
quoted_script="$(systemd_command "$repo_path/deploy/update.sh")"
quoted_branch="$(systemd_value "$branch")"
quoted_branch_env="$(systemd_command "AIRPX_UPDATE_BRANCH=$branch")"

temp_dir="$(mktemp -d)"
trap 'rm -rf "$temp_dir"' EXIT
render_template "$SCRIPT_DIR/airpx-update.service.in" "$temp_dir/airpx-update.service"
cp "$SCRIPT_DIR/airpx-update.timer.in" "$temp_dir/airpx-update.timer"
systemd-analyze verify "$temp_dir/airpx-update.service" "$temp_dir/airpx-update.timer"

if [[ "$check_only" == true ]]; then
  echo "Rendered systemd units verified; installation was skipped."
  exit 0
fi

install -m 0644 "$temp_dir/airpx-update.service" "$service_path"
install -m 0644 "$temp_dir/airpx-update.timer" "$timer_path"
systemctl daemon-reload
systemctl enable --now airpx-update.timer
printf 'Installed automatic updates for %s on branch %s as %s.\n' "$repo_path" "$branch" "$run_user"
