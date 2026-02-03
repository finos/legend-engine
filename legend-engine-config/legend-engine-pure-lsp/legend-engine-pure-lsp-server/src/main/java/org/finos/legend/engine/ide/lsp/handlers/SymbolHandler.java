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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.finos.legend.engine.ide.lsp.session.LSPSession;
import org.finos.legend.engine.ide.lsp.utils.PositionUtils;
import org.finos.legend.engine.ide.lsp.utils.URIUtils;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles document symbol and workspace symbol requests.
 */
public class SymbolHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SymbolHandler.class);

    private final LSPSession session;

    public SymbolHandler(LSPSession session)
    {
        this.session = session;
    }

    /**
     * Handle textDocument/documentSymbol request.
     */
    public List<Either<SymbolInformation, DocumentSymbol>> getDocumentSymbols(DocumentSymbolParams params)
    {
        String uri = params.getTextDocument().getUri();

        LOGGER.debug("Document symbols request for {}", uri);

        String sourceId = URIUtils.uriToSourceId(uri);
        MutableList<Either<SymbolInformation, DocumentSymbol>> symbols = Lists.mutable.empty();

        try
        {
            Source source = session.getSource(sourceId);
            if (source == null)
            {
                LOGGER.debug("Source not found: {}", sourceId);
                return symbols;
            }

            // Get all defined elements from the source
            // Note: This requires the source to be compiled
            if (!source.isCompiled())
            {
                session.compile();
            }

            // Get the source's defined elements
            for (CoreInstance element : source.getNewInstances())
            {
                DocumentSymbol symbol = createDocumentSymbol(element);
                if (symbol != null)
                {
                    symbols.add(Either.forRight(symbol));
                }
            }

            LOGGER.debug("Found {} symbols", symbols.size());
        }
        catch (Exception e)
        {
            LOGGER.error("Error getting document symbols", e);
        }

        return symbols;
    }

    private DocumentSymbol createDocumentSymbol(CoreInstance element)
    {
        if (element == null)
        {
            return null;
        }

        SourceInformation sourceInfo = element.getSourceInformation();
        if (sourceInfo == null)
        {
            return null;
        }

        String name = getElementName(element);
        if (name == null || name.isEmpty())
        {
            return null;
        }

        SymbolKind kind = getSymbolKind(element);
        Range range = PositionUtils.sourceInfoToRange(sourceInfo);
        Range selectionRange = PositionUtils.purePositionToRange(sourceInfo.getLine(), sourceInfo.getColumn());

        DocumentSymbol symbol = new DocumentSymbol();
        symbol.setName(name);
        symbol.setKind(kind);
        symbol.setRange(range);
        symbol.setSelectionRange(selectionRange);
        symbol.setDetail(getElementDetail(element));

        // Add children for nested elements (e.g., class properties)
        MutableList<DocumentSymbol> children = getChildSymbols(element);
        if (children.notEmpty())
        {
            symbol.setChildren(children);
        }

        return symbol;
    }

    private String getElementName(CoreInstance element)
    {
        try
        {
            // Try to get name from common properties
            CoreInstance nameProperty = element.getValueForMetaPropertyToOne("name");
            if (nameProperty != null)
            {
                return nameProperty.getName();
            }

            CoreInstance functionName = element.getValueForMetaPropertyToOne("functionName");
            if (functionName != null)
            {
                return functionName.getName();
            }

            return element.getName();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String getElementDetail(CoreInstance element)
    {
        if (element.getClassifier() != null)
        {
            return element.getClassifier().getName();
        }
        return null;
    }

    private SymbolKind getSymbolKind(CoreInstance element)
    {
        if (element.getClassifier() == null)
        {
            return SymbolKind.Object;
        }

        String classifierName = element.getClassifier().getName();
        switch (classifierName)
        {
            case "Class":
                return SymbolKind.Class;
            case "Enumeration":
                return SymbolKind.Enum;
            case "ConcreteFunctionDefinition":
            case "NativeFunction":
                return SymbolKind.Function;
            case "Property":
            case "QualifiedProperty":
                return SymbolKind.Property;
            case "Package":
                return SymbolKind.Package;
            case "Profile":
                return SymbolKind.Interface;
            case "Association":
                return SymbolKind.Struct;
            case "Enum":
                return SymbolKind.EnumMember;
            case "Mapping":
            case "Store":
            case "Database":
                return SymbolKind.Module;
            default:
                return SymbolKind.Object;
        }
    }

    private MutableList<DocumentSymbol> getChildSymbols(CoreInstance element)
    {
        MutableList<DocumentSymbol> children = Lists.mutable.empty();

        try
        {
            // For classes, add properties as children
            if ("Class".equals(element.getClassifier().getName()))
            {
                for (CoreInstance prop : element.getValueForMetaPropertyToMany("properties"))
                {
                    DocumentSymbol propSymbol = createDocumentSymbol(prop);
                    if (propSymbol != null)
                    {
                        children.add(propSymbol);
                    }
                }
            }
            // For enumerations, add enum values as children
            else if ("Enumeration".equals(element.getClassifier().getName()))
            {
                for (CoreInstance val : element.getValueForMetaPropertyToMany("values"))
                {
                    DocumentSymbol valSymbol = createDocumentSymbol(val);
                    if (valSymbol != null)
                    {
                        valSymbol.setKind(SymbolKind.EnumMember);
                        children.add(valSymbol);
                    }
                }
            }
        }
        catch (Exception e)
        {
            // Element doesn't support children
        }

        return children;
    }
}
