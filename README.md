# 🏫 Smart Campus — Sensor & Room Management API

> **Module:** 5COSC022W Client-Server Architectures  
> **Stack:** Java 11 · JAX-RS (Jersey 2.40) · Apache Tomcat · Jackson · Maven

---

## 📖 API Design Overview

This RESTful API manages the university's Smart Campus infrastructure. It follows a resource-based hierarchy:

```
/api/v1                          ← Discovery / HATEOAS entry point
/api/v1/rooms                    ← Room collection
/api/v1/rooms/{roomId}           ← Individual room
/api/v1/sensors                  ← Sensor collection (filterable by ?type=)
/api/v1/sensors/{sensorId}       ← Individual sensor
/api/v1/sensors/{sensorId}/readings  ← Sub-resource: reading history
```

**Design principles applied:**
- Versioned base path (`/api/v1`) via `@ApplicationPath`
- Resource nesting reflects the physical campus structure
- ConcurrentHashMap in-memory store (no database) for thread-safe shared state
- Custom exception mappers ensure every error returns structured JSON — never a stack trace
- HATEOAS links embedded in the discovery endpoint for API navigability
- JAX-RS filters provide request/response logging as a cross-cutting concern

---

## 🗂️ Project Structure

```
smart-campus-api/
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/smartcampus/
│       │   ├── SmartCampusApplication.java        ← @ApplicationPath("/api/v1")
│       │   ├── model/
│       │   │   ├── Room.java
│       │   │   ├── Sensor.java
│       │   │   ├── SensorReading.java
│       │   │   └── ErrorResponse.java
│       │   ├── store/
│       │   │   └── DataStore.java                 ← Singleton ConcurrentHashMap store
│       │   ├── resource/
│       │   │   ├── DiscoveryResource.java         ← GET /api/v1
│       │   │   ├── RoomResource.java              ← GET/POST/DELETE /api/v1/rooms
│       │   │   ├── SensorResource.java            ← GET/POST /api/v1/sensors + sub-resource locator
│       │   │   └── SensorReadingResource.java     ← GET/POST /api/v1/sensors/{id}/readings
│       │   ├── exception/
│       │   │   ├── RoomNotEmptyException.java           → 409 Conflict
│       │   │   ├── RoomNotEmptyExceptionMapper.java
│       │   │   ├── LinkedResourceNotFoundException.java → 422 Unprocessable Entity
│       │   │   ├── LinkedResourceNotFoundExceptionMapper.java
│       │   │   ├── SensorUnavailableException.java      → 403 Forbidden
│       │   │   ├── SensorUnavailableExceptionMapper.java
│       │   │   └── GlobalExceptionMapper.java           → 500 Internal Server Error (catch-all)
│       │   └── filter/
│       │       └── LoggingFilter.java             ← Request + Response logging filter
│       └── webapp/
│           └── WEB-INF/
│               └── web.xml                        ← Servlet configuration (Tomcat deployment descriptpr)
```

---

## 🛠️ How to Build and Deploy

### Prerequisites
| Tool | Version |
|------|---------|
| Java JDK | 11 or higher |
| Apache Maven | 3.8 or higher |
| Apache Tomcat | 9.0 or higher |

Verify installations:
```bash
java -version
mvn -version
```

### Step 1 — Clone the repository
```bash
git clone https://github.com/ThisangaD/smart-campus-api.git
cd smart-campus-api
```

### Step 2 — Build the WAR file
```bash
mvn clean package -DskipTests
```
This creates `target/smart-campus-api-1.0-SNAPSHOT.war` optimized for Tomcat deployment.

### Step 3 — Deploy to Tomcat

#### Option A: Manual Copy
1. Copy the WAR file to your Tomcat installation:
   ```bash
   cp target/smart-campus-api-1.0-SNAPSHOT.war $CATALINA_HOME/webapps/
   ```
   Or on Windows:
   ```bash
   copy target\smart-campus-api-1.0-SNAPSHOT.war %CATALINA_HOME%\webapps\
   ```

2. Start Tomcat:
   ```bash
   $CATALINA_HOME/bin/startup.sh     # Linux/macOS
   %CATALINA_HOME%\bin\startup.bat   # Windows
   ```
   Tomcat automatically deploys the WAR file.

#### Option B: Using Tomcat Manager (Web UI)
1. Start Tomcat if not already running
2. Navigate to `http://localhost:8080/manager`
3. Use "Deploy" section to upload the WAR file

### Step 4 — Verify Deployment
Once Tomcat starts, the API will be available at:
```
http://localhost:8080/smart-campus-api-1.0-SNAPSHOT/api/v1
```

Or if deployed with a custom context name:
```
http://localhost:8080/api/v1
```

The Discovery endpoint should return:
```json
{
  "api": "Smart Campus Sensor & Room Management API",
  "version": "v1",
  "contact": "admin@smartcampus.ac.uk",
  "description": "RESTful API for managing university campus rooms and IoT sensors",
  "_links": {
    "self": "/api/v1",
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  },
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

### Step 5 — Stop Tomcat
```bash
$CATALINA_HOME/bin/shutdown.sh      # Linux/macOS
%CATALINA_HOME%\bin\shutdown.bat    # Windows
```

---

## 🧪 Sample curl Commands

> **Note:** Replace `http://localhost:8080/api/v1` with your actual deployment URL.  
> If deployed as `ROOT.war`, the URL is `http://localhost:8080/api/v1`.  
> If deployed as `smart-campus-api-1.0-SNAPSHOT.war`, the URL is `http://localhost:8080/smart-campus-api-1.0-SNAPSHOT/api/v1`.

### 1. Discovery Endpoint (HATEOAS)
```bash
curl -X GET http://localhost:8080/api/v1 \
     -H "Accept: application/json"
```
Expected: `200 OK` with API metadata and `_links` map.

---

### 2. Create a New Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
           "id": "SCI-201",
           "name": "Science Lab 201",
           "capacity": 40
         }'
```
Expected: `201 Created` with the new room object.

---

### 3. Get All Sensors Filtered by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2" \
     -H "Accept: application/json"
```
Expected: `200 OK` with a list containing only CO2-type sensors.

---

### 4. Post a Sensor Reading (with side effect on currentValue)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{"value": 25.8}'
```
Expected: `201 Created`. After this, `GET /api/v1/sensors/TEMP-001` will show `"currentValue": 25.8`.

---

### 5. Attempt to Delete a Room That Has Sensors (409 Error)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 \
     -H "Accept: application/json"
```
Expected: `409 Conflict` — room still has sensors assigned.

---

### 6. Register a Sensor With an Invalid roomId (422 Error)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{
           "id": "BAD-001",
           "type": "Temperature",
           "roomId": "DOES-NOT-EXIST"
         }'
```
Expected: `422 Unprocessable Entity` — referenced room does not exist.

---

### 7. Post Reading to a MAINTENANCE Sensor (403 Error)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
     -H "Content-Type: application/json" \
     -H "Accept: application/json" \
     -d '{"value": 15}'
```
Expected: `403 Forbidden` — sensor OCC-001 is under MAINTENANCE.

---

### 8. Get Sensor Reading History
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings \
     -H "Accept: application/json"
```
Expected: `200 OK` with array of reading objects.

---

### 9. Get All Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms \
     -H "Accept: application/json"
```
Expected: `200 OK` with list of 3 seeded rooms.

---

### 10. Delete an Empty Room Successfully
```bash
# First create a room with no sensors
curl -X POST http://localhost:8080/api/v1/rooms \
     -H "Content-Type: application/json" \
     -d '{"id": "TEMP-ROOM", "name": "Temp Room", "capacity": 10}'

# Then delete it
curl -X DELETE http://localhost:8080/api/v1/rooms/TEMP-ROOM
```
Expected: `204 No Content`.

---

## 📋 Complete API Reference

| Method | Path | Description | Status |
|--------|------|-------------|--------|
| GET | `/api/v1` | HATEOAS Discovery | 200 |
| GET | `/api/v1/rooms` | List all rooms | 200 |
| POST | `/api/v1/rooms` | Create a room | 201 |
| GET | `/api/v1/rooms/{roomId}` | Get room by ID | 200 / 404 |
| DELETE | `/api/v1/rooms/{roomId}` | Delete room (blocked if sensors exist) | 204 / 404 / 409 |
| GET | `/api/v1/sensors` | List all sensors (optional `?type=`) | 200 |
| POST | `/api/v1/sensors` | Register sensor (validates roomId) | 201 / 422 |
| GET | `/api/v1/sensors/{sensorId}` | Get sensor by ID | 200 / 404 |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get reading history | 200 / 404 |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add new reading | 201 / 403 / 404 |

---

## 📍 REST Best Practices — Location Headers

When creating new resources (`POST` methods that return `201 Created`), the API includes a `Location` header in the response. This header contains the absolute URI of the newly created resource, allowing clients to immediately fetch or reference the resource without having to construct the URL manually.

**Example:**
```bash
POST /api/v1/rooms HTTP/1.1
Content-Type: application/json

{"id": "NEW-001", "name": "New Room", "capacity": 20}
```

**Response:**
```
HTTP/1.1 201 Created
Content-Type: application/json
Location: http://localhost:8080/api/v1/rooms/NEW-001

{"id": "NEW-001", "name": "New Room", "capacity": 20, "sensorIds": []}
```

Clients can extract the `Location` header and follow it to retrieve the resource, or use it in subsequent requests. This is documented in [RFC 7231, Section 7.1.2](https://tools.ietf.org/html/rfc7231#section-7.1.2) as a standard REST practice for resource creation responses.

---

## ❓ Report — Answers to Questions

### Part 1, Q1 — JAX-RS Resource Lifecycle & Thread Safety

By default, JAX-RS creates a **new instance of every resource class for each incoming HTTP request** (per-request scope). This means the JAX-RS runtime does not treat resource classes as singletons — a fresh object is instantiated for every call, then discarded.

**Impact on shared state:** Because a new instance is created per request, instance-level fields are reset every time. Storing data in an instance field would mean data is lost after each request. To share and persist data across requests, an external singleton is required. This project uses `DataStore` — a static eagerly-initialised singleton. Since multiple threads may be handling concurrent requests simultaneously, all maps in `DataStore` use `ConcurrentHashMap`. This provides atomic, thread-safe `get`, `put`, and `remove` operations without explicit `synchronized` blocks, preventing race conditions such as two concurrent POST requests both inserting the same sensor ID and corrupting the collection.

---

### Part 1, Q2 — Why HATEOAS Is a Hallmark of Advanced RESTful Design

HATEOAS (Hypermedia as the Engine of Application State) means the server embeds hyperlinks inside every response, pointing to related resources and valid next actions. For example, the discovery endpoint at `/api/v1` returns `"_links": { "rooms": "/api/v1/rooms", "sensors": "/api/v1/sensors" }`.

**Benefits over static documentation:**
- **Single entry point:** A client only needs to know the base URL. It can navigate the entire API dynamically by following links in responses, rather than memorising every endpoint from external docs.
- **Decoupling:** If server-side URL structures change, only the embedded links in responses need updating. Clients that follow links rather than hard-coding paths are not broken by URL changes.
- **Self-documenting runtime behaviour:** The set of links present in a response indicates exactly which actions are currently valid for that resource state, reducing the chance of clients attempting invalid operations.
- **Reduced coupling to documentation:** Static docs go stale. Hypermedia links are generated live from the server, always reflecting the current API structure.

---

### Part 2, Q1 — IDs Only vs Full Objects in List Responses

When returning a collection of rooms, two approaches exist:

**Returning only IDs** (`["LIB-301", "LAB-102", "HALL-01"]`): Very low bandwidth, but forces clients to make N additional GET requests to retrieve details for each room. For a list of 100 rooms this means 100 extra HTTP calls, adding significant latency and server load.

**Returning full objects** (chosen approach): Slightly higher payload size but the client receives everything it needs in a single round-trip. For typical campus management dashboards that immediately display room names, capacities, and sensor counts, this eliminates redundant follow-up requests. For this API's expected usage patterns and collection sizes, full objects provide a significantly better developer experience with minimal bandwidth cost.

---

### Part 2, Q2 — Is DELETE Idempotent?

**Yes, in terms of server state** — though the HTTP response codes differ between calls. REST defines idempotency as: making the same request multiple times produces the same server state.

In this implementation:
- **First `DELETE /api/v1/rooms/HALL-01`** (assuming it has no sensors): removes the room, returns `204 No Content`.
- **Second identical `DELETE /api/v1/rooms/HALL-01`**: the room is already gone, so the response is `404 Not Found`.

The server state is identical after both calls (the room is absent), satisfying idempotency. The different status codes (204 vs 404) are acceptable and widely considered correct REST behaviour — a 404 on a repeated DELETE communicates "the resource you wanted gone is indeed gone."

---

### Part 3, Q1 — Consequences of `@Consumes(APPLICATION_JSON)` Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells the JAX-RS runtime to only accept requests whose `Content-Type` header is `application/json`. If a client sends `Content-Type: text/plain` or `Content-Type: application/xml`, Jersey responds with **HTTP 415 Unsupported Media Type** before the resource method is even invoked — no custom code is required. This provides a strong content-type contract enforced at the framework level: malformed or wrongly-typed requests are rejected automatically, protecting the resource method from receiving data that Jackson cannot deserialise into the target POJO.

---

### Part 3, Q2 — `@QueryParam` vs Path Segment for Filtering

| Design | Example | Semantics |
|---|---|---|
| Query Parameter | `/api/v1/sensors?type=CO2` | Filters a collection |
| Path Segment | `/api/v1/sensors/type/CO2` | Implies CO2 is a uniquely addressable resource |

Query parameters are semantically correct for filtering because they narrow down a collection rather than address a distinct resource. A path like `/sensors/type/CO2` incorrectly implies there is a stable resource at that URL, which is not the case — CO2 is a filter criterion, not a resource identifier.

Additionally, query parameters are naturally optional: omitting `?type=` returns the full list with no code changes required. Multiple filters can be combined (`?type=CO2&status=ACTIVE`) without complicating the URL path structure. This is the standard REST convention for search and filter operations as established by RFC 3986, and is universally understood by client developers.

---

### Part 4, Q1 — Architectural Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern allows `SensorResource` to delegate all processing for `/{sensorId}/readings` to a dedicated `SensorReadingResource` class by returning an instance of it from a method annotated only with `@Path` (no HTTP method annotation). Benefits include:

1. **Separation of concerns:** Each class has a single responsibility. `SensorResource` handles sensor CRUD; `SensorReadingResource` handles reading history. Neither class needs to know about the other's internal logic.
2. **Reduced class size and complexity:** A monolithic resource handling every nested endpoint would grow to hundreds of lines, making it hard to read, test, and maintain. Delegation keeps each file focused.
3. **Contextual instantiation:** `SensorReadingResource` receives `sensorId` as a constructor argument, allowing all its methods to contextualise their logic without parsing path parameters themselves.
4. **Scalability:** Adding new nested sub-resources (e.g., `/sensors/{id}/alerts`) requires only a new class and one additional locator method — existing classes are untouched, respecting the Open/Closed Principle.

---

### Part 5, Q1 — Why HTTP 422 Over 404 for a Missing Reference in a JSON Payload

- **404 Not Found** signals that the *requested URL resource* does not exist. It applies when the path itself is invalid.
- **422 Unprocessable Entity** signals that the request was syntactically valid JSON, correctly formed, and targeted a valid URL — but contains a *semantic* error inside the payload.

When a client POSTs `{ "roomId": "GHOST-99" }` to `/api/v1/sensors`, the URL `/api/v1/sensors` is perfectly valid (404 does not apply). The JSON parses correctly (400 does not apply). The problem is the *value* of `roomId` — it references a room that does not exist. HTTP 422 precisely communicates "I understood and parsed your request, but I cannot process it because the content is semantically invalid." This gives API consumers a clear, unambiguous signal to fix the payload value, not the URL structure.

---

### Part 5, Q2 — Cybersecurity Risks of Exposing Java Stack Traces

Exposing raw Java stack traces to external API consumers reveals information that can be exploited:

1. **Internal package structure:** Class names like `com.smartcampus.store.DataStore` expose the exact package layout, helping attackers understand the codebase and craft targeted exploits.
2. **Framework and library versions:** Stack traces often include library identifiers (e.g., `jersey-server-2.40`). Attackers can search CVE databases for known vulnerabilities in those exact versions.
3. **Business logic flow:** The call stack reveals which methods were invoked in what order, exposing internal processing logic that should remain opaque to external users.
4. **File and line numbers:** These reveal the codebase structure and can help attackers identify specific vulnerable lines to target.
5. **Data layer details:** If an ORM or query layer is involved, stack traces can expose table names, column names, or connection details.

The `GlobalExceptionMapper<Throwable>` mitigates all of these risks by logging the full stack trace server-side only (for developers) while returning a generic "An unexpected error occurred" message to the client — leaking nothing about the internal system.

---

### Part 5, Q3 — Why Use JAX-RS Filters Instead of Manual Logging in Every Method

JAX-RS filters (`ContainerRequestFilter` / `ContainerResponseFilter`) apply **automatically to every single request and response** without any modification to existing resource methods. The alternative — manually inserting `Logger.info()` into each endpoint — has serious disadvantages:

1. **DRY violation:** Logging code is duplicated across every resource method. Any change (log format, correlation ID, log level) requires editing every file.
2. **Human error:** A developer writing a new endpoint can simply forget to add the logging statement, creating silent gaps in observability.
3. **Separation of concerns:** Logging is a cross-cutting infrastructure concern and does not belong mixed with business domain logic.
4. **Guaranteed coverage:** Filters are invoked by the JAX-RS framework regardless of which endpoint is called or which exception path is taken — no code path is silently missed.
5. **Easy extension:** Adding rate-limit tracking, authentication checks, or a request correlation ID to every call requires changing only the single filter class, not every resource method throughout the codebase.
