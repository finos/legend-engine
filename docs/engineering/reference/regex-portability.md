# Regular Expression Portability

**Verified at commit `e3efd41654c` — 2026-08-15.**
Normative semantics are defined in
[ADR-002](../decisions/ADR-002-legend-regex-canonical-semantics.md); this document records what
each execution target actually does and how far each feature travels.

To re-verify, see [Re-verification](#re-verification) at the end.

---

## TL;DR

- **Legend regex is Java regex** (`java.util.regex`), not POSIX ERE. The doc strings that say
  otherwise are wrong and are being corrected.
- **`matches` is a whole-string match. `regexpLike` is a contains.** They differ, and several
  dialects currently conflate them.
- Patterns are portable if they stay in [Tier 1](#tier-1--universal). Anything in
  [Tier 3](#tier-3--genuinely-dialect-limited) will not work on RE2-based engines and cannot be
  made to.
- `regexpIndexOf` is **0-based** with `-1` on no match — deliberately different from SQL's
  1-based/`0` convention.

---

## Tier model

The tier of a feature is the portability promise Legend makes about it.

### Tier 1 — Universal

Behaves identically on the reference runtime and every registered target. Safe to use without
qualification.

- Literal characters
- `.` (does not match newline by default)
- Character classes, ranges, negation: `[abc]`, `[a-z]`, `[^a]`
- Quantifiers `*`, `+`, `?`
- Grouping `(...)` for precedence
- `^` / `$` in `regexpLike`
- Alternation `|` **inside `regexpLike`**
- Case-insensitive matching
- `regexpReplace` with a literal (group-reference-free) replacement

### Tier 2 — Portable after emission normalization

The semantics are expressible on every target; Legend currently emits wrong or missing SQL. Each
entry is a defect, not a limitation. See [Known defects](#known-defects).

- `matches` anchoring where the pattern contains top-level alternation
- `matches` on Databricks, ClickHouse and Oracle
- `regexpIndexOf` index base and no-match sentinel
- `MULTILINE` / `NON_NEWLINE_SENSITIVE` flag plumbing
- The `regexp*` family in `toPostgresModel` (currently absent)
- Brace quantifiers `{n,m}`
- Escape classes `\d`, `\w`, `\s`
- `\b` word boundary — requires `\y` on PostgreSQL/Redshift ARE, where `\b` means backspace
- `regexpReplace` group references — Java `$n` versus SQL `\n`
- Metacharacter and backslash escaping through the string-to-SQL-literal boundary

**Promotion rule:** an item leaves Tier 2 only when its PCT probe is green on every default-build
adapter *and* the corresponding manifest entries are deleted — not reworded.

### Tier 3 — Genuinely dialect-limited

Not implementable without shipping a regex engine to the database. Documented and excluded.

| Feature | Unavailable on |
|---|---|
| Lookahead / lookbehind `(?=)` `(?!)` `(?<=)` `(?<!)` | RE2 engines: DuckDB, ClickHouse, BigQuery, Spanner, Trino/Presto |
| Backreferences `\1` | same |
| Atomic groups, possessive quantifiers | most non-Java engines |
| Named capture groups | varies; not portable |
| Unicode property classes `\p{...}` | most non-Java engines |
| Lazy quantifiers `*?` `+?` | strict POSIX ERE targets |

Tier 3 items move only on an upstream engine change; re-check on database version bumps.

### Traps — constructs that differ *silently*

Legend does not parse patterns; the literal is passed through to the database unchanged. These
constructs exist in more than one flavor with **different meanings and no error**, so they are the
ones that produce wrong answers rather than failures.

Measured on the three engines Legend can reach locally — Java 17 (the normative runtime, and the
engine behind H2, Databricks and SparkSQL), DuckDB 1.x (RE2) and PostgreSQL (ARE):

| Pattern / subject | Java | DuckDB (RE2) | PostgreSQL (ARE) |
|---|---|---|---|
| `\bcat\b` on `'the cat sat'` | `true` | `true` | **`false`** — `\b` is backspace in ARE |
| `\ycat\y` on `'the cat sat'` | `PatternSyntaxException` | error | `true` — ARE's word boundary |
| `^[[:alpha:]]+$` on `'abc'` | **`false`** | `true` | `true` |
| `^[[:alpha:]]+$` on `':::'` | **`true`** | `false` | `false` |
| `(?<=foo)bar` on `'foobar'` | `true` | **error** — RE2 has no lookbehind | `true` |

Two different shapes of divergence sit in that table, and they need different defences:

- **`\b` and `[[:alpha:]]` produce a wrong answer with no error.** On `\b`, Java and RE2 agree and
  PostgreSQL is the outlier. On `[[:alpha:]]`, RE2 and PostgreSQL agree and *Java* — the normative
  runtime — is the outlier, reading it as the character set `{:, a, l, p, h}`.
- **`(?<=…)` fails loudly**, but only on RE2 engines.

The first shape is why a green DuckDB run is not evidence of portability: DuckDB sides with Java on
`\b` and against it on `[[:alpha:]]`, so neither result generalises. See
[ADR-002 § Known limitation](../decisions/ADR-002-legend-regex-canonical-semantics.md#known-limitation-the-normative-flavor-is-not-enforced)
for why this is not fixed by choosing a different normative flavor, and for the proposed lint.

---

## What a green PCT run does and does not prove

The suites that run without credentials are H2 and DuckDB. Those cover **two of the four flavor
families** — Java and RE2 — and the Testcontainers suites add a third, PostgreSQL/ARE. Snowflake's
POSIX-ERE-with-extensions has no local coverage at all.

| Family | Engines | Local coverage |
|---|---|---|
| `java.util.regex` | H2, Databricks, Hive, SparkSQL | **H2** (no credentials) |
| RE2 | DuckDB, ClickHouse, BigQuery, Spanner | **DuckDB** (no credentials), ClickHouse (Docker) |
| joni / RE2J | Trino, Presto | Trino (Docker) |
| PostgreSQL ARE | PostgreSQL, Redshift | **PostgreSQL** (Docker) |
| ICU | MemSQL | MemSQL (Docker) |
| POSIX ERE + extensions | Snowflake, Oracle, Sybase | Oracle (Docker); **Snowflake: CI only** |

So a change to shared emission should be validated on **at least one engine per family it reaches**,
not on DuckDB alone. Concretely, verifying that `^(?:…)$` anchoring is accepted meant checking Java,
RE2 *and* ARE — three separate engines — because non-capturing groups are a syntax feature and
syntax is exactly where the families differ.

Where an engine's support is genuinely in doubt and no suite can reach it, prefer the construct that
is universal. `^(…)$` with a plain capturing group matches identically to `^(?:…)$` for a boolean
test and is accepted by every engine here; that is why Snowflake and MemSQL use it.

## Engine flavor by dialect

Determines which Tier 3 features are available and how patterns must be translated.

| Flavor | Dialects | Notes |
|---|---|---|
| `java.util.regex` | H2, Databricks/Spark, Hive, SparkSQL | The reference flavor — highest fidelity |
| RE2 | DuckDB, ClickHouse, BigQuery, Spanner | Syntactic subset of Java; no lookaround or backreferences |
| joni / RE2J | Trino, Presto | Perl-family |
| ICU | MemSQL | Perl-family |
| PostgreSQL ARE | PostgreSQL, Redshift | `\b` is backspace; word boundary is `\y` |
| POSIX ERE + extensions | Oracle, Snowflake, Sybase | No `(?:` on Oracle |

---

## `matches` emission by dialect

Source of truth for what is generated today. Line numbers are as of the stamped commit.

| Dialect | Emitted SQL | Source |
|---|---|---|
| PostgreSQL, Aurora | `%s ~ '^p$'` | `postgresExtension.pure:222`, helper `:394` |
| Oracle | `%s ~ '^p$'` | `oracleExtension.pure:231`, helper `:487` — **invalid, Oracle has no `~`** |
| H2 | `%s regexp '^p$'` | `h2Extension2_1_214.pure:237`, `h2Extension1_4_200.pure:128` |
| Snowflake | `%s regexp '^p$'` | `snowflakeExtension.pure:337` — anchors redundant, engine is whole-string |
| MemSQL | `%s regexp '^p$'` | `memSQLExtension.pure:140` |
| SparkSQL | `%s regexp '^p$'` | `sparkSQLExtension.pure:118` |
| Sybase ASE / IQ | `%s regexp '^p$'` | `sybaseASEExtension.pure:130`, `sybaseIQExtension.pure:122` |
| Presto | `REGEXP_LIKE(x, '^p$')` | `prestoExtension.pure:82` |
| Trino | `REGEXP_LIKE(x, '^p$')` | `trinoExtension.pure:91` |
| DuckDB | `%s similar to %s` | `duckdbExtension.pure:246` — **correct**; DuckDB `SIMILAR TO` is RE2 whole-string |
| ClickHouse | `cast(match(%s, %s) as Bool)` | `clickHouseExtension.pure:115` — was unanchored; now anchored in the transform |
| Databricks | `rlike(x, p)` | `databricksExtension.pure:131`, helper `:456` — was unanchored; now anchored in the transform |
| BigQuery, Redshift, Hive, Spanner, SQLServer | *not registered* | `[unsupported-api]` error |

The shared anchoring helpers are `regexpPattern` (`extensionDefaults.pure:671`) and
`regexpLikePattern` (`:768`); the newer translation stack has its own at
`sqlDialectDefaults.pure:892`.

### The `regexp*` family by dialect

Measured against real engines, not inferred. Each dialect's `RegexpParameter` flags are emitted as
an **inline pattern prefix** (`(?i)`, `(?m)`, `(?s)`) rather than an options argument, because none
of these engines takes a flags parameter and all the Java- and RE2-family ones accept the inline
form natively.

| Dialect | `regexpLike` | `regexpCount` | `regexpExtract` | `regexpReplace` | `regexpIndexOf` | Flags |
|---|---|---|---|---|---|---|
| DuckDB | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Snowflake | ✅ | ✅ | ✅ | ✅ | ✅ | MULTILINE ✗ |
| Databricks | ✅ | ✅ | ✅ | all only | group ✗ | ✅ |
| Trino | ✅ | ✅ | ✅ | all only | group ✗ | ✅ |
| MemSQL | ✅ | ✗ | first only | all only | group ✗ | ✗ |
| Spanner | ✅ | ✗ | ✗ | all only | ✗ | ✗ |
| H2, Postgres, ClickHouse, Oracle, SQLServer | ✗ | ✗ | ✗ | ✗ | ✗ | — |

Recurring engine limits, each now reported with a message naming the cause rather than a generic
`[unsupported-api]`:

- **No capture-group argument** on `regexp_instr` (Databricks, MemSQL) or `regexp_position` (Trino),
  so `regexpIndexOf` with a group is unavailable everywhere except DuckDB and Snowflake.
- **No first-occurrence-only replace** on Databricks, Trino, MemSQL or Spanner — their
  `regexp_replace` always replaces every match, and the extra argument is a start position rather
  than an occurrence count.
- **MemSQL rejects inline flags** outright: `Got error 'repetition-operator operand invalid' from
  regexp`. Spanner accepts them and then ignores them, which is worse — the query succeeds and the
  answer is wrong, so those tests fail on assertion rather than error.
- **Spanner is the PostgreSQL dialect**, not GoogleSQL: `regexp_contains`, `regexp_extract`,
  `regexp_extract_all` and `regexp_instr` all report *does not exist* / *is not supported*. Only the
  `~` operator and `regexp_replace` are available, which is why `matches` and `regexpLike` work and
  nothing else does.

The `MULTILINE` result is worth noting: Databricks and Trino pass every flag combination, including
the four that Snowflake fails. That confirms Snowflake's failure is `addWildCardCharacters` padding
patterns with `.*` to simulate substring matching — `.*` cannot cross a newline — rather than an
engine limitation.

---

## Known defects

Each row is a Tier 2 item: our emission is wrong, the engine is capable.

| # | Defect | Affected | Evidence |
|---|---|---|---|
| D1 | ~~Anchors concatenated without grouping, so top-level alternation escapes them: `matches(x,'cat\|dog')` → `'^cat\|dog$'` = `(^cat)\|(dog$)`. **Silent wrong rows.**~~ **fixed** on every PCT-covered dialect; verified on Java, RE2 and ARE. Still present on the pinned dialects below. | was: all substring-matching engines | `extensionDefaults.pure`, `sqlDialectDefaults.pure` |
| D2 | `matches` emitted unanchored, silently becoming `regexpLike` | Databricks, ClickHouse | `databricksExtension.pure:456`, `clickHouseExtension.pure:115` |
| D3 | PostgreSQL `~` operator emitted for Oracle → `ORA-00911` | Oracle | `oracleExtension.pure:487` |
| D4 | ~~`regexpIndexOf` 1-based and returning `0` for no match~~ **fixed** — Snowflake subtracts one from `regexp_instr` (which converts origin and sentinel together); DuckDB guards with `regexp_matches` and subtracts one | Snowflake, DuckDB | exclusions deleted; `testRegexpIndexOf_NoMatch` added |
| D4a | **Residual:** DuckDB has no `regexp_instr`, so the position is found by locating the matched *text*. When that text occurs earlier in the string the answer is wrong — `regexpIndexOf('aba', 'a$')` gives `0` where the platform gives `2`. No test covers it. | DuckDB | `duckdbExtension.pure`, `transformRegexpIndexOf` |
| D5 | `MULTILINE` not honoured — DuckDB ignores the `m` option letter; inline `(?m)` works | Snowflake, DuckDB | manifests: `Assert failed` ×4 each |
| D6 | `regexp*` family absent from `toPostgresModel`; and its `regexp_like` means full-match, contradicting PostgreSQL | All newer-stack dialects | `toPostgresModel.pure:346-351` |
| D7 | Transpiler accepts `~` only for fully-anchored case-sensitive string literals; `regexp_*` family absent | SQL front end | `fromPure.pure:4002`, `function_processors.pure:219` |
| D7a | The transpiler strips `^…$` and calls `matches`, without grouping the alternation — the same defect as D1, on the SQL side. Postgres `val ~ '^hello\|fox$'` is true for `'the quick brown fox'`; Legend returns false. Pinned by the parity suite. | SQL front end | `fromPure.pure`, `createRegexMatch` |
| D8 | Patterns are never parsed or checked — a dialect-specific literal passes straight through, so a model silently locks to whichever store it was authored against. See [Traps](#traps--constructs-that-differ-silently). | All | no validation exists |

### What the SQL parity suite can and cannot detect

`legend-engine-xt-sql-e2e-tests` runs each query against a real PostgreSQL and against Legend, and
compares cell by cell. But **the Legend path transpiles to Pure and pushes back down to that same
PostgreSQL**, so both sides are ultimately evaluated by one engine. That fixes what the suite can see:

- **Detects transpilation divergence** — anything that changes the *meaning* of the query before it
  reaches the database. D7a is exactly this: the anchors are stripped without grouping, so Legend
  sends a different pattern than the user wrote, and the two results differ.
- **Cannot detect flavor divergence** — a construct that Java and ARE read differently never shows
  up, because the Java engine is not in the loop. `val ~ '^[[:alpha:]]+$'` passes parity while
  answering differently on the platform runtime.

Flavor divergence is therefore a PCT concern, and transpilation divergence a parity concern. Neither
suite substitutes for the other, and a construct can be green in both while still being unportable.

### Dialects deliberately left alone

`matches` anchoring was corrected only where a PCT module can prove it. **Sybase ASE, Sybase IQ,
SparkSQL and Presto have no PCT module**, so they stay on the ungrouped `^p$` form via
`regexpPatternLegacy` / `regexpLikePatternLegacy` in `extensionDefaults.pure`. They carry D1, and
that is a deliberate choice: an unverifiable change to a store with long-standing users is worse
than a known, documented defect. Give one of them a PCT module and the pin can be lifted in the same
change that proves it.

Aurora is not in that group — it loads the PostgreSQL extension wholesale
(`auroraExtension.pure`), so it is covered by the PostgreSQL suite.

### Verifying D1 and D5 yourself

D1 measured against a real H2 2.2.224, comparing the form Legend used to emit with the corrected
one. `matches(x, 'cat|dog')` is `false` for both `'catfish'` and `'hotdog'` under the normative
Java semantics:

| Query | Result |
|---|---|
| `SELECT 'catfish' ~ '^cat\|dog$'` | `TRUE` ← wrong row returned, silently |
| `SELECT 'hotdog' ~ '^cat\|dog$'` | `TRUE` ← wrong row returned, silently |
| `SELECT 'cat' ~ '^cat\|dog$'` | `TRUE` |
| `SELECT 'catfish' ~ '^(?:cat\|dog)$'` | `FALSE` ← corrected |
| `SELECT 'hotdog' ~ '^(?:cat\|dog)$'` | `FALSE` ← corrected |
| `SELECT 'cat' ~ '^(?:cat\|dog)$'` | `TRUE` |

D5, against DuckDB:

```
$ duckdb -c "SELECT regexp_matches(E'abc\ndef','^def','m'), regexp_matches(E'abc\ndef','(?m)^def')"
false, true                                    -- the option letter is ignored; the inline flag works
```

---

## Exclusion classification

PCT manifests record *that* a test is excluded but not *why*, and the two causes need different
responses. Until `PCTManifestExclusion` can carry an `AdapterQualifier`
(see [Upstream](#upstream-gap)), classification lives in this table.

Read the cause off the `expectedError` shape:

| `expectedError` shape | Cause | Qualifier |
|---|---|---|
| `[unsupported-api] ... not supported yet` | Function is not wired for that dialect. Nothing about the database prevents it. | `needsImplementation` |
| `Couldn't find DynaFunction to Postgres model translation for X()` | Missing `toPostgresModel` arm. | `needsImplementation` |
| `java.sql.SQLException` / engine parse error | The database genuinely cannot do it. | `unsupportedFeature` |
| `expected: X actual: Y`, `Assert failed` | **We emit SQL that runs and returns the wrong answer.** The dangerous class. | `needsImplementation` unless provably impossible |

**Evidence rule:** a `needsImplementation` classification is only accepted alongside a companion
SQL-generation golden test asserting the currently-generated SQL (see
[Re-verification](#re-verification)). The fixer then edits the golden expectation and deletes the
manifest entry in the same commit, so the label cannot rot.

### Current classification

| Test | Adapter | Qualifier | Defect |
|---|---|---|---|
| `regexpLike`, `regexpCount`, `regexpExtract`, `regexpReplace`, `regexpIndexOf` (all) | h2 | `needsImplementation` | D6 |
| same five | postgres, clickhouse, databricks, memsql, oracle, trino, spanner, sqlserver | `needsImplementation` | not wired |
| `matches`, `matchesNoMatch` | oracle | `needsImplementation` | D3 |
| `matches`, `matchesNoMatch` | spanner, sqlserver | `needsImplementation` | not wired |
| `testRegexpIndexOf`, `_GroupNumber` | snowflake, duckdb | `needsImplementation` | D4 |
| `testRegexpLike_Multiline` ×4 | snowflake, duckdb | `needsImplementation` | D5 |

Nothing in the regex family is currently classified `unsupportedFeature` — every present exclusion
is our own gap. SQLServer is the expected future exception: it has no native regex before
SQL Server 2025.

### Upstream gap

`PCTManifest$PCTManifestExclusion` (legend-pure 5.94.0) has only `test` and `expectedError`. The
`AdapterQualifier` enum already exists — `unsupportedFeature`, `assertErrorMismatch`,
`needsInvestigation`, `needsImplementation` — and `PCT_to_SimpleHTML` already de-emphasises
`unsupportedFeature` rows, but it is unreachable from JSON.

**Do not add a `qualifiers` key to a manifest.** `PCTManifestLoader.MAPPER` is a bare
`ObjectMapper`, so an unknown property throws and takes down that adapter's entire suite. The
upstream change is three files: add the field, thread it through
`PCTReportConfiguration.buildExpectedFailures` (which currently hard-codes an empty array), and
relax `FAIL_ON_UNKNOWN_PROPERTIES`. This table migrates into the manifests once that lands.

---

## Re-verification

**Support matrix.** Do not hand-maintain one. The per-test pass/fail data is produced by the
`generate-pct-report` Maven goal, wired into every PCT module's `PCT-Generation` execution, and
rendered by `PCT_to_SimpleHTML`.

**Generated SQL, without a database** — the fastest way to tell "we emit wrong SQL" from "the
database can't", and the vehicle for the evidence rule above:

```bash
mvn -o -q test -pl <dialect>-pure -Dtest=Test_Pure_Relational_DbSpecific_<dialect>
```

Golden expectations live in each dialect's `sqlQueryToString/tests/` directory, e.g.
`testDuckDBSQLGeneration.pure`.

**Reference runtimes** — these must stay at zero regex exclusions; a new entry there means a test
encodes a wrong expectation, not that a target has a gap:

```bash
mvn -o -q test -pl <...>-compiled-functions-unclassified    -Dtest=Test_Compiled_UnclassifiedFunctions_PCT
mvn -o -q test -pl <...>-interpreted-functions-unclassified -Dtest=Test_Interpreted_UnclassifiedFunctions_PCT
```

**Ad-hoc engine probes.** Every empirical finding in this document came from one. Cheap, and worth
repeating before committing to any emission shape.

**Which adapters run where.** Only Snowflake and Databricks carry the `pct-cloud-test` profile;
the other sixteen PCT modules run in the default build, several via Testcontainers. A new PCT test
therefore lands on ~12 adapters at once.

```bash
for p in $(find . -name pom.xml -path "*-PCT/*" | grep -v /target/); do
  grep -q "pct-cloud-test" $p && echo "cloud-gated: $p"; done
```
