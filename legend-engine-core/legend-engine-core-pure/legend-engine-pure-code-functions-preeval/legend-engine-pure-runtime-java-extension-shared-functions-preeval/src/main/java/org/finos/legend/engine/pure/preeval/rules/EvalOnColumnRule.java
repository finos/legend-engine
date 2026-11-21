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
import org.finos.legend.engine.pure.preeval.PrevalRuntime;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;

public final class EvalOnColumnRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        if (!EvalExpansionRule.isEval(prologue, services))
        {
            return false;
        }
        Object first = EvalExpansionRule.firstParameter(prologue, services);
        return !(first instanceof LambdaFunction) && services.runtime().isInstanceOf(first, MetamodelPaths.COLUMN);
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        PrevalRuntime runtime = services.runtime();
        Object column = EvalExpansionRule.firstParameter(prologue, services);
        FunctionExpression value = runtime.withFuncAndParameters(prologue.rewritten(), column, prologue.rewrittenParameters().drop(1));
        Type rawType = value._genericType()._rawType();
        if (rawType == null)
        {
            throw runtime.error("Cannot cast a collection of size 0 to multiplicity [1]");
        }
        if (runtime.isInstanceOf(rawType, MetamodelPaths.RELATION_TYPE))
        {
            value = runtime.withGenericTypeAndMultiplicity(value, runtime.functionReturnType(column), runtime.functionReturnMultiplicity(column));
        }
        return new PrevalResult(value, true, Lists.immutable.empty(), true);
    }
}
