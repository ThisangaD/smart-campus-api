package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Application entry point.
 * Starts an embedded Grizzly HTTP server on port 8080.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    // Base URI — all endpoints will be served under /api/v1
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static void main(String[] args) throws IOException {
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI),
                new SmartCampusApplication()
        );

        LOGGER.info("====================================================");
        LOGGER.info("  Smart Campus API is running!");
        LOGGER.info("  Base URI : " + BASE_URI);
        LOGGER.info("  Discovery: http://localhost:8080/api/v1");
        LOGGER.info("  Rooms    : http://localhost:8080/api/v1/rooms");
        LOGGER.info("  Sensors  : http://localhost:8080/api/v1/sensors");
        LOGGER.info("  Press ENTER to stop the server.");
        LOGGER.info("====================================================");

        System.in.read(); // Block until user presses ENTER
        server.shutdownNow();
    }
}
