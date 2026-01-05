// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.mcp.server.orchestrator;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.mcp.protocol.v20251125.implementation.Implementation;
import org.finos.legend.engine.mcp.protocol.v20251125.notification.Notification;
import org.finos.legend.engine.mcp.protocol.v20251125.request.Request;
import org.finos.legend.engine.mcp.protocol.v20251125.response.ErrorResponse;
import org.finos.legend.engine.mcp.protocol.v20251125.response.Response;
import org.finos.legend.engine.mcp.protocol.v20251125.response.ResultResponse;
import org.finos.legend.engine.mcp.protocol.v20251125.result.CallToolResult;
import org.finos.legend.engine.mcp.protocol.v20251125.result.EmptyResult;
import org.finos.legend.engine.mcp.protocol.v20251125.result.InitializeResult;
import org.finos.legend.engine.mcp.protocol.v20251125.result.ListToolsResult;
import org.finos.legend.engine.mcp.protocol.v20251125.result.content.ContentBlock;
import org.finos.legend.engine.mcp.protocol.v20251125.tool.Tool;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class LegendStatelessMcpServerOrchestrator
{
    public static final String PING_METHOD = "ping";
    public static final String INITIALIZE_METHOD = "initialize";
    public static final String LIST_TOOLS_METHOD = "tools/list";
    public static final String CALL_TOOL_METHOD = "tools/call";

    private static final int LIST_TOOLS_BATCH_SIZE = 10;

    private final Implementation serverInfo;
    private final List<Tool> tools;
    private final Function3<Tool, Map<String, Object>, Identity, List<ContentBlock>> toolExecutor;
    // Resources, prompts, ... can be added here


    public LegendStatelessMcpServerOrchestrator(
            final Implementation serverInfo,
            final List<Tool> tools,
            final Function3<Tool, Map<String, Object>, Identity, List<ContentBlock>> toolExecutor
    )
    {
        this.serverInfo = Objects.requireNonNull(serverInfo);
        this.tools = Objects.requireNonNull(tools);
        this.toolExecutor = Objects.requireNonNull(toolExecutor);
    }

    public void handleNotification(Notification notification, Identity identity)
    {
        // No-op
    }

    public Response handleRequest(Request request, Identity identity)
    {
        try
        {
            switch (request.getMethod())
            {
                case PING_METHOD:
                    return handlePingRequest(request);
                case INITIALIZE_METHOD:
                    return handleInitializeRequest(request);
                case LIST_TOOLS_METHOD:
                    return handleListToolsRequest(request);
                case CALL_TOOL_METHOD:
                    return handleCallToolRequest(request, identity);
                default:
                    return new ErrorResponse(
                            request.getId(),
                            new org.finos.legend.engine.mcp.protocol.v20251125.error.Error(
                                    -32601,
                                    null,
                                    "Unhandled method: " + request.getMethod()
                            )
                    );
            }
        }
        catch (Exception e)
        {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return new ErrorResponse(
                    request.getId(),
                    new org.finos.legend.engine.mcp.protocol.v20251125.error.Error(
                            -32603,
                            null,
                            "Internal error: " + sw
                    )
            );
        }
    }

    private Response handlePingRequest(Request request)
    {
        return new ResultResponse(request.getId(), new EmptyResult());
    }

    private Response handleInitializeRequest(Request request)
    {
        return new ResultResponse(
                request.getId(),
                new InitializeResult(
                        null,
                        Maps.mutable.of(
                                "tools", Maps.mutable.of(
                                        "listChanged", false
                                )
                        ),
                        null,
                        this.serverInfo
                )
        );
    }

    private Response handleListToolsRequest(Request request)
    {
        int page = ((request.getParams() == null) || (request.getParams().get("cursor") == null)) ? 0 : Integer.parseInt((String) request.getParams().get("cursor"));
        int startIndex = page * LIST_TOOLS_BATCH_SIZE;
        if (startIndex >= this.tools.size())
        {
            throw new RuntimeException("Invalid cursor: " + page);
        }
        int endIndex = Math.min(startIndex + LIST_TOOLS_BATCH_SIZE, this.tools.size());
        return new ResultResponse(
                request.getId(),
                new ListToolsResult(
                        null,
                        (endIndex < this.tools.size() ? String.valueOf(page + 1) : null),
                        this.tools.subList(startIndex, endIndex)
                )
        );
    }

    private Response handleCallToolRequest(Request request, Identity identity)
    {
        String toolName = (String) Objects.requireNonNull(request.getParams().get("name"));
        Tool tool = this.tools.stream().filter(t -> toolName.equals(t.getName())).findFirst().orElse(null);

        if (tool == null)
        {
            return new ErrorResponse(
                    request.getId(),
                    new org.finos.legend.engine.mcp.protocol.v20251125.error.Error(
                            -32602,
                            null,
                            "Tool not found: " + toolName
                    )
            );
        }

        Map<String, Object> toolParams = request.getParams().get("arguments") == null ? Collections.emptyMap() : (Map<String, Object>) request.getParams().get("arguments");
        return new ResultResponse(
                request.getId(),
                new CallToolResult(
                        null,
                        this.toolExecutor.value(tool, toolParams, identity),
                        false,
                        null
                )
        );
    }
}
