package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1 — Discovery Endpoint
 * GET /api/v1  →  returns API metadata and hypermedia links (HATEOAS)
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();

        // API metadata
        response.put("api",         "Smart Campus Sensor & Room Management API");
        response.put("version",     "v1");
        response.put("contact",     "admin@smartcampus.ac.uk");
        response.put("description", "RESTful API for managing university campus rooms and IoT sensors");

        // HATEOAS links — clients can navigate the API from this single entry point
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1");
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("_links", links);

        // Resource collection index
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",   "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        response.put("resources", resources);

        return Response.ok(response).build();
    }
}
