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

package org.finos.legend.engine.shared.core.operational.prometheus;

import io.prometheus.client.Collector;
import org.eclipse.collections.api.factory.Lists;
import zipkin2.reporter.InMemoryReporterMetrics;

import java.util.ArrayList;
import java.util.List;

public class TracingExports extends Collector
{
    private final InMemoryReporterMetrics tracingMetrics;

    public TracingExports(InMemoryReporterMetrics tracingMetrics)
    {
        this.tracingMetrics = tracingMetrics;
    }

    private static MetricFamilySamples.Sample sample(String name, long value)
    {
        return new MetricFamilySamples.Sample(name, Lists.fixedSize.empty(), Lists.fixedSize.empty(), value);
    }

    @Override
    public List<MetricFamilySamples> collect()
    {
        List<MetricFamilySamples.Sample> samples = new ArrayList<>(5);

        samples.add(sample("Total_spans", tracingMetrics.spans()));
        samples.add(sample("Total_span_bytes", tracingMetrics.spanBytes()));
        samples.add(sample("Spans_dropped", tracingMetrics.spansDropped()));
        samples.add(sample("Spans_queued", tracingMetrics.queuedSpans()));

        MetricFamilySamples familySamples = new MetricFamilySamples("Tracing", Type.COUNTER, "Metrics for OpenTracing", samples);
        return Lists.fixedSize.of(familySamples);
    }
}
