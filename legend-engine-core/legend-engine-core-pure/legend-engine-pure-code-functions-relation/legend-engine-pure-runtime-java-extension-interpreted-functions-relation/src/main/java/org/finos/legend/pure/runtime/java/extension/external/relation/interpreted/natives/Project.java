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
import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpecAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpecArray;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.Shared;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSCoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TestTDSInterpreted;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.TestTDS;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;
import java.util.function.Function;

public class Project extends Shared
{
    public Project(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(functionExecution, repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance returnGenericType = getReturnGenericType(resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, processorSupport);

        ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);

        FuncColSpecArray<?, ?> funcColSpecArray = (FuncColSpecArray<?, ?>) Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        ListIterable<CoreInstance> functions = funcColSpecArray._funcSpecs().collect(c -> (CoreInstance)c._function()).toList();
        ListIterable<? extends String> names = funcColSpecArray._funcSpecs().collect(FuncColSpecAccessor::_name).toList();

        ListIterable<Pair<LambdaFunction<CoreInstance>, VariableContext>> funcs = functions.collect(f ->
                Tuples.pair((LambdaFunction<CoreInstance>) LambdaFunctionCoreInstanceWrapper.toLambdaFunction(f), this.getParentOrEmptyVariableContextForLambda(variableContext, f))
        );

        FixedSizeList<CoreInstance> parameters = Lists.fixedSize.with((CoreInstance) null);

        MutableList<TestTDSInterpreted> allRes = Lists.mutable.empty();
        for (CoreInstance instance : values)
        {
            parameters.set(0, ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport));
            int i = 0;
            MutableList<TestTDS> allTDS = Lists.mutable.empty();
            for (Pair<LambdaFunction<CoreInstance>, VariableContext> fInfo : funcs)
            {
                Type type = ((FunctionType) fInfo.getOne()._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType()._rawType();
                TestTDS tds = new TestTDSInterpreted(this.repository, processorSupport);
                Object colRes = null;
                DataType colResType = null;

                MutableList<Object> vals = Lists.mutable.empty();

                final Function<CoreInstance, Object> valExtractor = getExtractor(processorSupport, type);

                CoreInstance subResult = this.functionExecution.executeFunction(false, fInfo.getOne(), parameters, resolvedTypeParameters, resolvedMultiplicityParameters, fInfo.getTwo(), functionExpressionCallStack, profiler, instantiationContext, executionSupport);
                subResult.getValueForMetaPropertyToMany("values").forEach(c -> vals.add(valExtractor.apply(c)));

                if (type == _Package.getByUserPath("String", processorSupport))
                {
                    colResType = DataType.STRING;
                    colRes = vals.toArray(new String[0]);
                }
                if (type == _Package.getByUserPath("Integer", processorSupport))
                {
                    colResType = DataType.LONG;
                    colRes = vals.stream().mapToLong(x -> ((Number) x).longValue()).toArray();
                }
                if (type == _Package.getByUserPath("Float", processorSupport))
                {
                    colResType = DataType.DOUBLE;
                    colRes = vals.stream().mapToDouble(x -> (Double) x).toArray();
                }

                TestTDS resTDS = tds.addColumn(names.get(i++), colResType, colRes);
                if (vals.isEmpty())
                {
                    resTDS = resTDS.setNull();
                }
                allTDS.add(resTDS);
            }
            TestTDS init = allTDS.get(0);
            allRes.add(allTDS.drop(1).injectInto((TestTDSInterpreted) init, (a, b) -> (TestTDSInterpreted) a.join(b)));
        }
        TestTDSInterpreted res = allRes.get(0);
        if (allRes.size() > 1)
        {
            TestTDS init = allRes.get(0);
            res = allRes.drop(1).injectInto((TestTDSInterpreted) init, (a, b) -> (TestTDSInterpreted) a.concatenate(b));
        }
        return ValueSpecificationBootstrap.wrapValueSpecification(new TDSCoreInstance(res, returnGenericType, repository, processorSupport), false, processorSupport);
    }

    private static Function<CoreInstance, Object> getExtractor(ProcessorSupport processorSupport, Type type)
    {
        final Function<CoreInstance, Object> val;
        if (type == _Package.getByUserPath("String", processorSupport))
        {
            val = PrimitiveUtilities::getStringValue;
        }
        else if (type == _Package.getByUserPath("Integer", processorSupport))
        {
            val = PrimitiveUtilities::getIntegerValue;
        }
        else if (type == _Package.getByUserPath("Float", processorSupport))
        {
            val = PrimitiveUtilities::getFloatValue;
        }
        else
        {
            val = null;
        }
        return val;
    }
}

