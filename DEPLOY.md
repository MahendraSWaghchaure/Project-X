# Deploying Sunju for free

This is the path chosen for this project: **one free-forever Oracle Cloud VM** runs the backend,
Postgres+PostGIS, and Redis via Docker Compose; the **frontend deploys separately to Vercel**
(or Cloudflare Pages/Netlify - the build is a plain static SPA, any of them work identically).
Nothing here has a time limit or a "free for 12 months then billed" catch - Oracle's Always Free
tier and Vercel's Hobby tier are both permanent, not trials.

```
Browser
  |
  |--- https://your-app.vercel.app ------------> Vercel (static React build)
  |
  '--- https://<domain-or-sslip>.io -----------> Oracle VM
                                                    |
                                                  Caddy (auto HTTPS, :80/:443)
                                                    |
                                                  Spring Boot app (:8080, internal only)
                                                    |         \
                                                Postgres+PostGIS   Redis
                                                (internal only)  (internal only)
```

## What this actually costs

**$0/month, indefinitely**, with one honest caveat: Oracle requires a credit card on file to
verify your identity when you sign up, even for Always Free resources. As long as everything you
provision stays within the Always Free limits (which this guide does), you are not charged. Set a
billing alert in the Oracle console anyway so you'd notice if that ever stopped being true.

## Prerequisites

- This repo pushed to GitHub (already done)
- An Oracle Cloud account ([oracle.com/cloud/free](https://www.oracle.com/cloud/free/))
- A Vercel account ([vercel.com](https://vercel.com)) - or Cloudflare Pages/Netlify, sign-up flow differs but the frontend steps below are equivalent
- An SSH client (already on macOS/Linux; use PowerShell's built-in `ssh` on Windows, or PuTTY)

---

## Part 1 — Create the VM

1. Oracle Cloud console → **Compute → Instances → Create Instance**.
2. **Image**: Canonical Ubuntu (latest LTS).
3. **Shape**: click "Change shape" → **Ampere → VM.Standard.A1.Flex** → set OCPU/memory to
   whatever your account's current Always Free allowance is (check the "Always Free eligible"
   badge in the console - it's been reduced over time, most recently to 2 OCPU / 12GB in June
   2026; use the full eligible amount).
   - If you hit "Out of host capacity" - this is Oracle's well-known Ampere capacity crunch, not
     a mistake on your part. Retry in a few minutes, try a different Availability Domain in the
     same region, or try a different Always Free region entirely.
4. **Networking**: use the default VCN it offers to create, "Assign a public IPv4 address" checked.
5. **SSH keys**: let it generate a keypair and download the private key (`ssh-key-....key`). You
   won't see it again.
6. Create the instance, wait for it to go "Running", note its **public IP**.
7. **Open the firewall - two layers, both required:**
   - Cloud level: the instance's subnet → its **Security List** → Ingress Rules → add rules for
     `0.0.0.0/0`, TCP, destination ports `80` and `443` (port 22/SSH is usually open by default -
     confirm it's there too).
   - OS level: Ubuntu images on Oracle ship with `iptables` rules that block everything not
     explicitly allowed, on top of the cloud firewall - SSH in (see Part 3) and run:
     ```sh
     sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
     sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
     sudo netfilter-persistent save
     ```
     Missing this step is the single most common reason "it works via SSH but the website times
     out" on a fresh Oracle VM.

## Part 2 — Get a hostname pointed at it

Caddy's automatic HTTPS needs a real hostname, not a bare IP (Let's Encrypt won't issue a
certificate for an IP address). Two options, both free:

- **Zero setup**: use `<ip-with-dashes>.sslip.io`. For public IP `203.0.113.10`, that's
  `203-0-113-10.sslip.io` - it resolves automatically to the embedded IP, no DNS records to
  create, and Caddy can still get it a real certificate.
- **A real domain you own**: add an `A` record pointing at the VM's public IP (Cloudflare's free
  DNS works well here). Takes a few minutes to propagate.

Either way, write this hostname down - it goes in `.env` as `DOMAIN` in Part 4.

## Part 3 — Install Docker on the VM

```sh
ssh -i /path/to/ssh-key-....key ubuntu@<vm-public-ip>

curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker   # or log out/back in, so `docker` works without sudo
docker compose version   # confirms the Compose plugin is present
```

The Ampere VM is **arm64** - every image this stack uses (`postgis/postgis`, `redis:7-alpine`,
`caddy:2-alpine`, `eclipse-temurin`) ships official multi-arch builds including arm64, so this
isn't a special concern, just worth knowing if you ever add a service image that doesn't.

## Part 4 — Clone the repo and configure secrets

```sh
git clone https://github.com/<your-username>/Project-X.git
cd Project-X

# Generate a real RSA keypair (never the ephemeral dev one) somewhere outside the repo checkout
mkdir -p ~/secrets
openssl genrsa -out ~/secrets/jwt-private.pem 2048
openssl rsa -in ~/secrets/jwt-private.pem -pubout -out ~/secrets/jwt-public.pem

cp .env.prod.example .env
nano .env   # fill in DOMAIN, DB_PASSWORD, JWT_*_KEY_PATH (point at ~/secrets/...),
            # TRACKING_INGEST_SECRET (openssl rand -hex 32), PLATFORM_ADMIN_PASSWORD.
            # CORS_ALLOWED_ORIGINS: fill this in after Part 7 once you know the Vercel URL -
            # the app won't accept cross-origin requests from the frontend until this is set
            # correctly, so come back and update it (then `docker compose ... up -d` again).
```

## Part 5 — Launch the stack

```sh
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps    # all four services should show healthy/running
docker compose -f docker-compose.prod.yml logs -f app   # watch it boot; Ctrl-C to stop tailing
```

First boot compiles the backend inside the `app` build step (a few minutes on a 2-core VM) and
runs every Flyway migration. Once the logs show `Started ProjectXApplication`, you're live.

## Part 6 — Verify

```sh
curl https://<your-domain>/actuator/health
# {"status":"UP", ...}
```

If that hangs or refuses: recheck Part 1's firewall rules first (the most common cause), then
`docker compose -f docker-compose.prod.yml logs caddy` for certificate errors.

## Part 7 — Deploy the frontend

On [vercel.com](https://vercel.com): **New Project → import this repo**.

- **Root Directory**: `frontend`
- **Framework Preset**: Vite (auto-detected)
- **Environment Variables**:
  - `VITE_API_BASE_URL` = `https://<your-domain>` (the bare origin from Part 2 - **no** `/api`
    suffix; the backend's real routes are at root, e.g. `/auth/login`, not `/api/auth/login` -
    that prefix only exists in local dev's Vite proxy rewrite)
  - `VITE_WS_BASE_URL` = same value as above

Deploy. Vercel gives you a `https://<something>.vercel.app` URL - go back to the VM's `.env` and
set `CORS_ALLOWED_ORIGINS` to exactly that (comma-separate if you later add a custom domain on
Vercel too), then:

```sh
docker compose -f docker-compose.prod.yml up -d   # picks up the new CORS_ALLOWED_ORIGINS
```

`frontend/vercel.json` (Vercel) and `frontend/public/_redirects` (Cloudflare Pages/Netlify) are
already in the repo so client-side routes like `/parent/dashboard` don't 404 on a hard refresh -
nothing extra to configure there.

## Part 8 — First login

The seeded platform admin from `.env`'s `PLATFORM_ADMIN_EMAIL`/`PLATFORM_ADMIN_PASSWORD` can sign
in immediately and onboard your first school - see the root `README.md`'s "Quick start" for the
same walkthrough this environment now supports for real.

---

## Keeping it running

- **Updating after a `git push`**:
  ```sh
  cd ~/Project-X && git pull
  docker compose -f docker-compose.prod.yml up -d --build
  ```
- **Backups**: the only state that matters is the `projectx-postgres-data` volume.
  `docker compose -f docker-compose.prod.yml exec postgres pg_dump -U projectx projectx | gzip > backup-$(date +%F).sql.gz`
  periodically (a cron job calling this and copying the file off-VM is worth setting up before
  you rely on this for anything real).
- **Logs**: `docker compose -f docker-compose.prod.yml logs -f app`.
- **The billing cron jobs** (`BillingScheduler`: invoices on the 1st, late fees daily, payroll on
  the 10th) run in-process and fire correctly here, because nothing on this VM ever sleeps -
  unlike a free-tier PaaS backend that spins down on idle.
- **Payments/payroll**: still off by default (`RAZORPAY_ENABLED=false` etc. in `.env`) - flip
  them on with real credentials whenever you actually have a KYC'd Razorpay/RazorpayX account,
  per `backend/README.md`'s "Payments" section.
