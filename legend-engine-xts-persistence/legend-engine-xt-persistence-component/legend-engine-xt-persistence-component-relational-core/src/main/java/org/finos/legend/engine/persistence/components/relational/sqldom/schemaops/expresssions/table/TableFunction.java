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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table;

import java.util.List;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.StringUtils;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;

public class TableFunction extends TableLike
{
    public static final String TABLE = "TABLE";
    private final String db;
    private final String schema;
    private String name;
    private final List<Value> values;
    private final String quoteIdentifier;

    public TableFunction(String db, String schema, String name, List<Value> values, String quoteIdentifier)
    {
        this.db = db;
        this.schema = schema;
        this.name = name;
        this.values = values;
        this.quoteIdentifier = quoteIdentifier;
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
        if (StringUtils.notEmpty(db))
        {
            builder.append(String.format("%s.", SqlGenUtils.getQuotedField(db, quoteIdentifier)));
        }

        if (StringUtils.notEmpty(schema))
        {
            builder.append(String.format("%s.", SqlGenUtils.getQuotedField(schema, quoteIdentifier)));
        }

        builder.append(TABLE);
        builder.append(OPEN_PARENTHESIS);

        builder.append(name);
        builder.append(OPEN_PARENTHESIS);
        if (values != null)
        {
            for (int ctr = 0; ctr < values.size(); ctr++)
            {
                values.get(ctr).genSqlWithoutAlias(builder);
                if (ctr < (values.size() - 1))
                {
                    builder.append(COMMA);
                }
            }
        }
        builder.append(CLOSING_PARENTHESIS);
        builder.append(CLOSING_PARENTHESIS);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof String)
        {
            name = (String) node;
        }
        else if (node instanceof Value)
        {
            values.add((Value) node);
        }
    }
}
