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

## Least-privilege runtime database user

> NEN-7510 A.8.2 (privileged access rights) / A.8.3 (least privilege) — hardening
> checklist §2.

The `openmrs/openmrs-core` image connects with a **single** database account that both
runs Liquibase (DDL) at startup **and** serves runtime traffic. Left on the MariaDB
image default that account holds `ALL PRIVILEGES` on the database — so an
application-layer SQL injection or a compromised module could `DROP`/`ALTER` the schema
(threat-model TM-E2, audit finding SQ7). We split the roles into two accounts:

| Account | Env vars | Privileges | Used for |
|---------|----------|------------|----------|
| Migration / admin | `DB_USER` / `DB_PASSWORD` | full rights on the DB | schema creation + Liquibase upgrades at deploy time |
| Runtime | `DB_APP_USER` / `DB_APP_PASSWORD` | `SELECT, INSERT, UPDATE, DELETE` only — **can never `DROP`/`ALTER`/`CREATE`/`GRANT`** | steady-state application traffic |

The runtime account is provisioned automatically on first DB initialization by
[`db-init/10-runtime-least-privilege.sh`](db-init/10-runtime-least-privilege.sh), which
the base compose mounts read-only into `/docker-entrypoint-initdb.d`.

**Deploy procedure (prod):**

1. First boot migrates the schema using the privileged `DB_USER` (Liquibase needs DDL) —
   this is the default in `docker/prod/docker-compose.yml`.
2. Once the schema is at the target version, switch the running app to the runtime
   account: uncomment the `DB_APP_USER`/`DB_APP_PASSWORD` lines for
   `OMRS_CONFIG_CONNECTION_USERNAME`/`_PASSWORD` in the prod override and redeploy.
   Future schema upgrades are a privileged deploy-time operation: temporarily point the
   connection back at `DB_USER`, migrate, then return to `DB_APP_USER`.

**Verify** the runtime account cannot escalate:

```bash
docker compose -f docker-compose.yml -f docker/prod/docker-compose.yml exec db \
  mariadb -uroot -p"$DB_ROOT_PASSWORD" -e "SHOW GRANTS FOR '$DB_APP_USER'@'%';"
# Expect only: GRANT SELECT, INSERT, UPDATE, DELETE ON `openmrs`.* TO ...
# (no DROP/ALTER/CREATE/GRANT)
```

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
