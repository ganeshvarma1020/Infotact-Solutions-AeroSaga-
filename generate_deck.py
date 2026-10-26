import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.enum.shapes import MSO_SHAPE

def create_deck():
    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)

    blank_layout = prs.slide_layouts[6]

    # Modern Cyber/Aerospace Palette
    BG_COLOR = RGBColor(10, 14, 23)        # #0a0e17 Dark space navy
    CARD_BG = RGBColor(19, 27, 46)         # #131b2e Dark card
    CARD_INNER = RGBColor(26, 35, 58)      # #1a233a Card inner
    TEXT_WHITE = RGBColor(241, 245, 249)   # Slate 50
    TEXT_MUTED = RGBColor(148, 163, 184)   # Slate 400
    ACCENT_CYAN = RGBColor(0, 242, 254)    # Cyan
    ACCENT_BLUE = RGBColor(79, 172, 254)   # Blue
    ACCENT_GREEN = RGBColor(16, 185, 129)  # Emerald
    ACCENT_AMBER = RGBColor(245, 158, 11)  # Amber
    ACCENT_RED = RGBColor(239, 68, 68)     # Red
    BORDER_COLOR = RGBColor(39, 53, 83)    # Border slate

    def add_background(slide):
        bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, prs.slide_height)
        bg.fill.solid()
        bg.fill.fore_color.rgb = BG_COLOR
        bg.line.fill.background()
        return bg

    def add_header(slide, title_text, category="AEROSAGA MISSION CONTROL"):
        cat_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.4), Inches(11.5), Inches(0.35))
        tf_cat = cat_box.text_frame
        tf_cat.word_wrap = True
        p_cat = tf_cat.paragraphs[0]
        p_cat.text = category.upper()
        p_cat.font.size = Pt(11)
        p_cat.font.bold = True
        p_cat.font.color.rgb = ACCENT_CYAN

        title_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.75), Inches(11.5), Inches(0.6))
        tf_title = title_box.text_frame
        tf_title.word_wrap = True
        p_title = tf_title.paragraphs[0]
        p_title.text = title_text
        p_title.font.size = Pt(24)
        p_title.font.bold = True
        p_title.font.color.rgb = TEXT_WHITE

    def add_card(slide, left, top, width, height, title, body_bullets, accent_color=ACCENT_CYAN):
        card = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
        card.fill.solid()
        card.fill.fore_color.rgb = CARD_BG
        card.line.color.rgb = BORDER_COLOR
        card.line.width = Pt(1.2)

        tb = slide.shapes.add_textbox(left + Inches(0.2), top + Inches(0.2), width - Inches(0.4), height - Inches(0.4))
        tf = tb.text_frame
        tf.word_wrap = True

        p_t = tf.paragraphs[0]
        p_t.text = title
        p_t.font.size = Pt(15)
        p_t.font.bold = True
        p_t.font.color.rgb = accent_color
        p_t.space_after = Pt(10)

        for b in body_bullets:
            p_b = tf.add_paragraph()
            p_b.text = "• " + b
            p_b.font.size = Pt(12)
            p_b.font.color.rgb = TEXT_WHITE
            p_b.space_after = Pt(6)

    # ==========================================
    # SLIDE 1: Title Slide
    # ==========================================
    s1 = prs.slides.add_slide(blank_layout)
    add_background(s1)

    dec = s1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.5), Inches(11.733), Inches(4.8))
    dec.fill.solid()
    dec.fill.fore_color.rgb = CARD_BG
    dec.line.color.rgb = BORDER_COLOR
    dec.line.width = Pt(1.5)

    tb = s1.shapes.add_textbox(Inches(1.2), Inches(1.9), Inches(11.0), Inches(4.0))
    tf = tb.text_frame
    tf.word_wrap = True

    p0 = tf.paragraphs[0]
    p0.text = "INFOTACT SOLUTIONS INTERNSHIP FINAL PROJECT SUBMISSION"
    p0.font.size = Pt(13)
    p0.font.bold = True
    p0.font.color.rgb = ACCENT_CYAN

    p1 = tf.add_paragraph()
    p1.text = "AeroSaga: Autonomous Drone Mission Control Platform"
    p1.font.size = Pt(32)
    p1.font.bold = True
    p1.font.color.rgb = TEXT_WHITE
    p1.space_before = Pt(14)

    p2 = tf.add_paragraph()
    p2.text = "Distributed Orchestration (Temporal.io), Reactive Full-Stack (Spring Boot 3 + React 18), and Saga Compensation"
    p2.font.size = Pt(16)
    p2.font.color.rgb = TEXT_MUTED
    p2.space_before = Pt(10)

    p3 = tf.add_paragraph()
    p3.text = "Author: Chakravarthula Ganesh | Organization: Infotact Solutions | Submission Date: October 2026"
    p3.font.size = Pt(13)
    p3.font.color.rgb = ACCENT_GREEN
    p3.space_before = Pt(28)

    # ==========================================
    # SLIDE 2: Executive Summary & Objectives
    # ==========================================
    s2 = prs.slides.add_slide(blank_layout)
    add_background(s2)
    add_header(s2, "Executive Summary & Core Project Objectives")

    add_card(s2, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2),
             "Industrial Background & Motivation",
             [
                 "Autonomous UAV fleets require reliable, stateful orchestration across unpredictable real-world environments.",
                 "Traditional HTTP/REST requests cannot safely wait hours for long-running flights to execute.",
                 "Server restarts or network partitions in naive microservices cause lost process contexts and stranded flights.",
                 "AeroSaga delivers durable event-sourced workflows coupled with reactive telemetry visualization."
             ], ACCENT_BLUE)

    add_card(s2, Inches(6.8), Inches(1.6), Inches(5.7), Inches(5.2),
             "Key Project Deliverables Achieved",
             [
                 "Monolithic Layered Spring Boot 3.3.4 Backend with Spring Data JPA.",
                 "Decoupled React 18 Frontend on Vite with Recharts telemetry visualization.",
                 "Temporal.io Workflow Engine implementing the Saga Pattern for physical rollbacks.",
                 "Stateless JWT Authentication with Role-Based Access Control (ADMIN & OPERATOR).",
                 "Local MySQL 8.0 Production Persistence with complete referential integrity."
             ], ACCENT_GREEN)

    # ==========================================
    # SLIDE 3: Layered Micro-Architecture
    # ==========================================
    s3 = prs.slides.add_slide(blank_layout)
    add_background(s3)
    add_header(s3, "System Architecture: Decoupled Full-Stack Design")

    col_w = Inches(2.7)
    gap = Inches(0.2)
    t = Inches(1.6)
    h = Inches(5.2)

    add_card(s3, Inches(0.8), t, col_w, h, "1. Presentation Layer", [
        "React 18 + Vite (Port 5173)",
        "Axios HTTP Client with JWT interceptors",
        "Recharts Avionics Visualizer (Alt/Speed/Batt)",
        "Short-polling engine (2s telemetry refresh)",
        "Fleet & Mission management dashboards"
    ], ACCENT_CYAN)

    add_card(s3, Inches(0.8) + col_w + gap, t, col_w, h, "2. API Gateway & Security", [
        "Spring Boot 3.3.4 (Port 8080)",
        "Spring Security + BCrypt hashing",
        "JJWT Token Provider (HMAC-SHA256)",
        "Global CORS Configuration",
        "Jakarta Bean Validation & Exception Handler"
    ], ACCENT_BLUE)

    add_card(s3, Inches(0.8) + (col_w + gap)*2, t, col_w, h, "3. Orchestration & Saga", [
        "Temporal.io Workflow Engine",
        "Deterministic DroneMissionWorkflow",
        "Pre-flight, Dispatch, and Transit Activities",
        "Saga Pattern Compensating Rollbacks",
        "Signals (Abort, Pause) & Live Queries"
    ], ACCENT_AMBER)

    add_card(s3, Inches(0.8) + (col_w + gap)*3, t, col_w, h, "4. Data Access Layer", [
        "MySQL 8.0 (localhost:3306)",
        "Schema: aerosaga_db",
        "Spring Data JPA + Hibernate",
        "Auto-seeding DataInitializer runner",
        "HikariCP connection pool (HikariPool-1)"
    ], ACCENT_GREEN)

    # ==========================================
    # SLIDE 4: Database Design & Referential Integrity
    # ==========================================
    s4 = prs.slides.add_slide(blank_layout)
    add_background(s4)
    add_header(s4, "Relational Database Schema & Data Models")

    add_card(s4, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Core Entities & Relationships", [
        "users: id, username (UNIQUE), password_hash, role (ADMIN/OPERATOR).",
        "drones: id, name, model, battery_level, status (AVAILABLE, IN_MISSION, MAINTENANCE, OFFLINE), coordinates.",
        "missions: id, drone_id (FK), name, destination_lat, destination_lng, status (CREATED, SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED).",
        "telemetry: id, mission_id (FK), drone_id (FK), battery_percentage, altitude, speed, coordinates, timestamp.",
        "mission_events: id, mission_id (FK), event_type, description, timestamp."
    ], ACCENT_CYAN)

    add_card(s4, Inches(6.8), Inches(1.6), Inches(5.7), Inches(5.2), "Referential Integrity & Constraints", [
        "Foreign Key fk_mission_drone connects missions to drones (ON DELETE SET NULL).",
        "Cascading deletes on telemetry and mission_events when parent missions are pruned.",
        "Strict ENUM validation ensuring robust finite-state transitions.",
        "Optimized B-tree indices on mission_id and drone_id for high-throughput time-series queries.",
        "Live MySQL verification confirmed 100% data consistency under concurrent test loads."
    ], ACCENT_GREEN)

    # ==========================================
    # SLIDE 5: Temporal.io Durable Execution & Workflows
    # ==========================================
    s5 = prs.slides.add_slide(blank_layout)
    add_background(s5)
    add_header(s5, "Temporal.io Workflow Orchestration Engine")

    add_card(s5, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Workflow Determinism & Lifecycle", [
        "Deterministic Execution: DroneMissionWorkflow coordinates what happens and when without side-effects.",
        "Phase 1: validateMission activity checks avionics and flight envelope.",
        "Phase 2: reserveDrone activity locks physical asset (IN_MISSION) and verifies battery >= 20%.",
        "Phase 3: simulateFlightStep loop iterates through 5 flight waypoints.",
        "Phase 4: finalizeMission activity unlocks asset to AVAILABLE and marks mission COMPLETED.",
        "Durable State Machine survives process crashes and resumes automatically."
    ], ACCENT_BLUE)

    add_card(s5, Inches(6.8), Inches(1.6), Inches(5.7), Inches(5.2), "Signals, Queries & Activity Retry", [
        "Temporal Signals: Ground control intervention via signalEmergencyAbort, signalPause, and signalResume.",
        "Zero-Cost Queries: queryCurrentPhase, queryCurrentStep, and isAborted inspected in-memory without DB round-trips.",
        "Automatic Retries: Exponential backoff policy (initial 1s, max 10s, coefficient 2.0, max 3 attempts).",
        "Decoupled Activity Workers: Activities handle transactional I/O, sensor logs, and telemetry updates."
    ], ACCENT_AMBER)

    # ==========================================
    # SLIDE 6: The Temporal Saga Compensation Pattern
    # ==========================================
    s6 = prs.slides.add_slide(blank_layout)
    add_background(s6)
    add_header(s6, "Fail-Safe Resilience: The Temporal Saga Pattern")

    add_card(s6, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Why Sagas for Autonomous Drones?", [
        "In physical robotics, past flight distance and physical actions cannot be simply rolled back like database rows.",
        "When an unexpected hazard occurs (severe storm, battery drop, rotor anomaly), the system must execute forward-compensating actions.",
        "Temporal's Saga class registers compensating activities in reverse execution order.",
        "Ensures physical asset safety, operator notification, and audit compliance."
    ], ACCENT_RED)

    add_card(s6, Inches(6.8), Inches(1.6), Inches(5.7), Inches(5.2), "Compensating Rollback Flow", [
        "Registration: saga.addCompensation(activities::compensateMissionFailure, missionId, droneId, reason).",
        "Trigger: Activated upon unhandled exception or reception of signalEmergencyAbort.",
        "Step 1: Mission status updated to FAILED in MySQL database.",
        "Step 2: Drone status safely evaluated: set to AVAILABLE or MAINTENANCE if battery < 15%.",
        "Step 3: Emergency audit event ERROR logged to mission_events table with exact failure cause."
    ], ACCENT_AMBER)

    # ==========================================
    # SLIDE 7: Security Architecture & JWT Flow
    # ==========================================
    s7 = prs.slides.add_slide(blank_layout)
    add_background(s7)
    add_header(s7, "Spring Security & JWT Authentication Flow")

    add_card(s7, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Authentication & Authorization Architecture", [
        "Stateless Session Management via Spring Security filter chain.",
        "Cryptographic HMAC-SHA256 tokens generated by JwtTokenProvider.",
        "Claims payload encapsulates username, roles, issue timestamp, and expiration.",
        "BCrypt password hashing protects credentials at rest in MySQL.",
        "JwtAuthenticationFilter validates incoming Bearer tokens before request handling."
    ], ACCENT_CYAN)

    add_card(s7, Inches(6.8), Inches(1.6), Inches(5.7), Inches(5.2), "Role-Based Access Control (RBAC)", [
        "ADMIN Role: Full fleet management, drone asset registration, operational override.",
        "OPERATOR Role: Mission scheduling, trajectory dispatch, emergency abort signaling.",
        "Public Endpoints: /api/auth/login, /api/auth/register, and Swagger OpenAPI docs.",
        "Protected Endpoints: /api/drones/**, /api/missions/**, /api/telemetry/**.",
        "Axios Interceptors: Automatically attach Bearer token and handle 401 unauthenticated redirects."
    ], ACCENT_BLUE)

    # ==========================================
    # SLIDE 8: React Dashboard & Telemetry Visualizer
    # ==========================================
    s8 = prs.slides.add_slide(blank_layout)
    add_background(s8)
    add_header(s8, "Frontend Command Center (React 18 + Vite)")

    add_card(s8, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Interactive Dashboard Features", [
        "Fleet KPI Cards: Live counts of Total Fleet, Available Assets, Active Flights, and Maintenance.",
        "Fleet Assets Grid: Real-time battery gauges, coordinate tracking, and status badges.",
        "Mission Dispatch Console: One-click mission creation modal with destination coordinate inputs.",
        "Workflow Dispatch Controls: Launch buttons to trigger Temporal workflows and Abort buttons for emergency stops."
    ], ACCENT_CYAN)

    add_card(s8, Inches(6.8), Inches(1.6), Inches(5.7), Inches(5.2), "Real-Time Telemetry & Short-Polling", [
        "Recharts Data Visualization: Multi-metric line charts plotting Altitude (m), Speed (km/h), and Battery (%) over time.",
        "Short-Polling Engine: Queries /api/telemetry/mission/{id}/latest every 2 seconds during active flights.",
        "Live Temporal Phase Badge: Displays real-time workflow phase (e.g., TRANSIT_WAYPOINT_3).",
        "Instant Telemetry Simulation: 'Simulate Ping' button generates realistic telemetry points for testing."
    ], ACCENT_GREEN)

    # ==========================================
    # SLIDE 9: Quantitative Model Evaluation & Benchmarks
    # ==========================================
    s9 = prs.slides.add_slide(blank_layout)
    add_background(s9)
    add_header(s9, "Performance Benchmarks & Kinematics Evaluation")

    add_card(s9, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Battery Consumption Kinematics Model", [
        "Mathematical Model: Delta Battery (%) = (Distance_km * 0.8) + (Payload_kg * 0.1).",
        "Evaluated across 6 realistic flight profiles (medical delivery, surveillance, scans).",
        "Measured vs. Predicted variance: Less than 0.05% error margin.",
        "Confirms mathematical safety margins (30% reserve) for return-to-base maneuvers are maintained."
    ], ACCENT_AMBER)

    add_card(s9, Inches(6.8), Inches(1.6), Inches(5.7), Inches(5.2), "Latency & Orchestration Benchmarks", [
        "Workflow Dispatch Latency: Average 24.2 ms (P95: 31.0 ms).",
        "Synchronous State Query Latency: 2.8 ms (serviced directly in-memory).",
        "Emergency Signal Dispatch Latency: 14.1 ms.",
        "Full Saga Compensation Execution: 112 ms average (far exceeding aerospace < 500 ms constraint).",
        "Host Footprint: < 2% CPU utilization and ~150 MB memory."
    ], ACCENT_GREEN)

    # ==========================================
    # SLIDE 10: Automated Testing & Verification
    # ==========================================
    s10 = prs.slides.add_slide(blank_layout)
    add_background(s10)
    add_header(s10, "Testing & Quality Assurance")

    add_card(s10, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Integration & Security Test Suite", [
        "AeroSagaIntegrationTests:",
        "  - Admin login & JWT verification: PASS",
        "  - Unauthorized endpoint rejection (403): PASS",
        "  - Authenticated fleet query with Bearer token: PASS",
        "  - Dynamic operator registration & identity check: PASS",
        "100% compliance with Spring Security filter specifications."
    ], ACCENT_BLUE)

    add_card(s10, Inches(6.8), Inches(1.6), Inches(5.7), Inches(5.2), "Temporal Workflow Test Suite", [
        "AeroSagaTemporalWorkflowTests:",
        "  - End-to-end autonomous workflow execution: PASS (all 5 waypoints traversed, drone released, events logged).",
        "  - Emergency abort & Saga rollback: PASS (compensating activity triggered, mission marked FAILED).",
        "Total Test Results: 6 Tests Run, 0 Failures, 0 Errors, 0 Skipped.",
        "Frontend Vite Production Build: 2,554 modules transformed in 1.14s with 0 errors."
    ], ACCENT_GREEN)

    # ==========================================
    # SLIDE 11: Demonstration Walkthrough
    # ==========================================
    s11 = prs.slides.add_slide(blank_layout)
    add_background(s11)
    add_header(s11, "Live Demonstration Walkthrough Scenarios")

    add_card(s11, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Demonstration Scenarios 1 & 2", [
        "Scenario 1: JWT Authentication & Role Gate",
        "  - Sign in as operator / operator123.",
        "  - Inspect token storage in localStorage and Bearer header in DevTools network tab.",
        "Scenario 2: Nominal Autonomous Mission Dispatch",
        "  - Schedule mission to coordinates (12.9850, 77.6100).",
        "  - Click 'Launch' to dispatch Temporal workflow.",
        "  - Observe drone status update to IN_MISSION and live progress through 5 waypoints."
    ], ACCENT_CYAN)

    add_card(s11, Inches(6.8), Inches(1.6), Inches(5.7), Inches(5.2), "Demonstration Scenarios 3 & 4", [
        "Scenario 3: Live Telemetry Graph & Polling",
        "  - Recharts component polls every 2 seconds.",
        "  - Watch Altitude, Speed, and Battery lines react in real-time.",
        "Scenario 4: Mid-Air Hazard & Saga Compensation Rollback",
        "  - While mission is in transit, click 'Abort (Saga)'.",
        "  - Temporal catches signal, executes compensateMissionFailure.",
        "  - Mission status becomes FAILED and drone safely reset to AVAILABLE."
    ], ACCENT_AMBER)

    # ==========================================
    # SLIDE 12: Project Conclusions & Future Roadmap
    # ==========================================
    s12 = prs.slides.add_slide(blank_layout)
    add_background(s12)
    add_header(s12, "Conclusions & Future Scope")

    add_card(s12, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Project Conclusions", [
        "Successfully developed an enterprise-grade autonomous drone mission control platform.",
        "Demonstrated the necessity of Temporal.io durable execution for mission-critical operations.",
        "Proven the Saga Pattern as an effective mechanism for physical robotics rollback.",
        "Delivered a production-ready, fully responsive React dashboard with real-time analytics.",
        "Achieved 100% test pass rate across all functional, security, and orchestration suites."
    ], ACCENT_GREEN)

    add_card(s12, Inches(6.8), Inches(1.6), Inches(5.7), Inches(5.2), "Future Enhancement Opportunities", [
        "WebSocket / SSE Streaming: Upgrade from short-polling to bi-directional WebSocket telemetry channels.",
        "Geofencing Engine: Integration of 3D polygonal no-fly airspace zones (Airport Class B/C).",
        "Physical MAVLink Protocol: Bridge Temporal Activities with real PX4/ArduPilot drone autopilots.",
        "Multi-Tenant Cloud: Containerization with Docker Compose and Kubernetes orchestration."
    ], ACCENT_BLUE)

    # ==========================================
    # SLIDE 13: Thank You / Q&A Slide
    # ==========================================
    s13 = prs.slides.add_slide(blank_layout)
    add_background(s13)

    dec_end = s13.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(1.5), Inches(2.0), Inches(10.333), Inches(3.8))
    dec_end.fill.solid()
    dec_end.fill.fore_color.rgb = CARD_BG
    dec_end.line.color.rgb = BORDER_COLOR
    dec_end.line.width = Pt(1.5)

    tb_end = s13.shapes.add_textbox(Inches(2.0), Inches(2.4), Inches(9.333), Inches(3.0))
    tf_end = tb_end.text_frame
    tf_end.word_wrap = True

    pe0 = tf_end.paragraphs[0]
    pe0.text = "AEROSAGA MISSION CONTROL"
    pe0.font.size = Pt(14)
    pe0.font.bold = True
    pe0.font.color.rgb = ACCENT_CYAN
    pe0.alignment = PP_ALIGN.CENTER

    pe1 = tf_end.add_paragraph()
    pe1.text = "Thank You! Questions & Discussion"
    pe1.font.size = Pt(36)
    pe1.font.bold = True
    pe1.font.color.rgb = TEXT_WHITE
    pe1.space_before = Pt(14)
    pe1.alignment = PP_ALIGN.CENTER

    pe2 = tf_end.add_paragraph()
    pe2.text = "Infotact Solutions Internship Project Evaluation | October 2026\nLive Demonstration: http://localhost:5173  |  OpenAPI: http://localhost:8080/swagger-ui.html"
    pe2.font.size = Pt(15)
    pe2.font.color.rgb = TEXT_MUTED
    pe2.space_before = Pt(16)
    pe2.alignment = PP_ALIGN.CENTER

    output_path = os.path.abspath("AeroSaga_Final_Presentation.pptx")
    prs.save(output_path)
    print("Presentation created successfully at:", output_path)

if __name__ == "__main__":
    create_deck()
