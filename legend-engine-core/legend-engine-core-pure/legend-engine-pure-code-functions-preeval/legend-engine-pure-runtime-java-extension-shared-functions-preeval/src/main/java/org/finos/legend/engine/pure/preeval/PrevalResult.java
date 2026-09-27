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
import org.eclipse.collections.impl.utility.Iterate;

public final class PrevalResult
{
    private final Object value;
    private final boolean canPreval;
    private final ImmutableList<String> openVars;
    private final boolean modified;

    public PrevalResult(Object value, boolean canPreval, ImmutableList<String> openVars, boolean modified)
    {
        this.value = value;
        this.canPreval = canPreval;
        this.openVars = openVars;
        this.modified = modified;
    }

    public static PrevalResult unmodified(Object value)
    {
        return unmodified(value, true);
    }

    public static PrevalResult unmodified(Object value, boolean canPreval)
    {
        return new PrevalResult(value, canPreval, Lists.immutable.empty(), false);
    }

    public static boolean anyModified(Iterable<PrevalResult> results)
    {
        return Iterate.anySatisfy(results, PrevalResult::isModified);
    }

    public static boolean allCanPreval(Iterable<PrevalResult> results)
    {
        return Iterate.allSatisfy(results, PrevalResult::canPreval);
    }

    public Object getValue()
    {
        return this.value;
    }

    public boolean canPreval()
    {
        return this.canPreval;
    }

    public ImmutableList<String> getOpenVars()
    {
        return this.openVars;
    }

    public boolean isModified()
    {
        return this.modified;
    }

    public PrevalResult markModified()
    {
        return this.modified ? this : new PrevalResult(this.value, this.canPreval, this.openVars, true);
    }
}
