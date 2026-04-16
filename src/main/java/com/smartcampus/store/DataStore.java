package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory data store — singleton with ConcurrentHashMap for thread safety.
 *
 * WHY ConcurrentHashMap?
 * JAX-RS resource classes are per-request by default (a new instance per request).
 * The DataStore singleton is shared across all those instances. Without thread-safe
 * collections, concurrent requests could corrupt the maps. ConcurrentHashMap provides
 * atomic operations for get/put/remove without requiring explicit synchronisation.
 */
public class DataStore {

    // Eagerly initialised singleton
    private static final DataStore INSTANCE = new DataStore();

    // Primary collections — thread-safe maps keyed by resource ID
    private final Map<String, Room>              rooms          = new ConcurrentHashMap<>();
    private final Map<String, Sensor>            sensors        = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    // ── Private constructor with seed data ────────────────────────────────────

    private DataStore() {
        // Seed rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-102", "Computer Lab 102",    30);
        Room r3 = new Room("HALL-01", "Main Hall",          200);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        // Seed sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE",      22.5,  "LIB-301");
        Sensor s2 = new Sensor("CO2-001",  "CO2",         "ACTIVE",      415.0, "LAB-102");
        Sensor s3 = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE", 0.0,   "LIB-301");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        // Link sensors to rooms
        r1.getSensorIds().add(s1.getId());
        r1.getSensorIds().add(s3.getId());
        r2.getSensorIds().add(s2.getId());

        // Seed one reading for each active sensor
        sensorReadings.put(s1.getId(), new ArrayList<>(
                Collections.singletonList(new SensorReading(22.5))));
        sensorReadings.put(s2.getId(), new ArrayList<>(
                Collections.singletonList(new SensorReading(415.0))));
        sensorReadings.put(s3.getId(), new ArrayList<>());
    }

    // ── Singleton accessor ────────────────────────────────────────────────────

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Map<String, Room>    getRooms()   { return rooms; }
    public Map<String, Sensor>  getSensors() { return sensors; }

    /**
     * Returns (and lazily initialises) the readings list for a sensor.
     * computeIfAbsent is atomic in ConcurrentHashMap.
     */
    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return sensorReadings.computeIfAbsent(sensorId, k -> new ArrayList<>());
    }

    public Map<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }
}
