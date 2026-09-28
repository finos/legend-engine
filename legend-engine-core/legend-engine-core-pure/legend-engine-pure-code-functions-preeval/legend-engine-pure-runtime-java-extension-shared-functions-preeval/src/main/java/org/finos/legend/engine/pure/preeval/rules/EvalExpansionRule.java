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
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.pure.preeval.ExpressionRule;
import org.finos.legend.engine.pure.preeval.MetamodelPaths;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.PrevalState;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

public final class EvalExpansionRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        if (!isEval(prologue, services))
        {
            return false;
        }
        Object first = firstParameter(prologue, services);
        return first instanceof FunctionDefinition && Lists.mutable.withAll(((FunctionDefinition<?>) first)._expressionSequence()).size() == 1;
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        FunctionExpression expression = prologue.rewritten();
        FunctionDefinition<?> function = (FunctionDefinition<?>) firstParameter(prologue, services);
        PrevalState state = prologue.state();
        services.trace(state, "Expanding eval");
        ImmutableList<GenericType> typeParameters = services.runtime().resolvedTypeParameters(expression);
        ImmutableList<ValueSpecification> parameters = prologue.rewrittenParameters();
        PrevalState evalScope = services.addToScope(state, expression._func(), typeParameters, parameters, true);
        PrevalState scoped = services.addToScope(evalScope, function, typeParameters, parameters.drop(1), false);
        return RuleSupport.prevalBody(function, scoped, services).markModified();
    }

    static boolean isEval(Prologue prologue, PrevalServices services)
    {
        Object function = prologue.rewritten()._func();
        return MetamodelPaths.EVAL_FUNCTIONS.anySatisfy(path -> services.runtime().isFunction(function, path));
    }

    static Object firstParameter(Prologue prologue, PrevalServices services)
    {
        ValueSpecification first = prologue.rewrittenParameters().getFirst();
        if (!(first instanceof InstanceValue))
        {
            return first;
        }
        ImmutableList<Object> values = services.runtime().values((InstanceValue) first);
        if (values.size() != 1)
        {
            throw services.runtime().error("Cannot cast a collection of size " + values.size() + " to multiplicity [1]");
        }
        return values.getOnly();
    }
}
