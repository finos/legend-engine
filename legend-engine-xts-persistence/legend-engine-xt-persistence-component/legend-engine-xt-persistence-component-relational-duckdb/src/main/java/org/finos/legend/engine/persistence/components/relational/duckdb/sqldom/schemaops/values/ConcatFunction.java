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

package org.finos.legend.engine.persistence.components.relational.duckdb.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import java.util.ArrayList;
import java.util.List;

public class ConcatFunction extends Value
{
    private List<Value> values;

    public ConcatFunction(String quoteIdentifier)
    {
        super(quoteIdentifier);
        this.values = new ArrayList<>();
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
        for (int ctr = 0; ctr < values.size(); ctr++)
        {
            values.get(ctr).genSqlWithoutAlias(builder);
            if (ctr < (values.size() - 1))
            {
                builder.append("||");
            }
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Value)
        {
            values.add((Value) node);
        }
    }
}
