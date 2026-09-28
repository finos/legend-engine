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
import org.finos.legend.engine.pure.preeval.ExpressionRule;
import org.finos.legend.engine.pure.preeval.MetamodelPaths;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;

public final class GenericTypeRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        return services.runtime().isFunction(prologue.rewritten()._func(), MetamodelPaths.GENERIC_TYPE_FUNCTION)
                && prologue.rewrittenParameters().getFirst()._genericType()._rawType() != null;
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        FunctionExpression rewritten = prologue.rewritten();
        services.trace(prologue.state(), "Handling genericType");
        InstanceValue value = services.runtime().newInstanceValue(rewritten._genericType(), rewritten._multiplicity(), Lists.immutable.with(prologue.rewrittenParameters().getFirst()._genericType()));
        return new PrevalResult(value, true, Lists.immutable.empty(), true);
    }
}
