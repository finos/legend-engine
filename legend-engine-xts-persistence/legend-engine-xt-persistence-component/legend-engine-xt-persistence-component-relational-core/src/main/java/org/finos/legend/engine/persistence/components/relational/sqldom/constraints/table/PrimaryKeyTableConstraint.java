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

package org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;

import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause.NOT_ENFORCED;

public class PrimaryKeyTableConstraint extends TableConstraint
{
    private final List<String> columnNames;
    private final String quoteIdentifier;

    private boolean notEnforced;

    public PrimaryKeyTableConstraint(List<String> columnNames, String quoteIdentifier)
    {
        this.columnNames = columnNames;
        this.quoteIdentifier = quoteIdentifier;
        this.notEnforced = false;
    }

    public PrimaryKeyTableConstraint(List<String> columnNames, String quoteIdentifier, boolean notEnforced)
    {
        this.columnNames = columnNames;
        this.quoteIdentifier = quoteIdentifier;
        this.notEnforced = notEnforced;
    }

    public List<String> getColumnNames()
    {
        return columnNames;
    }

    public PrimaryKeyTableConstraint withColumnNames(List<String> columnNames)
    {
        return new PrimaryKeyTableConstraint(columnNames, quoteIdentifier, notEnforced);
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        String primaryKeys = columnNames.stream().map(column -> SqlGenUtils.getQuotedField(column, quoteIdentifier)).collect(Collectors.joining(SqlGenUtils.COMMA + SqlGenUtils.WHITE_SPACE));
        builder.append(String.format("PRIMARY KEY (%s)", primaryKeys));
        if (notEnforced)
        {
            builder.append(" " + NOT_ENFORCED.get());
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof String)
        {
            columnNames.add((String) node);
        }
    }

    void validate() throws SqlDomException
    {
        if (columnNames == null || columnNames.isEmpty())
        {
            throw new SqlDomException("columnNames is empty");
        }
    }
}
