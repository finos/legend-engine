# Legend 4.102 backport checklist (from 5.1)

As of 2026-11-25. Compares `release-legend-release-4.102` with `legend-release-5.1` in
legend-pure, legend-engine, legend-shared and legend-sdlc. legend-depot and
legend-engine-ide-lsp have neither branch. "Pick" results come from an in-memory
cherry-pick simulation against 4.102, applied in the order listed.

Most of 5.1 stays put: it is the Java 17, Dropwizard 5, Jetty 12, pac4j 6, Jakarta EE and
Jackson 2.21 migration. 4.102 remains on Dropwizard 1.3, Jetty 9.4, pac4j 4.5 and Jackson 2.10.

## 1. Release order and version pins

Two fixes already on 4.102 branches haven't reached the stack yet. Release in dependency order.

- [ ] **legend-pure:** cherry-pick the pure fixes (section 3), then release **5.108.1**.
      The #1384 backport is on the branch but unreleased; engine 4.102 still pins pure 5.108.0.
- [ ] **legend-shared:** 0.37.1 is already released from the 4.102 branch and includes #277
      (blank authorizers). Engine and sdlc 4.102 still pin 0.37.0. Release 0.37.2 too if the
      #281 cookie fix is ported.
- [ ] **legend-engine:** bump `legend.pure.version` to 5.108.1 and `legend.shared.version` to
      0.37.1+, cherry-pick the engine fixes (section 2), release 4.152.4.
- [ ] **legend-sdlc:** bump `legend.engine.version` to the new 4.152.x and
      `legend.shared.version` to match, pick the sdlc items (section 4), release.

## 2. legend-engine (cherry-pick in this order)

| # | PR | Fix | Pick | Done |
|---|----|-----|------|------|
| 1 | [#5409](https://github.com/finos/legend-engine/pull/5409) | M2M: cast parenthesization for a CastProxy-wrapped conditional | clean | [ ] |
| 2 | [#5429](https://github.com/finos/legend-engine/pull/5429) | Integer/Float/Decimal `minus` generating uncompilable Java | clean | [ ] |
| 3 | [#5430](https://github.com/finos/legend-engine/pull/5430) | M2M: register `partition()` for the Java code generator | clean | [ ] |
| 4 | [#5395](https://github.com/finos/legend-engine/pull/5395) | M2M source trees: follow constructed instances through child mappings and conditionals | clean | [ ] |
| 5 | [#5434](https://github.com/finos/legend-engine/pull/5434) | Reuse the plan's interfaces for classes instantiated with `new` | clean after #5395 | [ ] |
| 6 | [#5442](https://github.com/finos/legend-engine/pull/5442) | Protocol: arithmetic and Boolean expressions inside constructors | clean | [ ] |
| 7 | [#5448](https://github.com/finos/legend-engine/pull/5448) | M2M: keep casts in child set property mappings | clean after #5434 | [ ] |
| 8 | [#5472](https://github.com/finos/legend-engine/pull/5472) | `exists` clause exploding into unnecessary joins | clean | [ ] |
| 9 | [#5473](https://github.com/finos/legend-engine/pull/5473) | Release let-variable connections when a plan runs through `executeWithArgs` | clean | [ ] |
| 10 | [#5480](https://github.com/finos/legend-engine/pull/5480) | Use interfaces instead of `_Impl` classes in casts and type checks | trivial conflict in `ServicePostValidationRunner.java`: keep 4.102's `javax` imports | [ ] |
| 11 | [#5483](https://github.com/finos/legend-engine/pull/5483) | **Regression from #5243 (on 4.102):** in-flow enum TDS columns become String | conflict in `pom.xml` only: keep 4.102's pure version | [ ] |
| 12 | [#5491](https://github.com/finos/legend-engine/pull/5491) | **Router scoping regressions from the shouldBeRouted change (39742ba832c, on 4.102)** | **manual merge** of `router_routing.pure`: conflicts with code (`NoSetRoutedValueSpecification`) removed by 4.102's #5407 Variant revert. Check new tests don't rely on Variant. | [ ] |
| 13 | [#5507](https://github.com/finos/legend-engine/pull/5507) | Router: inline functions with lexical scoping | after #5491, same file | [ ] |
| 14 | [#5505](https://github.com/finos/legend-engine/pull/5505) | M2M: enrich child sets reached through a subtype cast | clean after #5448 | [ ] |
| 15 | [#5513](https://github.com/finos/legend-engine/pull/5513) | Nil argument matching the Relation comparison overloads | clean | [ ] |
| 16 | [#5519](https://github.com/finos/legend-engine/pull/5519) | **Regression from #5220 (on 4.102):** restore pre-#5220 `eval()` property-mapping handling | clean after #5483 | [ ] |
| 17 | [#5502](https://github.com/finos/legend-engine/pull/5502) | Legend SQL: grammar serializer for logical predicates | clean | [ ] |
| 18 | [#5512](https://github.com/finos/legend-engine/pull/5512) | Databricks: forward caller parameters by name in table function calls | clean | [ ] |
| 19 | [#5496](https://github.com/finos/legend-engine/pull/5496) | Keep Markdown headings in documentation blocks as `'''` blocks | clean | [ ] |
| 20 | [#5509](https://github.com/finos/legend-engine/pull/5509) | *Optional:* split wide M2M JSON readers into a class chain (Java size limits) | clean | [ ] |
| 21 | [#5478](https://github.com/finos/legend-engine/pull/5478) + [#5504](https://github.com/finos/legend-engine/pull/5504) | *Optional:* join arity/type checks; #5504 downgrades mismatched types to a warning. Take both or neither. | clean | [ ] |
| 22 | [#5445](https://github.com/finos/legend-engine/pull/5445) | *Optional:* remove calls to deprecated pure methods | applies cleanly but **needs pure #1392** to compile | [ ] |

## 3. legend-pure (then release 5.108.1)

| PR | Fix | Pick | Done |
|----|-----|------|------|
| [#1388](https://github.com/finos/legend-pure/pull/1388) | Stop callee type parameters leaking through `=`-bound ColSpec arguments | clean | [ ] |
| [#1389](https://github.com/finos/legend-pure/pull/1389) | ColSpecArray shared type-parameter widening for `=` (reapplied #1375; skip #1375 and its revert #1376) | conflict in `docs/README.md` only | [ ] |
| [#1379](https://github.com/finos/legend-pure/pull/1379) | Relation: widen checks on `concatenate` ability | clean | [ ] |
| [#1380](https://github.com/finos/legend-pure/pull/1380) | Dependency convergence fix | clean | [ ] |
| [#1371](https://github.com/finos/legend-pure/pull/1371), [#1372](https://github.com/finos/legend-pure/pull/1372), [#1373](https://github.com/finos/legend-pure/pull/1373) | *Optional:* reference-ID version fixes (latent: only version 1 exists today) | clean | [ ] |
| [#1367](https://github.com/finos/legend-pure/pull/1367), [#1392](https://github.com/finos/legend-pure/pull/1392) | *Optional:* calendar creation and strict zone resolution in `ResultSetValueHandlers` (pairs with engine #5445) | **manual merge**: same code as the #1384 backport | [ ] |

## 4. legend-shared and legend-sdlc

| Repo | PR | Change | Pick | Done |
|------|----|--------|------|------|
| shared | [#281](https://github.com/finos/legend-shared/pull/281) | Session cookie host-only. 4.102 also sets `Domain=<server name>`, so the subdomain leak applies. The Kerberos "Negotiate again" half uses pac4j 6 APIs and may only fix a pac4j 6 regression: verify before porting. | **manual port** | [ ] |
| shared | [#263](https://github.com/finos/legend-shared/pull/263), [#264](https://github.com/finos/legend-shared/pull/264), [#267](https://github.com/finos/legend-shared/pull/267), [#274](https://github.com/finos/legend-shared/pull/274) | *Optional:* CI and release tooling | clean | [ ] |
| sdlc | [#1121](https://github.com/finos/legend-sdlc/pull/1121) | freemarker 2.3.30 → 2.3.35 | one-line `pom.xml` conflict | [ ] |
| sdlc | [#1148](https://github.com/finos/legend-sdlc/pull/1148) | Exclude `jcl-over-slf4j` from legend-engine-shared-core (check the conflict exists on 4.102) | clean | [ ] |
| sdlc | [#1133](https://github.com/finos/legend-sdlc/pull/1133), [#1134](https://github.com/finos/legend-sdlc/pull/1134) | *Optional:* CI only | clean | [ ] |

Release-branch naming (pure #1391, engine #5447, sdlc #1144, shared #275) and engine release
workflow changes (#5485, #5486) are tooling; take them only if 4.102 releases need them.

## 5. Do not backport

- **The migration itself:** engine #5412 (applies cleanly — easy to pull in by mistake), sdlc #1135, shared #269.
- **Fixes for regressions the migration caused:** engine #5439, sdlc #1145, shared #272, #273.
- **Dependency bumps tied to the new stack:** pure #1390, engine #5414, #5489, shared #270, sdlc #1152, sdlc engine 5.0.x bump.
- **Pure #1374 + engine #5474:** pure reverted #1374 on 5.1 (#1396); they cancel out.
- **Need pure versions 4.102 doesn't use:** pure #1394 + engine #5482 (Relation mapping joins), pure #1395 + engine #5494 (PCT manifests in IDE/LSP).
- **New features:** GraphQL Store #5454, #5503, #5506; BigQuery PCT #5108; `^$var(...)` copy syntax #5499;
  Legend SQL SSL/mTLS/metrics/secrets #5435, #5443, #5475, #5514; Snowflake App #5440;
  Binary/Varbinary #5449; MemSQL DDL #5452; data-cube #5415; batch plan generation #5511;
  opt-in M2M type generation #5517; MAINTAINERS/SECURITY docs.

## 6. Already on 4.102 under a different commit

| 5.1 PR | 4.102 equivalent |
|--------|------------------|
| pure #1383 | #1384 |
| engine #5411 | #5416 |
| engine #5431 | #5433 |
| engine #5422 | #5425 |
| engine #5418 | #5421 |
| sdlc ModelGenerationRunner change | same patch |
| shared #268 | #278 |
| shared #280 | #277 |

4.102 also deliberately reverts engine #5407 (Variant) and #5265 (multiple `exists()` filters); keep those reverts.
