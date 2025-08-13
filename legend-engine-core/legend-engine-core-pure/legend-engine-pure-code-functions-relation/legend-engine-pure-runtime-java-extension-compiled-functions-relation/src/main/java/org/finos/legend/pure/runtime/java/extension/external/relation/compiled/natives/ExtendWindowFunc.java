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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

import static org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.Extend.buildCollectFuncSpec;
import static org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.GroupBy.processAggColSpec;

public class ExtendWindowFunc extends AbstractNative implements Native
{
    public ExtendWindowFunc()
    {
        super("extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        StringBuilder result = buildCode(transformedParams, s -> "Lists.mutable.with(" + transformedParams.get(2) + ")");
        return result.toString();
    }

    static StringBuilder buildCode(ListIterable<String> transformedParams, Function<String, String> collection)
    {
        StringBuilder result = new StringBuilder("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.extendWinFunc(");
        result.append(transformedParams.get(0) + ", ");
        processWindow(result, transformedParams.get(1));
        result.append(",");
        result.append(collection.valueOf(transformedParams.get(2)));
        buildCollectFuncSpec(result, true);
        result.append(", es)");
        return result;
    }

    public static void processWindow(StringBuilder result, String param)
    {
        result.append("Lists.mutable.with(" + param + ").collect(new DefendedFunction<Root_meta_pure_functions_relation__Window<? extends Object>, org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Window>()\n" +
                "{\n" +
                "    @Override\n" +
                "    public org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Window valueOf(Root_meta_pure_functions_relation__Window<?> w)\n" +
                "    {\n" +
                "  return ");
        result.append("new org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Window(" +
                "w._partition().toList(),");
        Sort.processSortInfo(result, "w._sortInfo()");
        result.append(",");
        buildFrame(result, "w._frame()");
        result.append(");");
        result.append(
                "    }\n" +
                        "}).getFirst()");
    }

    static void buildFrame(StringBuilder result, String s)
    {
        result.append("CompiledSupport.toPureCollection(" + s + ").collect(" +
                "new DefendedFunction<Root_meta_pure_functions_relation_Frame, org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Frame>()\n" +
                "{\n" +
                "    @Override\n" +
                "    public org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Frame valueOf(Root_meta_pure_functions_relation_Frame x)\n" +
                "    {\n" +
                "        return new org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.Frame(\n" +
                "                x instanceof Root_meta_pure_functions_relation_Rows ? org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.FrameType.rows : org.finos.legend.pure.runtime.java.extension.external.relation.shared.window.FrameType.range,\n" +
                "                x._offsetFrom() instanceof Root_meta_pure_functions_relation_UnboundedFrameValue,\n" +
                "                x._offsetFrom() instanceof Root_meta_pure_functions_relation_FrameIntValue ? (int)((Root_meta_pure_functions_relation_FrameIntValue) x._offsetFrom())._value() : (x._offsetFrom() instanceof Root_meta_pure_functions_relation_FrameNumericValue ? ((Root_meta_pure_functions_relation_FrameNumericValue) x._offsetFrom())._value() : -1),\n" +
                "                x._offsetTo() instanceof Root_meta_pure_functions_relation_UnboundedFrameValue,\n" +
                "                x._offsetTo() instanceof Root_meta_pure_functions_relation_FrameIntValue ? (int)((Root_meta_pure_functions_relation_FrameIntValue) x._offsetTo())._value() : (x._offsetTo() instanceof Root_meta_pure_functions_relation_FrameNumericValue ? ((Root_meta_pure_functions_relation_FrameNumericValue) x._offsetTo())._value() : -1)\n" +
                "        );\n" +
                "    } \n" +
                "}" +
                ").getFirst()");
    }
}

