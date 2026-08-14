# Overview
Pure functions can be thought of as the platform's api. The Pure Compatibility Testing (PCT) Framework is how
we specify expectations around the api. This guide enables you to contribute platform functions while leveraging PCT.

## Quick Start: Which Guide Do I Need?

| I want to... | Guide |
|--------------|-------|
| Add a function implemented in Pure | [Pure Function How-To](purefunction-howto.md) |
| Add a function implemented in Java (native) | [Native How-To](native-howto.md) |
| Make my function work on databases | [Wiring How-To](wiring-howto.md) |
| Run PCT tests and handle failures | [Expected Failures How-To](expected-failures-howto.md) |
| Document a function I added | [PCT Function Documentation](pct-documentation.md) |
| Understand a test in a `composition.pure` file | [Composition Tests](composition-tests.md) |
| Understand concepts like routing, DynaFunction | [Concepts & Glossary](concepts-glossary.md) |

## Development Setup
Set up development environment for *legend-engine*:
- https://github.com/finos/legend-engine/blob/master/README.md#development-setup

## How To Add Pure Platform Functions
A Pure Platform Function has an implementation in Pure code. See [Pure Function How-To](purefunction-howto.md) for step-by-step instructions.

## How To Add Java Platform Functions
A "native" function has an implementation in Java (the native language of the platform) and no implementation in Pure. See [the Native How-To](native-howto.md) for step-by-step instructions.

## How to Wire your Function to run on Relational Databases
A key feature of Legend is that functions on the platform are cross-compiled, or "wired," to target runtimes. "Wiring" to target databases is required if we wish the function to be evaluated in the target database's runtime. See [the Wiring How-To](wiring-howto.md) for a step-by-step guide.

## Finishing Up / Running Database-specific PCT Tests
The final step involves running PCT Tests against database targets for the functions you've defined/modified. See [this page](expected-failures-howto.md) for information on how
to run the tests and record expected failures in the adapter's JSON manifest file.

-------------
# Published Reports
Two reports are generated from the same aggregated data (`DocumentationGeneration.buildDocumentation()`, served as
`pct-docs.json`), and both are published to the Legend docs site by the `pct-report` job in `.github/workflows/build.yml`:

| Report | Served at | Reads best as |
|---|---|---|
| `PCT_Report_Compatibility.html` | `/api/pct/form` | One matrix — every function against every adapter |
| `PCT_Report_Functions.html` | `/api/pct/functions` | A javadoc-style reference — browse by package, then one page per function |

The function reference page shows, for a single function: every overload with its documentation, every test case with its
documentation and qualifiers, and per-adapter results with the failure message behind each `×`. It counts every reported test —
unlike the compatibility matrix, it gives no qualifier (including `unsupportedFeature`) special treatment.

Each function is addressable as `PCT_Report_Functions.html#f/<name>` (for example `#f/tan`), so a single function can be linked
directly. Names used in several packages resolve to a chooser listing them; the `link` in each function's header gives the
shortest URL that opens that page.

Regenerate both locally with:
```bash
mvn exec:java -pl org.finos.legend.engine:legend-engine-pure-ide-light-http-server \
    -Dexec.mainClass="org.finos.legend.engine.server.core.pct.GeneratePCTFiles"
```
The output lands in `target/pct/`; serve that directory over HTTP (the pages fetch `pct-docs.json` alongside themselves).
Adapters appear only when their PCT module is on the classpath, so a local run shows fewer stores than CI.

-------------
# References
## Conventions
For conventions and best practices, see [this page](conventions.md)

## Taxonomy Guide
An overview of Pure Function Taxonomy is [here](taxonomy.md)

## Key Concepts / Glossary, and FAQ
For concepts / glossary, and FAQ see [this page](concepts-glossary.md)
