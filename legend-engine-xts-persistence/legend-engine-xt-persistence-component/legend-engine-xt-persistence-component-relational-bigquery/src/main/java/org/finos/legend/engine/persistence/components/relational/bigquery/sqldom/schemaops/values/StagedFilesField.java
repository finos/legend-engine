// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class StagedFilesField extends Value
{
    private String columnName;

    private DataType dataType;

    public StagedFilesField(String quoteIdentifier, String columnName, DataType datatype)
    {
        super(quoteIdentifier);
        this.columnName = columnName;
        this.dataType = datatype;
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
        builder.append(WHITE_SPACE);
        dataType.genSql(builder);
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
        if (dataType == null)
        {
            throw new SqlDomException("dataType is empty");
        }
    }
}
