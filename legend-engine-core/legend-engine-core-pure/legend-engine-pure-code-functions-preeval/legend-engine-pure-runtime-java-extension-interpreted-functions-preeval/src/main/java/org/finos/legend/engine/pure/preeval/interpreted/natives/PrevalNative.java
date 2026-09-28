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

package org.finos.legend.engine.pure.preeval.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.engine.pure.preeval.Preevaluator;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalState;
import org.finos.legend.engine.pure.preeval.interpreted.InterpretedPrevalHooks;
import org.finos.legend.engine.pure.preeval.interpreted.InterpretedPrevalRuntime;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
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

public class PrevalNative extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;
    private final ModelRepository repository;

    public PrevalNative(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.functionExecution = functionExecution;
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        InterpretedPrevalRuntime runtime = new InterpretedPrevalRuntime(this.functionExecution, this.repository, processorSupport, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
        CoreInstance item = params.get(0).getValueForMetaPropertyToOne(M3Properties.values);
        CoreInstance hooks = Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport);
        CoreInstance debug = Instance.getValueForMetaPropertyToOneResolved(params.get(4), M3Properties.values, processorSupport);
        CoreInstance stopPreeval = Instance.getValueForMetaPropertyToOneResolved(hooks, "stopPreeval", processorSupport);
        CoreInstance shouldInline = Instance.getValueForMetaPropertyToOneResolved(hooks, "shouldInline", processorSupport);
        CoreInstance isGeneratedMilestoningProperty = Instance.getValueForMetaPropertyToOneResolved(hooks, "isGeneratedMilestoningProperty", processorSupport);
        PrevalState state = PrevalState.initial(runtime.toVars(params.get(1)), runtime.toVars(params.get(2)), PrimitiveUtilities.getBooleanValue(debug.getValueForMetaPropertyToOne("debug")));
        PrevalResult result = new Preevaluator(runtime, new InterpretedPrevalHooks(runtime,
                stopPreeval, getParentOrEmptyVariableContextForLambda(variableContext, stopPreeval),
                shouldInline, getParentOrEmptyVariableContextForLambda(variableContext, shouldInline),
                isGeneratedMilestoningProperty, getParentOrEmptyVariableContextForLambda(variableContext, isGeneratedMilestoningProperty))).preval(item, state);
        return ValueSpecificationBootstrap.wrapValueSpecification(runtime.toPureResult(result), false, processorSupport);
    }
}
