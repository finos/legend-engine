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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared;

import io.deephaven.csv.parsers.DataType;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpec;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.ColSpec;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.ColSpecArray;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.ColumnValue;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public abstract class AggregationShared extends Shared
{
    public AggregationShared(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    protected ColumnValue processOneAggColSpec(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> aggregationScope, AggColSpec<?,?,?> aggColSpec, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, RelationType<?> relationType, boolean compress, boolean twoParamsFunc, GenericType sourceTDSType)
    {
        String name = aggColSpec.getValueForMetaPropertyToOne("name").getName();
        LambdaFunction<CoreInstance> mapF = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggColSpec.getValueForMetaPropertyToOne("map"));
        LambdaFunction<CoreInstance> reduceF = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggColSpec.getValueForMetaPropertyToOne("reduce"));

        VariableContext mapFVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, mapF);
        VariableContext reduceFVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, reduceF);

        Type type = ((FunctionType) reduceF._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._rawType();

        int size = compress ? aggregationScope.getTwo().size() : (int) aggregationScope.getOne().getRowCount();
        if (type == _Package.getByUserPath("String", processorSupport))
        {
            String[] finalRes = new String[size];
            performAggregation(aggregationScope, mapF, reduceF, (j, val) -> finalRes[j] = PrimitiveUtilities.getStringValue(val), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, mapFVarContext, reduceFVarContext, compress, twoParamsFunc, sourceTDSType);
            return new ColumnValue(name, DataType.STRING, finalRes);
        }
        else if (type == _Package.getByUserPath("Integer", processorSupport))
        {
            int[] finalRes = new int[size];
            performAggregation(aggregationScope, mapF, reduceF, (j, val) -> finalRes[j] = PrimitiveUtilities.getIntegerValue(val).intValue(), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, mapFVarContext, reduceFVarContext, compress, twoParamsFunc, sourceTDSType);
            return new ColumnValue(name, DataType.INT, finalRes);
        }
        else if (type == _Package.getByUserPath("Float", processorSupport) || type == _Package.getByUserPath("Number", processorSupport))
        {
            double[] finalRes = new double[size];
            performAggregation(aggregationScope, mapF, reduceF, (j, val) -> finalRes[j] = PrimitiveUtilities.getFloatValue(val).doubleValue(), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, mapFVarContext, reduceFVarContext, compress, twoParamsFunc, sourceTDSType);
            return new ColumnValue(name, DataType.FLOAT, finalRes);
        }
        else
        {
            throw new RuntimeException("The type " + type._name() + " is not supported yet!");
        }
    }

    protected void performAggregation(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> orderedSource, LambdaFunction<CoreInstance> mapF, LambdaFunction<CoreInstance> reduceF, Procedure2<Integer, CoreInstance> setter, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, RelationType<?> relationType, VariableContext mapFVarContext, VariableContext reduceFVarContext, boolean compress, boolean twoParamFunc, GenericType sourceTDSType)
    {
        FixedSizeList<CoreInstance> mapParameters = twoParamFunc ? Lists.fixedSize.with((CoreInstance) null, (CoreInstance) null) : Lists.fixedSize.with((CoreInstance) null);
        FixedSizeList<CoreInstance> reduceParameters = Lists.fixedSize.with((CoreInstance) null);
        int size = orderedSource.getTwo().size();
        int uncompressedCursor = 0;
        for (int j = 0; j < size; j++)
        {
            Pair<Integer, Integer> r = orderedSource.getTwo().get(j);
            MutableList<CoreInstance> subList = Lists.mutable.empty();
            TestTDS sourceTDS = orderedSource.getOne().slice(r.getOne(), r.getTwo());
            for (int i = 0; i < r.getTwo() - r.getOne(); i++)
            {
                if (twoParamFunc)
                {
                    mapParameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(sourceTDS, sourceTDSType, repository, processorSupport), false, processorSupport));
                }
                mapParameters.set(twoParamFunc ? 1 : 0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(sourceTDS, i, "", null, relationType, -1, this.repository, false), true, processorSupport));
                CoreInstance oneRes = this.functionExecution.executeFunction(false, mapF, mapParameters, resolvedTypeParameters, resolvedMultiplicityParameters, mapFVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToOne("values");
                if (oneRes != null)
                {
                    subList.add(oneRes);
                }
            }
            reduceParameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(subList, true, processorSupport));
            CoreInstance re = this.functionExecution.executeFunction(false, reduceF, reduceParameters, resolvedTypeParameters, resolvedMultiplicityParameters, reduceFVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
            if (compress)
            {
                setter.value(j, re.getValueForMetaPropertyToOne("values"));
            }
            else
            {
                for (int i = r.getOne(); i < r.getTwo(); i++)
                {
                    setter.value(uncompressedCursor++, re.getValueForMetaPropertyToOne("values"));
                }
            }
        }
    }

    protected static ListIterable<String> getColumnIds(Object cols)
    {
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
        return ids;
    }
}
