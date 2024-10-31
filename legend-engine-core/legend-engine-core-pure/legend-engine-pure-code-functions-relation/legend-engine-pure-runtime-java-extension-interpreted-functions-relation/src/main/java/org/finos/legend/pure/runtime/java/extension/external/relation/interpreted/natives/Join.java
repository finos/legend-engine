// Copyright 2023 Goldman Sachs
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
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.*;
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

public class Join extends Shared
{
    public Join(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, processorSupport);

        TestTDS tds1 = getTDS(params, 0, processorSupport);
        TestTDS tds2 = getTDS(params, 1, processorSupport);

        TestTDS tds = tds1.join(tds2);

        RelationType<?> relationtype = (RelationType<?>)returnGenericType.getValueForMetaPropertyToMany("typeArguments").get(0).getValueForMetaPropertyToOne("rawType");

        String joinType = params.get(2).getValueForMetaPropertyToOne("values").getName();

        CoreInstance filterFunction = Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport);
        LambdaFunction<CoreInstance> lambdaFunction = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(filterFunction);
        VariableContext evalVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, filterFunction);

        MutableIntSet discardedRows = IntSets.mutable.empty();
        FixedSizeList<CoreInstance> parameters = Lists.fixedSize.with((CoreInstance) null, (CoreInstance) null);
        for (int i = 0; i < tds.getRowCount(); i++)
        {
            parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(tds, i, "", null, relationtype, -1, repository, false), true, processorSupport));
            parameters.set(1, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(tds, i, "", null, relationtype, -1, repository, false), true, processorSupport));
            CoreInstance subResult = this.functionExecution.executeFunction(false, lambdaFunction, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, evalVarContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
            if (!PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(subResult, M3Properties.values, processorSupport)))
            {
                discardedRows.add(i);
            }
        }
        TestTDS res = tds.drop(discardedRows);
        if (joinType.equals("LEFT"))
        {
            res = tds1.compensateLeft(res);
        }
        return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(res, returnGenericType, repository, processorSupport), false, processorSupport);
    }
}
