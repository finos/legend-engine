# Known release failure signatures

Append to this file every time a release failure is diagnosed. The value of
`release-triage` is entirely in how complete this list is.

Each entry: **signature** (what to grep for), **verdict** (real / environmental),
**action**.

---

## Environmental — no code fix, do not "fix" the branch

### Databricks: stale Delta table location

- **Signature:** `DELTA_CREATE_TABLE_WITH_NON_EMPTY_LOCATION`, usually with
  `Cannot create table ('`hive_metastore`.`ORDER_SCHEMA`.`ORDER_TABLE`')` and
  `The associated location ('dbfs:/user/hive/warehouse/order_schema.db/order_table') is not empty and also not a Delta table`.
- **Where:** `Test Module - databricks` job, class
  `Test_Relational_Databricks_Semistructured`. Hits ~13 distinct tests
  (`testSemiStructuredMatch*`, `testSemiStructuredTypeNameFunctionUsage*`),
  each counted twice in the surefire tally.
- **Verdict:** Environmental. Leftover state on the **shared** Databricks
  warehouse — nothing in the repo can fix it, and a rerun fails identically
  until the warehouse is cleaned.
- **Action:** Escalate to the owner of the shared Databricks warehouse.
  > OWNER: not yet recorded — fill this in. Another team owns the warehouse.
  Do not modify test code or PCT manifests in response to this.
- **First seen:** run 34857019489, legend-engine, 2026-09-14 (failed the 4.143.1 release).

### Cloud-store credentials / connectivity

- **Signature:** auth/timeout/connection-refused errors from Snowflake, BigQuery,
  Databricks, Spanner, MemSQL, Postgres jobs — as opposed to assertion failures.
- **Verdict:** Environmental. These stores run only under `pct-cloud-test` with
  CI secrets; they cannot be reproduced locally.
- **Action:** Report. A rerun is reasonable if the failure looks transient
  (timeout, 5xx); a credentials error needs the secret rotated.

---

## Real — the branch needs a fix before re-releasing

### PCT expected-error drift

- **Signature:** a single errored test under a `Test_Relational_<Store>_PCT`
  class, message comparing an expected error string against an actual one.
  Example: `meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_`
  expecting `The first parameter requires the ("NUMERIC"` but the dialect now
  returns `[DATATYPE_MISMATCH.UNEXPECTED_INPUT_TYPE]`.
- **Verdict:** Real, but usually not a code bug — the *dialect* changed its
  error text, so the adapter's manifest is stale.
- **Action:** Update the `expectedError` in
  `.../legend-engine-xt-relationalStore-<store>-PCT/src/main/resources/pct-manifests/<adapter>/<Group>_manifest.json`.
  The test runner prints a copy-paste snippet on failure — use it. See
  `docs/pct/expected-failures-howto.md`.
- **Example:** legend-engine #5186, backported as #5210 for 4.143.1.

### Compilation / unit-test failure in a non-cloud module

- **Signature:** failures in H2, DuckDB, Java-binding, core, server modules.
- **Verdict:** Real. These are the default-profile stores that run on every PR,
  so a failure here means something genuinely broke.
- **Action:** Fix on master, backport to the release branch, then re-release.

---

## Central publishing failures

### 413 Payload Too Large

- **Signature:** a bare `413` from the Portal upload, no API-level error body.
- **Cause:** a bundle over the Portal's 1GiB cap; nginx rejects it before the API
  sees it. The full legend-engine artifact set is ~1.7GB.
- **Action:** Handled by `.github/scripts/stage-and-split.sh` splitting into
  waves. If it recurs, the split thresholds need revisiting.

### Parent POM not resolvable during validation

- **Signature:** Central validation errors about an unresolvable `<parent>`.
- **Cause:** Central validates each bundle independently and must build an
  effective POM. A parent sitting in a sibling bundle resolves from neither.
- **Action:** This is why the engine release publishes in two waves — wave 1 is
  every coordinate that is somebody's parent, published and confirmed readable
  on repo1 before wave 2 uploads. Broke the 4.139.1 release. Do not reorder.

### Version already on Central

- **Signature:** `validate` job error, or a 200 from the Central coordinate URL.
- **Verdict:** Terminal for that version number. Central is immutable — a
  published version can never be replaced or withdrawn.
- **Action:** Bump to the next patch version. Do not attempt to republish.
