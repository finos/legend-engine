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
//

package org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.tds;

public abstract class AbstractResultPathVisitor<T> implements ResultPathVisitor<T>
{
    protected abstract T defaultValue(ResultPath val);

    @Override
    public T visit(AggregateResultPath val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(DocValueResultPath val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(_IDResultPath val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(FieldResultPath val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(SourceFieldResultPath val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(DocCountAggregateResultPath val)
    {
        return defaultValue(val);
    }
}
