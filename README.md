# ðŸ›¸ AeroSaga: Autonomous Drone Mission Control Platform

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/ganeshvarma1020/Infotact-Solutions-AeroSaga-)
[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/ganeshvarma1020/Infotact-Solutions-AeroSaga-)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Temporal.io](https://img.shields.io/badge/Temporal.io%20SDK-1.25.1-blue.svg)](https://temporal.io/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00.svg)](https://adoptium.net/)
[![H2 Database](https://img.shields.io/badge/H2-In--Memory%20DB-007acc.svg)](https://www.h2database.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

AeroSaga is an enterprise-grade autonomous drone fleet mission control and telemetry visualization system orchestrated by **Temporal.io**, powered by **Spring Boot 3**, and interfaced via a high-performance **Mission Control Dashboard** with real-time geospatial radar and telemetry tracking.

> ðŸš€ **Direct Cloud Run (No Local Installation Required)**: Click **[Open in GitHub Codespaces](https://codespaces.new/ganeshvarma1020/Infotact-Solutions-AeroSaga-)** to launch the entire platform in your browser. As soon as the container initializes, port `8080` is automatically forwarded and directly opens the live Mission Control UI on a new URL (`https://<codespace-id>-8080.app.github.dev`)!

---

## ðŸŒ 1-Click Cloud Execution (Run on New URL via GitHub Port Forwarding)

You can run and evaluate AeroSaga directly inside GitHub without installing Java, Maven, or Node.js on your computer:

### Option 1: GitHub Codespaces (1-Click Automated Launch)
1. **Launch**: Click the badge above or navigate to [`https://codespaces.new/ganeshvarma1020/Infotact-Solutions-AeroSaga-`](https://codespaces.new/ganeshvarma1020/Infotact-Solutions-AeroSaga-).
2. **Auto-Boot**: GitHub Codespaces provisions a Java 21 container and automatically starts the Spring Boot backend on port `8080`.
3. **Automatic URL / Port Forwarding**:
   - As soon as port `8080` starts listening, GitHub will display a popup: **"Your application running on port 8080 is available. Open in Browser"**.
   - Alternatively, open the **PORTS** tab in the bottom panel:
     - **Port**: `8080` (AeroSaga Mission Control)
     - **Forwarded Address**: Click the globe icon or the URL link (e.g. `https://<codespace-id>-8080.app.github.dev`).
4. **Make Port Public (For External Evaluators / Mentors)**:
   - In the **PORTS** tab, right-click on Port `8080` âž” **Port Visibility** âž” Select **Public**.
   - Anyone with the forwarded URL can now test and view your live platform without logging into GitHub!

### Option 2: Gitpod Cloud Environment
* Click **[Open in Gitpod](https://gitpod.io/#https://github.com/ganeshvarma1020/Infotact-Solutions-AeroSaga-)**.
* The application compiles and opens the live web interface automatically upon completion.

---

## ðŸ”— Live Endpoints on Forwarded Cloud Port (Port 8080)

Once the application is running in Codespaces or locally, access the following URLs:

| Service | Local URL | Forwarded Cloud URL (Codespaces) |
| :--- | :--- | :--- |
| **Mission Control Dashboard** | `http://localhost:8080/` | `https://<codespace-id>-8080.app.github.dev/` |
| **OpenAPI / Swagger UI Docs** | `http://localhost:8080/swagger-ui.html` | `https://<codespace-id>-8080.app.github.dev/swagger-ui.html` |
| **H2 Database Web Console** | `http://localhost:8080/h2-console` | `https://<codespace-id>-8080.app.github.dev/h2-console` |
| **Fleet Summary API** | `http://localhost:8080/api/dashboard/summary` | `https://<codespace-id>-8080.app.github.dev/api/dashboard/summary` |
| **Health Check Endpoint** | `http://localhost:8080/actuator/health` | `https://<codespace-id>-8080.app.github.dev/actuator/health` |

> **H2 Console Credentials**:
> - **JDBC URL**: `jdbc:h2:mem:aerosagadb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
> - **User Name**: `sa`
> - **Password**: *(leave blank)*

---

## ðŸ›ï¸ System Architecture

```text
                                  +---------------------------------------+
                                  |   Web Browser / Mission Control UI    |
                                  |   (Leaflet GIS Radar / Real-Time SVG) |
                                  +---------------------------------------+
                                                     |
                                            HTTP REST / JSON APIs
                                                     v
                                  +---------------------------------------+
                                  |      Spring Boot API Gateway Layer    |
                                  |  - AuthController (/api/auth)         |
                                  |  - DroneFleetController (/api/drones) |
                                  |  - MissionController (/api/missions)  |
                                  |  - DashboardController (/api/dashboard|
                                  +---------------------------------------+
                                                     |
                                                     v
                                  +---------------------------------------+
                                  |        Service & Business Layer       |
                                  |  - DroneSimulationEngine              |
                                  |  - GeofenceService                    |
                                  |  - WeatherService                     |
                                  |  - MissionExecutionService            |
                                  +---------------------------------------+
                                         /                         \
                                        v                           v
              +-----------------------------------+    +----------------------------+
              |     Temporal.io Workflow Engine   |    |      Spring Data JPA       |
              |-----------------------------------|    |----------------------------|
              | DroneMissionWorkflow (Orchestrator|    | H2 In-Memory & MySQL Dual  |
              |  - validatePreFlight (Activity)   |    |  * drones (Fleet assets)   |
              |  - reserveDrone (Activity)        |    |  * drone_missions (Flights)|
              |  - simulateFlightStep (Activity)  |    |  * telemetry_records       |
              |  - executeEmergencyAbort (Saga)   |    |  * users (RBAC & Auth)     |
              +-----------------------------------+    +----------------------------+
```

---

## ðŸ’» Local Quick Start

### Windows One-Click Launcher:
Double-click `Start AeroSaga Mission Control.bat` on your Desktop. It will automatically check dependencies, boot the Spring Boot server, poll port 8080 until ready, and open the browser.

### Manual Terminal Run:
```bash
# Clone the repository
git clone https://github.com/ganeshvarma1020/Infotact-Solutions-AeroSaga-.git
cd Infotact-Solutions-AeroSaga-

# Run with Maven wrapper (Zero installation required)
./mvnw spring-boot:run
```
Open `http://localhost:8080/` in your browser.

---

## ðŸ”‘ Default Credentials & Role-Based Access Control

The platform initializes with pre-configured authentication profiles:

| Role | Username | Password | Access Rights |
| :--- | :--- | :--- | :--- |
| **Drone Operator** | `operator` | `operator123` | Mission dispatch, live telemetry tracking, fault simulation |
| **Mission Administrator** | `admin` | `admin123` | Full fleet asset management, emergency abort, telemetry export |

---

## âš¡ Temporal.io Workflow & The Saga Compensation Pattern

Autonomous drone missions are long-running, asynchronous, and safety-critical:
1. **Deterministic Workflow**: `DroneMissionWorkflowImpl` executes deterministic stages:
   * **Stage 1 (Pre-Flight Validation)**: Geofence compliance, spatial polygon intersection, and battery threshold checks ($\ge 20\%$).
   * **Stage 2 (Resource Locking)**: Reserves drone asset, transitions state to `IN_MISSION`.
   * **Stage 3 (Simulated Flight Routine)**: Waypoint navigation with physics-based battery depletion model.
   * **Stage 4 (Finalization)**: Safe landing, state marked as `COMPLETED`, drone released to `AVAILABLE`.
2. **Saga Compensation (Fault Tolerance)**:
   * When an abort signal or simulated fault (e.g. `GEOFENCE_BREACH`, `LOW_BATTERY`) occurs:
     * Immediately transitions mission to `FAILED` or `ABORTED`.
     * Automatically initiates Return-To-Home (RTH) recovery.
     * Safely releases drone back to `AVAILABLE` (or `MAINTENANCE` if battery is critical).

---

## ðŸ“¡ REST API Catalog

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/dashboard/summary` | Complete fleet telemetry and simulation summary |
| `GET` | `/api/drones` | List all drone assets and real-time battery status |
| `POST` | `/api/missions` | Dispatch a new autonomous drone flight mission |
| `POST` | `/api/missions/{id}/abort` | Trigger emergency abort (Saga compensation) |
| `POST` | `/api/auth/login` | Authenticate user and receive session token |
| `GET` | `/api/auth/report/fleet-status` | Generate active vs inactive fleet analytics report |
| `GET` | `/swagger-ui.html` | Interactive Swagger / OpenAPI documentation |
| `GET` | `/h2-console` | In-memory H2 database web inspector |

---

## ðŸ“ Submission Deliverables

* [`PROJECT_REPORT.md`](PROJECT_REPORT.md) - Formal academic/internship project report.
* [`AeroSaga_Final_Presentation.pptx`](AeroSaga_Final_Presentation.pptx) - 14-slide widescreen presentation deck.
* [`AeroSaga_Build_and_Deploy_Guide.pdf`](AeroSaga_Build_and_Deploy_Guide.pdf) - Official build & go-live manual.
* [`AeroSaga_Project_Explanation.pdf`](AeroSaga_Project_Explanation.pdf) - Comprehensive project explanation report.
* [`DEMONSTRATION_GUIDE.md`](DEMONSTRATION_GUIDE.md) - Live evaluation and demonstration script.

