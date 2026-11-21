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
import io.opentracing.Span;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSpanTimer
{
    @Test
    public void testFinishedSpansAreCountedAndTimedByOperation() throws Exception
    {
        SpanTimer timer = new SpanTimer();
        Span span = timer.buildSpan("preval").start();
        Thread.sleep(5);
        span.finish();
        timer.buildSpan("other").start().finish();
        Assertions.assertEquals(1, timer.count("preval"));
        Assertions.assertTrue(timer.totalNanos("preval") >= 5_000_000L, String.valueOf(timer.totalNanos("preval")));
        Assertions.assertEquals(1, timer.count("other"));
        Assertions.assertEquals(0, timer.count("absent"));
        Assertions.assertEquals(0, timer.totalNanos("absent"));
    }

    @Test
    public void testNestedSameNameSpansCountOnce()
    {
        SpanTimer timer = new SpanTimer();
        Span outer = timer.buildSpan("preval").start();
        Span inner = timer.buildSpan("preval").start();
        inner.finish();
        outer.finish();
        Assertions.assertEquals(1, timer.count("preval"));
    }

    @Test
    public void testDepthIsPerThread() throws Exception
    {
        SpanTimer timer = new SpanTimer();
        Span outer = timer.buildSpan("preval").start();
        Thread other = new Thread(() -> timer.buildSpan("preval").start().finish());
        other.start();
        other.join();
        outer.finish();
        Assertions.assertEquals(2, timer.count("preval"));
    }

    @Test
    public void testActivatedSpanIsActiveUntilItsScopeCloses()
    {
        SpanTimer timer = new SpanTimer();
        Span span = timer.buildSpan("preval").start();
        try (Scope ignored = timer.activateSpan(span))
        {
            Assertions.assertSame(span, timer.activeSpan());
        }
        Assertions.assertNull(timer.activeSpan());
        span.finish();
    }

    @Test
    public void testResetClearsTotals()
    {
        SpanTimer timer = new SpanTimer();
        timer.buildSpan("preval").start().finish();
        timer.reset();
        Assertions.assertEquals(0, timer.count("preval"));
        Assertions.assertEquals(0, timer.totalNanos("preval"));
    }
}
