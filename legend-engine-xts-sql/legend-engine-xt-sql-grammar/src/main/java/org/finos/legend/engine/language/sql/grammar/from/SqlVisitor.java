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
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseLexer;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParser.*;
import org.finos.legend.engine.language.sql.grammar.from.antlr4.SqlBaseParserBaseVisitor;
import org.finos.legend.engine.protocol.sql.metamodel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SqlVisitor extends SqlBaseParserBaseVisitor<Node>
{
    private static final Pattern LITERAL_VALUE_PATTERN = Pattern.compile("(([\\+-]?[0-9]+)\\s([year|years|month|months|week|weeks|day|days|hour|hours|minute|minutes|second|seconds]+))+");

    private long positionalIndex = 1;

    private SqlVisitor()
    {
        //here to ensure static method below used, as not safe to reuse instance
    }

    public static Node process(ParserRuleContext ctx)
    {
        return ctx.accept(new SqlVisitor());
    }

    @Override
    public Node visitSingleStatement(SqlBaseParser.SingleStatementContext context)
    {
        return context.statement().accept(this);
    }

    @Override
    public Node visitStatements(StatementsContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitSingleExpression(SqlBaseParser.SingleExpressionContext context)
    {
        return visit(context.expr());
    }

    //  Statements

    @Override
    public Node visitBegin(SqlBaseParser.BeginContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitStartTransaction(SqlBaseParser.StartTransactionContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAnalyze(SqlBaseParser.AnalyzeContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitDeclare(DeclareContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitFetch(FetchContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitClose(CloseContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitDiscard(DiscardContext ctx)
    {
        return unsupported();
    }

    //TODO re-consider where to do parsing
    @Override
    public Node visitIntervalLiteral(SqlBaseParser.IntervalLiteralContext context)
    {
        IntervalLiteral intervalLiteral = new IntervalLiteral();
        StringLiteral stringLiteral = (StringLiteral) visitStringLiteral(context.stringLiteral());
        String value = stringLiteral.value.toLowerCase();

        Map<String, Long> matches = parseIntervalValue(value);

        intervalLiteral.years = matches.get("year");
        intervalLiteral.months = matches.get("month");
        intervalLiteral.weeks = matches.get("week");
        intervalLiteral.days = matches.get("day");
        intervalLiteral.hours = matches.get("hour");
        intervalLiteral.minutes = matches.get("minute");
        intervalLiteral.seconds = matches.get("second");

        return intervalLiteral;
    }

    private Map<String, Long> parseIntervalValue(String value)
    {
        Map<String, Long> matches = UnifiedMap.newMap();

        Matcher matcher = LITERAL_VALUE_PATTERN.matcher(value);
        while (matcher.find())
        {
            Long l = Long.parseLong(matcher.group(2));
            String period = matcher.group(3).replaceAll("s$", "");
            matches.put(period, l);
        }
        return matches;
    }

    @Override
    public Node visitCommit(SqlBaseParser.CommitContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitOptimize(SqlBaseParser.OptimizeContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCreateTable(SqlBaseParser.CreateTableContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCreateTableAs(SqlBaseParser.CreateTableAsContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterClusterSwapTable(SqlBaseParser.AlterClusterSwapTableContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterClusterGCDanglingArtifacts(SqlBaseParser.AlterClusterGCDanglingArtifactsContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterClusterDecommissionNode(SqlBaseParser.AlterClusterDecommissionNodeContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitCreateView(SqlBaseParser.CreateViewContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitDropView(SqlBaseParser.DropViewContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitCreateBlobTable(SqlBaseParser.CreateBlobTableContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCreateRepository(SqlBaseParser.CreateRepositoryContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCreateSnapshot(SqlBaseParser.CreateSnapshotContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCreateAnalyzer(SqlBaseParser.CreateAnalyzerContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitDropAnalyzer(SqlBaseParser.DropAnalyzerContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitCreateUser(SqlBaseParser.CreateUserContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitDropUser(SqlBaseParser.DropUserContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitGrantPrivilege(SqlBaseParser.GrantPrivilegeContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitDenyPrivilege(SqlBaseParser.DenyPrivilegeContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitRevokePrivilege(SqlBaseParser.RevokePrivilegeContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCharFilters(SqlBaseParser.CharFiltersContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitTokenFilters(SqlBaseParser.TokenFiltersContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitTokenizer(SqlBaseParser.TokenizerContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitNamedProperties(SqlBaseParser.NamedPropertiesContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitRestore(SqlBaseParser.RestoreContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitShowCreateTable(SqlBaseParser.ShowCreateTableContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitShowTransaction(SqlBaseParser.ShowTransactionContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitShowSessionParameter(SqlBaseParser.ShowSessionParameterContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitDropTable(SqlBaseParser.DropTableContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitDropRepository(SqlBaseParser.DropRepositoryContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitDropBlobTable(SqlBaseParser.DropBlobTableContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitDropSnapshot(SqlBaseParser.DropSnapshotContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCopyFrom(SqlBaseParser.CopyFromContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCopyTo(SqlBaseParser.CopyToContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitInsert(SqlBaseParser.InsertContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitValues(SqlBaseParser.ValuesContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitDelete(SqlBaseParser.DeleteContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitUpdate(SqlBaseParser.UpdateContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitSet(SqlBaseParser.SetContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitSetGlobal(SqlBaseParser.SetGlobalContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitSetLicense(SqlBaseParser.SetLicenseContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitSetTimeZone(SqlBaseParser.SetTimeZoneContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitSetTransaction(SetTransactionContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitResetGlobal(SqlBaseParser.ResetGlobalContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitSetSessionAuthorization(SqlBaseParser.SetSessionAuthorizationContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitResetSessionAuthorization(SqlBaseParser.ResetSessionAuthorizationContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitKill(SqlBaseParser.KillContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitDeallocate(SqlBaseParser.DeallocateContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitExplain(SqlBaseParser.ExplainContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitShowTables(SqlBaseParser.ShowTablesContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitShowSchemas(SqlBaseParser.ShowSchemasContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitShowColumns(SqlBaseParser.ShowColumnsContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitRefreshTable(SqlBaseParser.RefreshTableContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitTableOnly(SqlBaseParser.TableOnlyContext context)
    {
        Table table = new Table();
        table.name = getQualifiedName(context.qname());

        return table;
    }

    @Override
    public Node visitTableWithPartition(SqlBaseParser.TableWithPartitionContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCreateFunction(SqlBaseParser.CreateFunctionContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitDropFunction(SqlBaseParser.DropFunctionContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCreatePublication(SqlBaseParser.CreatePublicationContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitDropPublication(SqlBaseParser.DropPublicationContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterPublication(SqlBaseParser.AlterPublicationContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitCreateSubscription(SqlBaseParser.CreateSubscriptionContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitDropSubscription(SqlBaseParser.DropSubscriptionContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterSubscription(SqlBaseParser.AlterSubscriptionContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitNamedQuery(SqlBaseParser.NamedQueryContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitWith(SqlBaseParser.WithContext ctx)
    {
        return unsupported();
    }

    // Column / Table definition

    @Override
    public Node visitColumnDefinition(SqlBaseParser.ColumnDefinitionContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitColumnConstraintPrimaryKey(SqlBaseParser.ColumnConstraintPrimaryKeyContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitColumnConstraintNotNull(SqlBaseParser.ColumnConstraintNotNullContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitPrimaryKeyConstraint(SqlBaseParser.PrimaryKeyConstraintContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitTableCheckConstraint(SqlBaseParser.TableCheckConstraintContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitColumnCheckConstraint(SqlBaseParser.ColumnCheckConstraintContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitDropCheckConstraint(SqlBaseParser.DropCheckConstraintContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitColumnIndexOff(SqlBaseParser.ColumnIndexOffContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitColumnIndexConstraint(SqlBaseParser.ColumnIndexConstraintContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitIndexDefinition(SqlBaseParser.IndexDefinitionContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitColumnStorageDefinition(SqlBaseParser.ColumnStorageDefinitionContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitPartitionedBy(SqlBaseParser.PartitionedByContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitClusteredBy(SqlBaseParser.ClusteredByContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitBlobClusteredInto(SqlBaseParser.BlobClusteredIntoContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitFunctionArgument(SqlBaseParser.FunctionArgumentContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitRerouteMoveShard(SqlBaseParser.RerouteMoveShardContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitReroutePromoteReplica(SqlBaseParser.ReroutePromoteReplicaContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitRerouteAllocateReplicaShard(SqlBaseParser.RerouteAllocateReplicaShardContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitRerouteCancelShard(SqlBaseParser.RerouteCancelShardContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitWithGenericProperties(SqlBaseParser.WithGenericPropertiesContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitGenericProperties(SqlBaseParser.GenericPropertiesContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitGenericProperty(SqlBaseParser.GenericPropertyContext context)
    {
        return unsupported();
    }

    // Amending tables

    @Override
    public Node visitAlterTableProperties(SqlBaseParser.AlterTablePropertiesContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterBlobTableProperties(SqlBaseParser.AlterBlobTablePropertiesContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAddColumn(SqlBaseParser.AddColumnContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAddColumnDefinition(SqlBaseParser.AddColumnDefinitionContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterTableOpenClose(SqlBaseParser.AlterTableOpenCloseContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterTableRename(SqlBaseParser.AlterTableRenameContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterTableReroute(SqlBaseParser.AlterTableRerouteContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterClusterRerouteRetryFailed(SqlBaseParser.AlterClusterRerouteRetryFailedContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAlterUser(SqlBaseParser.AlterUserContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitSetGlobalAssignment(SqlBaseParser.SetGlobalAssignmentContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitAssignment(SqlBaseParser.AssignmentContext context)
    {
        return unsupported();
    }

    // Query specification


    @Override
    public Node visitQuery(SqlBaseParser.QueryContext context)
    {
        if (context.with() == null)
        {
            return visit(context.queryNoWith());
        }

        return unsupported();
    }

    @Override
    public Node visitQueryNoWith(SqlBaseParser.QueryNoWithContext context)
    {
        QueryBody term = (QueryBody) visit(context.queryTerm());

        Query query = new Query();
        query.queryBody = term;

        List<SortItem> orderBy = visitCollection(context.sortItem(), SortItem.class);
        Optional<Expression> limit = visitIfPresent(context.limitClause(), Expression.class);
        Optional<Expression> offset = visitIfPresent(context.offsetClause(), Expression.class);

        if (term instanceof QuerySpecification)
        {
            QuerySpecification body = (QuerySpecification) term;
            // When we have a simple query specification
            // followed by order by limit, fold the order by and limit
            // clauses into the query specification (analyzer/planner
            // expects this structure to resolve references with respect
            // to columns defined in the query specification)
            body.orderBy = orderBy;
            body.limit = limit.orElse(null);
            body.offset = offset.orElse(null);

            return query;
        }

        query.orderBy = orderBy;
        query.limit = limit.orElse(null);
        query.offset = offset.orElse(null);

        return query;
    }

    @Override
    public Node visitLimitClause(SqlBaseParser.LimitClauseContext ctx)
    {
        return ctx.limit != null ? visit(ctx.limit) : null;
    }

    @Override
    public Node visitOffsetClause(SqlBaseParser.OffsetClauseContext ctx)
    {
        return ctx.offset != null ? visit(ctx.offset) : null;
    }

    @Override
    public Node visitDefaultQuerySpec(SqlBaseParser.DefaultQuerySpecContext context)
    {
        List<SelectItem> selectItems = visitCollection(context.selectItem(), SelectItem.class);
        Select select = new Select();
        select.selectItems = selectItems;
        select.distinct = isDistinct(context.setQuant());

        List<Relation> relations = visitCollection(context.relation(), Relation.class);

        QuerySpecification specification = new QuerySpecification();
        specification.select = select;
        specification.from = relations;
        specification.groupBy = visitCollection(context.expr(), Expression.class);
        specification.where = visitIfPresent(context.where(), Expression.class).orElse(null);
        specification.having = visitIfPresent(context.having, Expression.class).orElse(null);

        return specification;
    }

    @Override
    public Node visitValuesRelation(SqlBaseParser.ValuesRelationContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitWhere(SqlBaseParser.WhereContext context)
    {
        return visit(context.condition);
    }

    @Override
    public Node visitSortItem(SqlBaseParser.SortItemContext context)
    {
        SortItem sortItem = new SortItem();
        sortItem.sortKey = (Expression) context.expr().accept(this);
        sortItem.ordering = getOrderingType(context.ordering);
        sortItem.nullOrdering = getNullOrderingType(context.nullOrdering);

        return sortItem;
    }

    @Override
    public Node visitSetOperation(SqlBaseParser.SetOperationContext context)
    {
        switch (context.operator.getType())
        {
            case SqlBaseLexer.UNION:
                QueryBody left = (QueryBody) visit(context.left);
                QueryBody right = (QueryBody) visit(context.right);
                boolean isDistinct = context.setQuant() == null || context.setQuant().ALL() == null;
                Union union = new Union();
                union.left = left;
                union.right = right;
                union.distinct = isDistinct;

                return union;
            default:
                throw new IllegalArgumentException("Unsupported set operation: " + context.operator.getText());
        }
    }

    @Override
    public Node visitSelectAll(SqlBaseParser.SelectAllContext context)
    {
        if (context.qname() != null)
        {
            AllColumns allColumns = new AllColumns();
            allColumns.prefix = qualifiedNameToString(getQualifiedName(context.qname()));

            return allColumns;
        }
        return new AllColumns();
    }

    @Override
    public Node visitSelectSingle(SqlBaseParser.SelectSingleContext context)
    {
        SingleColumn column = new SingleColumn();
        column.expression = (Expression) visit(context.expr());
        column.alias = getIdentText(context.ident());

        return column;
    }

    @Override
    public Node visitArraySubquery(SqlBaseParser.ArraySubqueryContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitUnquotedIdentifier(SqlBaseParser.UnquotedIdentifierContext context)
    {
        StringLiteral stringLiteral = new StringLiteral();
        stringLiteral.value = context.getText();

        return stringLiteral;
    }

    @Override
    public Node visitQuotedIdentifier(SqlBaseParser.QuotedIdentifierContext context)
    {
        String token = context.getText();
        String identifier = token.substring(1, token.length() - 1)
                .replace("\"\"", "\"");

        StringLiteral stringLiteral = new StringLiteral();
        stringLiteral.value = identifier;

        return stringLiteral;
    }

    private String getIdentText(SqlBaseParser.IdentContext ident)
    {
        if (ident != null)
        {
            StringLiteral literal = (StringLiteral) ident.accept(this);
            return literal.value;
        }
        return null;
    }

    @Override
    public Node visitTableName(SqlBaseParser.TableNameContext ctx)
    {
        Table table = new Table();
        table.name = getQualifiedName(ctx.qname());
        return table;
    }

    @Override
    public Node visitNamedFunctionArg(SqlBaseParser.NamedFunctionArgContext context)
    {
        NamedArgumentExpression namedArgumentExpression = new NamedArgumentExpression();
        namedArgumentExpression.name = getIdentText(context.name);
        namedArgumentExpression.expression = (Expression) context.valueExpression().accept(this);

        return namedArgumentExpression;
    }

    @Override
    public Node visitTableFunction(SqlBaseParser.TableFunctionContext context)
    {
        QualifiedName qualifiedName = getQualifiedName(context.qname());
        List<Expression> arguments = visitCollection(context.functionArg(), Expression.class);
        FunctionCall functionCall = new FunctionCall();
        functionCall.name = qualifiedName;
        functionCall.arguments = arguments;
        TableFunction tableFunction = new TableFunction();
        tableFunction.functionCall = functionCall;
        return tableFunction;
    }

    // Boolean expressions

    @Override
    public Node visitLogicalNot(SqlBaseParser.LogicalNotContext context)
    {
        NotExpression notExpression = new NotExpression();
        notExpression.value = (Expression) visit(context.booleanExpression());

        return notExpression;
    }

    @Override
    public Node visitLogicalBinary(SqlBaseParser.LogicalBinaryContext context)
    {
        LogicalBinaryExpression logicalBinaryExpression = new LogicalBinaryExpression();
        logicalBinaryExpression.type = getLogicalBinaryOperator(context.operator);
        logicalBinaryExpression.left = (Expression) visit(context.left);
        logicalBinaryExpression.right = (Expression) visit(context.right);

        return logicalBinaryExpression;
    }

    // From clause

    @Override
    public Node visitJoinRelation(SqlBaseParser.JoinRelationContext ctx)
    {
        Join join = new Join();
        join.left = (Relation) visit(ctx.left);

        if (ctx.CROSS() != null)
        {
            join.right = (Relation) visit(ctx.right);
            join.type = JoinType.CROSS;
            return join;
        }

        if (ctx.NATURAL() != null)
        {
            join.right = (Relation) visit(ctx.right);
            join.criteria = new NaturalJoin();
        }
        else
        {
            join.right = (Relation) visit(ctx.rightRelation);
            if (ctx.joinCriteria().ON() != null)
            {
                JoinOn joinOn = new JoinOn();
                joinOn.expression = (Expression) visit(ctx.joinCriteria().booleanExpression());
                join.criteria = joinOn;
            }
            else if (ctx.joinCriteria().USING() != null)
            {
                List<String> columns = identsToStrings(ctx.joinCriteria().ident());
                JoinUsing joinUsing = new JoinUsing();
                joinUsing.columns = columns;
                join.criteria = joinUsing;
            }
            else
            {
                throw new IllegalArgumentException("Unsupported join criteria");
            }
        }

        join.type = getJoinType(ctx.joinType());

        return join;
    }

    private static JoinType getJoinType(SqlBaseParser.JoinTypeContext joinTypeContext)
    {
        if (joinTypeContext.LEFT() != null)
        {
            return JoinType.LEFT;
        }
        else if (joinTypeContext.RIGHT() != null)
        {
            return JoinType.RIGHT;
        }
        else if (joinTypeContext.FULL() != null)
        {
            return JoinType.FULL;
        }
        else
        {
            return JoinType.INNER;
        }
    }

    @Override
    public Node visitAliasedRelation(SqlBaseParser.AliasedRelationContext context)
    {
        Relation child = (Relation) visit(context.relationPrimary());

        if (context.ident() == null)
        {
            return child;
        }
        AliasedRelation aliasedRelation = new AliasedRelation();
        aliasedRelation.relation = child;
        aliasedRelation.alias = getIdentText(context.ident());
        aliasedRelation.columnNames = getColumnAliases(context.aliasedColumns());

        return aliasedRelation;
    }

    @Override
    public Node visitSubqueryRelation(SqlBaseParser.SubqueryRelationContext context)
    {
        TableSubquery tableSubquery = new TableSubquery();
        tableSubquery.query = (Query) visit(context.query());

        return tableSubquery;
    }

    @Override
    public Node visitParenthesizedRelation(SqlBaseParser.ParenthesizedRelationContext context)
    {
        return visit(context.relation());
    }

    // Predicates

    @Override
    public Node visitPredicated(SqlBaseParser.PredicatedContext context)
    {
        if (context.predicate() != null)
        {
            return visit(context.predicate());
        }
        return visit(context.valueExpression);
    }

    @Override
    public Node visitComparison(SqlBaseParser.ComparisonContext context)
    {
        ComparisonExpression comparisonExpression = new ComparisonExpression();
        comparisonExpression.left = (Expression) visit(context.value);
        comparisonExpression.right = (Expression) visit(context.right);
        comparisonExpression.operator = getComparisonOperator(((TerminalNode) context.cmpOp().getChild(0)).getSymbol());

        return comparisonExpression;
    }

    @Override
    public Node visitDistinctFrom(SqlBaseParser.DistinctFromContext context)
    {
        ComparisonOperator operator = context.NOT() != null ? ComparisonOperator.IS_NOT_DISTINCT_FROM : ComparisonOperator.IS_DISTINCT_FROM;

        ComparisonExpression expression = new ComparisonExpression();
        expression.operator = operator;
        expression.left = (Expression) visit(context.value);
        expression.right = (Expression) visit(context.right);

        return expression;
    }

    @Override
    public Node visitBetween(SqlBaseParser.BetweenContext context)
    {
        BetweenPredicate predicate = new BetweenPredicate();

        predicate.min = (Expression) visit(context.lower);
        predicate.max = (Expression) visit(context.upper);
        predicate.value = (Expression) visit(context.value);

        return predicate;
    }

    @Override
    public Node visitNullPredicate(SqlBaseParser.NullPredicateContext context)
    {
        Expression value = (Expression) visit(context.value);

        if (context.NOT() != null)
        {
            IsNotNullPredicate isNotNullPredicate = new IsNotNullPredicate();
            isNotNullPredicate.value = value;
            return isNotNullPredicate;
        }
        IsNullPredicate isNullPredicate = new IsNullPredicate();
        isNullPredicate.value = value;
        return isNullPredicate;
    }

    public Node visitLike(SqlBaseParser.LikeContext context)
    {
        LikePredicate like = new LikePredicate();
        like.escape = visitOptionalContext(context.escape, Expression.class);
        like.ignoreCase = context.LIKE() == null && context.ILIKE() != null;
        like.value = (Expression) visit(context.value);
        like.pattern = (Expression) visit(context.pattern);

        if (context.NOT() != null)
        {
            NotExpression not = new NotExpression();
            not.value = like;
            return not;
        }

        return like;
    }

    @Override
    public Node visitArrayLike(SqlBaseParser.ArrayLikeContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitInList(SqlBaseParser.InListContext context)
    {
        InListExpression inList = new InListExpression();
        inList.values = visitCollection(context.expr(), Expression.class);

        InPredicate in = new InPredicate();
        in.value = (Expression) visit(context.value);
        in.valueList = inList;

        if (context.NOT() != null)
        {
            NotExpression not = new NotExpression();
            not.value = in;
            return not;
        }
        return in;
    }

    @Override
    public Node visitInSubquery(SqlBaseParser.InSubqueryContext context)
    {
        InPredicate in = new InPredicate();
        in.value = (Expression) visit(context.value);
        in.valueList = (Expression) visit(context.subqueryExpression());

        if (context.NOT() != null)
        {
            NotExpression not = new NotExpression();
            not.value = in;
            return not;
        }
        return in;
    }

    @Override
    public Node visitExists(SqlBaseParser.ExistsContext context)
    {
//        return new ExistsPredicate((Query) visit(context.query()));
        return unsupported();
    }

    @Override
    public Node visitQuantifiedComparison(SqlBaseParser.QuantifiedComparisonContext context)
    {
        //TODO
        return unsupported();
    }

    @Override
    public Node visitMatch(SqlBaseParser.MatchContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitMatchPredicateIdent(SqlBaseParser.MatchPredicateIdentContext context)
    {
        return unsupported();
    }

    // Value expressions

    @Override
    public Node visitArithmeticUnary(SqlBaseParser.ArithmeticUnaryContext context)
    {
        switch (context.operator.getType())
        {
            case SqlBaseLexer.MINUS:
                NegativeExpression negativeExpression = new NegativeExpression();
                negativeExpression.value = (Expression) visit(context.valueExpression());
                return negativeExpression;
            case SqlBaseLexer.PLUS:
                return visit(context.valueExpression());
            default:
                throw new UnsupportedOperationException("Unsupported sign: " + context.operator.getText());
        }
    }

    @Override
    public Node visitArithmeticBinary(SqlBaseParser.ArithmeticBinaryContext context)
    {
        ArithmeticExpression arithmeticExpression = new ArithmeticExpression();
        arithmeticExpression.type = getArithmeticBinaryOperator(context.operator);
        arithmeticExpression.left = (Expression) visit(context.left);
        arithmeticExpression.right = (Expression) visit(context.right);

        return arithmeticExpression;
    }

    @Override
    public Node visitConcatenation(SqlBaseParser.ConcatenationContext context)
    {
        FunctionCall concat = new FunctionCall();
        concat.name = qualifiedName("concat");
        concat.arguments = FastList.newListWith(
                (Expression) visit(context.left),
                (Expression) visit(context.right)
        );

        return concat;
    }

    @Override
    public Node visitOver(SqlBaseParser.OverContext context)
    {
        return visit(context.windowDefinition());
    }

    @Override
    public Node visitWithin(WithinContext ctx)
    {
        Group group = new Group();
        group.orderBy = (SortItem) visit(ctx.sortItem());

        return group;
    }

    @Override
    public Node visitWindowDefinition(SqlBaseParser.WindowDefinitionContext context)
    {
        Window window = new Window();
        window.windowRef = getIdentText(context.windowRef);
        window.partitions = visitCollection(context.partition, Expression.class);
        window.orderBy = visitCollection(context.sortItem(), SortItem.class);
        window.windowFrame = visitIfPresent(context.windowFrame(), WindowFrame.class).orElse(null);

        return window;
    }

    @Override
    public Node visitWindowFrame(SqlBaseParser.WindowFrameContext ctx)
    {
        WindowFrame frame = new WindowFrame();
        frame.mode = getFrameType(ctx.frameType);
        frame.start = (FrameBound) visit(ctx.start);
        frame.end = (FrameBound) visit(ctx.end);

        return frame;
    }

    @Override
    public Node visitUnboundedFrame(SqlBaseParser.UnboundedFrameContext context)
    {
        FrameBound frameBound = new FrameBound();
        frameBound.type = getUnboundedFrameBoundType(context.boundType);

        return frameBound;
    }

    @Override
    public Node visitBoundedFrame(SqlBaseParser.BoundedFrameContext context)
    {
        FrameBound frameBound = new FrameBound();
        frameBound.type = getBoundedFrameBoundType(context.boundType);
        frameBound.value = (Expression) visit(context.expr());

        return frameBound;
    }

    @Override
    public Node visitCurrentRowBound(SqlBaseParser.CurrentRowBoundContext context)
    {
        FrameBound frameBound = new FrameBound();
        frameBound.type = FrameBoundType.CURRENT_ROW;

        return frameBound;
    }

    @Override
    public Node visitDoubleColonCast(SqlBaseParser.DoubleColonCastContext context)
    {
        Cast cast = new Cast();
        cast.type = (ColumnType) visit(context.dataType());
        cast.expression = (Expression) visit(context.primaryExpression());

        return cast;
    }

    @Override
    public Node visitFromStringLiteralCast(SqlBaseParser.FromStringLiteralCastContext context)
    {
        ColumnType targetType = (ColumnType) visit(context.dataType());

        Cast cast = new Cast();
        cast.type = targetType;
        cast.expression = (Expression) visit(context.stringLiteral());

        return cast;
    }

    // Primary expressions

    @Override
    public Node visitCast(SqlBaseParser.CastContext context)
    {
        if (context.TRY_CAST() != null)
        {
            return unsupported();
        }
        else
        {
            Cast cast = new Cast();
            cast.type = (ColumnType) visit(context.dataType());
            cast.expression = (Expression) visit(context.expr());

            return cast;
        }
    }

    @Override
    public Node visitSpecialDateTimeFunction(SqlBaseParser.SpecialDateTimeFunctionContext context)
    {
        CurrentTime currentTime = new CurrentTime();
        currentTime.type = getDateTimeFunctionType(context.name);

        if (context.precision != null)
        {
            currentTime.precision = Long.parseLong(context.precision.getText());
        }

        return currentTime;
    }

    @Override
    public Node visitExtract(SqlBaseParser.ExtractContext context)
    {
        StringLiteral field = (StringLiteral) visit(context.stringLiteralOrIdentifier());

        Extract extract = new Extract();
        extract.expression = (Expression) visit(context.expr());
        extract.field = ExtractField.valueOf(field.value.toUpperCase());

        return extract;
    }

    @Override
    public Node visitSubstring(SqlBaseParser.SubstringContext context)
    {
        QualifiedName qualifiedName = qualifiedName("substring");

        FunctionCall functionCall = new FunctionCall();
        functionCall.name = qualifiedName;
        functionCall.arguments = visitCollection(context.expr(), Expression.class);

        return functionCall;
    }

    @Override
    public Node visitAtTimezone(SqlBaseParser.AtTimezoneContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitLeft(SqlBaseParser.LeftContext context)
    {
        return functionCall("left", context.strOrColName, context.len);
    }

    @Override
    public Node visitRight(SqlBaseParser.RightContext context)
    {
        return functionCall("right", context.strOrColName, context.len);
    }

    @Override
    public Node visitTrim(SqlBaseParser.TrimContext ctx)
    {
        Trim trim = new Trim();
        trim.value = (Expression) visit(ctx.target);
        trim.characters = visitIfPresent(ctx.charsToTrim, Expression.class).orElse(null);
        trim.mode = getTrimMode(ctx.trimMode);

        return trim;
    }

    @Override
    public Node visitCurrentSchema(SqlBaseParser.CurrentSchemaContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitCurrentUser(SqlBaseParser.CurrentUserContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitSessionUser(SqlBaseParser.SessionUserContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitNestedExpression(SqlBaseParser.NestedExpressionContext context)
    {
        return visit(context.expr());
    }

    @Override
    public Node visitSubqueryExpression(SqlBaseParser.SubqueryExpressionContext context)
    {
//        return new SubqueryExpression((Query) visit(context.query()));
        return unsupported();
    }

    @Override
    public Node visitArraySlice(SqlBaseParser.ArraySliceContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitDereference(SqlBaseParser.DereferenceContext context)
    {
        QualifiedName qualifiedName = new QualifiedName();
        qualifiedName.parts = identsToStrings(context.ident());
        QualifiedNameReference reference = new QualifiedNameReference();
        reference.name = qualifiedName;

        return reference;
    }

    @Override
    public Node visitRecordSubscript(SqlBaseParser.RecordSubscriptContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitColumnReference(SqlBaseParser.ColumnReferenceContext context)
    {
        QualifiedName qualifiedName = new QualifiedName();
        qualifiedName.parts = FastList.newListWith(getIdentText(context.ident()));

        QualifiedNameReference reference = new QualifiedNameReference();
        reference.name = qualifiedName;

        return reference;
    }

    @Override
    public Node visitSubscript(SqlBaseParser.SubscriptContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitSubscriptSafe(SqlBaseParser.SubscriptSafeContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitQname(SqlBaseParser.QnameContext context)
    {
        QualifiedNameReference reference = new QualifiedNameReference();
        reference.name = getQualifiedName(context);

        return reference;
    }

    @Override
    public Node visitSimpleCase(SqlBaseParser.SimpleCaseContext context)
    {
        SimpleCaseExpression simpleCaseExpression = new SimpleCaseExpression();
        simpleCaseExpression.operand = (Expression) visit(context.operand);
        simpleCaseExpression.whenClauses = visitCollection(context.whenClause(), WhenClause.class);
        simpleCaseExpression.defaultValue = visitOptionalContext(context.elseExpr, Expression.class);

        return simpleCaseExpression;
    }

    @Override
    public Node visitSearchedCase(SqlBaseParser.SearchedCaseContext context)
    {
        SearchedCaseExpression searchedCaseExpression = new SearchedCaseExpression();
        searchedCaseExpression.whenClauses = visitCollection(context.whenClause(), WhenClause.class);
        searchedCaseExpression.defaultValue = visitOptionalContext(context.elseExpr, Expression.class);

        return searchedCaseExpression;
    }

    @Override
    public Node visitIfCase(SqlBaseParser.IfCaseContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitWhenClause(SqlBaseParser.WhenClauseContext context)
    {
        WhenClause whenClause = new WhenClause();
        whenClause.operand = (Expression) visit(context.condition);
        whenClause.result = (Expression) visit(context.result);

        return whenClause;
    }

    @Override
    public Node visitFilter(SqlBaseParser.FilterContext context)
    {
        return visit(context.where());
    }

    @Override
    public Node visitFunctionCall(SqlBaseParser.FunctionCallContext context)
    {
        FunctionCall functionCall = new FunctionCall();
        functionCall.name = getQualifiedName(context.qname());
        functionCall.arguments = visitCollection(context.expr(), Expression.class);
        functionCall.filter = visitIfPresent(context.filter(), Expression.class).orElse(null);
        functionCall.distinct = isDistinct(context.setQuant());
        functionCall.window = visitIfPresent(context.over(), Window.class).orElse(null);
        functionCall.group = visitIfPresent(context.within(), Group.class).orElse(null);
        functionCall.orderBy = visitCollection(context.sortItem(), SortItem.class);

        return functionCall;
    }

    // Literals
    @Override
    public Node visitNullLiteral(SqlBaseParser.NullLiteralContext context)
    {
        return new NullLiteral();
    }

    @Override
    public Node visitBitString(BitStringContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitStringLiteral(SqlBaseParser.StringLiteralContext context)
    {
        if (context.STRING() != null)
        {
            StringLiteral stringLiteral = new StringLiteral();
            stringLiteral.value = unquote(context.STRING().getText());
            return stringLiteral;
        }
        return visitDollarQuotedStringLiteral(context.dollarQuotedStringLiteral());
    }

    public Node visitDollarQuotedStringLiteral(SqlBaseParser.DollarQuotedStringLiteralContext ctx)
    {
        StringLiteral stringLiteral = new StringLiteral();
        stringLiteral.value = ctx.DOLLAR_QUOTED_STRING_BODY().stream().map(ParseTree::getText).collect(Collectors.joining());

        return stringLiteral;
    }

    @Override
    public Node visitEscapedCharsStringLiteral(SqlBaseParser.EscapedCharsStringLiteralContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitIntegerLiteral(SqlBaseParser.IntegerLiteralContext context)
    {
        long value = Long.parseLong(context.getText());
        if (value < Integer.MAX_VALUE + 1L)
        {
            IntegerLiteral integerLiteral = new IntegerLiteral();
            integerLiteral.value = value;

            return integerLiteral;
        }
        LongLiteral longLiteral = new LongLiteral();
        longLiteral.value = value;

        return longLiteral;
    }

    @Override
    public Node visitDecimalLiteral(SqlBaseParser.DecimalLiteralContext context)
    {
        DoubleLiteral doubleLiteral = new DoubleLiteral();
        doubleLiteral.value = Double.valueOf(context.getText());

        return doubleLiteral;
    }

    @Override
    public Node visitBooleanLiteral(SqlBaseParser.BooleanLiteralContext context)
    {
        BooleanLiteral booleanLiteral = new BooleanLiteral();
        booleanLiteral.value = context.TRUE() != null ? true : false;

        return booleanLiteral;
    }

    @Override
    public Node visitArrayLiteral(SqlBaseParser.ArrayLiteralContext context)
    {
        ArrayLiteral arrayLiteral = new ArrayLiteral();
        arrayLiteral.values = visitCollection(context.expr(), Expression.class);

        return arrayLiteral;
    }

    @Override
    public Node visitEmptyArray(SqlBaseParser.EmptyArrayContext ctx)
    {
        return new ArrayLiteral();
    }

    @Override
    public Node visitObjectLiteral(SqlBaseParser.ObjectLiteralContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitParameterPlaceholder(SqlBaseParser.ParameterPlaceholderContext context)
    {
        ParameterPlaceholderExpression parameterExpression = new ParameterPlaceholderExpression();
        parameterExpression.index = positionalIndex++;

        return parameterExpression;
    }

    @Override
    public Node visitPositionalParameter(SqlBaseParser.PositionalParameterContext context)
    {
        PositionalParameterExpression parameterExpression = new PositionalParameterExpression();
        parameterExpression.index = Long.parseLong(context.integerLiteral().getText());

        return parameterExpression;
    }

    @Override
    public Node visitOn(SqlBaseParser.OnContext context)
    {
        BooleanLiteral booleanLiteral = new BooleanLiteral();
        booleanLiteral.value = true;

        return booleanLiteral;
    }

    @Override
    public Node visitArrayDataType(SqlBaseParser.ArrayDataTypeContext ctx)
    {
        return unsupported();
    }

    @Override
    public Node visitObjectTypeDefinition(SqlBaseParser.ObjectTypeDefinitionContext context)
    {
        return unsupported();
    }

    @Override
    public Node visitMaybeParametrizedDataType(SqlBaseParser.MaybeParametrizedDataTypeContext context)
    {
        StringLiteral name = (StringLiteral) visit(context.baseDataType());

        List<Long> parameters = ListIterate.collect(visitCollection(context.integerLiteral(), Node.class), n ->
        {
            if (n instanceof IntegerLiteral)
            {
                return Long.valueOf(((IntegerLiteral) n).value);
            }
            else if (n instanceof LongLiteral)
            {
                return ((LongLiteral) n).value;
            }
            throw new UnsupportedOperationException("Invalid cast parameter");
        });

        ColumnType columnType = new ColumnType();
        columnType.name = name.value;
        columnType.parameters = parameters;

        return columnType;
    }

    @Override
    public Node visitIdentDataType(SqlBaseParser.IdentDataTypeContext context)
    {
        StringLiteral stringLiteral = new StringLiteral();
        stringLiteral.value = getIdentText(context.ident());

        return stringLiteral;
    }

    @Override
    public Node visitDefinedDataType(SqlBaseParser.DefinedDataTypeContext context)
    {
        StringLiteral stringLiteral = new StringLiteral();
        stringLiteral.value = String.join(" ", ListIterate.collect(context.children, ParseTree::getText));

        return stringLiteral;
    }

    // Helpers

    @Override
    protected Node defaultResult()
    {
        return null;
    }

    @Override
    protected Node aggregateResult(Node aggregate, Node nextResult)
    {
        if (nextResult == null)
        {
            throw new UnsupportedOperationException("not yet implemented");
        }
        if (aggregate == null)
        {
            return nextResult;
        }

        throw new UnsupportedOperationException("not yet implemented");
    }

    private String qualifiedNameToString(QualifiedName name)
    {
        return String.join(".", name.parts);
    }

    private <T> T visitOptionalContext(ParserRuleContext context, Class<T> clazz)
    {
        if (context != null)
        {
            return clazz.cast(visit(context));
        }
        return null;
    }

    private <T> Optional<T> visitIfPresent(ParserRuleContext context, Class<T> clazz)
    {
        if (context == null)
        {
            return Optional.empty();
        }
        Node node = context.accept(this);
        if (node == null)
        {
            return Optional.empty();
        }
        return Optional.of(clazz.cast(node));
    }

    private <T> List<T> visitCollection(List<? extends ParserRuleContext> contexts, Class<T> clazz)
    {
        ArrayList<T> result = new ArrayList<>(contexts.size());
        assert contexts instanceof RandomAccess : "Index access must be fast";
        for (int i = 0; i < contexts.size(); i++)
        {
            ParserRuleContext parserRuleContext = contexts.get(i);
            T item = clazz.cast(parserRuleContext.accept(this));
            result.add(item);
        }
        return result;
    }

    private static String unquote(String value)
    {
        return value.substring(1, value.length() - 1)
                .replace("''", "'");
    }

    private QualifiedName getQualifiedName(SqlBaseParser.QnameContext context)
    {
        QualifiedName qualifiedName = new QualifiedName();
        qualifiedName.parts = identsToStrings(context.ident());

        return qualifiedName;
    }

    private List<String> identsToStrings(List<SqlBaseParser.IdentContext> idents)
    {
        return ListIterate.collect(idents, this::getIdentText);
    }

    private static boolean isDistinct(SqlBaseParser.SetQuantContext setQuantifier)
    {
        return setQuantifier != null && setQuantifier.DISTINCT() != null;
    }

    private List<String> getColumnAliases(SqlBaseParser.AliasedColumnsContext columnAliasesContext)
    {
        if (columnAliasesContext == null)
        {
            return FastList.newList();
        }
        return identsToStrings(columnAliasesContext.ident());
    }

    private static ArithmeticType getArithmeticBinaryOperator(Token operator)
    {
        switch (operator.getType())
        {
            case SqlBaseLexer.PLUS:
                return ArithmeticType.ADD;
            case SqlBaseLexer.MINUS:
                return ArithmeticType.SUBTRACT;
            case SqlBaseLexer.ASTERISK:
                return ArithmeticType.MULTIPLY;
            case SqlBaseLexer.SLASH:
                return ArithmeticType.DIVIDE;
            case SqlBaseLexer.PERCENT:
                return ArithmeticType.MODULUS;
            case SqlBaseLexer.CARET:
                return ArithmeticType.POWER;
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + operator.getText());
        }
    }

    private static ComparisonOperator getComparisonOperator(Token symbol)
    {
        switch (symbol.getType())
        {
            case SqlBaseLexer.EQ:
                return ComparisonOperator.EQUAL;
            case SqlBaseLexer.NEQ:
                return ComparisonOperator.NOT_EQUAL;
            case SqlBaseLexer.LT:
                return ComparisonOperator.LESS_THAN;
            case SqlBaseLexer.LTE:
                return ComparisonOperator.LESS_THAN_OR_EQUAL;
            case SqlBaseLexer.GT:
                return ComparisonOperator.GREATER_THAN;
            case SqlBaseLexer.GTE:
                return ComparisonOperator.GREATER_THAN_OR_EQUAL;
            case SqlBaseLexer.REGEX_MATCH:
                return ComparisonOperator.REGEX_MATCH;
            case SqlBaseLexer.REGEX_MATCH_CI:
                return ComparisonOperator.REGEX_MATCH_CI;
            case SqlBaseLexer.REGEX_NO_MATCH:
                return ComparisonOperator.REGEX_NO_MATCH;
            case SqlBaseLexer.REGEX_NO_MATCH_CI:
                return ComparisonOperator.REGEX_NO_MATCH_CI;
            case SqlBaseLexer.OP_LIKE:
                return ComparisonOperator.LIKE;
            case SqlBaseLexer.OP_ILIKE:
                return ComparisonOperator.ILIKE;
            case SqlBaseLexer.OP_NOT_LIKE:
                return ComparisonOperator.NOT_LIKE;
            case SqlBaseLexer.OP_NOT_ILIKE:
                return ComparisonOperator.NOT_ILIKE;
            //TODO handle other operators
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + symbol.getText());
        }
    }

    private static LogicalBinaryType getLogicalBinaryOperator(Token token)
    {
        switch (token.getType())
        {
            case SqlBaseLexer.AND:
                return LogicalBinaryType.AND;
            case SqlBaseLexer.OR:
                return LogicalBinaryType.OR;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + token.getText());
        }
    }

    private static CurrentTimeType getDateTimeFunctionType(Token token)
    {
        switch (token.getType())
        {
            case SqlBaseLexer.CURRENT_DATE:
                return CurrentTimeType.DATE;
            case SqlBaseLexer.CURRENT_TIME:
                return CurrentTimeType.TIME;
            case SqlBaseLexer.CURRENT_TIMESTAMP:
                return CurrentTimeType.TIMESTAMP;
            default:
                throw new UnsupportedOperationException("Unsupported special function: " + token.getText());
        }
    }

    private TrimMode getTrimMode(Token type)
    {
        if (type == null)
        {
            return TrimMode.BOTH;
        }
        switch (type.getType())
        {
            case SqlBaseLexer.BOTH:
                return TrimMode.BOTH;
            case SqlBaseLexer.LEADING:
                return TrimMode.LEADING;
            case SqlBaseLexer.TRAILING:
                return TrimMode.TRAILING;
            default:
                throw new UnsupportedOperationException("Unsupported trim mode: " + type.getText());
        }
    }

    private static SortItemNullOrdering getNullOrderingType(Token token)
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

    private static SortItemOrdering getOrderingType(Token token)
    {
        if (token != null)
        {
            switch (token.getType())
            {
                case SqlBaseLexer.ASC:
                    return SortItemOrdering.ASCENDING;
                case SqlBaseLexer.DESC:
                    return SortItemOrdering.DESCENDING;
                default:
                    throw new IllegalArgumentException("Unsupported ordering: " + token.getText());
            }
        }
        return SortItemOrdering.ASCENDING;
    }

    private QualifiedName qualifiedName(String... parts)
    {
        QualifiedName qualifiedName = new QualifiedName();
        qualifiedName.parts = FastList.newListWith(parts);

        return qualifiedName;
    }

    private FunctionCall functionCall(String name, ExprContext... contexts)
    {
        return functionCall(name, ArrayIterate.collect(contexts, c -> (Expression) visit(c)));
    }

    private FunctionCall functionCall(String name, List<Expression> arguments)
    {
        FunctionCall functionCall = new FunctionCall();
        functionCall.name = qualifiedName(name);
        functionCall.arguments = arguments;

        return functionCall;
    }

    private static WindowFrameMode getFrameType(Token type)
    {
        switch (type.getType())
        {
            case SqlBaseLexer.RANGE:
                return WindowFrameMode.RANGE;
            case SqlBaseLexer.ROWS:
                return WindowFrameMode.ROWS;
            default:
                throw new IllegalArgumentException("Unsupported frame type: " + type.getText());
        }
    }

    private static FrameBoundType getBoundedFrameBoundType(Token token)
    {
        switch (token.getType())
        {
            case SqlBaseLexer.PRECEDING:
                return FrameBoundType.PRECEDING;
            case SqlBaseLexer.FOLLOWING:
                return FrameBoundType.FOLLOWING;
            default:
                throw new IllegalArgumentException("Unsupported bound type: " + token.getText());
        }
    }

    private static FrameBoundType getUnboundedFrameBoundType(Token token)
    {
        switch (token.getType())
        {
            case SqlBaseLexer.PRECEDING:
                return FrameBoundType.UNBOUNDED_PRECEDING;
            case SqlBaseLexer.FOLLOWING:
                return FrameBoundType.UNBOUNDED_FOLLOWING;

            default:
                throw new IllegalArgumentException("Unsupported bound type: " + token.getText());
        }
    }

    private Node unsupported()
    {
        throw new UnsupportedOperationException();
    }
}