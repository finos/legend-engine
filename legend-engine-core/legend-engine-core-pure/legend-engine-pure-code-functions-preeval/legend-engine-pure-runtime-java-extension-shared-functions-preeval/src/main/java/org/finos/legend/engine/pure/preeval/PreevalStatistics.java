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

package org.finos.legend.engine.pure.preeval;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public final class PreevalStatistics
{
    public static final String SYSTEM_PROPERTY = "legend.engine.preeval.statistics";

    private static final ConcurrentMap<String, LongAdder> RULE_COUNTS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, LongAdder> HOOK_CALLS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, LongAdder> HOOK_NANOS = new ConcurrentHashMap<>();

    private PreevalStatistics()
    {
    }

    public static boolean enabled()
    {
        return Boolean.getBoolean(SYSTEM_PROPERTY);
    }

    public static PrevalHooks instrument(PrevalHooks hooks)
    {
        return enabled() ? new CountingPrevalHooks(hooks) : hooks;
    }

    public static void ruleApplied(Object rule)
    {
        if (enabled())
        {
            add(RULE_COUNTS, rule.getClass().getSimpleName(), 1);
        }
    }

    static void hookCalled(String hook, long nanos)
    {
        add(HOOK_CALLS, hook, 1);
        add(HOOK_NANOS, hook, nanos);
    }

    public static Map<String, Long> ruleCounts()
    {
        return snapshot(RULE_COUNTS);
    }

    public static Map<String, Long> hookCallCounts()
    {
        return snapshot(HOOK_CALLS);
    }

    public static Map<String, Long> hookNanos()
    {
        return snapshot(HOOK_NANOS);
    }

    public static void reset()
    {
        RULE_COUNTS.clear();
        HOOK_CALLS.clear();
        HOOK_NANOS.clear();
    }

    private static void add(ConcurrentMap<String, LongAdder> counters, String key, long amount)
    {
        counters.computeIfAbsent(key, k -> new LongAdder()).add(amount);
    }

    private static Map<String, Long> snapshot(ConcurrentMap<String, LongAdder> counters)
    {
        Map<String, Long> result = new TreeMap<>();
        counters.forEach((key, value) -> result.put(key, value.sum()));
        return Collections.unmodifiableMap(result);
    }
}
