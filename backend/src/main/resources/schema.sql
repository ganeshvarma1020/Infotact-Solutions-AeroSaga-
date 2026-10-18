-- AeroSaga Drone Mission Control Baseline DDL Schema
CREATE DATABASE IF NOT EXISTS aerosaga_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aerosaga_db;

-- 1. Users Table (Authentication & RBAC)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'OPERATOR') NOT NULL DEFAULT 'OPERATOR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Drones Table (Physical Fleet Assets)
CREATE TABLE IF NOT EXISTS drones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    battery_level DOUBLE NOT NULL DEFAULT 100.0,
    status ENUM('AVAILABLE', 'IN_MISSION', 'MAINTENANCE', 'OFFLINE') NOT NULL DEFAULT 'AVAILABLE',
    current_lat DOUBLE NOT NULL,
    current_lng DOUBLE NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. Missions Table (Scheduled or Active Flight Jobs)
CREATE TABLE IF NOT EXISTS missions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drone_id BIGINT NULL,
    name VARCHAR(150) NOT NULL,
    destination_lat DOUBLE NOT NULL,
    destination_lng DOUBLE NOT NULL,
    status ENUM('CREATED', 'SCHEDULED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'CREATED',
    scheduled_time DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mission_drone FOREIGN KEY (drone_id) REFERENCES drones(id) ON DELETE SET NULL
);

-- 4. Telemetry Table (Time-Series Avionics & Flight Coordinates)
CREATE TABLE IF NOT EXISTS telemetry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mission_id BIGINT NOT NULL,
    drone_id BIGINT NOT NULL,
    battery_percentage DOUBLE NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    altitude DOUBLE NOT NULL,
    speed DOUBLE NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_telemetry_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE,
    CONSTRAINT fk_telemetry_drone FOREIGN KEY (drone_id) REFERENCES drones(id) ON DELETE CASCADE
);

-- 5. Mission Events Table (Audit Log for Temporal State Transitions)
CREATE TABLE IF NOT EXISTS mission_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mission_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    description TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE
);
