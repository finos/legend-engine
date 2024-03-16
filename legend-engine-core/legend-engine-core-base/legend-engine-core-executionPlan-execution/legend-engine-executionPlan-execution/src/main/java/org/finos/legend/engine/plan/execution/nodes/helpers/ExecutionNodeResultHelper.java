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

package org.finos.legend.engine.plan.execution.nodes.helpers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;

import java.util.List;
import java.util.Map;

public class ExecutionNodeResultHelper
{
    @JsonIgnore
    public static boolean isResultSizeRangeSet(ExecutionNode executionNode)
    {
        return executionNode.resultSizeRange != null;
    }

    @JsonIgnore
    public static boolean isSingleRecordResult(ExecutionNode executionNode)
    {
        return executionNode.resultSizeRange.isUpperBoundEqualTo(1);
    }


    public static Function<Object, Object> buildReverseEnumFunc(ExecutionNode executionNode, Map<String, List<String>> enumMapping)
    {
        MutableMap<String, String> reverseEnumMap = UnifiedMap.newMapWith(Lists.mutable.withAll(enumMapping.entrySet()).flatCollect(s -> ListIterate.collect(s.getValue(), z -> Tuples.pair(z, s.getKey()))));
        return s -> reverseEnumMap.get(s == null ? "" : s.toString());
    }
}
