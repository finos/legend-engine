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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.functions.shared.string.CsvParseHelper;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.List;
import java.util.Stack;

public class ParseCSV extends NativeFunction
{
    private final ModelRepository repository;

    public ParseCSV(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        String csv = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport).getName();

        List<List<String>> parsed = CsvParseHelper.parseCSV(csv);

        CoreInstance listType = processorSupport.package_getByUserPath(M3Paths.List);
        CoreInstance stringType = processorSupport.package_getByUserPath(M3Paths.String);
        CoreInstance genericTypeType = processorSupport.package_getByUserPath(M3Paths.GenericType);

        CoreInstance stringGenericType = this.repository.newAnonymousCoreInstance(null, genericTypeType);
        Instance.addValueToProperty(stringGenericType, M3Properties.rawType, stringType, processorSupport);

        MutableList<CoreInstance> listInstances = Lists.mutable.ofInitialCapacity(parsed.size());
        for (List<String> row : parsed)
        {
            MutableList<CoreInstance> stringValues = Lists.mutable.ofInitialCapacity(row.size());
            for (String field : row)
            {
                stringValues.add(this.repository.newStringCoreInstance(field));
            }
            CoreInstance listInstance = processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.List);
            Instance.addValueToProperty(listInstance, M3Properties.values, stringValues, processorSupport);

            CoreInstance listGenericType = this.repository.newAnonymousCoreInstance(null, genericTypeType);
            Instance.addValueToProperty(listGenericType, M3Properties.rawType, listType, processorSupport);
            Instance.addValueToProperty(listGenericType, M3Properties.typeArguments, stringGenericType, processorSupport);
            Instance.addValueToProperty(listInstance, M3Properties.classifierGenericType, listGenericType, processorSupport);

            listInstances.add(listInstance);
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(listInstances, ValueSpecification.isExecutable(params.get(0), processorSupport), processorSupport);
    }
}


