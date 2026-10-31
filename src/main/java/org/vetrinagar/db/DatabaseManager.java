package org.vetrinagar.db;

import org.vetrinagar.model.*;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Manages SQLite database persistence for Vetrinagar Public Transit.
 * Ensures all stops, roads, buses, passenger requests, relief trips, and traffic changes
 * are reliably stored and loaded.
 */
public class DatabaseManager {
    private final String dbPath;
    private final String jdbcUrl;

    public DatabaseManager(String dbPath) {
        this.dbPath = dbPath != null ? dbPath : "vetrinagar_transit.db";
        this.jdbcUrl = "jdbc:sqlite:" + this.dbPath;
        initDatabase();
    }

    public DatabaseManager() {
        this("vetrinagar_transit.db");
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    /**
     * Initializes schema and seeds baseline dataset if database is newly created.
     */
    public synchronized void initDatabase() {
        try {
            // Ensure SQLite driver is loaded
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB WARNING] SQLite JDBC driver not on classpath. Running in in-memory simulation mode if driver missing: " + e.getMessage());
        }

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Users Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    username TEXT PRIMARY KEY,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL,
                    full_name TEXT NOT NULL
                );
            """);

            // 2. Stops Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stops (
                    stop_id TEXT PRIMARY KEY,
                    stop_name TEXT NOT NULL,
                    stop_type TEXT NOT NULL DEFAULT 'STOP',
                    location_desc TEXT,
                    map_x INTEGER NOT NULL DEFAULT 100,
                    map_y INTEGER NOT NULL DEFAULT 100
                );
            """);

            // 3. Roads Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS roads (
                    road_id INTEGER PRIMARY KEY,
                    from_stop_id TEXT NOT NULL,
                    to_stop_id TEXT NOT NULL,
                    travel_time INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'OPEN',
                    road_type TEXT NOT NULL DEFAULT 'TWO_WAY',
                    weight_limit TEXT NOT NULL DEFAULT 'All'
                );
            """);

            // 4. Buses Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS buses (
                    bus_id TEXT PRIMARY KEY,
                    bus_type TEXT NOT NULL,
                    capacity INTEGER NOT NULL,
                    current_location TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'AVAILABLE',
                    passengers_onboard INTEGER NOT NULL DEFAULT 0,
                    current_trip_id TEXT
                );
            """);

            // 5. Demand Batches Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS demand_batches (
                    batch_id TEXT PRIMARY KEY,
                    pickup_stop_id TEXT NOT NULL,
                    destination_stop_id TEXT NOT NULL,
                    passenger_count INTEGER NOT NULL,
                    original_count INTEGER NOT NULL,
                    arrival_timestamp INTEGER NOT NULL,
                    arrival_time_formatted TEXT NOT NULL,
                    remarks TEXT,
                    status TEXT NOT NULL DEFAULT 'WAITING'
                );
            """);

            // 6. Individual Demand Requests Table (FIFO Queue elements)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS demand_requests (
                    request_id TEXT PRIMARY KEY,
                    passenger_id TEXT NOT NULL,
                    passenger_name TEXT NOT NULL,
                    pickup_stop_id TEXT NOT NULL,
                    destination_stop_id TEXT NOT NULL,
                    passenger_count INTEGER NOT NULL,
                    original_count INTEGER NOT NULL,
                    submitted_at TEXT NOT NULL,
                    assigned_batch_id TEXT,
                    status TEXT NOT NULL DEFAULT 'WAITING'
                );
            """);

            // 7. Relief Trips Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS relief_trips (
                    trip_id TEXT PRIMARY KEY,
                    batch_id TEXT NOT NULL,
                    bus_id TEXT NOT NULL,
                    pickup_stop_id TEXT NOT NULL,
                    destination_stop_id TEXT NOT NULL,
                    passengers_allocated INTEGER NOT NULL,
                    bus_capacity INTEGER NOT NULL,
                    segment1_path TEXT,
                    segment1_time INTEGER NOT NULL,
                    segment2_path TEXT,
                    segment2_time INTEGER NOT NULL,
                    total_travel_time INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PLANNED',
                    created_at TEXT NOT NULL,
                    completed_at TEXT
                );
            """);

            // 8. Traffic Logs Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS traffic_logs (
                    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    road_id INTEGER NOT NULL,
                    old_time INTEGER NOT NULL,
                    new_time INTEGER NOT NULL,
                    reason TEXT,
                    logged_at TEXT NOT NULL
                );
            """);

            // Seed initial data if stops table is empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM stops");
            if (rs.next() && rs.getInt(1) == 0) {
                seedInitialData(conn);
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] initDatabase failed: " + e.getMessage());
        }
    }

    private void seedInitialData(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        try {
            // A. Seed Users
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, "supervisor");
                ps.setString(2, "admin123");
                ps.setString(3, "SUPERVISOR");
                ps.setString(4, "Chief Transit Supervisor");
                ps.addBatch();

                ps.setString(1, "passenger1");
                ps.setString(2, "pass123");
                ps.setString(3, "PASSENGER");
                ps.setString(4, "Kavitha Raman");
                ps.addBatch();

                ps.setString(1, "passenger2");
                ps.setString(2, "pass123");
                ps.setString(3, "PASSENGER");
                ps.setString(4, "Senthil Kumar");
                ps.addBatch();

                ps.executeBatch();
            }

            // B. Seed 13 Stops (1 Central Depot D00 + 12 Stops)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO stops (stop_id, stop_name, stop_type, location_desc, map_x, map_y) VALUES (?, ?, ?, ?, ?, ?)")) {
                Object[][] stopsData = {
                        {"D00", "Central Depot", "DEPOT", "Vetrinagar Municipal Bus Headquarters & Maintenance Yard", 450, 240},
                        {"S01", "Market Junction", "STOP", "Commercial District & Central Bazaar", 320, 160},
                        {"S02", "Tech Park", "STOP", "IT Corridor & Technology Center", 660, 140},
                        {"S03", "Railway Station", "STOP", "Main Junction Railway Terminal", 200, 280},
                        {"S04", "Gandhinagar", "STOP", "Dense Residential Quarter Phase 1", 340, 360},
                        {"S05", "College Road", "STOP", "Arts & Engineering Campus Zone", 180, 440},
                        {"S06", "Hospital Circle", "STOP", "District General Hospital & Medical College", 340, 480},
                        {"S07", "Anna Nagar", "STOP", "Eastern Township & Shopping Complex", 560, 280},
                        {"S08", "Industrial Estate", "STOP", "Manufacturing & SME Logistics Hub", 740, 290},
                        {"S09", "Lake View", "STOP", "Recreational Promenade & Outer Ring", 220, 560},
                        {"S10", "Temple Gate", "STOP", "Historic Heritage Quarter & Pilgrim Terminus", 500, 480},
                        {"S11", "North Bypass", "STOP", "Northern Highway Interchange & Freight Junction", 520, 80},
                        {"S12", "South Terminal", "STOP", "Southern Peripheral Outpost (Isolated Outpost)", 700, 560}
                };

                for (Object[] row : stopsData) {
                    ps.setString(1, (String) row[0]);
                    ps.setString(2, (String) row[1]);
                    ps.setString(3, (String) row[2]);
                    ps.setString(4, (String) row[3]);
                    ps.setInt(5, (Integer) row[4]);
                    ps.setInt(6, (Integer) row[5]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // C. Seed Buses (B01, B02, B03, B04)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO buses (bus_id, bus_type, capacity, current_location, status, passengers_onboard) VALUES (?, ?, ?, ?, ?, ?)")) {
                Object[][] busesData = {
                        {"B01", "MiniBus", 20, "D00", "AVAILABLE", 0},
                        {"B02", "StandardBus", 40, "D00", "AVAILABLE", 0},
                        {"B03", "StandardBus", 50, "D00", "AVAILABLE", 0},
                        {"B04", "MiniBus", 25, "D00", "BROKEN", 0} // Pre-set for breakdown test
                };

                for (Object[] row : busesData) {
                    ps.setString(1, (String) row[0]);
                    ps.setString(2, (String) row[1]);
                    ps.setInt(3, (Integer) row[2]);
                    ps.setString(4, (String) row[3]);
                    ps.setString(5, (String) row[4]);
                    ps.setInt(6, (Integer) row[5]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // D. Seed Roads
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO roads (road_id, from_stop_id, to_stop_id, travel_time, status, road_type, weight_limit) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                Object[][] roadsData = {
                        {1, "D00", "S01", 8, "OPEN", "TWO_WAY", "All"},
                        {2, "D00", "S03", 10, "OPEN", "TWO_WAY", "All"},
                        {3, "D00", "S04", 12, "OPEN", "TWO_WAY", "All"},
                        {4, "D00", "S07", 9, "OPEN", "TWO_WAY", "All"},
                        {5, "S01", "S02", 35, "OPEN", "TWO_WAY", "All"}, // Traffic-heavy corridor
                        {6, "S01", "S03", 7, "OPEN", "TWO_WAY", "All"},
                        {7, "S01", "S11", 11, "OPEN", "TWO_WAY", "All"},
                        {8, "S11", "S02", 14, "OPEN", "TWO_WAY", "All"},
                        {9, "S02", "S07", 12, "OPEN", "TWO_WAY", "All"},
                        {10, "S02", "S08", 16, "OPEN", "TWO_WAY", "All"},
                        {11, "S03", "S06", 15, "CLOSED", "TWO_WAY", "All"}, // Pipeline repair closure
                        {12, "S03", "S04", 9, "OPEN", "TWO_WAY", "All"},
                        {13, "S03", "S05", 13, "OPEN", "ONE_WAY", "All"}, // One-way downhill lane
                        {14, "S04", "S05", 11, "OPEN", "TWO_WAY", "All"},
                        {15, "S04", "S07", 15, "OPEN", "TWO_WAY", "All"},
                        {16, "S04", "S10", 13, "OPEN", "TWO_WAY", "All"},
                        {17, "S05", "S06", 10, "OPEN", "TWO_WAY", "All"},
                        {18, "S05", "S09", 15, "OPEN", "TWO_WAY", "All"},
                        {19, "S06", "S07", 14, "OPEN", "TWO_WAY", "All"},
                        {20, "S06", "S10", 8, "OPEN", "TWO_WAY", "MiniBus Only"}, // Narrow historic bridge
                        {21, "S07", "S08", 10, "OPEN", "TWO_WAY", "All"},
                        {22, "S07", "S10", 16, "OPEN", "TWO_WAY", "All"},
                        {23, "S08", "S11", 20, "OPEN", "TWO_WAY", "All"},
                        {24, "S09", "S10", 12, "OPEN", "TWO_WAY", "All"},
                        {25, "S10", "S12", 24, "CLOSED", "TWO_WAY", "All"} // Disconnected outpost
                };

                for (Object[] row : roadsData) {
                    ps.setInt(1, (Integer) row[0]);
                    ps.setString(2, (String) row[1]);
                    ps.setString(3, (String) row[2]);
                    ps.setInt(4, (Integer) row[3]);
                    ps.setString(5, (String) row[4]);
                    ps.setString(6, (String) row[5]);
                    ps.setString(7, (String) row[6]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // E. Seed 22 Sample Demand Batches (Requirement: at least 20 demand batches)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO demand_batches (batch_id, pickup_stop_id, destination_stop_id, passenger_count, original_count, arrival_timestamp, arrival_time_formatted, remarks, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                Object[][] batchesData = {
                        {"DEM-1001", "S01", "S02", 65, 65, 1700000001000L, "08:15:00", "IT shift morning peak surge", "WAITING"},
                        {"DEM-1002", "S03", "S02", 45, 45, 1700000002000L, "08:20:00", "Intercity train arrived at junction", "WAITING"},
                        {"DEM-1003", "S05", "S02", 40, 40, 1700000003000L, "08:22:00", "Student & staff tech cluster", "WAITING"},
                        {"DEM-1004", "S04", "S01", 35, 35, 1700000004000L, "08:25:00", "Market shoppers batch", "WAITING"},
                        {"DEM-1005", "S01", "S07", 30, 30, 1700000005000L, "08:28:00", "Residential commute", "WAITING"},
                        {"DEM-1006", "S03", "S06", 25, 25, 1700000006000L, "08:30:00", "Hospital shift visitors", "WAITING"},
                        {"DEM-1007", "S02", "S08", 20, 20, 1700000007000L, "08:32:00", "Industrial delivery crews", "WAITING"},
                        {"DEM-1008", "S06", "S01", 18, 18, 1700000008000L, "08:35:00", "Morning clinic returnees", "WAITING"},
                        {"DEM-1009", "S07", "S02", 15, 15, 1700000009000L, "08:37:00", "Tech park contractors", "WAITING"},
                        {"DEM-1010", "S05", "S03", 12, 12, 1700000010000L, "08:40:00", "College rail commuters", "WAITING"},
                        {"DEM-1011", "S01", "S08", 55, 55, 1700000011000L, "08:42:00", "Factory workers morning shift", "WAITING"},
                        {"DEM-1012", "S03", "S05", 40, 40, 1700000002500L, "08:18:00", "University exam batch", "WAITING"},
                        {"DEM-1013", "S04", "S07", 22, 22, 1700000013000L, "08:45:00", "Township residents", "WAITING"},
                        {"DEM-1014", "S08", "S01", 38, 38, 1700000014000L, "08:48:00", "Wholesale market buyers", "WAITING"},
                        {"DEM-1015", "S09", "S02", 14, 14, 1700000015000L, "08:50:00", "Lake view outer ring surge", "WAITING"},
                        {"DEM-1016", "S10", "S03", 28, 28, 1700000016000L, "08:52:00", "Pilgrims heading to railway station", "WAITING"},
                        {"DEM-1017", "S02", "S06", 16, 16, 1700000017000L, "08:55:00", "Medical supply staff", "WAITING"},
                        {"DEM-1018", "S05", "S07", 48, 48, 1700000018000L, "08:58:00", "College group trip", "WAITING"},
                        {"DEM-1019", "S01", "S10", 32, 32, 1700000019000L, "09:00:00", "Heritage bazaar visitors", "WAITING"},
                        {"DEM-1020", "S10", "S12", 20, 20, 1700000020000L, "09:02:00", "South terminal connection (Disconnected link)", "WAITING"},
                        {"DEM-1021", "S03", "S01", 15, 35, 1700000000500L, "08:00:00", "Partial trip serviced earlier", "PARTIALLY_ALLOCATED"},
                        {"DEM-1022", "S02", "S03", 0,  20, 1700000000100L, "07:45:00", "Early morning shuttle completed", "COMPLETED"}
                };

                for (Object[] row : batchesData) {
                    ps.setString(1, (String) row[0]);
                    ps.setString(2, (String) row[1]);
                    ps.setString(3, (String) row[2]);
                    ps.setInt(4, (Integer) row[3]);
                    ps.setInt(5, (Integer) row[4]);
                    ps.setLong(6, (Long) row[5]);
                    ps.setString(7, (String) row[6]);
                    ps.setString(8, (String) row[7]);
                    ps.setString(9, (String) row[8]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            System.out.println("[DB SUCCESS] Database seeded successfully with Vetrinagar transit dataset.");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // --- Loading Methods ---

    public List<Stop> loadAllStops() {
        List<Stop> list = new ArrayList<>();
        String sql = "SELECT stop_id, stop_name, stop_type, location_desc, map_x, map_y FROM stops ORDER BY stop_id";
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Stop(
                        rs.getString("stop_id"),
                        rs.getString("stop_name"),
                        StopType.fromString(rs.getString("stop_type")),
                        rs.getString("location_desc"),
                        rs.getInt("map_x"),
                        rs.getInt("map_y")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] loadAllStops: " + e.getMessage());
        }
        return list;
    }

    public List<Road> loadAllRoads() {
        List<Road> list = new ArrayList<>();
        String sql = "SELECT road_id, from_stop_id, to_stop_id, travel_time, status, road_type, weight_limit FROM roads ORDER BY road_id";
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Road(
                        rs.getInt("road_id"),
                        rs.getString("from_stop_id"),
                        rs.getString("to_stop_id"),
                        rs.getInt("travel_time"),
                        RoadStatus.fromString(rs.getString("status")),
                        RoadType.fromString(rs.getString("road_type")),
                        rs.getString("weight_limit")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] loadAllRoads: " + e.getMessage());
        }
        return list;
    }

    public List<Bus> loadAllBuses() {
        List<Bus> list = new ArrayList<>();
        String sql = "SELECT bus_id, bus_type, capacity, current_location, status, passengers_onboard, current_trip_id FROM buses ORDER BY bus_id";
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("bus_id");
                String type = rs.getString("bus_type");
                int cap = rs.getInt("capacity");
                String loc = rs.getString("current_location");
                BusStatus status = BusStatus.fromString(rs.getString("status"));
                int load = rs.getInt("passengers_onboard");
                String tripId = rs.getString("current_trip_id");

                Bus bus;
                if ("MiniBus".equalsIgnoreCase(type)) {
                    bus = new MiniBus(id, cap, loc, status);
                } else {
                    bus = new StandardBus(id, cap, loc, status);
                }
                if (load > 0) {
                    bus.boardPassengers(load);
                }
                bus.setCurrentTripId(tripId);
                list.add(bus);
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] loadAllBuses: " + e.getMessage());
        }
        return list;
    }

    public List<DemandBatch> loadAllDemandBatches() {
        List<DemandBatch> list = new ArrayList<>();
        String sql = "SELECT batch_id, pickup_stop_id, destination_stop_id, passenger_count, original_count, arrival_timestamp, arrival_time_formatted, remarks, status FROM demand_batches";
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                DemandBatch b = new DemandBatch(
                        rs.getString("batch_id"),
                        rs.getString("pickup_stop_id"),
                        rs.getString("destination_stop_id"),
                        rs.getInt("passenger_count"),
                        rs.getLong("arrival_timestamp"),
                        rs.getString("arrival_time_formatted"),
                        rs.getString("remarks"),
                        DemandStatus.fromString(rs.getString("status"))
                );
                list.add(b);
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] loadAllDemandBatches: " + e.getMessage());
        }
        return list;
    }

    public List<ReliefTrip> loadAllReliefTrips() {
        List<ReliefTrip> list = new ArrayList<>();
        String sql = "SELECT trip_id, batch_id, bus_id, pickup_stop_id, destination_stop_id, passengers_allocated, bus_capacity, segment1_path, segment1_time, segment2_path, segment2_time, status FROM relief_trips ORDER BY created_at DESC";
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String s1 = rs.getString("segment1_path");
                List<String> path1 = s1 != null && !s1.isBlank() ? Arrays.asList(s1.split("->")) : Collections.emptyList();
                String s2 = rs.getString("segment2_path");
                List<String> path2 = s2 != null && !s2.isBlank() ? Arrays.asList(s2.split("->")) : Collections.emptyList();

                list.add(new ReliefTrip(
                        rs.getString("trip_id"),
                        rs.getString("batch_id"),
                        rs.getString("bus_id"),
                        rs.getString("pickup_stop_id"),
                        rs.getString("destination_stop_id"),
                        rs.getInt("passengers_allocated"),
                        rs.getInt("bus_capacity"),
                        path1,
                        rs.getInt("segment1_time"),
                        path2,
                        rs.getInt("segment2_time"),
                        TripStatus.fromString(rs.getString("status"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] loadAllReliefTrips: " + e.getMessage());
        }
        return list;
    }

    public User loadUser(String username) {
        String sql = "SELECT username, password, role, full_name FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("username"),
                            rs.getString("password"),
                            Role.fromString(rs.getString("role")),
                            rs.getString("full_name")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] loadUser: " + e.getMessage());
        }
        return null;
    }

    // --- Save / Update Methods ---

    public void updateRoad(Road road) {
        String sql = "UPDATE roads SET travel_time = ?, status = ? WHERE road_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, road.getTravelTime());
            ps.setString(2, road.getStatus().name());
            ps.setInt(3, road.getRoadId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] updateRoad: " + e.getMessage());
        }
    }

    public void updateBus(Bus bus) {
        String sql = "UPDATE buses SET current_location = ?, status = ?, passengers_onboard = ?, current_trip_id = ? WHERE bus_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bus.getCurrentLocation());
            ps.setString(2, bus.getStatus().name());
            ps.setInt(3, bus.getPassengersOnboard());
            ps.setString(4, bus.getCurrentTripId());
            ps.setString(5, bus.getBusId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] updateBus: " + e.getMessage());
        }
    }

    public void saveDemandBatch(DemandBatch batch) {
        String sql = """
            INSERT INTO demand_batches (batch_id, pickup_stop_id, destination_stop_id, passenger_count, original_count, arrival_timestamp, arrival_time_formatted, remarks, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(batch_id) DO UPDATE SET
                passenger_count = excluded.passenger_count,
                original_count = excluded.original_count,
                status = excluded.status,
                remarks = excluded.remarks;
        """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batch.getBatchId());
            ps.setString(2, batch.getPickupStopId());
            ps.setString(3, batch.getDestinationStopId());
            ps.setInt(4, batch.getPassengerCount());
            ps.setInt(5, batch.getOriginalCount());
            ps.setLong(6, batch.getArrivalTimestamp());
            ps.setString(7, batch.getArrivalTimeFormatted());
            ps.setString(8, batch.getRemarks());
            ps.setString(9, batch.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] saveDemandBatch: " + e.getMessage());
        }
    }

    public void saveDemandRequest(DemandRequest req) {
        String sql = """
            INSERT INTO demand_requests (request_id, passenger_id, passenger_name, pickup_stop_id, destination_stop_id, passenger_count, original_count, submitted_at, assigned_batch_id, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(request_id) DO UPDATE SET
                passenger_count = excluded.passenger_count,
                status = excluded.status,
                assigned_batch_id = excluded.assigned_batch_id;
        """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.getRequestId());
            ps.setString(2, req.getPassengerId());
            ps.setString(3, req.getPassengerName());
            ps.setString(4, req.getPickupStopId());
            ps.setString(5, req.getDestinationStopId());
            ps.setInt(6, req.getPassengerCount());
            ps.setInt(7, req.getOriginalCount());
            ps.setString(8, req.getSubmittedAt().toString());
            ps.setString(9, req.getAssignedBatchId());
            ps.setString(10, req.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] saveDemandRequest: " + e.getMessage());
        }
    }

    public void saveReliefTrip(ReliefTrip trip) {
        String sql = """
            INSERT INTO relief_trips (trip_id, batch_id, bus_id, pickup_stop_id, destination_stop_id, passengers_allocated, bus_capacity, segment1_path, segment1_time, segment2_path, segment2_time, total_travel_time, status, created_at, completed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(trip_id) DO UPDATE SET
                bus_id = excluded.bus_id,
                passengers_allocated = excluded.passengers_allocated,
                status = excluded.status,
                completed_at = excluded.completed_at;
        """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trip.getTripId());
            ps.setString(2, trip.getBatchId());
            ps.setString(3, trip.getBusId());
            ps.setString(4, trip.getPickupStopId());
            ps.setString(5, trip.getDestinationStopId());
            ps.setInt(6, trip.getPassengersAllocated());
            ps.setInt(7, trip.getBusCapacity());
            ps.setString(8, String.join("->", trip.getSegment1Path()));
            ps.setInt(9, trip.getSegment1Time());
            ps.setString(10, String.join("->", trip.getSegment2Path()));
            ps.setInt(11, trip.getSegment2Time());
            ps.setInt(12, trip.getTotalTravelTime());
            ps.setString(13, trip.getStatus().name());
            ps.setString(14, trip.getCreatedAt().toString());
            ps.setString(15, trip.getCompletedAt() != null ? trip.getCompletedAt().toString() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] saveReliefTrip: " + e.getMessage());
        }
    }

    public void logTrafficChange(int roadId, int oldTime, int newTime, String reason) {
        String sql = "INSERT INTO traffic_logs (road_id, old_time, new_time, reason, logged_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roadId);
            ps.setInt(2, oldTime);
            ps.setInt(3, newTime);
            ps.setString(4, reason);
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] logTrafficChange: " + e.getMessage());
        }
    }
}
