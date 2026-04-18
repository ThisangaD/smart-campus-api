# 🏫 Smart Campus – Sensor & Room Management API

![Java Version](https://img.shields.io/badge/Java-11%2B-blue?logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-C71A36?logo=apache-maven&logoColor=white)
![JAX-RS](https://img.shields.io/badge/JAX--RS-Jersey-brightgreen)
![Grizzly](https://img.shields.io/badge/Server-Grizzly-orange)

A RESTful Web API for managing university campus rooms and IoT sensors. This application tracks basic room occupancy, environmental measurements (like Temperature and CO2), and alerts for disconnected sensors.

Designed as part of the **5COSC022W Client-Server Architectures** coursework.

---

## 🛠️ Tech Stack

- **Java 11:** Core programming language.
- **JAX-RS (Jersey 2.40):** Framework used for creating RESTful Spring services.
- **Grizzly 2 HTTP Server:** Embedded server to host our JAX-RS application.
- **Jackson:** JSON provider for object serialization and deserialization.
- **Maven:** Build and dependency management tool.

---

## ✨ Features

- **HATEOAS Discovery Endpoint:** Navigate the API starting from the root directory.
- **Rooms Management:** Add, retrieve, and delete rooms.
- **Sensors Management:** Register new IoT sensors to specific rooms.
- **Sensor Readings:** Track real-time metric readings (Temperature, Humidity, OCC) to sensors.
- **Global Error Handling:** Clean and uniform JSON exception messages preventing stack trace exposure.
- **Thread-safe Datastore:** High performance concurrent collections handle all states memory-persistently without dropping requests.

---

## ⚙️ Prerequisites

Before you begin, ensure you have met the following requirements:

- **Java Development Kit (JDK):** Version 11 or higher. Validate with `java -version`.
- **Apache Maven:** Version 3.8 or higher. Validate with `mvn -version`.

---

## 🚀 Installation & Build

1. **Clone the repository:**
   ```bash
   git clone <repository_url>
   cd smart-campus-api
   ```

2. **Clean and construct the single executable (fat) JAR:**
   ```bash
   mvn clean package
   ```
   *This compiles classes, processes dependencies, and uses the Maven Shade Plugin to emit `smart-campus-api-1.0-SNAPSHOT.jar` inside the `target/` directory.*

---

## 🏃 Running the Application

Once built, start the embedded Grizzly server:

```bash
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

The server will start up quickly and output:
```text
  Smart Campus API is running!
  Base URI : http://0.0.0.0:8080/api/v1/
  Discovery: http://localhost:8080/api/v1
```
*(Press `ENTER` in the terminal to stop the service).*

---

## 📡 API Endpoints Summary

All routes are prefixed with `/api/v1`.

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **GET** | `/api/v1` | **Discovery Endpoint**. Returns API metadata and HATEOAS navigational links. |
| **GET** | `/api/v1/rooms` | Retrieve all registered rooms. |
| **POST** | `/api/v1/rooms` | Create a new room. Requires `id`, `name`, `capacity`. |
| **GET** | `/api/v1/rooms/{id}` | Retrieve a specific room by its ID. |
| **DELETE**| `/api/v1/rooms/{id}` | Delete a specific room (Fails if sensors are still actively linked). |
| **GET** | `/api/v1/sensors` | Retrieve all sensors. Optional query: `?type=Temperature`. |
| **POST** | `/api/v1/sensors` | Register a new sensor. Requires `id`, `type`, `roomId`. |
| **GET** | `/api/v1/sensors/{id}` | Retrieve details for a specific sensor. |
| **GET** | `/api/v1/sensors/{id}/readings` | Get historical readings for a sensor. |
| **POST** | `/api/v1/sensors/{id}/readings` | Publish a new reading to the designated sensor. |

---

## 🏗️ Architecture Note

- **DataStore (`DataStore.java`):** An in-memory, thread-safe Singleton using `ConcurrentHashMap`. As Jersey processes incoming HTTP requests concurrently by creating individual resource instances, the thread safety ensures robust reliability avoiding generic race conditions.
- **Exception Mappers:** Various Exception models mapped specifically avoiding messy internal 500 dumps to users (ex: `LinkedResourceNotFoundException`, `GlobalExceptionMapper`).
