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

public class ForeignKeyTableConstraint extends TableConstraint
{
    private final List<String> columnNames;
    private final String quoteIdentifier;

    public ForeignKeyTableConstraint(List<String> columnNames, String quoteIdentifier)
    {
        this.columnNames = columnNames;
        this.quoteIdentifier = quoteIdentifier;
    }

    public List<String> getColumnNames()
    {
        return columnNames;
    }

    public ForeignKeyTableConstraint withColumnNames(List<String> columnNames)
    {
        return new ForeignKeyTableConstraint(columnNames, quoteIdentifier);
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        String fkColumns = columnNames.stream().map(column -> SqlGenUtils.getQuotedField(column, quoteIdentifier)).collect(Collectors.joining(SqlGenUtils.COMMA + SqlGenUtils.WHITE_SPACE));
        builder.append(String.format("FOREIGN KEY (%s)", fkColumns));
    }

    void validate() throws SqlDomException
    {
        if (columnNames == null || columnNames.isEmpty())
        {
            throw new SqlDomException("columnNames is empty");
        }
    }
}
