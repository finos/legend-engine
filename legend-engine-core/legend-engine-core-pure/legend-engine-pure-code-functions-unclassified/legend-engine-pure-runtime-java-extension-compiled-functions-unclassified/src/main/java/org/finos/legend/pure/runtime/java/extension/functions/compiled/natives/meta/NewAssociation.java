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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class NewAssociation extends AbstractNative
{
    public NewAssociation()
    {
        super("newAssociation_String_1__Property_1__Property_1__Association_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return "FunctionsGen.newAssociation(" + transformedParams.get(0) + "," + transformedParams.get(1) + "," + transformedParams.get(2) + ",((CompiledExecutionSupport)es).getMetadataAccessor(), " + NativeFunctionProcessor.buildM4SourceInformation(functionExpression.getSourceInformation()) + ")";
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "            {\n" +
                "                return FunctionsGen.newAssociation((String) vars.get(0), (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property) vars.get(1), (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property) vars.get(2), ((CompiledExecutionSupport) es).getMetadataAccessor(), null);\n" +
                "            }\n" +
                "        }";
    }
}
