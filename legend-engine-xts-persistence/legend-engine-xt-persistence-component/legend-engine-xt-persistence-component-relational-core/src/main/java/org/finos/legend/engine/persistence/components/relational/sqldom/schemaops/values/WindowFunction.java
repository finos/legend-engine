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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause.ORDER_BY;
import static org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause.OVER;
import static org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause.PARTITION_BY;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class WindowFunction extends Value
{
    /*
    <function> OVER (
    [ PARTITION BY <expr1> [, <expr2> ... ] ]
    ORDER BY <expr3> [ , <expr4> ... ] [ { ASC | DESC } ])
    */

    private Function function;
    private List<Field> partitionByFields;
    private List<OrderedField> orderByFields;

    public WindowFunction(String alias, String quoteIdentifier)
    {
        super(alias, quoteIdentifier);
        partitionByFields = new ArrayList<>();
        orderByFields = new ArrayList<>();
    }

    public WindowFunction(String quoteIdentifier, Function function, List<Field> partitionByFields, List<OrderedField> orderByFields)
    {
        super(quoteIdentifier);
        this.function = function;
        this.partitionByFields = partitionByFields;
        this.orderByFields = orderByFields;
    }

    public WindowFunction(String alias, String quoteIdentifier, Function function, List<Field> partitionByFields, List<OrderedField> orderByFields)
    {
        super(alias, quoteIdentifier);
        this.function = function;
        this.partitionByFields = partitionByFields;
        this.orderByFields = orderByFields;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        genSqlWithoutAlias(builder);
        super.genSql(builder);
    }

    @Override
    public void genSqlWithoutAlias(StringBuilder builder) throws SqlDomException
    {
        function.genSqlWithoutAlias(builder);
        builder.append(WHITE_SPACE);
        builder.append(OVER);
        builder.append(WHITE_SPACE);

        builder.append(OPEN_PARENTHESIS);
        // Add Partition By
        if (partitionByFields != null && !partitionByFields.isEmpty())
        {
            builder.append(PARTITION_BY.get() + WHITE_SPACE);
            for (int ctr = 0; ctr < partitionByFields.size(); ctr++)
            {
                partitionByFields.get(ctr).genSqlWithoutAlias(builder);
                if (ctr < (partitionByFields.size() - 1))
                {
                    builder.append(COMMA);
                }
            }
        }

        if ((partitionByFields != null && !partitionByFields.isEmpty()) &&
                (orderByFields != null && !orderByFields.isEmpty()))
        {
            builder.append(WHITE_SPACE);
        }


        // Add Order by
        SqlGen.genSqlList(builder, orderByFields, ORDER_BY.get() + WHITE_SPACE, COMMA);
        builder.append(CLOSING_PARENTHESIS);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Function)
        {
            function = (Function) node;
        }
        if (node instanceof OrderedField)
        {
            orderByFields.add((OrderedField) node);
        }
        else if (node instanceof Field)
        {
            partitionByFields.add((Field) node);
        }
    }
}
