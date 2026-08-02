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

This gap does **not** apply to documentation, which has its own construct — see §3.2.

### 3.2 Documentation (`'''…'''` before a declaration)

A `'''…'''` literal immediately preceding a declaration is **syntactic sugar** for the
`meta::pure::profiles::doc` `doc` tagged value. These two parse to the same protocol:

```
'''
A person in the system.
'''
Class model::Person
{
  '''
  Given name. Not guaranteed unique.
  '''
  firstName: String[1];
}
```

```
Class {meta::pure::profiles::doc.doc = 'A person in the system.'} model::Person
{
  {meta::pure::profiles::doc.doc = 'Given name. Not guaranteed unique.'} firstName: String[1];
}
```

There is no new profile, no new metamodel, and **no protocol change at all**: desugaring happens in
`DomainParseTreeWalker`, so everything downstream — compiler, `PureModelContextData` JSON, every
existing `doc` consumer — sees an ordinary `TaggedValue` and needs no update.

Documentation is a **parser rule** (`documentation` in `DomainParserGrammar.g4`), listed explicitly at
each declaration that accepts it rather than being lexer-level. That is what keeps the identical
literal in expression position an ordinary [multi-line string](#31-multi-line-string-literals-),
and it is why attachment needs no adjacency heuristic: either the literal is in a documentation
position or it is a value.

#### Where it attaches

Documentation precedes the **whole declaration**, before any stereotypes or tagged values.
Intervening whitespace and comments are skipped, so a note written between the documentation and the
declaration does not detach it.

The rule is the same everywhere: wherever an element accepts `taggedValues`, it accepts documentation.

| Grammar | Declarations |
|---|---|
| `###Pure` | `Class`, `Enum` and its values, `Association`, `function`, properties, qualified properties |
| `###Relational` | `Database`, `Schema`, `Table`, column, `View` |
| `###DataSpace` | `DataSpace` |
| `###Service` / `###HostedService` | `Service` |
| `###FunctionJar` | `FunctionJar` |
| `###DataQuality` | `DataQualityValidation`, `DataQualityRelationValidation` |
| `###Snowflake` | `SnowflakeApp`, `SnowflakeM2MUdf` |
| `###BigQueryFunction` / `###MemSqlFunction` | `BigQueryFunction`, `MemSqlFunction` |
| `###Data` | `Data` |

The `documentation` rule lives in `M3ParserGrammar.g4`, so the eight grammars that import it inherit
the rule and only name it in their element rule. `RelationalParserGrammar.g4` and
`DataParserGrammar.g4` import `CoreParserGrammar` and carry their own `stereotypes`/`taggedValues`,
so they declare a local copy.

**Several of these DSLs already have a `documentation:` or `description:` field of their own** —
Service, HostedService and FunctionJar have `documentation`; DataSpace, Snowflake, BigQuery and
MemSql have `description`. A `'''…'''` block does **not** populate those. It is the `doc` tagged
value in every grammar, so the same syntax means the same thing everywhere; the DSL-specific field
stays a separate slot and an element may carry both.

`PureGrammarParserUtility.taggedValuesWithDocumentation` is the single implementation every walker
calls. ANTLR generates a distinct `DocumentationContext` class per grammar even for a rule inherited
from `M3ParserGrammar`, so it takes the literal's token rather than the context.

An element may not carry both documentation and an explicit `doc.doc` tagged value — that is a parser
error rather than a silent drop. Because tag references are unresolved at parse time, the check
matches the profile *as written*: bare `doc` (through an import) or fully qualified
`meta::pure::profiles::doc`. `my::pkg::doc.doc` is a different profile and is not a conflict.

#### How the content is processed

Documentation shares the text-block **layout** of §3.1 and differs in two respects, both matching
legend-pure's `DocumentationCanonicalizer`:

1. **Content is literal — there is no escape processing.** `\n`, `\'` and `\\` are content.
   Unescaping prose would silently rewrite a regex (`\d+` → `d+`), a Markdown escape (`\*` → `*`)
   and a Windows path (`C:\temp` → `C:` followed by a tab).
2. **Leading and trailing blank lines are dropped**, which is what makes a documentation literal and
   the equivalent explicit `doc.doc` tagged value hold the same string.

`PureGrammarParserUtility.canonicalizeDocumentation` is the engine-side implementation;
`TestDocumentationParsing` is the lock. **If you change one, change the other.**

#### Engine-only: re-emission

`TaggedValue.value` is a `CString` in the protocol, so a doc value carries the same `multiLine`
flag a string literal does, and the same rule applies: the parser sets it from the token shape
alone (a documentation block or a `'''...'''` tagged-value literal), the composer obeys it, and
neither inspects the value. A value authored as a block composes back as a block; one authored as
an ordinary tagged value stays one, even when it contains newlines.

On the wire the value stays a plain JSON string — byte-identical to the pre-flag protocol — unless
`multiLine` is set, in which case it is an object (`{"_type": "string", "multiLine": true,
"value": ...}`). Both shapes deserialize into the same POJO (`TestTaggedValueCompatibility` is the
lock), so every existing model is unaffected in both directions and only models that actually use
documentation blocks put the new shape on the wire. The flag is protocol-only: compiling to the
graph drops it, so anything re-generated from a compiled model (`PureModelContextDataGenerator`,
the `meta::protocols` transfers) composes docs as ordinary tagged values.

Because documentation has no escape mechanism at all, the composer cannot encode an arbitrary value
the way `renderTextBlock` does. `isRenderableAsDocumentation` is the guard, and anything it rejects
stays an ordinary tagged value: an embedded `'''`, a carriage return, trailing whitespace on a line,
a leading or trailing blank line, an empty value, two `doc` tags on one element, and a line beginning
`###` (the section split runs before any grammar and is not string-aware, so it would re-split the
file). `TestDocumentationRoundtrip` has one test per trigger.

Both profile spellings are promoted — bare `doc` and fully qualified — so `{doc.doc = 'x'}` composes
to a block that reparses as `meta::pure::profiles::doc`. The text changes; the meaning does not.

#### Gaps against legend-pure

legend-pure additionally attaches documentation to `Profile`, `Measure`, `Primitive` and
`native function`. The engine cannot without unrelated work: its grammar gives `profile`,
`measureDefinition` and `nativeFunction` no annotation slots at all, the `Profile` protocol has no
`taggedValues` field, and `nativeFunction` has no walker (it reaches `visitElement`'s
"Unsupported syntax" throw). Deferred rather than half-supported.

Graph-fetch projections (`treePath`, `simpleProperty`, `complexProperty`, `derivedProperty`) are also
out of scope: they are projections rather than declarations, and legend-pure did not touch them.

In the other direction the engine goes **further** than legend-pure on `###Relational`, where
documentation on a `Database`, `Schema`, `Table`, column or `View` is supported. In legend-pure a
`'''…'''` relational tagged value parses and then NPEs in
`RelationalGraphBuilder.visitTaggedValueNew`.

---

## Related

- [Key Java Areas](key-java-areas.md) — grammar, compiler, and protocol layers
- [Alloy Compiler](alloy-compiler.md) — type and function resolution
- [Domain & Key Concepts](domain-concepts.md) — the modelling vocabulary built on these types
