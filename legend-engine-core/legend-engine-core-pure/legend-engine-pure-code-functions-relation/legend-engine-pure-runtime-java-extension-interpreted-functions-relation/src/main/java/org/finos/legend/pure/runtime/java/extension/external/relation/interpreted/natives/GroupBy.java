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

import io.deephaven.csv.parsers.DataType;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.Shared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSWithCursorCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class GroupBy extends Shared
{
    public GroupBy(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, processorSupport);

        TestTDS tds = getTDS(params, 0, processorSupport);

        RelationType<?> relationType = getRelationType(params, 0);

        Object cols = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        ListIterable<String> ids;
        if (cols instanceof ColSpec)
        {
            ids = Lists.mutable.with(((ColSpec<?>) cols)._name());
        }
        else if (cols instanceof ColSpecArray)
        {
            ids = ((ColSpecArray<?>) cols)._names().collect(c -> (String) c).toList();
        }
        else
        {
            throw new RuntimeException("Not Possible");
        }

        CoreInstance aggColSpec = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
        TestTDS result;
        if (aggColSpec instanceof AggColSpec)
        {
            result = processOneAggColSpec(tds, null, ids, aggColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType);
        }
        else if (aggColSpec instanceof AggColSpecArray)
        {
            result = ((AggColSpecArray<?, ?, ?>) aggColSpec)._aggSpecs().injectInto(null, (a, b) -> processOneAggColSpec(tds, a, ids, b, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType));
        }
        else
        {
            throw new RuntimeException("Not Possible");
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(result, returnGenericType, repository, processorSupport), false, processorSupport);
    }

    private TestTDS processOneAggColSpec(TestTDS tds, TestTDS existing, ListIterable<String> ids, CoreInstance aggColSpec, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, RelationType<?> relationType)
    {
        String name = aggColSpec.getValueForMetaPropertyToOne("name").getName();
        LambdaFunction<CoreInstance> mapF = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggColSpec.getValueForMetaPropertyToOne("map"));
        LambdaFunction<CoreInstance> reduceF = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggColSpec.getValueForMetaPropertyToOne("reduce"));

        VariableContext mapFVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, mapF);
        VariableContext reduceFVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, reduceF);

        Type type = ((FunctionType) reduceF._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._rawType();

        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> res = tds.sort(ids.collect(c -> new SortInfo(c, SortDirection.ASC)));

        FixedSizeList<CoreInstance> parameters = Lists.fixedSize.with((CoreInstance) null);

        int size = res.getTwo().size();
        DataType resType = null;
        Object _finalRes = null;
        if (type == _Package.getByUserPath("String", processorSupport))
        {
            String[] finalRes = new String[size];
            performAggregation(res, mapF, reduceF, (j, val) -> finalRes[j] = PrimitiveUtilities.getStringValue(val), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, size, parameters, mapFVarContext, reduceFVarContext);
            resType = DataType.STRING;
            _finalRes = finalRes;
        }

        if (type == _Package.getByUserPath("Integer", processorSupport))
        {
            int[] finalRes = new int[size];
            performAggregation(res, mapF, reduceF, (j, val) -> finalRes[j] = PrimitiveUtilities.getIntegerValue(val).intValue(), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, size, parameters, mapFVarContext, reduceFVarContext);
            resType = DataType.INT;
            _finalRes = finalRes;
        }

        if (type == _Package.getByUserPath("Float", processorSupport))
        {
            double[] finalRes = new double[size];
            performAggregation(res, mapF, reduceF, (j, val) -> finalRes[j] = PrimitiveUtilities.getFloatValue(val).doubleValue(), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, size, parameters, mapFVarContext, reduceFVarContext);
            resType = DataType.FLOAT;
            _finalRes = finalRes;
        }
        return existing == null ? res.getOne()._distinct(res.getTwo()).addColumn(name, resType, _finalRes) : existing.addColumn(name, resType, _finalRes);
    }

    private void performAggregation(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> res, LambdaFunction<CoreInstance> mapF, LambdaFunction<CoreInstance> reduceF, Procedure2<Integer, CoreInstance> setter, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, RelationType<?> relationType, int size, FixedSizeList<CoreInstance> parameters, VariableContext mapFVarContext, VariableContext reduceFVarContext)
    {
        for (int j = 0; j < size; j++)
        {
            Pair<Integer, Integer> r = res.getTwo().get(j);
            MutableList<CoreInstance> subList = Lists.mutable.empty();
            for (int i = r.getOne(); i < r.getTwo(); i++)
            {
                parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(res.getOne(), i, "", null, relationType, -1, repository, false), true, processorSupport));
                subList.add(this.functionExecution.executeFunction(false, mapF, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, mapFVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToOne("values"));
            }
            parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(subList, true, processorSupport));
            CoreInstance re = this.functionExecution.executeFunction(false, reduceF, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, reduceFVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
            setter.value(j, re.getValueForMetaPropertyToOne("values"));
        }
    }
}
