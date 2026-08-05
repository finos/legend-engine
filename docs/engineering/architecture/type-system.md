# Legend Engine — Type System

> **Audience:** Developers who need to know where types come from and whether the engine
> behaves differently from Legend Pure.
> **Scope:** deliberately thin. This page does not restate the Pure type system — it records
> where it comes from and tracks any place the engine diverges from it.

---

## 1. The type system is inherited, not defined here

Legend Engine defines **no type system of its own**. Types, multiplicity, generics, the
primitive set, and the rules for literals all come from **Legend Pure** (the M3 metamodel),
which the engine consumes as a dependency.

Canonical documentation for the type system lives in the
[legend-pure](https://github.com/finos/legend-pure) repository. When the two disagree,
legend-pure is the source of truth and the engine is the thing to fix.

## 2. Where the engine touches types

The engine mirrors and transports Pure's types rather than defining them:

| Layer | Role | Key location |
|-------|------|--------------|
| Protocol | JSON-serialisable POJOs mirroring M3 (`CString`, `CInteger`, `Multiplicity`, `GenericType`, …) | `legend-engine-protocol-pure`, package `...protocol.pure.m3` |
| Grammar | Parses literal syntax into those POJOs and composes it back | `legend-engine-language-pure-grammar` — see [Key Java Areas §1](key-java-areas.md#1-grammar-layer) |
| Compiler | Builds the typed `PureModel` graph via legend-pure's `ModelRepository` | see [Alloy Compiler](alloy-compiler.md) |

Because the engine has a text ↔ JSON ↔ text round-trip that legend-pure does not, it
occasionally needs protocol state that carries no meaning in Pure itself. Such state is
formatting metadata only and must never affect a value's meaning — see the `multiLine` flag
below.

## 3. Divergences from Legend Pure

Ideally this section stays empty. Anything listed here is a bug, a deliberate temporary gap,
or an engine-only concern — say which.

Note that the two sides can be briefly out of step whenever a language change lands in one
repository before the other picks it up. That is ordinary lag, not divergence; record something
here only when the two are expected to disagree *after* both are current.

### 3.1 Multi-line string literals (`'''…'''`)

Both projects support Java-text-block-style multi-line string literals. Because these are two
independent parsers, the semantics are aligned **by hand and by test**, not by construction.

`PureGrammarParserUtility.processTextBlock` is kept behaviourally identical to legend-pure's
`AntlrContextToM3CoreInstance.processMultilineString`:

- opening `'''` must be followed — after optional spaces or tabs — by a line terminator;
- line terminators normalized, opening-delimiter line and closing `'''` dropped;
- minimum indentation computed over non-blank lines **plus the last (closing-delimiter) line**,
  which pins the strip width and keeps a value's own indentation intact;
- indentation measured with `Character.isWhitespace`;
- trailing whitespace stripped per line;
- escapes processed last — the two use different utility classes but equivalent rules (octal,
  unicode, Java control characters, `\\`, `\'`, and a lone `\`).

**If you change one, change the other.** `TestTextBlockStringParsing` is the engine-side lock.

#### Engine-only: re-emission

legend-pure parses; the engine also has to compose text back. That round-trip needs state Pure
has no use for, so it lives only in the protocol:

- **`multiLine`** on `CString` records that a literal was *authored* as a block, so a value nobody
  wrote as a block never reformats into one. It is formatting metadata: never affects the value,
  excluded from `CString.equals`, and `@JsonInclude(NON_DEFAULT)` keeps it off the wire unless used.
- **The flag is the entire decision.** The parser sets it from the token shape alone
  (`isTextBlock(rawToken)`) and the composer obeys it — neither inspects the value. There is no
  heuristic anywhere, by design: a block is a block because it was written as one.

That puts the burden on `renderTextBlock`, which must encode *any* value so the parser returns it
unchanged. The parser dedents, strips trailing whitespace per line, then unescapes — so anything
those steps would consume is emitted as an escape rather than a raw character: carriage returns,
an embedded `'''`, trailing whitespace, whitespace-only lines, and (when the value has no trailing
newline, so the closing delimiter shares the last content line) each line's own leading whitespace.
`testFlagIsObeyedForAnyValue` drives a battery of these through compose → parse and asserts both
value equality and a composing fixed point.

#### Gap: tagged values

legend-pure accepts multi-line strings in **tagged values** as well as expression literals — the
documentation use case, `doc.doc = '''...'''`. The engine currently supports them only in
expression literals.

`TaggedValue` carries a bare `String` with no place to record how it was authored, so supporting
it symmetrically would mean a second protocol flag and its own indentation handling in the
annotation renderer. That was judged not clean enough to carry, and is deferred until the
composer side can be expressed as simply as it is for `CString`.

Consequence today: a `'''` block written in a tagged value still *parses* — the tagged-value
grammar rule accepts `STRING`, which includes the text-block form, and the value is dedented
correctly — but it composes back as an escaped single-line string. The value is preserved; only
the block formatting is lost on a round-trip.

---

## Related

- [Key Java Areas](key-java-areas.md) — grammar, compiler, and protocol layers
- [Alloy Compiler](alloy-compiler.md) — type and function resolution
- [Domain & Key Concepts](domain-concepts.md) — the modelling vocabulary built on these types
