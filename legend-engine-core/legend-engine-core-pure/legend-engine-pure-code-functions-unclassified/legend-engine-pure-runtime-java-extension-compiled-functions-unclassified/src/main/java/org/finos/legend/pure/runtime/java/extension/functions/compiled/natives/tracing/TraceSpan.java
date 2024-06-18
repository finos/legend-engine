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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.tracing;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class TraceSpan extends AbstractNative
{
    public TraceSpan()
    {
        super("traceSpan_Function_1__String_1__V_m_",
                "traceSpan_Function_1__String_1__Function_1__V_m_",
                "traceSpan_Function_1__String_1__Function_1__Boolean_1__V_m_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport());
        String type = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.genericType, processorSupport),
                Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport), processorSupport);
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport);
        String operationName = transformedParams.get(1);
        String functionToExec = transformedParams.get(0);
        String functionToGetTags = transformedParams.size() > 2 ? transformedParams.get(2) : "null";
        String tagsCritical = transformedParams.size() > 3 ? transformedParams.get(3) : "true";
        String eval = "FunctionsGen.traceSpan(es," + functionToExec + "," + operationName + "," + functionToGetTags + "," + tagsCritical + ")";
        return "((" + type + ")(Object)" + (Multiplicity.isToOne(multiplicity, false) ? eval : "CompiledSupport.toPureCollection(" + eval + ")") + ")";
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "            {\n" +
                "               Object value = FunctionsGen.traceSpan(es, (" + FullJavaPaths.Function + ") vars.get(0), \n" +
                "                               CompiledSupport.pureToString(vars.get(1), es), \n" +
                "                               vars.size() > 2 ? (" + FullJavaPaths.Function + ") vars.get(2) : null,\n" +
                "                               vars.size() > 3 ? Boolean.valueOf(CompiledSupport.pureToString(vars.get(1), es)) : true);\n" +
                "               return value instanceof Iterable ? CompiledSupport.toPureCollection(value) : value;\n" +
                "            }\n" +
                "        }";
    }

}
