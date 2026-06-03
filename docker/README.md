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

## GitHub Environments

The `dev` / `test` / `prod` GitHub Environments carry the deployment protection
rules (required reviewers on `prod`, branch restrictions). See issue #3.
