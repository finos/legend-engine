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

package org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schemaops.statements;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.select.SelectExpression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.ASSIGNMENT_OPERATOR;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class SelectFromFileStatement extends SelectExpression
{
    private final List<Value> columns;
    private Map<String, String> loadOptions;

    public SelectFromFileStatement()
    {
        columns = new ArrayList<>();
    }

    /*
     Select from file GENERIC PLAN for Big Query:
        (COLUMN_LIST)
        FROM FILES (LOAD_OPTIONS)
     */
    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();

        builder.append(OPEN_PARENTHESIS);
        SqlGen.genSqlList(builder, columns, WHITE_SPACE, COMMA);
        builder.append(CLOSING_PARENTHESIS);

        builder.append(WHITE_SPACE);
        builder.append(Clause.FROM_FILES.get());
        builder.append(WHITE_SPACE);

        if (loadOptions != null && loadOptions.size() > 0)
        {
            builder.append(OPEN_PARENTHESIS);

            int counter = 0;
            for (String option : loadOptions.keySet())
            {
                counter++;
                builder.append(option);
                builder.append(ASSIGNMENT_OPERATOR);
                builder.append(SqlGenUtils.singleQuote(loadOptions.get(option)));
                if (counter < columns.size())
                {
                    builder.append(COMMA + WHITE_SPACE);
                }
            }
            builder.append(CLOSING_PARENTHESIS);
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Value)
        {
            columns.add((Value) node);
        }
        if (node instanceof Map)
        {
            loadOptions = (Map<String, String>) node;
        }
    }

    void validate() throws SqlDomException
    {
        if (!loadOptions.containsKey("uris"))
        {
            throw new SqlDomException("uris are mandatory for loading from files");
        }
        if (!loadOptions.containsKey("format"))
        {
            throw new SqlDomException("format is mandatory for loading from files");
        }
    }
}
