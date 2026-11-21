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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;

import java.util.function.Function;

public final class Scope
{
    private final Function<Object, String> variableNameOf;

    public Scope(Function<Object, String> variableNameOf)
    {
        this.variableNameOf = variableNameOf;
    }

    public boolean areAllInScope(ListIterable<String> vars, ImmutableMap<String, ImmutableList<Object>> inScope)
    {
        if (vars.isEmpty())
        {
            return true;
        }
        MutableList<String> unresolved = Lists.mutable.empty();
        vars.forEach(v ->
        {
            ImmutableList<Object> value = inScope.get(v);
            if (value == null)
            {
                unresolved.add(v);
            }
            else
            {
                String alias = aliasOf(value);
                if (alias != null)
                {
                    unresolved.add(alias);
                }
            }
        });
        ListIterable<String> distinct = unresolved.distinct();
        if (distinct.anySatisfy(vars::contains))
        {
            return false;
        }
        return areAllInScope(distinct.reject(vars::contains), inScope);
    }

    public ImmutableList<String> openVars(ListIterable<String> vars, ImmutableMap<String, ImmutableList<Object>> inScope)
    {
        return vars.distinct().reject(v -> areAllInScope(Lists.immutable.with(v), inScope)).toImmutable();
    }

    public String resolveVariable(String name, ImmutableMap<String, ImmutableList<Object>> inScope)
    {
        ImmutableList<Object> value = inScope.get(name);
        String alias = value == null ? null : aliasOf(value);
        if (alias == null)
        {
            return name;
        }
        if (alias.equals(name))
        {
            throw new IllegalStateException("Circular variable reference: " + name);
        }
        return resolveVariable(alias, inScope);
    }

    private String aliasOf(ImmutableList<Object> value)
    {
        return value.size() == 1 ? this.variableNameOf.apply(value.getOnly()) : null;
    }
}
