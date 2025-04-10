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
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;

import java.text.MessageFormat;

public class NewLambdaFunction extends AbstractNative
{
    private static final String TEMPLATE = "new " + FullJavaPaths.LambdaFunction_Impl + "<Object>(\"NOID\")" +
            "._classifierGenericType(new " + FullJavaPaths.GenericType_Impl + "(\"NOID\")._rawType((" + FullJavaPaths.Class + "<Object>)((CompiledExecutionSupport)es).getMetadataAccessor().getClass(\"meta::pure::metamodel::function::LambdaFunction\"))" +
            "._typeArguments(Lists.immutable.of(new " + FullJavaPaths.GenericType_Impl + "(\"NOID\")._rawType({0}))))";

    public NewLambdaFunction()
    {
        super("newLambdaFunction_FunctionType_1__LambdaFunction_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return MessageFormat.format(TEMPLATE, transformedParams.get(0));
    }

    @Override
    public String buildBody()
    {

        String newLambda = MessageFormat.format(TEMPLATE, "(" + FullJavaPaths.FunctionType + ")vars.get(0)");

        return "new SharedPureFunction<Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "            {\n" +
                "                return " + newLambda + ";\n" +
                "            }\n" +
                "        }";
    }
}
