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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;

import static org.finos.legend.pure.runtime.java.compiled.generation.GenericTypeSerializationInCode.generateGenericTypeBuilder;

public class WrapPrimitiveInTDS extends AbstractNative implements Native
{
    public WrapPrimitiveInTDS()
    {
        super("wrapPrimitiveInTDS_T_$0_1$__T_1__TDS_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return getString(transformedParams, functionExpression, processorContext);
    }

    private static String getString(ListIterable<String> transformedParams, CoreInstance functionExpression, ProcessorContext processorContext)
    {
        StringBuilder result = new StringBuilder("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.build(");
        result.append(transformedParams.get(0));
        result.append(",");
        result.append(generateGenericTypeBuilder((GenericType) functionExpression.getValueForMetaPropertyToMany("parametersValues").get(0).getValueForMetaPropertyToOne("genericType"), processorContext));
        result.append(",es)");
        return result.toString();
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction2<Object, " + FullJavaPaths.GenericType + ", Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(Object value, " + FullJavaPaths.GenericType + " type, ExecutionSupport es)\n" +
                "            {\n" +
                "                return org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.build(value, type, es);\n" +
                "            }\n" +
                "        }\n";
    }
}
