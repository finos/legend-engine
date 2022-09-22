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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Pair;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.NumericalValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.ASSIGNMENT_OPERATOR;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class MergeStatement implements DMLStatement
{
    private TableLike sourceTable;
    private TableLike targetTable;
    private Long matchedPairsSize;
    private final List<Pair<Field, Value>> setMatchedPairs;
    private final List<Pair<Field, Value>> setUnmatchedPairs;
    private Condition onCondition;
    private Condition matchedCondition;

    public MergeStatement()
    {
        setMatchedPairs = new ArrayList<>();
        setUnmatchedPairs = new ArrayList<>();
    }

    public MergeStatement(TableLike sourceTable,
                          TableLike targetTable,
                          Long matchedPairsSize,
                          List<Pair<Field, Value>> setMatchedPairs,
                          List<Pair<Field, Value>> setUnmatchedPairs,
                          Condition onCondition,
                          Condition matchedCondition)
    {
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.matchedPairsSize = matchedPairsSize;
        this.setMatchedPairs = setMatchedPairs;
        this.setUnmatchedPairs = setUnmatchedPairs;
        this.onCondition = onCondition;
        this.matchedCondition = matchedCondition;
    }

    /*
     MergeStatement GENERIC PLAN:
     MERGE INTO sourceTable USING targetTable ON (onCondition)
       WHEN MATCHED (AND matchedCondition) THEN
         UPDATE SET column1 = value1 [, column2 = value2 ...]
       WHEN NOT MATCHED THEN
         INSERT (column1 [, column2 ...]) VALUES (value1 [, value2 ...]);
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        builder.append(Clause.MERGE_INTO.get());

        // Add table name
        builder.append(WHITE_SPACE);
        sourceTable.genSql(builder);

        // Add USING
        builder.append(WHITE_SPACE + Clause.USING.get() + WHITE_SPACE);

        // Add table_reference
        targetTable.genSql(builder);

        // Add ON Clause
        if (onCondition != null)
        {
            builder.append(WHITE_SPACE + Clause.ON.get() + WHITE_SPACE);
            onCondition.genSql(builder);
        }

        // Add WHEN MATCHED
        builder.append(WHITE_SPACE + Clause.WHEN_MATCHED.get() + WHITE_SPACE);

        // Add matchedCondition Clause
        if (matchedCondition != null)
        {
            builder.append(Clause.AND.get() + WHITE_SPACE);
            matchedCondition.genSql(builder);
            builder.append(WHITE_SPACE);
        }

        // Add THEN UPDATE SET
        builder.append(Clause.THEN.get() +
            WHITE_SPACE + Clause.UPDATE.get() +
            WHITE_SPACE + Clause.SET.get() + WHITE_SPACE);

        // Add set Values with Assignment operator
        for (int ctr = 0; ctr < setMatchedPairs.size(); ctr++)
        {
            setMatchedPairs.get(ctr).getKey().genSql(builder);
            builder.append(WHITE_SPACE + ASSIGNMENT_OPERATOR + WHITE_SPACE);
            setMatchedPairs.get(ctr).getValue().genSql(builder);
            if (ctr < (setMatchedPairs.size() - 1))
            {
                builder.append(COMMA);
            }
        }

        // Add WHEN NOT MATCHED
        builder.append(WHITE_SPACE + Clause.WHEN_NOT_MATCHED.get() + WHITE_SPACE);

        // Add THEN INSERT
        builder.append(Clause.THEN.get() + WHITE_SPACE + Clause.INSERT.get() + WHITE_SPACE);

        List<Field> columns = setUnmatchedPairs.stream().map(pair -> pair.getKey()).collect(Collectors.toList());
        List<Value> values = setUnmatchedPairs.stream().map(pair -> pair.getValue()).collect(Collectors.toList());

        // Add column names
        StringBuilder columnsStringBuilder = new StringBuilder();
        for (int i = 0; i < columns.size(); i++)
        {
            columns.get(i).genSqlWithNameOnly(columnsStringBuilder);
            if (i < (columns.size() - 1))
            {
                columnsStringBuilder.append(COMMA + WHITE_SPACE);
            }
        }
        if (columns.size() > 0)
        {
            builder.append(String.format("(%s)", columnsStringBuilder));
        }

        // Add VALUES
        builder.append(WHITE_SPACE + Clause.VALUES.get() + WHITE_SPACE);

        // Add values
        if (values != null && values.size() > 0)
        {
            builder.append("(");
            for (int ctr = 0; ctr < values.size(); ctr++)
            {
                values.get(ctr).genSql(builder);
                if (ctr < (values.size() - 1))
                {
                    builder.append(COMMA);
                }
            }
            builder.append(")");
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof TableLike)
        {
            if (sourceTable == null)
            {
                sourceTable = (TableLike) node;
            }
            else
            {
                targetTable = (TableLike) node;
            }
        }
        else if (node instanceof Condition)
        {
            if (onCondition == null)
            {
                onCondition = (Condition) node;
            }
            else
            {
                matchedCondition = (Condition) node;
            }
        }
        else if (node instanceof NumericalValue)
        {
            if (matchedPairsSize == null)
            {
                matchedPairsSize = ((NumericalValue) node).getValue();
            }
        }
        else if (node instanceof Pair)
        {
            if (setMatchedPairs == null || setMatchedPairs.isEmpty() || setMatchedPairs.size() < matchedPairsSize)
            {
                setMatchedPairs.add((Pair) node);
            }
            else
            {
                setUnmatchedPairs.add((Pair) node);
            }
        }

    }
}
