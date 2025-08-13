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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared;

import io.deephaven.csv.parsers.DataType;
import java.util.Arrays;
import java.util.Stack;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Sort;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.ColumnValue;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Frame;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.FrameType;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortInfo;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Window;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NumericUtilities;
import org.finos.legend.pure.runtime.java.interpreted.natives.grammar.math.operation.NumericAccumulator;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

public class ProjectExtend extends AggregationShared
{
    private final boolean includeExistingColumns;

    public ProjectExtend(boolean includeExistingColumns, FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);

        this.includeExistingColumns = includeExistingColumns;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, processorSupport);

        TestTDS tds = getTDS(params, 0, processorSupport);

        TestTDS targetTds = this.includeExistingColumns ? tds : tds.removeColumns(tds.getColumnNames().toSet());

        RelationType<?> relationType = getRelationType(params, 0);
        GenericType sourceRelationType = (GenericType) params.get(0).getValueForMetaPropertyToOne("genericType");

        CoreInstance secondParameter = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);

            TestTDS result;
            if (secondParameter instanceof FuncColSpec)
            {
                result = targetTds.addColumn(processFuncColSpec(tds.wrapFullTDS(), new Window(new Frame(FrameType.rows, true, true)), (FuncColSpec<?, ?>) secondParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, (GenericType) params.get(0).getValueForMetaPropertyToOne("genericType"), false));
            }
            else if (secondParameter instanceof FuncColSpecArray)
            {
                result = ((FuncColSpecArray<?, ?>) secondParameter)._funcSpecs().injectInto(
                        targetTds,
                        (a, funcColSpec) -> a.addColumn(processFuncColSpec(tds.wrapFullTDS(), new Window(new Frame(FrameType.rows, true, true)), funcColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, sourceRelationType, false))
                );
            }
            else if (secondParameter instanceof AggColSpec)
            {
                Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source = tds.wrapFullTDS();
                result = targetTds.addColumn(processOneAggColSpec(source, Lists.fixedSize.empty(), new Window(new Frame(FrameType.rows, true, true)), (AggColSpec<?, ?, ?>) secondParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, false, null));
            }
            else if (secondParameter instanceof AggColSpecArray)
            {
                Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source = tds.wrapFullTDS();
                result = ((AggColSpecArray<?, ?, ?>) secondParameter)._aggSpecs().injectInto(
                        targetTds,
                        (a, aggColSpec) -> a.addColumn(processOneAggColSpec(source, Lists.fixedSize.empty(), new Window(new Frame(FrameType.rows, true, true)), aggColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, false, null))
                );
            }
            else if (Instance.instanceOf(secondParameter, "meta::pure::functions::relation::_Window", processorSupport))
            {
                MutableList<String> partitionIds = secondParameter.getValueForMetaPropertyToMany("partition").collect(PrimitiveUtilities::getStringValue).toList();
                Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source = partitionIds.isEmpty() ? tds.wrapFullTDS() : tds.sort(partitionIds.collect(c -> new SortInfo(c, SortDirection.ASC)));

            ListIterable<? extends CoreInstance> sortInfos = secondParameter.getValueForMetaPropertyToMany("sortInfo");
            MutableList<SortInfo> newSortInfos = Sort.getSortInfos(sortInfos, processorSupport).toList();
            final Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sortedPartitions = TestTDS.sortPartitions(newSortInfos, source);

            Window window = Window.build(secondParameter, processorSupport, new RepoPrimitiveHandler(repository));

            CoreInstance thirdParameter = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
            if (thirdParameter instanceof AggColSpec)
            {
                result = sortedPartitions.getOne().addColumn(processOneAggColSpec(sortedPartitions, newSortInfos, window, (AggColSpec<?, ?, ?>) thirdParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, true, sourceRelationType));
            }
            else if (thirdParameter instanceof AggColSpecArray)
            {
                result = ((AggColSpecArray<?, ?, ?>) thirdParameter)._aggSpecs().injectInto(
                        sortedPartitions.getOne(),
                        (a, aggColSpec) -> a.addColumn(processOneAggColSpec(sortedPartitions, newSortInfos, window, aggColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, false, true, sourceRelationType))
                );
            }
            else if (thirdParameter instanceof FuncColSpec)
            {
                result = sortedPartitions.getOne().addColumn(processFuncColSpec(sortedPartitions, window, (FuncColSpec<?, ?>) thirdParameter, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, sourceRelationType, true));
            }
            else if (thirdParameter instanceof FuncColSpecArray)
            {
                result = ((FuncColSpecArray<?, ?>) thirdParameter)._funcSpecs().injectInto(
                        sortedPartitions.getOne(),
                        (a, funcColSpec) -> a.addColumn(processFuncColSpec(sortedPartitions, window, funcColSpec, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, sourceRelationType, true))
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

    private ColumnValue processFuncColSpec(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source, Window window, FuncColSpec<?, ?> funcColSpec, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, GenericType relationType, boolean twoParamsFunc)
    {
        LambdaFunction<CoreInstance> lambdaFunction = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(funcColSpec.getValueForMetaPropertyToOne(M3Properties.function));
        String name = funcColSpec.getValueForMetaPropertyToOne(M3Properties.name).getName();

        VariableContext evalVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, lambdaFunction);

        Type type = ((FunctionType) lambdaFunction._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._rawType();

        if (type == _Package.getByUserPath(M3Paths.String, processorSupport))
        {
            String[] finalRes = new String[(int) source.getOne().getRowCount()];
            processOneColumn(source, window, lambdaFunction, (j, val) -> finalRes[j] = val == null ? null : PrimitiveUtilities.getStringValue(val), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, evalVarContext, twoParamsFunc);
            return new ColumnValue(name, DataType.STRING, finalRes);
        }
        else if (type == _Package.getByUserPath(M3Paths.Integer, processorSupport))
        {
            long[] finalRes = new long[(int) source.getOne().getRowCount()];
            boolean[] nulls = new boolean[(int) source.getOne().getRowCount()];
            Arrays.fill(nulls, Boolean.FALSE);
            processOneColumn(source, window, lambdaFunction, (j, val) -> processWithNull(j, val, nulls, () -> finalRes[j] = PrimitiveUtilities.getIntegerValue(val).intValue()), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, evalVarContext, twoParamsFunc);
            return new ColumnValue(name, DataType.LONG, finalRes, nulls);
        }
        else if (type == _Package.getByUserPath(M3Paths.Float, processorSupport))
        {
            double[] finalRes = new double[(int) source.getOne().getRowCount()];
            boolean[] nulls = new boolean[(int) source.getOne().getRowCount()];
            Arrays.fill(nulls, Boolean.FALSE);
            processOneColumn(source, window, lambdaFunction, (j, val) -> processWithNull(j, val, nulls, () -> finalRes[j] = PrimitiveUtilities.getFloatValue(val).doubleValue()), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, evalVarContext, twoParamsFunc);
            return new ColumnValue(name, DataType.DOUBLE, finalRes, nulls);
        }
        else if (type == _Package.getByUserPath(M3Paths.Variant, processorSupport))
        {
            Variant[] finalRes = new Variant[(int) source.getOne().getRowCount()];
            processOneColumn(source, window, lambdaFunction, (j, val) -> finalRes[j] = val == null ? null : (Variant) val, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, evalVarContext, twoParamsFunc);
            return new ColumnValue(name, DataType.CUSTOM, finalRes);
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

    private void processOneColumn(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> source, Window window, LambdaFunction<CoreInstance> lambdaFunction, Procedure2<Integer, CoreInstance> setter, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, GenericType relationType, VariableContext evalVarContext, boolean twoParamsFunc)
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
                    parameters.set(1, ValueSpecificationBootstrap.wrapValueSpecification(window.convert(processorSupport, new RepoPrimitiveHandler(repository)), false, processorSupport));
                }
                parameters.set(twoParamsFunc ? 2 : 0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(source.getOne(), i, "", null, relationType, -1, repository, false), false, processorSupport));
                CoreInstance newValue = this.functionExecution.executeFunction(false, lambdaFunction, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, evalVarContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
                setter.value(k++, newValue.getValueForMetaPropertyToOne("values"));
            }
        }
    }

    public static class RepoPrimitiveHandler implements Frame.PrimitiveHandler
    {
        private final ModelRepository repository;

        public RepoPrimitiveHandler(ModelRepository repository)
        {
            this.repository = repository;
        }

        @Override
        public CoreInstance build(String val)
        {
            return this.repository.newStringCoreInstance(val);
        }

        @Override
        public CoreInstance build(Number val)
        {
            return NumericUtilities.toPureNumber(val, true, this.repository);
        }

        @Override
        public Number plus(Number left, Number right)
        {
            NumericAccumulator accumulator = NumericAccumulator.newAccumulator(left);
            accumulator.add(right);
            return accumulator.getValue();
        }

        @Override
        public Number minus(Number left, Number right)
        {
            NumericAccumulator accumulator = NumericAccumulator.newAccumulator(left);
            accumulator.subtract(right);
            return accumulator.getValue();
        }

        @Override
        public boolean lessThanEqual(Number left, Number right)
        {
            return NumericUtilities.compare(left, right) <= 0;
        }

        @Override
        public Number toJavaNumber(CoreInstance coreInstance, ProcessorSupport processorSupport)
        {
            return NumericUtilities.toJavaNumber(coreInstance, processorSupport);
        }
    }
}
