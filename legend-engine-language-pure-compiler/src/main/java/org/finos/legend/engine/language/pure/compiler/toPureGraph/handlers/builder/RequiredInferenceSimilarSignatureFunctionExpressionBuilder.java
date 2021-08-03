// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.builder;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.ParametersInference;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

import java.util.List;
import java.util.Optional;

public class RequiredInferenceSimilarSignatureFunctionExpressionBuilder extends FunctionExpressionBuilder
{
    private final ParametersInference parametersInference;
    private final MultiHandlerFunctionExpressionBuilder handlers;

    public RequiredInferenceSimilarSignatureFunctionExpressionBuilder(ParametersInference parametersInference, FunctionHandler[] handlers, PureModel pureModel)
    {
        this.parametersInference = parametersInference;
        this.handlers = new MultiHandlerFunctionExpressionBuilder(pureModel, handlers);
    }

    public String getFunctionName()
    {
        return this.handlers.getFunctionName();
    }

    @Override
    public void addFunctionHandler(FunctionHandler functionHandler)
    {
        handlers.addFunctionHandler(functionHandler);
    }

    @Override
    public Boolean supportFunctionHandler(FunctionHandler handler)
    {
        return this.handlers.supportFunctionHandler(handler);
    }

    @Override
    public Optional<Integer> getParametersSize() {
        return handlers.getParametersSize();
    }

    @Override
    public Pair<SimpleFunctionExpression, List<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification>> buildFunctionExpression(List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> parameters, MutableList<String> openVariables, CompileContext compileContext, ProcessingContext processingContext)
    {
        if (test(handlers.handlers.get(0).getFunc(), parameters, compileContext.pureModel, processingContext))
        {
            List<ValueSpecification> newParameters = parametersInference.update(parameters, openVariables, compileContext, processingContext);
            return Tuples.pair(this.handlers.buildFunctionExpressionGraph(newParameters, openVariables, compileContext, processingContext), newParameters);
        }
        else
        {
            return Tuples.pair(null, null);
        }
    }

    @Override
    public MutableList<FunctionHandler> handlers()
    {
        return handlers.handlers();
    }

    @Override
    public void insertFunctionHandlerAtIndex(int idx, FunctionHandler functionHandler)
    {
        if (idx < 0)
        {
            handlers.handlers().add(0, functionHandler);
        }
        else
        {
            handlers.handlers().add(Math.min(idx, handlers.handlers().size() - 1), functionHandler);
        }
    }

    @Override
    public FunctionExpressionBuilder getFunctionExpressionBuilderAtIndex(int idx)
    {
        throw new UnsupportedOperationException("Required inference similar signature function expression builder does not support getting function expression builder at a particular index");
    }

    @Override
    public void insertFunctionExpressionBuilderAtIndex(int idx, FunctionExpressionBuilder functionExpressionBuilder)
    {
        throw new UnsupportedOperationException("Required inference similar signature function expression builder does not support inserting function expression builder at a particular index");
    }
}
