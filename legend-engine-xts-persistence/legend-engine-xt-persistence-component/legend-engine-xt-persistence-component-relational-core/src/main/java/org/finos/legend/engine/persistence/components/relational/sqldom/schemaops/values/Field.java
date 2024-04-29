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
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.StringUtils;

public class Field extends Value
{
    private String name;
    private String datasetReferenceAlias;

    public Field(String name, String quoteIdentifier)
    {
        super(quoteIdentifier);
        this.name = name;
    }

    public Field(String datasetReferenceAlias, String name, String quoteIdentifier, String alias)
    {
        super(alias, quoteIdentifier);
        this.datasetReferenceAlias = datasetReferenceAlias;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
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
        if (datasetReferenceAlias != null)
        {
            builder.append(datasetReferenceAlias).append(".");
        }
        builder.append(SqlGenUtils.getQuotedField(name, getQuoteIdentifier()));
    }

    public void genSqlWithNameOnly(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(SqlGenUtils.getQuotedField(name, getQuoteIdentifier()));
    }

    @Override
    public void push(Object node)
    {
    }

    void validate() throws SqlDomException
    {
        if (StringUtils.empty(name))
        {
            throw new SqlDomException("Field name is empty");
        }
    }
}
