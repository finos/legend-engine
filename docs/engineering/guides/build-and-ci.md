# Build & CI Guide

## 1. Full Maven Build Lifecycle

The build is driven by the root `pom.xml` at `legend-engine/pom.xml`. The module reactor
order is determined by the `<modules>` declaration and inter-module dependency graph.

```mermaid
flowchart TD
    Pure["legend-pure\n(external dependency — pre-built JARs from Maven Central)"]
    Core["legend-engine-core\n(compiler, grammar, plan gen/exec, Pure runtime)"]
    Xts["legend-engine-xts-*\n(store / format / DSL extensions, parallel)"]
    Config["legend-engine-config\n(server assembly, REPL)"]
    Query["legend-engine-application-query\n(query app backend)"]

    Pure --> Core --> Xts --> Config --> Query
```

### Build commands

```bash
# Fast build — skip tests, 4 parallel threads
mvn install -DskipTests -T 4

# Full build with tests
mvn install -T 4

# Build a single module and all its dependencies
mvn install -pl <module-path> -am -DskipTests

# Build with verbose output for debugging
mvn install -DskipTests -e -X 2>&1 | head -200
```

### Maven properties that affect the build

| Property | Default | Effect |
|----------|---------|--------|
| `skipTests` | `false` | Skip test execution (sources still compiled) |
| `maven.compiler.source` / `.target` | `1.8` | Compiler source/target level (enforced as Java 8 bytecode) |
| `maven.compiler.release` | `8` | Combined source+target+bootclasspath |
| `maven.enforcer.requireJavaVersion` | `[11.0.10,12)` | Enforces JDK 11 at build time |
| `surefire.vm.params` | see root POM | JVM args for test forks: timezone, soft-ref LRU policy |
| `dependencies.failOnWarning` | `true` | `maven-dependency-plugin:analyze` fails on unused declared deps |

---

## 2. Key Maven Plugins

| Plugin | Version property | Purpose |
|--------|-----------------|---------|
| `maven-compiler-plugin` | `maven.compiler.plugin.version` | Java compilation (source/target 8, runtime JDK 11) |
| `maven-surefire-plugin` | `maven.surefire.plugin.version` | Test execution; configured with JVM params |
| `maven-checkstyle-plugin` | `maven.checkstyle.plugin.version` | Enforces `checkstyle.xml` rules on `.java`, `.xml`, `.pure` files |
| `maven-enforcer-plugin` | `maven.enforcer.plugin.version` | Enforces JDK 11, Maven 3.6.2+, no banned dependencies |
| `maven-dependency-plugin` | `maven.dependency.plugin.version` | Dependency analysis; fails on unused declared / used undeclared deps |
| `jacoco-maven-plugin` | `jacoco.maven.plugin.version` | Code coverage instrumentation and reporting |
| `maven-shade-plugin` | `maven.shade.plugin.version` | Assembles fat JARs for the server and REPL |
| `maven-source-plugin` | `maven.source.plugin.version` | Attaches source JARs for publishing |
| `maven-javadoc-plugin` | `maven.javadoc.plugin.version` | Generates and attaches Javadoc JARs |
| `antlr4-maven-plugin` | (via `antlr.version`) | Generates lexer/parser Java from `.g4` grammar files |
| `build-helper-maven-plugin` | `build-helper.maven.plugin.version` | Adds extra source/resource directories |
| `depgraph-maven-plugin` | `depgraph-maven-plugin.version` | Generates module dependency graphs (DOT format) |
| `exec-maven-plugin` | `exec.maven.plugin.version` | Runs external executables (e.g. npm build steps) |
| `google.maven.download.plugin` | `google.maven.download.plugin.version` | Downloads external artifacts during build |

### Generating a dependency graph

```bash
# Generate DOT graph for the entire project
mvn com.github.ferstl:depgraph-maven-plugin:graph \
  -DshowGroupIds=true -DshowVersions=true \
  -DgraphFormat=dot

# Output: target/dependency-graph.dot
# Render: dot -Tpng target/dependency-graph.dot -o deps.png
```

---

## 3. Maven Profiles

| Profile | Activation | Purpose |
|---------|-----------|---------|
| `docker-snapshot` | Manual (`-P docker-snapshot`) | Builds and pushes Docker snapshot image to Docker Hub. Used on `master` branch CI. |
| `pct-cloud-test` | Manual (`-P pct-cloud-test`) | Activates PCT tests that require cloud credentials (Snowflake, BigQuery, etc.). Only runs in CI when secrets are available. |
| `integration-test` | Manual (`-P integration-test`) | Activates Testcontainers-based integration tests requiring Docker. |

---

## 4. ANTLR4 Grammar Build

Grammar files (`.g4`) live in `src/main/antlr4/` of grammar modules. The
`antlr4-maven-plugin` runs during `generate-sources` and writes generated Java to
`target/generated-sources/antlr4/`.

> **legend-pure build pipeline:** `legend-engine`'s build is downstream of the `legend-pure`
> plugin pipeline. The Pure-specific build steps — PAR file generation, Java code-gen from Pure,
> and PCT report generation — are driven by `legend-pure` Maven plugins that run *before* the
> ANTLR4 step. See the
> [legend-pure Maven Plugins Reference](https://github.com/finos/legend-pure/blob/main/docs/reference/maven-plugins-reference.md)
> for the full goal/parameter reference for `legend-pure-maven-generation-par`,
> `-generation-java`, and the PCT plugin. `legend-engine` does **not** re-compile core Pure
> at build time; it consumes the pre-compiled PAR/JAR outputs that `legend-pure` publishes
> to Maven Central.

**Key grammar modules:**

- `legend-engine-language-pure-grammar` — core domain, mapping, runtime, connection grammars
- Each `xts-*` module with a `-grammar` sub-module adds its own `.g4` files

**Adding a new grammar section:**

1. Create `.g4` files in `src/main/antlr4/`.
2. Write a `SectionParser` that calls the ANTLR parser and produces protocol POJOs.
3. Register via `PureGrammarParserExtension` in `META-INF/services/`.
4. Write a `SectionComposer` and register via `PureGrammarComposerExtension`.
5. Add a round-trip test (`grammar → JSON → grammar`).

---

## 5. GitHub Actions CI Pipeline

The CI pipeline is defined in `.github/workflows/build.yml`.

### Workflow triggers

- `push` to `master`
- `pull_request` targeting `master`

Concurrent runs on the same PR branch are cancelled automatically.

### Pipeline stages

```mermaid
flowchart TD
    Push["Push / PR"]
    Build["build job\n1. Cache Maven repository\n2. Set up JDK 11 (Zulu distribution)\n3. Download all dependencies offline\n4. mvn install -DskipTests [-P docker-snapshot,pct-cloud-test]\n5. Upload target/** + ~/.m2/repository to artifact store\n6. Compute test matrix from modulesToTest.json"]
    Tests["test jobs (one per matrix entry)\n1. Restore artifact store from build job\n2. mvn test for the modules in this matrix slice\n3. Upload test reports"]
    Results["test-result job\nAggregates JUnit XML reports from all test jobs"]

    Push --> Build
    Build -->|"matrix fan-out (parallel jobs)"| Tests
    Tests --> Results
```

**On `master` only:** the `build` job also runs with `-P docker-snapshot` to build and
push a Docker snapshot image to Docker Hub under `finos/legend-engine`.

### Test matrix

The matrix is defined in `.github/workflows/resources/modulesToTest.json`. Each entry
lists the Maven module name(s) to test in that job. Key groups:

| Group | What it covers |
|-------|---------------|
| `server` | Full server integration tests |
| `core` | Pure compiled core, function extensions |
| `javaBinding` | Java platform binding PCT |
| `sql` | SQL grammar, pure, HTTP API, reverse-PCT |
| `relational` | Relational store execution, H2 dialect |
| `relationalDialects` | Postgres, Snowflake, BigQuery, DuckDB, Oracle, etc. |
| `graphQL` | GraphQL compiler, pure, HTTP API |
| `service` | Service DSL, execution, test runner |
| `persistence` | Persistence DSL and test runner |
| `authentication` | Authentication grammar and implementation |
| `analytics` | Analytics APIs |
| `changeToken` | Change-token compiler and tests |

### Adding a new module to CI testing

1. Add an entry to `.github/workflows/resources/modulesToTest.json`.
2. If the module needs cloud credentials (PCT cloud tests), add it under the
   `pct-cloud-test` profile block.

---

## 6. Docker Images

The `legend-engine-config/legend-engine-server` module produces a Docker image via the
`jib-maven-plugin` (no Dockerfile, no Docker daemon needed to build/push). The image:

- Base: `eclipse-temurin:11.0.17_8-jdk-jammy` (`<from><image>` in the module pom)
- Server listens on port `6300`
- Entrypoint: `java <jvmFlags> -cp ... org.finos.legend.engine.server.Server server /config/config.json`
- `src/main/resources/docker/config/` is baked in at `/config/`

All three profiles bind to `install`: `docker-snapshot` pushes the `snapshot` tag,
`docker` pushes `${project.version}`, and `docker-local` builds to the local Docker daemon
instead of pushing. The versioned push therefore happens both in `release.yml`'s
"Build Release Tag" step and in the `release:perform` workflows.

There is no `-jar`-able artifact for this module — the image is the distribution.

`legend-engine-pure-ide-light-http-server` still uses the older `dockerfile-maven-plugin`
plus a checked-in `Dockerfile`.

**Building locally:**

```bash
# push the snapshot tag (needs DOCKER_USERNAME / DOCKER_PASSWORD)
mvn install -P docker-snapshot -pl legend-engine-config/legend-engine-server/legend-engine-server-http-server -am

# or build into the local Docker daemon, no registry credentials required
mvn install -P docker-local -pl legend-engine-config/legend-engine-server/legend-engine-server-http-server -am
docker run -p 6300:6300 legend-engine-server-http-server
```

On Apple Silicon, `docker-local` fails with *"configured platforms don't match the Docker
Engine's OS and architecture"* — jib targets `linux/amd64` by default and refuses to load
a foreign-arch image into the local engine. Add `-Djib.from.platforms=linux/arm64` for
local builds. Do not set this in the pom: it would change the manifest of the published
image. `docker`/`docker-snapshot` push to a registry and are unaffected.

The baked `/config/config.json` is templated with environment variables (`ENGINE_PORT`,
`MONGODB_URI`, GitLab OAuth), so it will not boot as-is. To run the image standalone,
override the arguments with a self-contained config:

```bash
docker run -p 6300:6300 \
  -v "$PWD/legend-engine-config/legend-engine-server/legend-engine-server-http-server/src/test/resources/org/finos/legend/engine/server/test:/testconfig:ro" \
  legend-engine-server-http-server server /testconfig/userTestConfig.json
```

**Vulnerability scanning:** `.github/workflows/docker.yml` runs weekly (and on demand)
and scans the *published* `:snapshot` tags of all three images with Trivy. Results are
uploaded to the repository's Security tab as code-scanning alerts rather than failing the
run, so container CVEs are triaged alongside Dependabot alerts. It deliberately has no
`push`/`pull_request` triggers — `:snapshot` is republished on every master merge, so it
already tracks the code.

Note that scan coverage depends on the image layout: an uber-jar hides most dependency
identities from Trivy's Java scanner. When this module still shipped a shaded jar, Trivy
resolved 875 dependencies in the image; with jib's exploded layout it resolves 1083.

---

## 7. Release Process

Releases are managed by the `maven-release-plugin` via `.github/workflows/release.yml`
(triggered manually or on tag push).

Key steps:

1. `mvn release:prepare` — bumps version, creates tag, commits `[maven-release-plugin]` commit.
2. `mvn release:perform` — builds from tag, deploys to Maven Central (via OSSRH / Sonatype).
3. Docker image is tagged and pushed with the release version.

**Version format:** `MAJOR.MINOR.PATCH-SNAPSHOT` → `MAJOR.MINOR.PATCH` on release.
Current series: `4.x.x`.

Legend stack release coordination (across `legend-pure`, `legend-engine`, `legend-sdlc`,
`legend-studio`) is managed via `.github/workflows/legend-stack-release.yml`.
