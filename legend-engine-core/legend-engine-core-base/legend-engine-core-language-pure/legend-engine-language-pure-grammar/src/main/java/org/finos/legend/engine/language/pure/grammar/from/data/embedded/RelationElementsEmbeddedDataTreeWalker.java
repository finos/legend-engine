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

    public RelationElementsEmbeddedDataTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        this.walkerSourceInformation = walkerSourceInformation;
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

        // Determine if this is a path-based relationElement (paths TABLE_START table)
        // or a flat CSV relationElement (TABLE_START tableCSV)
        RelationElementsDataParserGrammar.ColumnNamesContext columnNamesCtx;
        RelationElementsDataParserGrammar.RowsContext rowsCtx;

        if (ctx.table() != null)
        {
            relationElement.paths = ctx.paths().identifier().stream().map(PureGrammarParserUtility::fromIdentifier).collect(Collectors.toList());
            columnNamesCtx = ctx.table().columnNames();
            rowsCtx = ctx.table().rows();
        }
        else
        {
            relationElement.paths = java.util.Collections.emptyList();
            columnNamesCtx = ctx.tableCSV().columnNames();
            rowsCtx = ctx.tableCSV().rows();
        }

        relationElement.columns = columnNamesCtx.cell().stream()
                .map(this::parseCellValue)
                .collect(Collectors.toList());
        relationElement.rows = new ArrayList<>();
        rowsCtx.rowValues().forEach(rowValuesContext ->
        {
            RelationRowTestData row = new RelationRowTestData();
            row.values = rowValuesContext.cell().stream()
                    .map(this::parseCellValue)
                    .collect(Collectors.toList());
            relationElement.rows.add(row);
        });
        return relationElement;
    }

    private String parseCellValue(RelationElementsDataParserGrammar.CellContext cellContext)
    {
        if (cellContext.QUOTED_ROW_VALUE() != null)
        {
            return cellContext.QUOTED_ROW_VALUE().getText().trim();
        }
        String text = cellContext.ROW_VALUE() != null ? cellContext.ROW_VALUE().getText() : "";
        return text.trim();
    }
}
