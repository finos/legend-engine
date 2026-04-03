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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.engine.mcp.protocol.v20251125.result.content.ContentBlock;
import org.finos.legend.engine.mcp.protocol.v20251125.result.content.TextContent;
import org.finos.legend.engine.mcp.protocol.v20251125.tool.Tool;
import org.finos.legend.engine.pure.repl.core.EvaluationResult;
import org.finos.legend.engine.pure.repl.core.OutputFormatter;
import org.finos.legend.engine.pure.repl.core.ReplEngine;
import org.finos.legend.engine.pure.repl.core.ReplSession;
import org.finos.legend.engine.pure.repl.core.TestResults;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes MCP tool calls to ReplEngine/ReplSession operations.
 */
public class PureReplToolExecutor implements Function3<Tool, Map<String, Object>, Identity, List<ContentBlock>>
{
    private final ReplSession session;
    private final ReplEngine engine;
    private final OutputFormatter outputFormatter;
    private final ObjectMapper objectMapper;

    public PureReplToolExecutor(ReplSession session, ReplEngine engine)
    {
        this.session = session;
        this.engine = engine;
        this.outputFormatter = new OutputFormatter(OutputFormatter.OutputFormat.JSON);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public List<ContentBlock> value(Tool tool, Map<String, Object> params, Identity identity)
    {
        try
        {
            switch (tool.getName())
            {
                case "evaluate_pure":
                    return handleEvaluatePure(params);
                case "get_expression_type":
                    return handleGetExpressionType(params);
                case "run_tests":
                    return handleRunTests(params);
                case "incremental_recompile":
                    return handleIncrementalRecompile();
                case "full_recompile":
                    return handleFullRecompile();
                case "get_runtime_info":
                    return handleGetRuntimeInfo();
                case "set_runtime_option":
                    return handleSetRuntimeOption(params);
                case "list_runtime_options":
                    return handleListRuntimeOptions();
                default:
                    return textResult(errorJson("UnknownTool", "Unknown tool: " + tool.getName()));
            }
        }
        catch (Exception e)
        {
            return textResult(errorJson(e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    private List<ContentBlock> handleEvaluatePure(Map<String, Object> params) throws Exception
    {
        String expression = (String) params.get("expression");
        EvaluationResult result = engine.evaluate(expression);
        return textResult(outputFormatter.formatEvaluationResultAsJson(result));
    }

    private List<ContentBlock> handleGetExpressionType(Map<String, Object> params) throws Exception
    {
        String expression = (String) params.get("expression");
        String type = engine.getExpressionType(expression);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("expression", expression);
        json.put("type", type);
        return textResult(toJson(json));
    }

    private List<ContentBlock> handleRunTests(Map<String, Object> params)
    {
        String path = (String) params.get("path");
        String pctAdapter = (String) params.get("pctAdapter");
        String filter = (String) params.get("filter");
        TestResults results = engine.runTests(path, pctAdapter, filter);
        return textResult(outputFormatter.formatTestResultsAsJson(results));
    }

    private List<ContentBlock> handleIncrementalRecompile()
    {
        long startTime = System.currentTimeMillis();
        int refreshed = session.refreshAndCompile();
        long durationMs = System.currentTimeMillis() - startTime;
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("status", "success");
        json.put("mode", "incremental");
        json.put("sourcesRefreshed", refreshed);
        json.put("durationMs", durationMs);
        return textResult(toJson(json));
    }

    private List<ContentBlock> handleFullRecompile()
    {
        long startTime = System.currentTimeMillis();
        session.fullRecompile();
        long durationMs = System.currentTimeMillis() - startTime;
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("status", "success");
        json.put("mode", "full");
        json.put("durationMs", durationMs);
        return textResult(toJson(json));
    }

    private List<ContentBlock> handleGetRuntimeInfo()
    {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("initialized", session.isInitialized());
        json.put("repositoryCount", session.getRepositoryCount());
        json.put("repositories", session.getRepositoryNames().toList());

        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("usedMb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        memory.put("totalMb", runtime.totalMemory() / (1024 * 1024));
        memory.put("maxMb", runtime.maxMemory() / (1024 * 1024));
        json.put("memory", memory);

        json.put("runtimeOptions", session.getAllPureRuntimeOptions());

        return textResult(toJson(json));
    }

    private List<ContentBlock> handleSetRuntimeOption(Map<String, Object> params)
    {
        String name = (String) params.get("name");
        Object valueObj = params.get("value");
        boolean value;
        if (valueObj instanceof Boolean)
        {
            value = (Boolean) valueObj;
        }
        else
        {
            value = Boolean.parseBoolean(String.valueOf(valueObj));
        }
        session.setPureRuntimeOption(name, value);

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("status", "success");
        json.put("name", name);
        json.put("value", value);
        return textResult(toJson(json));
    }

    private List<ContentBlock> handleListRuntimeOptions()
    {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("options", session.getAllPureRuntimeOptions());
        return textResult(toJson(json));
    }

    private List<ContentBlock> textResult(String text)
    {
        return Collections.singletonList(new TextContent(null, null, text));
    }

    private String errorJson(String errorType, String message)
    {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("status", "error");
        json.put("errorType", errorType);
        json.put("message", message);
        return toJson(json);
    }

    private String toJson(Map<String, Object> map)
    {
        try
        {
            return objectMapper.writeValueAsString(map);
        }
        catch (JsonProcessingException e)
        {
            return "{\"status\":\"error\",\"message\":\"JSON serialization failed: " + e.getMessage() + "\"}";
        }
    }
}
