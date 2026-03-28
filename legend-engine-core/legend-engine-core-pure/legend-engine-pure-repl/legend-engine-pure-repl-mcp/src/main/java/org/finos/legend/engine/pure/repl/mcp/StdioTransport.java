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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.finos.legend.engine.mcp.protocol.v20251125.notification.Notification;
import org.finos.legend.engine.mcp.protocol.v20251125.request.Request;
import org.finos.legend.engine.mcp.protocol.v20251125.response.Response;
import org.finos.legend.engine.mcp.server.orchestrator.LegendStatelessMcpServerOrchestrator;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * MCP stdio transport: reads JSON-RPC messages from an input stream (one per line),
 * dispatches to the orchestrator, and writes responses to an output stream.
 * Stdout is exclusively for MCP JSON-RPC messages; all logging goes to stderr.
 */
public class StdioTransport
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StdioTransport.class);

    private final LegendStatelessMcpServerOrchestrator orchestrator;
    private final Identity identity;
    private final InputStream inputStream;
    private final PrintStream outputStream;
    private final ObjectMapper objectMapper;

    public StdioTransport(LegendStatelessMcpServerOrchestrator orchestrator, Identity identity)
    {
        this(orchestrator, identity, System.in, System.out);
    }

    public StdioTransport(LegendStatelessMcpServerOrchestrator orchestrator, Identity identity,
                          InputStream inputStream, PrintStream outputStream)
    {
        this.orchestrator = orchestrator;
        this.identity = identity;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * Runs the transport loop, reading lines from input and dispatching to the orchestrator.
     * Exits when EOF is reached (readLine returns null).
     */
    public void run() throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null)
        {
            line = line.trim();
            if (line.isEmpty())
            {
                continue;
            }

            try
            {
                JsonNode node = objectMapper.readTree(line);

                if (node.has("id"))
                {
                    // It's a request (has an id)
                    Request request = objectMapper.treeToValue(node, Request.class);
                    Response response = orchestrator.handleRequest(request, identity);
                    String responseJson = objectMapper.writeValueAsString(response);
                    outputStream.println(responseJson);
                    outputStream.flush();
                }
                else
                {
                    // It's a notification (no id)
                    Notification notification = objectMapper.treeToValue(node, Notification.class);
                    orchestrator.handleNotification(notification, identity);
                    // No response for notifications
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Error processing message", e);
                // Write JSON-RPC parse error
                String errorResponse = "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"Parse error: " +
                        escapeJson(e.getMessage()) + "\"}}";
                outputStream.println(errorResponse);
                outputStream.flush();
            }
        }
    }

    private static String escapeJson(String s)
    {
        if (s == null)
        {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
