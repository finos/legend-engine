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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandler;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MultiHandlerFunctionExpressionBuilder extends FunctionExpressionBuilder
{
    MutableList<FunctionHandler> handlers;

    public MultiHandlerFunctionExpressionBuilder(FunctionHandler[] handlers, PureModel pureModel)
    {
        this.handlers = FastList.newListWith(handlers);
        MutableList<String> names = this.handlers.collect(FunctionHandler::getFunctionName).distinct();
        Assert.assertTrue(names.size() == 1, () -> "Multi handlers should have the same simple name. Found " + names.size() + " -> " + names);
        MutableList<String> signatures = this.handlers.collect(c -> c.buildCode(pureModel)).distinct();
        Assert.assertTrue(signatures.size() == 1, () -> "Multi handlers should have the same kind of function signatures. Found " + signatures.size() + " -> " + signatures);
    }

    public String getFunctionName()
    {
        return handlers.get(0).getFunctionName();
    }

    @Override
    public Pair<SimpleFunctionExpression, List<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification>> buildFunctionExpression(List<ValueSpecification> parameters, MutableList<String> openVariables, CompileContext compileContext, ProcessingContext processingContext)
    {
        if (test(handlers.get(0).getFunc(), parameters, compileContext.pureModel, processingContext))
        {
            List<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> processed = parameters.stream().map(p -> p.accept(new ValueSpecificationBuilder(compileContext, openVariables, processingContext))).collect(Collectors.toList());
            return Tuples.pair(buildFunctionExpressionGraph(processed, openVariables, compileContext, processingContext), processed);
        }
        return Tuples.pair(null, null);
    }

    public SimpleFunctionExpression buildFunctionExpressionGraph(List<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> parameters, MutableList<String> openVariables, CompileContext compileContext, ProcessingContext processingContext)
    {
        RichIterable<SimpleFunctionExpression> res = handlers.collect(h -> h.getDispatch().shouldSelect(parameters) ? h.process(parameters) : null);
        return res.select(Objects::nonNull).getFirst();
    }

    @Override
    public MutableList<FunctionHandler> handlers()
    {
        return this.handlers;
    }

    @Override
    public void insertFunctionHandlerAtIndex(int idx, FunctionHandler functionHandler)
    {
        if (idx < 0)
        {
            handlers.add(0, functionHandler);
        }
        else
        {
            handlers.add(Math.min(idx, handlers.size() - 1), functionHandler);
        }
    }

    @Override
    public FunctionExpressionBuilder getFunctionExpressionBuilderAtIndex(int idx)
    {
        throw new UnsupportedOperationException("Multi-handler function expression builder does not support getting function expression builder at a particular index");
    }

    @Override
    public void insertFunctionExpressionBuilderAtIndex(int idx, FunctionExpressionBuilder functionExpressionBuilder)
    {
        throw new UnsupportedOperationException("Multi-handler function expression builder does not support inserting function expression builder at a particular index");
    }
}
