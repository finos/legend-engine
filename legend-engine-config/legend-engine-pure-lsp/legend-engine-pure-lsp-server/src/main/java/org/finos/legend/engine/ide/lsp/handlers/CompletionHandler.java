// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.ide.lsp.handlers;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.finos.legend.engine.ide.lsp.converters.CompletionItemConverter;
import org.finos.legend.engine.ide.lsp.session.LSPSession;
import org.finos.legend.engine.ide.lsp.utils.PositionUtils;
import org.finos.legend.engine.ide.lsp.utils.URIUtils;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Handles code completion requests.
 */
public class CompletionHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CompletionHandler.class);

    // Auto-import packages from m3.pure
    private static final List<String> AUTO_IMPORTS = Arrays.asList(
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
    );

    private final LSPSession session;

    public CompletionHandler(LSPSession session)
    {
        this.session = session;
    }

    /**
     * Handle textDocument/completion request.
     */
    public Either<List<CompletionItem>, CompletionList> getCompletion(CompletionParams params)
    {
        String uri = params.getTextDocument().getUri();
        Position position = params.getPosition();

        LOGGER.debug("Completion request for {} at line {}, column {}",
            uri, position.getLine(), position.getCharacter());

        String sourceId = URIUtils.uriToSourceId(uri);
        String content = session.getDocumentManager().getDocumentContent(uri);

        if (content == null)
        {
            return Either.forLeft(Lists.mutable.empty());
        }

        MutableList<CompletionItem> items = Lists.mutable.empty();

        try
        {
            // Get context at cursor position
            CompletionContext context = getCompletionContext(content, position);

            switch (context.type)
            {
                case PATH:
                    items.addAll(getPathCompletions(context.prefix));
                    break;
                case IDENTIFIER:
                    items.addAll(getIdentifierCompletions(sourceId, context.prefix));
                    break;
                case VARIABLE:
                    items.addAll(getVariableCompletions(sourceId, position));
                    break;
                default:
                    // Provide general completions
                    items.addAll(getIdentifierCompletions(sourceId, context.prefix));
                    items.addAll(getVariableCompletions(sourceId, position));
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error getting completions", e);
        }

        return Either.forLeft(items);
    }

    /**
     * Get completions for a package path.
     */
    private MutableList<CompletionItem> getPathCompletions(String path)
    {
        MutableList<CompletionItem> items = Lists.mutable.empty();

        try
        {
            CoreInstance instance = session.getCoreInstance(path);
            if (instance instanceof Package)
            {
                ListIterable<? extends CoreInstance> children = instance.getValueForMetaPropertyToMany(M3Properties.children);
                for (CoreInstance child : children)
                {
                    String pureName = getPureName(child);
                    String pureId = PackageableElement.getUserPathForPackageableElement(child);
                    String pureType = child.getClassifier() != null ? child.getClassifier().getName() : null;

                    items.add(CompletionItemConverter.createPathCompletion(pureName, pureId, pureType));
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.debug("Error getting path completions for: {}", path, e);
        }

        return items;
    }

    /**
     * Get completions for identifiers based on imported packages.
     */
    private MutableList<CompletionItem> getIdentifierCompletions(String sourceId, String prefix)
    {
        MutableList<CompletionItem> items = Lists.mutable.empty();

        try
        {
            PureRuntime runtime = session.getPureRuntime();

            // Build list of packages to search: imports + root + auto-imports
            MutableList<String> packagePaths = Lists.mutable.of("::").withAll(AUTO_IMPORTS).distinct();

            MutableList<Package> packages = Lists.mutable.empty();
            for (String path : packagePaths)
            {
                CoreInstance instance = runtime.getCoreInstance(path);
                if (instance instanceof Package)
                {
                    packages.add((Package) instance);
                }
            }

            MutableSet<String> addedIds = Sets.mutable.empty();

            for (Package pkg : packages)
            {
                ListIterable<? extends CoreInstance> children = pkg.getValueForMetaPropertyToMany(M3Properties.children)
                    .select(child -> !(child instanceof Package));

                for (CoreInstance child : children)
                {
                    String pureName = getPureName(child);

                    // Filter by prefix if provided
                    if (prefix != null && !prefix.isEmpty() && !pureName.toLowerCase().startsWith(prefix.toLowerCase()))
                    {
                        continue;
                    }

                    String pureId = PackageableElement.getUserPathForPackageableElement(child);
                    if (addedIds.contains(pureId))
                    {
                        continue;
                    }
                    addedIds.add(pureId);

                    String pureType = child.getClassifier() != null ? child.getClassifier().getName() : null;
                    String text = child instanceof PackageableFunction
                        ? Function.prettyPrint(child, session.getProcessorSupport())
                        : pureName;

                    items.add(CompletionItemConverter.createIdentifierCompletion(pureName, pureId, pureType, text));
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.debug("Error getting identifier completions", e);
        }

        return items;
    }

    /**
     * Get completions for variables in scope.
     */
    private MutableList<CompletionItem> getVariableCompletions(String sourceId, Position position)
    {
        MutableList<CompletionItem> items = Lists.mutable.empty();

        try
        {
            Source source = session.getSource(sourceId);
            if (source == null)
            {
                return items;
            }

            int pureLine = PositionUtils.lspPositionToPureLine(position);
            int pureColumn = PositionUtils.lspPositionToPureColumn(position);

            ListIterable<CoreInstance> functionsOrLambdas = source.findFunctionsOrLambasAt(pureLine, pureColumn);
            MutableSet<String> varNames = Sets.mutable.empty();

            for (CoreInstance fn : functionsOrLambdas)
            {
                // Get let expressions
                RichIterable<InstanceValue> letVars = fn.getValueForMetaPropertyToMany(M3Properties.expressionSequence)
                    .select(expression -> expression instanceof SimpleFunctionExpression &&
                        "letFunction".equals(((SimpleFunctionExpression) expression)._functionName()))
                    .collect(expression -> ((SimpleFunctionExpression) expression)._parametersValues().toList().getFirst())
                    .select(letVar -> letVar.getSourceInformation().getEndLine() < pureLine ||
                        (letVar.getSourceInformation().getEndLine() == pureLine &&
                            letVar.getSourceInformation().getEndColumn() < pureColumn))
                    .selectInstancesOf(InstanceValue.class);

                for (InstanceValue var : letVars)
                {
                    varNames.add(var.getValueForMetaPropertyToOne(M3Properties.values).getName());
                }

                // Get function parameters
                RichIterable<VariableExpression> params = fn.getValueForMetaPropertyToOne(M3Properties.classifierGenericType)
                    .getValueForMetaPropertyToOne(M3Properties.typeArguments)
                    .getValueForMetaPropertyToOne(M3Properties.rawType)
                    .getValueForMetaPropertyToMany(M3Properties.parameters)
                    .selectInstancesOf(VariableExpression.class);

                for (VariableExpression var : params)
                {
                    varNames.add(var._name());
                }
            }

            for (String varName : varNames)
            {
                items.add(CompletionItemConverter.createVariableCompletion(varName));
            }
        }
        catch (Exception e)
        {
            LOGGER.debug("Error getting variable completions", e);
        }

        return items;
    }

    private String getPureName(CoreInstance element)
    {
        if (element instanceof PackageableFunction)
        {
            return element.getValueForMetaPropertyToOne(M3Properties.functionName).getName();
        }
        return element.getValueForMetaPropertyToOne(M3Properties.name).getName();
    }

    private CompletionContext getCompletionContext(String content, Position position)
    {
        // Get the line content up to the cursor
        String[] lines = content.split("\n", -1);
        if (position.getLine() >= lines.length)
        {
            return new CompletionContext(CompletionType.UNKNOWN, "");
        }

        String line = lines[position.getLine()];
        String beforeCursor = line.substring(0, Math.min(position.getCharacter(), line.length()));

        // Check for variable reference ($)
        int dollarIndex = beforeCursor.lastIndexOf('$');
        if (dollarIndex >= 0)
        {
            String afterDollar = beforeCursor.substring(dollarIndex + 1);
            if (afterDollar.matches("[a-zA-Z0-9_]*"))
            {
                return new CompletionContext(CompletionType.VARIABLE, afterDollar);
            }
        }

        // Check for path completion (::)
        int pathSep = beforeCursor.lastIndexOf("::");
        if (pathSep >= 0)
        {
            // Find the start of the path
            int start = pathSep;
            while (start > 0 && (Character.isLetterOrDigit(beforeCursor.charAt(start - 1)) ||
                beforeCursor.charAt(start - 1) == ':' || beforeCursor.charAt(start - 1) == '_'))
            {
                start--;
            }
            String path = beforeCursor.substring(start, pathSep + 2).trim();
            // Remove trailing :: for lookup
            if (path.endsWith("::"))
            {
                path = path.substring(0, path.length() - 2);
            }
            return new CompletionContext(CompletionType.PATH, path.isEmpty() ? "::" : path);
        }

        // Get identifier prefix
        int identStart = beforeCursor.length();
        while (identStart > 0 && (Character.isLetterOrDigit(beforeCursor.charAt(identStart - 1)) ||
            beforeCursor.charAt(identStart - 1) == '_'))
        {
            identStart--;
        }
        String prefix = beforeCursor.substring(identStart);

        return new CompletionContext(CompletionType.IDENTIFIER, prefix);
    }

    private enum CompletionType
    {
        PATH,
        IDENTIFIER,
        VARIABLE,
        UNKNOWN
    }

    private static class CompletionContext
    {
        final CompletionType type;
        final String prefix;

        CompletionContext(CompletionType type, String prefix)
        {
            this.type = type;
            this.prefix = prefix;
        }
    }
}
