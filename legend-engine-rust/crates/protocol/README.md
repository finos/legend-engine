# legend-pure-parser-protocol

Protocol v1 JSON model and bidirectional AST conversion for the Legend Pure parser.

## Purpose

This crate is the **serialization boundary** between the parser's AST and the Java Legend Engine protocol. It provides:

1. **Protocol v1 model** — Rust structs with `serde` that mirror `org.finos.legend.engine.protocol.pure.m3`
2. **AST → Protocol conversion** — transforms the parser AST into the protocol model for JSON emission
3. **Protocol → AST conversion** — reconstructs AST `SourceFile` from protocol JSON (for round-tripping protocol JSON back to source)

```
Source → Parser → AST ←→ Protocol v1 Model ←→ JSON bytes (serde_json)
```

This is the **only crate** in the workspace that depends on `serde`/`serde_json`. The AST crate remains a pure data model with zero serialization concerns.

## Module Structure

```
src/
├── lib.rs                    Crate root, re-exports
└── v1/                       Protocol v1 model (mirrors Java m3 package)
    ├── mod.rs
    ├── source_info.rs        SourceInformation
    ├── multiplicity.rs       Multiplicity
    ├── generic_type.rs       GenericType, PackageableType
    ├── annotation.rs         StereotypePtr, TagPtr, TaggedValue
    ├── property.rs           Property, QualifiedProperty, Constraint, DefaultValue
    ├── value_spec.rs         ValueSpecification enum + all variant structs
    ├── element.rs            PackageableElement enum + all element structs
    ├── context.rs            PureModelContextData, Protocol, Section
    ├── convert.rs            AST → Protocol conversion (fallible, returns Result)
    └── from_protocol.rs      Protocol → AST conversion
```

> **Note:** `convert_source_file()`, `convert_element()`, and `convert_expression()` all
> return `Result<T, serde_json::Error>` — callers must handle serialization errors.

## Java → Rust Type Mapping

| Java Protocol Class | JSON `_type` | Rust Struct |
|---|---|---|
| `PackageableElement` | (abstract) | `PackageableElement` enum |
| `Class` | `"class"` | `ProtocolClass` |
| `Enumeration` | `"Enumeration"` | `ProtocolEnumeration` |
| `Association` | `"association"` | `ProtocolAssociation` |
| `Profile` | `"profile"` | `ProtocolProfile` |
| `Function` | `"function"` | `ProtocolFunction` |
| `Measure` | `"measure"` | `ProtocolMeasure` |
| `SectionIndex` | `"sectionIndex"` | `ProtocolSectionIndex` |
| `ValueSpecification` | (abstract) | `ValueSpecification` enum |
| `CInteger` | `"integer"` | `CInteger` |
| `CString` | `"string"` | `CString` |
| `CBoolean` | `"boolean"` | `CBoolean` |
| `CFloat` | `"float"` | `CFloat` |
| `CDecimal` | `"decimal"` | `CDecimal` |
| `CDateTime` | `"dateTime"` | `CDateTime` |
| `CStrictDate` | `"strictDate"` | `CStrictDate` |
| `CStrictTime` | `"strictTime"` | `CStrictTime` |
| `CLatestDate` | `"latestDate"` | `CLatestDate` |
| `AppliedFunction` | `"func"` | `AppliedFunction` |
| `AppliedProperty` | `"property"` | `AppliedProperty` |
| `Collection` | `"collection"` | `ProtocolCollection` |
| `Variable` | `"var"` | `Variable` |
| `LambdaFunction` | `"lambda"` | `LambdaFunction` |
| `PackageableElementPtr` | `"packageableElementPtr"` | `ProtocolPackageableElementPtr` |
| `GenericTypeInstance` | `"genericTypeInstance"` | `ProtocolGenericTypeInstance` |
| `EnumValue` (VS) | `"enumValue"` | `ProtocolEnumValue` |
| `KeyExpression` | `"keyExpression"` | `ProtocolKeyExpression` |
| `ClassInstance` | `"classInstance"` | `ClassInstance` |

## Design Decisions

See [DESIGN.md](DESIGN.md) for the full design rationale. Key decisions:

| Decision | Choice | Rationale |
|---|---|---|
| Empty vectors | **Skip** with `skip_serializing_if` | Java defaults missing lists to `emptyList()` |
| Enum structure | **Struct-per-variant** | Avoids name collisions, enables isolated testing |
| Common fields | **Repeated per struct** | `flatten` + `tag` is fragile in serde |
| Extensions | **Custom `Deserialize`** | `serde(other)` only supports unit variants |
| Null `Option` fields | **Skip when `None`** | Validated per-field via TDD |

## Extension Model

The Java protocol uses two extension points, both mirrored in Rust:

### PackageableElement Extensions

Java registers subtypes via `PureProtocolExtension.getExtraProtocolSubTypeInfoCollectors()` (SPI). In Rust, core domain elements are hardcoded enum variants. Unknown `_type` values are handled via a custom `Deserialize` that preserves the JSON as `serde_json::Value`.

### ClassInstance Extensions

Java's `ClassInstance` holds `type: String` + `value: Object` with a class map from `PureProtocolObjectMapperFactory.getClassInstanceTypeMappings()`. In Rust, `ClassInstance` stores `type_name: String` + `value: serde_json::Value` (opaque). Typed deserialization comes in later phases.

## Testing Strategy

Tests validate **JSON shape compatibility** with the Java parser:

1. **Serialization shape tests** — assert exact field names match Java output
2. **Discriminator tests** — verify `_type` values (e.g., `"Enumeration"` with capital E)
3. **Skip-if-empty tests** — verify empty lists are omitted from JSON
4. **Snapshot tests** (insta) — full JSON comparison against Java-generated golden files

See [DESIGN.md](DESIGN.md) § TDD Strategy for details.
