# Protocol JSON

How the converters write Protocol v1 JSON: once, from typed data.

## The rule

- Library code builds protocol JSON from typed `#[derive(Serialize)]` structs and
  enums, one per engine protocol class. `serde_json::json!` is not used:
  `clippy.toml` disallows it, and `cargo lint-lib` (library code only) denies it.
  Tests keep it for the JSON they expect.
- Nothing is serialized twice. Never `serde_json::to_value` a struct that holds a
  `serde_json::Value`, and never interpolate one into another: both deep-copy it.
  Embed typed values; where JSON is opaque (below), move it.

Why: `json!` turns every interpolated expression into `to_value(&expr)`, so
`json!({"k": existing_value})` copies `existing_value`. With the workspace's
`arbitrary_precision`, the copy also parses every number in it again. Converters
that nested `json!` (schema → table → column → operation, a position at every
node) copied each subtree once per level above it. Building the same JSON from
typed structs, with output byte-identical, halved the Rust side of `parseModel`:

| input | before | after |
|---|---|---|
| EMIT sources (271 files) | 33 ms | 17 ms |
| engine corpus (3,560 sections) | 170 ms | 91 ms |
| a 568 KB generated relational model | 167 ms | 56 ms |

## Writing a converter

Each struct is named after its engine class with a `Json` suffix, borrows from the
AST, and is built by a `From<&Ast>` impl or a function that can fail:

```rust
/// The engine's `RootRelationalClassMapping`.
#[derive(Serialize)]
#[serde(tag = "_type", rename = "relational", rename_all = "camelCase")]
struct ClassMappingJson<'a> {
    class: &'a str,
    root: bool,
    property_mappings: Vec<PropertyMappingJson<'a>>,
    source_information: SourceInformation,
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<&'a str>,
}
```

Each protocol shape has its serde recipe:

| shape | how |
|---|---|
| a class's `_type` | `#[serde(tag = "_type", rename = "…")]` on the struct |
| a class's subclasses | an enum: `#[serde(tag = "_type")]`, a variant per subclass with `#[serde(rename = "…", rename_all = "camelCase")]` |
| a `_type` decided while converting | a `&'static str` field, `#[serde(rename = "_type")]` |
| field names | `rename_all = "camelCase"`; `type`, acronyms (`serverURLs`) and others by `#[serde(rename = "…")]` |
| a field the walker leaves null, so absent | `Option` with `skip_serializing_if = "Option::is_none"` |
| a field written as `null` | plain `Option` |
| a list or flag written only when set | `skip_serializing_if = "Vec::is_empty"` or `"std::ops::Not::not"` |
| alternatives with no `_type` of their own (a pointer or an embedded value) | `#[serde(untagged)]` enum |
| an always-empty list (`postProcessorWithParameter`, `stores`) | a `[(); 0]` field |
| fields keyed by names (service store parameters) | a small `Serialize` impl over `serialize_map` |
| lambdas and expressions | typed `ValueSpecification` (`convert_expression_typed`) |
| positions | typed `SourceInformation` (`SourceInformation::from`), written as one string: `sourceId:line:column-column` on one line, `sourceId:line:column-line:column` otherwise |
| a large enum variant (pedantic `large_enum_variant`) | box it |

Three things to watch:

- **An object that also appears in a list of another class must tag itself.** An
  internally tagged enum writes `_type` for its own variants only. Execution
  environments' `singleExecutionParameters` are written both on their own and
  inside `multiExecutionParameters`, so the struct carries its own
  `tag`/`rename`, and the list of alternatives is `untagged`.
- **Conversions that can fail run before serialization.** `Serialize` can't return
  a `ConvertError`, and an unsupported input must stay unsupported (the engine
  falls back on it), so convert into the typed structs first, then serialize.
- **Keep the conversion order.** When two parts can each fail, the error the
  engine sees is the first one converted. Convert them in the order the code
  (and the walker) did.

### Walker-shaped records

Sections whose protocol has dozens of small classes (persistence, data spaces,
diagrams, generations, …) build `protocol::v1::record::Record`s, as their walkers
do. `record.converted()?` converts what other grammars read in them: lambdas,
paths, primitive values, embedded data, assertions. It returns a `RecordJson`
that serializes the fields in order. The element's converter adds the element's
own fields with `.with(name, value)`:

```rust
let json = element
    .record
    .converted()?
    .with("package", package)
    .with("name", element.name.value.as_str());
```

## Plug-in JSON and opaque JSON

**An element's JSON is written once, as text.** `ElementProtocol::convert` and
`MappingElementGrammar::convert` return it as `Box<RawValue>`, written with
`serde_json::value::to_raw_value(&typed)`. A struct holding it (a mapping's
class mappings, the api's `{"elements":[…]}`) writes it verbatim.

**Never pass a `RawValue` through `to_value` or into a `Value`.** `serde_json`
parses it back there (`RawValueEmitter`, `value/ser.rs`): it works, and quietly
costs a parse. If a change at a boundary shows no gain, look for
`serde_json::from_str` under it in a profile.

**Some JSON is typed by no struct of the core:**

- `ClassInstance.value`: islands (graph fetch trees, column specs, navigation
  paths, TDS, SQL, store accessors).
- `ConnectionValueGrammar`, `EmbeddedDataProtocol` and `TestInputGrammar`'s
  JSON, and records' `Value::Json`.

There, build it once from typed structs (`serde_json::to_value(&typed)`) and hold
it as a `Value` in the typed struct around it. That struct is written to text, so
the value is never copied.

**Aggregation-aware mappings edit what another grammar wrote.** They name their
nested mappings, reposition their lambdas and read the main mapping's property
mappings, so they read the nested grammar's text back into a `Value`. That's the
one deliberate re-read. Edit such JSON with `Value`'s accessors and `Value::from`,
never `json!`. Its positions are strings: edit them with
`source_info::edit_positions`, which finds every field named `sourceInformation`
or `*SourceInformation` (islands' opaque JSON included) and reads and writes it
through the typed `SourceInformation`. A mapping test's query uses it too, to
name its positions' source after the mapping, as the engine's walker does.

**The `api` crate keeps two paths:**

- `parse_section_json`, `parse_expression_json` and `parse_model_json_bytes`
  are what JNI calls. They write each element's text once. A model's section
  index takes its element paths from the AST, not from the JSON.
- **A model is written in one buffer.** `parse_model_json_bytes` writes every
  element in place through `convert_into` (`ElementProtocol`,
  `MappingElementGrammar`), rather than into a `Box<RawValue>` of its own that
  is then copied. `convert_into` copies `convert`'s text by default. Converters
  of large elements override it: core elements, databases, relational class
  mappings, and mappings. `MappingProtocol::convert_into` writes the mapping
  field by field around its elements, so it must stay in step with
  `MappingJson`; `tests/model_json_in_place.rs` compares the two over the
  corpus.
- `parse_section`, `parse_model` and `parse_expression` read that text back into
  values, for tests and tools.

Text follows the structs' field order, so `_type` comes first wherever a struct
or enum carries a `tag`.

## Gates

- **`crates/api/tests/engine_corpus.rs`** (`cargo test`) holds the Java parser's
  JSON for every engine-corpus entry. A converter change must not change which
  entries match.
- **Before and after a refactor**, dump `parse_section_json`, `parse_model_json`
  and `parse_expression_json` over the corpus, EMIT's sources and a large
  generated model, then compare.
  - When only the construction changes, the output must be byte-identical. A
    `Value`'s maps are sorted, so output built through one is order-stable.
  - When key order may change, compare as JSON, with numbers as written
    (`1.50` ≠ `1.5`).
- **Unit tests next to the converter** cover what the corpus doesn't reach (e.g.
  `EmbeddedH2` in `dsl-relational/tests/relational_connection.rs`).
