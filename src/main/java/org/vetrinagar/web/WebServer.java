package org.vetrinagar.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.vetrinagar.graph.RouteResult;
import org.vetrinagar.model.*;
import org.vetrinagar.service.TransportService;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Embedded HTTP Server for Vetrinagar Public Transit.
 * Provides REST API and serves the rich interactive visual web application.
 */
public class WebServer {
    private final TransportService service;
    private int port;
    private HttpServer server;
    private final String staticResourceDir;

    public WebServer(TransportService service, int port) {
        this.service = service;
        this.port = port;
        this.staticResourceDir = "src/main/resources/web";
    }

    public WebServer(TransportService service) {
        this(service, 8080);
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        int[] portsToTry = {port, 8080, 8081, 8082, 8088, 3000};
        IOException lastEx = null;
        for (int p : portsToTry) {
            try {
                server = HttpServer.create(new InetSocketAddress("0.0.0.0", p), 0);
                this.port = p;
                lastEx = null;
                break;
            } catch (IOException e) {
                lastEx = e;
            }
        }
        if (server == null) {
            // Try wildcard without explicit 0.0.0.0 string
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
            } catch (IOException e) {
                if (lastEx != null) throw lastEx;
                throw e;
            }
        }

        // API Endpoints
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/passenger/request", new PassengerRequestHandler());
        server.createContext("/api/supervisor/allocate", new AllocateHandler());
        server.createContext("/api/supervisor/complete", new CompleteTripHandler());
        server.createContext("/api/supervisor/traffic", new TrafficUpdateHandler());
        server.createContext("/api/supervisor/toggle-road", new ToggleRoadHandler());
        server.createContext("/api/supervisor/breakdown", new BreakdownHandler());
        server.createContext("/api/supervisor/repair", new RepairBusHandler());
        server.createContext("/api/supervisor/dijkstra", new DijkstraQueryHandler());
        server.createContext("/api/supervisor/clear-queue", new ClearQueueHandler());
        server.createContext("/api/supervisor/reset-all", new ResetAllHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/database", new DatabaseOverviewHandler());
        server.createContext("/api/database/table", new TableRowsHandler());

        // Redirect /phpmyadmin on port 8080 to XAMPP Apache phpMyAdmin on port 80
        server.createContext("/phpmyadmin", exchange -> {
            exchange.getResponseHeaders().set("Location", "http://localhost/phpmyadmin/");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        // Static File Handler
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();

        System.out.println("================================================================================");
        System.out.println("  >>> VETRINAGAR TRANSIT WEB APPLICATION RUNNING AT: http://localhost:" + port + "/");
        System.out.println("================================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --- Helper Methods ---

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String val = kv[1].trim().replace("\"", "");
                map.put(key, val);
            }
        }
        return map;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // --- Handlers ---

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{");

            // Stops
            sb.append("\"stops\":[");
            List<Stop> stops = service.getAllStops();
            for (int i = 0; i < stops.size(); i++) {
                Stop s = stops.get(i);
                sb.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"desc\":\"%s\",\"x\":%d,\"y\":%d}",
                        escapeJson(s.getStopId()), escapeJson(s.getStopName()), escapeJson(s.getStopType().name()),
                        escapeJson(s.getLocationDesc()), s.getMapX(), s.getMapY()));
                if (i < stops.size() - 1) sb.append(",");
            }
            sb.append("],");

            // Roads
            sb.append("\"roads\":[");
            List<Road> roads = service.getAllRoads();
            for (int i = 0; i < roads.size(); i++) {
                Road r = roads.get(i);
                sb.append(String.format("{\"id\":%d,\"from\":\"%s\",\"to\":\"%s\",\"time\":%d,\"status\":\"%s\",\"type\":\"%s\",\"restriction\":\"%s\"}",
                        r.getRoadId(), escapeJson(r.getFromStopId()), escapeJson(r.getToStopId()),
                        r.getTravelTime(), r.getStatus().name(), r.getRoadType().name(), escapeJson(r.getWeightLimit())));
                if (i < roads.size() - 1) sb.append(",");
            }
            sb.append("],");

            // Buses
            sb.append("\"buses\":[");
            List<Bus> buses = service.getAllBuses();
            for (int i = 0; i < buses.size(); i++) {
                Bus b = buses.get(i);
                int remaining = Math.max(0, b.getCapacity() - b.getPassengersOnboard());
                sb.append(String.format("{\"id\":\"%s\",\"type\":\"%s\",\"capacity\":%d,\"remaining\":%d,\"location\":\"%s\",\"status\":\"%s\",\"load\":%d,\"tripId\":%s}",
                        escapeJson(b.getBusId()), escapeJson(b.getBusType()), b.getCapacity(), remaining,
                        escapeJson(b.getCurrentLocation()), b.getStatus().name(), b.getPassengersOnboard(),
                        b.getCurrentTripId() != null ? "\"" + escapeJson(b.getCurrentTripId()) + "\"" : "null"));
                if (i < buses.size() - 1) sb.append(",");
            }
            sb.append("],");

            // Ranked Waiting Batches
            sb.append("\"rankedDemand\":[");
            List<DemandBatch> waiting = service.getRankedWaitingBatches();
            for (int i = 0; i < waiting.size(); i++) {
                DemandBatch d = waiting.get(i);
                sb.append(String.format("{\"id\":\"%s\",\"pickup\":\"%s\",\"dest\":\"%s\",\"count\":%d,\"orig\":%d,\"time\":\"%s\",\"remarks\":\"%s\",\"status\":\"%s\"}",
                        escapeJson(d.getBatchId()), escapeJson(d.getPickupStopId()), escapeJson(d.getDestinationStopId()),
                        d.getPassengerCount(), d.getOriginalCount(), escapeJson(d.getArrivalTimeFormatted()),
                        escapeJson(d.getRemarks()), d.getStatus().name()));
                if (i < waiting.size() - 1) sb.append(",");
            }
            sb.append("],");

            // All Demand Batches
            sb.append("\"allDemand\":[");
            List<DemandBatch> allBatches = service.getAllDemandBatches();
            for (int i = 0; i < allBatches.size(); i++) {
                DemandBatch d = allBatches.get(i);
                sb.append(String.format("{\"id\":\"%s\",\"pickup\":\"%s\",\"dest\":\"%s\",\"count\":%d,\"orig\":%d,\"time\":\"%s\",\"remarks\":\"%s\",\"status\":\"%s\"}",
                        escapeJson(d.getBatchId()), escapeJson(d.getPickupStopId()), escapeJson(d.getDestinationStopId()),
                        d.getPassengerCount(), d.getOriginalCount(), escapeJson(d.getArrivalTimeFormatted()),
                        escapeJson(d.getRemarks()), d.getStatus().name()));
                if (i < allBatches.size() - 1) sb.append(",");
            }
            sb.append("],");

            // Trips
            sb.append("\"trips\":[");
            List<ReliefTrip> trips = service.getAllTrips();
            for (int i = 0; i < trips.size(); i++) {
                ReliefTrip t = trips.get(i);
                sb.append(String.format("{\"id\":\"%s\",\"batchId\":\"%s\",\"busId\":\"%s\",\"pickup\":\"%s\",\"dest\":\"%s\",\"pax\":%d,\"cap\":%d,\"seg1\":[\"%s\"],\"seg1Time\":%d,\"seg2\":[\"%s\"],\"seg2Time\":%d,\"totalTime\":%d,\"status\":\"%s\",\"createdAt\":\"%s\"}",
                        escapeJson(t.getTripId()), escapeJson(t.getBatchId()), escapeJson(t.getBusId()),
                        escapeJson(t.getPickupStopId()), escapeJson(t.getDestinationStopId()),
                        t.getPassengersAllocated(), t.getBusCapacity(),
                        String.join("\",\"", t.getSegment1Path()), t.getSegment1Time(),
                        String.join("\",\"", t.getSegment2Path()), t.getSegment2Time(),
                        t.getTotalTravelTime(), t.getStatus().name(),
                        t.getCreatedAt().toString()));
                if (i < trips.size() - 1) sb.append(",");
            }
            sb.append("],");

            // Consolidated Stop-Level Crowds
            sb.append("\"stopCrowds\":[");
            List<TransportService.StopCrowdSummary> crowds = service.getStopCrowdSummaries();
            for (int i = 0; i < crowds.size(); i++) {
                TransportService.StopCrowdSummary c = crowds.get(i);
                sb.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"pax\":%d,\"reqCount\":%d,\"breakdown\":[\"%s\"]}",
                        escapeJson(c.stopId()), escapeJson(c.stopName()), c.totalWaitingPax(), c.requestCount(),
                        String.join("\",\"", c.breakdowns())));
                if (i < crowds.size() - 1) sb.append(",");
            }
            sb.append("],");

            // Metrics
            TransportService.TransitMetrics m = service.calculateMetrics();
            sb.append(String.format("\"metrics\":{\"waiting\":%d,\"served\":%d,\"activeTrips\":%d,\"availBuses\":%d,\"totalFleet\":%d,\"utilization\":%.1f,\"co2Saved\":%.2f,\"avgWaitTime\":%.1f}",
                    m.totalPassengersWaiting(), m.totalPassengersServed(), m.activeReliefTrips(),
                    m.availableBuses(), m.totalFleetSize(), m.fleetUtilizationPercent(),
                    m.co2EmissionsSavedKg(), m.averageWaitTimeMinutes()));

            sb.append("}");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class PassengerRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseSimpleJson(body);

            String name = data.getOrDefault("passengerName", "Passenger");
            String pickup = data.getOrDefault("pickupStopId", "").toUpperCase();
            String dest = data.getOrDefault("destinationStopId", "").toUpperCase();
            int count = 1;
            try {
                count = Integer.parseInt(data.getOrDefault("passengerCount", "1"));
            } catch (Exception ignored) {}

            try {
                DemandRequest req = service.submitPassengerDemand("PASS-" + UUID.randomUUID().toString().substring(0, 5), name, pickup, dest, count);
                String resp = String.format("{\"success\":true,\"requestId\":\"%s\",\"batchId\":\"%s\",\"message\":\"Demand submitted successfully and aggregated!\"}",
                        req.getRequestId(), req.getAssignedBatchId());
                sendJsonResponse(exchange, 200, resp);
            } catch (Exception e) {
                String resp = String.format("{\"success\":false,\"message\":\"%s\"}", escapeJson(e.getMessage()));
                sendJsonResponse(exchange, 400, resp);
            }
        }
    }

    private class AllocateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            TransportService.AllocationResult res = service.allocateReliefBus();
            if (res.success()) {
                ReliefTrip t = res.trip();
                String resp = String.format("{\"success\":true,\"tripId\":\"%s\",\"busId\":\"%s\",\"batchId\":\"%s\",\"boarded\":%d,\"totalTime\":%d,\"message\":\"%s\"}",
                        t.getTripId(), t.getBusId(), t.getBatchId(), t.getPassengersAllocated(), t.getTotalTravelTime(), escapeJson(res.message()));
                sendJsonResponse(exchange, 200, resp);
            } else {
                String resp = String.format("{\"success\":false,\"message\":\"%s\"}", escapeJson(res.message()));
                sendJsonResponse(exchange, 400, resp);
            }
        }
    }

    private class CompleteTripHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseSimpleJson(body);
            String tripId = data.getOrDefault("tripId", "").toUpperCase();

            boolean done = service.completeTrip(tripId);
            if (done) {
                sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Trip completed successfully.\"}");
            } else {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"message\":\"Trip not found or already completed.\"}");
            }
        }
    }

    private class TrafficUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseSimpleJson(body);

            try {
                int roadId = Integer.parseInt(data.get("roadId"));
                int time = Integer.parseInt(data.get("travelTime"));
                String reason = data.getOrDefault("reason", "Supervisor adjustment");
                boolean ok = service.updateRoadTraffic(roadId, time, reason);
                sendJsonResponse(exchange, 200, String.format("{\"success\":%b,\"message\":\"Road #%d travel time updated to %d mins.\"}", ok, roadId, time));
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, String.format("{\"success\":false,\"message\":\"%s\"}", escapeJson(e.getMessage())));
            }
        }
    }

    private class ToggleRoadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseSimpleJson(body);
            try {
                int roadId = Integer.parseInt(data.get("roadId"));
                Road r = service.getRoad(roadId);
                if (r != null) {
                    RoadStatus next = r.isOpen() ? RoadStatus.CLOSED : RoadStatus.OPEN;
                    boolean ok = service.toggleRoadStatus(roadId, next);
                    sendJsonResponse(exchange, 200, String.format("{\"success\":%b,\"newStatus\":\"%s\"}", ok, next.name()));
                } else {
                    sendJsonResponse(exchange, 404, "{\"success\":false,\"message\":\"Road not found.\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, String.format("{\"success\":false,\"message\":\"%s\"}", escapeJson(e.getMessage())));
            }
        }
    }

    private class BreakdownHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseSimpleJson(body);
            String busId = data.getOrDefault("busId", "").toUpperCase();

            TransportService.AllocationResult res = service.reportBusBreakdown(busId);
            sendJsonResponse(exchange, 200, String.format("{\"success\":%b,\"message\":\"%s\"}", res.success(), escapeJson(res.message())));
        }
    }

    private class RepairBusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseSimpleJson(body);
            String busId = data.getOrDefault("busId", "").toUpperCase();
            boolean ok = service.repairBus(busId);
            sendJsonResponse(exchange, 200, String.format("{\"success\":%b,\"message\":\"Bus %s repaired and released.\"}", ok, busId));
        }
    }

    private class DijkstraQueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> qm = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2) {
                        qm.put(pair[0], pair[1]);
                    }
                }
            }
            String from = qm.get("from");
            String to = qm.get("to");
            String busId = qm.get("busId");
            Bus bus = busId != null ? service.getBus(busId) : null;

            RouteResult r = service.getRouter().findShortestRoute(from, to, bus);
            String pathJson = "[\"" + String.join("\",\"", r.getPathStopIds()) + "\"]";
            String resp = String.format("{\"reachable\":%b,\"time\":%d,\"path\":%s,\"message\":\"%s\"}",
                    r.isReachable(), r.getTotalTravelTime(), pathJson, escapeJson(r.getErrorMessage()));
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private class ClearQueueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            service.clearReliefQueue();
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Relief queue cleared successfully.\"}");
        }
    }

    private class ResetAllHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            service.resetAllTransitState();
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"All queues cleared and buses reset to Central Depot (D00) with full capacity.\"}");
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String body = readRequestBody(exchange);
            Map<String, String> data = parseSimpleJson(body);
            String user = data.getOrDefault("username", "");
            String pass = data.getOrDefault("password", "");

            User u = service.authenticate(user, pass);
            if (u != null) {
                String resp = String.format("{\"success\":true,\"user\":{\"username\":\"%s\",\"role\":\"%s\",\"fullName\":\"%s\"}}",
                        escapeJson(u.getUsername()), u.getRole().name(), escapeJson(u.getFullName()));
                sendJsonResponse(exchange, 200, resp);
            } else {
                sendJsonResponse(exchange, 401, "{\"success\":false,\"message\":\"Invalid credentials. Try supervisor/admin123 or passenger1/pass123.\"}");
            }
        }
    }

    private class DatabaseOverviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            Map<String, Object> meta = service.getDatabaseManager().getDatabaseOverview();
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"driver\":\"").append(escapeJson((String) meta.get("driver"))).append("\",");
            sb.append("\"url\":\"").append(escapeJson((String) meta.get("url"))).append("\",");
            sb.append("\"databaseType\":\"").append(escapeJson((String) meta.get("databaseType"))).append("\",");
            sb.append("\"connected\":true,");
            sb.append("\"filePath\":\"").append(escapeJson((String) meta.get("filePath"))).append("\",");
            sb.append("\"fileSizeFormatted\":\"").append(escapeJson((String) meta.get("fileSizeFormatted"))).append("\",");

            sb.append("\"tables\":{");
            @SuppressWarnings("unchecked")
            Map<String, Integer> counts = (Map<String, Integer>) meta.get("tableCounts");
            if (counts != null) {
                int i = 0;
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
                    if (i++ < counts.size() - 1) sb.append(",");
                }
            }
            sb.append("}}");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class TableRowsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String query = exchange.getRequestURI().getQuery();
            String tableName = "stops";
            if (query != null && query.contains("name=")) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2 && "name".equalsIgnoreCase(pair[0])) {
                        tableName = pair[1];
                        break;
                    }
                }
            }
            String json = service.getDatabaseManager().getTableRowsJson(tableName);
            sendJsonResponse(exchange, 200, json);
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String uriPath = exchange.getRequestURI().getPath();
            if (uriPath.equals("/") || uriPath.isBlank()) {
                uriPath = "/index.html";
            }

            Path filePath = Paths.get(staticResourceDir, uriPath);
            File file = filePath.toFile();

            if (!file.exists()) {
                String[] candidateRoots = {
                        "src/main/resources/web",
                        "../src/main/resources/web",
                        "C:/Users/sakth/.gemini/antigravity/scratch/smart-transit-optimization/src/main/resources/web",
                        System.getProperty("user.dir") + "/src/main/resources/web"
                };
                for (String root : candidateRoots) {
                    File candidate = new File(root, uriPath);
                    if (candidate.exists()) {
                        filePath = candidate.toPath();
                        file = candidate;
                        break;
                    }
                }
            }

            if (!file.exists() || file.isDirectory()) {
                String notFound = "<h1>404 Not Found</h1>";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            String mime = "text/plain";
            if (uriPath.endsWith(".html")) mime = "text/html; charset=UTF-8";
            else if (uriPath.endsWith(".css")) mime = "text/css; charset=UTF-8";
            else if (uriPath.endsWith(".js")) mime = "application/javascript; charset=UTF-8";
            else if (uriPath.endsWith(".svg")) mime = "image/svg+xml";

            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
