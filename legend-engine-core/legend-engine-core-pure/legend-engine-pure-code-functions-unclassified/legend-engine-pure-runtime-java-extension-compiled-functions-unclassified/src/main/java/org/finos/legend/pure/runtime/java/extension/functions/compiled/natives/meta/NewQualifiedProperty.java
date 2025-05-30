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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;

import java.text.MessageFormat;

public class NewQualifiedProperty extends AbstractNative
{
    private static final String TEMPLATE = "new " + FullJavaPaths.QualifiedProperty_Impl + "<Object>(\"NOID\")" +
            "._classifierGenericType(new " + FullJavaPaths.GenericType_Impl + "(\"NOID\")._rawType((" + FullJavaPaths.Class + "<Object>)((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"meta::pure::metamodel::function::property::QualifiedProperty\"))" +
            "   ._typeArguments(Lists.immutable.of(" +
            "      new " + FullJavaPaths.GenericType_Impl + "(\"NOID\")" +
            "         ._rawType(new " + FullJavaPaths.FunctionType_Impl + "(\"NOID\")" +
            "            ._classifierGenericType(new " + FullJavaPaths.GenericType_Impl + "(\"NOID\")._rawType((" + FullJavaPaths.Class + "<Object>)((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"meta::pure::metamodel::type::FunctionType\")))" +
            "            ._returnType({2})" +
            "            ._returnMultiplicity({3})" +
            "            ._parameters((RichIterable<? extends " + FullJavaPaths.VariableExpression + ">) (Object) CompiledSupport.toPureCollection({4}))" +
            "))))" +
            "._name({0})" +
            "._functionName({0})" +
            "._genericType({2})" +
            "._multiplicity({3})" +
            "._owner((" + FullJavaPaths.Class + "<Object>)({1})._rawType())";

    public NewQualifiedProperty()
    {
        super("newQualifiedProperty_String_1__GenericType_1__GenericType_1__Multiplicity_1__VariableExpression_MANY__QualifiedProperty_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return MessageFormat.format(TEMPLATE, transformedParams.get(0), transformedParams.get(1), transformedParams.get(2), transformedParams.get(3), transformedParams.get(4));
    }

    @Override
    public String buildBody()
    {
        String newQualifiedProperty = MessageFormat.format(TEMPLATE, "(String) vars.get(0)", "(" + FullJavaPaths.GenericType + ") vars.get(1)", "(" + FullJavaPaths.GenericType + ") vars.get(2)", "(" + FullJavaPaths.Multiplicity + ") vars.get(3)", "vars.get(4)");
        return "new SharedPureFunction<Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "            {\n" +
                "                return " + newQualifiedProperty + ";\n" +
                "            }\n" +
                "        }";
    }
}
