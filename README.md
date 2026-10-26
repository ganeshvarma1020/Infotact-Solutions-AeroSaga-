# 🛸 AeroSaga: Autonomous Drone Mission Control Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Temporal.io](https://img.shields.io/badge/Temporal.io%20SDK-1.25.1-blue.svg)](https://temporal.io/)
[![React](https://img.shields.io/badge/React-18.3-61DAFB.svg)](https://reactjs.org/)
[![Vite](https://img.shields.io/badge/Vite-5.4-646CFF.svg)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-orange.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

AeroSaga is an enterprise-grade autonomous drone fleet mission control and telemetry visualization system orchestrated by **Temporal.io**, powered by **Spring Boot**, and interfaced via a **React** dashboard with live telemetry charting and geospatial waypoint tracking.

---

## 🏛️ System Architecture

```text
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

## 📋 Prerequisites

Before running the project, ensure you have the following installed on your machine:
* **Java Development Kit (JDK)**: Version 17 or higher (`java -version`)
* **Node.js**: Version 18 or higher (`node -v`) & `npm` (`npm -v`)
* **MySQL Server**: Version 8.0 running locally on port `3306`
* **Git**: To clone and version-control the repository

---

## ⚙️ Environment Configuration

This project is configured out of the box with safe environment variable defaults. You do not need to commit any passwords or secrets to Git.

A template `.env.example` is provided:
```properties
# Database Configuration
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DB=aerosaga_db
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_mysql_password_here

# Server Port & JWT Configuration
SERVER_PORT=8080
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250655368566D5970
JWT_EXPIRATION_MS=86400000

# CORS & Temporal Workflow
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
TEMPORAL_TARGET=127.0.0.1:7233
TEMPORAL_NAMESPACE=default
```

You can pass environment variables in your terminal, or override them in `backend/application-local.yml` (which is automatically ignored by Git).

---

## 🚀 Quick Start Guide

### Option A: One-Click Master Launcher (Windows)
If you are on Windows, simply double-click:
```bat
START_AEROSAGA.bat
```
This script launches both the backend and frontend in separate consoles and automatically opens `http://localhost:5173` in your default browser.

---

### Option B: Manual Setup (Windows / macOS / Linux)

#### 1. Setup MySQL Database
Ensure your local MySQL service is active. The database schema `aerosaga_db` and tables will be created automatically upon backend startup via Hibernate `ddl-auto: update`, or you can execute the DDL script manually:
```sql
-- Located at backend/src/main/resources/schema.sql
mysql -u root -p < backend/src/main/resources/schema.sql
```

#### 2. Start the Spring Boot Backend (Port 8080)
Open a terminal in the `backend/` directory:

**Windows**:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**macOS / Linux**:
```bash
cd backend
chmod +x ./mvnw
./mvnw spring-boot:run
```

The backend starts with an embedded Temporal workflow test environment (no external Temporal cluster setup required).
* **API Base URL**: `http://localhost:8080/api`
* **Swagger UI Documentation**: `http://localhost:8080/swagger-ui.html`

#### 3. Start the React Frontend (Port 5173)
Open a second terminal in the `frontend/` directory:

```bash
cd frontend
npm install
npm run dev
```

* **Mission Control Web App**: `http://localhost:5173`

---

## 🔑 Pre-seeded Login Credentials

The application automatically seeds initial administrative and operator accounts upon first launch:

| Role | Username | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Full fleet control, mission dispatch, abort overrides, asset registration |
| **Mission Operator**| `operator` | `operator123` | Mission dispatch, live telemetry monitoring, status tracking |

---

## ⚡ Temporal.io Workflow & The Saga Pattern

Autonomous drone missions are long-running, asynchronous, and safety-critical:
1. **Deterministic Workflow**: `DroneMissionWorkflowImpl` executes deterministic stages:
   * **Stage 1 (Validation)**: Pre-flight coordinate validation, battery threshold inspection ($\ge 20\%$).
   * **Stage 2 (Resource Locking)**: Reserves drone asset, transitions state to `IN_MISSION`.
   * **Stage 3 (Simulated Flight Routine)**: Iterative waypoint navigation with physics-based battery/altitude models.
   * **Stage 4 (Finalization)**: Safe landing, state marked as `COMPLETED`, drone released to `AVAILABLE`.
2. **Saga Compensation (Fault Tolerance)**:
   * When an abort signal (`signalEmergencyAbort`) or simulated hardware failure occurs, Temporal's Saga compensation pattern executes rollback:
     * Immediately transitions mission to `FAILED` or `CANCELLED`.
     * Automatically initiates Return-To-Home (RTH) recovery.
     * Safely releases drone back to `AVAILABLE` (or `MAINTENANCE` if battery is critical).
     * Writes immutable audit logs into `mission_events`.

---

## 📡 REST API Catalog

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate and obtain JWT Bearer token | Public |
| `POST` | `/api/auth/register` | Register new user account | Public |
| `GET` | `/api/auth/me` | Fetch authenticated identity | Bearer JWT |
| `GET` | `/api/drones` | List all fleet assets | Bearer JWT |
| `GET` | `/api/drones/available` | List drones ready for flight dispatch | Bearer JWT |
| `POST` | `/api/drones` | Register a new drone asset | Bearer JWT (Admin) |
| `POST` | `/api/missions` | Schedule a new flight mission | Bearer JWT |
| `POST` | `/api/missions/{id}/launch` | Dispatch mission into Temporal workflow engine | Bearer JWT |
| `POST` | `/api/missions/{id}/abort` | Trigger emergency abort (Saga compensation) | Bearer JWT |
| `GET` | `/api/missions/{id}/temporal-state` | Query live Temporal workflow execution phase | Bearer JWT |
| `GET` | `/api/telemetry/mission/{id}/latest` | Fetch latest telemetry snapshot | Bearer JWT |

---

## 🧪 Running Automated Tests

Run backend unit and integration test suites:
```bash
cd backend
./mvnw test
```
All integration tests (including JWT authentication, Temporal workflow execution, and Saga compensation tests) run in an isolated test environment.

---

## 📁 Submission Deliverables

This repository contains the complete internship submission package:
* `backend/` - Spring Boot 3.3.4 + Temporal.io micro-architecture source code.
* `frontend/` - React 18 + Vite + TailwindCSS + Recharts source code.
* [`PROJECT_DOCUMENTATION.md`](PROJECT_DOCUMENTATION.md) - Complete architecture and API specifications.
* [`PROJECT_REPORT.md`](PROJECT_REPORT.md) - Formal academic/internship project report.
* [`AeroSaga_Final_Presentation.pptx`](AeroSaga_Final_Presentation.pptx) - 14-slide widescreen presentation deck.
* [`AeroSaga_Build_and_Deploy_Guide.pdf`](AeroSaga_Build_and_Deploy_Guide.pdf) - Comprehensive build and deployment guide.
* [`MODEL_EVALUATION_RESULTS.md`](MODEL_EVALUATION_RESULTS.md) - Battery kinematics, throughput, and recovery benchmarks.
* [`DEMONSTRATION_GUIDE.md`](DEMONSTRATION_GUIDE.md) - Step-by-step evaluation and live demonstration script.
