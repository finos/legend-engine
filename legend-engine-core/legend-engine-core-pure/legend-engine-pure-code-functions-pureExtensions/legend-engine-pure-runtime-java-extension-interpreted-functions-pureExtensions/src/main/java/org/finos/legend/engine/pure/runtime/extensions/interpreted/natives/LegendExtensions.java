//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.pure.runtime.extensions.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.pure.code.core.LegendPureCoreExtension;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
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

public class LegendExtensions extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    private final ModelRepository repository;

    public LegendExtensions(FunctionExecutionInterpreted functionExecution, ModelRepository modelRepository)
    {
        this.functionExecution = functionExecution;
        this.repository = modelRepository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        MutableList<String> allSignatures = PureCoreExtensionLoader.extensions().collect(LegendPureCoreExtension::functionSignature);
        if (allSignatures.contains(null))
        {
            String message = PureCoreExtensionLoader.extensions().select(c -> c.functionSignature() == null).collect(z -> z.getClass().getSimpleName() + " didn't define a functionSignature!").makeString(", ");
            throw new PureExecutionException(message, functionExpressionCallStack);
        }
        return ValueSpecificationBootstrap.wrapValueSpecification(allSignatures.flatCollect(x -> eval(x, functionExpressionCallStack)), true, processorSupport);
    }

    private ListIterable<? extends CoreInstance> eval(String name, MutableStack<CoreInstance> functionExpressionCallStack) throws PureExecutionException
    {
        CoreInstance func = _Package.getByUserPath(name, functionExecution.getRuntime().getProcessorSupport());
        if (func == null)
        {
            throw new PureExecutionException("The function '" + name + "' can't be found in the Pure graph.", functionExpressionCallStack);
        }
        CoreInstance ci = functionExecution.start(func, Lists.mutable.empty());
        return ci.getValueForMetaPropertyToMany("values");
    }
}
