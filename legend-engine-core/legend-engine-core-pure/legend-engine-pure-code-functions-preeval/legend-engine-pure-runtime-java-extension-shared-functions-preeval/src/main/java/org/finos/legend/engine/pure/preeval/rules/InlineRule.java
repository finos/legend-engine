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
import org.finos.legend.engine.pure.preeval.PrevalState;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;

public final class InlineRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        Object function = prologue.rewritten()._func();
        return function instanceof FunctionDefinition
                && Lists.mutable.withAll(((FunctionDefinition<?>) function)._expressionSequence()).size() == 1
                && !services.runtime().isQualifiedPropertyOf(function, MetamodelPaths.TDS_ROW)
                && !services.hooks().isGeneratedMilestoningProperty(function)
                && services.hooks().shouldInline(function)
                && prologue.state().getPath().noneSatisfy(function::equals);
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        FunctionExpression expression = prologue.rewritten();
        FunctionDefinition<?> function = (FunctionDefinition<?>) expression._func();
        PrevalState state = prologue.state();
        services.trace(state, "Inlining: " + services.runtime().typeDescription(function));
        PrevalState scoped = services.addToScope(state, function, services.runtime().resolvedTypeParameters(expression), prologue.rewrittenParameters(), true);
        PrevalState inlined = scoped.withPath(state.getPath().newWith(function));
        return RuleSupport.prevalBody(function, inlined, services).markModified();
    }
}
