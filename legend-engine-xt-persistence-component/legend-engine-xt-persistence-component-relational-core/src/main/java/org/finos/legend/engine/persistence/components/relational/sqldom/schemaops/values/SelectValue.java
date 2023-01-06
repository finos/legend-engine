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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.SelectStatement;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;

public class SelectValue extends Value
{
    private SelectStatement selectStatement;

    public SelectValue(String quoteIdentifier)
    {
        super(quoteIdentifier);
    }

    public SelectValue(String alias, String quoteIdentifier)
    {
        super(alias, quoteIdentifier);
    }

    public SelectValue(SelectStatement selectStatement, String quoteIdentifier)
    {
        super(quoteIdentifier);
        this.selectStatement = selectStatement;
    }

    public SelectValue(SelectStatement selectStatement, String alias, String quoteIdentifier)
    {
        super(alias, quoteIdentifier);
        this.selectStatement = selectStatement;
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
        builder.append(OPEN_PARENTHESIS);
        selectStatement.genSql(builder);
        builder.append(CLOSING_PARENTHESIS);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof SelectStatement)
        {
            selectStatement = (SelectStatement) node;
        }
    }
}