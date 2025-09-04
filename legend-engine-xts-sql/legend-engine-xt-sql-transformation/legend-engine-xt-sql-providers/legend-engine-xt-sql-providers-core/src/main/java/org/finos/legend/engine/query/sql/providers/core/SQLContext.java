// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.providers.core;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.sql.metamodel.Node;

import java.util.List;

public class SQLContext
{
    private final Node query;
    private final List<Object> positionalArguments = FastList.newList();

    public SQLContext(Node query)
    {
        this(query, null);
    }

    public SQLContext(Node query, List<Object> positionalArguments)
    {
        this.query = query;

        if (positionalArguments != null)
        {
            this.positionalArguments.addAll(positionalArguments);
        }
    }

    public Node getQuery()
    {
        return query;
    }

    public List<Object> getPositionalArguments()
    {
        return positionalArguments;
    }
}
