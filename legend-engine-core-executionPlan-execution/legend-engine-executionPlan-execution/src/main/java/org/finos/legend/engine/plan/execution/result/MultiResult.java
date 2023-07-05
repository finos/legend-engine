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

package org.finos.legend.engine.plan.execution.result;

import org.eclipse.collections.impl.factory.Maps;

import java.util.Map;

public class MultiResult extends Result
{
    private Map<String, Result> subResults;

    public MultiResult(Map<String, Result> subResults)
    {
        super("success");
        this.subResults = Maps.mutable.empty();
        subResults.forEach((key, value) -> this.subResults.put(key, value));
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return resultVisitor.visit(this);
    }

    public Map<String, Result> getSubResults()
    {
        return subResults;
    }

    @Override
    public Result realizeInMemory()
    {
        Map<String, Result> realizedSubResults = Maps.mutable.empty();
        this.subResults.forEach((key, value) -> realizedSubResults.put(key, value.realizeInMemory()));
        return new MultiResult(realizedSubResults);
    }
}
