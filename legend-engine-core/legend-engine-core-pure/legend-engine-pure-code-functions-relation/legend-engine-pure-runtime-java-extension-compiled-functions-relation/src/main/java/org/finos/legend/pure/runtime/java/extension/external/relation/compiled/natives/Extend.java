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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

public class Extend extends AbstractNative implements Native
{
    public Extend()
    {
        super("extend_Relation_1__FuncColSpec_1__Relation_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        StringBuilder result = buildCode(transformedParams, s -> "Lists.mutable.with(" + transformedParams.get(1) + ")");
        return result.toString();
    }

    static StringBuilder buildCode(ListIterable<String> transformedParams, Function<String, String> collection)
    {
        StringBuilder result = new StringBuilder("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.extend(");
        result.append(transformedParams.get(0) + ", ");
        result.append(collection.valueOf(transformedParams.get(1)));
        buildCollectFuncSpec(result, false);
        result.append(", es)");
        return result;
    }

    static void buildCollectFuncSpec(StringBuilder result, boolean twoArgs)
    {
        String className = "ColFuncSpecTrans" + (twoArgs ? "2" : "1");
        String functionType = "org.eclipse.collections.api.block.function.Function" + (twoArgs ? "3" : "2");
        result.append(".collect(");
        result.append("new DefendedFunction<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpec<? extends Object, ? extends Object>, org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation." + className + ">()\n" +
                "{\n" +
                "    @Override\n" +
                "    public  org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation." + className + " valueOf(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpec<?, ?> c)\n" +
                "    {\n");

        result.append("return new org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation." + className + "(");
        result.append("c._name(),");
        result.append("(" + functionType + ")PureCompiledLambda.getPureFunction(c._function(),es),");
        result.append(" ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)c._function()._classifierGenericType()._typeArguments().toList().get(0)._rawType())._returnType()._rawType()._name()\n");
        result.append(");\n");

        result.append("    }\n" +
                "   }" +
                ")");
    }
}
