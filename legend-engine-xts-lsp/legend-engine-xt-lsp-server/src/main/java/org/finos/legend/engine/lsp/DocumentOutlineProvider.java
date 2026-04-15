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
import java.util.Collections;
import java.util.List;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolKind;
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
 * Provides document outline (Ctrl+Shift+O) for Pure source files.
 * Uses the same display approach as PureIdeLight: Function.prettyPrint()
 * for human-readable function signatures, M3Properties.functionName for
 * simple function names, and instance.getName() for non-function elements.
 */
public class DocumentOutlineProvider
{
    public static List<DocumentSymbol> getOutline(PureRuntime runtime, String sourceId)
    {
        Source source = runtime.getSourceById(sourceId);
        if (source == null)
        {
            LspLog.debug("documentSymbol: source not found for ID: " + sourceId);
            return Collections.emptyList();
        }

        ListIterable<? extends CoreInstance> instances = source.getNewInstances();
        if (instances == null || instances.isEmpty())
        {
            return Collections.emptyList();
        }

        ProcessorSupport processorSupport = runtime.getProcessorSupport();
        List<DocumentSymbol> symbols = new ArrayList<>();

        for (CoreInstance instance : instances)
        {
            if (instance instanceof Package)
            {
                continue;
            }

            SourceInformation si = instance.getSourceInformation();
            if (si == null)
            {
                continue;
            }

            String classifierName = instance.getClassifier().getName();
            String qualifiedPath = PackageableElement.getUserPathForPackageableElement(instance);
            String displayName = getDisplayName(instance, processorSupport);

            SymbolKind kind = WorkspaceSymbolProvider.toSymbolKind(classifierName);

            DocumentSymbol symbol = new DocumentSymbol();
            symbol.setName(displayName);
            symbol.setDetail(qualifiedPath);
            symbol.setKind(kind);
            symbol.setRange(toRange(si));
            symbol.setSelectionRange(toSelectionRange(si));

            List<DocumentSymbol> children = getChildren(instance, classifierName);
            if (!children.isEmpty())
            {
                symbol.setChildren(children);
            }

            symbols.add(symbol);
        }

        return symbols;
    }

    /**
     * Get a human-readable display name for an element.
     * For functions, uses Function.prettyPrint() (same as PureIdeLight).
     * For other elements, uses the simple name.
     */
    static String getDisplayName(CoreInstance instance, ProcessorSupport processorSupport)
    {
        String classifierName = instance.getClassifier() != null
                ? instance.getClassifier().getName() : "";
        if (isFunction(classifierName))
        {
            try
            {
                return Function.prettyPrint(instance, processorSupport);
            }
            catch (Exception e)
            {
                CoreInstance fnName = instance.getValueForMetaPropertyToOne(M3Properties.functionName);
                if (fnName != null)
                {
                    return fnName.getName();
                }
            }
        }
        String name = instance.getName();
        return name != null ? name : "?";
    }

    /**
     * Get just the simple function name (without signature) for search matching.
     * Uses M3Properties.functionName — same as PureIdeLight's Suggestion.java.
     */
    static String getSimpleFunctionName(CoreInstance instance)
    {
        String classifierName = instance.getClassifier() != null
                ? instance.getClassifier().getName() : "";
        if (isFunction(classifierName))
        {
            CoreInstance fnName = instance.getValueForMetaPropertyToOne(M3Properties.functionName);
            if (fnName != null)
            {
                return fnName.getName();
            }
        }
        return instance.getName();
    }

    private static boolean isFunction(String classifierName)
    {
        return "ConcreteFunctionDefinition".equals(classifierName)
                || "NativeFunction".equals(classifierName);
    }

    private static List<DocumentSymbol> getChildren(CoreInstance instance, String classifierName)
    {
        switch (classifierName)
        {
            case "Class":
                return getClassChildren(instance);
            case "Enumeration":
                return getEnumChildren(instance);
            case "Association":
                return getAssociationChildren(instance);
            default:
                return Collections.emptyList();
        }
    }

    private static List<DocumentSymbol> getClassChildren(CoreInstance classInstance)
    {
        List<DocumentSymbol> children = new ArrayList<>();

        ListIterable<? extends CoreInstance> properties =
                classInstance.getValueForMetaPropertyToMany(M3Properties.properties);
        if (properties != null)
        {
            for (CoreInstance prop : properties)
            {
                addPropertySymbol(children, prop, SymbolKind.Property);
            }
        }

        ListIterable<? extends CoreInstance> qualifiedProperties =
                classInstance.getValueForMetaPropertyToMany(M3Properties.qualifiedProperties);
        if (qualifiedProperties != null)
        {
            for (CoreInstance prop : qualifiedProperties)
            {
                addPropertySymbol(children, prop, SymbolKind.Method);
            }
        }

        return children;
    }

    private static List<DocumentSymbol> getEnumChildren(CoreInstance enumInstance)
    {
        List<DocumentSymbol> children = new ArrayList<>();

        ListIterable<? extends CoreInstance> values =
                enumInstance.getValueForMetaPropertyToMany(M3Properties.values);
        if (values != null)
        {
            for (CoreInstance value : values)
            {
                SourceInformation si = value.getSourceInformation();
                if (si == null)
                {
                    continue;
                }
                String name = value.getName();
                if (name == null)
                {
                    continue;
                }
                DocumentSymbol symbol = new DocumentSymbol();
                symbol.setName(name);
                symbol.setKind(SymbolKind.EnumMember);
                symbol.setRange(toRange(si));
                symbol.setSelectionRange(toSelectionRange(si));
                children.add(symbol);
            }
        }

        return children;
    }

    private static List<DocumentSymbol> getAssociationChildren(CoreInstance assocInstance)
    {
        List<DocumentSymbol> children = new ArrayList<>();

        ListIterable<? extends CoreInstance> properties =
                assocInstance.getValueForMetaPropertyToMany(M3Properties.properties);
        if (properties != null)
        {
            for (CoreInstance prop : properties)
            {
                addPropertySymbol(children, prop, SymbolKind.Property);
            }
        }

        return children;
    }

    private static void addPropertySymbol(List<DocumentSymbol> children, CoreInstance prop, SymbolKind kind)
    {
        SourceInformation si = prop.getSourceInformation();
        if (si == null)
        {
            return;
        }
        String name = prop.getName();
        if (name == null)
        {
            return;
        }
        DocumentSymbol symbol = new DocumentSymbol();
        symbol.setName(name);
        symbol.setKind(kind);
        symbol.setRange(toRange(si));
        symbol.setSelectionRange(toSelectionRange(si));
        children.add(symbol);
    }

    private static Range toRange(SourceInformation si)
    {
        return new Range(
                new Position(Math.max(0, si.getStartLine() - 1), Math.max(0, si.getStartColumn() - 1)),
                new Position(Math.max(0, si.getEndLine() - 1), Math.max(0, si.getEndColumn()))
        );
    }

    private static Range toSelectionRange(SourceInformation si)
    {
        int line = si.getLine();
        int col = si.getColumn();
        if (line > 0 && col > 0)
        {
            return new Range(
                    new Position(line - 1, col - 1),
                    new Position(line - 1, col - 1)
            );
        }
        return new Range(
                new Position(Math.max(0, si.getStartLine() - 1), Math.max(0, si.getStartColumn() - 1)),
                new Position(Math.max(0, si.getStartLine() - 1), Math.max(0, si.getStartColumn() - 1))
        );
    }
}
