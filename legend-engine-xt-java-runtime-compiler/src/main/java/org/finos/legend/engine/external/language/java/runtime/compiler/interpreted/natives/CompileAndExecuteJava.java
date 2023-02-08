// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.language.java.runtime.compiler.interpreted.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.external.language.java.runtime.compiler.shared.PureCompileAndExecuteJava;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
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

public class CompileAndExecuteJava extends NativeFunction
{
    public CompileAndExecuteJava(FunctionExecutionInterpreted functionExecution, ModelRepository modelRepository)
    {
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        ListIterable<? extends CoreInstance> javaSources = params.get(0).getValueForMetaPropertyToMany(M3Properties.values);
        CoreInstance compilationConfig = params.get(1).getValueForMetaPropertyToOne(M3Properties.values);
        CoreInstance executionConfig = params.get(2).getValueForMetaPropertyToOne(M3Properties.values);
        CoreInstance result = PureCompileAndExecuteJava.compileAndExecutePure(javaSources, compilationConfig, executionConfig, processorSupport);
        return ValueSpecificationBootstrap.wrapValueSpecification(result, false, processorSupport);
    }
}
