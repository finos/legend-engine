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

public class NumericalValue extends Value
{
    private Long value;

    public NumericalValue(Long value, String quoteIdentifier)
    {
        super(quoteIdentifier);
        this.value = value;
    }

    public NumericalValue(Long value, String alias, String quoteIdentifier)
    {
        super(alias, quoteIdentifier);
        this.value = value;
    }

    public Long getValue()
    {
        return value;
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
        builder.append(value);
    }

    void validate() throws SqlDomException
    {
        if (value == null)
        {
            throw new SqlDomException("NumericalValue is null");
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Long)
        {
            value = (Long) node;
        }
    }
}
