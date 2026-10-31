-- ==============================================================
-- VETRINAGAR SMART PUBLIC TRANSIT ROUTION OPTIMIZATION
-- Database Schema & Seed Data for phpMyAdmin (MySQL / MariaDB)
-- ==============================================================

CREATE DATABASE IF NOT EXISTS `vetrinagar_transit` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `vetrinagar_transit`;

-- 1. Users Table
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `username` VARCHAR(50) NOT NULL PRIMARY KEY,
    `password` VARCHAR(100) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `users` (`username`, `password`, `role`, `full_name`) VALUES
('supervisor', 'admin123', 'SUPERVISOR', 'Chief Transit Supervisor'),
('passenger1', 'pass123', 'PASSENGER', 'Kavitha Raman'),
('passenger2', 'pass123', 'PASSENGER', 'Senthil Kumar');

-- 2. Stops Table
DROP TABLE IF EXISTS `stops`;
CREATE TABLE `stops` (
    `stop_id` VARCHAR(10) NOT NULL PRIMARY KEY,
    `stop_name` VARCHAR(100) NOT NULL,
    `stop_type` VARCHAR(20) NOT NULL,
    `location_desc` VARCHAR(255) DEFAULT NULL,
    `map_x` INT NOT NULL,
    `map_y` INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `stops` (`stop_id`, `stop_name`, `stop_type`, `location_desc`, `map_x`, `map_y`) VALUES
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

-- 3. Road Network Table
DROP TABLE IF EXISTS `roads`;
CREATE TABLE `roads` (
    `road_id` INT NOT NULL PRIMARY KEY,
    `from_stop_id` VARCHAR(10) NOT NULL,
    `to_stop_id` VARCHAR(10) NOT NULL,
    `travel_time` INT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    `road_type` VARCHAR(20) NOT NULL DEFAULT 'TWO_WAY',
    `weight_limit` VARCHAR(50) DEFAULT 'All',
    KEY `idx_from_stop` (`from_stop_id`),
    KEY `idx_to_stop` (`to_stop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `roads` (`road_id`, `from_stop_id`, `to_stop_id`, `travel_time`, `status`, `road_type`, `weight_limit`) VALUES
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

-- 4. Bus Fleet Table
DROP TABLE IF EXISTS `buses`;
CREATE TABLE `buses` (
    `bus_id` VARCHAR(10) NOT NULL PRIMARY KEY,
    `bus_type` VARCHAR(50) NOT NULL,
    `capacity` INT NOT NULL,
    `current_location` VARCHAR(10) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    `passengers_onboard` INT NOT NULL DEFAULT 0,
    `current_trip_id` VARCHAR(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `buses` (`bus_id`, `bus_type`, `capacity`, `current_location`, `status`, `passengers_onboard`, `current_trip_id`) VALUES
('B01', 'MiniBus', 30, 'D00', 'AVAILABLE', 0, NULL),
('B02', 'StandardBus', 40, 'D00', 'AVAILABLE', 0, NULL),
('B03', 'StandardBus', 50, 'D00', 'AVAILABLE', 0, NULL),
('B04', 'MiniBus', 25, 'D00', 'BROKEN', 0, NULL);

-- 5. Demand Batches Table (PriorityQueue representation)
DROP TABLE IF EXISTS `demand_batches`;
CREATE TABLE `demand_batches` (
    `batch_id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `pickup_stop_id` VARCHAR(10) NOT NULL,
    `destination_stop_id` VARCHAR(10) NOT NULL,
    `passenger_count` INT NOT NULL,
    `original_count` INT NOT NULL,
    `arrival_timestamp` BIGINT NOT NULL,
    `arrival_time_formatted` VARCHAR(20) NOT NULL,
    `remarks` VARCHAR(255) DEFAULT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'WAITING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `demand_batches` (`batch_id`, `pickup_stop_id`, `destination_stop_id`, `passenger_count`, `original_count`, `arrival_timestamp`, `arrival_time_formatted`, `remarks`, `status`) VALUES
('DEM-1001', 'S01', 'S02', 65, 65, 1700000001000, '08:15:00', 'IT shift morning peak surge', 'WAITING'),
('DEM-1002', 'S03', 'S02', 45, 45, 1700000002000, '08:20:00', 'Intercity train arrived at junction', 'WAITING'),
('DEM-1003', 'S05', 'S02', 40, 40, 1700000003000, '08:22:00', 'Student & staff tech cluster', 'WAITING'),
('DEM-1004', 'S04', 'S01', 35, 35, 1700000004000, '08:25:00', 'Market shoppers batch', 'WAITING'),
('DEM-1005', 'S01', 'S07', 30, 30, 1700000005000, '08:28:00', 'Residential commute', 'WAITING'),
('DEM-1006', 'S03', 'S06', 25, 25, 1700000006000, '08:30:00', 'Hospital shift visitors', 'WAITING'),
('DEM-1007', 'S02', 'S08', 20, 20, 1700000007000, '08:32:00', 'Industrial delivery crews', 'WAITING'),
('DEM-1008', 'S06', 'S01', 18, 18, 1700000008000, '08:35:00', 'Morning clinic returnees', 'WAITING'),
('DEM-1009', 'S07', 'S02', 15, 15, 1700000009000, '08:37:00', 'Tech park contractors', 'WAITING'),
('DEM-1010', 'S05', 'S03', 12, 12, 1700000010000, '08:40:00', 'College rail commuters', 'WAITING');

-- 6. Demand Requests Table (Individual Commuters)
DROP TABLE IF EXISTS `demand_requests`;
CREATE TABLE `demand_requests` (
    `request_id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `passenger_id` VARCHAR(50) NOT NULL,
    `passenger_name` VARCHAR(100) NOT NULL,
    `pickup_stop_id` VARCHAR(10) NOT NULL,
    `destination_stop_id` VARCHAR(10) NOT NULL,
    `passenger_count` INT NOT NULL,
    `created_at` VARCHAR(50) NOT NULL,
    `assigned_batch_id` VARCHAR(50) DEFAULT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'WAITING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `demand_requests` (`request_id`, `passenger_id`, `passenger_name`, `pickup_stop_id`, `destination_stop_id`, `passenger_count`, `created_at`, `assigned_batch_id`, `status`) VALUES
('REQ-501', 'PASS-101', 'Kavitha Raman', 'S01', 'S02', 5, '08:14:30', 'DEM-1001', 'WAITING'),
('REQ-502', 'PASS-102', 'Ravi Shankar', 'S01', 'S02', 20, '08:15:00', 'DEM-1001', 'WAITING');

-- 7. Relief Trips Table
DROP TABLE IF EXISTS `relief_trips`;
CREATE TABLE `relief_trips` (
    `trip_id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `batch_id` VARCHAR(50) NOT NULL,
    `bus_id` VARCHAR(10) NOT NULL,
    `pickup_stop_id` VARCHAR(10) NOT NULL,
    `destination_stop_id` VARCHAR(10) NOT NULL,
    `passengers_allocated` INT NOT NULL,
    `bus_capacity` INT NOT NULL,
    `segment1_path` TEXT,
    `segment1_time` INT NOT NULL,
    `segment2_path` TEXT,
    `segment2_time` INT NOT NULL,
    `total_travel_time` INT NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    `created_at` VARCHAR(50) NOT NULL,
    `completed_at` VARCHAR(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Traffic Logs Table
DROP TABLE IF EXISTS `traffic_logs`;
CREATE TABLE `traffic_logs` (
    `log_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `road_id` INT NOT NULL,
    `old_time` INT NOT NULL,
    `new_time` INT NOT NULL,
    `reason` VARCHAR(255) DEFAULT NULL,
    `logged_at` VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `traffic_logs` (`road_id`, `old_time`, `new_time`, `reason`, `logged_at`) VALUES
(5, 20, 35, 'Peak hour IT corridor bottle-neck congestion', NOW());
