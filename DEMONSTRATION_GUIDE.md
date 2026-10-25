# AEROSAGA MISSION CONTROL: DEMONSTRATION & EVALUATION GUIDE
## Step-by-Step Live Walkthrough for Mentors & Evaluators

**Organization:** Infotact Solutions  
**Candidate Name:** Chakravarthula Ganesh  
**Project:** AeroSaga Autonomous Drone Mission Control Platform  

---

## 🛠️ Step 1: System Verification & URLs

The platform components run concurrently across the following addresses:

| Component | URL | Expected Response |
| :--- | :--- | :--- |
| **React Mission Control UI** | [http://localhost:5173](http://localhost:5173) | Interactive Dashboard & Login View |
| **Spring Boot REST API** | [http://localhost:8080](http://localhost:8080) | Port 8080 Active |
| **Swagger OpenAPI Documentation** | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | Interactive API Explorer |
| **MySQL Database Instance** | `localhost:3306` (`aerosaga_db`) | 5 Relational Tables Live |

If you ever need to restart the servers:
- Start Backend: Run [`run-backend.bat`](file:///c:/Users/ganes/Desktop/Infotact%20Sloutions/run-backend.bat)
- Start Frontend: Run [`run-frontend.bat`](file:///c:/Users/ganes/Desktop/Infotact%20Sloutions/run-frontend.bat)

---

## 🌐 Step 2: Authentication & Role Verification

1. Open your browser and navigate to **[http://localhost:5173](http://localhost:5173)**.
2. Observe the authentication screen:
   - Login as an Operator:
     - **Username:** `operator`
     - **Password:** `operator123`
   - Click **"Sign In with JWT"**.
3. **What to Observe:**
   - In your browser's Developer Tools (`F12` $\rightarrow$ Application $\rightarrow$ Local Storage), point out `aerosaga_token` containing the signed JWT string.
   - In Developer Tools Network tab, observe the `Authorization: Bearer <token>` attached to incoming REST requests.
   - The top navigation bar updates with the current user badge: `operator [OPERATOR]`.

---

## 🚁 Step 3: Fleet Management Inspection

1. Look at the top **KPI Metrics Grid**:
   - `Total Fleet Assets`: Shows available drones loaded from MySQL.
   - `Available Assets`: Drones ready for deployment (`AVAILABLE`).
   - `Active In-Flight`: Currently executing missions.
2. In the **"Drone Fleet Status"** table:
   - Point out the pre-seeded drones (e.g. `Alpha-1`, `Beta-2`, `Falcon-X`, `SkyRanger-4`).
   - Point out the battery levels and current GPS coordinates.

---

## 🎯 Step 4: Nominal Mission Execution (Temporal Workflow)

1. Click **"+ Schedule Mission"**.
2. In the modal:
   - Enter Mission Name: `Medical Supply Recon Transit`
   - Destination Coordinates: `Lat: 12.9850`, `Lng: 77.6100`
   - Select Drone: Leave as *Auto-assign* or select `Alpha-1`.
   - Click **"Dispatch Mission"**.
3. The new mission appears in the **Missions Dispatch** table with status `SCHEDULED`.
4. Click **"Launch"**:
   - This dispatches the mission directly into the **Temporal.io Workflow Engine** via `POST /api/missions/{id}/launch`.
5. **Observe the Live Telemetry & Orchestration:**
   - The mission status immediately transitions to `RUNNING`.
   - The assigned drone status changes from `AVAILABLE` $\rightarrow$ `IN_MISSION`.
   - The **Temporal Phase Badge** displays live stages: `Phase: TRANSIT_WAYPOINT_1` through `TRANSIT_WAYPOINT_5`.
   - The **Recharts Line Graph** dynamically plots real-time altitude, velocity, and battery drain over time via 2-second short-polling!
   - Upon completing the 5th waypoint, the workflow finalizes: mission becomes `COMPLETED` and the drone resets back to `AVAILABLE`.

---

## 🚨 Step 5: Fail-Safe Emergency Abort & Saga Rollback

1. Click **"+ Schedule Mission"** and create another mission: `Hazardous Weather Test Flight`.
2. Click **"Launch"** to start the flight.
3. While the flight is in transit (`RUNNING`), click the red **"Abort"** button.
4. **Observe the Saga Pattern Rollback:**
   - An emergency abort signal (`signalEmergencyAbort`) is sent to the running Temporal workflow.
   - Temporal catches the abort, interrupts the loop, and invokes `saga.compensate()`.
   - The mission status updates to `FAILED`.
   - The drone is safely released back to `AVAILABLE`.
   - An immutable `ERROR` audit log is recorded into MySQL `mission_events`.

---

## 📚 Step 6: OpenAPI / Swagger Documentation & Verification

1. Open **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**.
2. Demonstrate the organized REST controllers:
   - `Authentication`: `/api/auth/login`, `/api/auth/register`, `/api/auth/me`
   - `Drones`: `/api/drones`, `/api/drones/available`, `/api/drones/{id}`
   - `Missions`: `/api/missions`, `/api/missions/{id}/launch`, `/api/missions/{id}/abort`, `/api/missions/{id}/temporal-state`
   - `Telemetry`: `/api/telemetry/mission/{id}/latest`
