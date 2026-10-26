# AeroSaga: Technical System Documentation & Architecture Guide

**System Name:** AeroSaga Autonomous Drone Mission Control Platform  
**Organization:** Infotact Solutions  
**Author:** Chakravarthula Ganesh  
**Tech Stack:** Java 25 (LTS) / 21 Target, Spring Boot 3.3.4, Temporal.io Java SDK 1.25.1, Spring Data JPA, MySQL 8.0, React 18, Vite, Recharts, Axios, Spring Security (JJWT)  

---

## 1. Architectural Overview

AeroSaga implements a decoupled hexagonal micro-architecture comprising an API Gateway / Security Layer, Business Logic Layer, Stateful Orchestration Engine (Temporal.io), and a Decoupled Reactive Presentation Layer (React 18).

```
                                  +---------------------------------------+
                                  |   Web Browser / Mission Control UI    |
                                  |   (React 18 / Vite / Recharts / JWT)  |
                                  +---------------------------------------+
                                                     |
                                            HTTP REST / Bearer JWT
                                                     v
                                  +---------------------------------------+
                                  |      Spring Boot API Gateway Layer    |
                                  |  - AuthController (/api/auth)         |
                                  |  - DroneController (/api/drones)      |
                                  |  - MissionController (/api/missions)  |
                                  |  - TelemetryController (/api/telemetry|
                                  +---------------------------------------+
                                                     |
                                                     v
                                  +---------------------------------------+
                                  |        Service & Business Layer       |
                                  |  - AuthService                        |
                                  |  - DroneFleetService                  |
                                  |  - MissionService                     |
                                  |  - TelemetryService                   |
                                  +---------------------------------------+
                                         /                         \
                                        v                           v
              +-----------------------------------+    +----------------------------+
              |     Temporal.io Workflow Engine   |    |      Spring Data JPA       |
              |-----------------------------------|    |----------------------------|
              | DroneMissionWorkflow (Orchestrator|    | MySQL 8.0: aerosaga_db     |
              |  - validateMission (Activity)     |    |  * users (RBAC & JWT)      |
              |  - reserveDrone (Activity)        |    |  * drones (Fleet assets)   |
              |  - simulateFlightStep (Activity)  |    |  * missions (Flight jobs)  |
              |  - finalizeMission (Activity)     |    |  * telemetry (Time series) |
              |  - compensateMissionFailure (Saga)|    |  * mission_events (Audits) |
              +-----------------------------------+    +----------------------------+
```

---

## 2. Layered Responsibilities

### 2.1 Presentation Layer (React + Vite)
- **Port:** `5173`
- **Responsibilities:**
  - Token-based identity management (`localStorage` token persistence, Axios Bearer interceptor, 401 unauthenticated redirect).
  - High-frequency data visualization via **Recharts** (`Altitude`, `Speed`, `Battery` plotted in real-time).
  - Short-polling mechanism fetching `/api/telemetry/mission/{id}/latest` every 2,000 ms.
  - Operator controls: Mission creation, Temporal workflow launching, and Emergency Abort triggering.

### 2.2 Security & Controller Layer (Spring Boot)
- **Port:** `8080`
- **Responsibilities:**
  - `JwtTokenProvider`: Generates HMAC-SHA256 tokens with configurable expiration (24h default).
  - `JwtAuthenticationFilter`: Inspects HTTP `Authorization: Bearer <token>` on incoming requests.
  - `SecurityConfig`: Configures stateless session management, BCrypt hashing, and cross-origin permissions for `http://localhost:5173`.
  - Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, `@Size`).
  - `GlobalExceptionHandler`: Returns standardized RFC-7807 formatted error envelopes.

### 2.3 Orchestration Layer (Temporal.io Workflows)
- **Interface:** `DroneMissionWorkflow`
- **Implementation:** `DroneMissionWorkflowImpl`
- **Responsibilities:**
  - Guarantees deterministic, durable execution across long-running flights.
  - Automatically recovers workflow state from event history if host nodes restart.
  - Manages the **Saga Pattern** for fail-safe physical compensation.
  - Exposes zero-cost state queries (`queryCurrentPhase`, `queryCurrentStep`) without database round-trips.

### 2.4 Activity Layer (Temporal Activities)
- **Interface:** `MissionActivities`
- **Implementation:** `MissionActivitiesImpl`
- **Responsibilities:**
  - Encapsulates non-deterministic side-effects (database transactions, external HTTP calls, telemetry physics).
  - Executes with exponential backoff retry policies (`maxAttempts = 3`, `backoffCoefficient = 2.0`).

### 2.5 Data Persistence Layer (Spring Data JPA / MySQL 8.0)
- **Database:** `aerosaga_db` on `localhost:3306`
- **Responsibilities:**
  - Referential integrity enforcement (Foreign keys, ON DELETE cascading rules).
  - Object-Relational Mapping via Hibernate 6.5.
  - HikariCP connection pooling (`HikariPool-1`).

---

## 3. Database Schema & Entity Specification

### 3.1 Entity-Relationship Layout
```
[ users ]
  - id (PK)
  - username (UNIQUE)
  - password_hash
  - role (ENUM: ADMIN, OPERATOR)
  - created_at

[ drones ] 1 <----+
  - id (PK)       |
  - name          |
  - model         |
  - battery_level |
  - status (ENUM) |
  - current_lat   |
  - current_lng   |
                  |
[ missions ] N ---+ (fk_mission_drone)
  - id (PK)       1 <----+
  - drone_id (FK)        |
  - name                 |
  - destination_lat      |
  - destination_lng      |
  - status (ENUM)        |
  - scheduled_time       |
                         |
[ telemetry ] N ---------+ (fk_telemetry_mission)
  - id (PK)
  - mission_id (FK)
  - drone_id (FK)
  - battery_percentage
  - latitude, longitude
  - altitude, speed
  - timestamp

[ mission_events ] N ----+ (fk_event_mission)
  - id (PK)
  - mission_id (FK)
  - event_type (VARCHAR)
  - description (TEXT)
  - timestamp
```

---

## 4. REST API Specification

### Authentication & Identity
| Method | Endpoint | Request Body | Response Body | Description |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | `LoginRequest` | `AuthResponse` | Authenticates credentials and returns JWT Bearer token |
| `POST` | `/api/auth/register` | `RegisterRequest` | `AuthResponse` | Registers a new user (`ADMIN` or `OPERATOR`) |
| `GET` | `/api/auth/me` | None | User profile map | Returns current user username and authorities |

### Fleet Assets
| Method | Endpoint | Parameters | Response | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/drones` | None | `List<DroneDTO>` | Lists all fleet assets |
| `GET` | `/api/drones/available`| None | `List<DroneDTO>` | Lists drones ready for assignment |
| `GET` | `/api/drones/{id}` | Path: `id` | `DroneDTO` | Retrieves specific drone |
| `POST` | `/api/drones` | `DroneDTO` | `DroneDTO` | Registers new physical drone |
| `PATCH`| `/api/drones/{id}/status`| Query: `status` | `DroneDTO` | Updates asset operational status |

### Missions & Temporal Orchestration
| Method | Endpoint | Parameters | Response | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/missions` | Query: `status` (opt) | `List<MissionDTO>`| Lists all missions |
| `GET` | `/api/missions/{id}` | Path: `id` | `MissionDTO` | Retrieves mission record |
| `POST` | `/api/missions` | `MissionCreateDTO` | `MissionDTO` | Schedules a new flight mission |
| `POST` | `/api/missions/{id}/launch` | Path: `id` | `MissionDTO` | Dispatches mission into Temporal workflow |
| `POST` | `/api/missions/{id}/abort` | Query: `reason` (opt) | Message map | Sends emergency abort signal (triggers Saga rollback) |
| `GET` | `/api/missions/{id}/temporal-state`| Path: `id` | State map | Queries live Temporal phase, step, and status |

### Telemetry Stream
| Method | Endpoint | Parameters | Response | Description |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/telemetry/mission/{id}` | Path: `id` | `List<TelemetryDTO>` | Full telemetry trajectory history |
| `GET` | `/api/telemetry/mission/{id}/latest`| Path: `id` | `TelemetryDTO` | Latest telemetry point for short-polling |
| `POST` | `/api/telemetry` | `TelemetryDTO` | `TelemetryDTO` | Ingests new coordinate/battery point |

---

## 5. Security Architecture (JJWT & Spring Security)

1. **Password Encryption:** Passwords are never stored in plaintext. Passwords are encrypted using BCrypt work-factor 10 (`$2a$10$...`).
2. **Token Format:**
   - **Header:** `{"alg": "HS256", "typ": "JWT"}`
   - **Payload Claims:**
     - `sub`: Username (e.g. `admin`)
     - `roles`: `ROLE_ADMIN` or `ROLE_OPERATOR`
     - `iat`: Timestamp of issue
     - `exp`: Timestamp of expiration (24 hours)
   - **Signature:** Cryptographic signature verified against `app.jwt.secret`.
3. **CORS:** Allows headers `Authorization`, `Content-Type`, `X-Requested-With` from `http://localhost:5173` and `http://localhost:3000`.
