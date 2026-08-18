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

/**
 * Shared code generation for the groupBy/aggregate overloads whose new column is a single lambda applied to
 * the whole group, rather than an AggColSpec map/reduce pair.
 */
class GroupByFuncSupport
{
    private static final String IMPL = "org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation";
    private static final String FUNC_COL_SPEC = "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpec";
    private static final String FUNC_COL_SPEC_ARRAY = "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.FuncColSpecArray";
    private static final String FUNCTION_TYPE = "org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType";

    private GroupByFuncSupport()
    {
    }

    static String getString(ListIterable<String> transformedParams, boolean hasCols, boolean isArray)
    {
        int specIndex = hasCols ? 2 : 1;
        StringBuilder result = new StringBuilder(IMPL + ".groupByFunc(");
        result.append(transformedParams.get(0));
        result.append(", ");
        if (hasCols)
        {
            result.append(transformedParams.get(1));
            result.append(", ");
        }
        // The generated expression for the spec parameter arrives untyped, so cast before reaching for
        // _funcSpecs() or handing an element to the collect below.
        String spec = transformedParams.get(specIndex);
        buildCollect(result, isArray
                ? "Lists.mutable.withAll(((" + FUNC_COL_SPEC_ARRAY + "<? extends Object, ? extends Object>)(Object)" + spec + ")._funcSpecs())"
                : "Lists.mutable.with((" + FUNC_COL_SPEC + "<? extends Object, ? extends Object>)(Object)" + spec + ")");
        result.append(", es)");
        return result.toString();
    }

    private static void buildCollect(StringBuilder result, String param)
    {
        result.append(param);
        result.append(".collect(");
        result.append("new DefendedFunction<" + FUNC_COL_SPEC + "<? extends Object, ? extends Object>, " + IMPL + ".ColFuncSpecTransRel>()\n" +
                "{\n" +
                "    @Override\n" +
                "    public  " + IMPL + ".ColFuncSpecTransRel valueOf(" + FUNC_COL_SPEC + "<?, ?> c)\n" +
                "    {\n");
        result.append("return new " + IMPL + ".ColFuncSpecTransRel(");
        result.append("c._name(),");
        result.append("PureCompiledLambda.getPureFunction(c._function(),es),");
        result.append(" ((" + FUNCTION_TYPE + ")c._function()._classifierGenericType()._typeArguments().toList().get(0)._rawType())\n");
        result.append(");\n");
        result.append("    }\n" +
                "   }" +
                ")");
    }
}
