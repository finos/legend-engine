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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

public class ExtendWindowFuncArray extends AbstractNative implements Native
{
    public ExtendWindowFuncArray()
    {
        super("extend_Relation_1___Window_1__FuncColSpecArray_1__Relation_1_");
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "{\n" +
                "   @Override\n" +
                "   public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "   {\n" +
                "       return " + ExtendWindowFunc.buildCode(Lists.mutable.with("(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Relation<? extends Object>)vars.get(0)", "(Root_meta_pure_functions_relation__Window<? extends Object>)vars.get(1)", "vars.get(2)"), s -> "((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpecArray)vars.get(2))._funcSpecs().toList()") + ";" +
                "   }\n" +
                "\n}";
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        StringBuilder result = ExtendWindowFunc.buildCode(transformedParams, s -> transformedParams.get(2) + "._funcSpecs().toList()");
        return result.toString();
    }
}

