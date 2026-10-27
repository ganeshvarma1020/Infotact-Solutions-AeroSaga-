# AeroSaga: Temporal.io Autonomous Drone Mission Control

Java 21 + Spring Boot 4 + Temporal. Plans and flies **simulated** drone missions.
Each mission is a durable Temporal workflow. If a step fails (low battery, no-fly zone, operator abort),
every finished step is undone in reverse order (saga pattern). Missions survive server and app restarts.

Full step-by-step guide (install, build, test, deploy, troubleshoot): **AeroSaga_Build_and_Deploy_Guide.pdf**

## Quick start (laptop)

```bash
# Terminal 1: workflow engine (keeps data in temporal.db)
temporal server start-dev --db-filename temporal.db

# Terminal 2: tests, then the app
mvn test                      # expect: Tests run: 59, Failures: 0, Errors: 0
mvn spring-boot:run
```

Dashboard: http://localhost:8080 (login `operator` / `local-dev-only`).
Temporal UI: http://localhost:8233

## Project map

```
src/main/java/com/infotact/aerosaga/
  domain/       pure rules: geography, routes, no-fly zones, mission validation
  fleet/        simulated drones, battery model, flight simulator with fault injection
  airspace/     flight slots + temporary no-fly zones
  state/        save/restore simulator state as text
  workflow/     Temporal workflow (saga), activities, query/signal contracts
  persistence/  JPA entities, repositories, mission journal, simulator state store
  service/      MissionService: start, query, abort, list
  api/          REST controllers, DTOs, error handling
  config/       Spring wiring: core beans, Temporal client + worker, security
src/main/resources/
  static/       dashboard (HTML + JS + CSS, Leaflet map)
  db/migration/ Flyway SQL migrations
deploy/         Caddyfile (HTTPS), backup script
Dockerfile, docker-compose.yml, .env.example, .github/workflows/ci.yml
```

## REST API (all require login, POST/DELETE need the CSRF header)

| Method | Path | Purpose |
|---|---|---|
| POST | /api/missions | Start mission (202) |
| GET | /api/missions | Last 50 missions |
| GET | /api/missions/{id} | Live state (workflow query) |
| GET | /api/missions/{id}/events | Stored history |
| POST | /api/missions/{id}/abort | Abort (signal) |
| GET | /api/fleet | Drones, slots, temporary zones |
| POST | /api/fleet/{droneId}/recharge | Fresh battery |
| DELETE | /api/airspace/zones/{name} | Lift temporary zone |
| GET | /api/config | Operating limits for the map |
| GET | /api/csrf | CSRF token for the dashboard |
| GET | /api/ping, /actuator/health | Public health checks |

## Production (one Ubuntu server)

```bash
cp .env.example .env    # fill every value
docker compose up -d --build
```

Caddy serves HTTPS on `$DOMAIN`. Temporal UI stays private (SSH tunnel to port 8233).
Daily backup: `bash deploy/backup.sh` (cron entry in the guide).

## Known limits

- Single-node Temporal (dev server with SQLite file). For high availability use Temporal Cloud or a full Temporal cluster.
- Drones are simulated. Battery, flight time and faults follow a simple, documented model.
- One app instance at a time (simulator state lives in one process, saved to the database).
