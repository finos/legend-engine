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

package org.finos.legend.pure.runtime.java.extension.external.relation.interpreted;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Columns;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Concatenate;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Distinct;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Drop;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Extend;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Filter;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.GroupBy;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.First;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Offset;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Last;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Join;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Limit;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Map;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Project;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Rename;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Select;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Size;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Slice;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.Sort;
import org.finos.legend.pure.runtime.java.extension.external.relation.interpreted.natives.shared.TDSWithCursorCoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.extension.InterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class RelationExtensionInterpreted extends BaseInterpretedExtension
{
    public RelationExtensionInterpreted()
    {
        super(Tuples.pair("concatenate_Relation_1__Relation_1__Relation_1_", Concatenate::new),
                Tuples.pair("drop_Relation_1__Integer_1__Relation_1_", Drop::new),
                Tuples.pair("extend_Relation_1__FuncColSpec_1__Relation_1_", Extend::new),
                Tuples.pair("extend_Relation_1__FuncColSpecArray_1__Relation_1_", Extend::new),
                Tuples.pair("extend_Relation_1__AggColSpec_1__Relation_1_", Extend::new),
                Tuples.pair("extend_Relation_1__AggColSpecArray_1__Relation_1_", Extend::new),
                Tuples.pair("extend_Relation_1___Window_1__AggColSpec_1__Relation_1_", Extend::new),
                Tuples.pair("extend_Relation_1___Window_1__AggColSpecArray_1__Relation_1_", Extend::new),
                Tuples.pair("extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_", Extend::new),
                Tuples.pair("extend_Relation_1___Window_1__FuncColSpecArray_1__Relation_1_", Extend::new),
                Tuples.pair("first_Relation_1__T_1_", First::new),
                Tuples.pair("last_Relation_1__T_1_", Last::new),
                Tuples.pair("offset_Relation_1__T_1__Integer_1__T_1_", Offset::new),
                Tuples.pair("filter_Relation_1__Function_1__Relation_1_", Filter::new),
                Tuples.pair("map_Relation_1__Function_1__V_MANY_", Map::new),
                Tuples.pair("size_Relation_1__Integer_1_", Size::new),
                Tuples.pair("limit_Relation_1__Integer_1__Relation_1_", Limit::new),
                Tuples.pair("rename_Relation_1__ColSpec_1__ColSpec_1__Relation_1_", Rename::new),
                Tuples.pair("slice_Relation_1__Integer_1__Integer_1__Relation_1_", Slice::new),
                Tuples.pair("sort_Relation_1__SortInfo_MANY__Relation_1_", Sort::new),
                Tuples.pair("join_Relation_1__Relation_1__JoinKind_1__Function_1__Relation_1_", Join::new),
                Tuples.pair("distinct_Relation_1__ColSpecArray_1__Relation_1_", Distinct::new),
                Tuples.pair("groupBy_Relation_1__ColSpecArray_1__AggColSpec_1__Relation_1_", GroupBy::new),
                Tuples.pair("groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_", GroupBy::new),
                Tuples.pair("groupBy_Relation_1__ColSpec_1__AggColSpecArray_1__Relation_1_", GroupBy::new),
                Tuples.pair("groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_", GroupBy::new),
                Tuples.pair("project_C_MANY__FuncColSpecArray_1__Relation_1_", Project::new),
                Tuples.pair("columns_Relation_1__Column_MANY_", Columns::new),
                Tuples.pair("select_Relation_1__ColSpec_1__Relation_1_", Select::new),
                Tuples.pair("select_Relation_1__ColSpecArray_1__Relation_1_", Select::new)
        );
    }

    public static InterpretedExtension extension()
    {
        return new RelationExtensionInterpreted();
    }

    @Override
    public CoreInstance getExtraFunctionExecution(Function<?> function, ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, FunctionExecutionInterpreted interpreted)
    {
        if (Instance.instanceOf(function, M3Paths.Column, processorSupport))
        {
            return ((TDSWithCursorCoreInstance) params.get(0).getValueForMetaPropertyToOne("values")).getValue(function._name());
        }
        return null;
    }
}
