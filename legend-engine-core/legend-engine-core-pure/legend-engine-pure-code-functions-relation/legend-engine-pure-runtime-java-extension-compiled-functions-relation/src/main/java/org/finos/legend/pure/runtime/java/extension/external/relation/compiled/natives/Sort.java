// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

public class Sort extends AbstractNative implements Native
{
    public Sort()
    {
        super("sort_Relation_1__SortInfo_MANY__Relation_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        StringBuilder result = new StringBuilder("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.sort");
        result.append('(');
        result.append(transformedParams.get(0));
        result.append(", ");
        processSortInfo(result, transformedParams.get(1));
        result.append(" , es)\n");
        return result.toString();
    }

    public static void processSortInfo(StringBuilder result, String param)
    {
        result.append("CompiledSupport.toPureCollection(" + param + ")");
        result.append(".collect(new DefendedFunction<Root_meta_pure_functions_relation_SortInfo<? extends Object>, org.eclipse.collections.api.tuple.Pair<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum, String>>()\n" +
                "{\n" +
                "    @Override\n" +
                "    public org.eclipse.collections.api.tuple.Pair<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum, String> valueOf(Root_meta_pure_functions_relation_SortInfo<?> it)\n" +
                "    {\n" +
                "        return org.eclipse.collections.impl.tuple.Tuples.pair(it._direction(), it._column()._name());\n" +
                "    }\n" +
                "})");
    }
}