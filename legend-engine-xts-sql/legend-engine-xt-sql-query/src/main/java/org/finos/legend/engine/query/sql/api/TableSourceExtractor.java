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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.sql.metamodel.ArrayLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.BooleanLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.DoubleLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.Expression;
import org.finos.legend.engine.protocol.sql.metamodel.IntegerLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.LongLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.NamedArgumentExpression;
import org.finos.legend.engine.protocol.sql.metamodel.NullLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.StringLiteral;
import org.finos.legend.engine.protocol.sql.metamodel.Table;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.engine.query.sql.providers.core.TableSource;
import org.finos.legend.engine.query.sql.providers.core.TableSourceArgument;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableSourceExtractor extends BaseNodeCollectorVisitor<Set<TableSource>>
{

    public TableSourceExtractor()
    {
        super(values -> values.stream().flatMap(Collection::stream).collect(Collectors.toSet()), Sets.mutable.empty());
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

        return expression;
    }
}