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
import io.opentracing.util.ThreadLocalScopeManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public final class SpanTimer implements Tracer
{
    private static final SpanContext NO_CONTEXT = new SpanContext()
    {
        @Override
        public String toTraceId()
        {
            return "";
        }

        @Override
        public String toSpanId()
        {
            return "";
        }

        @Override
        public Iterable<Map.Entry<String, String>> baggageItems()
        {
            return Collections.emptyList();
        }
    };

    private final ScopeManager scopeManager = new ThreadLocalScopeManager();
    private final ConcurrentMap<String, LongAdder> nanos = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> counts = new ConcurrentHashMap<>();
    private final ThreadLocal<Map<String, Integer>> depths = ThreadLocal.withInitial(HashMap::new);

    public long totalNanos(String operation)
    {
        LongAdder total = this.nanos.get(operation);
        return total == null ? 0 : total.sum();
    }

    public long count(String operation)
    {
        LongAdder total = this.counts.get(operation);
        return total == null ? 0 : total.sum();
    }

    public void reset()
    {
        this.nanos.clear();
        this.counts.clear();
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
        return new Builder(operationName);
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

    private final class Builder implements SpanBuilder
    {
        private final String operation;

        private Builder(String operation)
        {
            this.operation = operation;
        }

        @Override
        public SpanBuilder asChildOf(SpanContext parent)
        {
            return this;
        }

        @Override
        public SpanBuilder asChildOf(Span parent)
        {
            return this;
        }

        @Override
        public SpanBuilder addReference(String referenceType, SpanContext referencedContext)
        {
            return this;
        }

        @Override
        public SpanBuilder ignoreActiveSpan()
        {
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, String value)
        {
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, boolean value)
        {
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, Number value)
        {
            return this;
        }

        @Override
        public <T> SpanBuilder withTag(Tag<T> tag, T value)
        {
            return this;
        }

        @Override
        public SpanBuilder withStartTimestamp(long microseconds)
        {
            return this;
        }

        @Override
        @Deprecated
        public Span startManual()
        {
            return start();
        }

        @Override
        public Span start()
        {
            return new TimedSpan(this.operation);
        }

        @Override
        @Deprecated
        public Scope startActive(boolean finishSpanOnClose)
        {
            return SpanTimer.this.scopeManager.activate(start(), finishSpanOnClose);
        }
    }

    private final class TimedSpan implements Span
    {
        private final String operation;
        private final boolean outermost;
        private final long startNanos;
        private boolean finished;

        private TimedSpan(String operation)
        {
            this.operation = operation;
            this.outermost = SpanTimer.this.depths.get().merge(operation, 1, Integer::sum) == 1;
            this.startNanos = System.nanoTime();
        }

        @Override
        public SpanContext context()
        {
            return NO_CONTEXT;
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
            return this;
        }

        @Override
        public void finish()
        {
            if (this.finished)
            {
                return;
            }
            this.finished = true;
            long elapsed = System.nanoTime() - this.startNanos;
            SpanTimer.this.depths.get().merge(this.operation, -1, (depth, change) -> depth + change == 0 ? null : depth + change);
            if (this.outermost)
            {
                SpanTimer.this.nanos.computeIfAbsent(this.operation, k -> new LongAdder()).add(elapsed);
                SpanTimer.this.counts.computeIfAbsent(this.operation, k -> new LongAdder()).increment();
            }
        }

        @Override
        public void finish(long finishMicros)
        {
            finish();
        }
    }
}
