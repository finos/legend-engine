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
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseBaseVisitor;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseLexer;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.protocol.sql.metamodel.AllColumns;
import org.finos.legend.engine.protocol.sql.metamodel.AllRows;
import org.finos.legend.engine.protocol.sql.metamodel.ComparisonExpression;
import org.finos.legend.engine.protocol.sql.metamodel.ComparisonOperator;
import org.finos.legend.engine.protocol.sql.metamodel.Expression;
import org.finos.legend.engine.protocol.sql.metamodel.Identifier;
import org.finos.legend.engine.protocol.sql.metamodel.Limit;
import org.finos.legend.engine.protocol.sql.metamodel.LongLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.finos.legend.engine.protocol.sql.metamodel.OrderBy;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.QueryBody;
import org.finos.legend.engine.protocol.sql.metamodel.QuerySpecification;
import org.finos.legend.engine.protocol.sql.metamodel.Relation;
import org.finos.legend.engine.protocol.sql.metamodel.Select;
import org.finos.legend.engine.protocol.sql.metamodel.SelectItem;
import org.finos.legend.engine.protocol.sql.metamodel.SingleColumn;
import org.finos.legend.engine.protocol.sql.metamodel.SortItem;
import org.finos.legend.engine.protocol.sql.metamodel.SortItemNullOrdering;
import org.finos.legend.engine.protocol.sql.metamodel.SortItemOrdering;
import org.finos.legend.engine.protocol.sql.metamodel.Table;

import java.util.List;

import static org.eclipse.collections.impl.collector.Collectors2.toList;

public class SqlVisitor extends SqlBaseBaseVisitor<Node>
{
    @Override
    public Node visitSingleStatement(SqlBaseParser.SingleStatementContext ctx)
    {
        return ctx.statement().accept(this);
    }

    @Override
    public Node visitQuery(SqlBaseParser.QueryContext ctx)
    {
        return ctx.queryNoWith().accept(this);
    }

    @Override
    public Node visitQueryNoWith(SqlBaseParser.QueryNoWithContext ctx)
    {
        QueryBody term = (QueryBody) ctx.queryTerm().accept(this);

        Limit limit = visitIfPresent(ctx.limitClause(), Limit.class);

        OrderBy orderBy = new OrderBy();
        orderBy.sortItems = visit(ctx.sortItem(), SortItem.class);

        Query query = new Query();
        query.queryBody = term;
        query.limit = limit;
        query.orderBy = orderBy;
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
        querySpecification.where = visitIfPresent(ctx.where(), Expression.class);
        return querySpecification;
    }

    @Override
    public Node visitSelectAll(SqlBaseParser.SelectAllContext ctx)
    {
        return new AllColumns();
    }

    @Override
    public Node visitSelectSingle(SqlBaseParser.SelectSingleContext ctx)
    {
        SingleColumn singleColumn = new SingleColumn();
        singleColumn.expression = (Expression) ctx.expr().accept(this);
        singleColumn.prefix = visitIfPresent(ctx.ident(), Identifier.class);
        return singleColumn;
    }

    @Override
    public Node visitAliasedRelation(SqlBaseParser.AliasedRelationContext ctx)
    {
        return ctx.relationPrimary().accept(this);
    }

    @Override
    public Node visitTableName(SqlBaseParser.TableNameContext ctx)
    {
        Table table = new Table();
        SqlBaseParser.QnameContext qname = ctx.qname();
        table.name = getQualifiedName(qname);
        return table;
    }

    @Override
    public Node visitUnquotedIdentifier(SqlBaseParser.UnquotedIdentifierContext ctx)
    {
        Identifier identifier = new Identifier();
        identifier.delimited = false;
        identifier.value = ctx.getText();
        return identifier;
    }

    @Override
    public Node visitQuotedIdentifier(SqlBaseParser.QuotedIdentifierContext ctx)
    {
        Identifier identifier = new Identifier();
        identifier.delimited = true;
        String text = ctx.getText();
        identifier.value = text.substring(1, text.length() - 1)
                .replace("\"\"", "\"");
        return identifier;
    }

    @Override
    public Node visitLimitClause(SqlBaseParser.LimitClauseContext ctx)
    {
        if (ctx.LIMIT() == null)
        {
            throw new IllegalArgumentException("LIMIT value missing");
        }
        Expression rowCount;
        if (ctx.ALL() != null)
        {
            rowCount = new AllRows();
        }
        else if (ctx.limit != null)
        {
            rowCount = (Expression) ctx.limit.accept(this);
        }
        else
        {
            throw new IllegalArgumentException("LIMIT must contain [ALL|integer|param]");
        }
        Limit limit = new Limit();
        limit.rowCount = rowCount;
        return limit;
    }

    @Override
    public Node visitIntegerLiteral(SqlBaseParser.IntegerLiteralContext ctx)
    {
        String text = ctx.getText();
        long limit = Long.parseLong(text);
        LongLiteral limitLongLiteral = new LongLiteral();
        limitLongLiteral.value = limit;
        return limitLongLiteral;
    }

    @Override
    public Node visitSortItem(SqlBaseParser.SortItemContext ctx)
    {
        SortItem sortItem = new SortItem();
        sortItem.sortKey = (Expression) ctx.expr().accept(this);
        sortItem.ordering = getOrdering(ctx.ordering);
        sortItem.nullOrdering = getNullOrdering(ctx.nullOrdering);
        return sortItem;
    }

    @Override
    public Node visitWhere(SqlBaseParser.WhereContext ctx)
    {
        return ctx.condition.accept(this);
    }

    @Override
    public Node visitComparison(SqlBaseParser.ComparisonContext ctx)
    {
        TerminalNode cmpOpNode = (TerminalNode) ctx.cmpOp().getChild(0);
        Token cmpOpSymbol = cmpOpNode.getSymbol();

        ComparisonExpression comparisonExpression = new ComparisonExpression();
        comparisonExpression.operator = getComparisonOperator(cmpOpSymbol);
        comparisonExpression.left = (Expression) ctx.value.accept(this);
        comparisonExpression.right = (Expression) ctx.right.accept(this);

        return comparisonExpression;
    }

    private static ComparisonOperator getComparisonOperator(Token cmpOpSymbol)
    {
        switch (cmpOpSymbol.getType())
        {
            case SqlBaseParser.EQ:
                return ComparisonOperator.EQUAL;
            case SqlBaseParser.NEQ:
                return ComparisonOperator.NOT_EQUAL;
            case SqlBaseParser.LT:
                return ComparisonOperator.LESS_THAN;
            case SqlBaseParser.LTE:
                return ComparisonOperator.LESS_THAN_OR_EQUAL;
            case SqlBaseParser.GT:
                return ComparisonOperator.GREATER_THAN;
            case SqlBaseParser.GTE:
                return ComparisonOperator.GREATER_THAN_OR_EQUAL;
        }
        throw new IllegalArgumentException("Unknown comparison operator: " + cmpOpSymbol.getText());

    }

    private SortItemNullOrdering getNullOrdering(Token token)
    {
        if (token != null)
        {
            switch (token.getType())
            {
                case SqlBaseLexer.FIRST:
                    return SortItemNullOrdering.FIRST;
                case SqlBaseLexer.LAST:
                    return SortItemNullOrdering.LAST;
                default:
                    throw new IllegalArgumentException("Unknown null ordering: " + token.getText());
            }
        }
        return SortItemNullOrdering.UNDEFINED;
    }

    private SortItemOrdering getOrdering(Token token)
    {
        if (token != null)
        {
            switch (token.getType())
            {
                case SqlBaseParser.ASC:
                    return SortItemOrdering.ASCENDING;
                case SqlBaseLexer.DESC:
                    return SortItemOrdering.DESCENDING;
                default:
                    throw new IllegalArgumentException("Unknown ordering: " + token.getText());
            }
        }
        return SortItemOrdering.ASCENDING;
    }

    private <T> T visitIfPresent(ParserRuleContext context, Class<T> clazz)
    {
        if (context == null)
        {
            return null;
        }
        Node node = context.accept(this);
        return clazz.cast(node);
    }

    private <T> List<T> visit(List<? extends ParserRuleContext> contexts, Class<T> clazz)
    {
        return contexts.stream()
                .map(i -> i.accept(this))
                .map(clazz::cast)
                .collect(toList());
    }

    private static boolean isDistinct(SqlBaseParser.SetQuantContext setQuantifier)
    {
        return setQuantifier != null && setQuantifier.DISTINCT() != null;
    }

    private List<Identifier> getQualifiedName(SqlBaseParser.QnameContext qnameContext)
    {
        List<SqlBaseParser.IdentContext> ident = qnameContext.ident();
        return visit(ident, Identifier.class);
    }
}
