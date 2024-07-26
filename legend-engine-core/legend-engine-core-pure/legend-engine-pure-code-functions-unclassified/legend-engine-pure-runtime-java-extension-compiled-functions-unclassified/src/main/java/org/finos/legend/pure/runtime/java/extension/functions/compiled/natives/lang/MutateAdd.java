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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class MutateAdd extends AbstractNative
{
    public MutateAdd()
    {
        super("mutateAdd_T_1__String_1__Any_MANY__T_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        SourceInformation sourceInformation = functionExpression.getSourceInformation();

        return "FunctionsGen.mutateAdd(" +
                transformedParams.get(0) + "," +
                transformedParams.get(1) + "," +
                "CompiledSupport.toPureCollection(" + transformedParams.get(2) + ")," +
                NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) +
                ")";
    }
}
