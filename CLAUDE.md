# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
Rules that apply to every task are inline; the rest is a link — read the linked file before working
in that area. Full index: [`docs/engineering/README.md`](docs/engineering/README.md).

## Build & Run

Toolchain: **JDK 11** (Maven enforcer: `[11.0.10,12)`), **Maven 3.6.2+**. Full clean build is
expensive (15–25 min); prefer `-T 4` and `-DskipTests` during iteration.

Module rebuild and test-rerun commands (always `clean`, touched modules only, no `-am`, direct
Surefire for test-only reruns) are imported from `AGENTS.md`:

@AGENTS.md

- **Isolate from the shared `~/.m2`:** stamp every Maven command in a session, tests included, with
  `-Drevision=$(git rev-parse --abbrev-ref HEAD)-SNAPSHOT`. Never commit a changed `<revision>`,
  and do not isolate with `versions:set`. Tests failing in modules you never touched, or a test
  count that changes between identical runs, mean another checkout's jars — suspect the cache before
  your change. Why, and the chained local repository for when `~/.m2` must not be written at all:
  [`build-isolation.md`](docs/engineering/guides/build-isolation.md).
- **Checkstyle** (blocking in CI): `mvn checkstyle:check@verify -pl <module-path>`. The project
  rules are bound to the `verify` execution; a bare `checkstyle:check` runs Checkstyle's defaults.
- **`.pure` edit-test in seconds:** the Pure IDE delta compiler (`POST /executeGo`) —
  [`pure-ide-delta-compiler-debugging.md`](docs/engineering/guides/pure-ide-delta-compiler-debugging.md).
  **`openFiles` in that payload overwrites the target file on disk**: `GET /fileAsJson/<path>` to
  read the current content in full, apply the edit to that text, submit the whole result. Never
  compose the file from scratch — anything you omit is silently deleted, and `welcome.pure` is
  gitignored so git cannot recover it.
- Running the server, Pure IDE and REPL, IntelliJ settings, troubleshooting:
  [`getting-started.md`](docs/engineering/guides/getting-started.md). Maven plugins, profiles, CI
  pipeline, Docker, releases: [`build-and-ci.md`](docs/engineering/guides/build-and-ci.md).

### Claude Code dev-loop plugin (`pure-dev`)

This repo ships a Claude Code plugin (`.claude-plugin/marketplace.json` + `plugins/pure-dev/`) that
wraps the Pure LSP into a warm, incrementally-compiling daemon — the same fast edit-test loop as the
delta compiler above, but driven through skills instead of hand-built HTTP calls against
`ideLightConfig.json`'s server. **Not installed by default** — enable it once per machine:

```bash
bash .claude-plugin/install-pure-dev
```

Then restart the agent. Once installed, prefer these skills over `mvn clean install` or the raw
`/executeGo` HTTP calls above when iterating on `.pure` code: `pure-lsp-check` (compile-check a file
in ~1s), `pure-lsp-go`/`pure-lsp-execute`/`pure-lsp-execute-parallel` (run `go()`, a specific
function, or whole test scopes), `pure-lsp-launch-engine` (the one-command way to start the daemon
scoped to this repo, backend engine Server included), and `pure-chain-update` (resyncing a local
legend-pure + legend-engine checkout). Deep internals and operating rules live in
`plugins/pure-dev/references/lsp-devloop-internals.md` and `lsp-devloop-usage-rules.md`.

## Tests

- JUnit 5 for new tests (JUnit 4 exists but is legacy — don't add more). Class naming:
  `Test<Subject>`. Single test: `mvn test -pl <module-path> -Dtest="TestClassName#testMethodName"`.
- Use `JsonUnit.assertJsonEquals` for JSON comparison (not `String.equals`). Pure test resources go
  under `src/test/resources/` mirroring the production package.
- **PCT** (Pure Compatibility Tests) runs every `<<PCT.test>>` Pure function against every
  registered store. H2, DuckDB and the Java binding run on every PR; cloud stores are CI-only
  (`pct-cloud-test` profile, requires secrets). When a PCT test fails on a target, decide: real bug →
  fix; legitimately unsupported → add an `exclusions` entry (`test` FQN + `expectedError`) to that
  adapter's JSON manifest under `src/main/resources/pct-manifests/<adapter>/` — the runner prints
  the copy-paste snippet. See [`expected-failures-howto.md`](docs/pct/expected-failures-howto.md).
- Frameworks, naming, test data, running suites, CI matrix:
  [`testing-strategy.md`](docs/engineering/testing/testing-strategy.md). PCT authoring:
  [`docs/pct/`](docs/pct/) (`purefunction-howto.md`, `native-howto.md`, `wiring-howto.md`,
  `conventions.md`, `taxonomy.md`).

## Architecture

Legend Engine is the execution backbone of the FINOS Legend platform: a Dropwizard HTTP server
wrapping the Pure runtime. A Pure query + Mapping + Runtime goes through grammar ↔ protocol,
compilation, plan generation and plan execution. Production always runs **compiled mode** (Pure
pre-translated to Java bytecode at build time); the Pure IDE path uses interpreted mode.

Dependency direction: `config → xts-* → core → legend-pure → legend-shared`. Extensions depend only
on core interfaces.

| For | Read |
|-----|------|
| Pipelines and their endpoints, extension SPIs (Java `ServiceLoader` + Pure `Extension`), dependency layers, key data structures | [`architecture/overview.md`](docs/engineering/architecture/overview.md) |
| What each module is and its entry-point classes | [`reference/modules.md`](docs/engineering/reference/modules.md) |
| Adding a grammar section, store extension or function activator | [`guides/contributor-workflow.md`](docs/engineering/guides/contributor-workflow.md) |
| Router, Pure-to-SQL, dialects | [`architecture/router-and-pure-to-sql.md`](docs/engineering/architecture/router-and-pure-to-sql.md) |
| Domain concepts, execution plans, pre-evaluation, Alloy compiler, and the rest | [`docs/engineering/architecture/`](docs/engineering/architecture/) |

Rules that hold wherever you are working:

- **New extension:** follow the `xts-*` sub-module suffixes (`-grammar`, `-protocol`, `-compiler`,
  `-pure`, `-execution`, `-http-api`), and register on both sides — Java SPI files under
  `META-INF/services/` and the Pure `meta::pure::extension::Extension`.
- **New store:** also register a `-PCT` module and add it to
  `.github/workflows/resources/modulesToTest.json` in the appropriate CI group.
- **Protocol versioning:** protocol classes are versioned (`v1_24_0`, `v1_25_0`, …). Breaking changes
  require a new version sub-package, a transfer function from the old version, and an entry in
  `PureClientVersions`. Do **not** mutate an existing version's classes.

## Conventions to follow

Full detail: [`coding-standards.md`](docs/engineering/standards/coding-standards.md).

- **Indentation:** Java 4 spaces, Pure 2 spaces, XML/JSON/YAML 4 spaces. No tabs anywhere (`.java`, `.xml`, `.pure` all checked).
- **Braces:** always required (including single-statement `if`). Opening brace on a new line; closing brace alone on its line.
- **Copyright header:** every new file (including `.pure`) needs the Apache 2.0 header — Checkstyle enforces this.
- **Logging:** SLF4J only — never `System.out.println`. For `INFO`-level operational events use `LogInfo` wrapper + `LoggingEventType` enum (in `legend-shared`); add new event types to the enum rather than free-text strings. Never log credentials/tokens, not even at DEBUG.
- **Errors:** user-facing errors throw `EngineException` with `SourceInformation` and an `EngineErrorType` (`COMPILATION`/`EXECUTION`/`PARSER`/`INTERNAL`). Don't swallow exceptions. Pure-level: `assert(cond, | 'msg')` or `fail('msg')` — the Java layer converts `PureException` → `EngineException`.
- **Comments:** naming is the documentation — rename or restructure rather than explain. Do not
  narrate what the code does or rationalise why you wrote it, in tests least of all. A comment earns
  its place only when the code is genuinely ambiguous or looks like a mistake a reader would "fix".
  Match the surrounding file's comment density; most functions here have none. Full policy:
  [`coding-standards.md` §8](docs/engineering/standards/coding-standards.md#8-comments).
- **Grammar changes:** update both parser and composer, and add a round-trip test.
