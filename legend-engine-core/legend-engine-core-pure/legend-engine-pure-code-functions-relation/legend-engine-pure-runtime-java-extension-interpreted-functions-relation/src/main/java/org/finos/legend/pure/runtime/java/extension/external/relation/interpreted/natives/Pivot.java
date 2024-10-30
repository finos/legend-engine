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

import io.deephaven.csv.parsers.DataType;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.Shared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSWithCursorCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.SortInfo;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Objects;
import java.util.Stack;

public class Pivot extends Shared
{
    public Pivot(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        TestTDS tds = getTDS(params, 0, processorSupport);
        RelationType<?> relationType = getRelationType(params, 0);

        CoreInstance pivotColSpec = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        ListIterable<String> pivotCols;
        if (pivotColSpec instanceof ColSpec)
        {
            pivotCols = Lists.mutable.with(((ColSpec) pivotColSpec)._name());
        }
        else if (pivotColSpec instanceof ColSpecArray)
        {
            pivotCols = ((ColSpecArray) pivotColSpec)._names().toList();
        }
        else
        {
            throw new RuntimeException("Not Possible");
        }

        CoreInstance aggColSpec = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
        ListIterable<AggColSpec> aggColSpecs;
        if (aggColSpec instanceof AggColSpec)
        {
            aggColSpecs = Lists.mutable.with(((AggColSpec) aggColSpec));
        }
        else if (aggColSpec instanceof AggColSpecArray)
        {
            aggColSpecs = ((AggColSpecArray) aggColSpec)._aggSpecs().toList();
        }
        else
        {
            throw new RuntimeException("Not Possible");
        }

        // TODO: right now we make assumption that the map expression is really simple so we can safely extract the column(s)
        // used for aggregation, we make sure these column(s) are not part of the groupBy calculation
        ListIterable<String> columnsUsedInAggregation = aggColSpecs.collect(col ->
        {
            try
            {
                ValueSpecification lambda = ((LambdaFunction<?>) col._map())._expressionSequence().getFirst();
                if (lambda instanceof SimpleFunctionExpression && ((SimpleFunctionExpression) lambda)._func() instanceof Column)
                {
                    return ((SimpleFunctionExpression) lambda)._func()._name();
                }
                return null;
            }
            catch (Exception e)
            {
                // do nothing, the shape is not as expected, we will try to inspect no further
                return null;
            }
        }).select(Objects::nonNull);

        // these are the columns not being aggregated on, which will be used for groupBy calculation before transposing
        ListIterable<String> groupByColumns = tds.getColumnNames().reject(c -> columnsUsedInAggregation.anySatisfy(a -> a.equals(c)) || pivotCols.anySatisfy(a -> a.equals(c))).withAll(pivotCols);

        // create the big group-by table by processing all aggregations
        Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sorted = tds.sort(groupByColumns.collect(c -> new SortInfo(c, SortDirection.ASC)));
        TestTDS temp = aggColSpecs.injectInto(null, (a, b) -> processOneAggColSpec(a, b, sorted, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType));

        // transposing the table to complete pivoting
        TestTDS result = temp.applyPivot(groupByColumns.reject(pivotCols::contains), pivotCols, aggColSpecs.collect(AggColSpecAccessor::_name));

        // populate the generic type for the return at execution time since it cannot be reasoned out at compile time
        GenericType returnGenericType = (GenericType) getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, processorSupport);
        returnGenericType._typeArguments(Lists.mutable.with(
                ((GenericType) processorSupport.newGenericType(null, returnGenericType, true))._rawType(_RelationType.build(result.getColumnWithTypes().collect(c ->
                {
                    String type;
                    switch (c.getTwo())
                    {
                        case FLOAT:
                        {
                            type = M3Paths.Float;
                            break;
                        }
                        case DOUBLE:
                        {
                            type = M3Paths.Decimal;
                            break;
                        }
                        case INT:
                        {
                            type = M3Paths.Integer;
                            break;
                        }
                        case CHAR:
                        case STRING:
                        {
                            type = M3Paths.String;
                            break;
                        }
                        default:
                            throw new RuntimeException("ERROR " + c.getTwo() + " not supported in pivot!");
                    }
                    return _Column.getColumnInstance(c.getOne(), false, type, (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport), null, processorSupport);
                }), functionExpressionCallStack.peek().getSourceInformation(), processorSupport))
        ));

        return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(result, returnGenericType, repository, processorSupport), false, processorSupport);
    }

    private TestTDS processOneAggColSpec(TestTDS tds, AggColSpec aggColSpec, Pair<TestTDS, MutableList<Pair<Integer, Integer>>> sorted, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, RelationType<?> relationType)
    {
        String name = aggColSpec.getValueForMetaPropertyToOne("name").getName();
        LambdaFunction<CoreInstance> mapF = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggColSpec.getValueForMetaPropertyToOne("map"));
        LambdaFunction<CoreInstance> reduceF = (LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(aggColSpec.getValueForMetaPropertyToOne("reduce"));

        VariableContext mapFVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, mapF);
        VariableContext reduceFVarContext = this.getParentOrEmptyVariableContextForLambda(variableContext, reduceF);

        Type type = ((FunctionType) reduceF._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._rawType();

        FixedSizeList<CoreInstance> parameters = Lists.fixedSize.with((CoreInstance) null);

        int size = sorted.getTwo().size();
        DataType resType = null;
        Object _finalRes = null;
        if (type == _Package.getByUserPath("String", processorSupport))
        {
            String[] finalRes = new String[size];
            performAggregation(sorted, mapF, reduceF, (j, val) -> finalRes[j] = PrimitiveUtilities.getStringValue(val), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, size, parameters, mapFVarContext, reduceFVarContext);
            resType = DataType.STRING;
            _finalRes = finalRes;
        }
        if (type == _Package.getByUserPath("Integer", processorSupport))
        {
            int[] finalRes = new int[size];
            performAggregation(sorted, mapF, reduceF, (j, val) -> finalRes[j] = PrimitiveUtilities.getIntegerValue(val).intValue(), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, size, parameters, mapFVarContext, reduceFVarContext);
            resType = DataType.INT;
            _finalRes = finalRes;
        }
        if (type == _Package.getByUserPath("Float", processorSupport))
        {
            double[] finalRes = new double[size];
            performAggregation(sorted, mapF, reduceF, (j, val) -> finalRes[j] = PrimitiveUtilities.getFloatValue(val).doubleValue(), resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport, processorSupport, relationType, size, parameters, mapFVarContext, reduceFVarContext);
            resType = DataType.FLOAT;
            _finalRes = finalRes;
        }
        return tds == null ? sorted.getOne()._distinct(sorted.getTwo()).addColumn(name, resType, _finalRes) : tds.addColumn(name, resType, _finalRes);
    }

    private void performAggregation(Pair<TestTDS, MutableList<Pair<Integer, Integer>>> res, LambdaFunction<CoreInstance> mapF, LambdaFunction<CoreInstance> reduceF, Procedure2<Integer, CoreInstance> setter, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, RelationType<?> relationType, int size, FixedSizeList<CoreInstance> parameters, VariableContext mapFVarContext, VariableContext reduceFVarContext)
    {
        for (int j = 0; j < size; j++)
        {
            Pair<Integer, Integer> r = res.getTwo().get(j);
            MutableList<CoreInstance> subList = Lists.mutable.empty();
            for (int i = r.getOne(); i < r.getTwo(); i++)
            {
                parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(new TDSWithCursorCoreInstance(res.getOne(), i, "", null, relationType, -1, repository, false), true, processorSupport));
                subList.add(this.functionExecution.executeFunction(false, mapF, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, mapFVarContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport).getValueForMetaPropertyToOne("values"));
            }
            parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(subList, true, processorSupport));
            CoreInstance re = this.functionExecution.executeFunction(false, reduceF, parameters, resolvedTypeParameters, resolvedMultiplicityParameters, reduceFVarContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
            setter.value(j, re.getValueForMetaPropertyToOne("values"));
        }
    }
}
