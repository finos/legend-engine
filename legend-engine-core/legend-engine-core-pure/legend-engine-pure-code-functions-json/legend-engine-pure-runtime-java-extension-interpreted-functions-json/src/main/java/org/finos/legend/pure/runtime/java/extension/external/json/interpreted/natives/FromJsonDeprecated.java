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

package org.finos.legend.pure.runtime.java.extension.external.json.interpreted.natives;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonParser;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

/**
 * @deprecated use {@link FromJson} instead.
 */
@Deprecated
public class FromJsonDeprecated extends NativeFunction
{
    private final FunctionExecutionInterpreted functionExecution;

    public FromJsonDeprecated(FunctionExecutionInterpreted functionExecution, ModelRepository modelRepository)
    {
        this.functionExecution = functionExecution;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        // This is a temporary solution as it should not be referring to the other existing implementation which is in the model
        // space. It either needs its own native implementation or a function inside the platform.

        CoreInstance json = params.get(0);
        CoreInstance startingclass = params.get(1);
        CoreInstance config = params.get(2);

        String jsonText = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(json, M3Properties.values, processorSupport));
        CoreInstance jsonElement = new JsonParser(processorSupport).toPureJson(jsonText);
        CoreInstance fromContractStoreJSONFunction = processorSupport.package_getByUserPath("apps::model::contractFlow::fromJSON::fromContractStoreJSON_JSONElement_1__Class_1__JSONDeserializationConfig_1__Any_MANY_");

        ListIterable<CoreInstance> functionParameters = Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification(jsonElement, true, processorSupport), startingclass, config);

        return this.functionExecution.executeLambdaFromNative(fromContractStoreJSONFunction, functionParameters, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport);
    }
}
