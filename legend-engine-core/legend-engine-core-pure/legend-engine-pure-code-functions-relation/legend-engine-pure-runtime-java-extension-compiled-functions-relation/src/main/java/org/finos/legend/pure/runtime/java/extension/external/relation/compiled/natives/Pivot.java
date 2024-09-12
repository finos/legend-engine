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

public class Pivot extends AbstractNative implements Native
{
    public Pivot()
    {
        super("pivot_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_", "pivot_Relation_1__ColSpecArray_1__AggColSpec_1__Relation_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        StringBuilder result = new StringBuilder("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.pivot");
        result.append('(');
        result.append(transformedParams.get(0));
        result.append(", ");
        result.append(transformedParams.get(1));
        result.append(", ");
        result.append("Lists.mutable.with(" + transformedParams.get(2) + ")");
        Pivot.processAggColSpec(result);
        result.append(", es)");
        return result.toString();
    }

    static void processAggColSpec(StringBuilder result)
    {
        String className = "AggColSpecTrans1";

        result.append(".collect(");
        result.append("new DefendedFunction<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpec<? extends Object, ? extends Object, ? extends Object>, org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation." + className + ">()\n" +
                "{\n" +
                "    @Override\n" +
                "    public  org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation." + className + " valueOf(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpec<?, ?, ?> c)\n" +
                "    {\n");
        result.append("return new org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation." + className + "(c._name(),");
        result.append("PureCompiledLambda.getPureFunction(c._map(),es),");
        result.append("(Function2)PureCompiledLambda.getPureFunction(c._reduce(),es),");
        result.append("((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)c._reduce()._classifierGenericType()._typeArguments().toList().get(0)._rawType())._returnType()._rawType()._name(),");
        result.append("c);");
        result.append("    }\n" +
                "})");
    }
}
