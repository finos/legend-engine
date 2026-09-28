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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestRules
{
    @Test
    public void testPreParameterRuleOrder()
    {
        Assertions.assertEquals(Lists.immutable.with("IfRule", "AndOrRule"), Rules.PRE_PARAMETER.collect(r -> r.getClass().getSimpleName()));
    }

    @Test
    public void testExpansionRuleOrder()
    {
        Assertions.assertEquals(Lists.immutable.empty(), Rules.EXPANSION.collect(r -> r.getClass().getSimpleName()));
    }

    @Test
    public void testNotPrevalledRuleOrder()
    {
        Assertions.assertEquals(Lists.immutable.empty(), Rules.NOT_PREVALLED.collect(r -> r.getClass().getSimpleName()));
    }

    @Test
    public void testReactivationIsTheFallback()
    {
        Assertions.assertEquals("ReactivateRule", Rules.REACTIVATE.getClass().getSimpleName());
    }
}
