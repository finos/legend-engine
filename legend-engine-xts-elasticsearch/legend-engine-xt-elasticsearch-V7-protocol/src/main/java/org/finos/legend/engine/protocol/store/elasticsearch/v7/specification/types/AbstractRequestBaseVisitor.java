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

package org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types;

import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.bulk.BulkRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.closepointintime.ClosePointInTimeRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.count.CountRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.index.IndexRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.openpointintime.OpenPointInTimeRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.global.search.SearchRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.create.CreateRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.delete.DeleteRequest;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.indices.get.GetRequest;

public abstract class AbstractRequestBaseVisitor<T> implements RequestBaseVisitor<T>
{
    protected abstract T defaultValue(RequestBase val);

    @Override
    public T visit(BulkRequest val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(ClosePointInTimeRequest val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(CountRequest val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(CreateRequest val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(DeleteRequest val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(GetRequest val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(IndexRequest val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(OpenPointInTimeRequest val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(SearchRequest val)
    {
        return defaultValue(val);
    }
}
