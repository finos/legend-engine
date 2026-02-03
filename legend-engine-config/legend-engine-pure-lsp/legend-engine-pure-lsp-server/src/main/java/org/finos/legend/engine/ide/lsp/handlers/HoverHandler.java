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

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.finos.legend.engine.ide.lsp.session.LSPSession;
import org.finos.legend.engine.ide.lsp.utils.PositionUtils;
import org.finos.legend.engine.ide.lsp.utils.URIUtils;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles hover requests to show element information.
 */
public class HoverHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HoverHandler.class);

    private final LSPSession session;

    public HoverHandler(LSPSession session)
    {
        this.session = session;
    }

    /**
     * Handle textDocument/hover request.
     */
    public Hover getHover(HoverParams params)
    {
        String uri = params.getTextDocument().getUri();
        Position position = params.getPosition();

        LOGGER.debug("Hover request for {} at line {}, column {}",
            uri, position.getLine(), position.getCharacter());

        String sourceId = URIUtils.uriToSourceId(uri);
        int pureLine = PositionUtils.lspPositionToPureLine(position);
        int pureColumn = PositionUtils.lspPositionToPureColumn(position);

        try
        {
            CoreInstance found = session.navigate(sourceId, pureLine, pureColumn);

            if (found == null)
            {
                return null;
            }

            String hoverContent = buildHoverContent(found);
            if (hoverContent == null || hoverContent.isEmpty())
            {
                return null;
            }

            MarkupContent content = new MarkupContent();
            content.setKind(MarkupKind.MARKDOWN);
            content.setValue(hoverContent);

            Hover hover = new Hover();
            hover.setContents(content);

            // Optionally set range from source information
            SourceInformation sourceInfo = found.getSourceInformation();
            if (sourceInfo != null)
            {
                hover.setRange(PositionUtils.sourceInfoToRange(sourceInfo));
            }

            return hover;
        }
        catch (Exception e)
        {
            LOGGER.error("Error building hover", e);
            return null;
        }
    }

    private String buildHoverContent(CoreInstance element)
    {
        StringBuilder sb = new StringBuilder();

        String classifierName = element.getClassifier() != null ? element.getClassifier().getName() : "Unknown";

        // Build content based on element type
        if (element instanceof ConcreteFunctionDefinition)
        {
            buildFunctionHover(sb, (ConcreteFunctionDefinition<?>) element);
        }
        else if (element instanceof Class)
        {
            buildClassHover(sb, (Class<?>) element);
        }
        else if (element instanceof Enumeration)
        {
            buildEnumerationHover(sb, (Enumeration<?>) element);
        }
        else if (element instanceof Property)
        {
            buildPropertyHover(sb, (Property<?, ?>) element);
        }
        else
        {
            // Generic hover for other elements
            buildGenericHover(sb, element, classifierName);
        }

        return sb.toString();
    }

    private void buildFunctionHover(StringBuilder sb, ConcreteFunctionDefinition<?> function)
    {
        sb.append("```pure\n");
        sb.append("function ");
        try
        {
            sb.append(Function.prettyPrint(function, session.getProcessorSupport()));
        }
        catch (Exception e)
        {
            sb.append(PackageableElement.getUserPathForPackageableElement(function));
        }
        sb.append("\n```\n");

        String path = PackageableElement.getUserPathForPackageableElement(function);
        sb.append("\n**Path:** `").append(path).append("`");
    }

    private void buildClassHover(StringBuilder sb, Class<?> cls)
    {
        sb.append("```pure\n");
        sb.append("Class ");
        sb.append(PackageableElement.getUserPathForPackageableElement(cls));
        sb.append("\n```\n");

        // Add properties summary
        try
        {
            if (cls._properties() != null && cls._properties().notEmpty())
            {
                sb.append("\n**Properties:**\n");
                cls._properties().forEach(prop ->
                {
                    sb.append("- `").append(prop._name()).append("`\n");
                });
            }
        }
        catch (Exception e)
        {
            // Ignore property errors
        }
    }

    private void buildEnumerationHover(StringBuilder sb, Enumeration<?> enumeration)
    {
        sb.append("```pure\n");
        sb.append("Enum ");
        sb.append(PackageableElement.getUserPathForPackageableElement(enumeration));
        sb.append("\n```\n");

        // Add values
        try
        {
            if (enumeration._values() != null && enumeration._values().notEmpty())
            {
                sb.append("\n**Values:**\n");
                enumeration._values().forEach(val ->
                {
                    sb.append("- `").append(val._name()).append("`\n");
                });
            }
        }
        catch (Exception e)
        {
            // Ignore value errors
        }
    }

    private void buildPropertyHover(StringBuilder sb, Property<?, ?> property)
    {
        sb.append("```pure\n");
        sb.append("property ");
        sb.append(property._name());
        sb.append("\n```\n");

        try
        {
            if (property._owner() != null)
            {
                sb.append("\n**Owner:** `").append(PackageableElement.getUserPathForPackageableElement(property._owner())).append("`");
            }
        }
        catch (Exception e)
        {
            // Ignore owner errors
        }
    }

    private void buildGenericHover(StringBuilder sb, CoreInstance element, String classifierName)
    {
        sb.append("**").append(classifierName).append("**\n\n");

        String path = PackageableElement.getUserPathForPackageableElement(element);
        if (path != null && !path.isEmpty())
        {
            sb.append("**Path:** `").append(path).append("`\n");
        }

        SourceInformation sourceInfo = element.getSourceInformation();
        if (sourceInfo != null)
        {
            sb.append("\n**Source:** `").append(sourceInfo.getSourceId());
            sb.append(":").append(sourceInfo.getLine());
            sb.append(":").append(sourceInfo.getColumn()).append("`");
        }
    }
}
