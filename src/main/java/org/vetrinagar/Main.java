package org.vetrinagar;

import org.vetrinagar.cli.ConsoleApplication;
import org.vetrinagar.db.DatabaseManager;
import org.vetrinagar.policy.DefaultDemandPolicy;
import org.vetrinagar.service.TransportService;
import org.vetrinagar.web.WebServer;

/**
 * Main application entry point for Vetrinagar Public Transit Route Optimization.
 * Launches embedded SQLite database, core service layer, embedded Web Server,
 * and console interface.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  INITIATING VETRINAGAR SMART PUBLIC TRANSPORTATION SYSTEM                     ");
        System.out.println("  Project: Mini Project 5 - Route Optimization & Relief Allocation             ");
        System.out.println("================================================================================");

        // 1. Initialize SQLite Database
        DatabaseManager dbManager = new DatabaseManager("vetrinagar_transit.db");

        // 2. Initialize Transport Service Engine
        TransportService service = new TransportService(dbManager, new DefaultDemandPolicy());

        // 3. Start Embedded Web Server
        int webPort = 8080;
        try {
            WebServer webServer = new WebServer(service, webPort);
            webServer.start();
        } catch (Exception e) {
            System.err.println("[WEB WARNING] Could not start web server on port " + webPort + " (" + e.getMessage() + "). Console mode remains active.");
        }

        // 4. Check launch arguments
        boolean cliOnly = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                cliOnly = true;
                break;
            }
        }

        // 5. Start Console CLI Application
        ConsoleApplication consoleApp = new ConsoleApplication(service);
        consoleApp.start();
    }
}
