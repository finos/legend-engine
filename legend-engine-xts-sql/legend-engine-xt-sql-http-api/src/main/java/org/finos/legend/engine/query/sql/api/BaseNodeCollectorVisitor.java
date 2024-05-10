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

import org.eclipse.collections.impl.list.mutable.FastList;
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

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class BaseNodeCollectorVisitor<T> implements NodeVisitor<T>
{
    private final Function<List<T>, T> collator;
    private final T defaultValue;

    public BaseNodeCollectorVisitor(Function<List<T>, T> collator, T defaultValue)
    {
        this.collator = collator;
        this.defaultValue = defaultValue;
    }

    private T defaultValue()
    {
        return this.defaultValue;
    }

    protected T collate(T... values)
    {
        return collator.apply(FastList.newListWith(values));
    }

    protected T collate(List<T> values)
    {
        return collator.apply(values);
    }

    protected T collect(List<? extends Node> nodes)
    {
        return collate(ListIterate.collectIf(nodes, Objects::nonNull, this::collect));
    }

    protected T collect(Node... nodes)
    {
        return collect(FastList.newListWith(nodes));
    }

    protected T collect(Node node)
    {
        return node != null ? node.accept(this) : defaultValue();
    }

    @Override
    public T visit(AliasedRelation val)
    {
        return collect(val.relation);
    }

    @Override
    public T visit(AllColumns val)
    {
        return defaultValue();
    }

    @Override
    public T visit(ArithmeticExpression val)
    {
        return collect(val.left, val.right);
    }

    @Override
    public T visit(ArrayLiteral val)
    {
        return defaultValue();
    }

    @Override
    public T visit(BetweenPredicate val)
    {
        return collect(val.min, val.max, val.value);
    }

    @Override
    public T visit(BooleanLiteral val)
    {
        return defaultValue();
    }

    @Override
    public T visit(Cast val)
    {
        return collect(val.expression);
    }

    @Override
    public T visit(ColumnType val)
    {
        return defaultValue();
    }

    @Override
    public T visit(ComparisonExpression val)
    {
        return collect(val.left, val.right);
    }

    @Override
    public T visit(CurrentTime val)
    {
        return defaultValue();
    }

    @Override
    public T visit(DoubleLiteral val)
    {
        return defaultValue();
    }

    @Override
    public T visit(Expression val)
    {
        return collect(val);
    }

    @Override
    public T visit(Extract val)
    {
        return collect(val.expression);
    }

    @Override
    public T visit(FrameBound val)
    {
        return collect(val.value);
    }

    @Override
    public T visit(FunctionCall val)
    {
        T arguments = collect(val.arguments);
        T filter = collect(val.filter);
        T order = collect(val.orderBy);
        T group = collect(val.group);
        T window = collect(val.window);

        return collate(arguments, filter, order, group, window);
    }

    @Override
    public T visit(Group group)
    {
        return collect(group.orderBy);
    }

    @Override
    public T visit(InListExpression val)
    {
        return collect(val.values);
    }

    @Override
    public T visit(InPredicate val)
    {
        return collect(val.value, val.valueList);
    }

    @Override
    public T visit(IntegerLiteral val)
    {
        return defaultValue();
    }

    @Override
    public T visit(IntervalLiteral val)
    {
        return defaultValue();
    }

    @Override
    public T visit(IsNotNullPredicate val)
    {
        return collect(val.value);
    }

    @Override
    public T visit(IsNullPredicate val)
    {
        return collect(val.value);
    }

    @Override
    public T visit(Join val)
    {
        return collect(val.left, val.right);
    }

    @Override
    public T visit(LikePredicate val)
    {
        return collect(val.value, val.pattern, val.escape);
    }

    @Override
    public T visit(Literal val)
    {
        return defaultValue();
    }

    @Override
    public T visit(LogicalBinaryExpression val)
    {
        return collect(val.left, val.right);
    }

    @Override
    public T visit(LongLiteral val)
    {
        return defaultValue();
    }

    @Override
    public T visit(NamedArgumentExpression val)
    {
        return collect(val.expression);
    }

    @Override
    public T visit(NegativeExpression val)
    {
        return collect(val.value);
    }

    @Override
    public T visit(NotExpression val)
    {
        return collect(val.value);
    }

    @Override
    public T visit(NullLiteral val)
    {
        return defaultValue();
    }

    @Override
    public T visit(ParameterPlaceholderExpression val)
    {
        return defaultValue();
    }

    @Override
    public T visit(PositionalParameterExpression val)
    {
        return defaultValue();
    }


    @Override
    public T visit(QualifiedNameReference val)
    {
        return defaultValue();
    }

    @Override
    public T visit(Query val)
    {
        T offset = collect(val.offset);
        T body = collect(val.queryBody);
        T limit = collect(val.limit);
        T order = collect(val.orderBy);

        return collate(offset, body, limit, order);
    }

    @Override
    public T visit(QueryBody val)
    {
        return collect(val);
    }

    @Override
    public T visit(QuerySpecification val)
    {
        T from = collect(val.from);
        T limit = collect(val.limit);
        T order = collect(val.orderBy);
        T offset = collect(val.offset);
        T group = collect(val.groupBy);
        T having = collect(val.having);
        T select = collect(val.select);
        T where = collect(val.where);

        return collate(from, limit, order, offset, group, having, select, where);
    }

    @Override
    public T visit(Relation val)
    {
        return collect(val);
    }

    @Override
    public T visit(SearchedCaseExpression val)
    {
        T whenClauses = collect(val.whenClauses);
        T defaultValue = collect(val.defaultValue);

        return collate(whenClauses, defaultValue);
    }

    @Override
    public T visit(Select val)
    {
        return collect(val.selectItems);
    }

    @Override
    public T visit(SelectItem val)
    {
        return collect(val);
    }

    @Override
    public T visit(SetOperation val)
    {
        return collect(val);
    }

    @Override
    public T visit(SimpleCaseExpression val)
    {
        T whenClauses = collect(val.whenClauses);
        T defaultValue = collect(val.defaultValue);

        return collate(whenClauses, defaultValue);
    }

    @Override
    public T visit(SingleColumn val)
    {
        return collect(val.expression);
    }

    @Override
    public T visit(SortItem val)
    {
        return defaultValue();
    }

    @Override
    public T visit(Statement val)
    {
        return collect(val);
    }

    @Override
    public T visit(StringLiteral val)
    {
        return defaultValue();
    }

    @Override
    public T visit(SubqueryExpression val)
    {
        return collect(val.query);
    }

    @Override
    public T visit(Table val)
    {
        return defaultValue();
    }

    @Override
    public T visit(TableFunction val)
    {
        return collect(val.functionCall);
    }

    @Override
    public T visit(TableSubquery val)
    {
        return collect(val.query);
    }

    @Override
    public T visit(Trim val)
    {
        return collect(val.value, val.characters);
    }

    @Override
    public T visit(Union val)
    {
        return collect(val.left, val.right);
    }

    @Override
    public T visit(WhenClause val)
    {
        return collect(val.operand, val.result);
    }

    @Override
    public T visit(Window val)
    {
        T order = collect(val.orderBy);
        T partition = collect(val.partitions);
        T frame = collect(val.windowFrame);

        return collate(order, partition, frame);
    }

    @Override
    public T visit(WindowFrame val)
    {
        return collect(val.end, val.start);
    }
}