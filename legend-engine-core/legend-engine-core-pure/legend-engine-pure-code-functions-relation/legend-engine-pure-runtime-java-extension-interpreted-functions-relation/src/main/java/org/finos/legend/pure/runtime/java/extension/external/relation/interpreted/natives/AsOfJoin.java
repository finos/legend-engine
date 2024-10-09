// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives;

import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.Shared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSWithCursorCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class AsOfJoin extends Shared
{
    public AsOfJoin(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, processorSupport);
        RelationType<?> relationtype = (RelationType<?>) returnGenericType.getValueForMetaPropertyToMany("typeArguments").get(0).getValueForMetaPropertyToOne("rawType");

        CoreInstance matchFunction = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);

        CoreInstance filterFunction = params.size() > 3 ? Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport) : null;

        LambdaFunction<?> lambdaMatchFunction = (LambdaFunction<?>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(matchFunction);

        TestTDS tds1 = getTDS(params, 0, processorSupport).sortForOuterJoin(true, lambdaMatchFunction, processorSupport);
        TestTDS tds2 = getTDS(params, 1, processorSupport).sortForOuterJoin(false, lambdaMatchFunction, processorSupport);

        TestTDS result = tds1.join(tds2).newEmptyTDS();
        for (int i = 0; i < tds1.getRowCount(); i++)
        {
            TestTDS oneRow = tds1.slice(i, i + 1);
            TestTDS exploded = oneRow.join(tds2);
            TestTDS res = filter(exploded, matchFunction, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationtype);
            res = filterFunction == null ? res : filter(res, filterFunction, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationtype);
            if (res.getRowCount() == 0)
            {
                result = result.concatenate(oneRow.join(tds2.newNullTDS()));
            }
            else
            {
                result = result.concatenate(res.slice(0, 1));
            }
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(result, returnGenericType, repository, processorSupport), false, processorSupport);
    }

    private TestTDS filter(TestTDS tds, CoreInstance filterFunction, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, RelationType<?> relationtype)
    {
        LambdaFunction<CoreInstance> lambdaFunction = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(filterFunction);
        VariableContext evalVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, filterFunction);

        MutableIntSet discardedRows = IntSets.mutable.empty();
        FixedSizeList<CoreInstance> parameters = Lists.fixedSize.with((CoreInstance) null, (CoreInstance) null);
        for (int i = 0; i < tds.getRowCount(); i++)
        {
            parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(tds, i, "", null, relationtype, -1, repository, false), true, processorSupport));
            parameters.set(1, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(tds, i, "", null, relationtype, -1, repository, false), true, processorSupport));
            CoreInstance subResult = this.functionExecution.executeFunction(false, lambdaFunction, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, evalVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
            if (!PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(subResult, M3Properties.values, processorSupport)))
            {
                discardedRows.add(i);
            }
        }
        return tds.drop(discardedRows);
    }
}
