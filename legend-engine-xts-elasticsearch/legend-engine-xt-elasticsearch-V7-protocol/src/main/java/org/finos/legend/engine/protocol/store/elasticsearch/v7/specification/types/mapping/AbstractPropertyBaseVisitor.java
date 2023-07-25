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

package org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping;

public abstract class AbstractPropertyBaseVisitor<T> implements PropertyBaseVisitor<T>
{
    protected abstract T defaultValue(PropertyBase val);
    
    @Override
    public T visit(AggregateMetricDoubleProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(BinaryProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(BooleanProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ByteNumberProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(CompletionProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ConstantKeywordProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(CorePropertyBase val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DateNanosProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DateProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DateRangeProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DenseVectorProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DocValuesPropertyBase val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DoubleNumberProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DoubleRangeProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DynamicProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(FieldAliasProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(FlattenedProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(FloatNumberProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(FloatRangeProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(GeoPointProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(GeoShapeProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(HalfFloatNumberProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(HistogramProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(IntegerNumberProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(IntegerRangeProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(IpProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(IpRangeProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(JoinProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(KeywordProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(LongNumberProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(LongRangeProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(Murmur3HashProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(NestedProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(NumberPropertyBase val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ObjectProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(PercolatorProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(PointProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(RangePropertyBase val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(RankFeatureProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(RankFeaturesProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ScaledFloatNumberProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(SearchAsYouTypeProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ShapeProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ShortNumberProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(StandardNumberProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(TextProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(TokenCountProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(UnsignedLongNumberProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(VersionProperty val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(WildcardProperty val)
    {
        return this.defaultValue(val);
    }
}
