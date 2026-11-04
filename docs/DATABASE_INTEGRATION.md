# AeroSaga Database Integration Architecture

## Dual Persistence Architecture
- In-memory H2 mode: Zero-dependency runtime (jdbc:h2:mem:aerosagadb) with web console on /h2-console
- MySQL 8.0 mode: Enterprise persistent storage across reboots (jdbc:mysql://localhost:3306/aerosaga_db)

## Schema Verification
- drones: Primary asset registry with operational lifecycle state
- drone_missions: Full flight dispatch records with Temporal workflow IDs
- 	elemetry_records: High-frequency flight coordinates and sensor data
- users: Identity and Role-Based Access Control
