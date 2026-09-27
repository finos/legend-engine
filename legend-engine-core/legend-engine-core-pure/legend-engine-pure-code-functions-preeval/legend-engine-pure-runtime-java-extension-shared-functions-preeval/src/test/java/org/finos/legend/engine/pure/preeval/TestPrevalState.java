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
import org.eclipse.collections.api.factory.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPrevalState
{
    @Test
    public void testInitialStateHasNoDepthPathOrTypeParameters()
    {
        PrevalState state = PrevalState.initial(Maps.immutable.with("x", Lists.immutable.with(1L)), Maps.immutable.empty(), false);
        Assertions.assertEquals(-1, state.getDepth());
        Assertions.assertTrue(state.getPath().isEmpty());
        Assertions.assertTrue(state.getInScopeTypeParams().isEmpty());
        Assertions.assertEquals(Lists.immutable.with(1L), state.getInScopeVars().get("x"));
    }

    @Test
    public void testDeeperIncrementsDepthAndExtendsPathOnlyForFunctionDefinitions()
    {
        Object function = new Object();
        PrevalState state = PrevalState.initial(Maps.immutable.empty(), Maps.immutable.empty(), false);
        PrevalState once = state.deeper(null);
        PrevalState twice = once.deeper(function);
        Assertions.assertEquals(0, once.getDepth());
        Assertions.assertTrue(once.getPath().isEmpty());
        Assertions.assertEquals(1, twice.getDepth());
        Assertions.assertEquals(Lists.immutable.with(function), twice.getPath());
    }

    @Test
    public void testWithMethodsLeaveTheOriginalUnchanged()
    {
        PrevalState state = PrevalState.initial(Maps.immutable.empty(), Maps.immutable.empty(), true);
        PrevalState changed = state.withInScopeVars(Maps.immutable.with("a", Lists.immutable.empty()));
        Assertions.assertTrue(state.getInScopeVars().isEmpty());
        Assertions.assertTrue(changed.getInScopeVars().containsKey("a"));
        Assertions.assertTrue(changed.isDebug());
    }
}
