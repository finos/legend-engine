//  Copyright 2025 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.deephaven.test.interpreted;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.shared.DeephavenCommands;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class GetDeephavenTestConnection extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;

    public GetDeephavenTestConnection(FunctionExecutionInterpreted functionExecution, ModelRepository modelRepository)
    {
        this.functionExecution = functionExecution;
        this.repository = modelRepository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance imageTagCoreInstance = params.get(0);
        String imageTag = Instance.getValueForMetaPropertyToOneResolved(imageTagCoreInstance, M3Properties.values, processorSupport).getName();
        Root_meta_pure_functions_io_http_URL url = DeephavenCommands.startServer(imageTag);
        GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl(null, null, _Package.getByUserPath("meta::pure::metamodel::type::generics::GenericType", processorSupport))
                ._rawType((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type) _Package.getByUserPath("meta::pure::functions::io::http::URL", processorSupport));
        url._classifierGenericType(genericType);
        return ValueSpecificationBootstrap.wrapValueSpecification(url, true, processorSupport);
    }
}
