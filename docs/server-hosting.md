# Hosting AirPx PhotoCloud on a server

The Compose stack runs PostgreSQL, the Quarkus API, and the web app. Only the
web app is exposed on the server, bound to `127.0.0.1:8081`; the API is bound
to `127.0.0.1:${APP_PORT}` and PostgreSQL has no published port. Nginx serves
the web app and proxies `/api/` to the API over the Compose network.

## Configure and start

On the server, clone this repository and create `backend/.env` from
`backend/.env.example`. Set strong, unique values for the database passwords
and bootstrap password. Keep `POSTGRES_*` equal to the corresponding `DB_*`
credentials, leave `DB_HOST=db`, and choose an unused localhost `APP_PORT`
(8080 by default). `APP_PORT` is the only host port setting; the web service
uses 8081. The compose file and updater read `backend/.env`; it is never
overwritten by updates.

Start the stack from the repository root:

```sh
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
sudo tailscale serve --bg 8081
tailscale serve status
```

Serve terminates HTTPS using a certificate for the server's MagicDNS name and
forwards requests to `127.0.0.1:8081`. Its HTTPS address serves both the web
app and `/api`. Serve is private to tailnet devices. Do not enable Tailscale
Funnel, which would make it publicly reachable. See the [Tailscale Serve
guide](https://tailscale.com/docs/features/tailscale-serve) for HTTPS setup
and current CLI details.

## Check reboot recovery

After confirming the stack works, reboot the server:

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
`origin` branch every five minutes. It fetches first and exits without touching
containers when there are no new commits. When updates are available, it
fast-forward pulls, builds both images, then applies them. A failed fetch,
pull, or build does not run `compose up`, so running containers remain in
place. The updater reads (but never writes) `backend/.env`.

Edit `deploy/airpx-update.service` before installing it. Replace `CHANGE_ME`
with the Linux account that owns the checkout and replace both
`/path/to/airpx-PhotoCloud` values with the absolute clone path. The account
needs permission to read the checkout and run Docker. Copy the unit files and
enable the timer:

```sh
sudo cp deploy/airpx-update.service deploy/airpx-update.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now airpx-update.timer
systemctl list-timers airpx-update.timer
```

The timer starts two minutes after boot and repeats five minutes after each
activation. Inspect a run with `sudo systemctl start airpx-update.service` and
`journalctl -u airpx-update.service`.
