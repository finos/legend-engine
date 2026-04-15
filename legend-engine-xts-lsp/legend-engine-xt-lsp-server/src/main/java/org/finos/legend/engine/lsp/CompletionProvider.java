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

package org.finos.legend.engine.lsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

/**
 * Provides code completion for Pure source files, matching PureIdeLight's
 * Suggestion.java patterns:
 * - Package path completion (meta::pure:: → children)
 * - Identifier completion (bare names from imports + auto-imports)
 * - Variable completion ($var → let bindings + parameters in scope)
 */
public class CompletionProvider
{
    // Auto-import packages from m3.pure — same list as PureIdeLight
    static final List<String> AUTO_IMPORTS = Collections.unmodifiableList(Arrays.asList(
            "meta::pure::metamodel",
            "meta::pure::metamodel::type",
            "meta::pure::metamodel::type::generics",
            "meta::pure::metamodel::relationship",
            "meta::pure::metamodel::valuespecification",
            "meta::pure::metamodel::multiplicity",
            "meta::pure::metamodel::function",
            "meta::pure::metamodel::function::property",
            "meta::pure::metamodel::extension",
            "meta::pure::metamodel::import",
            "meta::pure::functions::date",
            "meta::pure::functions::string",
            "meta::pure::functions::collection",
            "meta::pure::functions::meta",
            "meta::pure::functions::constraints",
            "meta::pure::functions::lang",
            "meta::pure::functions::boolean",
            "meta::pure::functions::tools",
            "meta::pure::functions::io",
            "meta::pure::functions::math",
            "meta::pure::functions::asserts",
            "meta::pure::functions::test",
            "meta::pure::functions::multiplicity",
            "meta::pure::router",
            "meta::pure::service",
            "meta::pure::tds",
            "meta::pure::tools",
            "meta::pure::profiles"
    ));

    private static final int MAX_COMPLETIONS = 200;
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+(\\S+)");
    private static final Pattern PATH_PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*(?:::[a-zA-Z_][a-zA-Z0-9_]*)*)::([a-zA-Z_][a-zA-Z0-9_]*)?$");

    /**
     * Get completions for the given position in a source file.
     */
    public static List<CompletionItem> getCompletions(PureRuntime runtime, String sourceId,
                                                       String fileContent, int line, int column)
    {
        if (fileContent == null || line < 1)
        {
            return Collections.emptyList();
        }

        String[] lines = fileContent.split("\n", -1);
        if (line > lines.length)
        {
            return Collections.emptyList();
        }

        String currentLine = lines[line - 1];
        String textBeforeCursor = (column <= currentLine.length())
                ? currentLine.substring(0, column)
                : currentLine;

        ProcessorSupport processorSupport = runtime.getProcessorSupport();

        // 1. Variable completion: $...
        if (textBeforeCursor.matches(".*\\$[a-zA-Z_0-9]*$"))
        {
            return getVariableCompletions(runtime, sourceId, line, column);
        }

        // 2. Package path completion: meta::pure::... or any path with ::
        Matcher pathMatcher = PATH_PATTERN.matcher(textBeforeCursor);
        if (pathMatcher.find())
        {
            String packagePath = pathMatcher.group(1);
            String prefix = pathMatcher.group(2);
            return getPackagePathCompletions(runtime, processorSupport, packagePath, prefix);
        }

        // 3. Identifier completion: bare name, search imports + auto-imports
        String prefix = extractIdentifierPrefix(textBeforeCursor);
        if (prefix != null && !prefix.isEmpty())
        {
            List<String> importPaths = extractImports(lines, line);
            return getIdentifierCompletions(runtime, processorSupport, importPaths, prefix);
        }

        return Collections.emptyList();
    }

    /**
     * Package path completion — given "meta::pure::" complete with children of that package.
     */
    private static List<CompletionItem> getPackagePathCompletions(PureRuntime runtime,
                                                                    ProcessorSupport processorSupport,
                                                                    String packagePath, String prefix)
    {
        CoreInstance pkg = runtime.getCoreInstance(packagePath);
        if (!(pkg instanceof Package))
        {
            return Collections.emptyList();
        }

        String lowerPrefix = (prefix != null) ? prefix.toLowerCase() : "";
        ListIterable<? extends CoreInstance> children =
                pkg.getValueForMetaPropertyToMany(M3Properties.children);
        if (children == null)
        {
            return Collections.emptyList();
        }

        List<CompletionItem> items = new ArrayList<>();
        for (CoreInstance child : children)
        {
            if (items.size() >= MAX_COMPLETIONS)
            {
                break;
            }
            String name = getElementName(child);
            if (name == null || name.startsWith("@"))
            {
                continue;
            }
            if (!lowerPrefix.isEmpty() && !name.toLowerCase().startsWith(lowerPrefix))
            {
                continue;
            }

            items.add(toCompletionItem(child, name, processorSupport));
        }
        return items;
    }

    /**
     * Identifier completion — search imports + root + auto-imports for matching elements.
     */
    private static List<CompletionItem> getIdentifierCompletions(PureRuntime runtime,
                                                                   ProcessorSupport processorSupport,
                                                                   List<String> importPaths,
                                                                   String prefix)
    {
        String lowerPrefix = prefix.toLowerCase();

        // Collect unique package paths: file imports → root → auto-imports
        Set<String> seen = new HashSet<>();
        List<String> allPaths = new ArrayList<>();
        for (String p : importPaths)
        {
            if (seen.add(p))
            {
                allPaths.add(p);
            }
        }
        if (seen.add("::"))
        {
            allPaths.add("::");
        }
        for (String p : AUTO_IMPORTS)
        {
            if (seen.add(p))
            {
                allPaths.add(p);
            }
        }

        Set<String> addedIds = new HashSet<>();
        List<CompletionItem> items = new ArrayList<>();

        for (String pkgPath : allPaths)
        {
            if (items.size() >= MAX_COMPLETIONS)
            {
                break;
            }
            CoreInstance pkg = runtime.getCoreInstance(pkgPath);
            if (!(pkg instanceof Package))
            {
                continue;
            }
            ListIterable<? extends CoreInstance> children =
                    pkg.getValueForMetaPropertyToMany(M3Properties.children);
            if (children == null)
            {
                continue;
            }
            for (CoreInstance child : children)
            {
                if (items.size() >= MAX_COMPLETIONS)
                {
                    break;
                }
                if (child instanceof Package)
                {
                    continue;
                }
                String name = getElementName(child);
                if (name == null || name.startsWith("@"))
                {
                    continue;
                }
                if (!name.toLowerCase().startsWith(lowerPrefix))
                {
                    continue;
                }
                // Deduplicate by qualified path
                String qPath = PackageableElement.getUserPathForPackageableElement(child);
                if (!addedIds.add(qPath))
                {
                    continue;
                }
                items.add(toCompletionItem(child, name, processorSupport));
            }
        }
        return items;
    }

    /**
     * Variable completion — find let bindings and function parameters in scope.
     * Same approach as PureIdeLight's suggestion/variable endpoint.
     */
    private static List<CompletionItem> getVariableCompletions(PureRuntime runtime,
                                                                 String sourceId, int line, int column)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            return Collections.emptyList();
        }

        try
        {
            ListIterable<CoreInstance> functionsOrLambdas = source.findFunctionsOrLambasAt(line, column);
            Set<String> varNames = new HashSet<>();

            for (CoreInstance fn : functionsOrLambdas)
            {
                // Let expressions before the cursor
                RichIterable<? extends CoreInstance> exprs =
                        fn.getValueForMetaPropertyToMany(M3Properties.expressionSequence);
                if (exprs != null)
                {
                    for (CoreInstance expr : exprs)
                    {
                        String classifierName = expr.getClassifier() != null
                                ? expr.getClassifier().getName() : "";
                        if ("SimpleFunctionExpression".equals(classifierName))
                        {
                            CoreInstance fnNameCI = expr.getValueForMetaPropertyToOne(M3Properties.functionName);
                            if (fnNameCI != null && "letFunction".equals(fnNameCI.getName()))
                            {
                                ListIterable<? extends CoreInstance> params =
                                        expr.getValueForMetaPropertyToMany(M3Properties.parametersValues);
                                if (params != null && params.notEmpty())
                                {
                                    CoreInstance letVar = params.get(0);
                                    SourceInformation si = letVar.getSourceInformation();
                                    if (si != null && (si.getEndLine() < line
                                            || (si.getEndLine() == line && si.getEndColumn() < column)))
                                    {
                                        CoreInstance valCI = letVar.getValueForMetaPropertyToOne(M3Properties.values);
                                        if (valCI != null)
                                        {
                                            varNames.add(valCI.getName());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Function parameters
                try
                {
                    CoreInstance classifierGT = fn.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
                    if (classifierGT != null)
                    {
                        CoreInstance typeArgs = classifierGT.getValueForMetaPropertyToOne(M3Properties.typeArguments);
                        if (typeArgs != null)
                        {
                            CoreInstance rawType = typeArgs.getValueForMetaPropertyToOne(M3Properties.rawType);
                            if (rawType != null)
                            {
                                ListIterable<? extends CoreInstance> fnParams =
                                        rawType.getValueForMetaPropertyToMany(M3Properties.parameters);
                                if (fnParams != null)
                                {
                                    for (CoreInstance param : fnParams)
                                    {
                                        CoreInstance nameCI = param.getValueForMetaPropertyToOne(M3Properties.name);
                                        if (nameCI != null)
                                        {
                                            varNames.add(nameCI.getName());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Exception ignored)
                {
                    // Skip if type info unavailable
                }
            }

            List<CompletionItem> items = new ArrayList<>();
            for (String name : varNames)
            {
                CompletionItem item = new CompletionItem();
                item.setLabel("$" + name);
                item.setInsertText(name);
                item.setKind(CompletionItemKind.Variable);
                item.setSortText("0_" + name);
                items.add(item);
            }
            return items;
        }
        catch (Exception e)
        {
            LspLog.debug("Variable completion failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static CompletionItem toCompletionItem(CoreInstance element, String name,
                                                     ProcessorSupport processorSupport)
    {
        String classifierName = element.getClassifier() != null
                ? element.getClassifier().getName() : "";

        CompletionItem item = new CompletionItem();
        item.setKind(toCompletionKind(classifierName));

        if ("ConcreteFunctionDefinition".equals(classifierName)
                || "NativeFunction".equals(classifierName))
        {
            // For functions, show pretty signature and use simple name as insert
            try
            {
                item.setLabel(Function.prettyPrint(element, processorSupport));
            }
            catch (Exception e)
            {
                item.setLabel(name);
            }
            CoreInstance fnNameCI = element.getValueForMetaPropertyToOne(M3Properties.functionName);
            String simpleName = (fnNameCI != null) ? fnNameCI.getName() : name;
            item.setInsertText(simpleName);
            item.setFilterText(simpleName);
            item.setSortText("2_" + simpleName);
        }
        else if (element instanceof Package)
        {
            item.setLabel(name);
            item.setInsertText(name + "::");
            item.setSortText("0_" + name);
        }
        else
        {
            item.setLabel(name);
            item.setInsertText(name);
            item.setSortText("1_" + name);
        }

        String qualifiedPath = PackageableElement.getUserPathForPackageableElement(element);
        item.setDetail(qualifiedPath);

        return item;
    }

    private static CompletionItemKind toCompletionKind(String classifierName)
    {
        if (classifierName == null)
        {
            return CompletionItemKind.Text;
        }
        switch (classifierName)
        {
            case "Package":
                return CompletionItemKind.Module;
            case "Class":
                return CompletionItemKind.Class;
            case "Enumeration":
                return CompletionItemKind.Enum;
            case "ConcreteFunctionDefinition":
            case "NativeFunction":
                return CompletionItemKind.Function;
            case "Profile":
                return CompletionItemKind.Interface;
            case "Association":
                return CompletionItemKind.Struct;
            case "Measure":
            case "Unit":
                return CompletionItemKind.Unit;
            default:
                return CompletionItemKind.Text;
        }
    }

    /**
     * Get the display name for an element — simple name for non-functions,
     * functionName for functions.
     */
    private static String getElementName(CoreInstance element)
    {
        String classifierName = element.getClassifier() != null
                ? element.getClassifier().getName() : "";
        if ("ConcreteFunctionDefinition".equals(classifierName)
                || "NativeFunction".equals(classifierName))
        {
            CoreInstance fnName = element.getValueForMetaPropertyToOne(M3Properties.functionName);
            if (fnName != null)
            {
                return fnName.getName();
            }
        }
        return element.getName();
    }

    /**
     * Extract the identifier being typed at the cursor position.
     * Returns null if cursor is not on an identifier.
     */
    static String extractIdentifierPrefix(String textBeforeCursor)
    {
        // Walk backwards from end to find the start of the current identifier
        int end = textBeforeCursor.length();
        int start = end;
        while (start > 0)
        {
            char c = textBeforeCursor.charAt(start - 1);
            if (Character.isLetterOrDigit(c) || c == '_')
            {
                start--;
            }
            else
            {
                break;
            }
        }
        if (start == end)
        {
            return null;
        }
        // Don't trigger identifier completion if preceded by :: (that's path completion)
        if (start >= 2 && textBeforeCursor.charAt(start - 1) == ':' && textBeforeCursor.charAt(start - 2) == ':')
        {
            return null;
        }
        return textBeforeCursor.substring(start, end);
    }

    /**
     * Extract import paths from the file's import statements.
     */
    static List<String> extractImports(String[] lines, int currentLine)
    {
        List<String> imports = new ArrayList<>();
        for (int i = 0; i < lines.length && i < currentLine; i++)
        {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("import "))
            {
                String path = trimmed.substring("import ".length())
                        .replaceAll("[;\\s]", "")
                        .replace("::*", "");
                if (!path.isEmpty())
                {
                    imports.add(path);
                }
            }
        }
        return imports;
    }
}
