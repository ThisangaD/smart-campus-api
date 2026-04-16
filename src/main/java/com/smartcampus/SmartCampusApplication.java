package com.smartcampus;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * JAX-RS Application subclass.
 *
 * @ApplicationPath sets the API's versioned base path.
 * ResourceConfig auto-scans all classes in the "com.smartcampus" package
 * so every @Provider, @Path, filter, and exception mapper is registered
 * automatically without manual registration.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Auto-scan the entire com.smartcampus package tree
        packages("com.smartcampus");
        // Register Jackson for JSON serialisation / deserialisation
        register(JacksonFeature.class);
    }
}
