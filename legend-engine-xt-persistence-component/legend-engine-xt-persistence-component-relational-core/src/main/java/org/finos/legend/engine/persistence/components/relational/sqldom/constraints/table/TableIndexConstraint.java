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
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;

import java.util.List;
import java.util.stream.Collectors;

public class TableIndexConstraint extends TableConstraint
{
    private final List<String> columnNames;
    private final String indexName;
    private final String quoteIdentifier;

    public TableIndexConstraint(List<String> columnNames, String indexName, String quoteIdentifier)
    {
        this.columnNames = columnNames;
        this.indexName = indexName;
        this.quoteIdentifier = quoteIdentifier;
    }

    public List<String> getColumnNames()
    {
        return columnNames;
    }

    public String getIndexName()
    {
        return indexName;
    }

    public TableIndexConstraint withColumnNamesAndIndexName(List<String> columnNames, String indexName)
    {
        return new TableIndexConstraint(columnNames, indexName, quoteIdentifier);
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        String columns = columnNames.stream().map(column -> SqlGenUtils.getQuotedField(column, quoteIdentifier)).collect(Collectors.joining(SqlGenUtils.COMMA + SqlGenUtils.WHITE_SPACE));
        builder.append(String.format(Clause.INDEX.get() + " %s (%s)", SqlGenUtils.getQuotedField(indexName, quoteIdentifier), columns));
    }

    void validate() throws SqlDomException
    {
        if (columnNames == null || columnNames.isEmpty())
        {
            throw new SqlDomException("columnNames is empty");
        }
    }
}
