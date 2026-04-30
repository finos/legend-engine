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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.builder;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.Dispatch;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.inference.ParametersInference;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class UnifiedInferenceFunctionExpressionBuilder extends FunctionExpressionBuilder
{
    private final ParametersInference parametersInference;
    private final MutableList<FunctionHandler> handlers;

    public UnifiedInferenceFunctionExpressionBuilder(ParametersInference parametersInference, FunctionHandler... handlers)
    {
        this.parametersInference = parametersInference;
        this.handlers = FastList.newListWith(handlers);
    }

    @Override
    public void validate(Map<String, Dispatch> dispatchMap)
    {
        this.handlers.forEach(handler ->
        {
            Assert.assertTrue(handler.getFunctionName().equals(handler.getFunc()._functionName()), () -> "Wrong name specified in Handler: " + handler.getFunctionName() + " different from " + handler.getFunc()._functionName());
            if (dispatchMap.get(handler.getFullName()) == null)
            {
                throw new RuntimeException("Can't find a generated handler for the function " + handler.getFullName());
            }
        });
        MutableList<String> names = this.handlers.collect(FunctionHandler::getFunctionName).distinct();
        Assert.assertTrue(names.size() == 1, () -> "Unified inference handlers should have the same simple name. Found " + names.size() + " -> " + names);
    }

    @Override
    public void build(Map<String, Function<?>> result)
    {
        for (FunctionHandler handler : this.handlers)
        {
            handler.build(result);
        }
    }

    @Override
    public String getFunctionName()
    {
        return this.handlers.get(0).getFunctionName();
    }

    @Override
    public void addFunctionHandler(FunctionHandler functionHandler)
    {
        this.handlers.add(functionHandler);
    }

    public ParametersInference getParametersInference()
    {
        return this.parametersInference;
    }

    @Override
    public Boolean supportFunctionHandler(FunctionHandler handler)
    {
        // Unified builder supports any handler with the same function name
        return this.getFunctionName().equals(handler.getFunctionName());
    }

    @Override
    public Optional<Integer> getParametersSize()
    {
        // Multiple param sizes may be present across handlers
        return Optional.empty();
    }

    @Override
    public Pair<SimpleFunctionExpression, List<ValueSpecification>> buildFunctionExpression(List<org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification> parameters, SourceInformation sourceInformation, ValueSpecificationBuilder valueSpecificationBuilder)
    {
        List<ValueSpecification> compiled = parametersInference.update(parameters, valueSpecificationBuilder);

        if (compiled == null)
        {
            compiled = parameters.stream()
                    .map(p -> p.accept(valueSpecificationBuilder))
                    .collect(Collectors.toList());
        }

        SimpleFunctionExpression sfe = dispatchToHandlers(compiled, sourceInformation);
        return Tuples.pair(sfe, compiled);
    }

    private SimpleFunctionExpression dispatchToHandlers(List<ValueSpecification> compiled, SourceInformation sourceInformation)
    {
        return this.handlers.stream()
                .filter(h -> h.getDispatch().shouldSelect(compiled))
                .findFirst()
                .map(h -> h.process(compiled, sourceInformation))
                .orElse(null);
    }

    @Override
    public MutableList<FunctionHandler> handlers()
    {
        return this.handlers;
    }
}

