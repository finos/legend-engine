# Legend Pure Language Support

VS Code extension providing language support for Legend Pure (`.pure` files) via the Legend Pure LSP server.

## Features

### Implemented

- **Syntax highlighting** for `.pure` files (TextMate grammar)
- **Diagnostics** -- compilation errors reported inline as you type (300ms debounce)
- **Go-to-definition** (Ctrl-Click) -- navigates to definitions in workspace files and classpath sources via `pure://` virtual filesystem
- **Find all references** (Shift-F12) -- finds all usages of a class, function, enum, or property across all compiled sources
- **Hover information** -- shows element type, qualified path, class properties with full generic types (e.g., `Function<{String[1]->Boolean[1]}>`), enum values, and source location
- **Workspace symbol search** (Ctrl+T) -- pre-built index for instant search across all elements
- **Virtual filesystem** (`pure://` scheme) for read-only browsing of classpath Pure sources
- **Execute go()** (Ctrl+Shift+P, then "Legend Pure: Execute go()") -- runs the `go():Any[*]` function and shows console output
- **Hybrid storage** -- workspace repos use MutableFSCodeStorage (reads from disk, reflects changes immediately), classpath repos fall back to ClassLoaderCodeStorage
- **Error isolation** -- compilation failures in one file don't spread to others
- **Immutable source protection** -- platform/bootstrap sources are never re-parsed
- **File watcher** -- detects changes from git, external editors, and agents

### Not Yet Implemented

- Code completion (Ctrl+Space)
- Test execution
- Debugging (breakpoints, stepping)
- Rename / move refactoring
- Document outline (Ctrl+Shift+O)
- Full-text search across Pure sources
- Semantic tokens for enhanced highlighting

## Prerequisites

- **Java 11+** on your `PATH`, or configure `legendPure.java.home`
- **Built server JAR** from the `legend-engine-xt-lsp-server` Maven module
- For hybrid storage: at least one `mvn compile` so `src/main/resources/` directories exist

## Building

### Server JAR

From the legend-engine repository root:

```bash
mvn package -pl legend-engine-xts-lsp/legend-engine-xt-lsp-server -am -DskipTests
```

The JAR is written to `legend-engine-xts-lsp/legend-engine-xt-lsp-server/target/`.

### Extension

```bash
cd legend-engine-xts-lsp/legend-engine-xt-lsp-vscode
npm install
npm run bundle
npx vsce package --allow-missing-repository
```

### Install

```bash
code --install-extension legend-pure-lsp-0.1.0.vsix
```

## Configuration

| Setting                     | Description                                                                 |
|-----------------------------|-----------------------------------------------------------------------------|
| `legendPure.server.jarPath` | Absolute path to the LSP server JAR. If empty, the extension searches the default Maven output directories. |
| `legendPure.java.home`      | Path to a Java 11+ installation. If empty, `java` from `PATH` is used.     |

## Commands

| Command                          | Description                                                              |
|----------------------------------|--------------------------------------------------------------------------|
| `Legend Pure: Reindex Workspace`  | Re-initializes the Pure runtime and reindexes all workspace sources.    |
| `Legend Pure: Execute go()`       | Executes the `go():Any[*]` function and shows output in a panel.        |

## Architecture

The LSP server uses a **hybrid storage model**:

- **Workspace repos** (found via `*.definition.json` scanning) use `MutableFSCodeStorage`, reading `.pure` files directly from `src/main/resources/`. Changes are reflected immediately.
- **Classpath repos** (platform, extensions not checked out) use `ClassLoaderCodeStorage` from the fat JAR. These provide the Pure runtime, standard library, and pre-compiled extension modules.

This means Pure file changes in the workspace are live, while the standard library and unmodified extensions come from the stable JAR.

## Debugging the Server

The server writes `[LSP-DEBUG]` messages to stderr. In VS Code, open Output panel and select "Legend Pure LSP" to see:
- Repository scan results and storage type per repo
- Source ID resolution decisions
- Symbol index build timing
- go() execution results

## Test Suite

114 tests across 14 test classes covering compilation, navigation, hover, references, workspace symbols, URI mapping, repository scanning, hybrid storage, error isolation, and immutable source protection.
