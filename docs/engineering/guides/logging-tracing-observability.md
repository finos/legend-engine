# Legend Engine: Logging, Tracing and Observability Guide

> **Version**: Legend Engine 4.x  
> **Last Updated**: March 2026  
> **Audience**: Platform engineers, extension contributors, and operators  
> **Related reading**: [Identity, Authentication & Traceability Guide](identity-authentication-guide.md), [Connection Management Guide](../../connection/connection-management.md)

---

## Table of Contents

1. [Overview](#1-overview)
2. [Structured Logging with `LogInfo`](#2-structured-logging-with-loginfo)
3. [`LoggingEventType` — Standardised Event Vocabulary](#3-loggingeventtype--standardised-event-vocabulary)
4. [Logging Best Practices for Contributors](#4-logging-best-practices-for-contributors)
5. [Distributed Tracing with OpenTracing](#5-distributed-tracing-with-opentracing)
6. [Distributed Tracing with OpenTelemetry (SQL Server)](#6-distributed-tracing-with-opentelemetry-sql-server)
7. [What Gets Traced — Standard Span Tags](#7-what-gets-traced--standard-span-tags)
8. [Error Handling — Coordinated Logging and Tracing](#8-error-handling--coordinated-logging-and-tracing)
9. [Metrics and Observability](#9-metrics-and-observability)
10. [Connection Pool Metrics](#10-connection-pool-metrics)
11. [Observability Best Practices for Contributors](#11-observability-best-practices-for-contributors)

---

## 1. Overview

Observability in Legend Engine is designed around a single discipline:

> **Every operation that touches user data must be attributable to a named user, in logs, traces, and metrics simultaneously.**

This is not just good practice — it is a direct consequence of Legend's identity propagation model. Because every request carries an `Identity` object from the HTTP boundary all the way to the database connection, the user's name is always available and is always recorded. Logs, spans, and metrics that omit the user name are considered incomplete.

Legend uses three complementary observability systems:

| System | Technology | Scope |
|---|---|---|
| **Structured logging** | SLF4J + `LogInfo` | All components; always includes username |
| **Distributed tracing** | OpenTracing (`GlobalTracer`) | Core engine, HTTP API, execution plan, connection acquisition |
| **Distributed tracing** | OpenTelemetry | SQL wire-protocol server (`legend-engine-xt-sql-postgres-server`) |
| **Metrics** | Prometheus (Dropwizard client) | HTTP operations, connection pools |
| **Metrics** | OpenTelemetry meters | SQL server sessions and query execution |

---

## 2. Structured Logging with `LogInfo`

### 2.1 The `LogInfo` Structure

All log statements in Legend Engine use the `LogInfo` class rather than raw string messages. `LogInfo` serialises to JSON, making every log line machine-parseable and dashboard-friendly:

```java
// org.finos.legend.engine.shared.core.operational.logs.LogInfo
public class LogInfo {
    public Date timeStamp;          // ISO-8601 timestamp: "yyyy-MM-dd HH:mm:ss.SSS"
    public DeploymentMode mode;     // PROD / UAT / DEV — injected from DeploymentStateAndVersions
    public String user;             // ALWAYS populated — the requesting user's name
    public String eventType;        // Controlled vocabulary from LoggingEventType / ILoggingEventType
    public String message;          // Human-readable free text
    public Object info;             // Arbitrary domain object — JSON-serialised automatically
    public double duration;         // Elapsed milliseconds (for STOP events)
    public Throwable t;             // Exception detail (for ERROR events)
}
```

`LogInfo` has multiple constructors covering the common combinations. The user name is always the first positional argument to enforce the invariant:

```java
// Core constructor signatures — user is always first
new LogInfo(String user, ILoggingEventType eventType)
new LogInfo(String user, ILoggingEventType eventType, String message)
new LogInfo(String user, ILoggingEventType eventType, double durationMs)
new LogInfo(String user, ILoggingEventType eventType, Object info)
new LogInfo(String user, ILoggingEventType eventType, Object info, double durationMs)
```

### 2.2 Usage Pattern — START / STOP / ERROR Triples

Every significant operation follows a consistent three-log pattern:

```java
// 1. START — before the operation begins
long start = System.currentTimeMillis();
LOGGER.info(new LogInfo(identity.getName(),
    LoggingEventType.EXECUTION_PLAN_EXEC_START, "").toString());

// ... do the work ...

// 2. STOP — on success, with duration
LOGGER.info(new LogInfo(identity.getName(),
    LoggingEventType.EXECUTE_INTERACTIVE_STOP,
    (double) System.currentTimeMillis() - start).toString());

// 3. ERROR — on failure (user name still present)
// (in a catch block)
LOGGER.error(new LogInfo(identity.getName(),
    LoggingEventType.EXECUTION_PLAN_EXEC_ERROR, exception).toString());
```

This pattern ensures that:

- Every operation has a measurable wall-clock duration
- Failed operations are distinguishable from missing data in dashboards
- The user's name appears in all three log records, enabling full audit trails

### 2.3 Structured `info` Payloads

The `info` field accepts any Java object and serialises it to JSON automatically. This should be used for rich domain context rather than string concatenation:

```java
// Good: structured payload
Map<String, Object> context = new HashMap<>();
context.put("planType", plan.getClass().getSimpleName());
context.put("storeCount", plan.getStoreCount());
LOGGER.info(new LogInfo(identity.getName(),
    LoggingEventType.PLAN_GENERATED, context).toString());

// Avoid: string concatenation loses structure
LOGGER.info("Plan generated for " + identity.getName() + " type=" + plan.getClass()); // ✗
```

---

## 3. `LoggingEventType` — Standardised Event Vocabulary

`LoggingEventType` is a Java enum that defines the controlled vocabulary for all event types. Every event follows the naming pattern `NOUN_VERB_PHASE` where phase is `START`, `STOP`, or `ERROR`:

```java
// legend-engine-shared-core/.../operational/logs/LoggingEventType.java

// Execution plan lifecycle
EXECUTION_PLAN_EXEC_START,  EXECUTION_PLAN_EXEC_STOP,  EXECUTION_PLAN_EXEC_ERROR,

// Interactive query execution
EXECUTE_INTERACTIVE_START,  EXECUTE_INTERACTIVE_STOP,  EXECUTE_INTERACTIVE_ERROR,

// Relational store execution
EXECUTION_RELATIONAL_START, EXECUTION_RELATIONAL_STOP,
EXECUTION_RELATIONAL_COMMIT, EXECUTION_RELATIONAL_ROLLBACK,

// Middle-tier authorisation
MIDDLETIER_INTERACTIVE_EXECUTION,

// Compilation
COMPILE_ERROR, PARSE_ERROR,

// Graph (model) lifecycle
GRAPH_START, GRAPH_INITIALIZED, GRAPH_PARSED, GRAPH_DOMAIN_BUILT,
GRAPH_MAPPINGS_BUILT, GRAPH_CONNECTIONS_AND_RUNTIMES_BUILT,
GRAPH_SERVICES_BUILT, GRAPH_STOP, GRAPH_ERROR,

// Code generation
GENERATE_JAVA_START,    GENERATE_JAVA_STOP,    GENERATE_JAVA_ERROR,
GENERATE_GRAPHQL_CODE_START, ...

// Service operations
SERVICE_FACADE_W_UPDATE_ACTIVE_FOR_PATTERN,
SERVICE_BROADCAST_UPDATE, ...
```

> **Deprecation note**: `LoggingEventType` is marked `@Deprecated`. New code should implement the `ILoggingEventType` interface directly and define event types as constants in the module that owns them, rather than adding to the central enum. All `LogInfo` constructors accept `ILoggingEventType`.

### 3.1 Adding New Event Types

For a new module, define event types as an enum implementing `ILoggingEventType`:

```java
// In your module
public enum MyModuleEvents implements ILoggingEventType {
    MY_OPERATION_START,
    MY_OPERATION_STOP,
    MY_OPERATION_ERROR;

    @Override
    public String name() { return this.name(); }
}
```

---

## 4. Logging Best Practices for Contributors

When adding new features to Legend Engine, follow these conventions:

### Rule 1: Always include the user name

Never log an operation without identifying who triggered it:

```java
// Correct
LOGGER.info(new LogInfo(identity.getName(), MyModuleEvents.MY_OPERATION_START).toString());

// Wrong — no user context
LOGGER.info("Starting my operation"); // ✗
```

### Rule 2: Use START / STOP / ERROR triples for every significant operation

A "significant operation" is anything that:

- Crosses a component boundary (HTTP, database, vault, external service)
- Has a duration worth measuring
- Can fail in a way that needs to be audited

### Rule 3: Use `LogInfo.info` for structured context, not string concatenation

### Rule 4: Mirror errors to the active trace span (see §8)

### Rule 5: Classify exceptions with `ExceptionCategory`

```java
// Security/credential events — filtered separately by security monitoring
throw new EngineException(message, ExceptionCategory.USER_CREDENTIALS_ERROR);

// Other system errors
throw new EngineException(message, ExceptionCategory.SERVER_EXECUTION_ERROR);
```

`USER_CREDENTIALS_ERROR` is the signal to SIEM and security monitoring tools that a credential-related failure occurred, enabling targeted alerting separate from general application errors.

### Rule 6: Log connection-level operations with pool identity

For connection-related logging, include both the user name and the pool/datasource identifier (see also [Connection Management Guide §13](../../connection/connection-management.md#13-connection-metrics-and-observability)):

```java
LOGGER.info("Get Connection as [{}] for datasource [{}]",
    identity.getName(), connectionKey.shortId());
```

---

## 5. Distributed Tracing with OpenTracing

### 5.1 Core Engine Tracing

The core execution pipeline uses the OpenTracing API via `io.opentracing.util.GlobalTracer`. Spans are created with try-with-resources `Scope` blocks to ensure they are always closed:

```java
// Pattern: wrap significant operations in a named span
try (Scope scope = GlobalTracer.get()
        .buildSpan("Manage Results").startActive(true)) {

    // Annotate the span with context
    scope.span().setTag("user", identity.getName());

    // ... do the work ...

    LOGGER.info(new LogInfo(identity.getName(),
        LoggingEventType.EXECUTION_PLAN_EXEC_STOP, "").toString());
    return ResultManager.manageResult(...);
}
```

Named spans in the core engine:

| Span Name | Source | Tags |
|---|---|---|
| `"Execute Plan"` | `ExecutePlan` | `user`, `planType` |
| `"Authorize Plan Execution"` | `ExecutePlan.authorizePlan` | `plan authorization` (full JSON) |
| `"Manage Results"` | `ExecutePlan`, `ResultManager` | |
| `"Get Connection"` | `DataSourceSpecification` | `Principal`, `DataSourceSpecification`, `Pool` |
| `"Create Pool"` | `DataSourceSpecification.buildDataSource` | `Pool` |

### 5.2 Request Body Tracing

`BodySpanDecorator` is a JAX-RS `ReaderInterceptor` that attaches the incoming request body to the active span. It is registered on the HTTP server automatically:

```java
// BodySpanDecorator.java
@Override
public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
    if (GlobalTracer.get().activeSpan() != null && is != null) {
        // Read and buffer up to MAX_LENGTH (10 KB) of the request body
        // Truncate with "(truncated)" marker if longer
        GlobalTracer.get().activeSpan().setTag("body", body);
        // Restore the stream so the original handler can still read it
        context.setInputStream(new SequenceInputStream(...));
    }
    return context.proceed();
}
```

This means every inbound HTTP request that carries an active trace will have its body (up to 10 KB) attached to the root span — useful for debugging query payloads without needing to reconstruct them from logs.

### 5.3 Trace Context Propagation over HTTP

When Legend Engine makes outbound HTTP calls to downstream services, trace context is injected into the request headers using `HttpRequestHeaderMap`, which bridges Apache HttpClient's header API with OpenTracing's `TextMap` propagation format:

```java
// Propagate active trace context to a downstream HTTP call
GlobalTracer.get().inject(
    GlobalTracer.get().activeSpan().context(),
    Format.Builtin.HTTP_HEADERS,
    new HttpRequestHeaderMap(httpRequest)   // writes X-B3-TraceId, X-B3-SpanId, etc.
);
```

This ensures that a single user query can be correlated across multiple Legend Engine components and any downstream services in a distributed trace viewer (Jaeger, Zipkin, etc.).

### 5.4 Authorization Decision Tracing

Authorization results are always attached to spans as structured JSON, making them available in the trace alongside logs:

```java
// ExecutePlan.java
try (Scope scope = GlobalTracer.get()
        .buildSpan("Authorize Plan Execution").startActive(true)) {
    String authJSON = executionAuthorization.toPrettyJSON();
    scope.span().setTag("plan authorization", authJSON);
}
// Also log it — same information, two systems
LOGGER.info(new LogInfo(identity.getName(),
    LoggingEventType.MIDDLETIER_INTERACTIVE_EXECUTION,
    "Middle tier authorization result = " + auth.toJSON()));
```

---

## 6. Distributed Tracing with OpenTelemetry (SQL Server)

The SQL wire-protocol server (`legend-engine-xt-sql-postgres-server`) uses the OpenTelemetry API rather than OpenTracing, reflecting a more recent implementation. The central utility class is `OpenTelemetryUtil`:

```java
// OpenTelemetryUtil.java
public class OpenTelemetryUtil {
    private static final String INSTRUMENT_NAME = "legend-sql-server";
    private static final OpenTelemetry OPEN_TELEMETRY = GlobalOpenTelemetry.get();

    // Metrics — see §10 for full list

    public static Tracer getTracer() {
        return OPEN_TELEMETRY.getTracer(INSTRUMENT_NAME);
    }

    // W3C TraceContext / B3 propagation from Postgres protocol headers
    public static TextMapPropagator getPropagators() {
        return OPEN_TELEMETRY.getPropagators().getTextMapPropagator();
    }
}
```

Trace context arriving via the Postgres wire protocol (e.g., from a JDBC driver that injects trace headers) is extracted using `getPropagators()`, allowing SQL queries to be correlated with the client-side traces that initiated them.

> **Note on dual tracing systems**: The coexistence of OpenTracing and OpenTelemetry is a migration artefact. The OpenTracing API is now in maintenance mode; new components should use OpenTelemetry. OpenTracing bridges to OTel are available if cross-system correlation is needed.

---

## 7. What Gets Traced — Standard Span Tags

The following tags are always present on spans that involve user data access, forming the minimum required for audit and debugging:

| Span Tag | Source Class | Value | Purpose |
|---|---|---|---|
| `Principal` | `DataSourceSpecification` | Alice's username | Ties the connection acquisition to a user |
| `DataSourceSpecification` | `DataSourceSpecification` | `toString()` of the spec | Identifies the target database |
| `Pool` | `DataSourceSpecification` | Pool name (encodes user + datasource) | Correlates with pool metrics |
| `body` | `BodySpanDecorator` | Truncated request body (≤10 KB) | Request payload for debugging |
| `plan authorization` | `ExecutePlan` | Full JSON authorization result | Security audit record |
| `error` | `ExceptionTool` | `true` | Marks error spans for filtering |
| `error.message` | `ExceptionTool` | Full error JSON | Error detail without needing logs |

---

## 8. Error Handling — Coordinated Logging and Tracing

`ExceptionTool` is the single point of truth for turning exceptions into HTTP responses. It always logs the error **and** marks the active trace span simultaneously:

```java
// ExceptionTool.java
private static Response manage(LoggingEventType eventType, String user,
    ExceptionError error, Response.Status status) {

    // 1. Log — goes to the log aggregator (Splunk, ELK, etc.)
    LOGGER.error(new LogInfo(user, eventType, error).toString());

    // 2. Trace — annotates the active span in the distributed trace
    Span activeSpan = GlobalTracer.get().activeSpan();
    if (activeSpan != null) {
        Tags.ERROR.set(activeSpan, true);           // marks span as errored
        activeSpan.setTag("error.message", text);   // full error JSON on the span
    }

    // 3. HTTP response
    return Response.status(status)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(error)
        .build();
}
```

This guarantees that every error is visible in **both** the log stream and the trace — an operator looking at either system can find the full context of a failure without needing to cross-reference the other.

---

## 9. Metrics and Observability

### 9.1 HTTP Operation Metrics (Prometheus via Dropwizard)

`ServerMetricsHandler` exposes Prometheus metrics for high-level HTTP operation tracking:

```java
// legend-engine-server-support-core/.../monitoring/ServerMetricsHandler.java
// Core counters and summaries
datapush_operations             // Counter: operations started
datapush_operations_completed   // Summary: duration histogram of successful operations
datapush_operations_redirected  // Summary: duration histogram of redirected operations
datapush_operations_errors      // Summary: duration histogram of errored operations
```

Extensions can register their own named metrics dynamically:

```java
// Increment a named counter (auto-created with prefix "datapush_")
ServerMetricsHandler.incrementCounter("my_feature_invocations");

// Record a named duration summary
ServerMetricsHandler.operationComplete(startNanos, endNanos, "my_feature_duration");
ServerMetricsHandler.operationError(startNanos, endNanos,    "my_feature_duration");
```

All Prometheus metrics are exposed at the standard `/metrics` endpoint for scraping by Prometheus or compatible systems.

### 9.2 OpenTelemetry Metrics (SQL Server)

The SQL wire-protocol server exposes fine-grained metrics via OpenTelemetry meters, all under the instrumentation name `legend-sql-server`:

**Session metrics:**

| Metric | Type | Description |
|---|---|---|
| `active_sessions` | UpDownCounter | Currently open Postgres sessions |
| `total_sessions` | Counter | Total sessions since server start |

**Query execution metrics:**

| Metric | Type | Description |
|---|---|---|
| `active_execute_request` | UpDownCounter | Queries currently being executed |
| `total_execute_requests` | Counter | Total execute requests |
| `total_success_execute_requests` | Counter | Successful executions |
| `total_failure_execute_requests` | Counter | Failed executions |
| `execute_requests_duration` | Histogram | Execution latency distribution |

**Metadata request metrics:**

| Metric | Type | Description |
|---|---|---|
| `active_metadata_requests` | UpDownCounter | In-flight metadata requests |
| `total_metadata_requests` | Counter | Total metadata requests |
| `total_success_metadata_requests` | Counter | Successful metadata requests |
| `total_failure_metadata_requests` | Counter | Failed metadata requests |
| `metadata_requests_duration` | Histogram | Metadata latency distribution |

---

## 10. Connection Pool Metrics

Connection pool metrics bridge the logging/tracing system with the pool lifecycle managed by `ConnectionStateManager`. These are described in detail in the [Connection Management Guide §13](../../connection/connection-management.md#13-connection-metrics-and-observability). For completeness, the key metrics are:

| Metric | Type | Label | Description |
|---|---|---|---|
| `active_connections` | Gauge | `poolName` | Connections currently checked out |
| `total_connections` | Gauge | `poolName` | Total connections in pool (active + idle) |
| `idle_connections` | Gauge | `poolName` | Connections waiting for a request |

The `poolName` label encodes the user's identity (e.g., `DBPool_postgres_myhost_5432_alice_...`), enabling per-user connection monitoring. Metrics are updated by the `ConnectionStateManager` housekeeper thread and removed when a pool is evicted.

---

## 11. Observability Best Practices for Contributors

A summary checklist for any Legend Engine contributor adding or modifying a component:

### Logging Checklist

- [ ] Use `LogInfo` for all log statements, not bare strings
- [ ] `identity.getName()` is the first argument to every `LogInfo`
- [ ] Every significant operation has START, STOP, and ERROR log events
- [ ] Exceptions use `ExceptionTool.exceptionManager(...)` on API boundaries (ensures both log + trace)
- [ ] New event types implement `ILoggingEventType` (not added to the deprecated `LoggingEventType` enum)
- [ ] Credential failures throw `EngineException` with `ExceptionCategory.USER_CREDENTIALS_ERROR`

### Tracing Checklist

- [ ] Long-running or cross-component operations are wrapped in `GlobalTracer.buildSpan(...).startActive(true)` blocks
- [ ] The `Principal` tag is set on any span involving a database connection
- [ ] Authorization decisions are recorded as span tags (not just logs)
- [ ] Outbound HTTP calls inject trace context via `HttpRequestHeaderMap`
- [ ] New components in the SQL server use `OpenTelemetryUtil.getTracer()` (not OpenTracing)

### Metrics Checklist

- [ ] New HTTP-facing operations use `ServerMetricsHandler.operationStart/Complete/Error`
- [ ] New connection-pool-adjacent code calls `MetricsHandler.setConnectionMetrics` on the housekeeper cycle
- [ ] New SQL-server-adjacent code uses `OpenTelemetryUtil` meters following the existing `active_/total_/duration` naming pattern

---

## Related Documentation and Source References

| Topic | Location |
|---|---|
| `LogInfo` and `ILoggingEventType` | `legend-engine-shared-core/.../operational/logs/` |
| `LoggingEventType` (deprecated enum) | `legend-engine-shared-core/.../operational/logs/LoggingEventType.java` |
| `ExceptionTool` (coordinated log + trace on error) | `legend-engine-shared-core/.../errorManagement/ExceptionTool.java` |
| `BodySpanDecorator` (request body on spans) | `legend-engine-server-http-server/.../core/BodySpanDecorator.java` |
| `HttpRequestHeaderMap` (trace context propagation) | `legend-engine-shared-core/.../opentracing/HttpRequestHeaderMap.java` |
| `ServerMetricsHandler` (Prometheus HTTP metrics) | `legend-engine-server-support-core/.../monitoring/ServerMetricsHandler.java` |
| `MetricsHandler` (Prometheus connection metrics) | `legend-engine-shared-core/.../operational/prometheus/MetricsHandler.java` |
| `OpenTelemetryUtil` (OTel for SQL server) | `legend-engine-xt-sql-postgres-server/.../utils/OpenTelemetryUtil.java` |
| Connection pool metrics detail | [Connection Management Guide §13](../../connection/connection-management.md#13-connection-metrics-and-observability) |
| Identity and how user names enter the system | [Identity, Authentication & Traceability Guide §2](identity-authentication-guide.md#2-identity-the-core-concept) |
