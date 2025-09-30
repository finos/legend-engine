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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.functions.shared.string.RegexpParameter;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.NumericUtilities;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexpExtract extends NativeFunction
{
    private final ModelRepository repository;

    public RegexpExtract(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        String string = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport).getName();
        String regexp = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport).getName();
        boolean extractAll = PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport));
        int groupNumber = NumericUtilities.toJavaNumber(Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport), processorSupport).intValue();
        ListIterable<? extends CoreInstance> enumerations = Instance.getValueForMetaPropertyToManyResolved(params.get(4), M3Properties.values, processorSupport);
        int patternFlags = 0;
        for (CoreInstance enumeration : enumerations)
        {
            String enumName = enumeration.getName();
            RegexpParameter regexpParameter = RegexpParameter.valueOf(enumName);
            patternFlags |= RegexpParameter.toPatternFlag(regexpParameter);
        }

        Pattern pattern = Pattern.compile(regexp, patternFlags);
        Matcher matcher = pattern.matcher(string);
        MutableList<CoreInstance> result = FastList.newList();
        while (matcher.find())
        {
            if (groupNumber == 0)
            {
                result.add(this.repository.newStringCoreInstance(matcher.group()));
            }
            else if (groupNumber <= matcher.groupCount())
            {
                result.add(this.repository.newStringCoreInstance(matcher.group(groupNumber)));
            }
            else
            {
                result.add(null);
            }
            if (!extractAll)
            {
                break;
            }
        }

        return ValueSpecificationBootstrap.wrapValueSpecification(result, true, processorSupport);
    }
}
