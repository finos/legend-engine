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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.pure.preeval.ExpressionRule;
import org.finos.legend.engine.pure.preeval.MetamodelPaths;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.PrevalState;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;

public final class ReactivateRule implements ExpressionRule
{
    private static final String[] HOLDERS = {MetamodelPaths.BASIC_COLUMN_SPECIFICATION, MetamodelPaths.COLLECTION_AGGREGATE_VALUE, MetamodelPaths.TDS_AGGREGATE_VALUE, MetamodelPaths.AGG_COL_SPEC_ARRAY, MetamodelPaths.AGG_COL_SPEC};

    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        return prologue.notPrevalReason() == null;
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        PrevalState state = prologue.state();
        ImmutableList<Object> reactivated = services.runtime().reactivate(prologue.rewritten(), state.getInScopeVars());
        PrevalState emptyScope = state.withInScopeTypeParams(Maps.immutable.empty()).withInScopeVars(Maps.immutable.empty());
        ImmutableList<Object> values = reactivated.collect(v -> isHolder(v, services) ? services.preval(v, emptyScope).getValue() : v);
        Object value = values.size() == 1 && values.getOnly() instanceof InstanceValue
                ? values.getOnly()
                : services.runtime().newInstanceValue(prologue.rewritten()._genericType(), services.runtime().exactly(values.size()), values);
        return new PrevalResult(value, true, Lists.immutable.empty(), true);
    }

    private static boolean isHolder(Object value, PrevalServices services)
    {
        for (String path : HOLDERS)
        {
            if (services.runtime().isInstanceOf(value, path))
            {
                return true;
            }
        }
        return false;
    }
}
