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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandler;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

import java.util.List;

public class CompositeFunctionExpressionBuilder extends FunctionExpressionBuilder
{
    List<FunctionExpressionBuilder> builders;

    public CompositeFunctionExpressionBuilder(FunctionExpressionBuilder[] builders)
    {
        MutableList<String> names = FastList.newListWith(builders).collect(FunctionExpressionBuilder::getFunctionName).distinct();
        Assert.assertTrue(names.size() == 1, () -> "Composite builders should have the same simple name. Found " + names);
        this.builders = FastList.newListWith(builders);
    }

    public String getFunctionName()
    {
        return this.builders.get(0).getFunctionName();
    }

    @Override
    public Pair<SimpleFunctionExpression, List<ValueSpecification>> buildFunctionExpression(List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> parameters, MutableList<String> openVariables, CompileContext compileContext, ProcessingContext processingContext)
    {
        List<ValueSpecification> resolvedParams = null;
        for (FunctionExpressionBuilder b : builders)
        {
            Pair<SimpleFunctionExpression, List<ValueSpecification>> res = b.buildFunctionExpression(parameters, openVariables, compileContext, processingContext);
            if (res.getOne() != null)
            {
                return res;
            }
            if (res.getTwo() != null)
            {
                resolvedParams = res.getTwo();
            }
        }
        return Tuples.pair(null, resolvedParams);
    }

    @Override
    public MutableList<FunctionHandler> handlers()
    {
        MutableList<FunctionHandler> res = Lists.mutable.empty();
        for (FunctionExpressionBuilder b : builders)
        {
            res.addAll(b.handlers());
        }
        return res;
    }

    @Override
    public void insertFunctionHandlerAtIndex(int idx, FunctionHandler functionHandler)
    {
        throw new UnsupportedOperationException("Composite function expression builder does not support inserting function handler at a particular index");
    }

    @Override
    public FunctionExpressionBuilder getFunctionExpressionBuilderAtIndex(int idx)
    {
        if (idx < 0)
        {
            return builders.get(0);
        }
        return builders.get(Math.min(idx, builders.size() - 1));
    }

    @Override
    public void insertFunctionExpressionBuilderAtIndex(int idx, FunctionExpressionBuilder functionExpressionBuilder)
    {
        if (idx < 0)
        {
            builders.add(0, functionExpressionBuilder);
        }
        else
        {
            builders.add(Math.min(idx, builders.size() - 1), functionExpressionBuilder);
        }
    }
}
