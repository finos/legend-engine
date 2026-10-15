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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPreevalStatistics
{
    private static final PrevalHooks FIXED = new PrevalHooks()
    {
        @Override
        public boolean stopPreeval(ListIterable<?> values)
        {
            return true;
        }

        @Override
        public boolean shouldInline(Object function)
        {
            return false;
        }

        @Override
        public boolean isGeneratedMilestoningProperty(Object function)
        {
            return true;
        }

        @Override
        public boolean isGetAllFunction(Object function)
        {
            return false;
        }

        @Override
        public ImmutableList<Object> resolveTdsSchema(ValueSpecification value, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars)
        {
            return Lists.immutable.with("schema");
        }
    };

    @AfterEach
    public void clear()
    {
        System.clearProperty(PreevalStatistics.SYSTEM_PROPERTY);
        PreevalStatistics.reset();
    }

    @Test
    public void testInstrumentLeavesHooksUnwrappedWhenDisabled()
    {
        Assertions.assertSame(FIXED, PreevalStatistics.instrument(FIXED));
    }

    @Test
    public void testInstrumentedHooksDelegateAndRecordEachCall()
    {
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PrevalHooks hooks = PreevalStatistics.instrument(FIXED);
        Assertions.assertNotSame(FIXED, hooks);

        Assertions.assertTrue(hooks.stopPreeval(Lists.immutable.empty()));
        Assertions.assertTrue(hooks.stopPreeval(Lists.immutable.empty()));
        Assertions.assertFalse(hooks.shouldInline("f"));
        Assertions.assertTrue(hooks.isGeneratedMilestoningProperty("f"));
        Assertions.assertFalse(hooks.isGetAllFunction("f"));
        Assertions.assertEquals(Lists.immutable.with("schema"), hooks.resolveTdsSchema(null, null));

        Assertions.assertEquals(Long.valueOf(2), PreevalStatistics.hookCallCounts().get("stopPreeval"));
        Assertions.assertEquals(Long.valueOf(1), PreevalStatistics.hookCallCounts().get("shouldInline"));
        Assertions.assertEquals(Long.valueOf(1), PreevalStatistics.hookCallCounts().get("isGeneratedMilestoningProperty"));
        Assertions.assertEquals(Long.valueOf(1), PreevalStatistics.hookCallCounts().get("isGetAllFunction"));
        Assertions.assertEquals(Long.valueOf(1), PreevalStatistics.hookCallCounts().get("resolveTdsSchema"));
        Assertions.assertEquals(PreevalStatistics.hookCallCounts().keySet(), PreevalStatistics.hookNanos().keySet());
        Assertions.assertTrue(PreevalStatistics.hookNanos().values().stream().allMatch(n -> n >= 0));
    }

    @Test
    public void testRuleApplicationsAreIgnoredWhenDisabled()
    {
        PreevalStatistics.ruleApplied(Rules.REACTIVATE);
        Assertions.assertTrue(PreevalStatistics.ruleCounts().isEmpty());
    }

    @Test
    public void testRuleApplicationsAreCountedBySimpleClassName()
    {
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PreevalStatistics.ruleApplied(Rules.REACTIVATE);
        PreevalStatistics.ruleApplied(Rules.REACTIVATE);
        PreevalStatistics.ruleApplied(Rules.EXPANSION.getFirst());
        Assertions.assertEquals(Long.valueOf(2), PreevalStatistics.ruleCounts().get("ReactivateRule"));
        Assertions.assertEquals(Long.valueOf(1), PreevalStatistics.ruleCounts().get("InlineRule"));
    }

    @Test
    public void testResetClearsEveryCounter()
    {
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PreevalStatistics.ruleApplied(Rules.REACTIVATE);
        PreevalStatistics.instrument(FIXED).stopPreeval(Lists.immutable.empty());
        PreevalStatistics.reset();
        Assertions.assertTrue(PreevalStatistics.ruleCounts().isEmpty());
        Assertions.assertTrue(PreevalStatistics.hookCallCounts().isEmpty());
        Assertions.assertTrue(PreevalStatistics.hookNanos().isEmpty());
    }

    @Test
    public void testCountersAreThreadSafe() throws Exception
    {
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PrevalHooks hooks = PreevalStatistics.instrument(FIXED);
        Runnable work = () ->
        {
            for (int i = 0; i < 10_000; i++)
            {
                PreevalStatistics.ruleApplied(Rules.REACTIVATE);
                hooks.stopPreeval(Lists.immutable.empty());
            }
        };
        Thread first = new Thread(work);
        Thread second = new Thread(work);
        first.start();
        second.start();
        first.join();
        second.join();
        Assertions.assertEquals(Long.valueOf(20_000), PreevalStatistics.ruleCounts().get("ReactivateRule"));
        Assertions.assertEquals(Long.valueOf(20_000), PreevalStatistics.hookCallCounts().get("stopPreeval"));
    }
}
