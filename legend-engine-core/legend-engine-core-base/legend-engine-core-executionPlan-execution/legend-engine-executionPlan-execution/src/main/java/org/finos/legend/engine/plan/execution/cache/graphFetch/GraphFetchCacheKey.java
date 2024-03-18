// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.cache.graphFetch;

import java.io.Serializable;

public abstract class GraphFetchCacheKey implements Serializable
{
    public abstract String getStringIdentifier();

    protected abstract int hash();

    protected abstract boolean equivalent(Object other);

    @Override
    public int hashCode()
    {
        return this.hash();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof GraphFetchCacheKey))
        {
            return false;
        }

        return this.equivalent(other);
    }
}
