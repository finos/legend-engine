# Module README Template

> Copy this template into a new `README.md` at the root of any module that lacks one.  
> Fill in every section; delete placeholder text.  
> If a section genuinely does not apply (e.g. a Pure-only module has no REST endpoints),  
> replace the section body with `N/A` rather than deleting the heading.

---

## `<module-artifact-id>`

**Parent module:** `<parent-artifact-id>`  
**Maven coordinates:** `org.finos.legend.engine:<module-artifact-id>`

## Purpose

> _One paragraph. What problem does this module solve? What would break or be impossible without it?_

## Key Concepts

> _Bullet list of 3–6 domain concepts introduced or implemented by this module._

- **ConceptA** — brief explanation
- **ConceptB** — brief explanation

## Module Structure

> _Describe the sub-modules (if this is a multi-module POM) or the main source packages._

| Sub-module / Package | Purpose |
|----------------------|---------|
| `sub-module-a` | ... |
| `sub-module-b` | ... |

## Entry Points

> _List the most important classes/functions a developer would interact with directly._

| Class / Pure function | Role |
|-----------------------|------|
| `org.finos.legend.engine.XxxClass` | Main entry point for ... |
| `meta::some::pure::function` | Pure-side entry point for ... |

## Extension Points

> _Describe any SPI interfaces this module defines or consumes._

| SPI Interface | How to implement | ServiceLoader file |
|---------------|------------------|--------------------|
| `XxxExtension` | Implement interface, register via ServiceLoader | `META-INF/services/...XxxExtension` |

## REST Endpoints

> _List HTTP endpoints exposed by this module (if any)._

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/pure/v1/xxx/yyy` | ... |

## Dependencies

> _List the key **intra-project** dependencies (other legend-engine modules)._  
> For third-party library dependencies, refer to [tech-stack.md](../reference/tech-stack.md).

- `legend-engine-language-pure-compiler` — needs `PureModel` for ...
- `legend-engine-executionPlan-execution` — ...

## Testing

> _How are the tests in this module run? What infrastructure is needed?_

```bash
# Run all tests
mvn test -pl <relative-path-to-module>

# Run a specific test
mvn test -pl <relative-path-to-module> -Dtest=TestClassName
```

**Test infrastructure required:**

- [ ] None (pure unit tests)
- [ ] H2 in-memory database
- [ ] Docker (Testcontainers)
- [ ] External cloud credentials

## Known Limitations / TODOs

> _List known issues, missing features, or tech-debt items._

- [ ] TODO: ...

## Related Documentation

- [Architecture Overview](../architecture/overview.md)
- [Key Java Areas](../architecture/key-java-areas.md)
- [Key Pure Areas](../architecture/key-pure-areas.md)
- _(link to any existing `/docs/` content relevant to this module)_
