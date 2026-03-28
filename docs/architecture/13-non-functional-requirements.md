# 13 — Non-Functional Requirements

This document covers the non-functional aspects of Legend Engine: build infrastructure, code quality, security, performance, extensibility, testability, and observability.

---

## Build & CI

### Maven Multi-Module Build

Legend Engine uses a **Maven multi-module** structure with a root POM managing all versions centrally:

| Property | Value |
|----------|-------|
| Root POM size | ~4400 lines, ~200+ managed dependencies |
| Java source | JDK 11 (required to build) |
| Bytecode target | Java 8 |
| Maven version | 3.6+ required |
| Build command | `mvn install` |

### Key Build Plugins

| Plugin | Purpose |
|--------|---------|
| **ANTLR4** | Generates grammar parsers from `.g4` files |
| **legend-pure-maven-generation-par** | Packages Pure code into PAR (Pure Archive) artifacts |
| **legend-pure-maven-generation-java** | Generates Java from Pure code |
| **legend-pure-maven-generation-pct** | Generates PCT test infrastructure |
| **maven-checkstyle-plugin** | Enforces code style |
| **maven-dependency-plugin** | Validates dependency declarations |
| **maven-enforcer-plugin** | Enforces version convergence and dependency banning |
| **JaCoCo** | Code coverage reporting |

### CI/CD
- **GitHub Actions** — primary CI pipeline (`build.yml`)
- **SonarCloud** — static analysis (`sonar.projectKey=legend-engine`)
- **Reproducible builds** — `project.build.outputTimestamp` ensures deterministic outputs

### Build Performance Tips
- Use Maven parallelism (`-T 4`)
- IntelliJ: set `Thread Count` to 4+ and `Shared build process heap size` to 30000 MB
- Disable `Clear output directory on rebuild` in IntelliJ

---

## Code Quality

### Checkstyle

All Java code must conform to the project's checkstyle rules defined in `checkstyle.xml`. Violations fail the build at the `verify` phase.

### Dependency Analysis

The build enforces **strict dependency hygiene**:
- `failOnWarning: true` — unused or undeclared dependencies fail the build
- Ignoring false positives is done via explicit `ignoredUnusedDeclaredDependencies` and `ignoredNonTestScopedDependencies` entries in module POMs

### Dependency Convergence

The Maven Enforcer plugin ensures all transitive dependencies resolve to the same version (`dependencyConvergence`). This prevents classpath conflicts.

---

## Security

### Dependency Banning

The enforcer plugin **bans** known-problematic dependencies:

| Banned Dependency | Reason |
|-------------------|--------|
| `log4j:*` (compile/runtime) | CVE-2021-44228 and general log4j risks |
| `commons-logging` | Conflicts with SLF4J |
| `javax.mail` | Not needed, potential security surface |

Only specific, approved SLF4J and logging dependencies are allowed:
- `slf4j-api` (the API)
- `jul-to-slf4j`, `jcl-over-slf4j` (bridges)
- `reload4j` (replacement for log4j 1.x)

### Secret Management

- Secrets are **never inlined** in models or code
- All secrets reference a `CredentialVaultSecret` (properties file, environment variable, or custom vault)
- Connection credentials flow through the `CredentialProvider` → `CredentialBuilder` pipeline

### License

- Apache License, Version 2.0
- FINOS governance (Incubating project)
- `CODEOWNERS` file defines review requirements

---

## Extensibility

Legend Engine's architecture is designed for extensibility at every level:

| Concern | Extension Mechanism |
|---------|---------------------|
| New DSL syntax | Grammar extension (ANTLR4 + ServiceLoader) |
| New model types | Protocol extension + Compiler processor |
| New stores | Store extension (grammar + compiler + plan gen + plan exec) |
| New formats | External format extension contract |
| New deployment targets | Function activator extension |
| New query protocols | Query protocol translator |
| New auth mechanisms | Credential provider + intermediation rules |

All extensions use Java's `ServiceLoader` for discovery — no core code changes required.

---

## Testability

### Test Framework (`legend-engine-core-testable`)

A rich testing infrastructure:

| Module | Purpose |
|--------|---------|
| `legend-engine-test-framework` | Base test framework and utilities |
| `legend-engine-testable` | Testable contract for packageable elements |
| `legend-engine-test-runner-function` | Test runner for Pure functions |
| `legend-engine-test-runner-mapping` | Test runner for mappings |
| `legend-engine-test-runner-shared` | Shared test runner infrastructure |
| `legend-engine-test-server-shared` | Shared server test utilities |
| `legend-engine-test-mft` | Multi-Function Test framework |
| `legend-engine-execution-test-data-generation` | Test data generation |

### Test Types

| Type | Scope | Infrastructure |
|------|-------|---------------|
| **Unit tests** | Single module | JUnit 4/5, Mockito |
| **Integration tests** | Cross-module | Testcontainers (Docker) |
| **PCT tests** | Cross-database compatibility | Testcontainers + real databases |
| **MFT tests** | Composed function behavior | Pure test suites |
| **Service tests** | End-to-end service execution | Test data + assertions |

### Test Configuration

```xml
<surefire.vm.params>
  -XX:-MaxFDLimit 
  -XX:SoftRefLRUPolicyMSPerMB=1 
  -Duser.timezone=GMT
</surefire.vm.params>
```

- Timezone fixed to GMT for deterministic date/time tests
- File descriptor limits adjusted for database connection tests
- Soft reference policy tuned for memory management

---

## Performance

### Execution Performance
- **Connection pooling**: HikariCP for relational databases
- **Plan caching**: Execution plans can be serialized and reused
- **Streaming results**: Large result sets are streamed without full materialization
- **Column-oriented in-memory**: In-memory execution uses column-oriented data for efficiency

### Build Performance
- **Parallel builds**: Maven `-T` flag
- **Incremental compilation**: Only changed modules rebuild
- **PAR caching**: Pure archives are cached across builds

---

## Observability

### Tracing
- **OpenTracing** integration via `opentracing-contrib`
- **Zipkin** reporter for distributed tracing
- Traces span the full execution pipeline: API → compile → plan gen → plan exec → store

### Metrics
- **Prometheus** metrics exporter
- **Dropwizard Metrics** for server-level metrics (request counts, latencies)

### Logging
- **SLF4J** API with **Logback** implementation
- Structured logging via `logback-contrib` (JSON format)
- Strict logging dependency control (only approved SLF4J bindings)

---

## Version Management

### Protocol Versioning
- Protocol models are versioned (`v1`, `vX_X_X`)
- Supports backward compatibility for API clients
- `clientVersion` parameter in APIs selects protocol version

### Dependency Versioning
- All dependency versions are centrally managed in the root POM
- Legend-internal dependencies versioned together:
  - `legend.pure.version` = `5.80.0`
  - `legend.shared.version` = `0.33.0`
- External dependencies pinned to exact versions

### Build Versioning
- Current version: `4.123.3-SNAPSHOT`
- Git commit info embedded via `git-commit-id-maven-plugin`
- Build timestamp recorded for traceability

---

## Key Takeaways for Re-Engineering

1. **The root POM is the source of truth**: All versions, enforcer rules, and build configuration live in the root `pom.xml`.
2. **Dependency hygiene is enforced**: Unused/undeclared dependencies and version conflicts fail the build.
3. **Security is baked in**: Banned dependencies, externalized secrets, and explicit credential flows.
4. **Testing is multi-layered**: Unit → Integration → PCT → MFT → Service tests provide confidence at every level.
5. **Observability is built-in**: Tracing, metrics, and structured logging are wired through the core platform.
