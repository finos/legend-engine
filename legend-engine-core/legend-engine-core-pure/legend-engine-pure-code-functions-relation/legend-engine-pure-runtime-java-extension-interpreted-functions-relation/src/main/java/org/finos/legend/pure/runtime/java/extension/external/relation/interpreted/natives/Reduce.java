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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.AggregationShared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.ProjectExtend;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSWithCursorCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Window;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Reduce extends AggregationShared
{
    public Reduce(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        TestTDS tds = getTDS(params, 0, processorSupport);
        RelationType<?> relationType = getRelationType(params, 0);
        GenericType sourceRelationType = (GenericType) params.get(0).getValueForMetaPropertyToOne("genericType");

        Window window = Window.build(params.get(1).getValueForMetaPropertyToOne("values"), processorSupport, new ProjectExtend.RepoPrimitiveHandler(repository));

        TDSWithCursorCoreInstance rc = (TDSWithCursorCoreInstance) params.get(2).getValueForMetaPropertyToOne("values");

        MutableList<SortInfo> sortInfos = window.getSorts();
        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortedPartitions = tds.wrapFullTDS(); // it's already sorted and partitioned

        CoreInstance mapFunc = Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<CoreInstance> mapLambda = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<CoreInstance>) org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper.toLambdaFunction(mapFunc);
        VariableContext mapEvalVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, mapFunc);

        CoreInstance aggFunc = Instance.getValueForMetaPropertyToOneResolved(params.get(4), M3Properties.values, processorSupport);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<CoreInstance> aggLambda = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<CoreInstance>) org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggFunc);
        VariableContext aggEvalVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, aggFunc);

        CoreInstance[] result = new CoreInstance[(int) tds.getRowCount()];

        performAggregation(
                sortedPartitions,
                sortInfos,
                window,
                mapLambda,
                aggLambda,
                (j, r) -> result[j] = r,
                resolvedTypeParameters,
                resolvedMultiplicityParameters,
                functionExpressionCallStack,
                profiler,
                instantiationContext,
                executionSupport,
                processorSupport,
                relationType,
                mapEvalVarContext,
                aggEvalVarContext,
                false,
                false,
                sourceRelationType
        );

        return ValueSpecificationBootstrap.wrapValueSpecification(result[rc.getCurrentRow()], true, processorSupport);
    }
}
