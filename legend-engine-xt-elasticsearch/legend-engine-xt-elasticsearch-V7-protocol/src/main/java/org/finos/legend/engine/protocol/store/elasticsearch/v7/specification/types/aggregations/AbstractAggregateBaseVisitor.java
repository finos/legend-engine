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

public abstract class AbstractAggregateBaseVisitor<T> implements AggregateBaseVisitor<T>
{
    protected abstract T defaultValue(AggregateBase val);

    @Override
    public T visit(AdjacencyMatrixAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(AutoDateHistogramAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(AvgAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(BoxPlotAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(BucketMetricValueAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(CardinalityAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ChildrenAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(CompositeAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(CumulativeCardinalityAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DateHistogramAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DateRangeAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DerivativeAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(DoubleTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ExtendedStatsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ExtendedStatsBucketAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(FilterAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(FiltersAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(GeoBoundsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(GeoCentroidAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(GeoDistanceAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(GeoHashGridAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(GeoLineAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(GeoTileGridAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(GlobalAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(HdrPercentileRanksAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(HdrPercentilesAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(HistogramAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(InferenceAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(IpRangeAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(LongRareTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(LongTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(MatrixStatsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(MaxAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(MedianAbsoluteDeviationAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(MinAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(MissingAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(MultiBucketAggregateBase val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(MultiTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(NestedAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ParentAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(PercentilesAggregateBase val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(PercentilesBucketAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(RangeAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(RateAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ReverseNestedAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(SamplerAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ScriptedMetricAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(SignificantLongTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(SignificantStringTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(SignificantTermsAggregateBase val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(SimpleValueAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(SingleBucketAggregateBase val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(SingleMetricAggregateBase val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(StatsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(StatsBucketAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(StringRareTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(StringStatsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(StringTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(SumAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(TDigestPercentileRanksAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(TDigestPercentilesAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(TTestAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(TermsAggregateBase val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(TopHitsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(TopMetricsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(UnmappedRareTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(UnmappedSamplerAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(UnmappedSignificantTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(UnmappedTermsAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(ValueCountAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(VariableWidthHistogramAggregate val)
    {
        return this.defaultValue(val);
    }

    @Override
    public T visit(WeightedAvgAggregate val)
    {
        return this.defaultValue(val);
    }
}
