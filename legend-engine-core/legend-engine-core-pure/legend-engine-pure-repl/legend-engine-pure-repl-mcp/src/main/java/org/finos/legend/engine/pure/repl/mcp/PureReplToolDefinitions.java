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
import org.finos.legend.engine.mcp.protocol.v20251125.tool.ToolAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating MCP tool definitions that expose Pure REPL capabilities.
 */
public class PureReplToolDefinitions
{
    private PureReplToolDefinitions()
    {
        // static factory
    }

    public static List<Tool> createTools()
    {
        List<Tool> tools = new ArrayList<>();

        tools.add(createEvaluatePureTool());
        tools.add(createGetExpressionTypeTool());
        tools.add(createRunTestsTool());
        tools.add(createIncrementalRecompileTool());
        tools.add(createFullRecompileTool());
        tools.add(createGetRuntimeInfoTool());
        tools.add(createSetRuntimeOptionTool());
        tools.add(createListRuntimeOptionsTool());

        return tools;
    }

    private static Tool createEvaluatePureTool()
    {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("expression", prop("string", "The Pure expression to evaluate"));

        return new Tool(
                null,
                new ToolAnnotations(null, null, null, true, null),
                "Evaluate a Pure expression and return the result",
                null,
                null,
                new Tool.Schema(null, properties, Arrays.asList("expression")),
                "evaluate_pure",
                null,
                null
        );
    }

    private static Tool createGetExpressionTypeTool()
    {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("expression", prop("string", "The Pure expression to get the type of"));

        return new Tool(
                null,
                new ToolAnnotations(null, true, null, true, null),
                "Get the type of a Pure expression without executing it",
                null,
                null,
                new Tool.Schema(null, properties, Arrays.asList("expression")),
                "get_expression_type",
                null,
                null
        );
    }

    private static Tool createRunTestsTool()
    {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("path", prop("string", "The package path or test function path to run tests from"));
        properties.put("pctAdapter", prop("string", "Optional PCT adapter name"));
        properties.put("filter", prop("string", "Optional regex filter for test names"));

        return new Tool(
                null,
                new ToolAnnotations(null, null, null, true, null),
                "Run Pure tests at a specified path",
                null,
                null,
                new Tool.Schema(null, properties, Arrays.asList("path")),
                "run_tests",
                null,
                null
        );
    }

    private static Tool createIncrementalRecompileTool()
    {
        return new Tool(
                null,
                new ToolAnnotations(null, true, null, null, null),
                "Detect Pure source files changed on disk and incrementally recompile only the modified ones. Fast but does not pick up new files.",
                null,
                null,
                new Tool.Schema(null, Collections.emptyMap(), Collections.emptyList()),
                "incremental_recompile",
                null,
                null
        );
    }

    private static Tool createFullRecompileTool()
    {
        return new Tool(
                null,
                new ToolAnnotations(null, true, null, null, null),
                "Fully reset and reinitialize the Pure runtime from scratch. Slower but picks up all changes including new files.",
                null,
                null,
                new Tool.Schema(null, Collections.emptyMap(), Collections.emptyList()),
                "full_recompile",
                null,
                null
        );
    }

    private static Tool createGetRuntimeInfoTool()
    {
        return new Tool(
                null,
                new ToolAnnotations(null, true, null, true, null),
                "Get session info including repositories and memory usage",
                null,
                null,
                new Tool.Schema(null, Collections.emptyMap(), Collections.emptyList()),
                "get_runtime_info",
                null,
                null
        );
    }

    private static Tool createSetRuntimeOptionTool()
    {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", prop("string", "The runtime option name"));
        properties.put("value", prop("boolean", "The value to set"));

        return new Tool(
                null,
                null,
                "Set a Pure runtime option",
                null,
                null,
                new Tool.Schema(null, properties, Arrays.asList("name", "value")),
                "set_runtime_option",
                null,
                null
        );
    }

    private static Tool createListRuntimeOptionsTool()
    {
        return new Tool(
                null,
                new ToolAnnotations(null, true, null, true, null),
                "List all Pure runtime options and their current values",
                null,
                null,
                new Tool.Schema(null, Collections.emptyMap(), Collections.emptyList()),
                "list_runtime_options",
                null,
                null
        );
    }

    private static Map<String, Object> prop(String type, String description)
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("description", description);
        return map;
    }
}
