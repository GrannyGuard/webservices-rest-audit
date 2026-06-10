# OTAP Docker setup

Per-environment Docker Compose for the OpenMRS REST Web Services module, mapping
to the **O**ntwikkel / **T**est / **A**cceptatie / **P**roductie street.

## Layout

| Environment | Override file | GitHub Environment |
|-------------|---------------|--------------------|
| Development | `docker/dev/docker-compose.yml`  | `dev`  |
| Test        | `docker/test/docker-compose.yml` | `test` |
| Production  | `docker/prod/docker-compose.yml` | `prod` |

The root `docker-compose.yml` holds the shared base (db + openmrs); each override
only changes what differs (ports, restart policy, volumes, resource limits).

## Usage

```bash
cp .env.example .env        # fill in real values first

# Development
docker compose -f docker-compose.yml -f docker/dev/docker-compose.yml up -d

# Test (dispose of data afterwards)
docker compose -f docker-compose.yml -f docker/test/docker-compose.yml up -d
docker compose -f docker-compose.yml -f docker/test/docker-compose.yml down -v

# Production-like
docker compose -f docker-compose.yml -f docker/prod/docker-compose.yml up -d
```

## Environment differences

- **dev** — DB port published, `.omod` hot-deploy mount, no restart, debug on.
- **test** — isolated host port (8081), disposable, debug off.
- **prod** — `restart: always`, DB not published, memory limit, image tag must be pinned.

## Deploying the module (dev)

The `openmrs/openmrs-core` image **wipes `/openmrs/data/modules` on every boot** and
repopulates it from `/openmrs/distribution/openmrs_modules` (see `startup-init.sh`).
So that distribution dir — not `data/modules` — is the supported drop point. The dev
override read-only-mounts `docker/dev/modules/` there; any `.omod` placed in that host
folder is auto-loaded on the next `up`/restart.

```powershell
# build + stage + restart in one step
./docker/dev/deploy-module.ps1
```

Or manually: `mvn clean package -DskipTests`, then copy
`omod/target/webservices.rest-*.omod` to `docker/dev/modules/webservices.rest.omod`.

REST then answers at `http://localhost:8080/openmrs/ws/rest/v1/...`
(e.g. `GET /openmrs/ws/rest/v1/session`). A `No mapping for GET /openmrs/` warning on
the bare context root is normal and unrelated.

## Core image version

`OPENMRS_TAG` **must stay on the `2.8.x` line** (`.env` pins `2.8.7`). The
webservices.rest 3.2.0 module targets Platform 2.8.6 (Spring 5); the `nightly` tag has
moved to Spring 6, which removed `CommonsMultipartResolver` and breaks the module's
Spring context. Changing the core version requires wiping the DB volume
(`docker compose ... down -v`) because the schema is version-specific.

## GitHub Environments

The `dev` / `test` / `prod` GitHub Environments carry the deployment protection
rules (required reviewers on `prod`, branch restrictions). See issue #3.
