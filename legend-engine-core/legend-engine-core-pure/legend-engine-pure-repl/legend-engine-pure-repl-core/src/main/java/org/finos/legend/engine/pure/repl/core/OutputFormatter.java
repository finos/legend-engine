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

package org.finos.legend.engine.pure.repl.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formats REPL output for text or JSON modes.
 */
public class OutputFormatter
{
    public enum OutputFormat
    {
        TEXT,
        JSON
    }

    private static final String JSON_SCHEMA_RESULT = "pure-repl-result-v1";
    private static final String JSON_SCHEMA_TEST = "pure-repl-test-v1";

    private final OutputFormat format;
    private final ObjectMapper objectMapper;
    private final PrintStream outputStream;
    private final PrintStream errorStream;

    public OutputFormatter(OutputFormat format)
    {
        this(format, System.out, System.err);
    }

    public OutputFormatter(OutputFormat format, PrintStream outputStream, PrintStream errorStream)
    {
        this.format = format;
        this.outputStream = outputStream;
        this.errorStream = errorStream;
        this.objectMapper = createObjectMapper();
    }

    private ObjectMapper createObjectMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    public OutputFormat getFormat()
    {
        return format;
    }

    /**
     * Formats and outputs an evaluation result.
     */
    public void formatEvaluationResult(EvaluationResult result)
    {
        if (format == OutputFormat.JSON)
        {
            outputStream.println(formatEvaluationResultAsJson(result));
        }
        else
        {
            outputStream.println(result.toDisplayString());
        }
    }

    /**
     * Formats an evaluation result as JSON.
     */
    public String formatEvaluationResultAsJson(EvaluationResult result)
    {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("$schema", JSON_SCHEMA_RESULT);
        json.put("status", result.isSuccess() ? "success" : "error");
        json.put("expression", result.getExpression());

        if (result.isSuccess())
        {
            json.put("type", result.getType());
            json.put("result", parseResultValue(result.getResult()));

            if (result.getConsoleOutput() != null && !result.getConsoleOutput().isEmpty())
            {
                json.put("consoleOutput", result.getConsoleOutput());
            }

            Map<String, Object> timing = new LinkedHashMap<>();
            timing.put("parseMs", result.getParseMs());
            timing.put("compileMs", result.getCompileMs());
            timing.put("executeMs", result.getExecuteMs());
            json.put("timing", timing);
        }
        else
        {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("type", result.getErrorType());
            error.put("message", result.getErrorMessage());

            if (result.getSource() != null)
            {
                error.put("source", result.getSource());
            }
            if (result.getLine() != null)
            {
                error.put("line", result.getLine());
            }
            if (result.getColumn() != null)
            {
                error.put("column", result.getColumn());
            }
            if (result.getStackTrace() != null)
            {
                error.put("stackTrace", result.getStackTrace());
            }

            json.put("error", error);
        }

        return toJsonString(json);
    }

    /**
     * Formats and outputs test results.
     */
    public void formatTestResults(TestResults results)
    {
        if (format == OutputFormat.JSON)
        {
            outputStream.println(formatTestResultsAsJson(results));
        }
        else
        {
            outputStream.println(results.toDisplayString());
        }
    }

    /**
     * Formats test results as JSON.
     */
    public String formatTestResultsAsJson(TestResults results)
    {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("$schema", JSON_SCHEMA_TEST);
        json.put("status", results.getStatus().name().toLowerCase());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", results.getTotalTests());
        summary.put("passed", results.getPassedTests());
        summary.put("failed", results.getFailedTests());
        summary.put("skipped", results.getSkippedTests());
        summary.put("durationMs", results.getDurationMs());
        json.put("summary", summary);

        if (results.getErrorMessage() != null)
        {
            json.put("errorMessage", results.getErrorMessage());
        }

        List<Map<String, Object>> tests = new ArrayList<>();
        for (TestResults.TestResult test : results.getTestResults())
        {
            Map<String, Object> testJson = new LinkedHashMap<>();
            testJson.put("name", test.getName());
            testJson.put("status", test.getStatus().name());
            testJson.put("durationMs", test.getDurationMs());

            if (test.getConsoleOutput() != null && !test.getConsoleOutput().isEmpty())
            {
                testJson.put("console", test.getConsoleOutput());
            }

            if (test.getErrorMessage() != null)
            {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("message", test.getErrorMessage());
                testJson.put("error", error);
            }

            tests.add(testJson);
        }
        json.put("tests", tests);

        return toJsonString(json);
    }

    /**
     * Formats and outputs runtime info.
     */
    public void formatRuntimeInfo(ReplSession session)
    {
        if (format == OutputFormat.JSON)
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("repositoryCount", session.getRepositoryCount());
            json.put("repositories", session.getRepositoryNames().toList());
            json.put("initialized", session.isInitialized());

            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memory = new LinkedHashMap<>();
            memory.put("usedMb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
            memory.put("totalMb", runtime.totalMemory() / (1024 * 1024));
            memory.put("maxMb", runtime.maxMemory() / (1024 * 1024));
            json.put("memory", memory);

            outputStream.println(toJsonString(json));
        }
        else
        {
            outputStream.println("Pure REPL");
            outputStream.println("=========");
            outputStream.println("Repositories loaded: " + session.getRepositoryCount());
            outputStream.println("Initialized: " + session.isInitialized());

            Runtime runtime = Runtime.getRuntime();
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long maxMb = runtime.maxMemory() / (1024 * 1024);
            outputStream.println("Memory: " + usedMb + "MB / " + maxMb + "MB");
        }
    }

    /**
     * Formats and outputs a type string.
     */
    public void formatType(String expression, String type)
    {
        if (format == OutputFormat.JSON)
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("expression", expression);
            json.put("type", type);
            outputStream.println(toJsonString(json));
        }
        else
        {
            outputStream.println(type);
        }
    }

    /**
     * Formats and outputs an error message.
     */
    public void formatError(String message)
    {
        if (format == OutputFormat.JSON)
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("status", "error");
            Map<String, Object> errorMap = new LinkedHashMap<>();
            errorMap.put("message", message);
            json.put("error", errorMap);
            outputStream.println(toJsonString(json));
        }
        else
        {
            errorStream.println("Error: " + message);
        }
    }

    /**
     * Formats and outputs a message.
     */
    public void formatMessage(String message)
    {
        if (format == OutputFormat.JSON)
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("message", message);
            outputStream.println(toJsonString(json));
        }
        else
        {
            outputStream.println(message);
        }
    }

    /**
     * Parses a result value string into an appropriate type for JSON serialization.
     */
    private Object parseResultValue(String result)
    {
        if (result == null)
        {
            return null;
        }

        // Try to parse as integer
        try
        {
            return Long.parseLong(result);
        }
        catch (NumberFormatException e)
        {
            // Not an integer
        }

        // Try to parse as float
        try
        {
            return Double.parseDouble(result);
        }
        catch (NumberFormatException e)
        {
            // Not a float
        }

        // Check for boolean
        if ("true".equals(result))
        {
            return true;
        }
        if ("false".equals(result))
        {
            return false;
        }

        // Check for string (quoted)
        if (result.startsWith("'") && result.endsWith("'"))
        {
            return result.substring(1, result.length() - 1);
        }

        // Return as-is
        return result;
    }

    /**
     * Converts a map to a JSON string.
     */
    private String toJsonString(Map<String, Object> map)
    {
        try
        {
            return objectMapper.writeValueAsString(map);
        }
        catch (JsonProcessingException e)
        {
            // Fallback to simple representation
            return map.toString();
        }
    }
}
