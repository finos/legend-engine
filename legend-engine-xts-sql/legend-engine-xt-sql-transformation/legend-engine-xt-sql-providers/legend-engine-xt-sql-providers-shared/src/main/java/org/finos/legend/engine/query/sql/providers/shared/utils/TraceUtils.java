// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.providers.shared.utils;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.block.procedure.Procedure;

import java.util.function.Function;
import java.util.function.Supplier;

public class TraceUtils
{
    private static final String PREFIX = "Legend SQL: ";

    public static void trace(String name, Procedure<Span> procedure)
    {
        Span span = GlobalTracer.get().buildSpan(PREFIX + name).start();

        try (Scope ignored = GlobalTracer.get().activateSpan(span))
        {
            procedure.accept(span);
        }
        finally
        {
            span.finish();
        }
    }

    public static <T> T trace(String name, Supplier<T> supplier)
    {

        Span span = GlobalTracer.get().buildSpan(PREFIX + name).start();

        try (Scope ignored = GlobalTracer.get().activateSpan(span))
        {
            return supplier.get();
        }
        finally
        {
            span.finish();
        }
    }

    public static <T> T trace(String name, Function<Span, T> supplier)
    {
        Span span = GlobalTracer.get().buildSpan(PREFIX + name).start();

        try (Scope ignored = GlobalTracer.get().activateSpan(span))
        {
            return supplier.apply(span);
        }
        finally
        {
            span.finish();
        }
    }
}