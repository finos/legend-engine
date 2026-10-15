# Legend Engine — SQL E2E interpreted dev loop

Pure-side driver for the SQL e2e parity corpus: runs slices of it through the **interpreted** Pure
LSP so edits to `fromPure.pure` and the rest of the SQL→Pure translation are testable in seconds
instead of a 15–25 min rebuild.

This module is only the driver. The corpus, the reference Postgres and the harness all live in
[`legend-engine-xt-sql-e2e-tests`](../legend-engine-xt-sql-e2e-tests/README.md) — **that README is
the documentation for both modules**: how to run either suite, baseline statuses, the traps worth
knowing before investigating a failure, known-unsupported areas, and how this driver is built.

For driving it as an agent, use the `sql-e2e-devloop` skill.

- `src/main/resources/core_external_query_sql_e2e/model.pure` — test schema and mapping
- `src/main/resources/core_external_query_sql_e2e/framework.pure` — the driver
- `src/main/java/.../{compiled,interpreted,shared}` — the three natives, per runtime
