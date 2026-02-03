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

package org.finos.legend.engine.ide.lsp.converters;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;

/**
 * Converter for Pure suggestion types to LSP CompletionItem objects.
 */
public class CompletionItemConverter
{
    private CompletionItemConverter()
    {
        // Utility class - no instantiation
    }

    /**
     * Create a CompletionItem for a package path suggestion.
     */
    public static CompletionItem createPathCompletion(String pureName, String pureId, String pureType)
    {
        CompletionItem item = new CompletionItem();
        item.setLabel(pureName);
        item.setDetail(pureId);
        item.setKind(getCompletionKind(pureType));
        item.setInsertText(pureName);
        item.setFilterText(pureName);
        return item;
    }

    /**
     * Create a CompletionItem for an identifier (class, function, enum) suggestion.
     */
    public static CompletionItem createIdentifierCompletion(String pureName, String pureId, String pureType, String text)
    {
        CompletionItem item = new CompletionItem();
        item.setLabel(text != null ? text : pureName);
        item.setDetail(pureId);
        item.setKind(getCompletionKind(pureType));
        item.setFilterText(pureName);

        // For functions, use the full signature as insert text
        if ("ConcreteFunctionDefinition".equals(pureType) || "NativeFunction".equals(pureType))
        {
            item.setInsertText(pureName + "($1)");
            item.setInsertTextFormat(InsertTextFormat.Snippet);
        }
        else
        {
            item.setInsertText(pureName);
        }

        return item;
    }

    /**
     * Create a CompletionItem for a class with required properties.
     */
    public static CompletionItem createClassCompletion(String pureName, String pureId, String[] requiredProperties)
    {
        CompletionItem item = new CompletionItem();
        item.setLabel(pureName);
        item.setDetail(pureId);
        item.setKind(CompletionItemKind.Class);
        item.setFilterText(pureName);

        // Create snippet with required properties
        if (requiredProperties != null && requiredProperties.length > 0)
        {
            StringBuilder snippet = new StringBuilder();
            snippet.append("^").append(pureName).append("(");
            for (int i = 0; i < requiredProperties.length; i++)
            {
                if (i > 0)
                {
                    snippet.append(", ");
                }
                snippet.append(requiredProperties[i]).append(" = ${").append(i + 1).append("}");
            }
            snippet.append(")");
            item.setInsertText(snippet.toString());
            item.setInsertTextFormat(InsertTextFormat.Snippet);
        }
        else
        {
            item.setInsertText("^" + pureName + "($1)");
            item.setInsertTextFormat(InsertTextFormat.Snippet);
        }

        return item;
    }

    /**
     * Create a CompletionItem for an attribute (property, tag, stereotype, enum value) suggestion.
     */
    public static CompletionItem createAttributeCompletion(String pureName, String pureType, String owner, String ownerType)
    {
        CompletionItem item = new CompletionItem();
        item.setLabel(pureName);
        item.setDetail(owner + " (" + ownerType + ")");
        item.setKind(getAttributeCompletionKind(pureType));
        item.setInsertText(pureName);
        item.setFilterText(pureName);
        return item;
    }

    /**
     * Create a CompletionItem for a variable suggestion.
     */
    public static CompletionItem createVariableCompletion(String variableName)
    {
        CompletionItem item = new CompletionItem();
        item.setLabel("$" + variableName);
        item.setKind(CompletionItemKind.Variable);
        item.setInsertText("$" + variableName);
        item.setFilterText(variableName);
        return item;
    }

    /**
     * Create a keyword completion item.
     */
    public static CompletionItem createKeywordCompletion(String keyword)
    {
        CompletionItem item = new CompletionItem();
        item.setLabel(keyword);
        item.setKind(CompletionItemKind.Keyword);
        item.setInsertText(keyword);
        return item;
    }

    /**
     * Map Pure type names to LSP CompletionItemKind.
     */
    private static CompletionItemKind getCompletionKind(String pureType)
    {
        if (pureType == null)
        {
            return CompletionItemKind.Text;
        }

        switch (pureType)
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
            case "Property":
            case "QualifiedProperty":
                return CompletionItemKind.Property;
            case "Profile":
                return CompletionItemKind.Interface;
            case "Association":
                return CompletionItemKind.Struct;
            case "Mapping":
            case "Store":
            case "Database":
            case "Diagram":
                return CompletionItemKind.File;
            default:
                return CompletionItemKind.Text;
        }
    }

    /**
     * Map attribute types to LSP CompletionItemKind.
     */
    private static CompletionItemKind getAttributeCompletionKind(String pureType)
    {
        if (pureType == null)
        {
            return CompletionItemKind.Field;
        }

        switch (pureType)
        {
            case "Property":
            case "QualifiedProperty":
                return CompletionItemKind.Property;
            case "Tag":
                return CompletionItemKind.Constant;
            case "Stereotype":
                return CompletionItemKind.TypeParameter;
            case "Enum":
                return CompletionItemKind.EnumMember;
            default:
                return CompletionItemKind.Field;
        }
    }
}
