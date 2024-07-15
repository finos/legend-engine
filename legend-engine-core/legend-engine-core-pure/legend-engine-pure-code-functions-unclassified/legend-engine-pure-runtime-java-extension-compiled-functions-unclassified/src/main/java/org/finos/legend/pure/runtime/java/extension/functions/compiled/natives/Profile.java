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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class Profile extends AbstractNative
{
    public Profile()
    {
        super("profile_T_m__Boolean_1__ProfileResult_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);
        CoreInstance param = parametersValues.get(0);
        String type = TypeProcessor.typeToJavaPrimitiveSingle(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.genericType, processorSupport), processorSupport);
        return "((Root_meta_pure_functions_tools_ProfileResult<" + type + ">)new LambdaZero(){public Object execute(){((CompiledExecutionSupport)es).getConsole().print(\"Profile not supported yet!\");return new Root_meta_pure_functions_tools_ProfileResult_Impl<" + type + ">(\"NOID\")._result(CompiledSupport.toPureCollection(" + transformedParams.get(0) + "))._report(\"Profile not supported yet in compiled\");}}.execute())";
    }
}
