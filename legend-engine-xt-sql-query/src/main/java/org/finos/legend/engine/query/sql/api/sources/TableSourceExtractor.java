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

package org.finos.legend.engine.query.sql.api.sources;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.sql.metamodel.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableSourceExtractor implements NodeVisitor<Set<TableSource>>
{
    @Override
    public Set<TableSource> visit(AliasedRelation val)
    {
        return val.relation.accept(this);
    }

    @Override
    public Set<TableSource> visit(AllColumns val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(ArithmeticExpression val)
    {
        Set<TableSource> leftTables = val.left.accept(this);
        Set<TableSource> rightTables = val.right.accept(this);
        return Sets.mutable.withAll(leftTables).withAll(rightTables);
    }

    @Override
    public Set<TableSource> visit(ArrayLiteral val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(BooleanLiteral val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(Cast val)
    {
        return val.expression.accept(this);
    }

    @Override
    public Set<TableSource> visit(ColumnType val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(ComparisonExpression val)
    {
        Set<TableSource> leftTables = val.left.accept(this);
        Set<TableSource> rightTables = val.right.accept(this);
        return Sets.mutable.withAll(leftTables).withAll(rightTables);
    }

    @Override
    public Set<TableSource> visit(CurrentTime val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(DoubleLiteral val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(Expression val)
    {
        return val.accept(this);
    }

    @Override
    public Set<TableSource> visit(Extract val)
    {
        return val.expression.accept(this);
    }

    @Override
    public Set<TableSource> visit(FrameBound val)
    {
        return val.value.accept(this);
    }

    @Override
    public Set<TableSource> visit(FunctionCall val)
    {
        Set<TableSource> argumentTableNames = val.arguments.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Set<TableSource> filterTableNames = val.filter.accept(this);
        return Sets.mutable.withAll(argumentTableNames).withAll(filterTableNames);
    }

    @Override
    public Set<TableSource> visit(InListExpression val)
    {
        return val.values.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TableSource> visit(InPredicate val)
    {
        Set<TableSource> valueTableNames = val.value.accept(this);
        Set<TableSource> valueListTableNames = val.valueList.accept(this);
        return Sets.mutable.withAll(valueTableNames).withAll(valueListTableNames);
    }

    @Override
    public Set<TableSource> visit(IntegerLiteral val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(IntervalLiteral val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(IsNotNullPredicate val)
    {
        return val.value.accept(this);
    }

    @Override
    public Set<TableSource> visit(IsNullPredicate val)
    {
        return val.value.accept(this);
    }

    @Override
    public Set<TableSource> visit(Join val)
    {
        Set<TableSource> leftTableNames = val.left.accept(this);
        Set<TableSource> rightTableNames = val.right.accept(this);
        return Sets.mutable.withAll(leftTableNames).withAll(rightTableNames);
    }

    @Override
    public Set<TableSource> visit(Literal val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(LogicalBinaryExpression val)
    {
        Set<TableSource> leftTableNames = val.left.accept(this);
        Set<TableSource> rightTableNames = val.right.accept(this);
        return Sets.mutable.withAll(leftTableNames).withAll(rightTableNames);
    }

    @Override
    public Set<TableSource> visit(LongLiteral val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(NamedArgumentExpression val)
    {
        return val.expression.accept(this);
    }

    @Override
    public Set<TableSource> visit(NegativeExpression val)
    {
        return val.value.accept(this);
    }

    @Override
    public Set<TableSource> visit(NotExpression val)
    {
        return val.value.accept(this);
    }

    @Override
    public Set<TableSource> visit(NullLiteral val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(QualifiedNameReference val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(Query val)
    {
        return val.queryBody.accept(this);
    }

    @Override
    public Set<TableSource> visit(QueryBody val)
    {
        return val.accept(this);
    }

    @Override
    public Set<TableSource> visit(QuerySpecification val)
    {
        return val.from.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TableSource> visit(Relation val)
    {
        return val.accept(this);
    }

    @Override
    public Set<TableSource> visit(SearchedCaseExpression val)
    {
        Set<TableSource> whenClausesTableNames = val.whenClauses.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Set<TableSource> defaultValueTableName = val.defaultValue.accept(this);
        return Sets.mutable.withAll(whenClausesTableNames).withAll(defaultValueTableName);
    }

    @Override
    public Set<TableSource> visit(Select val)
    {
        return val.selectItems.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TableSource> visit(SelectItem val)
    {
        return val.accept(this);
    }

    @Override
    public Set<TableSource> visit(SetOperation val)
    {
        return val.accept(this);
    }

    @Override
    public Set<TableSource> visit(SimpleCaseExpression val)
    {
        Set<TableSource> whenClausesTableNames = val.whenClauses.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Set<TableSource> defaultValueTableNames = val.defaultValue.accept(this);
        return Sets.mutable.withAll(whenClausesTableNames).withAll(defaultValueTableNames);
    }

    @Override
    public Set<TableSource> visit(SingleColumn val)
    {
        return val.expression.accept(this);
    }

    @Override
    public Set<TableSource> visit(SortItem val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(Statement val)
    {
        return val.accept(this);
    }

    @Override
    public Set<TableSource> visit(StringLiteral val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(SubqueryExpression val)
    {
        return val.query.accept(this);
    }

    @Override
    public Set<TableSource> visit(Table val)
    {
        if (val.name.parts.size() != 2)
        {
            throw new IllegalArgumentException("All table names are expected to have 2 parts, <schema>.<name>");
        }
        return Sets.mutable.of(new TableSource(val.name.parts.get(0), Lists.mutable.of(new TableSourceArgument(null, 0, val.name.parts.get(1)))));
    }

    @Override
    public Set<TableSource> visit(TableFunction val)
    {
        if (val.functionCall.name.parts.size() != 1)
        {
            throw new IllegalArgumentException("All table functions must have 1 part");
        }

        List<TableSourceArgument> arguments = ListIterate.collectWithIndex(val.functionCall.arguments, this::extractArgument);

        return Sets.mutable.of(new TableSource(val.functionCall.name.parts.get(0), arguments));
    }

    private TableSourceArgument extractArgument(Expression expression, Integer index)
    {
        String name = expression instanceof NamedArgumentExpression ? (((NamedArgumentExpression) expression).name) : null;
        Integer adjustedIndex = expression instanceof NamedArgumentExpression ? null : index;
        Object value = extractArgumentValue(expression);

        return new TableSourceArgument(name, adjustedIndex, value);
    }

    private Object extractArgumentValue(Expression expression)
    {
        if (expression instanceof NamedArgumentExpression)
        {
            return extractArgumentValue(((NamedArgumentExpression) expression).expression);
        }
        else if (expression instanceof IntegerLiteral)
        {
            return ((IntegerLiteral) expression).value;
        }
        else if (expression instanceof StringLiteral)
        {
            return ((StringLiteral) expression).value;
        }
        else if (expression instanceof BooleanLiteral)
        {
            return ((BooleanLiteral) expression).value;
        }
        else if (expression instanceof DoubleLiteral)
        {
            return ((DoubleLiteral) expression).value;
        }
        else if (expression instanceof LongLiteral)
        {
            return ((LongLiteral) expression).value;
        }
        else if (expression instanceof ArrayLiteral)
        {
            return ListIterate.collect(((ArrayLiteral) expression).values, this::extractArgumentValue);
        }
        else if (expression instanceof NullLiteral)
        {
            return null;
        }

        throw new IllegalArgumentException("Table function arguments must be primitive");
    }

    @Override
    public Set<TableSource> visit(TableSubquery val)
    {
        return val.query.accept(this);
    }

    @Override
    public Set<TableSource> visit(Union val)
    {
        Set<TableSource> leftTableNames = val.left.accept(this);
        Set<TableSource> rightTableNames = val.right.accept(this);
        return Sets.mutable.withAll(leftTableNames).withAll(rightTableNames);
    }

    @Override
    public Set<TableSource> visit(WhenClause val)
    {
        Set<TableSource> operandTableNames = val.operand.accept(this);
        Set<TableSource> resultTableNames = val.result.accept(this);
        return Sets.mutable.withAll(operandTableNames).withAll(resultTableNames);
    }

    @Override
    public Set<TableSource> visit(Window val)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<TableSource> visit(WindowFrame val)
    {
        return Collections.emptySet();
    }
}
