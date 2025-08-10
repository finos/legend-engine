// Copyright 2025 Goldman Sachs
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
//

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives;

import java.util.Stack;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
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

public class LateralJoin extends Shared
{
    public LateralJoin(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, processorSupport);

        TestTDS sourceTds = getTDS(params, 0, processorSupport);
        CoreInstance sourceTdsType = getRelationType(params, 0);

        CoreInstance mapFunction = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        LambdaFunction<CoreInstance> lambdaFunction = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(mapFunction);
        VariableContext evalVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, lambdaFunction);

        RelationType<?> lateralResult = (RelationType<?>) ((FunctionType) lambdaFunction._classifierGenericType()._typeArguments().getOnly()._rawType())._returnType()._typeArguments().getOnly()._rawType();

        TestTDS agg = lateralResult._columns().injectInto(sourceTds.newEmptyTDS(), (t, x) ->
                t.addColumn(x._name(), x._classifierGenericType()._typeArguments().getLast()._rawType()));

        for (int i = 0; i < sourceTds.getRowCount(); i++)
        {
            TestTDS row = sourceTds.slice(i, i + 1);
            CoreInstance rowInput = ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(row, 0, "", functionExpressionCallStack.peek().getSourceInformation(), sourceTdsType, -1, this.repository, false), true, processorSupport);
            CoreInstance result = this.functionExecution.executeFunction(false, lambdaFunction, Lists.fixedSize.of(rowInput), resolvedTypeParameters, resolvedMultiplicityParameters, evalVarContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
            TestTDS resultTds = getTDS(Instance.getValueForMetaPropertyToOneResolved(result, M3Properties.values, processorSupport), processorSupport);
            TestTDS lateralJoined = row.join(resultTds);
            agg = agg.concatenate(lateralJoined);
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(agg, returnGenericType, repository, processorSupport), false, processorSupport);
    }
}
