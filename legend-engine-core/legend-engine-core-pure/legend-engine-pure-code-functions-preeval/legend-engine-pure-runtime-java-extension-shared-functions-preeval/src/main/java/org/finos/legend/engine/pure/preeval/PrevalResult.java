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
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public final class PrevalResult
{
    private final CoreInstance value;
    private final boolean canPreval;
    private final ImmutableList<String> openVars;
    private final boolean modified;

    public PrevalResult(CoreInstance value, boolean canPreval, ImmutableList<String> openVars, boolean modified)
    {
        this.value = value;
        this.canPreval = canPreval;
        this.openVars = openVars;
        this.modified = modified;
    }

    public static PrevalResult unmodified(CoreInstance value)
    {
        return new PrevalResult(value, true, Lists.immutable.empty(), false);
    }

    public CoreInstance getValue()
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
}
