// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.perf;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tag;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.ThreadLocalScopeManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects the OpenTracing spans that plan generation emits - the Java "Generate Plan" span and the
 * Pure {@code traceSpan} calls in the router, the plan builder and relational SQL generation - and
 * reduces each iteration's spans to inclusive milliseconds and a call count per operation name.
 *
 * OpenTracing's MockTracer timestamps spans with currentTimeMillis, too coarse for spans that last
 * a fraction of a millisecond, so this registers its own tracer on System.nanoTime.
 */
public final class SpanCollector
{
    private static BenchTracer tracer;

    private SpanCollector()
    {
    }

    public static synchronized void install()
    {
        if (tracer == null)
        {
            BenchTracer candidate = new BenchTracer();
            if (!GlobalTracer.registerIfAbsent(candidate))
            {
                throw new IllegalStateException("Another tracer is already registered; spans cannot be collected");
            }
            tracer = candidate;
        }
    }

    public static void reset()
    {
        if (tracer != null)
        {
            tracer.drainFinished();
        }
    }

    /**
     * Routing recurses (graph fetch, let bindings and inlined functions route their own
     * sub-expressions), so a span can sit inside another span of the same name. Only the outermost
     * one counts towards that name's time, otherwise nested work is added twice.
     */
    public static Map<String, SpanStats> drain()
    {
        Map<String, SpanStats> result = new TreeMap<>();
        if (tracer == null)
        {
            return result;
        }
        for (BenchSpan span : tracer.drainFinished())
        {
            SpanStats stats = result.computeIfAbsent(span.name, k -> new SpanStats());
            stats.count++;
            if (!span.hasAncestorNamed(span.name))
            {
                stats.nanos += span.endNanos - span.startNanos;
            }
        }
        return result;
    }

    public static final class SpanStats
    {
        public long nanos;
        public int count;

        public double millis()
        {
            return nanos / 1_000_000.0;
        }
    }

    private static final class BenchTracer implements Tracer
    {
        private final ScopeManager scopeManager = new ThreadLocalScopeManager();
        private final AtomicLong ids = new AtomicLong();
        private List<BenchSpan> finished = new ArrayList<>();

        synchronized void record(BenchSpan span)
        {
            this.finished.add(span);
        }

        synchronized List<BenchSpan> drainFinished()
        {
            List<BenchSpan> spans = this.finished;
            this.finished = new ArrayList<>();
            return spans;
        }

        @Override
        public ScopeManager scopeManager()
        {
            return this.scopeManager;
        }

        @Override
        public Span activeSpan()
        {
            return this.scopeManager.activeSpan();
        }

        @Override
        public Scope activateSpan(Span span)
        {
            return this.scopeManager.activate(span);
        }

        @Override
        public SpanBuilder buildSpan(String operationName)
        {
            return new BenchSpanBuilder(this, operationName);
        }

        @Override
        public <C> void inject(SpanContext spanContext, Format<C> format, C carrier)
        {
        }

        @Override
        public <C> SpanContext extract(Format<C> format, C carrier)
        {
            return null;
        }

        @Override
        public void close()
        {
        }
    }

    private static final class BenchSpanBuilder implements Tracer.SpanBuilder
    {
        private final BenchTracer tracer;
        private final String name;
        private BenchSpan parent;
        private boolean ignoreActive;

        BenchSpanBuilder(BenchTracer tracer, String name)
        {
            this.tracer = tracer;
            this.name = name;
        }

        @Override
        public Tracer.SpanBuilder asChildOf(SpanContext parent)
        {
            if (parent instanceof BenchSpan)
            {
                this.parent = (BenchSpan) parent;
            }
            return this;
        }

        @Override
        public Tracer.SpanBuilder asChildOf(Span parent)
        {
            return parent == null ? this : this.asChildOf(parent.context());
        }

        @Override
        public Tracer.SpanBuilder addReference(String referenceType, SpanContext referencedContext)
        {
            return this.asChildOf(referencedContext);
        }

        @Override
        public Tracer.SpanBuilder ignoreActiveSpan()
        {
            this.ignoreActive = true;
            return this;
        }

        @Override
        public Tracer.SpanBuilder withTag(String key, String value)
        {
            return this;
        }

        @Override
        public Tracer.SpanBuilder withTag(String key, boolean value)
        {
            return this;
        }

        @Override
        public Tracer.SpanBuilder withTag(String key, Number value)
        {
            return this;
        }

        @Override
        public <T> Tracer.SpanBuilder withTag(Tag<T> tag, T value)
        {
            return this;
        }

        @Override
        public Tracer.SpanBuilder withStartTimestamp(long microseconds)
        {
            return this;
        }

        @Override
        @Deprecated
        public Span startManual()
        {
            return this.start();
        }

        @Override
        public Span start()
        {
            BenchSpan resolvedParent = this.parent;
            if (resolvedParent == null && !this.ignoreActive)
            {
                Span active = this.tracer.activeSpan();
                if (active instanceof BenchSpan)
                {
                    resolvedParent = (BenchSpan) active;
                }
            }
            return new BenchSpan(this.tracer, this.tracer.ids.incrementAndGet(), this.name, resolvedParent);
        }

        @Override
        @Deprecated
        public Scope startActive(boolean finishSpanOnClose)
        {
            return this.tracer.scopeManager.activate(this.start(), finishSpanOnClose);
        }
    }

    private static final class BenchSpan implements Span, SpanContext
    {
        private final BenchTracer tracer;
        private final long id;
        private final BenchSpan parent;
        private final long startNanos = System.nanoTime();
        private String name;
        private long endNanos;

        BenchSpan(BenchTracer tracer, long id, String name, BenchSpan parent)
        {
            this.tracer = tracer;
            this.id = id;
            this.name = name;
            this.parent = parent;
        }

        boolean hasAncestorNamed(String operationName)
        {
            for (BenchSpan p = this.parent; p != null; p = p.parent)
            {
                if (p.name.equals(operationName))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public SpanContext context()
        {
            return this;
        }

        @Override
        public String toTraceId()
        {
            return "bench";
        }

        @Override
        public String toSpanId()
        {
            return Long.toString(this.id);
        }

        @Override
        public Iterable<Map.Entry<String, String>> baggageItems()
        {
            return Collections.emptyList();
        }

        @Override
        public Span setTag(String key, String value)
        {
            return this;
        }

        @Override
        public Span setTag(String key, boolean value)
        {
            return this;
        }

        @Override
        public Span setTag(String key, Number value)
        {
            return this;
        }

        @Override
        public <T> Span setTag(Tag<T> tag, T value)
        {
            return this;
        }

        @Override
        public Span log(Map<String, ?> fields)
        {
            return this;
        }

        @Override
        public Span log(long timestampMicroseconds, Map<String, ?> fields)
        {
            return this;
        }

        @Override
        public Span log(String event)
        {
            return this;
        }

        @Override
        public Span log(long timestampMicroseconds, String event)
        {
            return this;
        }

        @Override
        public Span setBaggageItem(String key, String value)
        {
            return this;
        }

        @Override
        public String getBaggageItem(String key)
        {
            return null;
        }

        @Override
        public Span setOperationName(String operationName)
        {
            this.name = operationName;
            return this;
        }

        @Override
        public void finish()
        {
            this.endNanos = System.nanoTime();
            this.tracer.record(this);
        }

        @Override
        public void finish(long finishMicros)
        {
            this.finish();
        }
    }
}
