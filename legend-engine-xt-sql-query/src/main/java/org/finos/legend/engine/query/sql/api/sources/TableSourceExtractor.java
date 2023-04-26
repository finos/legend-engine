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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.sql.metamodel.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TableSourceExtractor implements NodeVisitor<List<TableSource>>
{
    @Override
    public List<TableSource> visit(AliasedRelation val)
    {
        return val.relation.accept(this);
    }

    @Override
    public List<TableSource> visit(AllColumns val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(ArithmeticExpression val)
    {
        List<TableSource> leftTables = val.left.accept(this);
        List<TableSource> rightTables = val.right.accept(this);
        return Lists.mutable.withAll(leftTables).withAll(rightTables);
    }

    @Override
    public List<TableSource> visit(ArrayLiteral val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(BooleanLiteral val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(Cast val)
    {
        return val.expression.accept(this);
    }

    @Override
    public List<TableSource> visit(ColumnType val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(ComparisonExpression val)
    {
        List<TableSource> leftTables = val.left.accept(this);
        List<TableSource> rightTables = val.right.accept(this);
        return Lists.mutable.withAll(leftTables).withAll(rightTables);
    }

    @Override
    public List<TableSource> visit(CurrentTime val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(DoubleLiteral val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(Expression val)
    {
        return val.accept(this);
    }

    @Override
    public List<TableSource> visit(Extract val)
    {
        return val.expression.accept(this);
    }

    @Override
    public List<TableSource> visit(FrameBound val)
    {
        return val.value.accept(this);
    }

    @Override
    public List<TableSource> visit(FunctionCall val)
    {
        List<TableSource> argumentTableNames = val.arguments.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        List<TableSource> filterTableNames = val.filter.accept(this);
        return Lists.mutable.withAll(argumentTableNames).withAll(filterTableNames);
    }

    @Override
    public List<TableSource> visit(InListExpression val)
    {
        return val.values.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<TableSource> visit(InPredicate val)
    {
        List<TableSource> valueTableNames = val.value.accept(this);
        List<TableSource> valueListTableNames = val.valueList.accept(this);
        return Lists.mutable.withAll(valueTableNames).withAll(valueListTableNames);
    }

    @Override
    public List<TableSource> visit(IntegerLiteral val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(IntervalLiteral val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(IsNotNullPredicate val)
    {
        return val.value.accept(this);
    }

    @Override
    public List<TableSource> visit(IsNullPredicate val)
    {
        return val.value.accept(this);
    }

    @Override
    public List<TableSource> visit(Join val)
    {
        List<TableSource> leftTableNames = val.left.accept(this);
        List<TableSource> rightTableNames = val.right.accept(this);
        return Lists.mutable.withAll(leftTableNames).withAll(rightTableNames);
    }

    @Override
    public List<TableSource> visit(Literal val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(LogicalBinaryExpression val)
    {
        List<TableSource> leftTableNames = val.left.accept(this);
        List<TableSource> rightTableNames = val.right.accept(this);
        return Lists.mutable.withAll(leftTableNames).withAll(rightTableNames);
    }

    @Override
    public List<TableSource> visit(LongLiteral val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(NamedArgumentExpression val)
    {
        return val.expression.accept(this);
    }

    @Override
    public List<TableSource> visit(NegativeExpression val)
    {
        return val.value.accept(this);
    }

    @Override
    public List<TableSource> visit(NotExpression val)
    {
        return val.value.accept(this);
    }

    @Override
    public List<TableSource> visit(NullLiteral val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(QualifiedNameReference val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(Query val)
    {
        return val.queryBody.accept(this);
    }

    @Override
    public List<TableSource> visit(QueryBody val)
    {
        return val.accept(this);
    }

    @Override
    public List<TableSource> visit(QuerySpecification val)
    {
        return val.from.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<TableSource> visit(Relation val)
    {
        return val.accept(this);
    }

    @Override
    public List<TableSource> visit(SearchedCaseExpression val)
    {
        List<TableSource> whenClausesTableNames = val.whenClauses.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        List<TableSource> defaultValueTableName = val.defaultValue.accept(this);
        return Lists.mutable.withAll(whenClausesTableNames).withAll(defaultValueTableName);
    }

    @Override
    public List<TableSource> visit(Select val)
    {
        return val.selectItems.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<TableSource> visit(SelectItem val)
    {
        return val.accept(this);
    }

    @Override
    public List<TableSource> visit(SetOperation val)
    {
        return val.accept(this);
    }

    @Override
    public List<TableSource> visit(SimpleCaseExpression val)
    {
        List<TableSource> whenClausesTableNames = val.whenClauses.stream()
                .map(expression -> expression.accept(this))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        List<TableSource> defaultValueTableNames = val.defaultValue.accept(this);
        return Lists.mutable.withAll(whenClausesTableNames).withAll(defaultValueTableNames);
    }

    @Override
    public List<TableSource> visit(SingleColumn val)
    {
        return val.expression.accept(this);
    }

    @Override
    public List<TableSource> visit(SortItem val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(Statement val)
    {
        return val.accept(this);
    }

    @Override
    public List<TableSource> visit(StringLiteral val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(SubqueryExpression val)
    {
        return val.query.accept(this);
    }

    @Override
    public List<TableSource> visit(Table val)
    {
        if (val.name.parts.size() != 2)
        {
            throw new IllegalArgumentException("All table names are expected to have 2 parts, <schema>.<name>");
        }
        return Lists.mutable.of(new TableSource(val.name.parts.get(0), Lists.mutable.of(new TableSourceArgument(null, 0, val.name.parts.get(1)))));
    }

    @Override
    public List<TableSource> visit(TableFunction val)
    {
        if (val.functionCall.name.parts.size() != 1)
        {
            throw new IllegalArgumentException("All table functions must have 1 part");
        }

        List<TableSourceArgument> arguments = ListIterate.collectWithIndex(val.functionCall.arguments, this::extractArgument);

        return Lists.mutable.of(new TableSource(val.functionCall.name.parts.get(0), arguments));
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
    public List<TableSource> visit(TableSubquery val)
    {
        return val.query.accept(this);
    }

    @Override
    public List<TableSource> visit(Union val)
    {
        List<TableSource> leftTableNames = val.left.accept(this);
        List<TableSource> rightTableNames = val.right.accept(this);
        return Lists.mutable.withAll(leftTableNames).withAll(rightTableNames);
    }

    @Override
    public List<TableSource> visit(WhenClause val)
    {
        List<TableSource> operandTableNames = val.operand.accept(this);
        List<TableSource> resultTableNames = val.result.accept(this);
        return Lists.mutable.withAll(operandTableNames).withAll(resultTableNames);
    }

    @Override
    public List<TableSource> visit(Window val)
    {
        return Collections.emptyList();
    }

    @Override
    public List<TableSource> visit(WindowFrame val)
    {
        return Collections.emptyList();
    }
}
