# Legend Engine — Developer Documentation: Phased Plan

> **Owner:** Engineering team  
> **Last updated:** March 2026  
> **Purpose:** Provide a structured, phased roadmap to create and maintain comprehensive  
> developer-facing documentation for the `legend-engine` multi-module Maven project.
>
> **See also:** [Documentation Tasks Backlog](documentation-tasks.md) — granular open tasks
> and known gaps that feed into the phases below.

---

## 0. Guiding Principles

1. **Don't duplicate.** The `legend-pure` documentation already covers the Pure language itself,
   the M3/M4 metamodel, and fundamental Pure semantics. This plan focuses on what `legend-engine`
   *adds on top*: Java infrastructure, protocol layers, store integrations, extension points, etc.
2. **Code is the primary source of truth.** Documentation should point readers to real classes
   and Pure files rather than re-state what the code already shows.
3. **Living documents.** Each document should be updated alongside the code changes that affect it
   (same PR is ideal, separate follow-up PR is acceptable).
4. **Discoverable.** All documents live under `docs/engineering/` so they are indexed by GitHub
   and can be incorporated into tools such as MkDocs, Backstage, or Docusaurus with minimal effort.

---

## 1. Scope of Documentation Areas

The plan is organised around seven areas, deliberately mirroring the user's request:

| # | Area | Primary deliverable file |
|---|------|--------------------------|
| 1 | Project Overview & Architecture | `architecture/overview.md`, `reference/modules.md` |
| 2 | Key Dependencies & Technology Stack | `reference/tech-stack.md` |
| 3 | Key Concepts & Domain Model | `architecture/key-java-areas.md`, `architecture/key-pure-areas.md`, `architecture/domain-concepts.md` |
| 4 | Developer Getting-Started Guide | `guides/getting-started.md` |
| 5 | Coding Standards & Style Guide | `standards/coding-standards.md` |
| 6 | Testing Requirements & Strategy | `testing/testing-strategy.md` |
| 7 | Exploration & Discovery | `guides/exploration.md` |

---

## 2. Phased Timeline

### Phase 1 — Foundation (Weeks 1–2)

**Goal:** Make it possible for a brand-new developer to build and run the project.

| Deliverable | File | Key tasks |
|-------------|------|-----------|
| Getting-Started Guide (v1) | `guides/getting-started.md` | Document JDK 11, Maven 3.6+, clone, `mvn install`, Server launch, Pure IDE launch |
| Architecture Overview (v1) | `architecture/overview.md` | Hand-drawn or ASCII component diagram; identify the 5 core pipelines |
| Module Reference (v1) | `reference/modules.md` | Tabular listing of every top-level module group with a one-line purpose; link to sub-module README stubs |

**Definition of Done:** A developer with Java/Maven background can clone, build, start the server, and open Swagger UI following these docs alone.

---

### Phase 2 — Core Concepts (Weeks 3–5)

**Goal:** Enable a developer to understand *how* the engine works end-to-end.

| Deliverable | File | Key tasks |
|-------------|------|-----------|
| Key Java Areas (v1) | `architecture/key-java-areas.md` | Document Grammar layer, Compiler (`PureModel`), Execution Plan generation (`PlanGenerator`), Plan execution (`PlanExecutor`), Extension SPI, Relational store executor |
| Key Pure Areas (v1) | `architecture/key-pure-areas.md` | Document Router, ExecutionPlan metamodel, GraphFetch, Milestoning, TDS/Relation, M2M mapping chain, Service metamodel |
| Technology Stack | `reference/tech-stack.md` | Inventory all major third-party libraries from root `pom.xml`; capture rationale where known |

**Definition of Done:** A developer can trace a query from the HTTP endpoint through grammar parsing → compilation → plan generation → execution for both relational and M2M stores.

---

### Phase 3 — Extension & Contribution (Weeks 6–8)

**Goal:** Enable a developer to add a new store, format, or function-activator.

| Deliverable | File | Key tasks |
|-------------|------|-----------|
| Coding Standards | `standards/coding-standards.md` | Document Checkstyle rules, naming conventions, PR checklist, logging standards |
| Testing Strategy | `testing/testing-strategy.md` | Document test pyramid, PCT framework, how to run unit / integration / full suite, CI matrix |
| Module README template | `templates/module-readme-template.md` | Standardised per-module template; retrofit to 10 highest-impact modules first |
| Architecture Overview (v2) | `architecture/overview.md` | Add extension-point diagram (SPI / ServiceLoader pattern) |

**Definition of Done:** A developer can implement a minimal new external-format extension following the testing guide and have it pass CI.

---

### Phase 4 — Deep Dives & Specialist Areas (Weeks 9–12)

**Goal:** Capture tribal knowledge in high-complexity subsystems.

| Deliverable | Notes |
|-------------|-------|
| Relational store deep dive | SQL generation pipeline, dialect handling, connection-pool auth flow |
| GraphFetch deep dive | Batching strategy, cross-store execution, `graphFetchChecked` quality layer |
| Persistence DSL deep dive | Trigger/Service/Target model; relationship to ETL platforms |
| Change-Token deep dive | Upcast/downcast code generation; versioning strategy |
| REPL deep dive | DataCube integration, DuckDB local execution, autocomplete |
| Authentication deep dive | Vault hierarchy, `CredentialBuilder`, GCP/AWS federation flows |

Each deep dive lives in a sub-file under `docs/engineering/deep-dives/`.

---

### Phase 5 — Continuous Maintenance (Ongoing)

- Each PR that changes a public API, adds a module, or changes an extension SPI **must** update the
  relevant `docs/engineering/` file.
- Quarterly review: check for stale content, update version numbers, re-run exploration scripts.
- Annual review: revisit architecture diagram for accuracy; re-evaluate tooling.

---

## 3. Suggested Tools

| Purpose | Tool |
|---------|------|
| Dependency mapping | `mvn dependency:tree`, `mvn org.apache.maven.plugins:maven-dependency-plugin:analyze` |
| Module graph | `mvn com.github.ferstl:depgraph-maven-plugin:graph -DshowGroupIds=true` (already in root POM) |
| Architecture enforcement | ArchUnit tests (`org.finos.legend` package rules) |
| Static analysis | Checkstyle (already configured), SpotBugs (can be added) |
| Coverage reporting | JaCoCo (already in root POM as `jacoco.maven.plugin.version`) |
| Structural analysis | JDepend, Structure101 (commercial) |
| Documentation site | MkDocs + Material theme; or Backstage TechDocs |
| Diagrams-as-code | Mermaid (renders in GitHub markdown natively) |
| Tribal knowledge | ADR (Architecture Decision Records) — add `docs/engineering/decisions/` |

---

## 4. Exploration Recipes

### 4.1 Map all Maven modules

```bash
# List every artifact ID and its parent
find . -name pom.xml -not -path "*/target/*" \
  | xargs grep -l "<artifactId>" \
  | sort \
  | xargs -I{} sh -c 'echo "---"; grep -E "<artifactId>|<parent>" {} | head -6'
```

### 4.2 Dependency tree for a single module

```bash
mvn dependency:tree -pl legend-engine-core/legend-engine-core-base/\
legend-engine-core-executionPlan-generation/legend-engine-executionPlan-generation \
-Dincludes=org.finos.legend.engine
```

### 4.3 Find all SPI / extension points

```bash
# Java ServiceLoader provider files
find . -path "*/META-INF/services/org.finos*" -not -path "*/target/*"

# CompilerExtension implementations
grep -r "implements CompilerExtension" --include="*.java" -l | grep -v target

# StoreExecutorBuilder implementations  
grep -r "implements StoreExecutorBuilder" --include="*.java" -l | grep -v target
```

### 4.4 Understand the Pure extension registration

```bash
# Every Extension class in Pure
grep -r "meta::pure::extension::Extension" --include="*.pure" -l | grep -v target | sort
```

### 4.5 Count lines of code per module group

```bash
find . -name "*.java" -not -path "*/target/*" \
  | sed 's|./||' | awk -F'/' '{print $1}' \
  | sort | uniq -c | sort -rn | head -20
```

### 4.6 Find all HTTP API entry points (JAX-RS resources)

```bash
grep -r "@Path" --include="*.java" -l | grep -v target \
  | xargs grep -l "@GET\|@POST\|@PUT\|@DELETE" | sort
```

---

## 5. Module README Template

Each module should have a `README.md` following the template in
[`module-readme-template.md`](../templates/module-readme-template.md). Priority order for retrofitting:

1. `legend-engine-core` sub-modules (compiler, grammar, executionPlan-generation, executionPlan-execution)
2. `legend-engine-xts-relationalStore` (largest Pure + Java surface)
3. `legend-engine-xts-service`
4. `legend-engine-xts-persistence`
5. `legend-engine-xts-authentication`
6. `legend-engine-config/legend-engine-server`
7. `legend-engine-config/legend-engine-repl`

---

## 6. Interview Checklist (Tribal Knowledge Capture)

When interviewing existing team members, use the following prompts:

- What are the most common mistakes new contributors make?
- Which modules are the most complex / have the most hidden invariants?
- What is the intended evolution direction for `[subsystem]`?
- Are there any implicit contracts between modules that are not expressed in code?
- Which tests are the most brittle and why?
- What is the story behind `[specific design choice]`?
- Which external systems does the engine call at runtime that are not in source control?

Record answers as ADRs (Architecture Decision Records) in `docs/engineering/decisions/`.
