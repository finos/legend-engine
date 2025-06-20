// Copyright 2025 Goldman Sachs
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
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.*;

public class ToTimestampFunction extends Value
{
    private Value column;
    private int scale;

    public ToTimestampFunction(Value column, String quoteIdentifier, int scale)
    {
        super(column.getAlias(), quoteIdentifier);
        this.column = column;
        this.scale = scale;
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
        //TO_TIMESTAMP_NTZ($1:%s::NUMBER, scale)
        builder.append(Clause.TO_TIMESTAMP_NTZ);
        builder.append(OPEN_PARENTHESIS);
        column.genSqlWithoutAlias(builder);
        builder.append(COLON);
        builder.append(COLON);
        builder.append("NUMBER");
        builder.append(COMMA);
        builder.append(WHITE_SPACE);
        builder.append(scale);
        builder.append(CLOSING_PARENTHESIS);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Value)
        {
            column = (Value) node;
        }
    }
}
