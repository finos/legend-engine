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

package org.finos.legend.engine.ide.mcp;

import org.finos.legend.engine.mcp.protocol.v20251125.implementation.Implementation;
import org.finos.legend.engine.mcp.protocol.v20251125.request.Request;
import org.finos.legend.engine.mcp.protocol.v20251125.response.ResultResponse;
import org.finos.legend.engine.mcp.protocol.v20251125.response.Response;
import org.finos.legend.engine.mcp.protocol.v20251125.result.InitializeResult;
import org.finos.legend.engine.mcp.protocol.v20251125.result.ListToolsResult;
import org.finos.legend.engine.mcp.protocol.v20251125.tool.Tool;
import org.finos.legend.engine.mcp.server.orchestrator.LegendStatelessMcpServerOrchestrator;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class TestPureIDEMcpToolDefinitions
{
    @Test
    public void testToolListCompleteness()
    {
        List<Tool> tools = PureIDEMcpToolDefinitions.getTools();
        Assert.assertEquals(11, tools.size());

        Set<String> toolNames = new HashSet<>();
        for (Tool tool : tools)
        {
            toolNames.add(tool.getName());
            Assert.assertNotNull("Tool " + tool.getName() + " should have a description", tool.getDescription());
            Assert.assertNotNull("Tool " + tool.getName() + " should have an input schema", tool.getInputSchema());
        }

        Assert.assertTrue(toolNames.contains("read_file"));
        Assert.assertTrue(toolNames.contains("list_directory"));
        Assert.assertTrue(toolNames.contains("find_in_sources"));
        Assert.assertTrue(toolNames.contains("find_pure_file"));
        Assert.assertTrue(toolNames.contains("create_file"));
        Assert.assertTrue(toolNames.contains("delete_file"));
        Assert.assertTrue(toolNames.contains("execute_pure"));
        Assert.assertTrue(toolNames.contains("execute_go"));
        Assert.assertTrue(toolNames.contains("run_tests"));
        Assert.assertTrue(toolNames.contains("get_concept_info"));
        Assert.assertTrue(toolNames.contains("get_suggestions"));
    }

    @Test
    public void testToolSchemas()
    {
        List<Tool> tools = PureIDEMcpToolDefinitions.getTools();

        for (Tool tool : tools)
        {
            Assert.assertNotNull("Tool " + tool.getName() + " must have inputSchema", tool.getInputSchema());
            Assert.assertEquals("object", tool.getInputSchema().getType());
            Assert.assertNotNull("Tool " + tool.getName() + " must have required fields", tool.getInputSchema().getRequired());
            Assert.assertFalse("Tool " + tool.getName() + " must have at least one required field", tool.getInputSchema().getRequired().isEmpty());
        }

        // Check specific tool schemas
        Tool readFile = tools.stream().filter(t -> "read_file".equals(t.getName())).findFirst().orElse(null);
        Assert.assertNotNull(readFile);
        Assert.assertTrue(readFile.getInputSchema().getProperties().containsKey("path"));
        Assert.assertEquals(Collections.singletonList("path"), readFile.getInputSchema().getRequired());

        Tool findInSources = tools.stream().filter(t -> "find_in_sources".equals(t.getName())).findFirst().orElse(null);
        Assert.assertNotNull(findInSources);
        Assert.assertTrue(findInSources.getInputSchema().getProperties().containsKey("query"));
        Assert.assertTrue(findInSources.getInputSchema().getProperties().containsKey("regex"));
        Assert.assertTrue(findInSources.getInputSchema().getProperties().containsKey("caseSensitive"));
        Assert.assertTrue(findInSources.getInputSchema().getProperties().containsKey("limit"));

        Tool getConceptInfo = tools.stream().filter(t -> "get_concept_info".equals(t.getName())).findFirst().orElse(null);
        Assert.assertNotNull(getConceptInfo);
        Assert.assertEquals(Arrays.asList("file", "line", "column"), getConceptInfo.getInputSchema().getRequired());
    }

    @Test
    public void testServerInfo()
    {
        Implementation impl = PureIDEMcpToolDefinitions.getServerInfo();
        Assert.assertEquals("pure-ide-mcp-server", impl.getName());
        Assert.assertEquals("1.0.0", impl.getVersion());
        Assert.assertEquals("Pure IDE MCP Server", impl.getTitle());
    }

    @Test
    public void testInitializeRequest()
    {
        // Use orchestrator with a null-safe executor (tools won't be called in this test)
        LegendStatelessMcpServerOrchestrator orchestrator = new LegendStatelessMcpServerOrchestrator(
                PureIDEMcpToolDefinitions.getServerInfo(),
                PureIDEMcpToolDefinitions.getTools(),
                (t, p, i) -> Collections.emptyList()
        );

        Request req = new Request(1, LegendStatelessMcpServerOrchestrator.INITIALIZE_METHOD, Collections.emptyMap());
        Response resp = orchestrator.handleRequest(req, Identity.getAnonymousIdentity());
        Assert.assertTrue(resp instanceof ResultResponse);
        ResultResponse rr = (ResultResponse) resp;
        Assert.assertTrue(rr.getResult() instanceof InitializeResult);
        InitializeResult ir = (InitializeResult) rr.getResult();
        Assert.assertEquals("pure-ide-mcp-server", ir.getServerInfo().getName());
        Assert.assertEquals("1.0.0", ir.getServerInfo().getVersion());
    }

    @Test
    public void testToolListPagination()
    {
        LegendStatelessMcpServerOrchestrator orchestrator = new LegendStatelessMcpServerOrchestrator(
                PureIDEMcpToolDefinitions.getServerInfo(),
                PureIDEMcpToolDefinitions.getTools(),
                (t, p, i) -> Collections.emptyList()
        );

        // Page 0: should return 10 tools
        Response resp = orchestrator.handleRequest(
                new Request(1, LegendStatelessMcpServerOrchestrator.LIST_TOOLS_METHOD, Collections.emptyMap()),
                Identity.getAnonymousIdentity()
        );
        Assert.assertTrue(resp instanceof ResultResponse);
        ListToolsResult ltr = (ListToolsResult) ((ResultResponse) resp).getResult();
        Assert.assertEquals(10, ltr.getTools().size());
        Assert.assertEquals("1", ltr.getNextCursor());

        // Page 1: should return 1 tool
        resp = orchestrator.handleRequest(
                new Request(2, LegendStatelessMcpServerOrchestrator.LIST_TOOLS_METHOD, Collections.singletonMap("cursor", "1")),
                Identity.getAnonymousIdentity()
        );
        Assert.assertTrue(resp instanceof ResultResponse);
        ltr = (ListToolsResult) ((ResultResponse) resp).getResult();
        Assert.assertEquals(1, ltr.getTools().size());
        Assert.assertNull(ltr.getNextCursor());
    }

    @Test
    public void testToolAnnotations()
    {
        List<Tool> tools = PureIDEMcpToolDefinitions.getTools();

        // read_file should be read-only
        Tool readFile = tools.stream().filter(t -> "read_file".equals(t.getName())).findFirst().orElse(null);
        Assert.assertNotNull(readFile);
        Assert.assertNotNull(readFile.getAnnotations());
        Assert.assertTrue(readFile.getAnnotations().getReadOnlyHint());
        Assert.assertFalse(readFile.getAnnotations().getDestructiveHint());

        // create_file should be destructive, not read-only
        Tool createFile = tools.stream().filter(t -> "create_file".equals(t.getName())).findFirst().orElse(null);
        Assert.assertNotNull(createFile);
        Assert.assertNotNull(createFile.getAnnotations());
        Assert.assertTrue(createFile.getAnnotations().getDestructiveHint());
        Assert.assertFalse(createFile.getAnnotations().getReadOnlyHint());

        // delete_file should be destructive
        Tool deleteFile = tools.stream().filter(t -> "delete_file".equals(t.getName())).findFirst().orElse(null);
        Assert.assertNotNull(deleteFile);
        Assert.assertTrue(deleteFile.getAnnotations().getDestructiveHint());
    }
}
