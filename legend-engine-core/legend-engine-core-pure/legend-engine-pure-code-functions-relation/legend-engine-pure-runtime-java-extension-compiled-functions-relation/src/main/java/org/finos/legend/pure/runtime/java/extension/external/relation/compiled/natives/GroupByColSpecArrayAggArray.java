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
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

import static org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.GroupByColSpecAgg.processAggColSpec;

public class GroupByColSpecArrayAggArray extends AbstractNative implements Native
{
    public GroupByColSpecArrayAggArray()
    {
        super("groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return getString(transformedParams);
    }

    @Override
    public String buildBody()
    {
        return "new SharedPureFunction<Object>()\n" +
                "{\n" +
                "   @Override\n" +
                "   public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "   {\n" +
                "       return " + getString(Lists.mutable.with("(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Relation<? extends Object>)vars.get(0)", "(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.ColSpecArray<?>)vars.get(1)", "((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.AggColSpecArray<? extends Object, ? extends Object, ? extends Object>)vars.get(2))")) + ";" +
                "   }\n" +
                "\n}";
    }

    private static String getString(ListIterable<String> transformedParams)
    {
        StringBuilder result = new StringBuilder("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.groupBy");
        result.append('(');
        result.append(transformedParams.get(0));
        result.append(", ");
        result.append(transformedParams.get(1));
        result.append(", ");
        processAggColSpec(result, "Lists.mutable.withAll(" + transformedParams.get(2) + "._aggSpecs())", false);
        result.append(", es)");
        return result.toString();
    }
}