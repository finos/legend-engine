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

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElement;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElementsData;

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
        str.append(baseIndentation).append(String.join(".", relationElement.paths)).append(":");
        if (relationElement.columns != null)
        {
            String allColumns = String.join(",", relationElement.columns.stream().map(this::escapeRowValue).toArray(String[]::new));
            str.append("\n").append(baseIndentation).append(getTabString()).append(allColumns).append("\n");
        }
        if (relationElement.rows != null)
        {
            relationElement.rows.forEach(row ->
            {
                String allValues = String.join(",", row.values.stream().map(this::escapeRowValue).toArray(String[]::new));
                str.append(baseIndentation).append(getTabString()).append(allValues).append("\n");
            });
        }
        str.setLength(str.length() - 1);
        str.append(";");
        return str.toString();
    }

    private String escapeRowValue(String input)
    {
        if (input == null)
        {
            return "";
        }
        return StringEscapeUtils.escapeJava(input).replace("\\\"", "\"").replace(",", "\\,").replace(";", "\\;");
    }
}
