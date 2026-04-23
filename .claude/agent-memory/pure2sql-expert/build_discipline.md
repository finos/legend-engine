---
name: Build discipline reminders
description: Non-obvious Maven / IDE / PCT build conventions specific to legend-engine that you need every session
type: feedback
---

Every session repeats these. Keep them at hand.

**Rule:** Always use `mvn clean install …` — never skip `clean`.
- **Why:** Several Pure Maven plugins fail with spurious `duplicate artifact present` errors when building over a previous `target/` directory. Observed across many sub-modules.
- **How to apply:** even `mvn clean install -DskipTests -T 4` is cheaper than debugging the duplicate-artifact error.

**Rule:** Use `-pl <module> -am` when scoping to one dialect.
- **Why:** Dialect modules depend on core-pure, core-executionPlan-generation, core-relationalStore-pure, and legend-pure snapshots. Without `-am`, you rebuild against stale installed artifacts and hit classloading drift at test time.
- **How to apply:** `mvn clean install -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-duckdb/legend-engine-xt-relationalStore-duckdb-PCT -am -DskipTests -T 4`.

**Rule:** Cloud-only adapters (Databricks, Snowflake) cannot be validated locally.
- **Why:** They require the `pct-cloud-test` Maven profile and CI-injected secrets. Local runs of their PCT suites will fail in connection setup, not in translation.
- **How to apply:** propose reproduction steps and expected outcome; escalate to a maintainer who can run in CI.

**Rule:** Testcontainers auto-starts Docker for integration tests (DeepHaven, Spanner, Postgres, ClickHouse, Oracle, SqlServer, Trino).
- **Why:** They bring up the DB image on demand and tear it down. Pre-pulling is unnecessary but Docker must be running.
- **How to apply:** start Docker Desktop; don't try to run these inside a container-less CI.

**Rule:** Disable IntelliJ's `Clear output directory on rebuild`.
- **Why:** IntelliJ wipes generated Pure-runtime resources the server needs on startup, producing `ClassNotFoundException` for generated classes.
- **How to apply:** Preferences → Build → Compiler → uncheck the setting. One-time. If tests start failing with `ClassNotFoundException`, check this first.

**Rule:** Iterate on `.pure` without rebuilding Java via Pure IDE.
- **Why:** Recompiling the Pure compiled-code jars on every `.pure` edit is minutes-scale; PureIDE reloads the module tree live.
- **How to apply:** run `org.finos.legend.engine.ide.PureIDELight` with `legend-engine-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json`. UI at http://127.0.0.1:9200/ide.

**Rule:** `-am` builds **dependencies** of the selected module, not consumers.
- **Why:** Obvious in principle, easy to forget under pressure. Session 2026-04-22: fix in `legend-engine-xt-relationalStore-core-pure` ran downstream DuckDB PCT via `-am` but `-am` does NOT rebuild `legend-engine-xt-relationalStore-executionPlan` (a consumer), so a companion Java change there wasn't compiled. Result: test still failed with the un-fixed code path.
- **How to apply:** when a fix spans Pure-side (generator) AND Java-side (plan/runtime), either use `-amd` (also make dependents) or list each consumer explicitly on `-pl`.

**Rule:** Never pipe `mvn install` through `| tail -N` when you care about success.
- **Why:** Pipes discard the Maven exit code from the shell's POV, and `tail` shows only the trailer — `BUILD FAILURE` lines above it are silently dropped. Session 2026-04-22: an IDE edit with a type error (wrong `orElse` arity on `RelationalTreeNode[*]`) failed the PAR plugin; `| tail` hid the failure; we ran downstream tests against a stale jar for ~30 min before noticing.
- **How to apply:** redirect the whole build to a log file (`mvn clean install … > /tmp/build.log 2>&1`) and grep explicitly for `BUILD SUCCESS` / `BUILD FAILURE`. If you must use `tail`, verify the exit code separately (`; echo "exit=$?"`).

**Rule:** After applying a Pure-side fix, verify the new JAR timestamp in `~/.m2` before running downstream tests.
- **Why:** Session 2026-04-22: a silent PAR build failure left the old JAR in place; downstream DuckDB PCT passed/failed against stale bytecode, sending investigation in the wrong direction.
- **How to apply:** `ls -l ~/.m2/repository/org/finos/legend/engine/legend-engine-xt-relationalStore-core-pure/<version>/*.jar` before running the consumer test. If the mtime is older than the fix, the install didn't land.

**Rule:** DuckDB PCT requires `mvn clean test` not `mvn test`.
- **Why:** The Pure PAR plugin throws "code repository X already exists" on incremental rebuilds. Already in CLAUDE.md; bit us again this session.
- **How to apply:** always `mvn clean test -pl …-duckdb-PCT`, never incremental.

**Rule:** For commits, do NOT add `Co-Authored-By: Claude ...`.
- **Why:** CLA bots on FINOS public repos verify every author/co-author has a signed CLA. The Claude identity doesn't, so the check fails and blocks PRs.
- **How to apply:** use user's git identity only.
