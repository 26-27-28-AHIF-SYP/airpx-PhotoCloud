# Hosting AirPx PhotoCloud on a server

The Compose stack runs PostgreSQL, the Quarkus API, and the web app. Only the
web app is exposed on the server, bound to `127.0.0.1:8081`; the API is bound
to `127.0.0.1:${APP_PORT}` and PostgreSQL has no published port. Nginx serves
the web app and proxies `/api/` to the API over the Compose network.

## Configure and start

Prerequisites are a Linux server with Docker Engine and the Docker Compose
plugin, Git, `curl`, and Tailscale. Enable Docker at boot with
`sudo systemctl enable --now docker`. Clone this repository and create
`backend/.env` from `backend/.env.example`. Set strong, unique values for the
database passwords and bootstrap password. Keep `POSTGRES_*` equal to the
corresponding `DB_*` credentials, leave `DB_HOST=db`, and choose an unused
localhost `APP_PORT` (8080 by default). `APP_PORT` is the only host port
setting; the web service uses 8081. The compose file and updater read
`backend/.env`; it is never overwritten by updates.

For deployment, keep the checkout on `main` (or the chosen deployment branch)
with a matching local branch and `origin/<branch>`; the updater follows the
branch currently checked out.

Validate the Compose configuration and start the stack from the repository root:

```sh
docker compose --env-file backend/.env -f backend/docker-compose.yml config
docker compose --env-file backend/.env -f backend/docker-compose.yml up -d --build
docker compose --env-file backend/.env -f backend/docker-compose.yml ps
```

The first build packages the Quarkus app in Docker, so no local Maven build is
needed. Compose waits for PostgreSQL health before starting the API, and waits
for API health before starting the web service. Persistent `postgres_data` and
`photo_data` named volumes retain existing database and photo data.

## Serve privately with Tailscale

Install and connect Tailscale on the server and the client devices. In the
Tailscale admin console, enable HTTPS certificates for the tailnet. Then
publish the local web service with Tailscale Serve:

```sh
sudo tailscale serve --bg http://127.0.0.1:8081
tailscale serve status
```

Serve terminates HTTPS using a certificate for the server's MagicDNS name and
forwards requests to `127.0.0.1:8081`. Its HTTPS address serves both the web
app and `/api`. Serve is private to tailnet devices. Do not enable Tailscale
Funnel, which would make it publicly reachable. See the [Tailscale Serve
guide](https://tailscale.com/docs/features/tailscale-serve) for HTTPS setup
and current CLI details.

## Check reboot recovery

After confirming the stack works, reboot the server. `restart: unless-stopped`
restarts the containers when Docker starts; Docker itself must be enabled at
boot as described above.

```sh
sudo reboot
```

After it reconnects, verify the containers restarted and the endpoints answer:

```sh
docker compose --env-file backend/.env -f backend/docker-compose.yml ps
curl --fail http://127.0.0.1:8081/api
curl --fail http://127.0.0.1:8081/
tailscale serve status
```

The first curl should return the API's `{"status":"up"}` response through
nginx; the second should return the web app HTML. The Tailscale HTTPS URL
should also load from another device on the tailnet.

## Install automatic updates

The updater checks the repository's current branch against its matching
`origin` branch every two minutes (first check one minute after boot). It uses
a lock to prevent overlapping runs and exits without touching containers when
there are no new commits. A dirty tree or diverged branch is logged and
rejected. When updates are available, it fast-forward pulls, builds both images,
then applies them. A failed fetch or pull does not run `compose up`; a failed
build restores the checkout to its previous commit and leaves running
containers in place. Logs, including timestamps, are available in journald.
The updater reads (but never writes) `backend/.env`.

Install the updater as root after checking out the deployment branch. The
installer defaults to this repository, the invoking user (`SUDO_USER`), and
the current branch. It verifies rendered systemd units before installation;
the service account must be in the `docker` group. Check the rendered units
without root or system changes with `./deploy/install.sh --check`.

```sh
sudo ./deploy/install.sh --branch main
systemctl list-timers airpx-update.timer
```

The timer starts one minute after boot and repeats two minutes after each
activation. Inspect a run with `sudo systemctl start airpx-update.service` and
`journalctl -u airpx-update.service -o short-iso`. Use
`./deploy/install.sh --check` to render and verify the units without
installing them, or `sudo ./deploy/install.sh --uninstall` to remove the timer
and service.

## Rollback and troubleshooting

For an application regression, revert the bad change on the tracked branch and
push the revert; the updater will build and apply it. For an urgent server-only
rollback, stop the timer, check out the last known-good commit, build and start
that version. Once the deployment branch is repaired, switch back to it and
re-enable the timer:

```sh
REPO_PATH=/path/to/airpx-PhotoCloud
sudo systemctl stop airpx-update.timer
git -C "$REPO_PATH" checkout --detach <known-good-commit>
docker compose --env-file "$REPO_PATH/backend/.env" -f "$REPO_PATH/backend/docker-compose.yml" up -d --build
git -C "$REPO_PATH" switch main
sudo systemctl enable --now airpx-update.timer
```

If containers are unhealthy, inspect
`docker compose --env-file backend/.env -f backend/docker-compose.yml ps` and
`docker compose --env-file backend/.env -f backend/docker-compose.yml logs --tail=200 app web db`.
If updates are rejected, check the service journal, then resolve any dirty
files or branch divergence in the checkout. If the web page loads but API
requests fail, check app health and that Nginx can resolve the `app` service.
If the tailnet URL is unavailable, check `tailscale status`,
`tailscale serve status`, and that Funnel is not enabled.

## Acceptance tests

Run the verification script from the checkout after installing the timer:

```sh
./deploy/verify.sh
```

### Reboot recovery

Confirm Docker is enabled at boot and the Compose stack is healthy, then reboot:

```sh
sudo reboot
```

After reconnecting to the server, run `./deploy/verify.sh`. It checks that the
three containers are running, the app is healthy, Postgres has no host port,
the app and web ports bind only to loopback, `/api` responds through Nginx, and
the update timer is enabled and active. It also checks the Tailscale Serve
mapping when Tailscale is installed.

### Automatic update

Push a harmless commit to the branch configured in the installer. For a
commit-only check that follows the repository trailer convention, run on the
development checkout:

```sh
git commit --allow-empty -m 'chore(deploy): verify automatic update' -m "Co-authored-by: Claude <noreply@anthropic.com>
Co-authored-by: Codex <noreply@openai.com>"
git push origin main
```

On the server, follow the updater log and check the deployed checkout revision:

```sh
sudo journalctl -u airpx-update.service -f
git -C /path/to/airpx-PhotoCloud rev-parse --short HEAD
```

Within five minutes, the log should report the old and new commit hashes and
the server checkout should match the pushed commit. Run `./deploy/verify.sh`
again to confirm the stack remains healthy.

### Failed build recovery

Use a disposable deployment branch rather than `main`. Record the current app
container ID with `docker inspect quarkus-app --format '{{.Id}} {{.State.Health.Status}}'`,
push a commit that makes the backend Docker build fail, and wait for the
scheduled update. Follow `sudo journalctl -u airpx-update.service -f`.
The log should state that the build failed and the checkout was restored. Run
the same `docker inspect` command and confirm the container ID is unchanged and
healthy. The recorded app container should still be running and healthy, and
`./deploy/verify.sh` should pass. Repair or revert the test commit on that
branch, then confirm the next update succeeds. The failed commit is logged on
each retry until the branch is repaired; the timer does not apply its image.
