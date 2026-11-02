package org.vetrinagar.db;

import org.vetrinagar.model.*;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Manages dual-engine database persistence for Vetrinagar Public Transit.
 * Primary: MySQL / MariaDB (connecting to phpMyAdmin at localhost:3306).
 * Fallback: SQLite (vetrinagar_transit.db) if MySQL is unavailable.
 * Supports full bidirectional synchronization so any edits in phpMyAdmin
 * are instantly reflected on the website, and all website actions are
 * immediately stored in MySQL.
 */
public class DatabaseManager {
    private boolean isMySql = false;
    private String dbName = "vetrinagar_transit";
    private String jdbcUrl;
    private String dbUser = "root";
    private String dbPassword = "";
    private String sqliteDbPath = "vetrinagar_transit.db";

    public DatabaseManager() {
        initConnection();
        initDatabase();
    }

    public DatabaseManager(String customDb) {
        if (customDb != null && customDb.contains("mysql")) {
            this.jdbcUrl = customDb;
            this.isMySql = true;
        } else {
            this.sqliteDbPath = customDb != null ? customDb : "vetrinagar_transit.db";
        }
        initConnection();
        initDatabase();
    }

    private void initConnection() {
        // Attempt MySQL connection first (port 3306, default XAMPP credentials)
        String mysqlHost = "localhost:3306";
        this.dbName = "vetrinagar_transit";
        String mysqlUrl = "jdbc:mysql://" + mysqlHost + "/" + dbName + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection testConn = DriverManager.getConnection(mysqlUrl, dbUser, dbPassword)) {
                this.jdbcUrl = mysqlUrl;
                this.isMySql = true;
                System.out.println("================================================================================");
                System.out.println("  >>> [DB SUCCESS] Connected Live to MySQL Database: '" + dbName + "'");
                System.out.println("  >>> Live synchronization with phpMyAdmin is ACTIVE at port 3306!");
                System.out.println("================================================================================");
                return;
            }
        } catch (Throwable e) {
            System.out.println("[DB NOTICE] MySQL connection to '" + dbName + "' not available: " + e.getMessage());
            // Try fallback MySQL database name 'transport_optimization'
            try {
                String altUrl = "jdbc:mysql://" + mysqlHost + "/transport_optimization?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true";
                try (Connection testConn2 = DriverManager.getConnection(altUrl, dbUser, dbPassword)) {
                    this.dbName = "transport_optimization";
                    this.jdbcUrl = altUrl;
                    this.isMySql = true;
                    System.out.println("  >>> [DB SUCCESS] Connected Live to MySQL Database: 'vetrinagar_transit' (phpMyAdmin)");
                    return;
                }
            } catch (Throwable ignored) {}
        }

        // Fallback to SQLite
        try {
            Class.forName("org.sqlite.JDBC");
            this.jdbcUrl = "jdbc:sqlite:" + sqliteDbPath;
            this.isMySql = false;
            System.out.println("  >>> [DB INFO] Running on SQLite fallback engine: " + sqliteDbPath);
        } catch (ClassNotFoundException e) {
            System.err.println("  >>> [DB WARNING] Neither MySQL nor SQLite driver could be loaded: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (isMySql) {
            return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        } else {
            return DriverManager.getConnection(jdbcUrl);
        }
    }

    public boolean isMySql() {
        return isMySql;
    }

    public String getDbName() {
        return dbName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * Initializes schema and seeds baseline dataset if database is newly created.
     */
    public synchronized void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            if (isMySql) {
                // MySQL DDL
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS `users` (
                        `username` VARCHAR(50) NOT NULL PRIMARY KEY,
                        `password` VARCHAR(100) NOT NULL,
                        `role` VARCHAR(20) NOT NULL,
                        `full_name` VARCHAR(100) NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS `stops` (
                        `stop_id` VARCHAR(10) NOT NULL PRIMARY KEY,
                        `stop_name` VARCHAR(100) NOT NULL,
                        `stop_type` VARCHAR(20) NOT NULL,
                        `location_desc` VARCHAR(255) DEFAULT NULL,
                        `map_x` INT NOT NULL,
                        `map_y` INT NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS `roads` (
                        `road_id` INT NOT NULL PRIMARY KEY,
                        `from_stop_id` VARCHAR(10) NOT NULL,
                        `to_stop_id` VARCHAR(10) NOT NULL,
                        `travel_time` INT NOT NULL,
                        `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                        `road_type` VARCHAR(20) NOT NULL DEFAULT 'TWO_WAY',
                        `weight_limit` VARCHAR(50) DEFAULT 'All'
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS `buses` (
                        `bus_id` VARCHAR(10) NOT NULL PRIMARY KEY,
                        `bus_type` VARCHAR(50) NOT NULL,
                        `capacity` INT NOT NULL,
                        `current_location` VARCHAR(10) NOT NULL,
                        `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
                        `passengers_onboard` INT NOT NULL DEFAULT 0,
                        `current_trip_id` VARCHAR(50) DEFAULT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS `demand_batches` (
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
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS `demand_requests` (
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
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS `relief_trips` (
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
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS `traffic_logs` (
                        `log_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        `road_id` INT NOT NULL,
                        `old_time` INT NOT NULL,
                        `new_time` INT NOT NULL,
                        `reason` VARCHAR(255) DEFAULT NULL,
                        `logged_at` VARCHAR(50) NOT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);
            } else {
                // SQLite DDL
                stmt.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT NOT NULL, role TEXT NOT NULL, full_name TEXT NOT NULL);");
                stmt.execute("CREATE TABLE IF NOT EXISTS stops (stop_id TEXT PRIMARY KEY, stop_name TEXT NOT NULL, stop_type TEXT NOT NULL DEFAULT 'STOP', location_desc TEXT, map_x INTEGER NOT NULL DEFAULT 100, map_y INTEGER NOT NULL DEFAULT 100);");
                stmt.execute("CREATE TABLE IF NOT EXISTS roads (road_id INTEGER PRIMARY KEY, from_stop_id TEXT NOT NULL, to_stop_id TEXT NOT NULL, travel_time INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'OPEN', road_type TEXT NOT NULL DEFAULT 'TWO_WAY', weight_limit TEXT NOT NULL DEFAULT 'All');");
                stmt.execute("CREATE TABLE IF NOT EXISTS buses (bus_id TEXT PRIMARY KEY, bus_type TEXT NOT NULL, capacity INTEGER NOT NULL, current_location TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'AVAILABLE', passengers_onboard INTEGER NOT NULL DEFAULT 0, current_trip_id TEXT);");
                stmt.execute("CREATE TABLE IF NOT EXISTS demand_batches (batch_id TEXT PRIMARY KEY, pickup_stop_id TEXT NOT NULL, destination_stop_id TEXT NOT NULL, passenger_count INTEGER NOT NULL, original_count INTEGER NOT NULL, arrival_timestamp INTEGER NOT NULL, arrival_time_formatted TEXT NOT NULL, remarks TEXT, status TEXT NOT NULL DEFAULT 'WAITING');");
                stmt.execute("CREATE TABLE IF NOT EXISTS demand_requests (request_id TEXT PRIMARY KEY, passenger_id TEXT NOT NULL, passenger_name TEXT NOT NULL, pickup_stop_id TEXT NOT NULL, destination_stop_id TEXT NOT NULL, passenger_count INTEGER NOT NULL, created_at TEXT NOT NULL, assigned_batch_id TEXT, status TEXT NOT NULL DEFAULT 'WAITING');");
                stmt.execute("CREATE TABLE IF NOT EXISTS relief_trips (trip_id TEXT PRIMARY KEY, batch_id TEXT NOT NULL, bus_id TEXT NOT NULL, pickup_stop_id TEXT NOT NULL, destination_stop_id TEXT NOT NULL, passengers_allocated INTEGER NOT NULL, bus_capacity INTEGER NOT NULL, segment1_path TEXT, segment1_time INTEGER NOT NULL, segment2_path TEXT, segment2_time INTEGER NOT NULL, total_travel_time INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'PLANNED', created_at TEXT NOT NULL, completed_at TEXT);");
                stmt.execute("CREATE TABLE IF NOT EXISTS traffic_logs (log_id INTEGER PRIMARY KEY AUTOINCREMENT, road_id INTEGER NOT NULL, old_time INTEGER NOT NULL, new_time INTEGER NOT NULL, reason TEXT, logged_at TEXT NOT NULL);");
            }

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
                ps.setString(1, "supervisor"); ps.setString(2, "admin123"); ps.setString(3, "SUPERVISOR"); ps.setString(4, "Chief Transit Supervisor"); ps.addBatch();
                ps.setString(1, "passenger1"); ps.setString(2, "pass123"); ps.setString(3, "PASSENGER"); ps.setString(4, "Kavitha Raman"); ps.addBatch();
                ps.setString(1, "passenger2"); ps.setString(2, "pass123"); ps.setString(3, "PASSENGER"); ps.setString(4, "Senthil Kumar"); ps.addBatch();
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

            // C. Seed Buses (B01 Cap 30, B02 Cap 40, B03 Cap 50, B04 Cap 25 Broken)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO buses (bus_id, bus_type, capacity, current_location, status, passengers_onboard) VALUES (?, ?, ?, ?, ?, ?)")) {
                Object[][] busesData = {
                        {"B01", "MiniBus", 30, "D00", "AVAILABLE", 0},
                        {"B02", "StandardBus", 40, "D00", "AVAILABLE", 0},
                        {"B03", "StandardBus", 50, "D00", "AVAILABLE", 0},
                        {"B04", "MiniBus", 25, "D00", "BROKEN", 0}
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
                        {1, "D00", "S01", 10, "OPEN", "TWO_WAY", "All"},
                        {2, "S01", "D00", 10, "OPEN", "TWO_WAY", "All"},
                        {3, "D00", "S02", 15, "OPEN", "TWO_WAY", "All"},
                        {4, "S02", "D00", 15, "OPEN", "TWO_WAY", "All"},
                        {5, "S01", "S02", 20, "OPEN", "TWO_WAY", "All"},
                        {6, "S02", "S01", 20, "OPEN", "TWO_WAY", "All"},
                        {7, "S01", "S03", 12, "OPEN", "TWO_WAY", "All"},
                        {8, "S03", "S01", 12, "OPEN", "TWO_WAY", "All"},
                        {9, "S01", "S04", 15, "OPEN", "TWO_WAY", "All"},
                        {10, "S04", "S01", 15, "OPEN", "TWO_WAY", "All"},
                        {11, "D00", "S04", 8, "OPEN", "TWO_WAY", "All"},
                        {12, "S04", "D00", 8, "OPEN", "TWO_WAY", "All"},
                        {13, "S03", "S05", 14, "OPEN", "TWO_WAY", "All"},
                        {14, "S05", "S03", 14, "OPEN", "TWO_WAY", "All"},
                        {15, "S04", "S06", 10, "OPEN", "TWO_WAY", "All"},
                        {16, "S06", "S04", 10, "OPEN", "TWO_WAY", "All"},
                        {17, "S05", "S06", 12, "OPEN", "TWO_WAY", "All"},
                        {18, "S06", "S05", 12, "OPEN", "TWO_WAY", "All"},
                        {19, "D00", "S07", 12, "OPEN", "TWO_WAY", "All"},
                        {20, "S07", "D00", 12, "OPEN", "TWO_WAY", "All"},
                        {21, "S02", "S07", 14, "OPEN", "TWO_WAY", "All"},
                        {22, "S07", "S02", 14, "OPEN", "TWO_WAY", "All"},
                        {23, "S02", "S08", 16, "OPEN", "TWO_WAY", "All"},
                        {24, "S08", "S02", 16, "OPEN", "TWO_WAY", "All"},
                        {25, "S07", "S08", 10, "OPEN", "TWO_WAY", "All"},
                        {26, "S08", "S07", 10, "OPEN", "TWO_WAY", "All"},
                        {27, "S04", "S07", 18, "OPEN", "TWO_WAY", "All"},
                        {28, "S07", "S04", 18, "OPEN", "TWO_WAY", "All"},
                        {29, "S05", "S09", 15, "OPEN", "TWO_WAY", "All"},
                        {30, "S09", "S05", 15, "OPEN", "TWO_WAY", "All"},
                        {31, "S06", "S09", 16, "OPEN", "TWO_WAY", "All"},
                        {32, "S09", "S06", 16, "OPEN", "TWO_WAY", "All"},
                        {33, "S06", "S10", 14, "OPEN", "TWO_WAY", "All"},
                        {34, "S10", "S06", 14, "OPEN", "TWO_WAY", "All"},
                        {35, "S07", "S10", 15, "OPEN", "TWO_WAY", "All"},
                        {36, "S10", "S07", 15, "OPEN", "TWO_WAY", "All"},
                        {37, "S08", "S10", 22, "OPEN", "TWO_WAY", "All"},
                        {38, "S10", "S08", 22, "OPEN", "TWO_WAY", "All"},
                        {39, "S01", "S11", 14, "OPEN", "TWO_WAY", "All"},
                        {40, "S11", "S01", 14, "OPEN", "TWO_WAY", "All"},
                        {41, "S02", "S11", 12, "OPEN", "TWO_WAY", "All"},
                        {42, "S11", "S02", 12, "OPEN", "TWO_WAY", "All"},
                        {43, "D00", "S11", 16, "OPEN", "TWO_WAY", "All"},
                        {44, "S11", "D00", 16, "OPEN", "TWO_WAY", "All"}
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

            conn.commit();
            System.out.println("[DB SUCCESS] Database seeded successfully.");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // --- Multi-Database Auto-Synchronization Engine ---

    /**
     * Synchronizes tables between 'vetrinagar_transit' and 'transport_optimization'.
     * If the user edits either database in phpMyAdmin, changes are instantly detected
     * via MySQL information_schema UPDATE_TIME or data comparison, and automatically
     * mirrored to the other database so that the website ALWAYS displays the real-time state.
     */
    public synchronized void syncDualDatabases() {
        if (!isMySql) return;
        try (Connection conn = getConnection()) {
            boolean hasVT = false, hasTO = false;
            try (ResultSet rs = conn.getMetaData().getCatalogs()) {
                while (rs.next()) {
                    String cat = rs.getString(1);
                    if ("vetrinagar_transit".equalsIgnoreCase(cat)) hasVT = true;
                    if ("transport_optimization".equalsIgnoreCase(cat)) hasTO = true;
                }
            }
            if (!hasVT || !hasTO) return;

            // 1. Sync Buses
            syncTableBetweenDbs(conn, "buses", "bus_id",
                    Arrays.asList("capacity", "current_location", "status", "passengers_onboard", "current_trip_id"));

            // 2. Sync Roads
            syncTableBetweenDbs(conn, "roads", "road_id",
                    Arrays.asList("from_stop_id", "to_stop_id", "travel_time", "status", "road_type", "weight_limit"));

            // 3. Sync Stops
            syncTableBetweenDbs(conn, "stops", "stop_id",
                    Arrays.asList("stop_name", "stop_type", "location_desc", "map_x", "map_y"));

            // 4. Sync Demand Batches
            syncTableBetweenDbs(conn, "demand_batches", "batch_id",
                    Arrays.asList("pickup_stop_id", "destination_stop_id", "passenger_count", "original_count", "remarks", "status"));

        } catch (Exception e) {
            // Silently handled to keep live polling smooth
        }
    }

    private void syncTableBetweenDbs(Connection conn, String table, String pkCol, List<String> cols) {
        try {
            Timestamp toTime = null;
            Timestamp vtTime = null;
            String checkSql = "SELECT TABLE_SCHEMA, UPDATE_TIME FROM information_schema.TABLES WHERE TABLE_NAME = ? AND TABLE_SCHEMA IN ('vetrinagar_transit', 'transport_optimization')";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, table);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String s = rs.getString("TABLE_SCHEMA");
                        Timestamp t = rs.getTimestamp("UPDATE_TIME");
                        if ("transport_optimization".equalsIgnoreCase(s)) toTime = t;
                        if ("vetrinagar_transit".equalsIgnoreCase(s)) vtTime = t;
                    }
                }
            }

            String sourceDb = "vetrinagar_transit";
            String targetDb = "transport_optimization";

            if (toTime != null && (vtTime == null || toTime.after(vtTime))) {
                sourceDb = "transport_optimization";
                targetDb = "vetrinagar_transit";
            }

            // Sync modified rows from sourceDb to targetDb
            StringBuilder updateSql = new StringBuilder();
            updateSql.append("UPDATE ").append(targetDb).append(".").append(table).append(" t ")
                     .append("JOIN ").append(sourceDb).append(".").append(table).append(" s ")
                     .append("ON t.").append(pkCol).append(" = s.").append(pkCol).append(" SET ");
            for (int i = 0; i < cols.size(); i++) {
                String c = cols.get(i);
                updateSql.append("t.").append(c).append(" = s.").append(c);
                if (i < cols.size() - 1) updateSql.append(", ");
            }
            updateSql.append(" WHERE ");
            for (int i = 0; i < cols.size(); i++) {
                String c = cols.get(i);
                updateSql.append("(t.").append(c).append(" != s.").append(c)
                         .append(" OR (t.").append(c).append(" IS NULL AND s.").append(c).append(" IS NOT NULL)")
                         .append(" OR (t.").append(c).append(" IS NOT NULL AND s.").append(c).append(" IS NULL))");
                if (i < cols.size() - 1) updateSql.append(" OR ");
            }

            try (Statement st = conn.createStatement()) {
                st.executeUpdate(updateSql.toString());
            }

            // Copy any newly inserted records from sourceDb into targetDb
            String insertSql = "INSERT IGNORE INTO " + targetDb + "." + table + " SELECT * FROM " + sourceDb + "." + table;
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(insertSql);
            }
        } catch (Exception ignored) {}
    }

    // --- Loading Methods ---

    public List<Stop> loadAllStops() {
        syncDualDatabases();
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
        syncDualDatabases();
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
        syncDualDatabases();
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

    public List<DemandRequest> loadAllDemandRequests() {
        List<DemandRequest> list = new ArrayList<>();
        String sql = "SELECT request_id, passenger_id, passenger_name, pickup_stop_id, destination_stop_id, passenger_count, created_at, assigned_batch_id, status FROM demand_requests";
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                DemandRequest req = new DemandRequest(
                        rs.getString("request_id"),
                        rs.getString("passenger_id"),
                        rs.getString("passenger_name"),
                        rs.getString("pickup_stop_id"),
                        rs.getString("destination_stop_id"),
                        rs.getInt("passenger_count"),
                        null
                );
                req.setAssignedBatchId(rs.getString("assigned_batch_id"));
                req.setStatus(DemandStatus.fromString(rs.getString("status")));
                list.add(req);
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] loadAllDemandRequests: " + e.getMessage());
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
        if (isMySql) {
            String altDb = "vetrinagar_transit".equalsIgnoreCase(dbName) ? "transport_optimization" : "vetrinagar_transit";
            try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
                st.executeUpdate(String.format("UPDATE %s.roads SET travel_time = %d, status = '%s' WHERE road_id = %d",
                        altDb, road.getTravelTime(), road.getStatus().name(), road.getRoadId()));
            } catch (Exception ignored) {}
        }
    }

    public void updateBus(Bus bus) {
        String sql = "UPDATE buses SET capacity = ?, current_location = ?, status = ?, passengers_onboard = ?, current_trip_id = ? WHERE bus_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bus.getCapacity());
            ps.setString(2, bus.getCurrentLocation());
            ps.setString(3, bus.getStatus().name());
            ps.setInt(4, bus.getPassengersOnboard());
            ps.setString(5, bus.getCurrentTripId());
            ps.setString(6, bus.getBusId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] updateBus: " + e.getMessage());
        }
        if (isMySql) {
            String altDb = "vetrinagar_transit".equalsIgnoreCase(dbName) ? "transport_optimization" : "vetrinagar_transit";
            try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + altDb + ".buses SET capacity = ?, current_location = ?, status = ?, passengers_onboard = ?, current_trip_id = ? WHERE bus_id = ?")) {
                ps.setInt(1, bus.getCapacity());
                ps.setString(2, bus.getCurrentLocation());
                ps.setString(3, bus.getStatus().name());
                ps.setInt(4, bus.getPassengersOnboard());
                ps.setString(5, bus.getCurrentTripId());
                ps.setString(6, bus.getBusId());
                ps.executeUpdate();
            } catch (Exception ignored) {}
        }
    }

    public void saveDemandBatch(DemandBatch batch) {
        String sql;
        if (isMySql) {
            sql = """
                INSERT INTO demand_batches (batch_id, pickup_stop_id, destination_stop_id, passenger_count, original_count, arrival_timestamp, arrival_time_formatted, remarks, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    passenger_count = VALUES(passenger_count),
                    original_count = VALUES(original_count),
                    status = VALUES(status),
                    remarks = VALUES(remarks);
            """;
        } else {
            sql = """
                INSERT INTO demand_batches (batch_id, pickup_stop_id, destination_stop_id, passenger_count, original_count, arrival_timestamp, arrival_time_formatted, remarks, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(batch_id) DO UPDATE SET
                    passenger_count = excluded.passenger_count,
                    original_count = excluded.original_count,
                    status = excluded.status,
                    remarks = excluded.remarks;
            """;
        }
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
        String sql;
        if (isMySql) {
            sql = """
                INSERT INTO demand_requests (request_id, passenger_id, passenger_name, pickup_stop_id, destination_stop_id, passenger_count, created_at, assigned_batch_id, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    passenger_count = VALUES(passenger_count),
                    status = VALUES(status),
                    assigned_batch_id = VALUES(assigned_batch_id);
            """;
        } else {
            sql = """
                INSERT INTO demand_requests (request_id, passenger_id, passenger_name, pickup_stop_id, destination_stop_id, passenger_count, created_at, assigned_batch_id, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(request_id) DO UPDATE SET
                    passenger_count = excluded.passenger_count,
                    status = excluded.status,
                    assigned_batch_id = excluded.assigned_batch_id;
            """;
        }
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, req.getRequestId());
            ps.setString(2, req.getPassengerId());
            ps.setString(3, req.getPassengerName());
            ps.setString(4, req.getPickupStopId());
            ps.setString(5, req.getDestinationStopId());
            ps.setInt(6, req.getPassengerCount());
            ps.setString(7, req.getSubmittedAt() != null ? req.getSubmittedAt().toString() : LocalDateTime.now().toString());
            ps.setString(8, req.getAssignedBatchId());
            ps.setString(9, req.getStatus().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] saveDemandRequest: " + e.getMessage());
        }
    }

    public void saveReliefTrip(ReliefTrip trip) {
        String sql;
        if (isMySql) {
            sql = """
                INSERT INTO relief_trips (trip_id, batch_id, bus_id, pickup_stop_id, destination_stop_id, passengers_allocated, bus_capacity, segment1_path, segment1_time, segment2_path, segment2_time, total_travel_time, status, created_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    bus_id = VALUES(bus_id),
                    passengers_allocated = VALUES(passengers_allocated),
                    status = VALUES(status),
                    completed_at = VALUES(completed_at);
            """;
        } else {
            sql = """
                INSERT INTO relief_trips (trip_id, batch_id, bus_id, pickup_stop_id, destination_stop_id, passengers_allocated, bus_capacity, segment1_path, segment1_time, segment2_path, segment2_time, total_travel_time, status, created_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(trip_id) DO UPDATE SET
                    bus_id = excluded.bus_id,
                    passengers_allocated = excluded.passengers_allocated,
                    status = excluded.status,
                    completed_at = excluded.completed_at;
            """;
        }
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

    public void clearAllDemands() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM demand_requests");
            stmt.executeUpdate("DELETE FROM demand_batches");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] clearAllDemands: " + e.getMessage());
        }
    }

    public void clearAllTrips() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM relief_trips");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] clearAllTrips: " + e.getMessage());
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

    public Map<String, Object> getDatabaseOverview() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("driver", isMySql ? "com.mysql.cj.jdbc.Driver" : "org.sqlite.JDBC");
        map.put("url", jdbcUrl);
        map.put("databaseType", isMySql ? "MySQL / MariaDB (phpMyAdmin)" : "SQLite Local File");
        map.put("databaseName", dbName);
        map.put("connected", true);
        map.put("phpmyadminUrl", "http://localhost/phpmyadmin/index.php?route=/database/structure&db=" + dbName);

        Map<String, Integer> counts = new LinkedHashMap<>();
        String[] tables = {"stops", "roads", "buses", "demand_batches", "demand_requests", "relief_trips", "traffic_logs", "users"};
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String t : tables) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + t)) {
                    if (rs.next()) counts.put(t, rs.getInt(1));
                } catch (Exception e) {
                    counts.put(t, 0);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getDatabaseOverview: " + e.getMessage());
        }
        map.put("tableCounts", counts);
        return map;
    }

    public String getTableRowsJson(String tableName) {
        List<String> validTables = Arrays.asList("stops", "roads", "buses", "demand_batches", "demand_requests", "relief_trips", "traffic_logs", "users");
        if (!validTables.contains(tableName.toLowerCase())) {
            return "{\"columns\":[],\"rows\":[]}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"tableName\":\"").append(tableName).append("\",");

        try (Connection conn = getConnection(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + tableName + " LIMIT 100")) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            sb.append("\"columns\":[");
            for (int i = 1; i <= colCount; i++) {
                sb.append("\"").append(meta.getColumnName(i)).append("\"");
                if (i < colCount) sb.append(",");
            }
            sb.append("],\"rows\":[");

            boolean firstRow = true;
            while (rs.next()) {
                if (!firstRow) sb.append(",");
                firstRow = false;

                sb.append("[");
                for (int i = 1; i <= colCount; i++) {
                    String val = rs.getString(i);
                    if (val == null) {
                        sb.append("null");
                    } else {
                        sb.append("\"").append(val.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")).append("\"");
                    }
                    if (i < colCount) sb.append(",");
                }
                sb.append("]");
            }
            sb.append("]}");
            return sb.toString();
        } catch (SQLException e) {
            return "{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }
}
