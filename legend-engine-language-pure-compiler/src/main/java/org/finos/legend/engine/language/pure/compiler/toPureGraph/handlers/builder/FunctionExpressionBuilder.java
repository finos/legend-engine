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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandler;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.Path;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.type.Type;

import java.util.List;

public abstract class FunctionExpressionBuilder
{
    public abstract Pair<SimpleFunctionExpression, List<ValueSpecification>> buildFunctionExpression(List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> parameters, MutableList<String> openVariables, CompileContext compileContext, ProcessingContext processingContext);

    public abstract String getFunctionName();

    public boolean test(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, List<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification> parameters, PureModel pureModel)
    {
        RichIterable<? extends VariableExpression> vars = ((FunctionType) func._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters();
        if (vars.size() == parameters.size())
        {
            return vars.zip(parameters).injectInto(true, (a, b) -> a && comp(b.getOne(), b.getTwo(), pureModel));
        }
        return false;
    }

    private boolean comp(VariableExpression vv, org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification vs, PureModel pureModel)
    {
        boolean isSignatureFunction = vv._genericType()._rawType() != null && Type.subTypeOf(vv._genericType()._rawType(), pureModel.getType("meta::pure::metamodel::function::Function"), pureModel.getExecutionSupport().getProcessorSupport());
        boolean isParamFunction = vs instanceof Lambda || vs instanceof Path || (vs instanceof Collection && ((Collection) vs).values.stream().allMatch(v -> v instanceof Lambda || v instanceof Path));
        boolean isParamEmpty = vs instanceof Collection && ((Collection) vs).values.isEmpty();
        return isParamEmpty || (isSignatureFunction && isParamFunction) || (!isSignatureFunction && !isParamFunction);
    }

    public abstract MutableList<FunctionHandler> handlers();

    // Methods to get and insert function handler at certain coordinate within the function expression builder

    public abstract void insertFunctionHandlerAtIndex(int idx, FunctionHandler functionHandler);

    public abstract FunctionExpressionBuilder getFunctionExpressionBuilderAtIndex(int idx);

    public abstract void insertFunctionExpressionBuilderAtIndex(int idx, FunctionExpressionBuilder functionExpressionBuilder);
}
