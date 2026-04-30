# Legend Engine — Coding Standards & Style Guide

> **Audience:** All contributors to `legend-engine`.  
> **Enforcement:** Checkstyle runs as part of `mvn verify`. PRs that fail Checkstyle are blocked.

---

## 1. Code Formatting

### Checkstyle

The project uses a **custom Checkstyle configuration** (`checkstyle.xml` in the repo root),
adapted from the Google Java Style Guide.

Key rules enforced:

| Rule | Detail |
|------|--------|
| **No tabs** | `FileTabCharacter` — spaces only, applied to `.java`, `.xml`, and `.pure` files |
| **Copyright header** | Every file must contain a valid Apache 2.0 copyright header (checked by `RegexpMultiline`) |
| **One top-level class per file** | `OneTopLevelClass` |
| **No line wrapping** | `NoLineWrap` — long lines are permitted; wrapping is not enforced to a column limit |
| **Braces required** | `NeedBraces` — all `if`, `else`, `for`, `while`, `do` blocks must have braces |
| **Opening brace on new line** | `LeftCurly option=nl` |
| **Closing brace alone on line** | `RightCurly option=alone` |
| **Empty blocks use TEXT** | Empty catch/finally/if/else/switch blocks must have a comment |

**Running Checkstyle locally:**

```bash
mvn checkstyle:check
```

**Suppressing a rule for a specific line** (use sparingly):

```java
// CHECKSTYLE.OFF: NoLineWrap
```

### Import ordering

The project does not enforce a specific import ordering tool (no `impsort-maven-plugin`), but
by convention:

1. `java.*` and `javax.*`
2. Third-party libraries (alphabetical by package)
3. `org.finos.legend.*`

Do not use wildcard imports (`import foo.bar.*;`).

### Indentation

- **Java:** 4 spaces per level.
- **Pure:** 3 spaces per level (convention in the existing codebase).
- **XML / JSON / YAML:** 4 spaces per level.

---

## 2. Naming Conventions

### Java

| Element | Convention | Example |
|---------|-----------|---------|
| Packages | Lowercase, dot-separated, mirror module path | `org.finos.legend.engine.plan.execution.stores.relational` |
| Classes | `UpperCamelCase` | `RelationalExecutor`, `PlanGenerator` |
| Interfaces | `UpperCamelCase`, no `I` prefix | `StoreExecutor`, `CompilerExtension` |
| Methods | `lowerCamelCase` | `generateExecutionPlan`, `buildConnection` |
| Constants | `UPPER_SNAKE_CASE` | `DEFAULT_DB_TIME_ZONE`, `USER_ID` |
| Fields | `lowerCamelCase` | `planExecutorInfo`, `isJavaCompilationAllowed` |
| Test classes | `Test<ClassUnderTest>` or `<ClassUnderTest>Test` | `TestRelationalExecutor`, `PlanGeneratorTest` |
| Test methods | `test<Scenario>` or descriptive sentence | `testExecuteSimpleQuery`, `shouldThrowWhenMappingNotFound` |

### Pure

| Element | Convention | Example |
|---------|-----------|---------|
| Packages | `meta::` prefix, lowercase with `::` separator | `meta::pure::executionPlan`, `meta::relational::mapping` |
| Classes | `UpperCamelCase` | `ExecutionPlan`, `RelationalExecutionNode` |
| Functions | `lowerCamelCase` | `routeFunction`, `generatePlan`, `planToString` |
| Properties | `lowerCamelCase` | `rootExecutionNode`, `resultType` |
| Stereotypes | `<<profile.tag>>` | `<<PCT.test>>`, `<<doc.deprecated>>` |

### REST Endpoints

| Convention | Example |
|-----------|---------|
| Base path: `/api` | All endpoints under `/api` |
| Version in path for Pure APIs | `/api/pure/v1/...` |
| Noun-based resource paths | `/api/pure/v1/compilation/compile` |
| HTTP verb matches semantics | `POST` for compilations, executions, transformations |
| Path segments: `lowerCamelCase` or `kebab-case` | `/executionPlan/generate`, `/grammar/transformGrammarToJson` |

---

## 3. Branching Strategy & Git Workflow

The project uses a **trunk-based development** model on GitHub:

| Branch | Purpose |
|--------|---------|
| `master` | Production-ready trunk. All releases cut from here. |
| `feature/<short-description>` | Short-lived feature branches. Merge via PR to `master`. |
| `fix/<issue-or-description>` | Bug-fix branches. |
| `release-*` | Created automatically by `maven-release-plugin`. Do not push manually. |

**Rules:**

- All changes to `master` must go through a Pull Request.
- PRs require at least one approving review (see `CODEOWNERS`).
- CI (`build.yml`) must pass: Checkstyle, unit tests, compilation.
- Commit messages: imperative mood, present tense. E.g. `Add relational milestoning support for Oracle dialect`.
- **Do not rebase `master`**. Use merge commits to preserve history.

---

## 4. Pull Request Checklist

Before opening a PR, verify:

- [ ] `mvn install -DskipTests` passes locally.
- [ ] `mvn checkstyle:check` passes with zero violations.
- [ ] New public APIs have Javadoc comments.
- [ ] New Pure functions have a comment block describing their purpose.
- [ ] Tests are added for new functionality (unit test at minimum).
- [ ] If adding a new module: `pom.xml` parent reference is correct; module is listed in root `pom.xml`; a `README.md` stub is created.
- [ ] If changing a grammar section: both parser and composer are updated; round-trip test is added.
- [ ] If changing a protocol class: migration/versioning is considered; old version transfer functions are not broken.
- [ ] If adding a new store extension: `StoreExecutorBuilder` SPI registration is present in `META-INF/services/`.
- [ ] Copyright header is present on all new files.
- [ ] No `System.out.println` — use SLF4J logger.
- [ ] No hard-coded credentials or connection strings.

---

## 5. Logging Standards

The project uses **SLF4J with Logback** (version `1.2.3`).

### Logger declaration

```java
private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MyClass.class);
```

### Log levels

| Level | When to use |
|-------|-------------|
| `ERROR` | Unrecoverable failures that impact the current request. Always include the exception. |
| `WARN` | Recoverable problems; degraded but functional state. |
| `INFO` | Key lifecycle events: server start/stop, model load, plan generated, SQL executed. Use `LogInfo` wrapper. |
| `DEBUG` | Detailed diagnostic information useful during development/troubleshooting. |
| `TRACE` | Very fine-grained; SQL parameter values, routing decisions. Never in hot paths. |

### Structured logging with `LogInfo`

Always use the `LogInfo` wrapper for `INFO`-level operational events. This ensures consistent
structured JSON log output:

```java
LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.PLAN_GENERATED, planJson).toString());
LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.SQL_EXECUTED, sqlString).toString());
```

`LoggingEventType` is an enum in `legend-shared` — add new event types there rather than
using free-text strings.

### What NOT to log

- User PII or personal data.
- Raw credentials, tokens, or secrets — not even at DEBUG level.
- Full SQL parameter values containing sensitive data in PROD (configurable via `logSQLWithParamValues` flag on `PlanExecutor`).
- Stack traces at INFO or below — always use `LOGGER.error("message", exception)`.

---

## 6. Exception Handling

### Use `EngineException` for user-facing errors

```java
throw new EngineException(
    "Cannot find mapping '" + mappingPath + "'",
    sourceInformation,
    EngineErrorType.COMPILATION
);
```

`EngineException` carries:

- A human-readable message (shown to the Studio user).
- `SourceInformation` (file, line, column) — populated from protocol parsing.
- `EngineErrorType` (`COMPILATION`, `EXECUTION`, `PARSER`, `INTERNAL`).

### Do not swallow exceptions

```java
// BAD
try { ... } catch (Exception e) { /* ignore */ }

// GOOD
try { ... } catch (Exception e) { throw new RuntimeException("Failed to ...", e); }
```

### Pure-level errors

In Pure, use `assert(condition, | 'message')` or `fail('message')` to signal errors.
The engine's Java layer catches `PureException` and converts it to an `EngineException`.

---

## 7. API Design Conventions

### REST response format for errors

All error responses use a consistent JSON body (produced by the Dropwizard exception mapper):

```json
{
  "status": 400,
  "type": "compilation",
  "message": "Cannot resolve type 'my::Model::Foo'",
  "sourceInformation": { "sourceId": "my/model.pure", "startLine": 10, "startColumn": 5 }
}
```

### Versioning

Protocol classes are versioned (e.g. `v1_24_0`). When making breaking changes to a protocol class:

1. Create a new version sub-package (`v1_25_0`).
2. Add a transfer function from the old version to the new.
3. Retain the old version classes for backwards compatibility.
4. Update `PureClientVersions` to add the new version string.

### Idempotency

Compilation and plan-generation endpoints are **idempotent** (same input always produces same output).
Execution endpoints are not (they hit live databases).

### Content negotiation

Execution endpoints support `Accept: application/json` (default), `text/csv`,
`application/x-arrow-stream` (Apache Arrow IPC). Specify via request header.
