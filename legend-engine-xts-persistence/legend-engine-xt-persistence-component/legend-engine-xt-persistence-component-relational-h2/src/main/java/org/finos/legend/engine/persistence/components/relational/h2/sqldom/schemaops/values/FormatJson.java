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

package org.finos.legend.engine.persistence.components.relational.h2.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

public class FormatJson extends Value
{
    private StringValue jsonString;
    private static final String JSON_FORMAT = "JSON ";

    public FormatJson(String quoteIdentifier)
    {
        super(quoteIdentifier);
    }

    public FormatJson(StringValue jsonString, String quoteIdentifier)
    {
        super(quoteIdentifier);
        this.jsonString = jsonString;
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
        builder.append(JSON_FORMAT);
        jsonString.genSqlWithoutAlias(builder);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Value)
        {
            jsonString = (StringValue) node;
        }
    }
}