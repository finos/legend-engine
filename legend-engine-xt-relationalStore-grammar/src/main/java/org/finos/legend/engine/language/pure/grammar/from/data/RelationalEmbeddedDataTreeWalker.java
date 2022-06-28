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

package org.finos.legend.engine.language.pure.grammar.from.data;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.RelationalEmbeddedDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;

public class RelationalEmbeddedDataTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final SourceInformation sourceInformation;
    private final PureGrammarParserExtensions extensions;

    public RelationalEmbeddedDataTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.sourceInformation = sourceInformation;
        this.extensions = extensions;
    }

    public RelationalCSVData visit(RelationalEmbeddedDataParserGrammar.DefinitionContext context)
    {
        RelationalCSVData relationalData = new RelationalCSVData();
        relationalData.sourceInformation = this.sourceInformation;
        relationalData.tables = ListIterate.collect(context.csvTable(), this::visitTable);
        return relationalData;
    }

    public RelationalCSVTable visitTable(RelationalEmbeddedDataParserGrammar.CsvTableContext tableContext)
    {
        RelationalCSVTable table = new RelationalCSVTable();
        table.table = PureGrammarParserUtility.fromIdentifier(tableContext.table());
        table.schema = PureGrammarParserUtility.fromIdentifier(tableContext.schema());
        table.sourceInformation = this.walkerSourceInformation.getSourceInformation(tableContext);
        if (tableContext.rows() != null)
        {
            table.values =  ListIterate.collect(tableContext.rows().STRING(), x -> PureGrammarParserUtility.fromGrammarString(x.getText().replace("\\;","\\\\;"), true)).makeString("");
        }
        return table;
    }

}
