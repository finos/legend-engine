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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpec;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpecArray;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpec;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpecArray;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.AggregationShared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSWithCursorCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.ColumnValue;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Arrays;
import java.util.Stack;

public class Extend extends AggregationShared
{
    public Extend(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        try
        {
            CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, processorSupport);

            TestTDS tds = getTDS(params, 0, processorSupport);

            RelationType<?> relationType = getRelationType(params, 0);
            GenericType sourceRelationType = (GenericType) params.get(0).getValueForMetaPropertyToOne("genericType");

            CoreInstance secondParameter = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);

            TestTDS result;
            if (secondParameter instanceof FuncColSpec)
            {
                result = tds.addColumn(processFuncColSpec(tds.wrapFullTDS(), new Frame(FrameType.rows, true, true), (FuncColSpec<?, ?>) secondParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, (GenericType) params.get(0).getValueForMetaPropertyToOne("genericType"), false));
            }
            else if (secondParameter instanceof FuncColSpecArray)
            {
                result = ((FuncColSpecArray<?, ?>) secondParameter)._funcSpecs().injectInto(
                        tds,
                        (a, funcColSpec) -> a.addColumn(processFuncColSpec(tds.wrapFullTDS(), new Frame(FrameType.rows, true, true), funcColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, sourceRelationType, false))
                );
            }
            else if (secondParameter instanceof AggColSpec)
            {
                Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source = tds.wrapFullTDS();
                result = tds.addColumn(processOneAggColSpec(source, new Frame(FrameType.rows, true, true), (AggColSpec<?, ?, ?>) secondParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, false, null));
            }
            else if (secondParameter instanceof AggColSpecArray)
            {
                Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source = tds.wrapFullTDS();
                result = ((AggColSpecArray<?, ?, ?>) secondParameter)._aggSpecs().injectInto(
                        source.getOne(),
                        (a, aggColSpec) -> a.addColumn(processOneAggColSpec(source, new Frame(FrameType.rows, true, true), aggColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, false, null))
                );
            }
            else if (Instance.instanceOf(secondParameter, "meta::pure::functions::relation::_Window", processorSupport))
            {
                MutableList<String> partitionIds = secondParameter.getValueForMetaPropertyToMany("partition").collect(PrimitiveUtilities::getStringValue).toList();
                Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source = partitionIds.isEmpty() ? tds.wrapFullTDS() : tds.sort(partitionIds.collect(c -> new SortInfo(c, SortDirection.ASC)));

                ListIterable<? extends CoreInstance> sortInfos = secondParameter.getValueForMetaPropertyToMany("sortInfo");
                final Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortedPartitions = TestTDS.sortPartitions(Sort.getSortInfos(sortInfos, processorSupport).toList(), source);

                CoreInstance frameCore = secondParameter.getValueForMetaPropertyToOne("frame");
                Frame frame = frameCore == null ? sortInfos.isEmpty() ? new Frame(FrameType.rows, true, true) : new Frame(FrameType.rows, true, 0) : Frame.build(frameCore, processorSupport);

                CoreInstance thirdParameter = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
                if (thirdParameter instanceof AggColSpec)
                {
                    result = sortedPartitions.getOne().addColumn(processOneAggColSpec(sortedPartitions, frame, (AggColSpec<?, ?, ?>) thirdParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, true, sourceRelationType));
                }
                else if (thirdParameter instanceof AggColSpecArray)
                {
                    result = ((AggColSpecArray<?, ?, ?>) thirdParameter)._aggSpecs().injectInto(
                            sortedPartitions.getOne(),
                            (a, aggColSpec) -> a.addColumn(processOneAggColSpec(sortedPartitions, frame, aggColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, true, sourceRelationType))
                    );
                }
                else if (thirdParameter instanceof FuncColSpec)
                {
                    result = sortedPartitions.getOne().addColumn(processFuncColSpec(sortedPartitions, frame, (FuncColSpec<?, ?>) thirdParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, sourceRelationType, true));
                }
                else if (thirdParameter instanceof FuncColSpecArray)
                {
                    result = ((FuncColSpecArray<?, ?>) thirdParameter)._funcSpecs().injectInto(
                            sortedPartitions.getOne(),
                            (a, funcColSpec) -> a.addColumn(processFuncColSpec(sortedPartitions, frame, funcColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, sourceRelationType, true))
                    );
                }
                else
                {
                    throw new RuntimeException("Not possible");
                }
            }
            else
            {
                throw new RuntimeException("Not possible");
            }
            return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(result, returnGenericType, repository, processorSupport), false, processorSupport);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    private ColumnValue processFuncColSpec(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source, Frame frame, FuncColSpec<?, ?> funcColSpec, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, GenericType relationType, boolean twoParamsFunc)
    {
        LambdaFunction<CoreInstance> lambdaFunction = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(funcColSpec.getValueForMetaPropertyToOne(M3Properties.function));
        String name = funcColSpec.getValueForMetaPropertyToOne(M3Properties.name).getName();

        VariableContext evalVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, lambdaFunction);

        Type type = ((FunctionType) lambdaFunction._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._rawType();

        if (type == _Package.getByUserPath("String", processorSupport))
        {
            String[] finalRes = new String[(int) source.getOne().getRowCount()];
            processOneColumn(source, frame, lambdaFunction, (j, val) -> finalRes[j] = val == null ? null : PrimitiveUtilities.getStringValue(val), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, evalVarContext, twoParamsFunc);
            return new ColumnValue(name, DataType.STRING, finalRes);
        }
        else if (type == _Package.getByUserPath("Integer", processorSupport))
        {
            int[] finalRes = new int[(int) source.getOne().getRowCount()];
            boolean[] nulls = new boolean[(int) source.getOne().getRowCount()];
            Arrays.fill(nulls, Boolean.FALSE);
            processOneColumn(source, frame, lambdaFunction, (j, val) -> processWithNull(j, val, nulls, () -> finalRes[j] = PrimitiveUtilities.getIntegerValue(val).intValue()), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, evalVarContext, twoParamsFunc);
            return new ColumnValue(name, DataType.INT, finalRes, nulls);
        }
        else if (type == _Package.getByUserPath("Float", processorSupport))
        {
            double[] finalRes = new double[(int) source.getOne().getRowCount()];
            boolean[] nulls = new boolean[(int) source.getOne().getRowCount()];
            Arrays.fill(nulls, Boolean.FALSE);
            processOneColumn(source, frame, lambdaFunction, (j, val) -> processWithNull(j, val, nulls, () -> finalRes[j] = PrimitiveUtilities.getFloatValue(val).doubleValue()), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, evalVarContext, twoParamsFunc);
            return new ColumnValue(name, DataType.DOUBLE, finalRes, nulls);
        }
        else
        {
            throw new RuntimeException("The type " + type._name() + " is not supported yet!");
        }
    }

    private interface Proc
    {
        void invoke();
    }

    private void processWithNull(Integer j, CoreInstance val, boolean[] nulls, Proc p)
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

    private void processOneColumn(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source, Frame frame, LambdaFunction<CoreInstance> lambdaFunction, Procedure2<Integer, CoreInstance> setter, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, GenericType relationType, VariableContext evalVarContext, boolean twoParamsFunc)
    {
        FixedSizeList<CoreInstance> parameters = twoParamsFunc ? Lists.fixedSize.with((CoreInstance) null, (CoreInstance) null, (CoreInstance) null) : Lists.fixedSize.with((CoreInstance) null);
        int k = 0;
        for (int j = 0; j < source.getTwo().size(); j++)
        {
            Pair<Integer, Integer> r = source.getTwo().get(j);
            TestTDS sourceTDS = source.getOne().slice(r.getOne(), r.getTwo());
            for (int i = 0; i < r.getTwo() - r.getOne(); i++)
            {
                if (twoParamsFunc)
                {
                    parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(sourceTDS, relationType, repository, processorSupport), false, processorSupport));
                    parameters.set(1, ValueSpecificationBootstrap.wrapValueSpecification(frame.convert(repository, processorSupport), false, processorSupport));
                }
                parameters.set(twoParamsFunc ? 2 : 0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(source.getOne(), i, "", null, relationType, -1, repository, false), false, processorSupport));
                CoreInstance newValue = this.functionExecution.executeFunction(false, lambdaFunction, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, evalVarContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
                setter.value(k++, newValue.getValueForMetaPropertyToOne("values"));
            }
        }
    }

    public static enum FrameType
    {
        rows, range
    }

    public static class Frame
    {
        FrameType type;
        boolean fromUnbounded;
        int offsetFrom;
        boolean toUnbounded;
        int offsetTo;

        public Frame(FrameType type, boolean fromUnbounded, int offsetTo)
        {
            this.type = type;
            this.fromUnbounded = fromUnbounded;
            this.offsetTo = offsetTo;
        }

        public Frame(FrameType type, boolean fromUnbounded, boolean toUnbounded)
        {
            this.type = type;
            this.fromUnbounded = fromUnbounded;
            this.toUnbounded = toUnbounded;
        }

        public Frame(FrameType type, int offsetFrom, boolean toUnbounded)
        {
            this.type = type;
            this.offsetFrom = offsetFrom;
            this.toUnbounded = toUnbounded;

        }

        public Frame(FrameType type, int offsetFrom, int offsetTo)
        {
            this.type = type;
            this.offsetFrom = offsetFrom;
            this.offsetTo = offsetTo;
        }

        public static Frame build(CoreInstance frameCore, ProcessorSupport processorSupport)
        {
            FrameType type = processorSupport.instance_instanceOf(frameCore, "meta::pure::functions::relation::Rows") ? FrameType.rows : FrameType.range;
            CoreInstance from = frameCore.getValueForMetaPropertyToOne("offsetFrom");
            CoreInstance to = frameCore.getValueForMetaPropertyToOne("offsetTo");
            boolean fromUn = processorSupport.instance_instanceOf(from, "meta::pure::functions::relation::UnboundedFrameValue");
            boolean toUn = processorSupport.instance_instanceOf(to, "meta::pure::functions::relation::UnboundedFrameValue");
            Frame result;
            if (fromUn)
            {
                result = toUn ? new Frame(type, fromUn, toUn) : new Frame(type, fromUn, (int) PrimitiveUtilities.getIntegerValue(to.getValueForMetaPropertyToOne("value")));
            }
            else
            {
                result = toUn ? new Frame(type, (int) PrimitiveUtilities.getIntegerValue(from.getValueForMetaPropertyToOne("value")), toUn) : new Frame(type, (int) PrimitiveUtilities.getIntegerValue(from.getValueForMetaPropertyToOne("value")), (int) PrimitiveUtilities.getIntegerValue(to.getValueForMetaPropertyToOne("value")));
            }
            return result;
        }

        public int getLow(int currentRow)
        {
            return fromUnbounded ? 0 : Math.max(0, currentRow - offsetFrom);
        }

        public int getHigh(int currentRow, int maxSize)
        {
            return toUnbounded ? maxSize - 1 : Math.min(maxSize - 1, currentRow + offsetTo);
        }

        public CoreInstance convert(ModelRepository r, ProcessorSupport ps)
        {
            CoreInstance result = ps.newCoreInstance("", this.type == FrameType.rows ? "meta::pure::functions::relation::Rows" : "meta::pure::functions::relation::Range", null);
            CoreInstance from = ps.newCoreInstance("", this.fromUnbounded ? "meta::pure::functions::relation::UnboundedFrameValue" : "meta::pure::functions::relation::FrameIntValue", null);
            if (!this.fromUnbounded)
            {
                from.setKeyValues(Lists.mutable.with("value"), Lists.mutable.with(r.newIntegerCoreInstance(this.offsetFrom)));
            }
            CoreInstance to = ps.newCoreInstance("", this.toUnbounded ? "meta::pure::functions::relation::UnboundedFrameValue" : "meta::pure::functions::relation::FrameIntValue", null);
            if (!this.toUnbounded)
            {
                to.setKeyValues(Lists.mutable.with("value"), Lists.mutable.with(r.newIntegerCoreInstance(this.offsetTo)));
            }
            result.setKeyValues(Lists.mutable.with("offsetFrom"), Lists.mutable.with(from));
            result.setKeyValues(Lists.mutable.with("offsetTo"), Lists.mutable.with(to));
            return result;
        }
    }
}
