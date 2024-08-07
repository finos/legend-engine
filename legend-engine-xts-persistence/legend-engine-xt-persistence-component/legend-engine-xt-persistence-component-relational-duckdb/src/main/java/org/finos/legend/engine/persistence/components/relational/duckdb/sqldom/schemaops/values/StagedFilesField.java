// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.duckdb.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;

public class StagedFilesField extends Value
{
    private String columnName;

    public StagedFilesField(String quoteIdentifier, String columnName)
    {
        super(quoteIdentifier);
        this.columnName = columnName;
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
        validate();
        builder.append(SqlGenUtils.getQuotedField(columnName, getQuoteIdentifier()));
    }

    @Override
    public void push(Object node)
    {
    }

    void validate() throws SqlDomException
    {
        if (columnName == null)
        {
            throw new SqlDomException("columnName is empty");
        }
    }
}
