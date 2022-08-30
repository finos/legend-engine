//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.shared.core.operational.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import java.util.function.Supplier;

/**
 * Consider tracing the executor service rather than the supplier via opentracing-contrib/java-concurrent
 */
public class TracedSupplier<T> implements Supplier<T>
{
    private final Supplier<T> delegate;
    private final Tracer tracer;
    private final Span span;

    public TracedSupplier(Supplier<T> delegate, Tracer tracer)
    {
        this.delegate = delegate;
        this.tracer = tracer;
        this.span = tracer.activeSpan();
    }

    @Override
    public T get()
    {
        try (Scope ignored = span == null ? null : tracer.activateSpan(span))
        {
            return delegate.get();
        }
    }

    /**
     * Wrap a supplier such that it can be executed on another thread whilst re-activating the
     * current active span at the time of creation
     * <p>
     * The active span is taken from the {@link io.opentracing.util.GlobalTracer}
     *
     * @param work work to decorate
     */
    public static <T> TracedSupplier<T> reActivateSpan(Supplier<T> work)
    {
        return reActivateSpan(work, GlobalTracer.get());
    }

    /**
     * Wrap a supplier such that it can be executed on another thread whilst re-activating the
     * current active span at the time of creation
     *
     * @param work   work to decorate
     * @param tracer tracer used ti retrieve the active span, and re-activate a span
     */
    public static <T> TracedSupplier<T> reActivateSpan(Supplier<T> work, Tracer tracer)
    {
        return new TracedSupplier<>(work, tracer);
    }
}
