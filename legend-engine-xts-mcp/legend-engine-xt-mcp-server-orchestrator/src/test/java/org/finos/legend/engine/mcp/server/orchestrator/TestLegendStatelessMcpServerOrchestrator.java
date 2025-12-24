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

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.mcp.protocol.v20251125.implementation.Implementation;
import org.finos.legend.engine.mcp.protocol.v20251125.request.Request;
import org.finos.legend.engine.mcp.protocol.v20251125.response.ErrorResponse;
import org.finos.legend.engine.mcp.protocol.v20251125.response.Response;
import org.finos.legend.engine.mcp.protocol.v20251125.response.ResultResponse;
import org.finos.legend.engine.mcp.protocol.v20251125.result.CallToolResult;
import org.finos.legend.engine.mcp.protocol.v20251125.result.InitializeResult;
import org.finos.legend.engine.mcp.protocol.v20251125.result.ListToolsResult;
import org.finos.legend.engine.mcp.protocol.v20251125.tool.Tool;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class TestLegendStatelessMcpServerOrchestrator
{
    @Test
    public void testInitializeReturnsServerInfo() throws Exception
    {
        Implementation impl = new Implementation("desc", null, "test-server", "Test Server", "1.0", null);
        LegendStatelessMcpServerOrchestrator orchestrator = new LegendStatelessMcpServerOrchestrator(impl, Collections.emptyList(), (t, p, i) -> Collections.emptyList());

        Request req = new Request(1, LegendStatelessMcpServerOrchestrator.INITIALIZE_METHOD, Collections.emptyMap());
        Response resp = orchestrator.handleRequest(req, Identity.getAnonymousIdentity());
        Assert.assertTrue(resp instanceof ResultResponse);
        ResultResponse rr = (ResultResponse) resp;
        Assert.assertTrue(rr.getResult() instanceof InitializeResult);
        InitializeResult ir = (InitializeResult) rr.getResult();
        Assert.assertEquals(impl.getName(), ir.getServerInfo().getName());
        Assert.assertEquals(impl.getVersion(), ir.getServerInfo().getVersion());
    }

    @Test
    public void testListToolsPagination() throws Exception
    {
        Implementation impl = new Implementation("desc", null, "test-server", "Test Server", "1.0", null);
        List<Tool> tools = new ArrayList<>();
        for (int i = 0; i < 15; i++)
        {
            tools.add(new Tool(null, null, null, null, null, new Tool.Schema(null, null, null), "tool-" + i, null, null));
        }
        LegendStatelessMcpServerOrchestrator orchestrator = new LegendStatelessMcpServerOrchestrator(impl, tools, (t, p, i) -> Collections.emptyList());

        Response resp = orchestrator.handleRequest(new Request(1, LegendStatelessMcpServerOrchestrator.LIST_TOOLS_METHOD, Collections.emptyMap()), Identity.getAnonymousIdentity());
        Assert.assertTrue(resp instanceof ResultResponse);
        ResultResponse rr = (ResultResponse) resp;
        Assert.assertTrue(rr.getResult() instanceof ListToolsResult);
        ListToolsResult ltr = (ListToolsResult) rr.getResult();
        Assert.assertEquals(10, ltr.getTools().size());
        Assert.assertEquals("tool-0", ltr.getTools().get(0).getName());
        Assert.assertEquals("1", ltr.getNextCursor());

        // Fetch next page
        resp = orchestrator.handleRequest(new Request(2, LegendStatelessMcpServerOrchestrator.LIST_TOOLS_METHOD, Collections.singletonMap("cursor", "1")), Identity.getAnonymousIdentity());
        Assert.assertTrue(resp instanceof ResultResponse);
        rr = (ResultResponse) resp;
        Assert.assertTrue(rr.getResult() instanceof ListToolsResult);
        ltr = (ListToolsResult) rr.getResult();
        Assert.assertEquals(5, ltr.getTools().size());
        Assert.assertEquals("tool-10", ltr.getTools().get(0).getName());
        Assert.assertNull(ltr.getNextCursor());

        // Fetch with invalid cursor
        resp = orchestrator.handleRequest(new Request(3, LegendStatelessMcpServerOrchestrator.LIST_TOOLS_METHOD, Collections.singletonMap("cursor", "2")), Identity.getAnonymousIdentity());
        Assert.assertTrue(resp instanceof ErrorResponse);
        ErrorResponse er = (ErrorResponse) resp;
        Assert.assertTrue(er.getError().getMessage().contains("Invalid cursor"));
    }

    @Test
    public void testCallToolSuccessAndNotFound() throws Exception
    {
        Implementation impl = new Implementation("desc", null, "test-server", "Test Server", "1.0", null);
        List<Tool> tools = new ArrayList<>();
        tools.add(new Tool(null, null, null, null, null, new Tool.Schema(null, null, null), "t1", null, null));

        LegendStatelessMcpServerOrchestrator orchestrator = new LegendStatelessMcpServerOrchestrator(impl, tools, (t, p, i) -> Collections.emptyList());

        // Successful call
        Map<String, Object> params = new HashMap<>();
        params.put("name", "t1");
        params.put("arguments", Collections.emptyMap());
        Response resp = orchestrator.handleRequest(new Request(3, LegendStatelessMcpServerOrchestrator.CALL_TOOL_METHOD, params), Identity.getAnonymousIdentity());
        Assert.assertTrue(resp instanceof ResultResponse);
        Assert.assertTrue(((ResultResponse) resp).getResult() instanceof CallToolResult);

        // Not found
        Response resp2 = orchestrator.handleRequest(new Request(4, LegendStatelessMcpServerOrchestrator.CALL_TOOL_METHOD, Maps.mutable.of("name", "unknown")), Identity.getAnonymousIdentity());
        Assert.assertTrue(resp2 instanceof ErrorResponse);
        Assert.assertTrue(((ErrorResponse) resp2).getError().getMessage().contains("Tool not found"));
    }
}
