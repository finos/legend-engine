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
import org.eclipse.collections.api.stack.MutableStack;
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
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.ColumnValue;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.FrameType;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Window;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Frame;

import java.util.Objects;
import java.util.Stack;

public abstract class AggregationShared extends Shared
{
    public AggregationShared(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    protected ColumnValue processOneAggColSpec(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> aggregationScope, MutableList<SortInfo> sortInfos, Window window, AggColSpec<?, ?, ?> aggColSpec, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, RelationType<?> relationType, boolean compress, boolean twoParamsFunc, GenericType sourceTDSType)
    {
        String name = aggColSpec.getValueForMetaPropertyToOne("name").getName();
        LambdaFunction<CoreInstance> mapF = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggColSpec.getValueForMetaPropertyToOne("map"));
        LambdaFunction<CoreInstance> reduceF = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggColSpec.getValueForMetaPropertyToOne("reduce"));

        VariableContext mapFVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, mapF);
        VariableContext reduceFVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, reduceF);

        Type type = ((FunctionType) reduceF._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._rawType();

        int size = compress ? aggregationScope.getTwo().size() : (int) aggregationScope.getOne().getRowCount();
        boolean[] nulls = new boolean[(int) size];
        if (type == _Package.getByUserPath(M3Paths.String, processorSupport))
        {
            String[] finalRes = new String[size];
            performAggregation(aggregationScope, sortInfos, window, mapF, reduceF, (j, val) -> finalRes[j] = PrimitiveUtilities.getStringValue(val), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, mapFVarContext, reduceFVarContext, compress, twoParamsFunc, sourceTDSType);
            return new ColumnValue(name, DataType.STRING, finalRes);
        }
        else if (type == _Package.getByUserPath(M3Paths.Integer, processorSupport))
        {
            long[] finalRes = new long[size];
            performAggregation(aggregationScope, sortInfos, window, mapF, reduceF, (j, val) -> processWithNull(j, val, nulls, () -> finalRes[j] = PrimitiveUtilities.getIntegerValue(val).intValue()), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, mapFVarContext, reduceFVarContext, compress, twoParamsFunc, sourceTDSType);
            return new ColumnValue(name, DataType.LONG, finalRes, nulls);
        }
        else if (type == _Package.getByUserPath(M3Paths.Float, processorSupport) || type == _Package.getByUserPath(M3Paths.Number, processorSupport))
        {
            double[] finalRes = new double[size];
            performAggregation(aggregationScope, sortInfos, window, mapF, reduceF, (j, val) -> processWithNull(j, val, nulls, () -> finalRes[j] = PrimitiveUtilities.getFloatValue(val).doubleValue()), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, mapFVarContext, reduceFVarContext, compress, twoParamsFunc, sourceTDSType);
            return new ColumnValue(name, DataType.DOUBLE, finalRes, nulls);
        }
        else
        {
            throw new RuntimeException("The type " + type._name() + " is not supported yet!");
        }
    }

    private static void processWithNull(Integer j, Object val, boolean[] nulls, Proc p)
    {
        {
            if (val == null)
            {
                nulls[j] = true;
            }
            else
            {
                p.invoke();
            }
        }
    }

    private interface Proc
    {
        void invoke();
    }

    protected void performAggregation(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> orderedSource, MutableList<SortInfo> sortInfos, Window window, LambdaFunction<CoreInstance> mapF, LambdaFunction<CoreInstance> reduceF, Procedure2<Integer, CoreInstance> setter, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, RelationType<?> relationType, VariableContext mapFVarContext, VariableContext reduceFVarContext, boolean compress, boolean twoParamFunc, GenericType sourceTDSType)
    {
        FixedSizeList<CoreInstance> mapParameters = twoParamFunc ? Lists.fixedSize.with((CoreInstance) null,(CoreInstance) null, (CoreInstance) null) : Lists.fixedSize.with((CoreInstance) null);
        FixedSizeList<CoreInstance> reduceParameters = Lists.fixedSize.with((CoreInstance) null);
        int size = orderedSource.getTwo().size();
        int uncompressedCursor = 0;
        ProjectExtend.RepoPrimitiveHandler repoPrimitiveHandler = new ProjectExtend.RepoPrimitiveHandler(repository);
        for (int j = 0; j < size; j++)
        {
            Pair<Integer, Integer> r = orderedSource.getTwo().get(j);
            MutableList<CoreInstance> subList = Lists.mutable.empty();
            Integer partitionStartIndex = r.getOne();
            Integer partitionEndIndex = r.getTwo();
            int partitionSize = partitionEndIndex - partitionStartIndex;
            TestTDS sourceTDS = orderedSource.getOne().slice(partitionStartIndex, partitionEndIndex);
            TDSCoreInstance sourceTDSCoreInstance = new TDSCoreInstance(sourceTDS, sourceTDSType, repository, processorSupport);
            if (window != null && window.getFrame().getFrameType() == FrameType.range)
            {
                if (sortInfos.size() != 1)
                {
                    throw new RuntimeException("There must be exactly one sort info for range frame, but found " + sortInfos.size());
                }
                SortInfo sortInfo = sortInfos.get(0);
                String orderByColumnName = sortInfo.getColumnName();
                SortDirection sortDirection = sortInfo.getDirection();
                Frame frame = window.getFrame();
                Number offsetFrom = frame.getOffsetFrom();
                Number offsetTo = frame.getOffsetTo(0);
                MutableList<Object> orderByValues = Lists.mutable.empty();
                for (int i = 0; i < partitionSize; i++)
                {
                    Object orderByRowValue = sourceTDS.getValue(orderByColumnName, i);
                    orderByValues.add(orderByRowValue);
                    if (twoParamFunc)
                    {
                        mapParameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(sourceTDSCoreInstance, false, processorSupport));
                        mapParameters.set(1, ValueSpecificationBootstrap.wrapValueSpecification(window.convert(processorSupport, repoPrimitiveHandler), false, processorSupport));
                    }
                    mapParameters.set(twoParamFunc ? 2 : 0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(sourceTDS, i, "", null, relationType, -1, this.repository, false), true, processorSupport));
                    CoreInstance oneRes = this.functionExecution.executeFunction(false, mapF, mapParameters, resolvedTypeParameters, resolvedMultiplicityParameters, mapFVarContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToOne("values");
                    subList.add(oneRes);
                }

                for (int i = 0; i < partitionSize; i++)
                {
                    int currentPartitionIndex = partitionStartIndex + i;
                    MutableList<CoreInstance> aggregationValues = Lists.mutable.empty();
                    Object orderByCurrentRowValue = orderByValues.get(i);
                    for (int k = 0; k < partitionSize; k++)
                    {
                        Object currentPartitionValueAsObject = orderByValues.get(k);
                        CoreInstance aggregateValue = subList.get(k);
                        if (orderByCurrentRowValue == null)
                        {
                            if (currentPartitionValueAsObject == null) // Rows with NULL in the ORDER BY column are included in frame boundary only when the ORDER BY value of the current row is NULL.
                            {
                                aggregationValues.add(aggregateValue);
                            }
                        }
                        else if (orderByCurrentRowValue instanceof Number)
                        {
                            Number currentRowValue = (Number) orderByCurrentRowValue;
                            if (offsetFrom == null && offsetTo == null) // RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
                            {
                                aggregationValues.add(aggregateValue);
                            }
                            else if (offsetFrom == null) // RANGE BETWEEN UNBOUNDED PRECEDING AND N PRECEDING/FOLLOWING
                            {
                                if (sortDirection == SortDirection.ASC)
                                {
                                    Number upperBound = repoPrimitiveHandler.plus(currentRowValue, offsetTo);
                                    if (currentPartitionValueAsObject != null)
                                    {
                                        Number currentPartitionValue = (Number) currentPartitionValueAsObject;
                                        if (repoPrimitiveHandler.lessThanEqual(currentPartitionValue, upperBound))
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                                else
                                {
                                    Number lowerBound = repoPrimitiveHandler.minus(currentRowValue, offsetTo);
                                    if (currentPartitionValueAsObject == null) // When the ORDER BY clause specifies NULLS FIRST, which is default when sort order is DESC, rows with NULL in the ORDER BY column are included in UNBOUNDED PRECEDING frames.
                                    {
                                        aggregationValues.add(aggregateValue);
                                    }
                                    else
                                    {
                                        Number currentPartitionValue = (Number) currentPartitionValueAsObject;
                                        if (repoPrimitiveHandler.lessThanEqual(lowerBound, currentPartitionValue))
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                            }
                            else if (offsetTo == null) // RANGE BETWEEN N PRECEDING/FOLLOWING AND UNBOUNDED FOLLOWING
                            {
                                if (sortDirection == SortDirection.ASC)
                                {
                                    Number lowerBound = repoPrimitiveHandler.plus(currentRowValue, offsetFrom);
                                    if (currentPartitionValueAsObject == null) // When the ORDER BY clause specifies NULLS LAST, which is default when sort order is ASC, rows with NULL in the ORDER BY column are included in UNBOUNDED FOLLOWING frames.
                                    {
                                        aggregationValues.add(aggregateValue);
                                    }
                                    else
                                    {
                                        Number currentPartitionValue = (Number) currentPartitionValueAsObject;
                                        if (repoPrimitiveHandler.lessThanEqual(lowerBound, currentPartitionValue))
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                                else
                                {
                                    Number upperBound = repoPrimitiveHandler.minus(currentRowValue, offsetFrom);
                                    if (currentPartitionValueAsObject != null)
                                    {
                                        Number currentPartitionValue = (Number) currentPartitionValueAsObject;
                                        if (repoPrimitiveHandler.lessThanEqual(currentPartitionValue, upperBound))
                                        {
                                            aggregationValues.add(aggregateValue);
                                        }
                                    }
                                }
                            }
                            else // RANGE BETWEEN N PRECEDING/FOLLOWING AND N PRECEDING/FOLLOWING
                            {
                                Number lowerBound = sortDirection == SortDirection.ASC ? repoPrimitiveHandler.plus(currentRowValue, offsetFrom) : repoPrimitiveHandler.minus(currentRowValue, offsetTo);
                                Number upperBound = sortDirection == SortDirection.ASC ? repoPrimitiveHandler.plus(currentRowValue, offsetTo) : repoPrimitiveHandler.minus(currentRowValue, offsetFrom);
                                if (currentPartitionValueAsObject != null)
                                {
                                    Number currentPartitionValue = (Number) currentPartitionValueAsObject;
                                    if (repoPrimitiveHandler.lessThanEqual(lowerBound, currentPartitionValue) && repoPrimitiveHandler.lessThanEqual(currentPartitionValue, upperBound))
                                    {
                                        aggregationValues.add(aggregateValue);
                                    }
                                }
                            }
                        }
                        else
                        {
                            throw new RuntimeException("Non-numeric values for order by column are not currently supported for range frame, but found: " + orderByCurrentRowValue.getClass());
                        }
                    }

                    aggregationValues.removeIf(Objects::isNull);
                    if (aggregationValues.isEmpty())
                    {
                        setter.value(currentPartitionIndex, null);
                    }
                    else
                    {
                        reduceParameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(aggregationValues, true, processorSupport));
                        CoreInstance re = this.functionExecution.executeFunction(false, reduceF, reduceParameters, resolvedTypeParameters, resolvedMultiplicityParameters, reduceFVarContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
                        setter.value(currentPartitionIndex, re.getValueForMetaPropertyToOne("values"));
                    }
                }
            }
            else
            {
                for (int i = 0; i < partitionSize; i++)
                {
                    if (twoParamFunc)
                    {
                        mapParameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(sourceTDSCoreInstance, false, processorSupport));
                        mapParameters.set(1, ValueSpecificationBootstrap.wrapValueSpecification(window.convert(processorSupport, repoPrimitiveHandler), false, processorSupport));
                    }
                    mapParameters.set(twoParamFunc ? 2 : 0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(sourceTDS, i, "", null, relationType, -1, this.repository, false), true, processorSupport));
                    CoreInstance oneRes = this.functionExecution.executeFunction(false, mapF, mapParameters, resolvedTypeParameters, resolvedMultiplicityParameters, mapFVarContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToOne("values");
                    subList.add(oneRes);
                }
                if (compress)
                {
                    subList.removeIf(Objects::isNull);
                    reduceParameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(subList, true, processorSupport));
                    CoreInstance re = this.functionExecution.executeFunction(false, reduceF, reduceParameters, resolvedTypeParameters, resolvedMultiplicityParameters, reduceFVarContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
                    setter.value(j, re.getValueForMetaPropertyToOne("values"));
                }
                else
                {
                    for (int i = 0; i < partitionSize; i++)
                    {
                        if (i + (Integer) window.getFrame().getOffsetTo(subList.size()) < 0 || i + (Integer) window.getFrame().getOffsetFrom() >= subList.size())
                        {
                            setter.value(partitionStartIndex + i, null);
                        }
                        else
                        {
                            MutableList<CoreInstance> l = framedList(subList, window.getFrame(), i);
                            l.removeIf(Objects::isNull);
                            reduceParameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(l, true, processorSupport));
                            CoreInstance re = this.functionExecution.executeFunction(false, reduceF, reduceParameters, resolvedTypeParameters, resolvedMultiplicityParameters, reduceFVarContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
                            setter.value(partitionStartIndex + i, re.getValueForMetaPropertyToOne("values"));
                        }
                    }
                }
            }
        }
    }

    protected static MutableList<CoreInstance> framedList(MutableList<CoreInstance> src, Frame frame, int currentRow)
    {
        MutableList<CoreInstance> copy = Lists.mutable.withAll(src);
        return copy.subList(frame.getLow(currentRow), frame.getHigh(currentRow, src.size()) + 1);
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
