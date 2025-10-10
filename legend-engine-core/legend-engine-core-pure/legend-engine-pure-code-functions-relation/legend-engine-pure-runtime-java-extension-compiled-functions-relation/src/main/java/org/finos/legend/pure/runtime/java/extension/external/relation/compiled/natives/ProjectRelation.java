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
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

public class ProjectRelation extends AbstractNative implements Native
{
    public ProjectRelation()
    {
        super("project_Relation_1__FuncColSpecArray_1__Relation_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        StringBuilder result = Extend.buildCode(transformedParams, s -> "Lists.mutable.withAll(" + transformedParams.get(1) + "._funcSpecs())", false);
        return result.toString();
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "{\n" +
                "   @Override\n" +
                "   public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "   {\n" +
                "       return " + Extend.buildCode(Lists.mutable.of("(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Relation<?>)vars.get(0)", "vars.get(1)"), s -> "Lists.mutable.withAll(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpecArray)vars.get(1))._funcSpecs())", false) + ";\n" +
                "   }\n" +
                "\n}";
    }
}
