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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.ColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.StringUtils;

import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class Column implements SqlGen
{
    private String columnName;
    private final DataType dataType;
    private final List<ColumnConstraint> columnConstraints;
    private final String quoteIdentifier;

    public Column(String columnName, DataType dataType, List<ColumnConstraint> columnConstraints, String quoteIdentifier)
    {
        this.columnName = columnName;
        this.dataType = dataType;
        this.columnConstraints = columnConstraints;
        this.quoteIdentifier = quoteIdentifier;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public void setColumnName(String columnName)
    {
        this.columnName = columnName;
    }

    public DataType getDataType()
    {
        return dataType;
    }

    public List<ColumnConstraint> getColumnConstraints()
    {
        return columnConstraints;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(SqlGenUtils.getQuotedField(columnName, quoteIdentifier) + WHITE_SPACE);
        dataType.genSql(builder);
        SqlGen.genSqlList(builder, columnConstraints, WHITE_SPACE, WHITE_SPACE);
    }

    public void genSqlWithNameOnly(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(SqlGenUtils.getQuotedField(columnName, quoteIdentifier));
    }

    @Override
    public void push(Object node)
    {
        if (node.getClass().equals(String.class))
        {
            columnName = (String) node;
        }
    }

    void validate() throws SqlDomException
    {
        if (StringUtils.empty(columnName))
        {
            throw new SqlDomException("Column name empty");
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        Column column = (Column) o;

        // Compare the column name
        if (!this.getColumnName().equals(column.getColumnName()))
        {
            return false;
        }

        // Compare the data type
        if (!this.getDataType().getClass().equals(column.getDataType().getClass()))
        {
            return false;
        }
        // todo: include the check for data length and scale in future

        // Compare constraints
        if (this.getColumnConstraints().size() != column.getColumnConstraints().size())
        {
            return false;
        }
        for (ColumnConstraint c : this.getColumnConstraints())
        {
            ColumnConstraint matchedColumnConstraint = column.getColumnConstraints().stream().filter(constraint -> constraint.getClass().equals(c.getClass())).findFirst().orElse(null);
            if (matchedColumnConstraint == null)
            {
                return false;
            }
        }

        return true;
    }

}
