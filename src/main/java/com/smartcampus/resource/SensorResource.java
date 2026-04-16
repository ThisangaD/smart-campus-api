package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 — Sensor Resource
 * Manages sensors and delegates to SensorReadingResource via sub-resource locator.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    // ── GET /sensors  (with optional ?type= filter) ───────────────────────────

    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensorList = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.trim().isEmpty()) {
            // Case-insensitive filter by sensor type
            sensorList = sensorList.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type.trim()))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensorList).build();
    }

    // ── POST /sensors ─────────────────────────────────────────────────────────

    @POST
    public Response createSensor(Sensor sensor) {
        // Validate required fields
        if (sensor == null || sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", 400, "error", "Bad Request",
                                   "message", "Sensor 'id' is required"))
                    .build();
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("status", 409, "error", "Conflict",
                                   "message", "Sensor '" + sensor.getId() + "' already exists"))
                    .build();
        }

        // Integrity check: the referenced roomId must exist
        if (sensor.getRoomId() == null || !store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                    "Cannot register sensor: Room with ID '"
                    + sensor.getRoomId() + "' does not exist in the system."
            );
        }

        // Default status to ACTIVE if not provided
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        // Persist sensor
        store.getSensors().put(sensor.getId(), sensor);

        // Maintain bidirectional link: add sensorId to the parent room's list
        store.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        // Initialise an empty readings list for this sensor
        store.getSensorReadings().put(sensor.getId(), new ArrayList<>());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // ── GET /sensors/{sensorId} ───────────────────────────────────────────────

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                                   "message", "Sensor not found: " + sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // ── SUB-RESOURCE LOCATOR: /sensors/{sensorId}/readings ───────────────────
    // No HTTP method annotation → JAX-RS treats this as a sub-resource locator.
    // Jersey delegates all further path matching to the returned object.

    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
