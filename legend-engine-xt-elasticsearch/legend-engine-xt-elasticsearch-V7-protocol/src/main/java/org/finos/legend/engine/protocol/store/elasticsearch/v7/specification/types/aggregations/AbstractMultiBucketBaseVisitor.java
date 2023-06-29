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

package org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.aggregations;

public abstract class AbstractMultiBucketBaseVisitor<T> implements MultiBucketBaseVisitor<T>
{
    protected abstract T defaultValue(MultiBucketBase val);

    @Override
    public T visit(AdjacencyMatrixBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(CompositeBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(DateHistogramBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(DoubleTermsBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(FiltersBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(GeoHashGridBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(GeoTileGridBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(HistogramBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(IpRangeBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(LongRareTermsBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(LongTermsBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(MultiTermsBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(RangeBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(SignificantLongTermsBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(SignificantStringTermsBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(SignificantTermsBucketBase val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(StringRareTermsBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(StringTermsBucket val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(TermsBucketBase val)
    {
        return defaultValue(val);
    }

    @Override
    public T visit(VariableWidthHistogramBucket val)
    {
        return defaultValue(val);
    }
}
