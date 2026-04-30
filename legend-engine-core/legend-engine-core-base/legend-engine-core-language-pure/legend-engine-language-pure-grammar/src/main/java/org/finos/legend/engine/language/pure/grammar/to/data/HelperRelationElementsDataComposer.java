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
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.to.data;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElement;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElementsData;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationRowTestData;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperRelationElementsDataComposer
{
    private PureGrammarComposerContext context;

    public HelperRelationElementsDataComposer(PureGrammarComposerContext context)
    {
        this.context = context;
    }

    public String visitRelationElementsData(RelationElementsData relationElementsData)
    {
        String indentString = context.getIndentationString();
        return ListIterate.collect(relationElementsData.relationElements, table -> this.visitRelationElement(table, indentString)).makeString("\n\n");
    }

    private String visitRelationElement(RelationElement relationElement, String baseIndentation)
    {
        StringBuilder str = new StringBuilder();
        boolean isStandAloneRelation = relationElement.paths == null || relationElement.paths.isEmpty();
        if (!isStandAloneRelation)
        {
            str.append(baseIndentation).append(String.join(".", relationElement.paths)).append(":\n");
            str.append(renderAlignedRelationElement(relationElement, baseIndentation + getTabString(), isStandAloneRelation));
        }
        else
        {
            str.append(renderAlignedRelationElement(relationElement, baseIndentation, isStandAloneRelation));
        }
        return str.toString();
    }

    /**
     * Renders a RelationElement as an aligned table inside #{ ... }#.
     * Computes max column widths and pads values with spaces for readability.
     */
    public static String renderAlignedRelationElement(RelationElement element, String baseIndentation, boolean isStandAloneRelation)
    {
        String innerIndent = baseIndentation + getTabString(1);
        List<String> columns = element.columns != null ? element.columns : Collections.emptyList();
        List<RelationRowTestData> rows = element.rows != null ? element.rows : Collections.emptyList();

        int numCols = columns.size();
        int numRows = rows.size();
        int[] maxWidths = new int[numCols];
        for (int i = 0; i < numCols; i++)
        {
            maxWidths[i] = escapeRelationValue(columns.get(i)).length();
        }
        for (RelationRowTestData row : rows)
        {
            for (int i = 0; i < numCols && i < row.values.size(); i++)
            {
                maxWidths[i] = Math.max(maxWidths[i], escapeRelationValue(row.values.get(i)).length());
            }
        }

        StringBuilder str = new StringBuilder();
        if (isStandAloneRelation)
        {
            str.append(baseIndentation).append("#{\n");
        }

        // Column header
        str.append(innerIndent);
        for (int i = 0; i < numCols; i++)
        {
            if (i > 0)
            {
                str.append(", ");
            }
            String val = escapeRelationValue(columns.get(i));
            str.append(i < numCols - 1 ? padRight(val, maxWidths[i]) : val);
        }
        if (numRows == 0)
        {   str.append(";");
            return str.toString();
        }

        str.append("\n");

        // Data rows
        for (int j = 0; j < numRows; j++)
        {
            RelationRowTestData row = rows.get(j);
            str.append(innerIndent);
            for (int i = 0; i < numCols; i++)
            {
                if (i > 0)
                {
                    str.append(", ");
                }
                String value = i < row.values.size() ? escapeRelationValue(row.values.get(i)) : "";
                str.append(i < numCols - 1 ? padRight(value, maxWidths[i]) : value);
            }
            if (j < numRows - 1)
            {
                str.append("\n");
            }
            else
            {
                str.append(";");
                if (!isStandAloneRelation)
                {
                    return str.toString();
                }
                str.append("\n");
            }
        }

        if (isStandAloneRelation)
        {
            str.append(baseIndentation).append("}#");
        }
        return str.toString();
    }

    private static String padRight(String s, int width)
    {
        if (s.length() >= width)
        {
            return s;
        }
        StringBuilder sb = new StringBuilder(width);
        sb.append(s);
        for (int i = s.length(); i < width; i++)
        {
            sb.append(' ');
        }
        return sb.toString();
    }

    public static String escapeRelationValue(String input)
    {
        if (input == null)
        {
            return "";
        }
        if (input.startsWith("\"") && input.endsWith("\""))
        {
            return input;
        }
        if (input.contains(",") || input.contains(";"))
        {
            return "\"" + input.replace("\"", "\\\"") + "\"";
        }
        return input;
    }

}
