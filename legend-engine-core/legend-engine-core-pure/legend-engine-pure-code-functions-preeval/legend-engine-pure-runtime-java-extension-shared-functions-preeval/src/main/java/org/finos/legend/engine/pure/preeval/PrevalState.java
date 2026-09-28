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

public final class PrevalState
{
    private final ImmutableMap<String, ImmutableList<Object>> inScopeVars;
    private final ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars;
    private final ImmutableMap<String, Object> inScopeTypeParams;
    private final ImmutableList<Object> path;
    private final int depth;
    private final boolean debug;

    private PrevalState(ImmutableMap<String, ImmutableList<Object>> inScopeVars, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars, ImmutableMap<String, Object> inScopeTypeParams, ImmutableList<Object> path, int depth, boolean debug)
    {
        this.inScopeVars = inScopeVars;
        this.rollingInScopeVars = rollingInScopeVars;
        this.inScopeTypeParams = inScopeTypeParams;
        this.path = path;
        this.depth = depth;
        this.debug = debug;
    }

    public static PrevalState initial(ImmutableMap<String, ImmutableList<Object>> inScopeVars, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars, boolean debug)
    {
        return new PrevalState(inScopeVars, rollingInScopeVars, Maps.immutable.empty(), Lists.immutable.empty(), -1, debug);
    }

    public ImmutableMap<String, ImmutableList<Object>> getInScopeVars()
    {
        return this.inScopeVars;
    }

    public ImmutableMap<String, ImmutableList<Object>> getRollingInScopeVars()
    {
        return this.rollingInScopeVars;
    }

    public ImmutableMap<String, Object> getInScopeTypeParams()
    {
        return this.inScopeTypeParams;
    }

    public ImmutableList<Object> getPath()
    {
        return this.path;
    }

    public int getDepth()
    {
        return this.depth;
    }

    public boolean isDebug()
    {
        return this.debug;
    }

    public PrevalState withInScopeVars(ImmutableMap<String, ImmutableList<Object>> vars)
    {
        return new PrevalState(vars, this.rollingInScopeVars, this.inScopeTypeParams, this.path, this.depth, this.debug);
    }

    public PrevalState withRollingInScopeVars(ImmutableMap<String, ImmutableList<Object>> vars)
    {
        return new PrevalState(this.inScopeVars, vars, this.inScopeTypeParams, this.path, this.depth, this.debug);
    }

    public PrevalState withInScopeTypeParams(ImmutableMap<String, Object> typeParams)
    {
        return new PrevalState(this.inScopeVars, this.rollingInScopeVars, typeParams, this.path, this.depth, this.debug);
    }

    public PrevalState withPath(ImmutableList<Object> path)
    {
        return new PrevalState(this.inScopeVars, this.rollingInScopeVars, this.inScopeTypeParams, path, this.depth, this.debug);
    }

    public PrevalState deeper(Object functionDefinition)
    {
        return new PrevalState(this.inScopeVars, this.rollingInScopeVars, this.inScopeTypeParams, functionDefinition == null ? this.path : this.path.newWith(functionDefinition), this.depth + 1, this.debug);
    }
}
