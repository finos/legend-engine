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

package org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

public class MetadataRowNumberValue extends Value
{
    private int startingRowNumber = 1;

    public MetadataRowNumberValue(String quoteIdentifier)
    {
        super(quoteIdentifier);
    }

    public MetadataRowNumberValue(String quoteIdentifier, int startingRowNumber)
    {
        super(quoteIdentifier);
        this.startingRowNumber = startingRowNumber;
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
        builder.append("METADATA$FILE_ROW_NUMBER");
        if (startingRowNumber != 1)
        {
            int offset = 1 - startingRowNumber;
            builder.append(String.format(" + %d", offset)); // This is to standardize such that row numbers start from 1
        }
    }
}
