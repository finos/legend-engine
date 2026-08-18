// Copyright 2026 Goldman Sachs
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

public class GroupByColSpecFuncArray extends AbstractNative implements Native
{
    public GroupByColSpecFuncArray()
    {
        super("groupBy_Relation_1__ColSpec_1__FuncColSpecArray_1__Relation_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return GroupByFuncSupport.getString(transformedParams, true, true);
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "{\n" +
                "   @Override\n" +
                "   public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "   {\n" +
                "       return " + GroupByFuncSupport.getString(Lists.mutable.with("(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Relation<? extends Object>)vars.get(0)", "(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.ColSpec<?>)vars.get(1)", "(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpecArray<? extends Object, ? extends Object>)vars.get(2)"), true, true) + ";" +
                "   }\n" +
                "\n}";
    }
}
