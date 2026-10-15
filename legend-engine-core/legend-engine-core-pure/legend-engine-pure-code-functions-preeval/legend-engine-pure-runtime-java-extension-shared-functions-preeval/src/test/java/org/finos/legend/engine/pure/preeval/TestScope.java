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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestScope
{
    private final Scope scope = new Scope(v -> (v instanceof String && ((String) v).startsWith("$")) ? ((String) v).substring(1) : null);

    @Test
    public void testConcreteVariablesAreInScope()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with(1L));
        Assertions.assertTrue(this.scope.areAllInScope(Lists.immutable.with("a"), vars));
        Assertions.assertFalse(this.scope.areAllInScope(Lists.immutable.with("b"), vars));
        Assertions.assertTrue(this.scope.areAllInScope(Lists.immutable.empty(), vars));
    }

    @Test
    public void testVariablesBoundToOtherVariablesFollowTheChain()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with("$b"), "b", Lists.immutable.with(2L));
        Assertions.assertTrue(this.scope.areAllInScope(Lists.immutable.with("a"), vars));
        ImmutableMap<String, ImmutableList<Object>> dangling = Maps.immutable.with("a", Lists.immutable.with("$c"));
        Assertions.assertFalse(this.scope.areAllInScope(Lists.immutable.with("a"), dangling));
    }

    @Test
    public void testSelfReferenceIsNotInScope()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with("$a"));
        Assertions.assertFalse(this.scope.areAllInScope(Lists.immutable.with("a"), vars));
    }

    @Test
    public void testOpenVarsAreDistinctAndExcludeResolvableOnes()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with(1L));
        Assertions.assertEquals(Lists.immutable.with("b", "c"), this.scope.openVars(Lists.immutable.with("b", "a", "c", "b"), vars));
    }

    @Test
    public void testResolveVariableFollowsAliases()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with("$b"), "b", Lists.immutable.with(1L));
        Assertions.assertEquals("b", this.scope.resolveVariable("a", vars));
        Assertions.assertEquals("z", this.scope.resolveVariable("z", vars));
    }

    @Test
    public void testResolveVariableRejectsCircularReference()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with("$a"));
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> this.scope.resolveVariable("a", vars));
        Assertions.assertEquals("Circular variable reference: a", e.getMessage());
    }
}
