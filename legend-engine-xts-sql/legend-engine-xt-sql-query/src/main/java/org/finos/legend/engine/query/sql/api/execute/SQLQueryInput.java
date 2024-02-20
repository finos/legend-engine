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

package org.finos.legend.engine.query.sql.api.execute;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.sql.metamodel.Query;

import java.util.List;

public class SQLQueryInput
{
    private final Query query;
    private final String sql;
    private final List<Object> positionalArguments = FastList.newList();

    public SQLQueryInput(@JsonProperty("query") Query query, @JsonProperty("sql") String sql, @JsonProperty("positionalArguments") List<Object> positionalArguments)
    {
        this.query = query;
        this.sql = sql;

        if (query != null && sql != null)
        {
            throw new IllegalArgumentException("only sql string or query must be defined, not both");
        }

        if (query == null && sql == null)
        {
            throw new IllegalArgumentException("sql or query must be defined");
        }

        if (positionalArguments != null)
        {
            this.positionalArguments.addAll(positionalArguments);
        }
    }

    public Query getQuery()
    {
        return query;
    }

    public String getSql()
    {
        return sql;
    }

    public List<Object> getPositionalArguments()
    {
        return positionalArguments;
    }
}
