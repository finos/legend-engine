# Pure IDE MCP Server

An [MCP (Model Context Protocol)](https://modelcontextprotocol.io/) server that exposes the Pure IDE's capabilities to AI assistants and tools. This allows AI agents to read, search, navigate, create, and execute Pure code through a standardized protocol.

## Overview

The Pure IDE Light is a Dropwizard-based IDE for the [Pure language](https://github.com/finos/legend-pure) with capabilities including code execution, testing, search, file navigation, and code completion. This module wraps those capabilities as MCP tools, making them accessible to any MCP-compatible client.

### Architecture

```
┌──────────────────┐       JSON-RPC 2.0        ┌─────────────────────────────┐
│   AI Assistant    │ ◄────────────────────────► │   PureIDEMcpHttpEndpoint    │
│  (Claude, etc.)  │    POST /mcp               │   (JAX-RS, port 9010)      │
└──────────────────┘                            └──────────┬──────────────────┘
                                                           │
┌──────────────────┐       JSON-RPC 2.0        ┌───────────▼──────────────────┐
│   AI Assistant    │ ◄────────────────────────► │   LegendStatelessMcp        │
│  (subprocess)    │    stdin/stdout            │   ServerOrchestrator        │
└──────────────────┘                            └───────────┬──────────────────┘
                                                            │
                                                ┌───────────▼──────────────────┐
                                                │  PureIDEMcpToolDefinitions   │
                                                │  (11 tools)                  │
                                                └───────────┬──────────────────┘
                                                            │
                                                ┌───────────▼──────────────────┐
                                                │  PureRuntime / CodeStorage   │
                                                │  / FunctionExecution         │
                                                └──────────────────────────────┘
```

Two transports are provided:

- **HTTP** (`PureIDEMcpHttpEndpoint`): A `POST /mcp` JAX-RS endpoint embedded in the Dropwizard server. This is automatically registered when you start the Pure IDE.
- **Stdio** (`PureIDEMcpStdioServer`): A standalone process that reads JSON-RPC from stdin and writes to stdout. Suitable for subprocess-based MCP clients.

### Available Tools

| Tool | Description | Read-only |
|------|-------------|-----------|
| `read_file` | Read the content of a Pure source file | Yes |
| `list_directory` | List files and directories at a given path | Yes |
| `find_in_sources` | Search for text or regex across all Pure sources | Yes |
| `find_pure_file` | Find Pure files by name pattern | Yes |
| `get_concept_info` | Get concept info for a symbol at a source location | Yes |
| `get_suggestions` | Get code completion suggestions for a package path | Yes |
| `create_file` | Create a new Pure source file (optionally with content) | No |
| `delete_file` | Delete a Pure source file | No |
| `execute_pure` | Execute a Pure function by fully-qualified name | No |
| `execute_go` | Compile and execute a Pure code block | No |
| `run_tests` | Run Pure tests at a given package path | No |

## Getting Started

### Prerequisites

- Java 11+
- Maven 3.6+
- The legend-engine repository built locally

### Building

```bash
# Build just the MCP module
mvn install -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-mcp -DskipTests

# Build the full IDE (including MCP)
mvn install -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server -DskipTests -am
```

### Running the Pure IDE with MCP

Start the Pure IDE as normal. The `/mcp` endpoint is automatically available:

```bash
# Start the IDE (adjust for your launcher)
java -cp <classpath> org.finos.legend.engine.ide.PureIDELight server config.yml
```

The MCP endpoint is now available at `http://localhost:9010/mcp`.

### Verifying the MCP Endpoint

```bash
# Initialize
curl -s -X POST http://localhost:9010/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'

# List available tools
curl -s -X POST http://localhost:9010/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'

# Read a file
curl -s -X POST http://localhost:9010/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"read_file","arguments":{"path":"/welcome.pure"}}}'

# Search across sources
curl -s -X POST http://localhost:9010/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"find_in_sources","arguments":{"query":"function go","limit":10}}}'
```

---

## Connecting AI Tools to the Pure IDE MCP Server

### Claude Code (CLI)

Claude Code natively supports MCP servers. Connect to the Pure IDE using either the HTTP or stdio transport.

**HTTP transport (recommended when the IDE is already running):**

```bash
claude mcp add --transport http pure-ide http://localhost:9010/mcp
```

**Stdio transport (launches a standalone MCP process):**

```bash
claude mcp add --transport stdio pure-ide -- java -cp <classpath> \
  org.finos.legend.engine.ide.mcp.PureIDEMcpStdioServer
```

**Verify it's connected:**

```bash
claude mcp list
```

Or inside a Claude Code session, type `/mcp` to see connected servers and their tools.

**Scoping options:**

```bash
# Available only in the current project (default)
claude mcp add --transport http --scope local pure-ide http://localhost:9010/mcp

# Shared with everyone via .mcp.json (committed to source control)
claude mcp add --transport http --scope project pure-ide http://localhost:9010/mcp

# Available across all your projects
claude mcp add --transport http --scope user pure-ide http://localhost:9010/mcp
```

**Using JSON configuration directly:**

```bash
claude mcp add-json pure-ide '{"type":"http","url":"http://localhost:9010/mcp"}'
```

**Project-scoped `.mcp.json` (share with your team):**

Create a `.mcp.json` file in your project root:

```json
{
  "mcpServers": {
    "pure-ide": {
      "type": "http",
      "url": "http://localhost:9010/mcp"
    }
  }
}
```

See the [Claude Code MCP documentation](https://code.claude.com/docs/en/mcp) for more details.

### VS Code (GitHub Copilot / Copilot Chat)

VS Code supports MCP servers for use with GitHub Copilot Chat.

**Create `.vscode/mcp.json` in your workspace:**

```json
{
  "servers": {
    "pure-ide": {
      "type": "http",
      "url": "http://localhost:9010/mcp"
    }
  }
}
```

Alternatively, for a stdio-based server:

```json
{
  "servers": {
    "pure-ide": {
      "command": "java",
      "args": ["-cp", "<classpath>", "org.finos.legend.engine.ide.mcp.PureIDEMcpStdioServer"]
    }
  }
}
```

VS Code will automatically discover the tools and make them available in Copilot Chat. See the [VS Code MCP documentation](https://code.visualstudio.com/docs/copilot/customization/mcp-servers) for more details.

### Cursor

Cursor supports MCP servers through its configuration file.

**Create `.cursor/mcp.json` in your project root:**

```json
{
  "mcpServers": {
    "pure-ide": {
      "transport": "sse",
      "url": "http://localhost:9010/mcp"
    }
  }
}
```

For stdio transport:

```json
{
  "mcpServers": {
    "pure-ide": {
      "command": "java",
      "args": ["-cp", "<classpath>", "org.finos.legend.engine.ide.mcp.PureIDEMcpStdioServer"]
    }
  }
}
```

### Claude Desktop

Claude Desktop supports local MCP servers via stdio transport.

**Edit your `claude_desktop_config.json`:**

- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "pure-ide": {
      "command": "java",
      "args": ["-cp", "<classpath>", "org.finos.legend.engine.ide.mcp.PureIDEMcpStdioServer"]
    }
  }
}
```

**Note:** Claude Desktop has limited support for remote HTTP servers. Use the stdio transport for the most reliable experience.

### Windsurf / Other MCP-Compatible Editors

Most MCP-compatible editors follow a similar configuration pattern. Create an MCP configuration file (usually `.mcp.json` or a tool-specific config) with either:

**HTTP transport:**
```json
{
  "mcpServers": {
    "pure-ide": {
      "type": "http",
      "url": "http://localhost:9010/mcp"
    }
  }
}
```

**Stdio transport:**
```json
{
  "mcpServers": {
    "pure-ide": {
      "command": "java",
      "args": ["-cp", "<classpath>", "org.finos.legend.engine.ide.mcp.PureIDEMcpStdioServer"]
    }
  }
}
```

### Custom MCP Clients

Any MCP client can connect by sending JSON-RPC 2.0 messages to the `/mcp` endpoint. The protocol flow is:

1. Send `initialize` request
2. Send `notifications/initialized` notification
3. Send `tools/list` to discover available tools
4. Send `tools/call` to invoke tools

Example session:

```
→ {"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}
← {"jsonrpc":"2.0","id":1,"result":{"serverInfo":{"name":"pure-ide-mcp-server","version":"1.0.0",...},...}}

→ {"jsonrpc":"2.0","method":"notifications/initialized","params":{}}

→ {"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
← {"jsonrpc":"2.0","id":2,"result":{"tools":[{"name":"read_file",...},...]}}

→ {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"find_pure_file","arguments":{"file":"grammar"}}}
← {"jsonrpc":"2.0","id":3,"result":{"content":[{"type":"text","text":"/platform/pure/grammar.pure\n..."}]}}
```

---

## Tool Reference

### read_file

Read the content of a Pure source file.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | Yes | The file path (e.g. `/platform/pure/grammar.pure`) |

### list_directory

List files and directories at a given path.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | Yes | The directory path (e.g. `/` or `/platform`) |

### find_in_sources

Search for text or regex pattern across all Pure sources.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | Yes | The search string or regex pattern |
| `regex` | boolean | No | Treat query as regex (default: false) |
| `caseSensitive` | boolean | No | Case sensitive search (default: true) |
| `limit` | integer | No | Max results to return (default: 100) |

### find_pure_file

Find Pure source files by name pattern.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | string | Yes | The file name or pattern to search for |
| `regex` | boolean | No | Treat file as regex (default: false) |

### create_file

Create a new Pure source file.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | Yes | The file path to create |
| `content` | string | No | Initial content for the file |

### delete_file

Delete a Pure source file.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | Yes | The file path to delete |

### execute_pure

Execute a Pure function by fully-qualified name.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `function` | string | Yes | The function name (e.g. `go():Any[*]`) |

### execute_go

Compile and execute a Pure code block. Writes code to a source file, compiles, and runs `go():Any[*]`.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `code` | string | Yes | The Pure code to compile and execute |
| `path` | string | No | Source file path (default: `/ide_mcp_exec.pure`) |

### run_tests

Run Pure tests at a given package path.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | Yes | The package path (e.g. `meta::pure::functions::string`) |
| `pctAdapter` | string | No | Optional PCT adapter |

### get_concept_info

Get concept info for a symbol at a given source location.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | string | Yes | The source file path |
| `line` | integer | Yes | Line number (1-based) |
| `column` | integer | Yes | Column number (1-based) |

### get_suggestions

Get code completion suggestions for a package path.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | string | Yes | The package path (e.g. `meta::pure::functions`) |
