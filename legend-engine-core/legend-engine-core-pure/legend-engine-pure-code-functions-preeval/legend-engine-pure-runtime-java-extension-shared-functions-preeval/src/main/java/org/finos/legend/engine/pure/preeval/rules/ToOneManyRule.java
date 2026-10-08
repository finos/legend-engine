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

package org.finos.legend.engine.pure.preeval.rules;

import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.pure.preeval.ExpressionRule;
import org.finos.legend.engine.pure.preeval.MetamodelPaths;
import org.finos.legend.engine.pure.preeval.Multiplicities;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalRuntime;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

public final class ToOneManyRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        PrevalRuntime runtime = services.runtime();
        if (!runtime.isFunction(prologue.rewritten()._func(), MetamodelPaths.TO_ONE_MANY_FUNCTION))
        {
            return false;
        }
        ImmutableList<ValueSpecification> parameters = prologue.rewrittenParameters();
        if (parameters.size() != 1)
        {
            return false;
        }
        Multiplicity multiplicity = parameters.getOnly()._multiplicity();
        return runtime.isMultiplicityConcrete(multiplicity) && Multiplicities.hasLowerBound(runtime, multiplicity) && runtime.lowerBound(multiplicity) > 0;
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        services.trace(prologue.state(), () -> "Handling toOneMany");
        return prologue.parameters().getOnly().markModified();
    }
}
