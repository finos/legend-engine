# Documentation Maintenance

Documentation lives alongside the code. Documentation changes follow the same PR
review process as code changes.

---

## 1. Rule: Update Docs in the Same PR as Code

| Type of code change | Required doc update |
|---------------------|---------------------|
| New module added | New module entry in [Module Reference](../reference/modules.md); new per-module `README.md` using the [template](../templates/module-readme-template.md) |
| Module renamed or removed | Update [Architecture Overview](../architecture/overview.md) and [Module Reference](../reference/modules.md) |
| New third-party library added | Update [Technology Stack](../reference/tech-stack.md) |
| New grammar section added | Update [Module Reference](../reference/modules.md) and [Contributor Workflow](../guides/contributor-workflow.md) |
| New store extension added | Update [Module Reference](../reference/modules.md) and [Key Java Areas](../architecture/key-java-areas.md) |
| New test convention established | Update [Testing Strategy](../testing/testing-strategy.md) |
| New Checkstyle rule added | Update [Coding Standards](../standards/coding-standards.md) |
| New domain concept or glossary term | Update [Domain & Key Concepts](../architecture/domain-concepts.md) |
| JDK or Maven version requirement changed | Update [Getting Started](../guides/getting-started.md) and [Technology Stack](../reference/tech-stack.md) |
| CI pipeline changed | Update [Build & CI Guide](../guides/build-and-ci.md) |
| Extension SPI changed | Update [Key Java Areas](../architecture/key-java-areas.md) and [Contributor Workflow](../guides/contributor-workflow.md) |

---

## 2. Ownership Model

| Documentation section | Owner | Backup |
|-----------------------|-------|--------|
| Architecture Overview & Module Reference | Core platform lead | Any senior engineer |
| Technology Stack | Build / platform lead | Core platform lead |
| Domain & Key Concepts | Domain modelling lead | Core platform lead |
| Getting Started Guide | Developer experience lead | Any engineer who recently onboarded |
| Build & CI Guide | Build / DevOps lead | Core platform lead |
| Contributor Workflow | Core platform lead | Any senior engineer |
| Coding Standards | Tech lead | Any senior engineer |
| Testing Strategy | QA / test lead | Core platform lead |
| Exploration Guide | Developer experience lead | Any senior engineer |
| Per-module READMEs | Module owner (see `CODEOWNERS`) | Module contributors |
| Documentation Plan & Maintenance | Tech lead | Project manager |

> **`CODEOWNERS` integration:** Add documentation ownership entries to
> `.github/CODEOWNERS` so that relevant owners are automatically requested as
> reviewers on PRs that touch documentation files.

---

## 3. Review Cadence

| Review type | Frequency | Participants |
|-------------|-----------|-------------|
| **Full docs review** | Quarterly | All doc owners |
| **Onboarding feedback session** | After every new engineer joins | New engineer + developer experience lead |
| **Architecture Decision Record (ADR)** | On-demand when major technical decisions are made | Tech lead + affected module owners |
| **Stale content audit** | As part of quarterly review | Tech lead |

### Periodic Review Checklist

- [ ] Are all module descriptions in [Module Reference](../reference/modules.md) still accurate?
- [ ] Are all dependency versions in [Technology Stack](../reference/tech-stack.md) still current?
- [ ] Does [Getting Started](../guides/getting-started.md) still work end-to-end for a clean checkout?
- [ ] Are there new tribal-knowledge items that should be documented?
- [ ] Are per-module READMEs complete for all priority modules?
- [ ] Are there any broken links in the docs? (run `markdown-link-check docs/**/*.md`)
- [ ] Has the CI pipeline changed since the last review?

---

## 4. PR Requirements for Documentation

The PR description (or template at `.github/PULL_REQUEST_TEMPLATE.md`) should include:

```markdown
### Documentation

- [ ] Documentation updated in `/docs/engineering` if behaviour, API, or setup changed.
- [ ] Per-module README updated if this module's purpose or dependencies changed.
- [ ] No hard-coded version numbers in docs (use root POM property names instead).
```

---

## 5. Documentation Tooling

The documentation is plain Markdown — no build step required. However, the following
tools are recommended:

| Tool | Purpose |
|------|---------|
| **markdownlint** | Lint Markdown for consistent style. Run: `npx markdownlint "docs/**/*.md"` |
| **markdown-link-check** | Detect broken links. Run: `npx markdown-link-check docs/engineering/README.md` |
| **MkDocs + Material theme** *(future)* | Convert the `/docs/engineering` folder to a searchable static site |

To install linting tools:

```bash
npm install -g markdownlint-cli markdown-link-check
markdownlint "docs/engineering/**/*.md"
markdown-link-check docs/engineering/README.md
```

---

## 6. Architecture Decision Records (ADRs)

For significant technical decisions, create an ADR in `docs/engineering/decisions/`:

```text
docs/engineering/decisions/
  ADR-001-dropwizard-over-spring-boot.md
  ADR-002-eclipse-collections-primary.md
  ...
```

### ADR Template

```markdown
# ADR-NNN: <Short Title>

**Status:** Accepted | Superseded by ADR-XYZ | Deprecated
**Date:** YYYY-MM-DD
**Deciders:** [names]

## Context
What is the issue we are addressing?

## Decision
What was decided?

## Consequences
What are the positive and negative consequences of this decision?
```

### Suggested first ADRs to write

1. Why Dropwizard (not Spring Boot)?
2. Why Eclipse Collections over JDK collections?
3. Why the Pure router runs in Pure (not Java)?
4. Why FreeMarker for SQL templating in execution plans?
5. The versioned protocol strategy (why `v1_24_0` packages)?
6. Why the `Extension` parameter is passed explicitly everywhere (not via a registry/singleton)?

---

*Back: [Testing Strategy](../testing/testing-strategy.md) | Next: [Documentation Plan](documentation-plan.md)*
