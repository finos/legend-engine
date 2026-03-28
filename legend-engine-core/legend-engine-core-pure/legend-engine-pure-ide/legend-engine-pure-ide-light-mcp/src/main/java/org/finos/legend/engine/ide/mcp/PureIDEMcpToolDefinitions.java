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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.engine.mcp.protocol.v20251125.implementation.Implementation;
import org.finos.legend.engine.mcp.protocol.v20251125.result.content.ContentBlock;
import org.finos.legend.engine.mcp.protocol.v20251125.result.content.TextContent;
import org.finos.legend.engine.mcp.protocol.v20251125.tool.Tool;
import org.finos.legend.engine.mcp.protocol.v20251125.tool.ToolAnnotations;
import org.finos.legend.engine.mcp.server.orchestrator.LegendStatelessMcpServerOrchestrator;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.SourceCoordinates;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PureIDEMcpToolDefinitions
{
    private static final String SERVER_NAME = "pure-ide-mcp-server";
    private static final String SERVER_TITLE = "Pure IDE MCP Server";
    private static final String SERVER_VERSION = "1.0.0";
    private static final String SERVER_DESCRIPTION = "MCP server exposing Pure IDE capabilities";

    private PureIDEMcpToolDefinitions()
    {
    }

    public static Implementation getServerInfo()
    {
        return new Implementation(SERVER_DESCRIPTION, null, SERVER_NAME, SERVER_TITLE, SERVER_VERSION, null);
    }

    public static List<Tool> getTools()
    {
        List<Tool> tools = new ArrayList<>();
        addReadTools(tools);
        addWriteTools(tools);
        addExecutionTools(tools);
        addNavigationTools(tools);
        return tools;
    }

    public static Function3<Tool, Map<String, Object>, Identity, List<ContentBlock>> getToolExecutor(PureRuntime pureRuntime, MutableRepositoryCodeStorage codeStorage, FunctionExecution functionExecution)
    {
        return (tool, params, identity) ->
        {
            try
            {
                return dispatch(tool, params, pureRuntime, codeStorage, functionExecution);
            }
            catch (Exception e)
            {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                return Collections.singletonList(new TextContent(null, null, "Error: " + e.getMessage() + "\n" + sw));
            }
        };
    }

    public static LegendStatelessMcpServerOrchestrator createOrchestrator(PureRuntime pureRuntime, MutableRepositoryCodeStorage codeStorage, FunctionExecution functionExecution)
    {
        return new LegendStatelessMcpServerOrchestrator(
                getServerInfo(),
                getTools(),
                getToolExecutor(pureRuntime, codeStorage, functionExecution));
    }

    private static void addReadTools(List<Tool> tools)
    {
        tools.add(createTool(
                "read_file",
                "Read the content of a Pure source file",
                "Read File",
                mapOf("path", mapOf(
                        "type", "string",
                        "description", "The file path (e.g. /platform/pure/grammar.pure)"
                )),
                Collections.singletonList("path"),
                false,
                true
        ));

        tools.add(createTool(
                "list_directory",
                "List files and directories at a given path",
                "List Directory",
                mapOf("path", mapOf(
                        "type", "string",
                        "description", "The directory path (e.g. / or /platform)"
                )),
                Collections.singletonList("path"),
                false,
                true
        ));

        tools.add(createTool(
                "find_in_sources",
                "Search for text or regex pattern across all Pure sources. Returns matching locations with preview context.",
                "Find in Sources",
                buildFindInSourcesProps(),
                Collections.singletonList("query"),
                false,
                true
        ));

        tools.add(createTool(
                "find_pure_file",
                "Find Pure source files by name pattern",
                "Find Pure File",
                mapOf(
                        "file", mapOf(
                                "type", "string",
                                "description", "The file name or pattern to search for"
                        ),
                        "regex", mapOf(
                                "type", "boolean",
                                "description", "Whether to treat file as a regex pattern (default false)"
                        )
                ),
                Collections.singletonList("file"),
                false,
                true
        ));
    }

    private static Tool createTool(String name, String description, String displayName, Map<String, Object> properties, List<String> required, boolean destructiveHint, boolean idempotentHint)
    {
        return new Tool(
                null,
                getAnnotations(destructiveHint, idempotentHint),
                description,
                null,
                null,
                new Tool.Schema(null, properties, required),
                name,
                null,
                displayName
        );
    }

    private static ToolAnnotations getAnnotations(boolean destructiveHint, boolean idempotentHint)
    {
        return new ToolAnnotations(destructiveHint, idempotentHint, false, idempotentHint, null);
    }

    private static Map<String, Object> buildFindInSourcesProps()
    {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("query", mapOf(
                "type", "string",
                "description",
                "The search string or"
                        + " regex pattern"));
        props.put("regex", mapOf(
                "type", "boolean",
                "description",
                "Whether to treat query as a"
                        + " regex pattern"
                        + " (default false)"));
        props.put("caseSensitive", mapOf(
                "type", "boolean",
                "description",
                "Whether search is case"
                        + " sensitive"
                        + " (default true)"));
        props.put("limit", mapOf(
                "type", "integer",
                "description",
                "Maximum number of results"
                        + " to return"));
        return props;
    }

    private static void addWriteTools(List<Tool> tools)
    {
        tools.add(new Tool(
                null,
                getAnnotations(true, false),
                "Create a new Pure source file",
                null, null,
                new Tool.Schema(null,
                        mapOf(
                                "path", mapOf(
                                        "type", "string",
                                        "description",
                                        "The file path to create"
                                                + " (e.g. /myRepo/"
                                                + "myFile.pure)"),
                                "content", mapOf(
                                        "type", "string",
                                        "description",
                                        "Optional initial content"
                                                + " for the file")
                        ),
                        Collections.singletonList(
                                "path")),
                "create_file", null,
                "Create File"));

        tools.add(new Tool(
                null,
                getAnnotations(true, false),
                "Delete a Pure source file",
                null, null,
                new Tool.Schema(null,
                        mapOf("path", mapOf(
                                "type", "string",
                                "description",
                                "The file path to delete")),
                        Collections.singletonList(
                                "path")),
                "delete_file", null,
                "Delete File"));
    }

    private static void addExecutionTools(List<Tool> tools)
    {
        tools.add(new Tool(
                null,
                getAnnotations(false, false),
                "Execute a Pure function by"
                        + " fully-qualified name."
                        + " The Pure runtime must be"
                        + " initialized.",
                null, null,
                new Tool.Schema(null,
                        mapOf("function", mapOf(
                                "type", "string",
                                "description",
                                "The fully-qualified"
                                        + " function name to"
                                        + " execute (e.g."
                                        + " go():Any[*])")),
                        Collections.singletonList(
                                "function")),
                "execute_pure", null,
                "Execute Pure"));

        tools.add(new Tool(
                null,
                getAnnotations(false, false),
                "Compile and execute a Pure code"
                        + " block. Modifies the source"
                        + " at the given path, compiles,"
                        + " and runs the go():Any[*]"
                        + " function.",
                null, null,
                new Tool.Schema(null,
                        mapOf(
                                "code", mapOf(
                                        "type", "string",
                                        "description",
                                        "The Pure code to compile"
                                                + " and execute"),
                                "path", mapOf(
                                        "type", "string",
                                        "description",
                                        "The source file path for"
                                                + " the code (defaults"
                                                + " to /ide_mcp_exec"
                                                + ".pure)")
                        ),
                        Collections.singletonList(
                                "code")),
                "execute_go", null, "Execute Go"));

        tools.add(new Tool(
                null,
                getAnnotations(false, false),
                "Run Pure tests matching a"
                        + " path pattern",
                null, null,
                new Tool.Schema(null,
                        mapOf(
                                "path", mapOf(
                                        "type", "string",
                                        "description",
                                        "The test path or package"
                                                + " to run (e.g. meta::"
                                                + "pure::functions"
                                                + "::string)"),
                                "pctAdapter", mapOf(
                                        "type", "string",
                                        "description",
                                        "Optional PCT adapter"
                                                + " to use")
                        ),
                        Collections.singletonList(
                                "path")),
                "run_tests", null, "Run Tests"));
    }

    private static void addNavigationTools(List<Tool> tools)
    {
        tools.add(new Tool(
                null,
                getAnnotations(false, true),
                "Get documentation/concept info"
                        + " for a symbol at a given"
                        + " source location",
                null, null,
                new Tool.Schema(null,
                        buildConceptInfoProps(),
                        Arrays.asList(
                                "file", "line", "column")),
                "get_concept_info", null,
                "Get Concept Info"));

        tools.add(new Tool(
                null,
                getAnnotations(false, true),
                "Get code completion suggestions"
                        + " for a path. Returns matching"
                        + " elements in the given"
                        + " package.",
                null, null,
                new Tool.Schema(null,
                        mapOf("path", mapOf(
                                "type", "string",
                                "description",
                                "The package path to get"
                                        + " suggestions for (e.g."
                                        + " meta::pure::"
                                        + "functions)"
                        )),
                        Collections.singletonList(
                                "path")),
                "get_suggestions", null,
                "Get Suggestions"));
    }

    private static Map<String, Object> buildConceptInfoProps()
    {
        Map<String, Object> props =
                new LinkedHashMap<>();
        props.put("file", mapOf(
                "type", "string",
                "description",
                "The source file path"));
        props.put("line", mapOf(
                "type", "integer",
                "description",
                "The line number (1-based)"));
        props.put("column", mapOf(
                "type", "integer",
                "description",
                "The column number (1-based)"));
        return props;
    }

    private static List<ContentBlock> dispatch(Tool tool, Map<String, Object> params, PureRuntime pureRuntime, MutableRepositoryCodeStorage cs, FunctionExecution fe)
    {
        switch (tool.getName())
        {
            case "read_file":
                return handleReadFile(cs, params);
            case "list_directory":
                return handleListDir(cs, params);
            case "find_in_sources":
                return handleFindInSources(pureRuntime, params);
            case "find_pure_file":
                return handleFindPureFile(pureRuntime, params);
            case "create_file":
                return handleCreateFile(pureRuntime, params);
            case "delete_file":
                return handleDeleteFile(pureRuntime, params);
            case "execute_pure":
                return handleExecutePure(pureRuntime, fe, params);
            case "execute_go":
                return handleExecuteGo(pureRuntime, fe, params);
            case "run_tests":
                return handleRunTests(pureRuntime, fe, params);
            case "get_concept_info":
                return handleGetConceptInfo(pureRuntime, params);
            case "get_suggestions":
                return handleGetSuggestions(pureRuntime, params);
            default:
                return Collections.singletonList(new TextContent(null, null, "Unknown tool: " + tool.getName()));
        }
    }

    private static List<ContentBlock> handleReadFile(MutableRepositoryCodeStorage cs, Map<String, Object> params)
    {
        String path = (String) params.get("path");
        byte[] content = cs.getContentAsBytes(path);
        if (content == null)
        {
            return Collections.singletonList(new TextContent(null, null, "File not found: " + path));
        }
        return Collections.singletonList(new TextContent(null, null, new String(content)));
    }

    private static List<ContentBlock> handleListDir(MutableRepositoryCodeStorage cs, Map<String, Object> params)
    {
        String path = (String) params.get("path");
        RichIterable<CodeStorageNode> nodes = cs.getFiles(path);

        StringBuilder sb = new StringBuilder();
        for (CodeStorageNode node : nodes)
        {
            if (sb.length() > 0)
            {
                sb.append('\n');
            }
            sb.append(node.isDirectory() ? "[DIR]  " : "[FILE] ");
            sb.append(node.getName());
        }
        return Collections.singletonList(new TextContent(null, null, sb.toString()));
    }

    private static List<ContentBlock> handleFindInSources(PureRuntime runtime, Map<String, Object> params)
    {
        String query = (String) params.get("query");
        boolean regex = params.get("regex") != null && (Boolean) params.get("regex");
        boolean caseSensitive = params.get("caseSensitive") == null || (Boolean) params.get("caseSensitive");
        int limit = params.get("limit") != null ? ((Number) params.get("limit")).intValue() : 100;

        RichIterable<SourceCoordinates> results;
        if (regex)
        {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            Pattern pattern = Pattern.compile(query, flags);
            results = runtime.getSourceRegistry().find(pattern, null);
        }
        else
        {
            results = runtime.getSourceRegistry().find(query, caseSensitive, null);
        }

        return formatFindResults(results, limit, query);
    }

    private static List<ContentBlock> formatFindResults(RichIterable<SourceCoordinates> results, int limit, String query)
    {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (SourceCoordinates sc : results)
        {
            if (count >= limit)
            {
                break;
            }
            if (sb.length() > 0)
            {
                sb.append('\n');
            }
            sb.append(sc.getSourceId()).append(':').append(sc.getStartLine()).append(':').append(sc.getStartColumn());
            if (sc.getPreview() != null)
            {
                sb.append(" | ").append(sc.getPreview().getBeforeText()).append(">>>").append(sc.getPreview().getFoundText()).append("<<<").append(sc.getPreview().getAfterText());
            }
            count++;
        }
        if (count == 0)
        {
            return Collections.singletonList(new TextContent(null, null, "No matches found for: " + query));
        }
        return Collections.singletonList(new TextContent(null, null, sb.toString()));
    }

    private static List<ContentBlock> handleFindPureFile(PureRuntime runtime, Map<String, Object> params)
    {
        String file = (String) params.get("file");
        boolean regex = params.get("regex") != null && (Boolean) params.get("regex");

        RichIterable<String> matches;
        if (regex)
        {
            Pattern pattern = Pattern.compile(file);
            matches = runtime.getSourceRegistry().findSourceIds(pattern);
        }
        else
        {
            matches = runtime.getSourceRegistry().findSourceIds(file);
        }

        List<String> sorted = matches.toSortedList();
        if (sorted.isEmpty())
        {
            return Collections.singletonList(new TextContent(null, null, "No files found matching: " + file));
        }
        return Collections.singletonList(new TextContent(null, null, String.join("\n", sorted)));
    }

    private static List<ContentBlock> handleCreateFile(PureRuntime runtime, Map<String, Object> params)
    {
        String path = (String) params.get("path");
        String content = (String) params.get("content");

        runtime.create(path);
        if (content != null && !content.isEmpty())
        {
            runtime.modify(path, content);
        }
        return Collections.singletonList(new TextContent(null, null, "Created file: " + path));
    }

    private static List<ContentBlock> handleDeleteFile(PureRuntime runtime, Map<String, Object> params)
    {
        String path = (String) params.get("path");
        runtime.delete(path);
        return Collections.singletonList(new TextContent(null, null, "Deleted file: " + path));
    }

    private static List<ContentBlock> handleExecutePure(PureRuntime runtime, FunctionExecution fe, Map<String, Object> params)
    {
        String function = (String) params.get("function");
        if (fe == null || !fe.isFullyInitializedForExecution())
        {
            return Collections.singletonList(new TextContent(null, null, "Pure runtime is not initialized. Please wait for compilation to complete."));
        }

        CoreInstance func = runtime.getFunction(function);
        if (func == null)
        {
            return Collections.singletonList(new TextContent(null, null, "Function not found: " + function));
        }

        CoreInstance result = fe.start(func, org.eclipse.collections.impl.factory.Lists.mutable.empty());
        return Collections.singletonList(new TextContent(null, null, result != null ? result.toString() : "null"));
    }

    private static List<ContentBlock> handleExecuteGo(PureRuntime runtime, FunctionExecution fe, Map<String, Object> params)
    {
        String code = (String) params.get("code");
        String path = params.get("path") != null ? (String) params.get("path") : "/ide_mcp_exec.pure";

        if (runtime.getSourceById(path) == null)
        {
            runtime.create(path);
        }
        runtime.modify(path, code);

        try
        {
            org.finos.legend.pure.m3.SourceMutation mutation = runtime.compile();
            List<String> modifiedFiles = org.eclipse.collections.impl.factory.Lists.mutable.withAll(mutation.getModifiedFiles());

            if (fe != null && fe.isFullyInitializedForExecution())
            {
                return executeGoFunc(runtime, fe, modifiedFiles);
            }
            return Collections.singletonList(new TextContent(null, null, "Code compiled successfully."));
        }
        catch (Exception e)
        {
            return Collections.singletonList(new TextContent(null, null, "Compilation/execution error: " + e.getMessage()));
        }
    }

    private static List<ContentBlock> executeGoFunc(PureRuntime runtime, FunctionExecution fe, List<String> modifiedFiles)
    {
        CoreInstance func = runtime.getFunction("go():Any[*]");

        if (func != null)
        {
            CoreInstance result = fe.start(func, org.eclipse.collections.impl.factory.Lists.mutable.empty());
            String resultText = result != null ? result.toString() : "Execution completed (no return value)";
            if (!modifiedFiles.isEmpty())
            {
                resultText += "\nModified files: " + String.join(", ", modifiedFiles);
            }
            return Collections.singletonList(new TextContent(null, null, resultText));
        }
        return Collections.singletonList(new TextContent(null, null, "Compilation succeeded but no go():Any[*] function found to execute."));
    }

    private static List<ContentBlock> handleRunTests(PureRuntime runtime, FunctionExecution fe, Map<String, Object> params)
    {
        String path = (String) params.get("path");

        if (fe == null || !fe.isFullyInitializedForExecution())
        {
            return Collections.singletonList(new TextContent(null, null,"Pure runtime is not initialized."));
        }

        try
        {
            return doRunTests(runtime, fe, path);
        }
        catch (Exception e)
        {
            return Collections.singletonList(new TextContent(null, null, "Error running tests: " + e.getMessage()));
        }
    }

    private static List<ContentBlock> doRunTests(PureRuntime runtime, FunctionExecution fe, String path)
    {
        CoreInstance coreInstance = runtime.getCoreInstance(path);
        if (coreInstance == null)
        {
            return Collections.singletonList(new TextContent(null, null, "Path not found: " + path));
        }

        org.finos.legend.pure.m3.navigation.ProcessorSupport processorSupport = runtime.getProcessorSupport();
        org.finos.legend.pure.m3.execution.test.TestCollection collection = org.finos.legend.pure.m3.execution.test.TestCollection.collectTests(
                                coreInstance,
                                processorSupport,
                                fn -> org.finos.legend.pure.m3.execution.test.TestCollection.collectTestsFromPure(fn, fe),
                                org.eclipse.collections.impl.block.factory.Predicates
                                        .alwaysTrue());
        int totalTests = collection.getTestCount();
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(totalTests).append(" test(s) at path: ").append(path).append('\n');

        if (totalTests > 0)
        {
            appendTestResults(sb, collection, fe);
        }
        return Collections.singletonList(new TextContent(null, null, sb.toString()));
    }

    private static void appendTestResults(StringBuilder sb, org.finos.legend.pure.m3.execution.test.TestCollection collection, FunctionExecution fe)
    {
        int passed = 0;
        int failed = 0;
        List<String> failedNames = new ArrayList<>();

        for (CoreInstance test : collection.getAllTestFunctions())
        {
            try
            {
                fe.start(test, org.eclipse.collections.impl.factory.Lists.mutable.empty());
                passed++;
            }
            catch (Exception e)
            {
                failed++;
                failedNames.add(test.getName() + ": " + e.getMessage());
            }
        }
        sb.append("Passed: ").append(passed).append('\n');
        sb.append("Failed: ").append(failed).append('\n');

        if (!failedNames.isEmpty())
        {
            sb.append("\nFailed tests:\n");
            for (String name : failedNames)
            {
                sb.append("  - ").append(name).append('\n');
            }
        }
    }

    private static List<ContentBlock> handleGetConceptInfo(PureRuntime runtime, Map<String, Object> params)
    {
        String file = (String) params.get("file");
        int line = ((Number) params.get("line")).intValue();
        int column = ((Number) params.get("column")).intValue();

        Source source = runtime.getSourceById(file);
        if (source == null)
        {
            return Collections.singletonList(new TextContent(null, null, "Source not found: " + file));
        }

        CoreInstance element = source.navigate(line, column, runtime.getProcessorSupport());
        if (element == null)
        {
            return Collections.singletonList(new TextContent(null, null, "No element found at " + file + ":" + line + ":" + column));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Element: ").append(element.getName()).append('\n');
        if (element.getSourceInformation() != null)
        {
            sb.append("Defined at: ").append(element.getSourceInformation().getSourceId())
                    .append(':')
                    .append(element.getSourceInformation().getStartLine())
                    .append(':')
                    .append(element.getSourceInformation().getStartColumn())
                    .append('\n');
        }
        String classifier = element.getClassifier() != null ? element.getClassifier().getName() : "unknown";
        sb.append("Classifier: ").append(classifier);

        return Collections.singletonList(new TextContent(null, null, sb.toString()));
    }

    private static List<ContentBlock> handleGetSuggestions(PureRuntime runtime, Map<String, Object> params)
    {
        String path = (String) params.get("path");

        try
        {
            CoreInstance ci = runtime.getCoreInstance(path);
            if (ci == null)
            {
                return Collections.singletonList(new TextContent(null, null, "No element found for path: " + path));
            }

            if (ci instanceof org.finos.legend.pure.m3.coreinstance.Package)
            {
                return formatPackageChildren(ci, path);
            }

            String clsName = ci.getClassifier() != null ? ci.getClassifier().getName() : "unknown";
            return Collections.singletonList(new TextContent(null, null, "Element: " + ci.getName() + " (type: " + clsName + ")"));
        }
        catch (Exception e)
        {
            return Collections.singletonList(new TextContent(null, null, "Error getting suggestions: " + e.getMessage()));
        }
    }

    private static List<ContentBlock> formatPackageChildren(CoreInstance ci, String path)
    {
        org.eclipse.collections.api.list.ListIterable<? extends CoreInstance> children = ci.getValueForMetaPropertyToMany("children");
        if (children == null || children.isEmpty())
        {
            return Collections.singletonList(new TextContent(null, null, "No children found in package: " + path));
        }
        StringBuilder sb = new StringBuilder();
        for (CoreInstance child : children)
        {
            if (sb.length() > 0)
            {
                sb.append('\n');
            }
            sb.append(child.getClassifier() != null ? child.getClassifier().getName() : "?");
            sb.append(' ');
            sb.append(child.getName());
        }
        return Collections.singletonList(new TextContent(null, null, sb.toString()));
    }

    private static Map<String, Object> mapOf(String k1, Object v1)
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(k1, v1);
        return map;
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2)
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }
}
