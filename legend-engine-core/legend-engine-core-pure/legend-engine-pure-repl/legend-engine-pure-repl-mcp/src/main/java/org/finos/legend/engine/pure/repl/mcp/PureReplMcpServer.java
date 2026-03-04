// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.pure.repl.mcp;

import org.finos.legend.engine.mcp.protocol.v20251125.implementation.Implementation;
import org.finos.legend.engine.mcp.protocol.v20251125.tool.Tool;
import org.finos.legend.engine.mcp.server.orchestrator.LegendStatelessMcpServerOrchestrator;
import org.finos.legend.engine.pure.repl.core.ReplEngine;
import org.finos.legend.engine.pure.repl.core.ReplSession;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Arrays;
import java.util.List;

/**
 * Entry point for the Pure REPL MCP Server.
 * Exposes Pure REPL capabilities as MCP tools over stdio transport.
 */
public class PureReplMcpServer
{
    public static void main(String[] args) throws Exception
    {
        String sourceRoot = null;
        List<String> repositories = null;
        long timeout = 30000;

        // Simple arg parsing
        for (int i = 0; i < args.length; i++)
        {
            switch (args[i])
            {
                case "--source-root":
                    if (i + 1 < args.length)
                    {
                        sourceRoot = args[++i];
                    }
                    break;
                case "--repositories":
                    if (i + 1 < args.length)
                    {
                        repositories = Arrays.asList(args[++i].split(","));
                    }
                    break;
                case "--timeout":
                    if (i + 1 < args.length)
                    {
                        timeout = Long.parseLong(args[++i]);
                    }
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    System.err.println("Usage: pure-repl-mcp [--source-root <path>] [--repositories <csv>] [--timeout <ms>]");
                    System.exit(1);
            }
        }

        System.err.println("Starting Pure REPL MCP Server...");

        // Create and initialize REPL session
        ReplSession session = new ReplSession(sourceRoot, repositories);
        session.initialize();

        // Create engine
        ReplEngine engine = new ReplEngine(session);
        engine.setTimeout(timeout);

        // Build MCP components
        List<Tool> tools = PureReplToolDefinitions.createTools();
        PureReplToolExecutor executor = new PureReplToolExecutor(session, engine);
        Implementation impl = new Implementation(
                "Pure REPL MCP Server - evaluate Pure expressions via MCP",
                null,
                "pure-repl-mcp",
                null,
                "1.0.0",
                null
        );
        LegendStatelessMcpServerOrchestrator orchestrator =
                new LegendStatelessMcpServerOrchestrator(impl, tools, executor);

        System.err.println("MCP server ready.");

        // Run transport (blocks until EOF)
        try
        {
            StdioTransport transport = new StdioTransport(orchestrator, Identity.getAnonymousIdentity());
            transport.run();
        }
        finally
        {
            engine.shutdown();
            session.close();
        }
    }
}
