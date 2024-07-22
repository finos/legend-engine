// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class LegendTest extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;

    public LegendTest(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        String clientVersion = System.getProperty("legend.test.clientVersion");
        String serverVersion = System.getProperty("legend.test.serverVersion");
        String serializationKind = System.getProperty("legend.test.serializationKind");
        String host = System.getProperty("legend.test.server.host");
        int port = System.getProperty("legend.test.server.port") == null ? -1 : Integer.parseInt(System.getProperty("legend.test.server.port"));

        if (host != null)
        {
            if (port == -1)
            {
                throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "The system variable 'legend.test.server.host' is set to '" + host + "' however 'legend.test.server.port' has not been set!");
            }
            if (serializationKind == null || !(serializationKind.equals("text") || serializationKind.equals("json")))
            {
                serializationKind = "json";
            }
            if (clientVersion == null)
            {
                throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "The system variable 'legend.test.clientVersion' should be set");
            }
            if (serverVersion == null)
            {
                throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "The system variable 'legend.test.serverVersion' should be set");
            }
            MutableList<CoreInstance> fParams = Lists.mutable.with(
                    ValueSpecificationBootstrap.newStringLiteral(this.repository, clientVersion, this.functionExecution.getProcessorSupport()),
                    ValueSpecificationBootstrap.newStringLiteral(this.repository, serverVersion, this.functionExecution.getProcessorSupport()),
                    ValueSpecificationBootstrap.newStringLiteral(this.repository, serializationKind, this.functionExecution.getProcessorSupport()),
                    ValueSpecificationBootstrap.newStringLiteral(this.repository, host, this.functionExecution.getProcessorSupport()),
                    ValueSpecificationBootstrap.newIntegerLiteral(this.repository, port, this.functionExecution.getProcessorSupport()));

            return this.functionExecution.executeFunctionExecuteParams(FunctionCoreInstanceWrapper.toFunction(Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport)),
                    fParams,
                    resolvedTypeParameters,
                    resolvedMultiplicityParameters,
                    getParentOrEmptyVariableContext(variableContext),
                    functionExpressionToUseInStack,
                    profiler,
                    instantiationContext,
                    executionSupport);
        }
        else
        {
            return this.functionExecution.executeFunctionExecuteParams(FunctionCoreInstanceWrapper.toFunction(Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport)),
                    Lists.immutable.empty(),
                    resolvedTypeParameters,
                    resolvedMultiplicityParameters,
                    getParentOrEmptyVariableContext(variableContext),
                    functionExpressionToUseInStack,
                    profiler,
                    instantiationContext,
                    executionSupport);

        }
    }
}
