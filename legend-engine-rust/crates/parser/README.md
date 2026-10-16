# legend-pure-parser-parser

Recursive descent parser for the Pure grammar. Consumes tokens from the lexer, produces the AST. Supports plugin dispatch for island and section grammars.

## Usage

```rust
use legend_pure_parser_parser::parse;

let ast = parse("Class my::Person { name: String[1]; }", "file.pure")?;
// ast.sections[0].elements[0] is Element::Class(...)
```

## Key Types

- `Parser` — Main parser struct with recursive descent methods
- `PluginRegistry` — Holds island, section, and sub-parser plugins
- `ParseResult` — Success result containing `SourceFile`
- `ParseError` — Error with message and source span

## Expression Parsing

The parser uses a **Pratt parser** (operator precedence climbing) for binary expressions,
with these precedence levels (lowest to highest):

| Level | Operators | Associativity |
|-------|-----------|---------------|
| 1 | `\|\|` | Left |
| 2 | `&&` | Left |
| 3 | `==`, `!=` | Left |
| 4 | `<`, `<=`, `>`, `>=` | Left |
| 5 | `+`, `-` | Left |
| 6 | `*`, `/` | Left |

Parenthesized expressions `(expr)` are wrapped in `Expression::Group` to preserve
source grouping for faithful roundtripping.

### Identifier Disambiguation

A bare identifier can be several things depending on context:

| Pattern | Parsed As |
|---------|-----------|
| `name(args)` | `FunctionApplication` |
| `name` (no `(`) | `PackageableElementRef` |
| `name\|body` | Bare `Lambda` |
| `$name.prop` | `Variable` + `MemberAccess` |

### Lambda Variants

| Syntax | Form |
|--------|------|
| `\|body` | No-param braced |
| `x\|body` | Bare single-param |
| `{x\|body}` | Braced single-param |
| `{x: String[1]\|body}` | Typed param |
| `{x, y\|body}` | Multi-param |

## Type Parsing

- `parse_type_reference()` → `TypeReference` (plain types like `String`, `my::Person<T>`)
- `parse_type_spec()` → `TypeSpec` — calls `parse_type_reference()`, then handles `~Unit` suffix → wraps in `TypeSpec::Unit(UnitReference)` or `TypeSpec::Type(TypeReference)`

Property types and return types use `parse_type_spec()`. Super types use `parse_type_reference()`.

## String Escaping

`unquote_string` handles Pure string literal unescaping:

| Escape | Result |
|--------|--------|
| `\'` | `'` |
| `\\` | `\` |
| `\n` | newline |
| `\t` | tab |
| `\r` | carriage return |

## Testing

```bash
# Run all tests (63 snapshot + unit tests, 0 ignored)
cargo test -p legend-pure-parser-parser

# Run with clippy
cargo clippy -p legend-pure-parser-parser --tests -- -D warnings
```

Snapshot tests use `insta` for AST golden file comparison. Run `cargo insta review` after AST changes to update snapshots.
