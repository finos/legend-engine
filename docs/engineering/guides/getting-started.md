# Legend Engine — Developer Getting-Started Guide

> **Last updated:** March 2026
> **Audience:** Developers joining the project for the first time.

---

## 1. Prerequisites

| Tool | Required version | Notes |
|------|-----------------|-------|
| JDK | **11** (LTS) | Enforced by Maven enforcer: `[11.0.10,12)`. Download: [Adoptium](https://adoptium.net/) or `sdk install java 11.0.23-tem` (SDKMAN). |
| Maven | **3.6.2+** | Download: [maven.apache.org](https://maven.apache.org/download.cgi). |
| Git | Any recent | — |
| Docker | Optional, recommended | Required for Testcontainers-based integration tests (MongoDB, Postgres, etc.) and local Zipkin tracing. |
| IntelliJ IDEA | Recommended | Community edition works. |
| RAM | 16 GB minimum, 32 GB recommended | Full parallel build requires ~8 GB Maven heap. |

---

## 2. Clone and Initial Build

```bash
git clone https://github.com/finos/legend-engine.git
cd legend-engine

# Full build, skipping tests — expect 15-25 min first run
mvn install -DskipTests=true -T 4
```

> **Parallelism:** `-T 4` runs 4 Maven threads. In IntelliJ set `Thread Count` under
> `Preferences → Build → Maven` and raise `Shared build process heap size` to 30 000 MB
> under `Preferences → Build → Compiler → Java Compiler`.
> **⚠️ IntelliJ:** Disable `Clear output directory on rebuild`
> (`Preferences → Build → Compiler`). This option deletes generated resources the server needs.

---

## 3. Running the Server Locally

### 3.1 From IntelliJ IDEA

1. Create a **Run Configuration** → Application.
2. **Main class:** `org.finos.legend.engine.server.Server`
3. **Program arguments:**

   ```text
   server legend-engine-config/legend-engine-server/legend-engine-server-http-server/src/test/resources/org/finos/legend/engine/server/test/userTestConfig.json
   ```

4. **VM options:** `-Xmx4g`

### 3.2 From the Command Line

```bash
java -Xmx4g \
  -cp "legend-engine-config/legend-engine-server/legend-engine-server-http-server/target/legend-engine-server-http-server-*.jar" \
  org.finos.legend.engine.server.Server server \
  legend-engine-config/legend-engine-server/legend-engine-server-http-server/src/test/resources/org/finos/legend/engine/server/test/userTestConfig.json
```

### 3.3 Verify

- **Health check:** <http://127.0.0.1:6300/>
- **Swagger UI:** <http://127.0.0.1:6300/api/swagger>

---

## 4. Running the Pure IDE

The Pure IDE lets you interactively develop and test Pure code without recompiling the Java project.

**Main class:** `org.finos.legend.engine.ide.PureIDELight`
**Program arguments:**

```text
server legend-engine-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json
```

Access at: <http://127.0.0.1:9200/ide>

**Debugging Pure code:**

1. Insert `meta::pure::ide::debug()` to set a breakpoint in your Pure function.
2. Press F9 to run.
3. Use `debug <expression>`, `debug summary`, or `debug abort` in the IDE terminal.

---

## 5. Running the REPL

```bash
cd legend-engine-config/legend-engine-repl/legend-engine-repl-app-assembly
./assemble
```

See `legend-engine-config/legend-engine-repl/README.md` for full usage.

---

## 6. Configuration Files

| File | Purpose |
|------|---------|
| `legend-engine-server-http-server/src/test/resources/.../userTestConfig.json` | Local dev server config (H2, no auth, mock SDLC) |
| `ideLightConfig.json` | Pure IDE server config |
| `h2Server.sh` / `h2Console.sh` | Start H2 TCP server and web console for inspecting test data |

### Common environment variables (production configs)

| Variable | Purpose |
|----------|---------|
| `LEGEND_ENGINE_HOST` | Bind address |
| `LEGEND_ENGINE_PORT` | HTTP port (default 6300) |
| `SDLC_SERVER_HOST` | Legend SDLC server hostname |
| `VAULTS_*` | Credential vault connection parameters |

---

## 7. Building Specific Modules

```bash
# Build a single module (skip tests)
mvn install -DskipTests \
  -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-execution/legend-engine-xt-relationalStore-executionPlan

# Build a module and all its Maven dependencies (-am = also-make)
mvn install -DskipTests \
  -pl legend-engine-config/legend-engine-server/legend-engine-server-http-server -am
```

---

## 8. Running Tests

```bash
# All unit tests
mvn test

# One module
mvn test -pl <module-path>

# Single test class
mvn test -pl <module-path> -Dtest=TestClassName

# Integration tests (requires Docker)
mvn verify -P integration-test
```

See [Testing Strategy](../testing/testing-strategy.md) for full details.

---

## 9. Running MemSQL Tests on macOS Apple Silicon

Follow the [SingleStore Docker image instructions](https://github.com/singlestore-labs/singlestoredb-dev-image?tab=readme-ov-file#how-to-run-the-docker-image-on-apple-silicon-m1m2-chips).

---

## 10. Common Troubleshooting

### `OutOfMemoryError` during build

```bash
export MAVEN_OPTS="-Xmx8g -XX:+TieredCompilation"
```

### Server fails: `ClassNotFoundException` for generated classes

Generated Pure-runtime Java classes in `target/` are missing. Run `mvn install -DskipTests`
to regenerate them. Ensure `Clear output directory on rebuild` is **disabled** in IntelliJ.

### `Could not find artifact org.finos.legend.pure:...`

Run `mvn install` from the repo root (not a sub-module) to resolve the parent POM's
dependency management, or check your network access to Maven Central.

### Pure IDE blank or errors on startup

Verify `legend-engine-pure-code-compiled-core` was built successfully.

### Port 6300 already in use

```bash
lsof -ti:6300 | xargs kill -9
```

### Testcontainers fail: `Could not find a valid Docker environment`

Ensure Docker Desktop is running. On Linux: `sudo usermod -aG docker $USER` (then re-login).

---

## 11. IDE Recommendations

| Setting | Value |
|---------|-------|
| Java SDK | 11 |
| Maven | Use `.mvn/` wrapper |
| Code style | Import `checkstyle.xml` from repo root |
| Annotation processing | Enable |
| Build tool | IntelliJ's Maven integration |
