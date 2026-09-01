# NULLS FIRST / NULLS LAST — Handover

Plan: `~/artifacts/null-ordering-plan.html` (§ references below refer to it).
Repos: `legend-pure` (`~/legend-pure`, branch `nulls-first-last`) and `legend-engine` (this repo, branch `nulls-first-last`, fork `gs-rpant1729/legend-engine`).

## Status as of 2026-09-01

**H2 relational PCT: 1332 tests, 0 failures, 0 errors — fully green**, including the 3 new
`meta::pure::functions::relation::tests::sort` null-order tests. In-memory (compiled +
interpreted) Relation PCT: 459/459 green.

Not yet run: DuckDB / cloud-store PCT (explicitly out of scope for this session — no cloud
creds / instruction not to run cloud tests), Legend SQL parity suite, other dbExtension PCT
suites (postgres/snowflake/databricks/etc.).

## Decisions taken

1. **legend-pure PR 0 (shipped).** `NullOrder` enum + `SortInfo.nullOrder[0..1]` (legend-pure,
   commit `81fb9df6`) + relational `OrderBy`/`SortByInfo.nullOrder[0..1]` (legend-pure,
   same commit). Installed locally as version `5.96.0` (the branch's own pom is
   `5.97.2-SNAPSHOT`, but engine is pinned to `5.96.0` — see **Build gotchas** below for how
   to rebuild it).

2. **API surface (shipped, WIP commit `e6d27f29a1`).** `nullsFirst`/`nullsLast` modifiers,
   2-arg `ascending(col, NullOrder)` / `descending(col, NullOrder)` sugar, in-memory
   compiled+interpreted natives, TDS bridge, relational SQL generation (root ORDER BY, window,
   WITHIN GROUP), Legend SQL CASE-WHEN revert. See commit message for full file list.

3. **`nullsFirst`/`nullsLast` body: explicit construction, not a copy.**
   `nullsFirst.pure`/`nullsLast.pure` now do
   `^SortInfo<T>(column=$sortInfo.column, direction=$sortInfo.direction, nullOrder=NullOrder.FIRST)`
   instead of `^$sortInfo(nullOrder=NullOrder.FIRST)`, matching `ascending`/`descending`'s
   style. (This alone did not fix the NPE below — kept for consistency.)

4. **Root cause + fix for the `->nullsFirst()`/`->nullsLast()` NPE on the relational path.**
   `X->nullsLast()` (chained modifier) NPE'd only when relationally routed — never in-memory.
   Traced (via a temporary `printStackTrace` patch to legend-pure's
   `CompiledSupport.executePCTTest`, since the PCT surveyor collapses every `NullPointerException`
   to the string `"NullPointer exception"`) to:
   `router preval → addToScope → resolveGenericType` NPE, because `Handlers.java`'s
   `nullsFirst`/`nullsLast` registrations computed their resolved type parameter as
   `ps.get(0)._genericType()._typeArguments().getFirst()` — copied verbatim from
   `ascending`/`descending`, whose argument is a `ColSpec<T>` (always carries its type
   argument). `nullsFirst`/`nullsLast`'s argument is a `SortInfo<T>` produced by a **nested**
   normalize-required call (e.g. `descending(~id)`), whose `genericType` has **no** type
   arguments yet at handler time → `.getFirst()` → `null` → `resolvedTypeParameters = [null]`
   → NPE in `resolveGenericType`.
   **Fix:** `Handlers.java` — guard with
   `ta.isEmpty() ? ps.get(0)._genericType() : ta.getFirst()`, mirroring the existing
   `over(SortInfo...)` handlers, which face the same shape and already do this.

5. **H2 unspecified-sort → canonical (shipped, pending your sign-off — see Decisions pending).**
   `toPostgresModel::convertOrderBy` now emits the canonical clause (`NULLS LAST` on ASC,
   `NULLS FIRST` on DESC) when `OrderBy.nullOrder` is empty, unconditionally. In production
   this only affects **H2** (`useDialectTranslation` gates the whole `toPostgresModel` path to
   H2 today — see `relationalMappingExecution.pure:429-432` — postgres/snowflake/duckdb
   `SqlDialect` defs exist but aren't live), so it's currently H2-scoped in practice despite
   being unconditional in the code.

6. **Other-DB unspecified-sort reconciliation, legacy string path (shipped, per §2.3, NOT
   cloud-verified).** `extensionDefaults.pure`:
   - `renderNullOrder(nullOrder, direction, dbConfig)` — was 1-arg (explicit-only), now 3-arg.
   - `reconcileUnspecifiedNullOrder` / `supportsNullOrderingSyntax` / `nativeNullOrdering` —
     new classification helpers.
   - Wired into the 3 shared chokepoints every dialect but 3 goes through: `processOrderBy`
     (root), default `processWindowColumn` (window), `withinGroupOrderBy` (ordered
     aggregates). DuckDB/Databricks/Snowflake override `processWindowColumn` with their own
     logic (DuckDB/Databricks already nullOrder-aware from the WIP; Snowflake hardcodes
     canonical unconditionally, pre-existing, untouched) — see **Per-DB status** below.

## Decisions pending (yours to make)

1. **H2 approach.** Two options were on the table (see conversation with the user — they said
   "I will decide for H2"):
   - **(A) Keep `convertOrderBy`'s unconditional canonical emit** (current state, H2 PCT green
     with it). Cost: every H2 dialect-translation ORDER BY now carries an explicit
     `NULLS LAST`/`NULLS FIRST` — **untested** against the SQL-string-assertion test corpus
     outside the PCT suite (roughly a dozen files across `sqlQueryToString/tests`, Legend-SQL
     H2 tests, execution-plan tests reference `H2`+`order by`; exact count not measured).
   - **(B) `;DEFAULT_NULL_ORDERING=HIGH` on the H2 JDBC URLs** (`H2Defaults.java`,
     `H2Manager.java`, `TestH2Abstract.java` — H2 currently runs `MODE=LEGACY`, which defaults
     `DEFAULT_NULL_ORDERING` to `LOW`) + **revert** `convertOrderBy` back to
     `nullOrdering = $o.nullOrder->convertNullOrder()` (UNDEFINED on empty). Zero SQL-string
     churn — H2 handles it natively. Cost: flips H2's null-order for **all** Legend queries,
     not just Relation API; any existing test asserting H2's nulls-low *result* order (not
     just SQL text) would flip. Not yet probed for how many, if any, exist.
   - If you pick (B): revert `toPostgresModel.pure`'s `convertOrderBy` hunk, add the URL param
     in the three files above, then re-run the full H2 PCT + a broader `mvn test` sweep of
     `sqlQueryToString`/Legend-SQL/exec-plan H2 tests to confirm zero churn.

2. **§2.3 per-dialect null-ordering matrix — NOT cloud-probe-verified.** The classification in
   `extensionDefaults.pure::nativeNullOrdering` is built from vendor-documentation knowledge,
   not empirical probes, **except H2** (confirmed empirically this session: nulls-low under
   `MODE=LEGACY`). Plan §2.3 explicitly says: "verify each with a one-off probe PCT test before
   wiring the config — do not trust documentation." This has not been done for any store other
   than H2. Do this via `-P pct-cloud-test` before treating the matrix as authoritative.

## Per-DB status

| DB | Classification (unverified except H2) | Root ORDER BY | Window ORDER BY | WITHIN GROUP | Notes |
|---|---|---|---|---|---|
| **H2** | nulls-low, `MODE=LEGACY` (**verified**) | canonical emitted (dialect-translation path, `convertOrderBy`) | n/a — H2 uses dialect-translation only, no separate window fix needed there | n/a | See "Decisions pending #1" |
| Postgres | canonical (nulls-high) | no-op (shared helper) | no-op (uses default `processWindowColumn`) | no-op | `toPostgresModel` dialect-translation SqlDialect exists but not live in prod (`useDialectTranslation` = H2 only) |
| Oracle | canonical | no-op | no-op | no-op | |
| Redshift | canonical | no-op | no-op | no-op | |
| Trino / Presto | canonical | no-op | no-op | no-op | Not cloud-verified |
| Snowflake | canonical | no-op (shared helper) | **unaffected by this change** — `processWindowColumnForSnowflake` hardcodes canonical unconditionally (pre-existing code, redundant-but-correct SQL even on unspecified) | no-op | Not cloud-verified |
| Databricks | nulls-low | canonical emitted via shared helper | already nullOrder-aware from WIP (`processWindowColumnForDatabricks`) | canonical emitted via shared helper | Not cloud-verified |
| SparkSQL | nulls-low | canonical emitted | uses default `processWindowColumn` → canonical emitted | canonical emitted | Not cloud-verified |
| Hive | nulls-low | canonical emitted | uses default → canonical emitted | canonical emitted | Not cloud-verified |
| BigQuery | nulls-low | canonical emitted | uses default → canonical emitted | canonical emitted | Not cloud-verified |
| Spanner | nulls-low | canonical emitted | uses default → canonical emitted | canonical emitted | Not cloud-verified |
| DuckDB | uniform NULLS LAST (diverges on DESC only) | canonical emitted on DESC via shared helper | already nullOrder-aware from WIP (`processWindowColumnForDuckDB`) | canonical emitted via shared helper | Not cloud/local-DuckDB-verified this session |
| ClickHouse | uniform NULLS LAST (diverges on DESC only) | canonical emitted on DESC | uses default → canonical emitted | canonical emitted | Not verified |
| **SQL Server** | nulls-low, **no `NULLS FIRST/LAST` syntax** | **not reconciled** — `supportsNullOrderingSyntax` returns false, so unspecified sorts render nothing (unchanged from before this session) | same | same | **Emulation not implemented.** Needs an `isnull(col)`/`CASE` prefix sort key in the dialect's own ORDER BY processor (plan §2.3), or a PCT `expectedError` exclusion. Not started. |
| **MemSQL / SingleStore** | nulls-low, no syntax | not reconciled | same | same | Same as SQL Server — not started |
| **Sybase ASE** | nulls-low, no syntax | not reconciled | same | same | Same — not started |
| **Sybase IQ** | nulls-low, no syntax | not reconciled | same | same | Same — not started |
| DB2 | canonical (assumed) | no-op | no-op | no-op | Not verified |
| Aurora, Athena | assumed canonical (postgres/trino-compatible) | no-op | no-op | no-op | Not verified — not in the plan's explicit matrix, included here by inference only |

## Known follow-ups (not started)

- Emulation for the 4 no-syntax dialects (SQL Server, MemSQL, Sybase ASE, Sybase IQ).
- Cloud probe verification of the whole matrix (`-P pct-cloud-test`).
- PCT manifest exclusions for adapters/positions that can't be reconciled (plan §J).
- `order_limit_offset.yaml` Legend-SQL parity refresh (plan §I).
- New PCT tests: window `over` with explicit null order, `joinStrings`/ordered-aggregate with
  nulls in the sort key (plan §5) — the WIP only added the 3 root-sort tests.
- `docs/engineering/reference/tds-and-relation.md` + `docs/pct/*` documentation (plan §J).

## Build gotchas (read before touching anything here)

- **Always `mvn -o clean install -DskipTests -pl <module>`** for any module you change, never
  bare `install`/`test` — a stale `target/` + the PAR-generation plugin double-registers the
  module's own code repository (`Error serializing Pure PAR: The code repository
  core_functions_unclassified already exists!` / same for `core_relational_h2_pct` etc.) unless
  `clean` runs first. Full detail: `[[build-legend-engine-stale-modules]]` (session memory) —
  also holds true for the PCT test module itself (`mvn -o clean test -pl <h2-PCT>`).
- **legend-pure `-pl` builds fail** on this branch (`nulls-first-last`) because its pom is
  `5.97.2-SNAPSHOT` but engine is pinned to `5.96.0` (deps don't resolve). To rebuild any
  legend-pure module against what engine actually uses: `git checkout legend-pure-5.96.0`
  (detached tag — the nullOrder commit `81fb9df6` is a descendant of it, not the tag itself, so
  the tag alone does NOT have nullOrder; only rebuild modules that don't touch the nullOrder
  files, e.g. `legend-pure-runtime-java-engine-compiled`), build, then `git checkout
  nulls-first-last` to restore.
- **A temporary debug patch is currently baked into the locally-installed pure 5.96.0 jar**
  (`~/.m2/repository/org/finos/legend/pure/legend-pure-runtime-java-engine-compiled/5.96.0/`):
  a `System.err.println(...) + e.printStackTrace()` in `CompiledSupport.executePCTTest`'s
  `catch (Throwable e)` block, added to get the real NPE stack past the PCT surveyor's
  `"NullPointer exception"` masking. The source is `git stash`ed in `~/legend-pure` (not
  committed anywhere). It's harmless — just noisy stderr on every already-expected PCT failure
  — but should be cleaned up: `git checkout legend-pure-5.96.0 && git stash pop` (drops it) or
  just checkout-clean + rebuild `legend-pure-runtime-java-engine-compiled` at the tag.

## Files changed this session (uncommitted → see accompanying commit)

- `Handlers.java` — nullsFirst/nullsLast type-param guard
- `nullsFirst.pure`, `nullsLast.pure` — copy → explicit construction
- `pureToSQLQuery_deprecated.pure` — window `SortByInfo.nullOrder` wiring (pre-existing
  uncommitted change from before this session, carried through untouched)
- `toPostgresModel.pure` — H2 unspecified-sort canonical emit (decision #1 above)
- `extensionDefaults.pure` — other-DB reconciliation helpers (decision #2 above)
