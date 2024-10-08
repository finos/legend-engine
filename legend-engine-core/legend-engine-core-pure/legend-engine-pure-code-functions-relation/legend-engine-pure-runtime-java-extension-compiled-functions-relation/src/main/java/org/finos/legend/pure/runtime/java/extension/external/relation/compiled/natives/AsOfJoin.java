// Copyright 2024 Goldman Sachs
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

public class AsOfJoin extends AbstractNative implements Native
{
    public AsOfJoin()
    {
        super("asOfJoin_Relation_1__Relation_1__Function_1__Relation_1_", "asOfJoin_Relation_1__Relation_1__Function_1__Function_1__Relation_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        StringBuilder result = new StringBuilder("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.asOfJoin");
        result.append('(');
        result.append(transformedParams.get(0));
        result.append(", ");
        result.append(transformedParams.get(1));
        result.append(", ");
        result.append("(org.eclipse.collections.api.block.function.Function3)PureCompiledLambda.getPureFunction(");
        result.append(transformedParams.get(2));
        result.append(",es)\n");
        result.append(", ");
        result.append(transformedParams.get(2));
        if (transformedParams.size() > 3)
        {
            result.append(", ");
            result.append("(org.eclipse.collections.api.block.function.Function3)PureCompiledLambda.getPureFunction(");
            result.append(transformedParams.get(3));
            result.append(",es)\n");
        }
        result.append(", ");
        result.append("es)");
        return result.toString();
    }
}
