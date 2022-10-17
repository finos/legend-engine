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

package org.finos.legend.engine.language.sql.grammar.from;

import org.antlr.v4.runtime.ParserRuleContext;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseBaseVisitor;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.protocol.sql.metamodel.AllColumns;
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.QueryBody;
import org.finos.legend.engine.protocol.sql.metamodel.QuerySpecification;
import org.finos.legend.engine.protocol.sql.metamodel.Relation;
import org.finos.legend.engine.protocol.sql.metamodel.Select;
import org.finos.legend.engine.protocol.sql.metamodel.SelectItem;
import org.finos.legend.engine.protocol.sql.metamodel.Table;

import java.util.List;

import static org.eclipse.collections.impl.collector.Collectors2.toList;

public class SqlVisitor extends SqlBaseBaseVisitor<Node>
{
    @Override
    public Node visitSingleStatement(SqlBaseParser.SingleStatementContext ctx)
    {
        return visit(ctx.statement());
    }

    @Override
    public Node visitQuery(SqlBaseParser.QueryContext ctx)
    {
        Query body = (Query) visit(ctx.queryNoWith());
        return body;
    }

    @Override
    public Node visitQueryNoWith(SqlBaseParser.QueryNoWithContext ctx)
    {
        QueryBody term = (QueryBody) visit(ctx.queryTerm());
        Query query = new Query();
        query.queryBody = term;
        return query;
    }

    @Override
    public Node visitDefaultQuerySpec(SqlBaseParser.DefaultQuerySpecContext ctx)
    {
        List<SelectItem> selectItems = visit(ctx.selectItem(), SelectItem.class);
        List<Relation> relations = visit(ctx.relation(), Relation.class);
        QuerySpecification querySpecification = new QuerySpecification();
        Select select = new Select();
        select.selectItems = selectItems;
        select.distinct = isDistinct(ctx.setQuant());
        querySpecification.select = select;
        querySpecification.from = relations;
        return querySpecification;
    }

    @Override
    public Node visitSelectAll(SqlBaseParser.SelectAllContext ctx)
    {
        return new AllColumns();
    }

    @Override
    public Node visitAliasedRelation(SqlBaseParser.AliasedRelationContext ctx)
    {
        Relation child = (Relation) visit(ctx.relationPrimary());
        return child;
    }

    @Override
    public Node visitTableName(SqlBaseParser.TableNameContext ctx)
    {
        Table table = new Table();
        table.name = ctx.qname().getText();
        return table;
    }

    private <T> List<T> visit(List<? extends ParserRuleContext> contexts, Class<T> clazz)
    {
        return contexts.stream()
                .map(this::visit)
                .map(clazz::cast)
                .collect(toList());
    }

    private static boolean isDistinct(SqlBaseParser.SetQuantContext setQuantifier)
    {
        return setQuantifier != null && setQuantifier.DISTINCT() != null;
    }
}
