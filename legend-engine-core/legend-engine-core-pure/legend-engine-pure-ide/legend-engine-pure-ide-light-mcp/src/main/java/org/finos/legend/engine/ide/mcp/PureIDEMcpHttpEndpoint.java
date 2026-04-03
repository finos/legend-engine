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
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
// implied. See the License for the specific language governing
// permissions and limitations under the License.

package org.finos.legend.engine.ide.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.finos.legend.engine.mcp.protocol.v20251125.notification.Notification;
import org.finos.legend.engine.mcp.protocol.v20251125.request.Request;
import org.finos.legend.engine.mcp.protocol.v20251125.response.Response;
import org.finos.legend.engine.mcp.server.orchestrator.LegendStatelessMcpServerOrchestrator;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(tags = "MCP")
@Path("/")
public class PureIDEMcpHttpEndpoint
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LegendStatelessMcpServerOrchestrator orchestrator;

    public PureIDEMcpHttpEndpoint(PureRuntime pureRuntime, MutableRepositoryCodeStorage codeStorage, FunctionExecution functionExecution)
    {
        this.orchestrator = PureIDEMcpToolDefinitions.createOrchestrator(pureRuntime, codeStorage, functionExecution);
    }

    @POST
    @Path("mcp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public javax.ws.rs.core.Response handleMcp(String body)
    {
        try
        {
            JsonNode node = OBJECT_MAPPER.readTree(body);

            if (node.has("id"))
            {
                return handleRequest(node);
            }
            else
            {
                return handleNotification(node);
            }
        }
        catch (Exception e)
        {
            return buildErrorResponse(e);
        }
    }

    private javax.ws.rs.core.Response handleRequest(JsonNode node) throws Exception
    {
        Request request = OBJECT_MAPPER.treeToValue(node, Request.class);
        Response response = this.orchestrator.handleRequest(request, Identity.getAnonymousIdentity());
        String responseJson = OBJECT_MAPPER.writeValueAsString(response);
        return javax.ws.rs.core.Response.ok(responseJson, MediaType.APPLICATION_JSON).build();
    }

    private javax.ws.rs.core.Response handleNotification(JsonNode node) throws Exception
    {
        Notification notification = OBJECT_MAPPER.treeToValue(node, Notification.class);
        this.orchestrator.handleNotification(notification, Identity.getAnonymousIdentity());
        return javax.ws.rs.core.Response.noContent().build();
    }

    private static javax.ws.rs.core.Response buildErrorResponse(Exception e)
    {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("jsonrpc", "2.0");
        error.put("id", null);
        Map<String, Object> errorObj = new LinkedHashMap<>();
        errorObj.put("code", -32700);
        errorObj.put("message", "Parse error: " + e.getMessage());
        error.put("error", errorObj);
        try
        {
            return javax.ws.rs.core.Response
                    .status(javax.ws.rs.core.Response.Status.BAD_REQUEST)
                    .entity(OBJECT_MAPPER.writeValueAsString(error))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        catch (Exception ex)
        {
            return javax.ws.rs.core.Response
                    .status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}
