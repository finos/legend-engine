# Pure REPL with MCP

An interactive REPL (Read-Eval-Print Loop) for the [Pure language](https://legend.finos.org/docs/getting-started/glossary#pure) with built-in [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server support. The MCP server exposes Pure evaluation capabilities as tools, making them accessible to AI coding assistants such as [Claude Code](https://docs.anthropic.com/en/docs/claude-code), [Cursor](https://www.cursor.com/), and other MCP-compatible clients.

## Overview

The Pure REPL allows you to:

- **Evaluate Pure expressions** interactively with detailed timing and type information
- **Query expression types** without execution (type inference)
- **Run Pure test suites** with optional filtering and PCT adapter support
- **Reload sources** incrementally or fully after editing Pure files on disk
- **Inspect and configure runtime options** for the Pure compiler and execution engine

The MCP server wraps these capabilities as tools over a stdio JSON-RPC 2.0 transport, enabling AI agents to programmatically evaluate Pure code, run tests, and inspect the runtime.

## Module Structure

```
legend-engine-pure-repl/
├── legend-engine-pure-repl-core/    # Core REPL engine, session, configuration
├── legend-engine-pure-repl-cli/     # Interactive CLI (JLine-based terminal)
└── legend-engine-pure-repl-mcp/     # MCP server (stdio transport)
```

## Building

From the legend-engine repository root:

```bash
# Build the entire REPL module
mvn clean install -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-repl -DskipTests

# Build only the MCP server
mvn clean install -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-repl/legend-engine-pure-repl-mcp -DskipTests
```

The MCP module produces a shaded (uber) JAR:

```
legend-engine-pure-repl-mcp/target/legend-engine-pure-repl-mcp-<version>-shaded.jar
```

## Running the MCP Server

```bash
# Basic startup (classpath-only repositories)
java -jar legend-engine-pure-repl-mcp-<version>-shaded.jar

# With filesystem source loading (enables hot-reload)
java -jar legend-engine-pure-repl-mcp-<version>-shaded.jar \
  --source-root /path/to/legend-engine \
  --repositories core,relational,service \
  --timeout 60000
```

### Command-Line Arguments

| Argument | Description |
|----------|-------------|
| `--source-root <path>` | Root directory of a legend-engine checkout. Enables filesystem-backed repositories for hot-reload. |
| `--repositories <csv>` | Comma-separated list of repository names to load (optional subset filter). |
| `--timeout <ms>` | Expression evaluation timeout in milliseconds (default: 30000). |

## MCP Tools

The server exposes 8 tools via the MCP protocol:

### `evaluate_pure`
Evaluate a Pure expression and return the result with type and timing information.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `expression` | string | yes | The Pure expression to evaluate |

**Example response:**
```json
{
  "status": "success",
  "expression": "1 + 2",
  "type": "Integer[1]",
  "result": 3,
  "timing": {"parseMs": 2, "compileMs": 25, "executeMs": 3}
}
```

### `get_expression_type`
Get the type of a Pure expression without evaluating it.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `expression` | string | yes | The Pure expression to type-check |

### `run_tests`
Run Pure tests at a given package path.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | yes | Package path or test function path (e.g. `meta::test::*`, `::`) |
| `pctAdapter` | string | no | PCT adapter name |
| `filter` | string | no | Regex filter for test names |

### `incremental_recompile`
Detect changed files on disk and recompile only those. Fast but does not pick up newly created files.

### `full_recompile`
Fully reset the runtime and recompile all sources. Slower but picks up all changes including new files.

### `get_runtime_info`
Return runtime status including loaded repositories, memory usage, and current options.

### `set_runtime_option`
Set a Pure runtime boolean option.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | yes | Option name |
| `value` | boolean | yes | Value to set |

### `list_runtime_options`
List all current Pure runtime options and their values.

## Using with AI Tools

The MCP server communicates over **stdio** (stdin/stdout) using JSON-RPC 2.0, with all logging directed to stderr. This makes it compatible with any MCP-capable AI tool.

### Claude Code

Add the following to your [Claude Code MCP configuration](https://docs.anthropic.com/en/docs/claude-code/mcp) (either in `.claude/settings.json` project-level or `~/.claude/settings.json` globally):

```json
{
  "mcpServers": {
    "pure-repl": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/legend-engine-pure-repl-mcp-<version>-shaded.jar",
        "--source-root",
        "/path/to/legend-engine",
        "--repositories",
        "core,relational,service"
      ]
    }
  }
}
```

Once configured, Claude Code can evaluate Pure expressions, run tests, and inspect types directly through the MCP tools.

### Cursor

In Cursor, go to **Settings > MCP** and add a new server:

- **Name:** `pure-repl`
- **Type:** `command`
- **Command:** `java -jar /absolute/path/to/legend-engine-pure-repl-mcp-<version>-shaded.jar --source-root /path/to/legend-engine`

### VS Code with Continue

Add to your `.continue/config.json`:

```json
{
  "mcpServers": [
    {
      "name": "pure-repl",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/legend-engine-pure-repl-mcp-<version>-shaded.jar",
        "--source-root",
        "/path/to/legend-engine"
      ]
    }
  ]
}
```

### Generic MCP Client

Any MCP client that supports stdio transport can connect. The server expects one JSON-RPC 2.0 message per line on stdin and writes responses to stdout. Example request:

```json
{"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "evaluate_pure", "arguments": {"expression": "1 + 2"}}}
```

## Configuration

The REPL can be configured via a JSON file and/or environment variables.

### Configuration File

Default location: `~/.pure-repl.json` (override with `PURE_REPL_CONFIG` environment variable).

```json
{
  "timeout": 30000,
  "outputFormat": "text",
  "debug": false,
  "quiet": false,
  "sourceRoot": "/path/to/legend-engine",
  "requiredRepositories": ["core", "relational", "service"]
}
```

### Environment Variables

| Variable | Description |
|----------|-------------|
| `PURE_REPL_CONFIG` | Path to configuration JSON file |
| `PURE_REPL_TIMEOUT` | Evaluation timeout in milliseconds |
| `PURE_REPL_OUTPUT_FORMAT` | Output format: `text` or `json` |
| `PURE_REPL_DEBUG` | Enable debug logging (`true`/`false`) |
| `PURE_REPL_HISTORY` | Path to command history file |

Environment variables take precedence over the configuration file.

## Interactive CLI Commands

When using the CLI (not the MCP server), these commands are available:

| Command | Aliases | Description |
|---------|---------|-------------|
| `:help` | `:h`, `:?` | Display help |
| `:info` | `:i` | Show runtime info (repositories, memory) |
| `:type <expr>` | `:t` | Get type of expression without evaluating |
| `:reload` | `:r` | Recompile all Pure sources |
| `:test <path>` | | Run tests at package path |
| `:option <name> <true\|false>` | `:opt` | Set a runtime option |
| `:option <name>` | `:opt` | Get a runtime option value |
| `:options` | | List all runtime options |
| `:quit` | `:exit`, `:q` | Exit the REPL |
