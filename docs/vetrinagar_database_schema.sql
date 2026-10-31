-- =====================================================================
-- SMART PUBLIC TRANSPORTATION ROUTE OPTIMIZATION - VETRINAGAR TRANSIT
-- Comprehensive Relational Database Schema & Baseline Data Corpus
-- Compatible with: SQLite 3, MySQL 8.0, and MariaDB
-- =====================================================================

-- 1. USERS TABLE (Role-Based Authentication: Supervisor & Passenger)
CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('SUPERVISOR', 'PASSENGER')),
    full_name VARCHAR(100) NOT NULL
);

INSERT INTO users (username, password, role, full_name) VALUES
('supervisor', 'admin123', 'SUPERVISOR', 'Chief Transit Supervisor'),
('passenger1', 'pass123', 'PASSENGER', 'Kavitha Raman'),
('passenger2', 'pass123', 'PASSENGER', 'Senthil Kumar');

-- 2. STOPS TABLE (12 Passenger Stops + 1 Central Depot)
CREATE TABLE IF NOT EXISTS stops (
    stop_id VARCHAR(10) PRIMARY KEY,
    stop_name VARCHAR(100) NOT NULL,
    stop_type VARCHAR(20) NOT NULL DEFAULT 'STOP',
    location_desc VARCHAR(255),
    map_x INTEGER NOT NULL DEFAULT 100,
    map_y INTEGER NOT NULL DEFAULT 100
);

INSERT INTO stops (stop_id, stop_name, stop_type, location_desc, map_x, map_y) VALUES
('D00', 'Central Depot', 'DEPOT', 'Vetrinagar Municipal Bus Headquarters & Maintenance Yard', 450, 240),
('S01', 'Market Junction', 'STOP', 'Commercial District & Central Bazaar', 320, 160),
('S02', 'Tech Park', 'STOP', 'IT Corridor & Technology Center', 660, 140),
('S03', 'Railway Station', 'STOP', 'Main Junction Railway Terminal', 200, 280),
('S04', 'Gandhinagar', 'STOP', 'Dense Residential Quarter Phase 1', 340, 360),
('S05', 'College Road', 'STOP', 'Arts & Engineering Campus Zone', 180, 440),
('S06', 'Hospital Circle', 'STOP', 'District General Hospital & Medical College', 340, 480),
('S07', 'Anna Nagar', 'STOP', 'Eastern Township & Shopping Complex', 560, 280),
('S08', 'Industrial Estate', 'STOP', 'Manufacturing & SME Logistics Hub', 740, 290),
('S09', 'Lake View', 'STOP', 'Recreational Promenade & Outer Ring', 220, 560),
('S10', 'Temple Gate', 'STOP', 'Historic Heritage Quarter & Pilgrim Terminus', 500, 480),
('S11', 'North Bypass', 'STOP', 'Northern Highway Interchange & Freight Junction', 520, 80),
('S12', 'South Terminal', 'STOP', 'Southern Peripheral Outpost (Isolated Outpost)', 700, 560);

-- 3. BUSES TABLE (Relief Fleet with Fixed Seat Capacities)
CREATE TABLE IF NOT EXISTS buses (
    bus_id VARCHAR(10) PRIMARY KEY,
    bus_type VARCHAR(50) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    current_location VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    passengers_onboard INTEGER NOT NULL DEFAULT 0,
    current_trip_id VARCHAR(20),
    FOREIGN KEY (current_location) REFERENCES stops (stop_id)
);

INSERT INTO buses (bus_id, bus_type, capacity, current_location, status, passengers_onboard) VALUES
('B01', 'MiniBus', 20, 'D00', 'AVAILABLE', 0),
('B02', 'StandardBus', 40, 'D00', 'AVAILABLE', 0),
('B03', 'StandardBus', 50, 'D00', 'AVAILABLE', 0),
('B04', 'MiniBus', 25, 'D00', 'BROKEN', 0);

-- 4. ROADS TABLE (Weighted Graph Edges with Positive Travel Times)
CREATE TABLE IF NOT EXISTS roads (
    road_id INTEGER PRIMARY KEY,
    from_stop_id VARCHAR(10) NOT NULL,
    to_stop_id VARCHAR(10) NOT NULL,
    travel_time INTEGER NOT NULL CHECK (travel_time > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    road_type VARCHAR(20) NOT NULL DEFAULT 'TWO_WAY',
    weight_limit VARCHAR(50) NOT NULL DEFAULT 'All',
    FOREIGN KEY (from_stop_id) REFERENCES stops (stop_id),
    FOREIGN KEY (to_stop_id) REFERENCES stops (stop_id)
);

INSERT INTO roads (road_id, from_stop_id, to_stop_id, travel_time, status, road_type, weight_limit) VALUES
(1, 'D00', 'S01', 8, 'OPEN', 'TWO_WAY', 'All'),
(2, 'D00', 'S03', 10, 'OPEN', 'TWO_WAY', 'All'),
(3, 'D00', 'S04', 12, 'OPEN', 'TWO_WAY', 'All'),
(4, 'D00', 'S07', 9, 'OPEN', 'TWO_WAY', 'All'),
(5, 'S01', 'S02', 35, 'OPEN', 'TWO_WAY', 'All'),
(6, 'S01', 'S03', 7, 'OPEN', 'TWO_WAY', 'All'),
(7, 'S01', 'S11', 11, 'OPEN', 'TWO_WAY', 'All'),
(8, 'S11', 'S02', 14, 'OPEN', 'TWO_WAY', 'All'),
(9, 'S02', 'S07', 12, 'OPEN', 'TWO_WAY', 'All'),
(10, 'S02', 'S08', 16, 'OPEN', 'TWO_WAY', 'All'),
(11, 'S03', 'S06', 15, 'CLOSED', 'TWO_WAY', 'All'),
(12, 'S03', 'S04', 9, 'OPEN', 'TWO_WAY', 'All'),
(13, 'S03', 'S05', 13, 'OPEN', 'ONE_WAY', 'All'),
(14, 'S04', 'S05', 11, 'OPEN', 'TWO_WAY', 'All'),
(15, 'S04', 'S07', 15, 'OPEN', 'TWO_WAY', 'All'),
(16, 'S04', 'S10', 13, 'OPEN', 'TWO_WAY', 'All'),
(17, 'S05', 'S06', 10, 'OPEN', 'TWO_WAY', 'All'),
(18, 'S05', 'S09', 15, 'OPEN', 'TWO_WAY', 'All'),
(19, 'S06', 'S07', 14, 'OPEN', 'TWO_WAY', 'All'),
(20, 'S06', 'S10', 8, 'OPEN', 'TWO_WAY', 'MiniBus Only'),
(21, 'S07', 'S08', 10, 'OPEN', 'TWO_WAY', 'All'),
(22, 'S07', 'S10', 16, 'OPEN', 'TWO_WAY', 'All'),
(23, 'S08', 'S11', 20, 'OPEN', 'TWO_WAY', 'All'),
(24, 'S09', 'S10', 12, 'OPEN', 'TWO_WAY', 'All'),
(25, 'S10', 'S12', 24, 'CLOSED', 'TWO_WAY', 'All');

-- 5. DEMAND_BATCHES TABLE (PriorityQueue Corridor Aggregation)
CREATE TABLE IF NOT EXISTS demand_batches (
    batch_id VARCHAR(20) PRIMARY KEY,
    pickup_stop_id VARCHAR(10) NOT NULL,
    destination_stop_id VARCHAR(10) NOT NULL,
    passenger_count INTEGER NOT NULL CHECK (passenger_count >= 0),
    original_count INTEGER NOT NULL CHECK (original_count > 0),
    arrival_timestamp BIGINT NOT NULL,
    arrival_time_formatted VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    FOREIGN KEY (pickup_stop_id) REFERENCES stops (stop_id),
    FOREIGN KEY (destination_stop_id) REFERENCES stops (stop_id)
);

-- 6. DEMAND_REQUESTS TABLE (Individual Passenger Requests via ArrayDeque)
CREATE TABLE IF NOT EXISTS demand_requests (
    request_id VARCHAR(20) PRIMARY KEY,
    passenger_id VARCHAR(50) NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    pickup_stop_id VARCHAR(10) NOT NULL,
    destination_stop_id VARCHAR(10) NOT NULL,
    passenger_count INTEGER NOT NULL CHECK (passenger_count > 0),
    original_count INTEGER NOT NULL CHECK (original_count > 0),
    submitted_at VARCHAR(30) NOT NULL,
    assigned_batch_id VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    FOREIGN KEY (pickup_stop_id) REFERENCES stops (stop_id),
    FOREIGN KEY (destination_stop_id) REFERENCES stops (stop_id),
    FOREIGN KEY (assigned_batch_id) REFERENCES demand_batches (batch_id)
);

-- 7. RELIEF_TRIPS TABLE (2-Segment Shortest Path Execution)
CREATE TABLE IF NOT EXISTS relief_trips (
    trip_id VARCHAR(20) PRIMARY KEY,
    batch_id VARCHAR(20) NOT NULL,
    bus_id VARCHAR(10) NOT NULL,
    pickup_stop_id VARCHAR(10) NOT NULL,
    destination_stop_id VARCHAR(10) NOT NULL,
    passengers_allocated INTEGER NOT NULL CHECK (passengers_allocated > 0),
    bus_capacity INTEGER NOT NULL,
    segment1_path TEXT,
    segment1_time INTEGER NOT NULL,
    segment2_path TEXT,
    segment2_time INTEGER NOT NULL,
    total_travel_time INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at VARCHAR(30) NOT NULL,
    completed_at VARCHAR(30),
    FOREIGN KEY (batch_id) REFERENCES demand_batches (batch_id),
    FOREIGN KEY (bus_id) REFERENCES buses (bus_id),
    FOREIGN KEY (pickup_stop_id) REFERENCES stops (stop_id),
    FOREIGN KEY (destination_stop_id) REFERENCES stops (stop_id)
);

-- 8. TRAFFIC_LOGS TABLE (Dynamic Travel-Time Changes Auditing)
CREATE TABLE IF NOT EXISTS traffic_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    road_id INTEGER NOT NULL,
    old_time INTEGER NOT NULL,
    new_time INTEGER NOT NULL,
    reason VARCHAR(255),
    logged_at VARCHAR(30) NOT NULL,
    FOREIGN KEY (road_id) REFERENCES roads (road_id)
);
