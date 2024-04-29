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
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.StringUtils;

public abstract class Value implements SqlGen
{
    private String alias;
    private String quoteIdentifier;

    protected Value(String quoteIdentifier)
    {
        this.quoteIdentifier = quoteIdentifier;
    }

    protected Value(String alias, String quoteIdentifier)
    {
        this.alias = alias;
        this.quoteIdentifier = quoteIdentifier;
    }

    public String getQuoteIdentifier()
    {
        return quoteIdentifier;
    }

    public String getAlias()
    {
        return alias;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        if (StringUtils.notEmpty(alias))
        {
            builder.append(" as ").append(SqlGenUtils.getQuotedField(alias, quoteIdentifier));
        }
    }

    public abstract void genSqlWithoutAlias(StringBuilder builder) throws SqlDomException;
}
