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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.string;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.functions.shared.string.RegexpParameter;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativePredicate;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;
import java.util.regex.Pattern;

public class RegexpLike extends NativePredicate
{
    public RegexpLike(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(repository);
    }

    @Override
    protected boolean executeBoolean(Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, ListIterable<? extends CoreInstance> params, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport) throws PureExecutionException
    {
        String string = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport).getName();
        String regexp = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport).getName();
        ListIterable<? extends CoreInstance> enumerations = Instance.getValueForMetaPropertyToManyResolved(params.get(2), M3Properties.values, processorSupport);
        int patternFlags = 0;
        for (CoreInstance enumeration : enumerations)
        {
            String enumName = enumeration.getName();
            RegexpParameter regexpParameter = RegexpParameter.valueOf(enumName);
            patternFlags |= RegexpParameter.toPatternFlag(regexpParameter);
        }

        Pattern pattern = Pattern.compile(regexp, patternFlags);
        return pattern.matcher(string).find();
    }
}
