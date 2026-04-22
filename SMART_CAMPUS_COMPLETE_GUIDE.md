# 🏫 Smart Campus – Sensor & Room Management API
### 5COSC022W Client-Server Architectures Coursework — Complete Implementation Guide

> **Stack:** Java 11 · JAX-RS (Jersey 2.40) · Grizzly HTTP Server · Jackson JSON · Maven  
> **Due:** 24 April 2026, 13:00  
> **Weight:** 60% of final grade

---

## 📁 Complete Project File Structure

```
smart-campus-api/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/
                └── smartcampus/
                    ├── Main.java
                    ├── SmartCampusApplication.java
                    ├── model/
                    │   ├── Room.java
                    │   ├── Sensor.java
                    │   ├── SensorReading.java
                    │   └── ErrorResponse.java
                    ├── store/
                    │   └── DataStore.java
                    ├── resource/
                    │   ├── DiscoveryResource.java
                    │   ├── RoomResource.java
                    │   ├── SensorResource.java
                    │   └── SensorReadingResource.java
                    ├── exception/
                    │   ├── RoomNotEmptyException.java
                    │   ├── RoomNotEmptyExceptionMapper.java
                    │   ├── LinkedResourceNotFoundException.java
                    │   ├── LinkedResourceNotFoundExceptionMapper.java
                    │   ├── SensorUnavailableException.java
                    │   ├── SensorUnavailableExceptionMapper.java
                    │   └── GlobalExceptionMapper.java
                    └── filter/
                        └── LoggingFilter.java
```

---

## ⚙️ Step 1 — Prerequisites

Install these before starting:

| Tool | Version | Download |
|------|---------|---------|
| Java JDK | 11+ | https://adoptium.net |
| Apache Maven | 3.8+ | https://maven.apache.org |
| Git | Latest | https://git-scm.com |
| Postman | Latest | https://postman.com |

Verify installations:
```bash
java -version    # should say 11 or higher
mvn -version     # should say 3.8 or higher
```

---

## ⚙️ Step 2 — Create the Maven Project

```bash
# Create project scaffold
mvn archetype:generate \
  -DgroupId=com.smartcampus \
  -DartifactId=smart-campus-api \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.4 \
  -DinteractiveMode=false

cd smart-campus-api

# Delete the generated App.java and AppTest.java
rm src/main/java/com/smartcampus/App.java
rm -rf src/test/

# Create the sub-directories
mkdir -p src/main/java/com/smartcampus/model
mkdir -p src/main/java/com/smartcampus/store
mkdir -p src/main/java/com/smartcampus/resource
mkdir -p src/main/java/com/smartcampus/exception
mkdir -p src/main/java/com/smartcampus/filter
```

---

## 📄 FILE 1 — `pom.xml`

Replace the entire contents of `pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.smartcampus</groupId>
    <artifactId>smart-campus-api</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>Smart Campus API</name>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <jersey.version>2.40</jersey.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Jersey with Grizzly2 Embedded HTTP Server -->
        <dependency>
            <groupId>org.glassfish.jersey.containers</groupId>
            <artifactId>jersey-container-grizzly2-http</artifactId>
            <version>${jersey.version}</version>
        </dependency>

        <!-- Jersey HK2 Dependency Injection -->
        <dependency>
            <groupId>org.glassfish.jersey.inject</groupId>
            <artifactId>jersey-hk2</artifactId>
            <version>${jersey.version}</version>
        </dependency>

        <!-- Jackson JSON provider for Jersey -->
        <dependency>
            <groupId>org.glassfish.jersey.media</groupId>
            <artifactId>jersey-media-json-jackson</artifactId>
            <version>${jersey.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>

            <!-- Shade Plugin — builds a single executable fat JAR -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <!-- Set the main class -->
                                <transformer implementation=
                                  "org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.smartcampus.Main</mainClass>
                                </transformer>
                                <!-- Merge META-INF/services (critical for Jersey providers) -->
                                <transformer implementation=
                                  "org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                            </transformers>
                            <!-- Exclude signature files that cause security exceptions -->
                            <filters>
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 📄 FILE 2 — `Main.java`

Path: `src/main/java/com/smartcampus/Main.java`

```java
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
```

---

## 📄 FILE 3 — `SmartCampusApplication.java`

Path: `src/main/java/com/smartcampus/SmartCampusApplication.java`

```java
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
```

---

## 📄 FILE 4 — `model/Room.java`

Path: `src/main/java/com/smartcampus/model/Room.java`

```java
package com.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a physical room on campus.
 */
public class Room {

    private String id;           // e.g. "LIB-301"
    private String name;         // e.g. "Library Quiet Study"
    private int    capacity;     // Maximum occupancy
    private List<String> sensorIds = new ArrayList<>(); // IDs of deployed sensors

    // ── Constructors ──────────────────────────────────────────────────────────

    public Room() {}

    public Room(String id, String name, int capacity) {
        this.id       = id;
        this.name     = name;
        this.capacity = capacity;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                      { return id; }
    public void   setId(String id)             { this.id = id; }

    public String getName()                    { return name; }
    public void   setName(String name)         { this.name = name; }

    public int    getCapacity()                { return capacity; }
    public void   setCapacity(int capacity)    { this.capacity = capacity; }

    public List<String> getSensorIds()         { return sensorIds; }
    public void setSensorIds(List<String> ids) { this.sensorIds = ids; }
}
```

---

## 📄 FILE 5 — `model/Sensor.java`

Path: `src/main/java/com/smartcampus/model/Sensor.java`

```java
package com.smartcampus.model;

/**
 * Represents a sensor deployed in a campus room.
 * Status values: "ACTIVE" | "MAINTENANCE" | "OFFLINE"
 */
public class Sensor {

    private String id;             // e.g. "TEMP-001"
    private String type;           // e.g. "Temperature", "CO2", "Occupancy"
    private String status;         // ACTIVE | MAINTENANCE | OFFLINE
    private double currentValue;   // Most recent measurement
    private String roomId;         // Foreign key → Room

    // ── Constructors ──────────────────────────────────────────────────────────

    public Sensor() {}

    public Sensor(String id, String type, String status,
                  double currentValue, String roomId) {
        this.id           = id;
        this.type         = type;
        this.status       = status;
        this.currentValue = currentValue;
        this.roomId       = roomId;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                         { return id; }
    public void   setId(String id)                { this.id = id; }

    public String getType()                       { return type; }
    public void   setType(String type)            { this.type = type; }

    public String getStatus()                     { return status; }
    public void   setStatus(String status)        { this.status = status; }

    public double getCurrentValue()               { return currentValue; }
    public void   setCurrentValue(double v)       { this.currentValue = v; }

    public String getRoomId()                     { return roomId; }
    public void   setRoomId(String roomId)        { this.roomId = roomId; }
}
```

---

## 📄 FILE 6 — `model/SensorReading.java`

Path: `src/main/java/com/smartcampus/model/SensorReading.java`

```java
package com.smartcampus.model;

import java.util.UUID;

/**
 * Represents a single historical reading event captured by a sensor.
 */
public class SensorReading {

    private String id;          // UUID
    private long   timestamp;   // Epoch ms
    private double value;       // Measured value

    // ── Constructors ──────────────────────────────────────────────────────────

    public SensorReading() {}

    public SensorReading(double value) {
        this.id        = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.value     = value;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                      { return id; }
    public void   setId(String id)             { this.id = id; }

    public long   getTimestamp()               { return timestamp; }
    public void   setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getValue()                   { return value; }
    public void   setValue(double value)       { this.value = value; }
}
```

---

## 📄 FILE 7 — `model/ErrorResponse.java`

Path: `src/main/java/com/smartcampus/model/ErrorResponse.java`

```java
package com.smartcampus.model;

/**
 * Standard JSON error envelope returned by all exception mappers.
 * Ensures the API never leaks raw stack traces to clients.
 */
public class ErrorResponse {

    private int    status;
    private String error;
    private String message;
    private long   timestamp;

    public ErrorResponse(int status, String error, String message) {
        this.status    = status;
        this.error     = error;
        this.message   = message;
        this.timestamp = System.currentTimeMillis();
    }

    public int    getStatus()    { return status; }
    public String getError()     { return error; }
    public String getMessage()   { return message; }
    public long   getTimestamp() { return timestamp; }
}
```

---

## 📄 FILE 8 — `store/DataStore.java`

Path: `src/main/java/com/smartcampus/store/DataStore.java`

```java
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
```

---

## 📄 FILE 9 — `resource/DiscoveryResource.java`

Path: `src/main/java/com/smartcampus/resource/DiscoveryResource.java`

```java
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
```

---

## 📄 FILE 10 — `resource/RoomResource.java`

Path: `src/main/java/com/smartcampus/resource/RoomResource.java`

```java
package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Part 2 — Room Resource
 * Manages CRUD operations on the /api/v1/rooms collection.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    // ── GET /rooms ────────────────────────────────────────────────────────────

    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(store.getRooms().values());
        return Response.ok(roomList).build();
    }

    // ── POST /rooms ───────────────────────────────────────────────────────────

    @POST
    public Response createRoom(Room room) {
        // Validate required fields
        if (room == null || room.getId() == null || room.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", 400, "error", "Bad Request",
                                   "message", "Room 'id' field is required"))
                    .build();
        }
        if (room.getName() == null || room.getName().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", 400, "error", "Bad Request",
                                   "message", "Room 'name' field is required"))
                    .build();
        }
        // Check for duplicate ID
        if (store.getRooms().containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("status", 409, "error", "Conflict",
                                   "message", "Room with ID '" + room.getId() + "' already exists"))
                    .build();
        }
        // Ensure sensorIds is never null
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        store.getRooms().put(room.getId(), room);

        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    // ── GET /rooms/{roomId} ───────────────────────────────────────────────────

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                                   "message", "Room not found: " + roomId))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ── DELETE /rooms/{roomId} ────────────────────────────────────────────────

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);

        // 404 — room does not exist (idempotent: second call also returns 404)
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                                   "message", "Room not found: " + roomId))
                    .build();
        }

        // Business Logic Constraint — cannot delete if sensors are still assigned
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                    "Room '" + roomId + "' cannot be deleted. It has "
                    + room.getSensorIds().size()
                    + " active sensor(s) assigned. Reassign or remove sensors first."
            );
        }

        store.getRooms().remove(roomId);
        // 204 No Content — successful deletion with no body
        return Response.noContent().build();
    }
}
```

---

## 📄 FILE 11 — `resource/SensorResource.java`

Path: `src/main/java/com/smartcampus/resource/SensorResource.java`

```java
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
```

---

## 📄 FILE 12 — `resource/SensorReadingResource.java`

Path: `src/main/java/com/smartcampus/resource/SensorReadingResource.java`

```java
package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Part 4 — Sub-Resource for sensor reading history.
 * This class has NO @Path on the class itself — it is instantiated
 * and returned by the SensorResource sub-resource locator method.
 *
 * Effective paths:
 *   GET  /api/v1/sensors/{sensorId}/readings
 *   POST /api/v1/sensors/{sensorId}/readings
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String    sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ── GET readings ──────────────────────────────────────────────────────────

    @GET
    public Response getReadings() {
        // Verify sensor exists
        if (!store.getSensors().containsKey(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                                   "message", "Sensor not found: " + sensorId))
                    .build();
        }
        List<SensorReading> readings = store.getReadingsForSensor(sensorId);
        return Response.ok(readings).build();
    }

    // ── POST reading ──────────────────────────────────────────────────────────

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);

        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("status", 404, "error", "Not Found",
                                   "message", "Sensor not found: " + sensorId))
                    .build();
        }

        // State constraint: MAINTENANCE sensors cannot accept new readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor '" + sensorId + "' is under MAINTENANCE and cannot accept new readings."
            );
        }

        // Auto-generate ID if not provided
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        // Auto-set timestamp if not provided
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Persist the reading
        store.getReadingsForSensor(sensorId).add(reading);

        // *** SIDE EFFECT: update parent sensor's currentValue ***
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
```

---

## 📄 FILE 13 — `exception/RoomNotEmptyException.java`

Path: `src/main/java/com/smartcampus/exception/RoomNotEmptyException.java`

```java
package com.smartcampus.exception;

/**
 * Thrown when a DELETE is attempted on a Room that still has sensors.
 * Mapped to HTTP 409 Conflict by RoomNotEmptyExceptionMapper.
 */
public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String message) {
        super(message);
    }
}
```

---

## 📄 FILE 14 — `exception/RoomNotEmptyExceptionMapper.java`

Path: `src/main/java/com/smartcampus/exception/RoomNotEmptyExceptionMapper.java`

```java
package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps RoomNotEmptyException → HTTP 409 Conflict with JSON body.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        ErrorResponse error = new ErrorResponse(
                409,
                "Conflict",
                exception.getMessage()
        );
        return Response
                .status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
```

---

## 📄 FILE 15 — `exception/LinkedResourceNotFoundException.java`

Path: `src/main/java/com/smartcampus/exception/LinkedResourceNotFoundException.java`

```java
package com.smartcampus.exception;

/**
 * Thrown when a POST sensor references a roomId that does not exist.
 * Mapped to HTTP 422 Unprocessable Entity.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
```

---

## 📄 FILE 16 — `exception/LinkedResourceNotFoundExceptionMapper.java`

Path: `src/main/java/com/smartcampus/exception/LinkedResourceNotFoundExceptionMapper.java`

```java
package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps LinkedResourceNotFoundException → HTTP 422 Unprocessable Entity.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        ErrorResponse error = new ErrorResponse(
                422,
                "Unprocessable Entity",
                exception.getMessage()
        );
        return Response
                .status(422) // JAX-RS Status enum does not include 422, use int directly
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
```

---

## 📄 FILE 17 — `exception/SensorUnavailableException.java`

Path: `src/main/java/com/smartcampus/exception/SensorUnavailableException.java`

```java
package com.smartcampus.exception;

/**
 * Thrown when a POST reading is attempted on a MAINTENANCE sensor.
 * Mapped to HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
```

---

## 📄 FILE 18 — `exception/SensorUnavailableExceptionMapper.java`

Path: `src/main/java/com/smartcampus/exception/SensorUnavailableExceptionMapper.java`

```java
package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps SensorUnavailableException → HTTP 403 Forbidden.
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        ErrorResponse error = new ErrorResponse(
                403,
                "Forbidden",
                exception.getMessage()
        );
        return Response
                .status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
```

---

## 📄 FILE 19 — `exception/GlobalExceptionMapper.java`

Path: `src/main/java/com/smartcampus/exception/GlobalExceptionMapper.java`

```java
package com.smartcampus.exception;

import com.smartcampus.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 — Global Safety Net.
 *
 * Catches ALL uncaught Throwable (including NullPointerException,
 * IndexOutOfBoundsException, etc.) and returns a sanitised HTTP 500.
 *
 * CRITICAL: Never expose the stack trace to the client. Log it server-side only.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER =
            Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable throwable) {
        // Log the full stack trace server-side for debugging
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by global mapper", throwable);

        // Return a generic, non-leaking message to the client
        ErrorResponse error = new ErrorResponse(
                500,
                "Internal Server Error",
                "An unexpected error occurred. Please contact the system administrator."
        );
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
```

---

## 📄 FILE 20 — `filter/LoggingFilter.java`

Path: `src/main/java/com/smartcampus/filter/LoggingFilter.java`

```java
package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 — API Observability Filter.
 *
 * Implements BOTH ContainerRequestFilter (logs incoming) and
 * ContainerResponseFilter (logs outgoing) in a single class.
 *
 * @Provider ensures Jersey auto-registers this via package scanning.
 */
@Provider
public class LoggingFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER =
            Logger.getLogger(LoggingFilter.class.getName());

    /** Logs every incoming HTTP request. */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
                "[REQUEST ] --> %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()
        ));
    }

    /** Logs every outgoing HTTP response with its status code. */
    @Override
    public void filter(ContainerRequestContext  requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
                "[RESPONSE] <-- %s %s | HTTP %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()
        ));
    }
}
```

---

## 🔨 Step 3 — Build & Run

### Build the project
```bash
# Inside the project root (where pom.xml is)
mvn clean package -DskipTests
```
This creates `target/smart-campus-api-1.0-SNAPSHOT.jar` — a single fat JAR with all dependencies.

### Run the server
```bash
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

The server starts on **http://localhost:8080/api/v1**  
Press **ENTER** to stop.

---

## 🧪 Step 4 — Sample curl Commands (5 required)

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/api/v1 \
     -H "Accept: application/json" | python3 -m json.tool
```

### 2. Get all rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms \
     -H "Accept: application/json" | python3 -m json.tool
```

### 3. Create a new room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
           "id": "SCI-201",
           "name": "Science Lab 201",
           "capacity": 40
         }' | python3 -m json.tool
```

### 4. Create a sensor (valid roomId)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
           "id": "TEMP-002",
           "type": "Temperature",
           "status": "ACTIVE",
           "currentValue": 19.5,
           "roomId": "SCI-201"
         }' | python3 -m json.tool
```

### 5. Post a reading to a sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-002/readings \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{"value": 21.3}' | python3 -m json.tool
```

### 6. Get sensor reading history
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Accept: application/json" | python3 -m json.tool
```

### 7. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2" \
     -H "Accept: application/json" | python3 -m json.tool
```

### 8. Try deleting a room with sensors (expect 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 \
     -H "Accept: application/json" | python3 -m json.tool
```

### 9. Try creating sensor with invalid roomId (expect 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
           "id": "BAD-001",
           "type": "Temperature",
           "roomId": "NONEXISTENT-ROOM"
         }' | python3 -m json.tool
```

### 10. Try posting reading to a MAINTENANCE sensor (expect 403)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{"value": 15}' | python3 -m json.tool
```

---

## 📋 Complete API Reference

| Method | Path | Description | Success Code |
|--------|------|-------------|-------------|
| GET | `/api/v1` | Discovery / HATEOAS entry point | 200 |
| GET | `/api/v1/rooms` | List all rooms | 200 |
| POST | `/api/v1/rooms` | Create a room | 201 |
| GET | `/api/v1/rooms/{roomId}` | Get room by ID | 200 |
| DELETE | `/api/v1/rooms/{roomId}` | Delete room (fails if sensors exist) | 204 |
| GET | `/api/v1/sensors` | List all sensors (optional `?type=`) | 200 |
| POST | `/api/v1/sensors` | Register sensor (validates roomId) | 201 |
| GET | `/api/v1/sensors/{sensorId}` | Get sensor by ID | 200 |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get reading history | 200 |
| POST | `/api/v1/sensors/{sensorId}/readings` | Append new reading | 201 |

---

## 📝 Report — Answers to Questions

### Part 1 — Q1: Default JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of every resource class for each incoming HTTP request** (per-request scope). This means the runtime does not treat resource classes as singletons.

**Impact on shared state:** Since a new resource instance is created per request, any instance-level fields (e.g., a `HashMap` on the resource class) would be reset for every request and would never actually persist data. To maintain state across requests you must use an external, shared store. In this project the `DataStore` singleton pattern is used. Because multiple threads may handle concurrent requests, all maps use `ConcurrentHashMap` which provides atomic, thread-safe get/put/remove operations without needing explicit `synchronized` blocks. This prevents race conditions such as two concurrent POSTs both inserting the same ID.

---

### Part 1 — Q2: Why is HATEOAS a Hallmark of Advanced RESTful Design?

HATEOAS (Hypermedia as the Engine of Application State) means the server includes hyperlinks in every response pointing to related resources and valid transitions. For example, a room response might include `"_links": { "sensors": "/api/v1/rooms/LIB-301/sensors", "delete": "/api/v1/rooms/LIB-301" }`.

**Benefits over static documentation:**
- **Discoverability:** A client only needs to know the entry point (`/api/v1`). It can navigate the entire API dynamically from links in responses, rather than having to memorise or hard-code every URL pattern.
- **Decoupling:** If the server changes a URL path, only the response links need updating. Clients that follow links rather than hard-coding paths are not broken.
- **Self-documenting:** The current valid actions for a resource state are embedded directly in the response, reducing the chance of clients attempting invalid operations.

---

### Part 2 — Q1: IDs-only vs Full Objects in List Responses

| Approach | Bandwidth | Client Work | Use Case |
|---|---|---|---|
| Return IDs only | Very low | Client must make N follow-up GET calls | Huge collections where most items won't be used |
| Return full objects | Higher | Client has everything in one call | Normal-sized collections or when details are always needed |

For this campus API, returning full Room objects in the list is appropriate. The collection size is manageable, and clients (facilities managers, dashboards) nearly always need the name, capacity, and sensorIds immediately. Returning only IDs would force clients to make separate GET requests per room, multiplying network round-trips and latency.

---

### Part 2 — Q2: Is DELETE Idempotent in This Implementation?

**Partially idempotent.** REST defines idempotency as: calling the same request multiple times produces the same server state. Deleting a non-existent room does not change server state on the second call. However, the **HTTP response code differs**: the first successful DELETE returns `204 No Content`, while a subsequent DELETE on the same ID returns `404 Not Found`.

In this implementation: the first `DELETE /api/v1/rooms/LAB-102` removes the room and returns 204. The second identical request finds no room and returns 404. The server state after both calls is identical (room is absent), which satisfies the idempotency invariant. The differing status codes are acceptable and widely considered correct REST behaviour.

---

### Part 3 — Q1: Consequences of Missing `@Consumes(APPLICATION_JSON)`

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells the JAX-RS runtime that this endpoint only accepts requests whose `Content-Type` header is `application/json`. If a client sends `Content-Type: text/plain` or `Content-Type: application/xml`, Jersey will respond with **HTTP 415 Unsupported Media Type** before the method is even invoked. No custom code is needed. This provides a strong contract at the framework level: malformed or wrongly-typed requests are rejected automatically, protecting the method from receiving data that Jackson cannot deserialise.

---

### Part 3 — Q2: `@QueryParam` vs Path Segment for Filtering

| Design | Example | Semantics |
|---|---|---|
| Query Parameter | `/api/v1/sensors?type=CO2` | Filtering a collection |
| Path Segment | `/api/v1/sensors/type/CO2` | Implies CO2 is a unique, addressable resource |

Query parameters are semantically correct for filtering because they narrow a collection rather than address a distinct resource. A path segment like `/sensors/type/CO2` implies there is a stable resource identified by the path, which is incorrect for a filter. Additionally, query parameters are optional by nature — if `?type=` is absent, the full list is returned. Multiple filters can also be combined (`?type=CO2&status=ACTIVE`) without complicating the path. Query parameters are the standard REST convention for search/filter operations as documented in RFC 3986.

---

### Part 4 — Q1: Architectural Benefits of Sub-Resource Locator Pattern

The Sub-Resource Locator pattern lets a parent resource class (`SensorResource`) delegate all processing for a sub-path (`/{sensorId}/readings`) to a dedicated class (`SensorReadingResource`). Benefits include:

1. **Separation of concerns:** Each class has a single responsibility. `SensorResource` manages sensor CRUD; `SensorReadingResource` manages reading history.
2. **Reduced class size:** A monolithic resource handling every nested path would quickly become hundreds of lines. Delegation keeps each class focused and testable.
3. **Reusability:** `SensorReadingResource` can receive the `sensorId` as a constructor argument, allowing it to contextualise its logic without knowing its URL.
4. **Maintainability:** Adding a new nested path (e.g., `/sensors/{id}/alerts`) only requires creating a new resource class and one locator method — the existing classes are untouched.

---

### Part 5 — Q1: Why HTTP 422 over 404 for a Missing Reference in a Payload?

- **404 Not Found** signals that the *requested URL resource* does not exist.
- **422 Unprocessable Entity** signals that the request was syntactically valid JSON (parseable), correctly structured, and targeted the right URL — but contains a semantic error (a field references a resource that doesn't exist).

When a client POSTs `{ "roomId": "GHOST-99" }`, the URL `/api/v1/sensors` is perfectly valid (404 does not apply). The JSON is syntactically correct (400 does not apply). The problem is the *value* of `roomId` — it points to a non-existent room. HTTP 422 precisely communicates "I understood your request but cannot process it because the content is semantically invalid." This gives clients a clear signal to fix the payload value rather than the URL.

---

### Part 5 — Q4: Risks of Exposing Stack Traces to API Consumers

From a cybersecurity perspective, exposing raw Java stack traces is dangerous because they reveal:

1. **Internal package structure:** Class names like `com.smartcampus.store.DataStore` tell attackers the exact package layout, helping them craft targeted attacks.
2. **Framework and library versions:** Stack traces often include library names and versions (e.g., `jersey-server-2.40.jar`). Attackers can look up known CVEs for those exact versions.
3. **Business logic flow:** The call stack reveals which methods were invoked in what order, exposing internal processing logic that should remain opaque.
4. **File and line numbers:** These help attackers understand the codebase structure and identify vulnerable lines.
5. **Database/query details:** If an ORM is used, stack traces can expose table names, query structures, or connection details.

The `GlobalExceptionMapper` mitigates this by logging the full trace server-side (for developers) while returning only a generic "unexpected error occurred" message to the client.

---

### Part 5 — Q5: Why Use JAX-RS Filters for Cross-Cutting Concerns like Logging?

JAX-RS filters (`ContainerRequestFilter` / `ContainerResponseFilter`) apply **automatically to every request/response** without any changes to resource methods. The alternative — manually inserting `Logger.info()` into every resource method — has serious drawbacks:

1. **DRY Violation:** Logging code is duplicated across dozens of methods. Any change (log format, log level, adding a request ID) requires editing every file.
2. **Developer error-prone:** A developer writing a new endpoint can simply forget to add logging.
3. **Separation of concerns:** Logging is a cross-cutting concern (infrastructure) and should not mix with business logic (domain operations).
4. **Consistency:** Filters guarantee uniform log entries for every request. Manual insertion can miss some paths (e.g., error paths that return early).
5. **Easy augmentation:** Adding a correlation ID, authentication check, or rate-limit counter to every request only requires modifying the single filter class.

---

## 🎥 Video Demonstration Checklist

For the 10-minute Postman video, demonstrate **each part in order**:

- [ ] **Part 1:** GET `/api/v1` — show the discovery response with links
- [ ] **Part 2:** GET all rooms → POST new room → GET by ID → attempt DELETE with sensors (409) → DELETE empty room (204)
- [ ] **Part 3:** GET all sensors → GET with `?type=Temperature` filter → POST sensor with valid roomId → POST sensor with invalid roomId (422)
- [ ] **Part 4:** POST a reading to TEMP-001 → GET reading history → verify `currentValue` updated on sensor → POST reading to OCC-001 (403 MAINTENANCE)
- [ ] **Part 5:** Show server console logs for request/response filter output

**Remember:** Face camera, speak clearly, keep it under 10 minutes.

---

## 🐙 GitHub Setup

```bash
# Initialise git in project root
git init
git add .
git commit -m "feat: initial Smart Campus API implementation"

# Create repo on github.com, then:
git remote add origin https://github.com/YOUR_USERNAME/smart-campus-api.git
git branch -M main
git push -u origin main
```

Add a `.gitignore`:
```
target/
*.class
.idea/
*.iml
.DS_Store
```

---

## ✅ Marking Breakdown Summary

| Part | Topic | Marks | Coding (50%) | Video (30%) | Report (20%) |
|------|-------|-------|-------------|-------------|--------------|
| 1 | Setup + Discovery | 10 | 5 | 3 | 2 |
| 2 | Room Management | 20 | 10 | 6 | 4 |
| 3 | Sensor + Filtering | 20 | 10 | 6 | 4 |
| 4 | Sub-Resources | 20 | 10 | 6 | 4 |
| 5 | Error Handling + Logging | 30 | 15 | 9 | 6 |
| **Total** | | **100** | **50** | **30** | **20** |

---

## ⚠️ Critical Reminders

- ✅ **JAX-RS only** — No Spring Boot (instant zero)
- ✅ **No database** — ConcurrentHashMap and ArrayList only
- ✅ **GitHub repo** — No ZIP submissions (instant zero)
- ✅ **Public repo** with README.md
- ✅ **PDF report** with question answers only
- ✅ **Video** on Blackboard — be on camera, speak clearly
- ✅ **Deadline:** 24 April 2026, 13:00
