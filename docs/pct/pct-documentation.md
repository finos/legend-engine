# Writing PCT function documentation

This standard governs the `'''…'''` documentation on `<<PCT.function>>` declarations and
`<<PCT.test>>` cases in the engine-side Pure function repositories:

| Repository | Module |
|---|---|
| `core_functions_standard` | `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-standard/legend-engine-pure-functions-standard-pure` |
| `core_functions_relation` | `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-functions-relation-pure` |
| `core_functions_unclassified` | `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-unclassified/legend-engine-pure-functions-unclassified-pure` |
| `core_functions_variant` | `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-variant/legend-engine-pure-functions-variant-pure` |
| `core_scenario_quant` | `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-scenario-quant-pure` |
| `core` | `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core` |
| `core_dataquality` | `legend-engine-xts-dataquality/legend-engine-xt-dataquality-pure` |

These strings are **published to end users**. Write for someone using Pure to get work done, not for
someone maintaining the compiler.

For the language mechanics — where documentation attaches, how content is processed, the conflict
with an explicit `doc.doc` — see
[Type System § Documentation](../engineering/architecture/type-system.md#32-documentation--before-a-declaration).
This document covers only *what to write*.

The platform functions in legend-pure follow the same standard; see
`docs/standards/pct-documentation.md` there.

---

## Why it is worth writing

Documentation is sugar over the `meta::pure::profiles::doc` `doc` tagged value, and the pipeline that
publishes it already exists end to end:

```
.pure  '''markdown'''
  → doc.doc tagged value
  → PCTTools.getDoc()
  → Signature.documentation             (per signature)
  → FunctionDefinition.tests[].documentation   (per test)
  → FUNCTIONS_<repository>.json
  → DocumentationGeneration.buildDocumentation()
  → consumed downstream for published documentation
```

`FUNCTIONS_<repository>.json` is emitted by the `legend-pure-maven-generation-pct`
`generate-pct-functions` execution declared in each `-pure` module's `pom.xml`, and lands in
`target/classes/pct-reports/`.

> **Test documentation requires `legend.pure.version` ≥ 5.94.0.** `TestDefinition` — the model
> object that carries a test's documentation into the JSON — first exists at 5.94.0. Below that a
> `'''…'''` literal on a `<<PCT.test>>` parses and attaches to the graph, and is then silently
> dropped by the report generator. Signature documentation has no such constraint.

---

## The template

````pure
'''
One-sentence summary, ending in a period.

Optional paragraph on semantics, edge cases, and empty/`[0..1]` behaviour.

**Parameters**
- `variant` — what it is.

**Returns** what comes back.

**Examples**
```pure
fromJson('{ "Hello" : null }')->toJson()   // '{"Hello":null}'
```

**See also** `fromJson(String[1]):Variant[1]`,
`toVariant(Any[*]):Variant[1]`
'''
native function
    <<PCT.function>>
meta::pure::functions::variant::convert::toJson(variant: Variant[1]): String[1];
````

The live reference implementation is
[`core_functions_variant/functions/convert/toJson.pure`](../../legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-variant/legend-engine-pure-functions-variant-pure/src/main/resources/core_functions_variant/functions/convert/toJson.pure).

Put the delimiters on their own lines, even for a one-liner — `'''One line.'''` does not canonicalize
the way you would expect:

```pure
'''
A key that is absent yields empty rather than failing.
'''
function <<PCT.test, PCTCoreQualifier.variant>> meta::pure::functions::variant::navigation::tests::get::testGetFromObjectWhenKeyDoesNotExists<Z|y>(…):Boolean[1]
```

Every function gets **at least one runnable example**. Prefer examples showing a normal case and at
least one boundary — no match, empty collection, zero.

---

## Formatting rules

1. **Keep the opening delimiter, the body and the closing delimiter at one indentation.** Content
   indentation is measured relative to the closing delimiter, so a `'''` pulled left of the body leaves
   that difference on every line — and 4+ spaces renders as a Markdown code block.

2. **Use `-` for bullets.** Consistent with the corpus, and avoids any confusion with emphasis.

3. **Never write `'''` inside the content** — it closes the literal. This is the only sequence that
   needs avoiding; ordinary single quotes in Pure examples are fine.

4. **Content is literal** — never unescaped, so `\n` is a backslash and an `n`. Write real line breaks.

5. **Tag fenced blocks `pure`** so the published renderer can highlight them.

6. **One documentation literal per declaration.** Two in a row is a parse error, and the message points
   at the literal rather than at the duplication — easy to misread when it happens.

7. **The literal precedes the whole declaration** — ahead of `native`/`function` and ahead of the
   stereotype block, whatever qualifiers it carries (`<<PCT.function, PCT.platformOnly>>`,
   `<<PCT.test, PCTRelationQualifier.relation>>`).

---

## Replacing an existing `doc.doc`

Most engine signatures already carry a one-line `doc.doc` tagged value. **An element may not carry
both** a documentation literal and an explicit `doc.doc` — it is a parse error:

```
Element has both documentation and an explicit doc.doc tagged value. Use one.
```

Delete the tagged value in the same edit that adds the literal. **Keep `PCT.grammarDoc` and
`PCT.grammarCharacters`** — different tags, unaffected.

```diff
+'''
+Renders a variant as its JSON text.
+…
+'''
 native function
     <<PCT.function>>
-    {
-        doc.doc='Returns the json representation of the given variant.'
-    }
 meta::pure::functions::variant::convert::toJson(variant: Variant[1]): String[1];
```

Where only a `doc.doc` was present, the whole brace block goes; where `PCT.grammarDoc` shares the
block, only the `doc.doc` entry does.

Two shapes are worth watching for, because both currently publish something wrong:

- **Concatenated values.** `doc.doc='first line\n' + 'second line'` publishes as
  `first line\n, second line` — the concatenation is not evaluated at the tagged-value level. A
  documentation literal has real line breaks and no such artefact.
- **Empty values.** `doc.doc=''` publishes an empty string, which is indistinguishable downstream
  from a function nobody documented.

---

## Where the content comes from

**Take examples and stated behaviour from the PCT tests in the same file, not from the signature.**
The signature says what the types are; the assertions say what actually happens, and that is where
the value is. Behaviour worth stating is almost always behaviour a test already pins:

- `get(Variant[0..1], String[1])` returns empty for a missing key rather than failing —
  `testGetFromObjectWhenKeyDoesNotExists` asserts it.
- `to(Variant[0..1], T[0..1])` coerces across JSON types, so `'"1"'` reads as `1` for `@Integer`,
  but `1.25` does not — `testToIntegerFromString` and `testToIntegerFromFloat` draw the line.
- `toMany` fails rather than wrapping when the variant is not an array —
  `testToManyFromNonArray`.

Deriving prose from the signature is how documentation ends up factually wrong. Review of the
equivalent legend-pure change found five such errors, every one of them contradicted by an assertion
sitting in the same file.

---

## Overloads

`Signature.documentation` is per-signature, and the REPL shows the **first** signature that has
documentation.

> Put the **full** documentation on the **primary** variant — the plain form a caller reaches for,
> which is usually the shortest. Give each other overload a short comment stating only how it differs.

Primary is not the same as most general. `to(Variant[0..1], T[0..1]):T[0..1]` delegates to the
four-argument form that takes an explicit type key and lookup, yet it is the one every caller and
every test uses, so it carries the text and the four-argument form gets the delta. **Reorder the file
so the primary leads** when it does not already — legend-pure did exactly this for `greaterThanEqual`
and `elementToPath`, whose documentation had landed on a secondary variant because that variant
happened to come first.

Make the delta self-contained: name the signature being contrasted rather than writing "as above", since
a renderer may show each signature on its own.

```pure
'''
As `to(Variant[0..1], T[0..1]):T[0..1]`, but resolves a subtype from `typeKeyName` in the payload
against `typeLookup` rather than from the default `_type` key and an empty lookup.
'''
```

Check which signature actually carries the stereotype block before assuming — it is not always the
one you would guess.

---

## Cross-references (`See also`)

Reference other functions by **full signature**, never bare name, so a renderer can resolve exactly one
target:

```
**See also** `fromJson(String[1]):Variant[1]`
```

Two reasons from the current corpus: `to` and `toMany` each have two overloads, and `get` exists in
both `meta::pure::functions::variant::navigation` and `meta::pure::functions::collection`.

**Format** — match the `simple` field of `Signature` in `FUNCTIONS_*.json`: parameter types comma-space
separated, return type after the colon. Strip the package when the target is in the same package as the
function you are documenting; keep it otherwise.

**Exception — you may drop to a fully qualified path when the signature is unwieldy.** The signature
exists to disambiguate, and a path already does that when nothing shares the name. Some relation
functions take a wide row type: `simpleMovingAverage5Days`'s `simple` field runs past 300 characters,
and pasting it into a `See also` line buries the reference it is supposed to make. `core_scenario_quant`
therefore references by path alone — `meta::external::scenario::quant::sma::simpleMovingAverage5Days`.
This is a readability escape hatch, not a second convention: keep full signatures wherever they fit on a
line, as `core_dataquality` does with `rowCountEqual(Relation<T>[1], Number[1]):Boolean[1]`. Confirm the
name really is unique rather than assuming it:

```bash
python3 -c "
import json,collections; d=json.load(open('<module>/target/classes/pct-reports/FUNCTIONS_<repository>.json'))
c=collections.Counter(s['simple'].split('(')[0] for f in d['functionDefinitions'] for s in f['signatures'])
print([n for n,k in c.items() if k>1] or 'all unique')"
```

**Do not hand-write signatures.** Copy them from the generated JSON:

```bash
python3 -c "
import json; d=json.load(open('legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-variant/legend-engine-pure-functions-variant-pure/target/classes/pct-reports/FUNCTIONS_variant.json'))
print('\n'.join(s['simple'] for f in d['functionDefinitions'] for s in f['signatures']))"
```

Hand-written references are wrong surprisingly often.

---

## Before you document a file: check for position assertions

Adding documentation **shifts every line below it**, and a test that asserts the line or column of an
element in a documented file will fail naming a line number, not your documentation — so it reads as
unrelated.

**As of writing, the engine PCT corpus carries none of the three shapes.** Re-run these before a
large pass rather than assuming it stayed that way:

```bash
# self-referential assertError — asserts the line it is written on
grep -rn --include='*.pure' "assertError(.*',[[:space:]]*[0-9]" legend-engine-core/legend-engine-core-pure

# error-message strings baking in a source position
grep -rn --include='*.java' "\.pure line:" .

# integer position assertions
grep -rn --include='*.java' 'getSourceInformation()\.\(getLine\|getColumn\)' .
```

Note also that `FunctionsGeneration` permits only **one PCT function name per source file**. If a file
holds several, split it — and move each function's tests with it, since tests are attributed to a
function by source id.

---

## Verifying

Documentation is parse-time sugar over a tagged value, so **a documentation-only change cannot alter
behaviour**. The module build is the whole verification; **do not run the relational PCT suites for
it**, which costs minutes for a guaranteed-green result.

Building the Pure repository is what catches everything that can actually break — a malformed literal,
and the parse error raised when a declaration carries both a literal and an explicit `doc.doc`:

```bash
mvn clean install -pl <the -pure module>
```

**Do not verify with `-DskipTests`.** The `-pure` module's own tests include the compiled-state
integrity check, and the runtime-extension modules beside it run the `<<PCT.test>>` functions against
this code — worth building too when a change is large, since **they load it as bytecode from their
jars** rather than from source:

```bash
mvn clean install -pl <…-runtime-java-extension-compiled-functions-X>,<…-runtime-java-extension-interpreted-functions-X>
```

The exception that would justify a PCT run is a manifest whose `expectedError` embeds a source
position from a file you are documenting — added lines shift it and break the match. The engine
corpus carries none today; the greps in the section above are how you confirm that before a large
pass. Note also that a shared `~/.m2` is contended, so a build there may pick up artifacts from
another checkout; `-Dmaven.repo.local` pointed at a scratch copy avoids it.

Finally check the emitted JSON, which is the thing actually published:

```bash
python3 -c "
import json; d=json.load(open('<module>/target/classes/pct-reports/FUNCTIONS_<repository>.json'))
print(json.dumps([f for f in d['functionDefinitions'] if f.get('name')=='toJson'], indent=2))"
```

Confirm `signatures[].documentation` holds the intended Markdown with newlines preserved, and — at
`legend.pure.version` ≥ 5.94.0 — that tests appear under `tests` with their documentation attached.
