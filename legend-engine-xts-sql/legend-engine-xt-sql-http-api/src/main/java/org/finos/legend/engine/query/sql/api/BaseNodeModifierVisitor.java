// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//


package org.finos.legend.engine.query.sql.api;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.sql.metamodel.AliasedRelation;
import org.finos.legend.engine.protocol.sql.metamodel.AllColumns;
import org.finos.legend.engine.protocol.sql.metamodel.ArithmeticExpression;
import org.finos.legend.engine.protocol.sql.metamodel.ArrayLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.BetweenPredicate;
import org.finos.legend.engine.protocol.sql.metamodel.BooleanLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.Cast;
import org.finos.legend.engine.protocol.sql.metamodel.ColumnType;
import org.finos.legend.engine.protocol.sql.metamodel.ComparisonExpression;
import org.finos.legend.engine.protocol.sql.metamodel.CurrentTime;
import org.finos.legend.engine.protocol.sql.metamodel.DoubleLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.Expression;
import org.finos.legend.engine.protocol.sql.metamodel.Extract;
import org.finos.legend.engine.protocol.sql.metamodel.FrameBound;
import org.finos.legend.engine.protocol.sql.metamodel.FunctionCall;
import org.finos.legend.engine.protocol.sql.metamodel.Group;
import org.finos.legend.engine.protocol.sql.metamodel.InListExpression;
import org.finos.legend.engine.protocol.sql.metamodel.InPredicate;
import org.finos.legend.engine.protocol.sql.metamodel.IntegerLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.IntervalLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.IsNotNullPredicate;
import org.finos.legend.engine.protocol.sql.metamodel.IsNullPredicate;
import org.finos.legend.engine.protocol.sql.metamodel.Join;
import org.finos.legend.engine.protocol.sql.metamodel.JoinOn;
import org.finos.legend.engine.protocol.sql.metamodel.LikePredicate;
import org.finos.legend.engine.protocol.sql.metamodel.Literal;
import org.finos.legend.engine.protocol.sql.metamodel.LogicalBinaryExpression;
import org.finos.legend.engine.protocol.sql.metamodel.LongLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.NamedArgumentExpression;
import org.finos.legend.engine.protocol.sql.metamodel.NegativeExpression;
import org.finos.legend.engine.protocol.sql.metamodel.Node;
import org.finos.legend.engine.protocol.sql.metamodel.NodeVisitor;
import org.finos.legend.engine.protocol.sql.metamodel.NotExpression;
import org.finos.legend.engine.protocol.sql.metamodel.NullLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.ParameterPlaceholderExpression;
import org.finos.legend.engine.protocol.sql.metamodel.PositionalParameterExpression;
import org.finos.legend.engine.protocol.sql.metamodel.QualifiedNameReference;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.metamodel.QueryBody;
import org.finos.legend.engine.protocol.sql.metamodel.QuerySpecification;
import org.finos.legend.engine.protocol.sql.metamodel.Relation;
import org.finos.legend.engine.protocol.sql.metamodel.SearchedCaseExpression;
import org.finos.legend.engine.protocol.sql.metamodel.Select;
import org.finos.legend.engine.protocol.sql.metamodel.SelectItem;
import org.finos.legend.engine.protocol.sql.metamodel.SetOperation;
import org.finos.legend.engine.protocol.sql.metamodel.SimpleCaseExpression;
import org.finos.legend.engine.protocol.sql.metamodel.SingleColumn;
import org.finos.legend.engine.protocol.sql.metamodel.SortItem;
import org.finos.legend.engine.protocol.sql.metamodel.Statement;
import org.finos.legend.engine.protocol.sql.metamodel.StringLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.SubqueryExpression;
import org.finos.legend.engine.protocol.sql.metamodel.Table;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.engine.protocol.sql.metamodel.TableSubquery;
import org.finos.legend.engine.protocol.sql.metamodel.Trim;
import org.finos.legend.engine.protocol.sql.metamodel.Union;
import org.finos.legend.engine.protocol.sql.metamodel.WhenClause;
import org.finos.legend.engine.protocol.sql.metamodel.Window;
import org.finos.legend.engine.protocol.sql.metamodel.WindowFrame;
import org.finos.legend.engine.protocol.sql.metamodel.With;
import org.finos.legend.engine.protocol.sql.metamodel.WithQuery;

import java.util.List;
import java.util.function.Predicate;

public class BaseNodeModifierVisitor implements NodeVisitor<Node>
{
    @Override
    public Node visit(AliasedRelation val)
    {
        val.relation = _visit(val.relation);

        return val;
    }

    @Override
    public Node visit(AllColumns val)
    {
        return val;
    }

    @Override
    public Node visit(ArithmeticExpression val)
    {
        val.left = _visit(val.left);
        val.right = _visit(val.right);

        return val;
    }

    @Override
    public Node visit(ArrayLiteral val)
    {
        val.values = _visit(val.values);

        return val;
    }

    @Override
    public Node visit(BetweenPredicate val)
    {
        val.value = _visit(val.value);
        val.min = _visit(val.min);
        val.max = _visit(val.max);

        return val;
    }

    @Override
    public Node visit(BooleanLiteral val)
    {
        return val;
    }

    @Override
    public Node visit(Cast val)
    {
        val.expression = _visit(val.expression);

        return val;
    }

    @Override
    public Node visit(ColumnType val)
    {
        return val;
    }

    @Override
    public Node visit(WithQuery val)
    {
        val.query = _visit(val.query);
        
        return val;
    }

    @Override
    public Node visit(With val)
    {
        val.withQueries = _visit(val.withQueries);
        
        return val;
    }

    @Override
    public Node visit(ComparisonExpression val)
    {
        val.left = _visit(val.left);
        val.right = _visit(val.right);

        return val;
    }

    @Override
    public Node visit(CurrentTime val)
    {
        return val;
    }

    @Override
    public Node visit(DoubleLiteral val)
    {
        return val;
    }

    @Override
    public Node visit(Expression val)
    {
        return val;
    }

    @Override
    public Node visit(Extract val)
    {
        val.expression = _visit(val.expression);

        return val;
    }

    @Override
    public Node visit(FrameBound val)
    {
        val.value = _visit(val.value);

        return val;
    }

    @Override
    public Node visit(FunctionCall val)
    {
        val.arguments = _visit(val.arguments);

        return val;
    }

    @Override
    public Node visit(Group val)
    {
        return val;
    }

    @Override
    public Node visit(InListExpression val)
    {
        val.values = _visit(val.values);

        return val;
    }

    @Override
    public Node visit(InPredicate val)
    {
        val.value = _visit(val.value);
        val.valueList = _visit(val.valueList);

        return val;
    }

    @Override
    public Node visit(IntegerLiteral val)
    {
        return val;
    }

    @Override
    public Node visit(IntervalLiteral val)
    {
        return val;
    }

    @Override
    public Node visit(IsNotNullPredicate val)
    {
        val.value = _visit(val.value);

        return val;
    }

    @Override
    public Node visit(IsNullPredicate val)
    {
        val.value = _visit(val.value);

        return val;
    }

    @Override
    public Node visit(Join val)
    {
        val.left = (Relation) val.left.accept(this);
        val.right = (Relation) val.right.accept(this);

        if (val.criteria instanceof JoinOn)
        {
            ((JoinOn) val.criteria).expression = (Expression) ((JoinOn) val.criteria).expression.accept(this);
        }

        return val;
    }

    @Override
    public Node visit(LikePredicate val)
    {
        val.value = _visit(val.value);
        val.pattern = _visit(val.pattern);

        return val;
    }

    @Override
    public Node visit(Literal val)
    {
        return val;
    }

    @Override
    public Node visit(LogicalBinaryExpression val)
    {
        val.left = _visit(val.left);
        val.right = _visit(val.right);

        return val;
    }

    @Override
    public Node visit(LongLiteral val)
    {
        return val;
    }

    @Override
    public Node visit(NamedArgumentExpression val)
    {
        val.expression = _visit(val.expression);

        return val;
    }

    @Override
    public Node visit(NegativeExpression val)
    {
        val.value = _visit(val.value);

        return val;
    }

    @Override
    public Node visit(NotExpression val)
    {
        val.value = _visit(val.value);

        return val;
    }

    @Override
    public Node visit(NullLiteral val)
    {
        return val;
    }

    @Override
    public Node visit(PositionalParameterExpression val)
    {
        return val;
    }

    @Override
    public Node visit(ParameterPlaceholderExpression val)
    {
        return val;
    }

    @Override
    public Node visit(QualifiedNameReference val)
    {
        return val;
    }

    @Override
    public Node visit(Query val)
    {
        val.queryBody = (QueryBody) val.queryBody.accept(this);
        val.limit = _visit(val.limit);
        val.offset = _visit(val.offset);

        return val;
    }

    @Override
    public Node visit(QueryBody val)
    {
        return val;
    }

    @Override
    public Node visit(QuerySpecification val)
    {
        val.from = _visit(val.from);
        val.where = _visit(val.where);
        val.groupBy = _visit(val.groupBy, e -> !(e instanceof IntegerLiteral));
        val.having = _visit(val.having);
        val.select = (Select) val.select.accept(this);
        val.limit = _visit(val.limit);
        val.offset = _visit(val.offset);

        return val;
    }

    @Override
    public Node visit(Relation val)
    {
        return val;
    }

    @Override
    public Node visit(SearchedCaseExpression val)
    {
        val.defaultValue = _visit(val.defaultValue);
        val.whenClauses = _visit(val.whenClauses);

        return val;
    }

    @Override
    public Node visit(Select val)
    {
        val.selectItems = _visit(val.selectItems);

        return val;
    }

    @Override
    public Node visit(SelectItem val)
    {
        return val;
    }

    @Override
    public Node visit(SetOperation val)
    {
        return val;
    }

    @Override
    public Node visit(SimpleCaseExpression val)
    {
        val.defaultValue = _visit(val.defaultValue);
        val.operand = _visit(val.operand);
        val.whenClauses = _visit(val.whenClauses);

        return val;
    }

    @Override
    public Node visit(SingleColumn val)
    {
        val.expression = _visit(val.expression);

        return val;
    }

    @Override
    public Node visit(SortItem val)
    {
        val.sortKey = _visit(val.sortKey);

        return val;
    }

    @Override
    public Node visit(Statement val)
    {
        return val;
    }

    @Override
    public Node visit(StringLiteral val)
    {
        return val;
    }

    @Override
    public Node visit(SubqueryExpression val)
    {
        val.query = _visit(val.query);

        return val;
    }

    @Override
    public Node visit(Table val)
    {
        return val;
    }

    @Override
    public Node visit(TableFunction val)
    {
        val.functionCall = _visit(val.functionCall);

        return val;
    }

    @Override
    public Node visit(TableSubquery val)
    {
        val.query = _visit(val.query);

        return val;
    }

    @Override
    public Node visit(Trim val)
    {
        val.value = _visit(val.value);
        val.characters = _visit(val.characters);

        return val;
    }

    @Override
    public Node visit(Union val)
    {
        val.left = _visit(val.left);
        val.right = _visit(val.right);

        return val;
    }

    @Override
    public Node visit(WhenClause val)
    {
        val.result = _visit(val.result);
        val.operand = _visit(val.operand);

        return val;
    }

    @Override
    public Node visit(Window val)
    {
        return val;
    }

    @Override
    public Node visit(WindowFrame val)
    {
        return val;
    }

    protected  <T extends Node> List<T> _visit(List<T> nodes)
    {
        return _visit(nodes, n -> true);
    }

    protected <T extends Node> List<T> _visit(List<T> nodes, Predicate<T> filter)
    {
        return nodes != null ? ListIterate.collect(nodes, n -> filter.test(n) ? (T) n.accept(this) : n) : null;
    }

    protected <T extends Node> T _visit(T node)
    {
        return node != null ? (T) node.accept(this) : null;
    }
}