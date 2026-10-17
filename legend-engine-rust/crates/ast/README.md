# legend-pure-parser-ast

Core AST data model for the Pure grammar parser. Protocol JSON is **not** this crate's concern (that lives exclusively in the `protocol` crate); the only `serde` derives are on `SourceInfo` and `SpannedString`, for binary round-trips.

## Key Types

- `Element` — Top-level enum: `Class`, `Enum`, `Function`, `Profile`, `Association`, `Measure`
- `Expression` — Value specifications: literals, variables, function calls, lambdas, operators
- `TypeReference` — Type with `package` + `name` + optional generics and type variable values
- `TypeSpec` — Enum wrapping `TypeReference` or `UnitReference`, used where units are valid
- `UnitReference` — Measure~Unit pairing: `{ measure: TypeReference, unit: Identifier }`
- `SourceInfo` — Line/column tracking for every AST node
- `Multiplicity` — `[1]`, `[*]`, `[0..1]`, `[1..*]`

## Type System AST

### `TypeReference`
Represents a qualified type name with optional generics:

```rust
TypeReference {
    package: Option<Package>,  // e.g., Some("my::model")
    name: Identifier,          // e.g., "Person"
    type_arguments: Vec<TypeReference>,
    type_variable_values: Vec<TypeVariableValue>,
}
```

Helper: `type_ref.full_path()` returns `"my::model::Person"`.

### `UnitReference`
Represents a measure unit pairing: `NewMeasure~UnitOne`

```rust
UnitReference {
    measure: TypeReference,  // e.g., "NewMeasure"
    unit: Identifier,        // e.g., "UnitOne"
}
```

### `TypeSpec`
Used in positions that accept both types and units (property types, return types):

```rust
enum TypeSpec {
    Type(TypeReference),
    Unit(UnitReference),
}
```

`ClassDef.super_types` uses `Vec<TypeReference>` directly (supertypes are always types).

## Expression Variants

| Variant | Example | Notes |
|---------|---------|-------|
| `Literal` | `42`, `'hello'`, `true` | Integer, Float, Decimal, String, Boolean, Date |
| `Variable` | `$x` | |
| `Arithmetic` | `a + b`, `x * y` | Binary with precedence |
| `Comparison` | `a == b`, `x < y` | |
| `Logical` | `a && b`, `x \|\| y` | |
| `Bitwise` | `a &&& b`, `x \|\|\| y` | F#-style triple-char syntax |
| `Not` / `UnaryMinus` / `BitwiseNot` | `!x`, `-x`, `~~~x` | Unary |
| `FunctionApplication` | `foo(1, 2)` | With arguments |
| `PackageableElementRef` | `MyEnum`, `my::Type` | Bare name without `()` |
| `ArrowFunction` | `$x->map(f)` | Full `PackageableElementPtr` for path |
| `MemberAccess` | `$x.name` | Simple or qualified property access |
| `Group` | `(expr)` | Preserves source parentheses for roundtripping |
| `Lambda` | `x\|$x + 1`, `{x: String[1]\|...}` | Bare, braced, typed |
| `Let` | `let x = expr` | |
| `Collection` | `[1, 2, 3]` | |
| `NewInstance` | `^MyClass(name='John')` | |
| `Column` | `~name`, `~[name: String]` | TDS column expressions |
| `TypeReferenceExpr` | `@MyType` | |

## Trait Hierarchy

- **`Spanned`** — Access `source_info()` on any node
- **`Annotated: Spanned`** — `stereotypes()` + `tagged_values()`
- **`PackageableElement: Spanned + Annotated`** — `package()` + `name()` for top-level elements

## Derive Macros

Use one derive per struct — higher-level derives bring lower-level traits automatically:

```rust
#[derive(crate::PackageableElement)]  // → Spanned + Annotated + PackageableElement
pub struct ClassDef { ... }

#[derive(crate::Annotated)]           // → Spanned + Annotated
pub struct EnumValue { ... }

#[derive(crate::Spanned)]             // → Spanned only
pub struct Constraint { ... }
```
