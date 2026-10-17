# Protocol V1: Design Decisions

This document records the design decisions made for the protocol v1 model. It serves as a reference for contributors and future maintainers.

## Architecture

```
Source → Parser → AST ←→ Protocol v1 Model ←→ JSON bytes
                         (this crate)
```

The protocol model sits between the parser AST and the JSON wire format:
- **AST → Protocol**: Operators become `AppliedFunction`, member access becomes `AppliedProperty`, etc.
- **Protocol → JSON**: `serde_json` handles serialization/deserialization automatically via derive macros.
- **Protocol → AST** (future): Reverse conversion for round-trip scenarios.

## Design Decisions

### 1. Struct-per-Variant

Each `PackageableElement` and `ValueSpecification` enum variant wraps a **named struct**:

```rust
#[serde(tag = "_type")]
pub enum PackageableElement {
    #[serde(rename = "class")]
    Class(ProtocolClass),
    // ...
}
```

**Why not inline fields?** Three reasons:
- **Name collision**: `ValueSpecification::String { value: String }` requires `std::string::String` everywhere
- **Testability**: Individual structs can be unit-tested in isolation
- **Readability**: A 300-line enum with inline fields is hard to navigate

### 2. Empty Vectors Are Omitted

All `Vec` fields use `#[serde(default, skip_serializing_if = "Vec::is_empty")]`.

This is an **intentional deviation** from Java's Jackson behavior (which serializes empty lists as `[]`). It's safe because Java protocol classes initialize list fields to `Collections.emptyList()` — Jackson uses this initializer when the field is absent from JSON, producing the same result.

### 3. No `#[serde(flatten)]`

Common fields (`package`, `name`, `sourceInformation`) are **repeated** in each struct variant rather than shared via `#[serde(flatten)]`.

`flatten` + `#[serde(tag)]` has known issues in serde:
- Incompatible with `deny_unknown_fields`
- Poor error locations during deserialization
- Performance overhead from intermediate buffering

The repetition is mechanical and predictable.

### 4. Extension Handling

#### PackageableElement

`#[serde(other)]` only works on **unit variants** — it cannot capture the unknown `_type` string or payload data. For the extension catch-all, we'll implement a custom `Deserialize` that:
1. Reads the `_type` field
2. Attempts to match known variants
3. Falls back to storing the entire JSON as `serde_json::Value`

This is only needed for deserializing externally-produced JSON (round-trip), not for our own serialization output.

#### ClassInstance (ValueSpecification)

`ClassInstance` stores `type_name: String` + `value: serde_json::Value` (opaque). This mirrors Java's `PureProtocolObjectMapperFactory.getClassInstanceTypeMappings()` which maps type strings to classes. Typed deserialization via a registry comes in later phases.

### 5. Null Option Fields Are Omitted

`Option<T>` fields use `#[serde(skip_serializing_if = "Option::is_none")]`. When `None`, the field is omitted from JSON.

This matches common Jackson behavior for most fields (default `null` values are typically not written). Exact per-field behavior is validated via TDD against Java-generated golden files.

## Serde Conventions

| Pattern | Attribute | Example |
|---|---|---|
| camelCase field names | `#[serde(rename_all = "camelCase")]` | `source_id` → `sourceId` |
| Type discriminator | `#[serde(tag = "_type")]` | `{"_type": "class", ...}` |
| Variant name | `#[serde(rename = "class")]` | Enum variant → JSON string |
| Skip empty vec | `#[serde(default, skip_serializing_if = "Vec::is_empty")]` | Omit `[]` |
| Skip None | `#[serde(skip_serializing_if = "Option::is_none")]` | Omit `null` |
| Reserved keyword | `#[serde(rename = "package")]` on `_package` field | `package` is Rust keyword |

## TDD Strategy

### Test Categories

1. **JSON shape tests** — Construct a Rust struct, serialize to `serde_json::Value`, assert exact field names and structure match Java output
2. **Discriminator tests** — Verify `_type` values serialize correctly (e.g., `"Enumeration"` with capital E)
3. **Skip-if-empty tests** — Verify empty lists are omitted from JSON
4. **Roundtrip tests** — Serialize → deserialize → assert equal
5. **Snapshot tests** (insta) — Full JSON comparison against Java-generated golden files

### Test Order (Leaves to Roots)

1. `SourceInformation` — simplest flat struct
2. `Multiplicity` — tests `upperBound: null` for infinite
3. `PackageableType` + `GenericType` — nested type references
4. `StereotypePtr`, `TagPtr`, `TaggedValue` — annotations
5. `Property`, `QualifiedProperty`, `Constraint`, `DefaultValue` — class members
6. Primitive `ValueSpecification` variants — `CInteger`, `CString`, `CBoolean`, etc.
7. Complex `ValueSpecification` — `AppliedFunction`, `AppliedProperty`, `ProtocolCollection`, `Variable`, `LambdaFunction`
8. `PackageableElement` variants — `ProtocolClass`, `ProtocolEnumeration`, etc.
9. `PureModelContextData` — top-level container

### Test File Structure

```
crates/protocol/
├── src/v1/
│   ├── source_info.rs        (struct + inline #[cfg(test)] mod tests)
│   ├── multiplicity.rs       (struct + inline tests)
│   └── ...
├── tests/                    (integration/snapshot tests)
│   ├── snapshots/            (insta snapshot files)
│   └── java_compat.rs        (golden file comparison tests)
└── testdata/                 (JSON captured from Java parser)
```

## V1 Phases

### Phase 1 (Current): Core Protocol Model
- All grammar-produced domain elements and value specifications
- AST → Protocol conversion
- SectionIndex for section metadata
- Extension variant for unknown PackageableElement types (pass-through)

### Later Phases Within V1
- GraphFetchTree, Path typed ClassInstance deserialization
- DSL-specific ClassInstance types (TDS col specs, olap, aggregate values)
- Function test suites (complex deserialize structure)
- ClassInstanceWrapper → ClassInstance mapping
- Protocol → AST reverse conversion
- Protocol converters (post-deserialization transforms)
