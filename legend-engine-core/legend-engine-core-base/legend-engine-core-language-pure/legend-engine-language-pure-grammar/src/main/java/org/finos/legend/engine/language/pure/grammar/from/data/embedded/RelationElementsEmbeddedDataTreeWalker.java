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

package org.finos.legend.engine.language.pure.grammar.from.data.embedded;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.relation.RelationElementsDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElement;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElementsData;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationRowTestData;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class RelationElementsEmbeddedDataTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final SourceInformation sourceInformation;
    private final PureGrammarParserExtensions extensions;

    public RelationElementsEmbeddedDataTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.sourceInformation = sourceInformation;
        this.extensions = extensions;
    }

    public RelationElementsData visit(RelationElementsDataParserGrammar.DefinitionContext ctx)
    {
        RelationElementsData relationElementsData = new RelationElementsData();
        relationElementsData.relationElements = ListIterate.collect(ctx.relationElement(), this::visitRelationElement);
        relationElementsData.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return relationElementsData;
    }

    private RelationElement visitRelationElement(RelationElementsDataParserGrammar.RelationElementContext ctx)
    {
        RelationElement relationElement = new RelationElement();
        relationElement.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        relationElement.paths = ctx.paths().identifier().stream().map(PureGrammarParserUtility::fromIdentifier).collect(Collectors.toList());
        RelationElementsDataParserGrammar.TableContext tableContext = ctx.table();
        relationElement.columns = tableContext.columnNames().cell().stream()
                .map(cellContext -> StringEscapeUtils.unescapeJava(cellContext.ROW_VALUE() != null ? cellContext.ROW_VALUE().getText() : ""))
                .collect(Collectors.toList());
        relationElement.rows = new ArrayList<>();
        tableContext.rows().rowValues().forEach(rowValuesContext ->
        {
            RelationRowTestData row = new RelationRowTestData();
            row.values = rowValuesContext.cell().stream()
                    .map(cellContext -> StringEscapeUtils.unescapeJava(cellContext.ROW_VALUE() != null ? cellContext.ROW_VALUE().getText() : ""))
                    .collect(Collectors.toList());;
            relationElement.rows.add(row);
        });
        return relationElement;
    }
}
