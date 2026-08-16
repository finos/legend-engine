# ADR-002: Canonical Semantics for Legend Regular Expressions

**Status:** Accepted
**Date:** 2026-08-15
**Deciders:** Legend Engine core team

---

## Context

Legend exposes six regular-expression functions in Pure —
`matches`, `regexpLike`, `regexpCount`, `regexpReplace`, `regexpExtract`, `regexpIndexOf`
(`legend-engine-core/.../core_functions_unclassified/string/regex/`) — and pushes them down to
roughly sixteen relational dialects plus the SQL→Pure transpiler.

That pushdown has never been specified. Three consequences follow, all observed in the current
tree:

**1. The reference implementation and the documentation disagree.** The functions are implemented
in `FunctionsHelper.java` and the interpreted natives using `java.util.regex`. All six doc strings
claim *"The platform supports the full POSIX ERE (Extended Regular Expression) syntax"* — fifteen
occurrences. POSIX ERE and `java.util.regex` are not the same language and neither contains the
other: `\d`, lazy quantifiers, lookaround and backreferences exist in Java and not in POSIX ERE,
and POSIX leftmost-longest alternation differs from Java's leftmost-first.

**2. Each dialect was wired independently, so identical Pure returns different answers.** `matches`
alone emits seven different constructs across dialects. Two of them (`rlike` on Databricks, `match`
on ClickHouse) are substring-matching functions with no anchoring, so `matches` silently behaves as
"contains". And the shared anchoring helper builds its pattern by string concatenation:

```pure
regexpPattern(query) = '%s regexp \'' + '^' + $query + '$' +'\''
```

Because alternation binds loosest in every regex grammar, `matches(x, 'cat|dog')` emits
`'^cat|dog$'`, which the engine reads as `(^cat)|(dog$)`. `'catfish'` and `'hotdog'` match; Java
returns false. No error is raised.

**3. Nothing detects any of this.** The `matches` PCT suite is three assertions, and every one of
them returns the same answer under full-match and substring semantics — which is precisely why the
dialects that implement `matches` as "contains" pass today.

Underlying all three is an unanswered question: **when a Pure regex and a database regex disagree,
which one is right?** Without an answer, every dialect wiring is a local judgement call, and the
`toPostgresModel` intermediate representation has already accumulated a name whose meaning
contradicts the database it is named after (`regexp_like` there means *anchored full match*, while
Postgres `regexp_like` is substring).

---

## Decision

Regex portability is governed by **two separate layers with different authorities**. Conflating
them is what produced the `regexp_like` collision, so they are decided independently.

### Layer 1 — Pattern syntax flavor: `java.util.regex`

What `\d`, `\b`, `(?:`, lookaround and quantifier greediness *mean* is defined by
`java.util.regex` (Java 11+). Not POSIX ERE, and not PostgreSQL's Advanced Regular Expressions.

Rationale:

- The reference implementation *is* Java. Much of Pure never reaches a database — M2M
  transformations, in-memory execution, and the compiled and interpreted runtimes — and PCT's
  definition of functional correctness makes the platform runtime normative by construction. The
  compiled and interpreted PCT manifests carry zero regex exclusions, so this is already true in
  practice; the ADR records it rather than changing it.
- Java is the center of the target population, not the edge. Java-family and RE2-family engines
  dominate: Java (H2, Databricks/Spark, Hive, SparkSQL), RE2 (DuckDB, ClickHouse, BigQuery,
  Spanner), joni/RE2J (Trino, Presto), ICU (MemSQL). Only PostgreSQL and Redshift use ARE.
  Choosing Java makes RE2 a clean *syntactic subset*, which is why the unportable set collapses to
  "lookaround and backreferences". Choosing ARE would require pattern translation for roughly ten
  dialects instead of two.
- Changing this layer would mean implementing ARE semantics inside the Java runtime — shipping a
  regex engine — rather than adjusting SQL emission.

### Layer 2 — Function contract: PostgreSQL

Function names, arity, substring-versus-full-match semantics, flag letters and argument order
follow PostgreSQL wherever doing so does not force a change to the reference implementation.

Rationale:

- The SQL front end is PostgreSQL by construction: the grammar is `postgresSql-grammar`, the wire
  protocol is PostgreSQL, and the parity suite compares against a live PostgreSQL 16.
- `toPostgresModel` is already the intermediate representation for the newer dialect-translation
  stack; every dialect passes through a PostgreSQL-shaped model. Giving that model PostgreSQL's
  meanings is what the layering already assumes.
- It dissolves the `regexp_like` collision rather than negotiating it: the name returns to
  substring semantics, and no emitter compensates with `^…$`.
- It simplifies the transpiler. `createRegexMatch`'s three restrictions (literal patterns only,
  mandatory `^…$` anchoring, no case-insensitivity) exist only because PostgreSQL `~` — a
  substring operator — is being forced onto `matches`, a full-match function. With `regexpLike` as
  the target, `~` maps directly.

### Normative function semantics

| Function | Semantics |
|---|---|
| `matches(s, p)` | **Full-string.** True iff `p` matches all of `s` (`Matcher.matches()`). |
| `regexpLike(s, p, flags)` | **Substring.** True iff `p` matches anywhere in `s` (`Matcher.find()`). |
| `regexpCount(s, p, flags)` | Count of **non-overlapping** successive matches. |
| `regexpExtract(s, p, all, g, flags)` | Group `g` of the first match, or of every match when `all`. |
| `regexpReplace(s, p, r, all, flags)` | `Matcher.replaceFirst`/`replaceAll`. |
| `regexpIndexOf(s, p, g, flags)` | **0-based** index of the start of group `g` of the first match. |

**Normative identity — this is the definition of `matches`, and the source of the anchoring defect:**

```
matches(s, p)  ≡  regexpLike(s, '^(?:' + p + ')$')
```

The **non-capturing group is part of the identity**, not an implementation detail. Emitting
`'^' + p + '$'` without it is incorrect for any pattern with top-level alternation.

Further normative points:

- `regexpIndexOf` returns **`-1`** when there is no match, and `-1` when the requested group
  exceeds the pattern's group count. It is never 1-based (see the exception below).
- `regexpReplace` replacement strings use **Java `$n`** group-reference syntax, not SQL `\n`.
  Dialects whose native function uses `\n` require translation.
- `RegexpParameter` maps to `java.util.regex` flags: `CASE_SENSITIVE` → none,
  `CASE_INSENSITIVE` → `CASE_INSENSITIVE`, `MULTILINE` → `MULTILINE`, `NON_NEWLINE_SENSITIVE` →
  `DOTALL`. Flags are OR-ed, so `[CASE_SENSITIVE, CASE_INSENSITIVE]` yields case-insensitive.
  `UNICODE_CASE` is **not** set, so case folding is ASCII-only.
- The empty pattern matches at every position: `regexpLike(s,'')` is true for all `s`;
  `matches(s,'')` is true only for the empty string.
- Relational NULL handling is **out of scope for this version**. All signatures are `String[1]`,
  so Pure-side null is unreachable; SQL three-valued logic applies only to nullable columns and
  cannot be expressed in the current PCT test shape.

### The one deliberate exception: `regexpIndexOf` is 0-based

PostgreSQL `REGEXP_INSTR` is 1-based and returns `0` on no match. Legend keeps **0-based with `-1`
on no match**, against the Layer 2 rule.

The function's *shape* (occurrence, group number) was already modelled on SQL `REGEXP_INSTR` while
its index base was implemented Java-style; that incoherence is the root of the current
off-by-one failures on Snowflake and DuckDB. Aligning to PostgreSQL would buy identity mappings in
the transpiler and on Snowflake, but costs a breaking change to the platform contract, an edit to
the reference implementation, and churn in the two PCT manifests that are currently at zero
exclusions. The alternative is a single arithmetic term in emission — `- 1` toward the database,
`+ 1` toward SQL — which is provably exact, because the no-match sentinel conversion falls out of
the same offset. The cost is paid on the cheap side.

### Known limitation: the normative flavor is not enforced

Declaring Java normative does not make it so. Legend never parses a regex pattern — it is a string
literal that is unquoted and concatenated into the generated SQL, so **whatever the user wrote is
what the database receives**. The reference runtime would enforce Java semantics, but a query
against a relational store never reaches it.

The consequence is that a pattern authored against whichever dialect the user happened to test on
will appear to work, and the model becomes silently dialect-locked. Three grades of failure, worst
last:

1. **Loud, on the platform.** `\y` (PostgreSQL ARE word boundary) throws
   `PatternSyntaxException` on the Java runtime. Fine — the user finds out.
2. **Loud, on some target.** Lookbehind `(?<=foo)bar` and backreferences `(ab)\1` work on Java and
   H2 and are rejected by RE2 engines (DuckDB, ClickHouse, BigQuery, Spanner). The model works
   until it is pointed at a different store.
3. **Silent, and inverted.** `[[:alpha:]]` is an ordinary POSIX bracket expression to anyone coming
   from PostgreSQL, Oracle or RE2, where it means "any letter". Java has no such construct and
   parses it as a nested character class — the set `{:, a, l, p, h}` — without complaining.
   Measured, for `matches(s, '[[:alpha:]]+')`:

   | Input | Java (normative) | DuckDB / RE2 |
   |---|---|---|
   | `'abc'` | `false` | `true` |
   | `'xyz'` | `false` | `true` |
   | `'alpha'` | `true` | `true` |
   | `':::'` | `true` | `false` |

   Opposite answers on three of four inputs, no error raised anywhere.

`\b` is the same trap in the other direction: word boundary in Java and RE2, backspace in
PostgreSQL ARE.

**This is a limitation of the decision, not an argument against it** — choosing PostgreSQL ARE as
normative would invert which users are surprised without reducing the surprise. What it does mean is
that the flavor choice has to be backed by something that acts on the pattern, and the cheap version
is a static check rather than a translator:

- **Recommended — a portability lint on literal patterns.** Patterns are string literals in the
  overwhelmingly common case, so they can be scanned at compile or plan time and checked against
  the tier model in `regex-portability.md`. Flag constructs outside Tier 1/Tier 2 (`[[:…:]]`, `\y`,
  lookaround, backreferences, `\p{…}`) with a diagnostic naming the target that cannot support
  them. This catches all three grades above, including the silent one, and needs no regex engine.
- **Rejected for now — per-dialect pattern translation.** Rewriting patterns between flavors is a
  regex transpiler, with a large correctness surface of its own. Revisit only if the lint proves
  insufficient.
- **Insufficient on its own — validating with `Pattern.compile`.** It catches grade 1 and nothing
  else; `[[:alpha:]]` compiles cleanly and still means the wrong thing.

Until a lint exists, this is a documentation obligation: `regex-portability.md` carries the tier
model and the trap list, and the function documentation points at it.

### Emission contract

Binding on every dialect implementation:

- A dialect implementing `matches` MUST emit an anchored, alternation-safe form — either the
  engine's native whole-string matching function, or an explicitly grouped `^(?:p)$` / `^(p)$`.
- A dialect implementing `regexpIndexOf` MUST report the **match position**, 0-based, with `-1` on
  no match. Searching for the matched *text* is not a valid implementation.
- Flags MUST be emitted as inline pattern prefixes (`(?i)`, `(?m)`, `(?s)`) wherever the engine's
  option-string equivalent is unreliable.

---

## Consequences

### Positive

- There is a single answer to "which behaviour is correct", so dialect wiring stops being a
  judgement call and manifest exclusions become classifiable: a divergence is either our emission
  defect or a genuine engine limit.
- The unportable set is small and principled — lookaround and backreferences, both impossible on
  RE2 — rather than an open-ended list.
- The transpiler's restrictions become removable rather than load-bearing.

### Negative / Limitations

- **Fixing the emission defects changes results for existing queries.** `matches(col,'a|b')`
  currently behaves as "starts with `a` or ends with `b`" on the substring-matching dialects, and
  `matches(col,'foo')` behaves as `contains` on Databricks and ClickHouse. Correcting these
  *shrinks result sets*. This ships as a fix with a release note, not behind a compatibility flag:
  the current behaviour is a defect, not a contract.
- Patterns using Java-only constructs (lookaround, backreferences, `\p{...}`, atomic groups) are
  unportable to RE2-family engines and must be documented as such rather than emulated.
- PostgreSQL and Redshift need `\b` → `\y` translation, since ARE reads `\b` as backspace.
- `regexpIndexOf` remains the one place where Legend and PostgreSQL deliberately differ, so every
  translation across that boundary carries a `±1`.

---

## References

- `docs/engineering/reference/regex-portability.md` — the tier model, the per-dialect emission
  table, and the current known-defect list
- `docs/pct/expected-failures-howto.md` — exclusion classification convention
- `legend-engine-core/.../core_functions_unclassified/string/regex/` — the six function definitions
- `.../legend-engine-pure-runtime-java-extension-compiled-functions-unclassified/.../FunctionsHelper.java`
  — the reference implementation
- `.../core_relational/relational/sqlQueryToString/extensionDefaults.pure` — the shared anchoring
  helpers
- `.../core_relational/relational/sqlDialectTranslation/toPostgresModel.pure` — the PostgreSQL-shaped IR
- `legend-engine-xts-sql/.../binding/fromPure/` — the SQL→Pure transpiler
