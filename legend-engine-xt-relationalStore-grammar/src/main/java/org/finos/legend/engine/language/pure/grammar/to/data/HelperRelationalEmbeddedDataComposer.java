// Copyright 2022 Goldman Sachs
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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperRelationalEmbeddedDataComposer
{
    private PureGrammarComposerContext context;

    public HelperRelationalEmbeddedDataComposer(PureGrammarComposerContext context)
    {
        this.context = context;
    }

    public String visitRelationalDataEmbeddedData(RelationalCSVData relationalData)
    {
        String indentString = context.getIndentationString();
        return ListIterate.collect(relationalData.tables, table -> this.visitTable(table, indentString)).makeString("\n\n");

    }

    public String visitTable(RelationalCSVTable table, String baseIndentation)
    {
        StringBuilder str = new StringBuilder();
        str.append(baseIndentation).append(table.schema).append(".").append(table.table).append(":");
        if (table.values != null)
        {
            MutableList<String> lines = org.eclipse.collections.api.factory.Lists.mutable.of(table.values.split("\\n"));
            str.append("\n" + lines.collect(l -> getTabString(5) + convertString(l + "\n", true)).makeString("+\n"));
        }
        str.append(";");
        return str.toString();
    }

}
