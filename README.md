# Smart Campus Sensor & Room Management API

A JAX-RS RESTful service built with Jersey 2.41 and an embedded Grizzly HTTP server.

---

## API Overview

| Base Path | Description |
|-----------|-------------|
| `GET  /api/v1` | Discovery / HATEOAS root |
| `GET  /api/v1/rooms` | List all rooms |
| `POST /api/v1/rooms` | Create a room |
| `GET  /api/v1/rooms/{id}` | Get room by ID |
| `DELETE /api/v1/rooms/{id}` | Delete room (blocked if sensors present) |
| `GET  /api/v1/sensors` | List sensors (optional `?type=`) |
| `POST /api/v1/sensors` | Register a sensor |
| `GET  /api/v1/sensors/{id}` | Get sensor by ID |
| `DELETE /api/v1/sensors/{id}` | Remove sensor |
| `GET  /api/v1/sensors/{id}/readings` | Get reading history |
| `POST /api/v1/sensors/{id}/readings` | Add a reading |

---

## How to Build & Run (NetBeans 18 + Tomcat)

**Prerequisites:** NetBeans 18, Java 11+, Apache Tomcat 9 or 10

### Step 1 — Open the project in NetBeans
1. Open NetBeans 18
2. Click **File → Open Project**
3. Navigate to and select the `smart-campus-api` folder
4. NetBeans detects it as a Maven project automatically

### Step 2 — Add Tomcat as a server
1. Go to **Tools → Servers → Add Server**
2. Select **Apache Tomcat or TomEE**
3. Browse to your Tomcat installation folder
4. Click **Finish**

### Step 3 — Set Tomcat as the project server
1. Right-click the project → **Properties**
2. Go to **Run** category
3. Set **Server** to your Tomcat instance
4. Set **Context Path** to `/smart-campus-api`

### Step 4 — Run
- Right-click the project → **Run** (or press F6)
- NetBeans will build the WAR and deploy it to Tomcat automatically

**Base URL:** `http://localhost:8080/smart-campus-api/api/v1`

---

## Sample curl Commands

```bash
# 1. Discovery endpoint
curl -X GET http://localhost:8080/smart-campus-api/api/v1

# 2. List all rooms
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms

# 3. Create a new room
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-01","name":"Main Hall","capacity":200}'

# 4. Register a new sensor linked to a room
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-002","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'

# 5. Filter sensors by type
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2"

# 6. Post a reading (updates sensor currentValue)
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.3}'

# 7. Get reading history for a sensor
curl -X GET http://localhost:8080/smart-campus-api/api/v1/sensors/TEMP-001/readings

# 8. Delete a room (fails with 409 if sensors still assigned)
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301

# 9. Attempt sensor on non-existent room (triggers 422)
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"X-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```

---

## Report: Answers to Specification Questions

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of each resource class for every incoming HTTP request** (per-request lifecycle). This means resource classes are stateless by design and should not store shared data in instance fields.

This architectural decision directly impacts how in-memory data is managed: because each request gets its own resource object, any data stored as an instance variable would be lost the moment the request ends. To share state across requests, all rooms, sensors, and readings are stored in **static `ConcurrentHashMap` fields** inside `DataStore`. `ConcurrentHashMap` is essential because multiple requests can execute concurrently, and a standard `HashMap` would suffer race conditions (lost updates, corrupted entries). `ConcurrentHashMap` provides thread-safe read and write operations without requiring explicit `synchronized` blocks.
Additionally, although JAX-RS supports a singleton lifecycle if explicitly configured, the default behavior remains per-request to ensure safer concurrent request handling.

---

### Part 1.2 — HATEOAS

**Hypermedia as the Engine of Application State (HATEOAS)** means that API responses include links to related resources and available actions, rather than forcing clients to construct URLs from documentation.
For example, a response for a room resource may include links such as "self", "sensors", and "delete", allowing clients to directly navigate or perform actions without constructing URLs manually.

Benefits over static documentation:
- Clients can **navigate the API dynamically** without hard-coding paths. If a URI changes, clients following links adapt automatically.
- Responses are **self-describing** — a client that receives a room object can immediately see the link to its sensors without consulting external docs.
- It **reduces coupling** between client and server. The server drives state transitions; clients just follow links.
- Onboarding is faster: a developer can start at `GET /api/v1` and explore the entire API tree from there.

---

### Part 2.1 — Full Objects vs IDs in List Responses

| Approach | Bandwidth | Client Work |
|----------|-----------|-------------|
| Return IDs only | Low — small payload | High — client must issue N follow-up GET requests |
| Return full objects | Higher — larger payload | None — all data available immediately |

Returning full objects is better for most use cases because it avoids the **N+1 request problem**: if there are 500 rooms and the client needs their names, returning only IDs would require 501 HTTP round-trips. Returning full objects costs more bandwidth but delivers a far better client experience and fewer API calls overall.

---

### Part 2.2 — Is DELETE Idempotent?

**Yes**, DELETE is idempotent by definition in the HTTP specification — multiple identical DELETE requests must produce the same server state as a single one.

In this implementation:
- **First DELETE** on a valid, empty room: removes it, returns `204 No Content`.
- **Second DELETE** on the same (now absent) room: returns `404 Not Found`.

The server state is identical after both calls (the room is gone), so idempotency holds. The response code differs between calls, but HTTP idempotency refers to **server state**, not response codes. This is correct and standard REST behaviour.

---

### Part 3.1 — @Consumes and Content-Type Mismatch

`@Consumes(MediaType.APPLICATION_JSON)` declares that the endpoint only accepts `application/json` request bodies. If a client sends `Content-Type: text/plain` or `application/xml`, JAX-RS automatically returns **HTTP 415 Unsupported Media Type** without ever invoking the resource method. The body is not parsed and the business logic is never reached. This protects the endpoint from malformed or unexpected data formats at the framework level. This validation occurs at the JAX-RS runtime level before the resource method is invoked, ensuring that invalid requests are rejected early.

---

### Part 3.2 — @QueryParam vs Path Segment for Filtering

| Design | Example | Nature |
|--------|---------|--------|
| Query parameter | `GET /sensors?type=CO2` | Optional filter on a collection |
| Path segment | `GET /sensors/type/CO2` | Implies a distinct sub-resource |

The query parameter approach is superior for filtering because:
- It is **optional** — `GET /sensors` and `GET /sensors?type=CO2` both address the same resource collection, just with different views.
- Path segments imply a **different resource identity**. `/sensors/type/CO2` suggests "CO2" is a specific resource, not a filter, which is semantically incorrect.
- Query params are the **REST convention** for searching, sorting, and filtering collections, making the API intuitive for any developer familiar with REST.
- Multiple filters compose naturally: `?type=CO2&status=ACTIVE`.

---

### Part 4.1 — Sub-Resource Locator Pattern Benefits

The sub-resource locator pattern (returning an instance of another class rather than handling the path directly) provides several architectural benefits:

- **Separation of concerns**: `SensorResource` manages sensor CRUD; `SensorReadingResource` manages reading history. Each class has one responsibility and is independently testable.
- **Avoids "God controller" classes**: without delegation, a single class would handle sensors, readings, filtering, error cases, and sub-paths — growing unmaintainably large.
- **Reusability**: `SensorReadingResource` could be reused or extended independently without modifying `SensorResource`.
- **Clarity**: developers reading the code immediately know which class handles which path segment, making onboarding and debugging faster. This approach also improves scalability, as new nested resources can be added without increasing the complexity of existing resource classes.

---

### Part 5.2 — Why HTTP 422 Over 404 for Missing roomId Reference

**404 Not Found** means the *requested URL* could not be found on the server — the resource identified by the URI does not exist.

**422 Unprocessable Entity** means the server understood the request content type and successfully parsed it, but the *semantic content* of the payload is invalid.

When a client POSTs a sensor with a `roomId` that does not exist, the URL `/api/v1/sensors` is perfectly valid (200 OK for GET). The problem is inside the JSON body — a reference that fails a business logic constraint. Using 404 would mislead the client into thinking the endpoint itself doesn't exist. 422 precisely communicates: "your request was received and parsed, but the data inside it is logically invalid." This makes error handling on the client side much clearer.

---

### Part 5.4 — Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a significant security risk:

1. **Technology fingerprinting**: Stack traces reveal exact class names, package names, and third-party library names and versions (e.g., `org.hibernate:5.6.2`). Attackers can look up known CVEs (Common Vulnerabilities and Exposures) for those exact versions.
2. **Internal architecture exposure**: Package and class names reveal system design, layer boundaries, and naming conventions, making it easier to craft targeted attacks.
3. **File system paths**: Stack traces often include absolute paths (e.g., `/home/app/src/...`), revealing server directory structure.
4. **Logic and flow disclosure**: Line numbers and method call chains show exactly which code paths are reachable, helping an attacker understand how to trigger edge cases or bypass validation.
5. **Information for social engineering**: Detailed internal errors can be used to craft convincing phishing or impersonation attacks against support staff.

The `GlobalExceptionMapper` replaces all of this with a single safe message while logging the full trace server-side only, where it can be reviewed by authorised developers.

---

### Part 5.5 — Why Filters Over Manual Logging in Resource Methods

Using JAX-RS filters for cross-cutting concerns like logging is superior to inserting `Logger.info()` calls in every resource method because:

- **DRY principle**: With 10+ resource methods, duplicating logging code in each one is error-prone. A filter handles all of them in one place.
- **Guaranteed execution**: Filters run even when exceptions are thrown and bypass resource methods — manual logging inside resource methods would be skipped in those cases.
- **Centralised configuration**: Changing log format, adding request IDs, or switching logging frameworks requires editing one class, not dozens.
- **Clean business logic**: Resource methods stay focused on their business purpose; infrastructure concerns (logging, authentication, CORS) live in filters.
- **Consistent coverage**: It is impossible to accidentally omit logging for a newly added endpoint — the filter covers it automatically.
