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

import org.finos.legend.engine.mcp.protocol.v20251125.tool.Tool;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPureReplToolDefinitions
{
    @Test
    public void testCreateToolsReturns8Tools()
    {
        List<Tool> tools = PureReplToolDefinitions.createTools();
        Assert.assertEquals(8, tools.size());
    }

    @Test
    public void testToolNames()
    {
        List<Tool> tools = PureReplToolDefinitions.createTools();
        Map<String, Tool> toolMap = new HashMap<>();
        for (Tool tool : tools)
        {
            toolMap.put(tool.getName(), tool);
        }

        Assert.assertTrue(toolMap.containsKey("evaluate_pure"));
        Assert.assertTrue(toolMap.containsKey("get_expression_type"));
        Assert.assertTrue(toolMap.containsKey("run_tests"));
        Assert.assertTrue(toolMap.containsKey("incremental_recompile"));
        Assert.assertTrue(toolMap.containsKey("full_recompile"));
        Assert.assertTrue(toolMap.containsKey("get_runtime_info"));
        Assert.assertTrue(toolMap.containsKey("set_runtime_option"));
        Assert.assertTrue(toolMap.containsKey("list_runtime_options"));
    }

    @Test
    public void testAllToolsHaveInputSchema()
    {
        List<Tool> tools = PureReplToolDefinitions.createTools();
        for (Tool tool : tools)
        {
            Assert.assertNotNull("Tool " + tool.getName() + " should have an input schema", tool.getInputSchema());
            Assert.assertEquals("object", tool.getInputSchema().getType());
        }
    }

    @Test
    public void testAllToolsHaveDescriptions()
    {
        List<Tool> tools = PureReplToolDefinitions.createTools();
        for (Tool tool : tools)
        {
            Assert.assertNotNull("Tool " + tool.getName() + " should have a description", tool.getDescription());
            Assert.assertFalse("Tool " + tool.getName() + " description should not be empty", tool.getDescription().isEmpty());
        }
    }

    @Test
    public void testEvaluatePureTool()
    {
        Tool tool = findTool("evaluate_pure");
        Assert.assertNotNull(tool.getAnnotations());
        Assert.assertTrue(tool.getAnnotations().getReadOnlyHint());
        Assert.assertTrue(tool.getInputSchema().getRequired().contains("expression"));
        Assert.assertTrue(tool.getInputSchema().getProperties().containsKey("expression"));
    }

    @Test
    public void testGetExpressionTypeTool()
    {
        Tool tool = findTool("get_expression_type");
        Assert.assertNotNull(tool.getAnnotations());
        Assert.assertTrue(tool.getAnnotations().getReadOnlyHint());
        Assert.assertTrue(tool.getAnnotations().getIdempotentHint());
        Assert.assertTrue(tool.getInputSchema().getRequired().contains("expression"));
    }

    @Test
    public void testRunTestsTool()
    {
        Tool tool = findTool("run_tests");
        Assert.assertNotNull(tool.getAnnotations());
        Assert.assertTrue(tool.getAnnotations().getReadOnlyHint());
        Assert.assertTrue(tool.getInputSchema().getRequired().contains("path"));
        Assert.assertTrue(tool.getInputSchema().getProperties().containsKey("pctAdapter"));
        Assert.assertTrue(tool.getInputSchema().getProperties().containsKey("filter"));
    }

    @Test
    public void testIncrementalRecompileTool()
    {
        Tool tool = findTool("incremental_recompile");
        Assert.assertNotNull(tool.getAnnotations());
        Assert.assertTrue(tool.getAnnotations().getIdempotentHint());
        Assert.assertTrue(tool.getInputSchema().getRequired().isEmpty());
    }

    @Test
    public void testFullRecompileTool()
    {
        Tool tool = findTool("full_recompile");
        Assert.assertNotNull(tool.getAnnotations());
        Assert.assertTrue(tool.getAnnotations().getIdempotentHint());
        Assert.assertTrue(tool.getInputSchema().getRequired().isEmpty());
    }

    @Test
    public void testGetRuntimeInfoTool()
    {
        Tool tool = findTool("get_runtime_info");
        Assert.assertNotNull(tool.getAnnotations());
        Assert.assertTrue(tool.getAnnotations().getReadOnlyHint());
        Assert.assertTrue(tool.getAnnotations().getIdempotentHint());
        Assert.assertTrue(tool.getInputSchema().getRequired().isEmpty());
    }

    @Test
    public void testSetRuntimeOptionTool()
    {
        Tool tool = findTool("set_runtime_option");
        Assert.assertNull(tool.getAnnotations());
        Assert.assertTrue(tool.getInputSchema().getRequired().contains("name"));
        Assert.assertTrue(tool.getInputSchema().getRequired().contains("value"));
    }

    @Test
    public void testListRuntimeOptionsTool()
    {
        Tool tool = findTool("list_runtime_options");
        Assert.assertNotNull(tool.getAnnotations());
        Assert.assertTrue(tool.getAnnotations().getReadOnlyHint());
        Assert.assertTrue(tool.getAnnotations().getIdempotentHint());
        Assert.assertTrue(tool.getInputSchema().getRequired().isEmpty());
    }

    private Tool findTool(String name)
    {
        List<Tool> tools = PureReplToolDefinitions.createTools();
        for (Tool tool : tools)
        {
            if (name.equals(tool.getName()))
            {
                return tool;
            }
        }
        Assert.fail("Tool not found: " + name);
        return null;
    }
}
