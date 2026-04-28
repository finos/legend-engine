# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

Toolchain: **JDK 11** (Maven enforcer: `[11.0.10,12)`), **Maven 3.6.2+**. Full clean build is expensive (15–25 min); prefer `-T 4` and `-DskipTests` during iteration.

```bash
mvn clean install -DskipTests -T 4                 # fast first build
mvn clean install -T 4                             # full build with tests
mvn clean install -DskipTests -pl <module-path> -am  # build one module + its deps
mvn checkstyle:check                               # Checkstyle (blocking in CI)
```

Always pass `clean` — several Pure Maven plugins are buggy and fail with "duplicate artifact present" errors when building over a prior target directory.

Run the server (main: `org.finos.legend.engine.server.Server`):
```
server legend-engine-config/legend-engine-server/legend-engine-server-http-server/src/test/resources/org/finos/legend/engine/server/test/userTestConfig.json
```
Swagger: <http://127.0.0.1:6300/api/swagger>.

Pure IDE (main: `org.finos.legend.engine.ide.PureIDELight`) — required for iterating on `.pure` code without rebuilding Java. Args: `server legend-engine-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json`. UI at <http://127.0.0.1:9200/ide>.

IntelliJ gotcha: **disable** `Clear output directory on rebuild` under `Preferences → Build → Compiler`. It wipes generated Pure-runtime resources the server needs to start, producing `ClassNotFoundException` for generated classes.

## Tests

JUnit 5 for new tests (JUnit 4 exists but is legacy — don't add more). Typical patterns:
```bash
mvn test -pl <module-path> -Dtest=TestClassName
mvn test -pl <module-path> -Dtest="TestClassName#testMethodName"
mvn verify -Pintegration-test                # Docker-based (Testcontainers)
```

Class naming: `Test<Subject>` (preferred) or `<Subject>Test` (legacy), `Test<Subject>WithH2` for H2-backed integration, `PCT<Store>_<Dialect>_Test` for PCT variants.

### PCT (Pure Compatibility Tests)

PCT is how cross-store behavioural parity is enforced. Pure functions marked `<<PCT.test>>` run against every registered store. Default-profile stores (run on every PR): **H2, DuckDB, Java binding**. Cloud stores (`pct-cloud-test` profile, CI-only, requires secrets): Postgres, Snowflake, BigQuery, Databricks, Spanner, MemSQL.

Run a single dialect's PCT suite:
```bash
mvn test -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-h2/legend-engine-xt-relationalStore-h2-PCT
```

When a PCT test fails on a target, decide: real bug → fix; legitimately unsupported → add to that adapter's `expectedFailures` with an `AdapterQualifier` (`needsImplementation`, `unsupportedFeature`, `needsInvestigation`, `assertErrorMismatch`). See `docs/pct/expected-failures-howto.md`.

Detailed PCT authoring guides: `docs/pct/` (`purefunction-howto.md`, `native-howto.md`, `wiring-howto.md`, `conventions.md`). PCT framework itself is defined in `legend-pure` upstream.

## Architecture

Legend Engine is the execution backbone of the FINOS Legend platform: a Dropwizard HTTP server wrapping a Pure language runtime. Given a Pure query + Mapping + Runtime, it executes the five-stage pipeline:

1. **Grammar ↔ Protocol** — `POST /api/pure/v1/grammar/transformGrammarToJson` — ANTLR4 parsers ↔ `PureModelContextData` POJOs.
2. **Compilation** — `POST /api/pure/v1/compilation/compile` — builds a fully-typed `PureModel` (in-memory Pure graph) via multi-pass compiler.
3. **Plan generation** — `POST /api/pure/v1/executionPlan/generate` — `PlanGenerator` calls into Pure; `meta::pure::router::routeFunction` dispatches sub-expressions to `StoreContract`s; produces an `ExecutionPlan` tree; `PlanTransformer`s (e.g. `JavaPlatformBinder`) add platform-native nodes.
4. **Plan execution** — `POST /api/pure/v1/execution/execute` — `PlanExecutor` walks the tree; each node type goes to a registered `StoreExecutor`; results streamed as `Result`/`StreamingResult`.
5. **Service execution** — compiles + plans + executes a packaged Legend Service inline.

Production always runs **compiled mode** (Pure functions pre-translated to Java bytecode at build time — see `legend-engine-pure-runtime-java-extension-compiled-*`). The Pure IDE path uses interpreted mode.

### Module taxonomy (prefix-based)

| Prefix | Role |
|--------|------|
| `legend-engine-core` | Compiler, grammar, plan generation, plan execution, Pure runtime wiring |
| `legend-engine-xts-*` | Extension modules — stores, formats, DSLs, function activators |
| `legend-engine-config` | Server assembly, REPL, configuration |
| `legend-engine-application-query` | Saved-query backend |

Dependency direction: `config → xts-* → core → legend-pure → legend-shared`. Extensions depend only on core interfaces (one common exception: several `xts-*` modules reuse `xts-relationalStore` for their relational test harness).

Sub-module suffixes are consistent across `xts-*`: `-grammar`, `-protocol`, `-compiler`, `-pure`, `-execution`, `-http-api`. Use this convention when adding a new extension.

### Extension architecture — how to add stores/formats/DSLs

Two parallel SPI mechanisms, both required for a full extension:

- **Java side:** register SPI implementations via `java.util.ServiceLoader` (files under `META-INF/services/`): `CompilerExtension`, `StoreExecutorBuilder`, `PlanGeneratorExtension`, `PureGrammarParserExtension`.
- **Pure side:** register against `meta::pure::extension::Extension` — aggregates `availableStores` (StoreContracts), `availableExternalFormats`, `availableFeatures`, `availablePlatformBindings`. The `Extension` is threaded through all planning/execution functions as an explicit parameter.

When adding a new store, also register a `-PCT` module and add it to `.github/workflows/resources/modulesToTest.json` in the appropriate CI group.

### Key data structures

- `PureModelContextData` — JSON-serialisable model snapshot (protocol POJOs).
- `PureModel` — compiled in-memory Pure graph (wraps legend-pure's `ModelRepository`).
- `ExecutionPlan` (Pure) / `SingleExecutionPlan` (Java POJO) — tree of `ExecutionNode`s.
- `ExecutionState` — mutable bag threaded through the executor (variables, result caches).
- `Extension` (Pure) — registry of all active plug-ins; must be passed into most planning/execution calls.

### Protocol versioning

Protocol classes are versioned (`v1_24_0`, `v1_25_0`, …). Breaking changes require a new version sub-package, a transfer function from the old version, and an entry in `PureClientVersions`. Do **not** mutate an existing version's classes.

## Conventions to follow

- **Indentation:** Java 4 spaces, Pure 3 spaces, XML/JSON/YAML 4 spaces. No tabs anywhere (`.java`, `.xml`, `.pure` all checked).
- **Braces:** always required (including single-statement `if`). Opening brace on a new line; closing brace alone on its line.
- **Copyright header:** every new file (including `.pure`) needs the Apache 2.0 header — Checkstyle enforces this.
- **Logging:** SLF4J only — never `System.out.println`. For `INFO`-level operational events use `LogInfo` wrapper + `LoggingEventType` enum (in `legend-shared`); add new event types to the enum rather than free-text strings. Never log credentials/tokens, not even at DEBUG.
- **Errors:** user-facing errors throw `EngineException` with `SourceInformation` and an `EngineErrorType` (`COMPILATION`/`EXECUTION`/`PARSER`/`INTERNAL`). Don't swallow exceptions. Pure-level: `assert(cond, | 'msg')` or `fail('msg')` — the Java layer converts `PureException` → `EngineException`.
- **Grammar changes:** update both parser and composer, and add a round-trip test.
- **Tests:** use `JsonUnit.assertJsonEquals` for JSON comparison (not `String.equals`). Pure test resources go under `src/test/resources/` mirroring the production package.

## Further docs in this repo

- `docs/engineering/README.md` — full engineering documentation index (architecture deep-dives, module reference, guides).
- `docs/engineering/architecture/` — overview, domain concepts, key Java/Pure areas, execution plans, router/Pure-to-SQL, pre-evaluation, Alloy compiler.
- `docs/engineering/standards/coding-standards.md` — Checkstyle rules, naming, PR checklist.
- `docs/engineering/testing/testing-strategy.md` — test pyramid, frameworks, PCT, CI matrix.
- `docs/pct/` — PCT framework how-tos (Pure functions, native functions, wiring, expected failures, conventions, taxonomy).