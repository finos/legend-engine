// Copyright 2025 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres.protocol.sql.serialization;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParserVisitor;

import java.util.Objects;

public class SQLSerializer implements SqlBaseParserVisitor<String>
{
    @Override
    public String visitSingleStatement(SqlBaseParser.SingleStatementContext ctx)
    {
        return ctx.statement().accept(this) + ";";
    }

    @Override
    public String visitDefault(SqlBaseParser.DefaultContext ctx)
    {
        return ctx.query().accept(this);
    }

    @Override
    public String visitQuery(SqlBaseParser.QueryContext ctx)
    {
        return (ctx.with() == null ? "" : ctx.with().accept(this) + " ") + ctx.queryNoWith().accept(this);
    }

    @Override
    public String visitQueryNoWith(SqlBaseParser.QueryNoWithContext ctx)
    {
        MutableList<String> str = ListIterate.collect(ctx.sortItem(), x -> x.accept(this));
        return ctx.queryTerm().accept(this) + (str.isEmpty() ? "" : " order by " + str.makeString(", ")) +
                (ctx.limitClause() == null ? "" : ctx.limitClause().accept(this)) +
                (ctx.offsetClause() == null ? "" : ctx.offsetClause().accept(this));
    }

    @Override
    public String visitQueryTermDefault(SqlBaseParser.QueryTermDefaultContext ctx)
    {
        return ctx.querySpec().accept(this);
    }

    @Override
    public String visitSortItem(SqlBaseParser.SortItemContext ctx)
    {
        return ctx.expr().accept(this) + (ctx.ordering == null ? "" : " " + ctx.ordering.getText()) + (ctx.nullOrdering == null ? "" : " NULLS " + ctx.nullOrdering.getText());
    }

    @Override
    public String visitPrimaryRelation(SqlBaseParser.PrimaryRelationContext ctx)
    {
        return ctx.relationPrimary().accept(this);
    }

    @Override
    public String visitQueryRelation(SqlBaseParser.QueryRelationContext ctx)
    {
        return ctx.query().accept(this);
    }

    @Override
    public String visitDefaultQuerySpec(SqlBaseParser.DefaultQuerySpecContext ctx)
    {
//                SELECT setQuant? selectItem (COMMA selectItem)*
//                    (FROM relation (COMMA relation)*)?
//                where?
//                        (GROUP BY expr (COMMA expr)*)?
//                (HAVING having=booleanExpression)?
//                (WINDOW windows+=namedWindow (COMMA windows+=namedWindow)*)?


        return "select " + (ctx.setQuant() == null ? "" : ctx.setQuant().getText() + " ") + ListIterate.collect(ctx.selectItem(), x -> x.accept(this)).makeString(", ") +
                (ctx.FROM() == null ? "" : " from " + ListIterate.collect(ctx.relation(), x -> x.accept(this)).makeString(", ")) +
                (ctx.where() == null ? "" : ctx.where().accept(this)) +
                (ctx.GROUP() == null ? "" : " group by " + ListIterate.collect(ctx.expr(), c -> c.accept(this)).makeString(", ")) +
                (ctx.having == null ? "" : " having " + ctx.having.accept(this));
        //+ ListIterate.collect(ctx.windows, c -> c.name.getText() + " as " + c.windowDefinition().accept(this)).makeString(", ");
    }

    @Override
    public String visitSelectSingle(SqlBaseParser.SelectSingleContext ctx)
    {
        return ctx.expr().accept(this) + (ctx.ident() == null ? "" : (ctx.AS() == null ? "" : " as") + " " + ctx.ident().accept(this));
    }

    @Override
    public String visitWhere(SqlBaseParser.WhereContext ctx)
    {
        return " where " + ctx.condition.accept(this);
    }

    @Override
    public String visitRelationDefault(SqlBaseParser.RelationDefaultContext ctx)
    {
        return ctx.aliasedRelation().accept(this);
    }

    @Override
    public String visitJoinRelation(SqlBaseParser.JoinRelationContext ctx)
    {
//                left=relation
//                        ( CROSS JOIN right=aliasedRelation (WITH ORDINALITY AS? ident aliasedColumns)?
//                                | joinType JOIN rightRelation=relation joinCriteria
//                                | NATURAL joinType JOIN right=aliasedRelation
//                        )                                                                              #joinRelation

        return ctx.left.accept(this) + " " +
                (ctx.operator == null ? "" : ctx.operator.getText() + (ctx.setQuant() == null ? "" : " " + ctx.setQuant().accept(this)) + " " + ctx.right.accept(this)) +
                (ctx.CROSS() == null ? "" : ctx.CROSS().accept(this) + " join " + ctx.right.accept(this) + (ctx.WITH() == null ? "" : " with ordinality " + ctx.ident().accept(this) + ctx.aliasedColumns().accept(this))) +
                (ctx.joinCriteria() == null ? "" : ctx.joinType().accept(this) + "join " + ctx.rightRelation.accept(this) + " " + ctx.joinCriteria().accept(this)) +
                (ctx.NATURAL() == null ? "" : "natural " + ctx.joinType().accept(this) + "join " + ctx.right.accept(this));
    }

    @Override
    public String visitSetOperation(SqlBaseParser.SetOperationContext ctx)
    {
//            | first=querySpec operator=(INTERSECT | EXCEPT) second=querySpec                 #setOperation
//            | left=queryTerm operator=UNION setQuant? right=queryTerm                        #setOperation
        return (ctx.first == null ? "" : ctx.first.accept(this) + " " + ctx.operator.getText() + " " + ctx.second.accept(this)) +
                (ctx.left == null ? "" : ctx.left.accept(this) + " " + ctx.operator.getText() + (ctx.setQuant() == null ? "" : " " + ctx.setQuant().accept(this)) + " " + ctx.right.accept(this));
    }

    @Override
    public String visitJoinType(SqlBaseParser.JoinTypeContext ctx)
    {
        return (ctx.INNER() == null ? "" : "inner ") +
                (ctx.LEFT() == null ? "" : "left outer ") +
                (ctx.RIGHT() == null ? "" : "right outer ") +
                (ctx.FULL() == null ? "" : "full outer ");
    }

    @Override
    public String visitJoinCriteria(SqlBaseParser.JoinCriteriaContext ctx)
    {
//                ON booleanExpression
//                | USING OPEN_ROUND_BRACKET ident (COMMA ident)* CLOSE_ROUND_BRACKET
        return (ctx.ON() == null ? "" : "on " + (ctx.booleanExpression() == null ? "" : ctx.booleanExpression().accept(this)))
                + (ctx.USING() == null ? "" : "using (" + ListIterate.collect(ctx.ident(), x -> x.accept(this)).makeString(", ") + ")");
    }

    @Override
    public String visitAliasedRelation(SqlBaseParser.AliasedRelationContext ctx)
    {
        // relationPrimary (AS? ident aliasedColumns?)?
        return ctx.relationPrimary().accept(this) + (ctx.AS() == null ? "" : " as") + (ctx.ident() == null ? "" : " " + ctx.ident().accept(this)) + (ctx.aliasedColumns() == null ? "" : ctx.aliasedColumns().accept(this));
    }

    @Override
    public String visitTableRelation(SqlBaseParser.TableRelationContext ctx)
    {
        return ctx.table().accept(this);
    }

    @Override
    public String visitParameterOrSimpleLiteral(SqlBaseParser.ParameterOrSimpleLiteralContext ctx)
    {
//                : nullLiteral
//                        | intervalLiteral
//                        | escapedCharsStringLiteral
//                        | stringLiteral
//                        | numericLiteral
//                        | booleanLiteral
//                        | bitString
//                        | parameterExpr
        return (ctx.nullLiteral() == null ? "" : ctx.nullLiteral().accept(this)) +
                (ctx.intervalLiteral() == null ? "" : ctx.intervalLiteral().accept(this)) +
                (ctx.escapedCharsStringLiteral() == null ? "" : ctx.escapedCharsStringLiteral().accept(this)) +
                (ctx.stringLiteral() == null ? "" : ctx.stringLiteral().accept(this)) +
                (ctx.numericLiteral() == null ? "" : ctx.numericLiteral().accept(this)) +
                (ctx.booleanLiteral() == null ? "" : ctx.booleanLiteral().accept(this)) +
                (ctx.bitString() == null ? "" : ctx.bitString().accept(this)) +
                (ctx.parameterExpr() == null ? "" : ctx.parameterExpr().accept(this));

    }

    @Override
    public String visitStringLiteral(SqlBaseParser.StringLiteralContext ctx)
    {
        return ctx.getText();
    }

    @Override
    public String visitCmpOp(SqlBaseParser.CmpOpContext ctx)
    {
        return ctx.getText();
    }

    @Override
    public String visitOver(SqlBaseParser.OverContext ctx)
    {
        return " over " + ctx.windowDefinition().accept(this);
    }

    @Override
    public String visitWindowDefinition(SqlBaseParser.WindowDefinitionContext ctx)
    {
//                windowDefinition
//                : windowRef=ident
//                        | OPEN_ROUND_BRACKET
//                        (windowRef=ident)?
//                        (PARTITION BY partition+=expr (COMMA partition+=expr)*)?
//                (ORDER BY sortItem (COMMA sortItem)*)?
//                windowFrame?
//                        CLOSE_ROUND_BRACKET
//                ;
        return (ctx.windowRef == null ? "" : ctx.ident().accept(this)) +
                (ctx.OPEN_ROUND_BRACKET() == null ? "" :
                        "(" +
                                Lists.mutable.with(
                                                ctx.windowRef == null ? null : ctx.ident().accept(this),
                                                ctx.PARTITION() == null ? null : "partition by " + ListIterate.collect(ctx.partition, x -> x.accept(this)).makeString(", "),
                                                ctx.ORDER() == null ? null : "order by " + ListIterate.collect(ctx.sortItem(), x -> x.accept(this)).makeString(", "),
                                                ctx.windowFrame() == null ? null : ctx.windowFrame().accept(this)
                                        ).select(Objects::nonNull)
                                        .makeString(" ") +
                                ")"
                );
    }

    @Override
    public String visitIdent(SqlBaseParser.IdentContext ctx)
    {
        return (ctx.quotedIdent() == null ? "" : ctx.quotedIdent().accept(this)) + (ctx.unquotedIdent() == null ? "" : ctx.unquotedIdent().accept(this));
    }

    @Override
    public String visitTableName(SqlBaseParser.TableNameContext ctx)
    {
        return ctx.qname().accept(this);
    }

    @Override
    public String visitWith(SqlBaseParser.WithContext ctx)
    {
        return "with " + ListIterate.collect(ctx.namedQuery(), x -> x.accept(this)).makeString(", ");
    }

    @Override
    public String visitNamedQuery(SqlBaseParser.NamedQueryContext ctx)
    {
        return ctx.name.getText() + " as (" + ctx.query().accept(this) + ")";
    }

    @Override
    public String visitExpr(SqlBaseParser.ExprContext ctx)
    {
        return ctx.booleanExpression().accept(this);
    }

    @Override
    public String visitBooleanDefault(SqlBaseParser.BooleanDefaultContext ctx)
    {
        return ctx.predicated().accept(this);
    }

    @Override
    public String visitLogicalBinary(SqlBaseParser.LogicalBinaryContext ctx)
    {
//                    | left=booleanExpression operator=AND right=booleanExpression                    #logicalBinary
//                    | left=booleanExpression operator=OR right=booleanExpression                     #logicalBinary
        return ctx.left.accept(this) + " " + ctx.operator.getText() + " " + ctx.right.accept(this);
    }

    @Override
    public String visitPredicated(SqlBaseParser.PredicatedContext ctx)
    {
        return ctx.valueExpression.accept(this) + (ctx.predicate() == null ? "" : ctx.predicate().accept(this));
    }

    @Override
    public String visitComparison(SqlBaseParser.ComparisonContext ctx)
    {
        // cmpOp right=valueExpression
        return " " + ctx.cmpOp().accept(this) + " " + ctx.right.accept(this);
    }

    @Override
    public String visitValueExpressionDefault(SqlBaseParser.ValueExpressionDefaultContext ctx)
    {
        return ctx.primaryExpression().accept(this);
    }

    @Override
    public String visitArithmeticBinary(SqlBaseParser.ArithmeticBinaryContext ctx)
    {
//                    | left=valueExpression operator=(ASTERISK | SLASH | PERCENT | CARET)
//                right=valueExpression                                                        #arithmeticBinary
//                    | left=valueExpression operator=(PLUS | MINUS) right=valueExpression             #arithmeticBinary

        return ctx.left.accept(this) + " " + ctx.operator.getText() + " " + ctx.right.accept(this);
    }

    @Override
    public String visitDereference(SqlBaseParser.DereferenceContext ctx)
    {
        // ident (DOT ident)*
        return ListIterate.collect(ctx.ident(), x -> x.accept(this)).makeString(".");
    }

    @Override
    public String visitColumnReference(SqlBaseParser.ColumnReferenceContext ctx)
    {
        return ctx.ident().accept(this);
    }

    @Override
    public String visitDefaultParamOrLiteral(SqlBaseParser.DefaultParamOrLiteralContext ctx)
    {
        return ctx.parameterOrLiteral().accept(this);
    }

    @Override
    public String visitFunctionCall(SqlBaseParser.FunctionCallContext ctx)
    {
//                    | qname OPEN_ROUND_BRACKET ASTERISK CLOSE_ROUND_BRACKET within? filter? over?    #functionCall
//                    | qname OPEN_ROUND_BRACKET (setQuant? expr (COMMA expr)*)? (ORDER BY sortItem (COMMA sortItem)*)? CLOSE_ROUND_BRACKET within? filter?
//                    ((IGNORE|RESPECT) NULLS)? over?                                              #functionCall
        return ctx.qname().accept(this) + "(" +
                (ctx.setQuant() == null ? "" : "*") +
                ListIterate.collect(ctx.expr(), x -> x.accept(this)).makeString(", ") +
                (ctx.ORDER() == null ? "" : " order by " + ListIterate.collect(ctx.sortItem(), x -> x.accept(this)).makeString(", ")) +
                ")" +
                (ctx.within() == null ? "" : ctx.within().accept(this)) +
                (ctx.filter() == null ? "" : ctx.filter().accept(this)) +
                (ctx.NULLS() == null ? "" : (ctx.IGNORE() == null ? "" : ctx.IGNORE().getText()) + (ctx.RESPECT() == null ? "" : ctx.RESPECT().getText()) + ctx.NULLS().accept(this)) +
                (ctx.over() == null ? "" : ctx.over().accept(this));
    }


    @Override
    public String visitStatements(SqlBaseParser.StatementsContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSingleExpression(SqlBaseParser.SingleExpressionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitBegin(SqlBaseParser.BeginContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitStartTransaction(SqlBaseParser.StartTransactionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCommit(SqlBaseParser.CommitContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitExplain(SqlBaseParser.ExplainContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitOptimize(SqlBaseParser.OptimizeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitRefreshTable(SqlBaseParser.RefreshTableContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitUpdate(SqlBaseParser.UpdateContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDelete(SqlBaseParser.DeleteContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitShowTransaction(SqlBaseParser.ShowTransactionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitShowCreateTable(SqlBaseParser.ShowCreateTableContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitShowTables(SqlBaseParser.ShowTablesContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitShowSchemas(SqlBaseParser.ShowSchemasContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitShowColumns(SqlBaseParser.ShowColumnsContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitShowSessionParameter(SqlBaseParser.ShowSessionParameterContext ctx)
    {
        //| SHOW (qname | ALL)                                                             #showSessionParameter
        return "show " + (ctx.qname() == null ? "" : ctx.qname().accept(this)) + (ctx.ALL() == null ? "" : "all");
    }

    @Override
    public String visitAlter(SqlBaseParser.AlterContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitResetGlobal(SqlBaseParser.ResetGlobalContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSetTransaction(SqlBaseParser.SetTransactionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSetSessionAuthorization(SqlBaseParser.SetSessionAuthorizationContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitResetSessionAuthorization(SqlBaseParser.ResetSessionAuthorizationContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSet(SqlBaseParser.SetContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSetGlobal(SqlBaseParser.SetGlobalContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSetLicense(SqlBaseParser.SetLicenseContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSetTimeZone(SqlBaseParser.SetTimeZoneContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitKill(SqlBaseParser.KillContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitInsert(SqlBaseParser.InsertContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitRestore(SqlBaseParser.RestoreContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCopyFrom(SqlBaseParser.CopyFromContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCopyTo(SqlBaseParser.CopyToContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDrop(SqlBaseParser.DropContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitGrantPrivilege(SqlBaseParser.GrantPrivilegeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDenyPrivilege(SqlBaseParser.DenyPrivilegeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitRevokePrivilege(SqlBaseParser.RevokePrivilegeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreate(SqlBaseParser.CreateContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDeallocate(SqlBaseParser.DeallocateContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAnalyze(SqlBaseParser.AnalyzeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDiscard(SqlBaseParser.DiscardContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDeclare(SqlBaseParser.DeclareContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitFetch(SqlBaseParser.FetchContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitClose(SqlBaseParser.CloseContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropBlobTable(SqlBaseParser.DropBlobTableContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropTable(SqlBaseParser.DropTableContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropAlias(SqlBaseParser.DropAliasContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropRepository(SqlBaseParser.DropRepositoryContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropSnapshot(SqlBaseParser.DropSnapshotContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropFunction(SqlBaseParser.DropFunctionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropUser(SqlBaseParser.DropUserContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropView(SqlBaseParser.DropViewContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropAnalyzer(SqlBaseParser.DropAnalyzerContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropPublication(SqlBaseParser.DropPublicationContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropSubscription(SqlBaseParser.DropSubscriptionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAddColumn(SqlBaseParser.AddColumnContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDropCheckConstraint(SqlBaseParser.DropCheckConstraintContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterTableProperties(SqlBaseParser.AlterTablePropertiesContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterBlobTableProperties(SqlBaseParser.AlterBlobTablePropertiesContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterTableOpenClose(SqlBaseParser.AlterTableOpenCloseContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterTableRename(SqlBaseParser.AlterTableRenameContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterTableReroute(SqlBaseParser.AlterTableRerouteContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterClusterRerouteRetryFailed(SqlBaseParser.AlterClusterRerouteRetryFailedContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterClusterSwapTable(SqlBaseParser.AlterClusterSwapTableContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterClusterDecommissionNode(SqlBaseParser.AlterClusterDecommissionNodeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterClusterGCDanglingArtifacts(SqlBaseParser.AlterClusterGCDanglingArtifactsContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterUser(SqlBaseParser.AlterUserContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterPublication(SqlBaseParser.AlterPublicationContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterSubscription(SqlBaseParser.AlterSubscriptionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitQueryOptParens(SqlBaseParser.QueryOptParensContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitLimitClause(SqlBaseParser.LimitClauseContext ctx)
    {
//        limitClause
//        : LIMIT (limit=parameterOrInteger | ALL)
//                | FETCH (FIRST | NEXT) (limit=parameterOrInteger) (ROW | ROWS) ONLY
//    ;
        return (ctx.limit == null ? "" : " limit " + (ctx.ALL() == null ? "" : "all") + (ctx.limit == null ? "" : ctx.limit.accept(this))) +
                (ctx.FETCH() == null ? "" : "fetchTODO");
    }

    @Override
    public String visitOffsetClause(SqlBaseParser.OffsetClauseContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSetQuant(SqlBaseParser.SetQuantContext ctx)
    {
        return ctx.getText();
    }


    @Override
    public String visitValuesRelation(SqlBaseParser.ValuesRelationContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSelectAll(SqlBaseParser.SelectAllContext ctx)
    {
//            | qname DOT ASTERISK                                                             #selectAll
//            | ASTERISK
        return (ctx.qname() == null ? "*" : ctx.qname().accept(this) + ".*");
    }

    @Override
    public String visitReturning(SqlBaseParser.ReturningContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitFilter(SqlBaseParser.FilterContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSubqueryRelation(SqlBaseParser.SubqueryRelationContext ctx)
    {
        //     | OPEN_ROUND_BRACKET query CLOSE_ROUND_BRACKET                                   #subqueryRelation
        return "(" + ctx.query().accept(this) + ")";
    }

    @Override
    public String visitParenthesizedRelation(SqlBaseParser.ParenthesizedRelationContext ctx)
    {
        return "(" + ctx.relation().accept(this) + ")";
    }

    @Override
    public String visitTableWithPartition(SqlBaseParser.TableWithPartitionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitNamedFunctionArg(SqlBaseParser.NamedFunctionArgContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitFunctionArg(SqlBaseParser.FunctionArgContext ctx)
    {
        return (ctx.valueExpression() == null ? "" : ctx.valueExpression().accept(this)) +
                (ctx.namedFunctionArg() == null ? "" : ctx.namedFunctionArg().accept(this));
    }

    @Override
    public String visitTableFunction(SqlBaseParser.TableFunctionContext ctx)
    {
//        | qname OPEN_ROUND_BRACKET
//        functionArg? (COMMA functionArg)* CLOSE_ROUND_BRACKET                       #tableFunction
        return ctx.qname().accept(this) + "(" + ListIterate.collect(ctx.functionArg(), x -> x.accept(this)).makeString(", ") + ")";
    }

    @Override
    public String visitAliasedColumns(SqlBaseParser.AliasedColumnsContext ctx)
    {
//        aliasedColumns
//        : OPEN_ROUND_BRACKET ident (COMMA ident)* CLOSE_ROUND_BRACKET
//    ;
        return "(" + ListIterate.collect(ctx.ident(), x -> x.accept(this)).makeString(", ") + ")";
    }


    @Override
    public String visitLogicalNot(SqlBaseParser.LogicalNotContext ctx)
    {
        //    | NOT booleanExpression                                                          #logicalNot
        return "not " + ctx.booleanExpression().accept(this);
    }


    @Override
    public String visitMatch(SqlBaseParser.MatchContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitMulticolumns(SqlBaseParser.MulticolumnsContext ctx)
    {
        // OPEN_ROUND_BRACKET qnames CLOSE_ROUND_BRACKET EQ OPEN_ROUND_BRACKET qnames CLOSE_ROUND_BRACKET #multicolumns
        return "(" + ctx.left.accept(this) + ") = (" + ctx.right.accept(this) + ")";
    }

    @Override
    public String visitQuantifiedComparison(SqlBaseParser.QuantifiedComparisonContext ctx)
    {
        // cmpOp setCmpQuantifier primaryExpression                                       #quantifiedComparison
        return " " + ctx.cmpOp().accept(this) + " " + ctx.setCmpQuantifier().accept(this) + ctx.primaryExpression().accept(this);
    }

    @Override
    public String visitBetween(SqlBaseParser.BetweenContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitInList(SqlBaseParser.InListContext ctx)
    {
        // | NOT? IN OPEN_ROUND_BRACKET expr (COMMA expr)* CLOSE_ROUND_BRACKET              #inList
        return (ctx.NOT() == null ? "" : " not") + " in (" + ListIterate.collect(ctx.expr(), x -> x.accept(this)).makeString(", ") + ")";
    }

    @Override
    public String visitInSubquery(SqlBaseParser.InSubqueryContext ctx)
    {
        //     | NOT? IN subqueryExpression                                                     #inSubquery
        return (ctx.NOT() == null ? "" : " not") + " in " + ctx.subqueryExpression().accept(this);
    }

    @Override
    public String visitLike(SqlBaseParser.LikeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitArrayLike(SqlBaseParser.ArrayLikeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitNullPredicate(SqlBaseParser.NullPredicateContext ctx)
    {
        // | IS NOT? NULL                                                                   #nullPredicate
        return " is " + (ctx.NOT() == null ? "" : "not ") + "null";
    }

    @Override
    public String visitDistinctFrom(SqlBaseParser.DistinctFromContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitBitwiseBinary(SqlBaseParser.BitwiseBinaryContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitConcatenation(SqlBaseParser.ConcatenationContext ctx)
    {
        return ctx.left.accept(this) + " " + ctx.CONCAT().getText() + " " + ctx.right.accept(this);
    }

    @Override
    public String visitFromStringLiteralCast(SqlBaseParser.FromStringLiteralCastContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitArithmeticUnary(SqlBaseParser.ArithmeticUnaryContext ctx)
    {
//        | operator=(MINUS | PLUS) valueExpression                                        #arithmeticUnary
        return ctx.operator.getText() + ctx.valueExpression().accept(this);
    }

    @Override
    public String visitSubqueryExpressionDefault(SqlBaseParser.SubqueryExpressionDefaultContext ctx)
    {
        return ctx.subqueryExpression().accept(this);
    }

    @Override
    public String visitAtTimezone(SqlBaseParser.AtTimezoneContext ctx)
    {
//        | timestamp=primaryExpression AT TIME ZONE zone=primaryExpression                #atTimezone
        return ctx.timestamp.accept(this) + " at time zone " + ctx.zone.accept(this);
    }

    @Override
    public String visitSubscript(SqlBaseParser.SubscriptContext ctx)
    {
//        | value=primaryExpression
//        OPEN_SQUARE_BRACKET index=valueExpression CLOSE_SQUARE_BRACKET               #subscript
        return ctx.primaryExpression().accept(this) + "[" + ctx.index.accept(this) + "]";
    }

    @Override
    public String visitRecordSubscript(SqlBaseParser.RecordSubscriptContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitExplicitFunctionDefault(SqlBaseParser.ExplicitFunctionDefaultContext ctx)
    {
        return ctx.explicitFunction().accept(this);
    }

    @Override
    public String visitDoubleColonCast(SqlBaseParser.DoubleColonCastContext ctx)
    {
//            | primaryExpression CAST_OPERATOR dataType                                       #doubleColonCast
        return ctx.primaryExpression().accept(this) + "::" + ctx.dataType().accept(this);
    }

    @Override
    public String visitNestedExpression(SqlBaseParser.NestedExpressionContext ctx)
    {
        //OPEN_ROUND_BRACKET expr CLOSE_ROUND_BRACKET
        return "(" + ctx.expr().accept(this) + ")";
    }

    @Override
    public String visitArraySlice(SqlBaseParser.ArraySliceContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitExists(SqlBaseParser.ExistsContext ctx)
    {
        //     | EXISTS OPEN_ROUND_BRACKET query CLOSE_ROUND_BRACKET                            #exists
        return "exists (" + ctx.query().accept(this) + ")";
    }

    @Override
    public String visitEmptyArray(SqlBaseParser.EmptyArrayContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSpecialDateTimeFunction(SqlBaseParser.SpecialDateTimeFunctionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCurrentSchema(SqlBaseParser.CurrentSchemaContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCurrentUser(SqlBaseParser.CurrentUserContext ctx)
    {
        return ctx.getText();
    }

    @Override
    public String visitSessionUser(SqlBaseParser.SessionUserContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitLeft(SqlBaseParser.LeftContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitRight(SqlBaseParser.RightContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSubstring(SqlBaseParser.SubstringContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitTrim(SqlBaseParser.TrimContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitExtract(SqlBaseParser.ExtractContext ctx)
    {
//        | EXTRACT OPEN_ROUND_BRACKET stringLiteralOrIdentifier FROM
//        expr CLOSE_ROUND_BRACKET                                                     #extract
        return "extract(" + ctx.stringLiteralOrIdentifier().accept(this) + " from " + ctx.expr().accept(this) + ")";
    }

    @Override
    public String visitCast(SqlBaseParser.CastContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSimpleCase(SqlBaseParser.SimpleCaseContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSearchedCase(SqlBaseParser.SearchedCaseContext ctx)
    {
        //| CASE whenClause+ (ELSE elseExpr=expr)? END                                     #searchedCase
        return "case " + ListIterate.collect(ctx.whenClause(), w -> w.accept(this)).makeString(" ") + (ctx.elseExpr == null ? "" : " else " + ctx.elseExpr.accept(this)) + " end";
    }

    @Override
    public String visitIfCase(SqlBaseParser.IfCaseContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitArraySubquery(SqlBaseParser.ArraySubqueryContext ctx)
    {
        //ARRAY subqueryExpression                                                       #arraySubquery
        return ctx.ARRAY().getText() + " " + ctx.subqueryExpression().accept(this);
    }

    @Override
    public String visitSubqueryExpression(SqlBaseParser.SubqueryExpressionContext ctx)
    {
//        subqueryExpression
//        : OPEN_ROUND_BRACKET query CLOSE_ROUND_BRACKET
//        ;
        return "(" + ctx.query().accept(this) + ")";
    }

    @Override
    public String visitSimpleLiteral(SqlBaseParser.SimpleLiteralContext ctx)
    {
        return ctx.parameterOrSimpleLiteral().accept(this);
    }

    @Override
    public String visitArrayLiteral(SqlBaseParser.ArrayLiteralContext ctx)
    {
//        | ARRAY? OPEN_SQUARE_BRACKET (expr (COMMA expr)*)?
//            CLOSE_SQUARE_BRACKET                                                         #arrayLiteral
        return (ctx.ARRAY() == null ? "" : ctx.ARRAY().getText()) + "[" + ListIterate.collect(ctx.expr(), x -> x.accept(this)).makeString(", ") + "]";
    }

    @Override
    public String visitObjectLiteral(SqlBaseParser.ObjectLiteralContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitParameterOrInteger(SqlBaseParser.ParameterOrIntegerContext ctx)
    {
//        parameterOrInteger
//        : parameterExpr
//                | integerLiteral
//                | nullLiteral
//        ;
        return (ctx.parameterExpr() == null ? "" : ctx.parameterExpr().accept(this)) +
                (ctx.integerLiteral() == null ? "" : ctx.integerLiteral().accept(this)) +
                (ctx.nullLiteral() == null ? "" : ctx.nullLiteral().accept(this));
    }

    @Override
    public String visitParameterOrIdent(SqlBaseParser.ParameterOrIdentContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitParameterOrString(SqlBaseParser.ParameterOrStringContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitPositionalParameter(SqlBaseParser.PositionalParameterContext ctx)
    {
//            : DOLLAR integerLiteral                                                          #positionalParameter
        return "$" + ctx.integerLiteral().accept(this);
    }

    @Override
    public String visitParameterPlaceholder(SqlBaseParser.ParameterPlaceholderContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitNullLiteral(SqlBaseParser.NullLiteralContext ctx)
    {
        return "null";
    }

    @Override
    public String visitEscapedCharsStringLiteral(SqlBaseParser.EscapedCharsStringLiteralContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDollarQuotedStringLiteral(SqlBaseParser.DollarQuotedStringLiteralContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitBitString(SqlBaseParser.BitStringContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSubscriptSafe(SqlBaseParser.SubscriptSafeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSetCmpQuantifier(SqlBaseParser.SetCmpQuantifierContext ctx)
    {
//        setCmpQuantifier
//        : ANY | SOME | ALL
//        ;
        return (ctx.ANY() == null ? "" : "any") +
                (ctx.SOME() == null ? "" : "some") +
                (ctx.ALL() == null ? "" : "all");
    }

    @Override
    public String visitWhenClause(SqlBaseParser.WhenClauseContext ctx)
    {
//            : WHEN condition=expr THEN result=expr
        return "when " + ctx.condition.accept(this) + " then " + ctx.result.accept(this);
    }

    @Override
    public String visitNamedWindow(SqlBaseParser.NamedWindowContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitWithin(SqlBaseParser.WithinContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitWindowFrame(SqlBaseParser.WindowFrameContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitUnboundedFrame(SqlBaseParser.UnboundedFrameContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCurrentRowBound(SqlBaseParser.CurrentRowBoundContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitBoundedFrame(SqlBaseParser.BoundedFrameContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitQnames(SqlBaseParser.QnamesContext ctx)
    {
//        qnames
//        : qname (COMMA qname)*
//        ;
        return ListIterate.collect(ctx.qname(), x -> x.accept(this)).makeString(", ");
    }

    @Override
    public String visitQname(SqlBaseParser.QnameContext ctx)
    {
        return ctx.getText();
    }

    @Override
    public String visitIdents(SqlBaseParser.IdentsContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitUnquotedIdentifier(SqlBaseParser.UnquotedIdentifierContext ctx)
    {
//            : IDENTIFIER                        #unquotedIdentifier
//            | nonReserved                       #unquotedIdentifier

        return (ctx.IDENTIFIER() == null ? "" : ctx.IDENTIFIER().accept(this)) +
                (ctx.nonReserved() == null ? "" : ctx.nonReserved().accept(this));
    }

    @Override
    public String visitDigitIdentifier(SqlBaseParser.DigitIdentifierContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitQuotedIdentifier(SqlBaseParser.QuotedIdentifierContext ctx)
    {
        return ctx.getText();
    }

    @Override
    public String visitBackQuotedIdentifier(SqlBaseParser.BackQuotedIdentifierContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitStringLiteralOrIdentifier(SqlBaseParser.StringLiteralOrIdentifierContext ctx)
    {
//        stringLiteralOrIdentifier
//        : ident
//                | stringLiteral
//        ;
        return (ctx.ident() == null ? "" : ctx.ident().accept(this)) + (ctx.stringLiteral() == null ? "" : ctx.stringLiteral().accept(this));
    }

    @Override
    public String visitStringLiteralOrIdentifierOrQname(SqlBaseParser.StringLiteralOrIdentifierOrQnameContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitNumericLiteral(SqlBaseParser.NumericLiteralContext ctx)
    {
        return ctx.getText();
    }

    @Override
    public String visitIntervalLiteral(SqlBaseParser.IntervalLiteralContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitIntervalField(SqlBaseParser.IntervalFieldContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitBooleanLiteral(SqlBaseParser.BooleanLiteralContext ctx)
    {
        return ctx.getText();
    }

    @Override
    public String visitDecimalLiteral(SqlBaseParser.DecimalLiteralContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitIntegerLiteral(SqlBaseParser.IntegerLiteralContext ctx)
    {
        return ctx.getText();
    }

    @Override
    public String visitObjectKeyValue(SqlBaseParser.ObjectKeyValueContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitInsertSource(SqlBaseParser.InsertSourceContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitOnConflict(SqlBaseParser.OnConflictContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitConflictTarget(SqlBaseParser.ConflictTargetContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitValues(SqlBaseParser.ValuesContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitColumns(SqlBaseParser.ColumnsContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAssignment(SqlBaseParser.AssignmentContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreateTable(SqlBaseParser.CreateTableContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreateTableAs(SqlBaseParser.CreateTableAsContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreateBlobTable(SqlBaseParser.CreateBlobTableContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreateRepository(SqlBaseParser.CreateRepositoryContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreateSnapshot(SqlBaseParser.CreateSnapshotContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreateAnalyzer(SqlBaseParser.CreateAnalyzerContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreateFunction(SqlBaseParser.CreateFunctionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreateUser(SqlBaseParser.CreateUserContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreateView(SqlBaseParser.CreateViewContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreatePublication(SqlBaseParser.CreatePublicationContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCreateSubscription(SqlBaseParser.CreateSubscriptionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitFunctionArgument(SqlBaseParser.FunctionArgumentContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitTableOnly(SqlBaseParser.TableOnlyContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitTableWithPartitionDefault(SqlBaseParser.TableWithPartitionDefaultContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAlterSubscriptionMode(SqlBaseParser.AlterSubscriptionModeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitPartitionedByOrClusteredInto(SqlBaseParser.PartitionedByOrClusteredIntoContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitPartitionedBy(SqlBaseParser.PartitionedByContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitClusteredBy(SqlBaseParser.ClusteredByContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitBlobClusteredInto(SqlBaseParser.BlobClusteredIntoContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitColumnDefinitionDefault(SqlBaseParser.ColumnDefinitionDefaultContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitPrimaryKeyConstraint(SqlBaseParser.PrimaryKeyConstraintContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitIndexDefinition(SqlBaseParser.IndexDefinitionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitTableCheckConstraint(SqlBaseParser.TableCheckConstraintContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitColumnDefinition(SqlBaseParser.ColumnDefinitionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAddColumnDefinition(SqlBaseParser.AddColumnDefinitionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitRerouteMoveShard(SqlBaseParser.RerouteMoveShardContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitRerouteAllocateReplicaShard(SqlBaseParser.RerouteAllocateReplicaShardContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitReroutePromoteReplica(SqlBaseParser.ReroutePromoteReplicaContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitRerouteCancelShard(SqlBaseParser.RerouteCancelShardContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitObjectDataType(SqlBaseParser.ObjectDataTypeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitMaybeParametrizedDataType(SqlBaseParser.MaybeParametrizedDataTypeContext ctx)
    {
//            : baseDataType
//            (OPEN_ROUND_BRACKET integerLiteral (COMMA integerLiteral )* CLOSE_ROUND_BRACKET)?  #maybeParametrizedDataType
        return ctx.baseDataType().accept(this) + (ctx.OPEN_ROUND_BRACKET() == null ? "" : "(" + ListIterate.collect(ctx.integerLiteral(), x -> x.accept(this)).makeString(", ") + ")");
    }

    @Override
    public String visitArrayDataType(SqlBaseParser.ArrayDataTypeContext ctx)
    {
//            | ARRAY OPEN_ROUND_BRACKET dataType CLOSE_ROUND_BRACKET                                #arrayDataType
//            | dataType EMPTY_SQUARE_BRACKET                                                        #arrayDataType

        return (ctx.ARRAY() == null ? "" : "array(" + ctx.dataType().accept(this) + ")") +
                (ctx.EMPTY_SQUARE_BRACKET() == null ? "" : ctx.dataType().accept(this) + "[]");
    }

    @Override
    public String visitDefinedDataTypeDefault(SqlBaseParser.DefinedDataTypeDefaultContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitIdentDataType(SqlBaseParser.IdentDataTypeContext ctx)
    {
        return ctx.ident().accept(this);

    }

    @Override
    public String visitDefinedDataType(SqlBaseParser.DefinedDataTypeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitObjectTypeDefinition(SqlBaseParser.ObjectTypeDefinitionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitColumnConstraintPrimaryKey(SqlBaseParser.ColumnConstraintPrimaryKeyContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitColumnConstraintNotNull(SqlBaseParser.ColumnConstraintNotNullContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitColumnIndexConstraint(SqlBaseParser.ColumnIndexConstraintContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitColumnIndexOff(SqlBaseParser.ColumnIndexOffContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitColumnStorageDefinition(SqlBaseParser.ColumnStorageDefinitionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitColumnCheckConstraint(SqlBaseParser.ColumnCheckConstraintContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCheckConstraint(SqlBaseParser.CheckConstraintContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitWithGenericProperties(SqlBaseParser.WithGenericPropertiesContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitGenericProperties(SqlBaseParser.GenericPropertiesContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitGenericProperty(SqlBaseParser.GenericPropertyContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitMatchPredicateIdents(SqlBaseParser.MatchPredicateIdentsContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitMatchPredicateIdent(SqlBaseParser.MatchPredicateIdentContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitAnalyzerElement(SqlBaseParser.AnalyzerElementContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitTokenizer(SqlBaseParser.TokenizerContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitTokenFilters(SqlBaseParser.TokenFiltersContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitCharFilters(SqlBaseParser.CharFiltersContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitNamedProperties(SqlBaseParser.NamedPropertiesContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitTableWithPartitions(SqlBaseParser.TableWithPartitionsContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSetGlobalAssignment(SqlBaseParser.SetGlobalAssignmentContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitSetExpr(SqlBaseParser.SetExprContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitOn(SqlBaseParser.OnContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitClazz(SqlBaseParser.ClazzContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitTransactionMode(SqlBaseParser.TransactionModeContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitIsolationLevel(SqlBaseParser.IsolationLevelContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDirection(SqlBaseParser.DirectionContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitDeclareCursorParams(SqlBaseParser.DeclareCursorParamsContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitNonReserved(SqlBaseParser.NonReservedContext ctx)
    {
        return ctx.getText();
    }

    @Override
    public String visit(ParseTree parseTree)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitChildren(RuleNode ruleNode)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitTerminal(TerminalNode terminalNode)
    {
        return terminalNode.getText();
    }

    @Override
    public String visitErrorNode(ErrorNode errorNode)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitBitwiseShift(SqlBaseParser.BitwiseShiftContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonBinary(SqlBaseParser.JsonBinaryContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonExtract(SqlBaseParser.JsonExtractContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonExtractText(SqlBaseParser.JsonExtractTextContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonPathExtract(SqlBaseParser.JsonPathExtractContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonPathExtractText(SqlBaseParser.JsonPathExtractTextContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonbContainRight(SqlBaseParser.JsonbContainRightContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonbContainLeft(SqlBaseParser.JsonbContainLeftContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonbContainTopKey(SqlBaseParser.JsonbContainTopKeyContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonbContainAnyTopKey(SqlBaseParser.JsonbContainAnyTopKeyContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonbContainAllTopKey(SqlBaseParser.JsonbContainAllTopKeyContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonbConcat(SqlBaseParser.JsonbConcatContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonbDelete(SqlBaseParser.JsonbDeleteContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonbPathDelete(SqlBaseParser.JsonbPathDeleteContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonbPathContainAnyValue(SqlBaseParser.JsonbPathContainAnyValueContext ctx)
    {
        throw new RuntimeException("");
    }

    @Override
    public String visitJsonbPathPredicateCheck(SqlBaseParser.JsonbPathPredicateCheckContext ctx)
    {
        throw new RuntimeException("");
    }
}


