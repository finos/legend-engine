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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSResultType;

import java.util.Objects;
import java.util.stream.Collectors;

public class ExecutionNodeTDSResultHelper
{
    @JsonIgnore
    public static boolean isResultTDS(ExecutionNode executionNode)
    {
        return executionNode.resultType instanceof TDSResultType;
    }

    @JsonIgnore
    public static TDSColumn getTDSColumn(ExecutionNode executionNode, String name, boolean isColumnNameCaseSensitive)
    {
        TDSResultType resultType = (TDSResultType) executionNode.resultType;
        return Objects.requireNonNull(
                ListIterate.select(resultType.tdsColumns, n -> isColumnNameCaseSensitive ? n.name.equals(name) : n.name.equalsIgnoreCase(name)).getAny(), 
                () -> "Column '" + name + "' (case sensitive? " + isColumnNameCaseSensitive + ") not found among: " + resultType.tdsColumns.stream().map(x -> x.name).collect(Collectors.joining(", ", "[", "]"))
        );
    }

    @JsonIgnore
    public static boolean isTDSColumnEnum(ExecutionNode executionNode, String name, boolean isColumnNameCaseSensitive)
    {
        TDSColumn col = getTDSColumn(executionNode, name, isColumnNameCaseSensitive);
        return !col.enumMapping.isEmpty();
    }

    @JsonIgnore
    public static Function<Object, Object> getTDSEnumTransformer(ExecutionNode executionNode, String name, boolean isColumnNameCaseSensitive)
    {
        return ExecutionNodeResultHelper.buildReverseEnumFunc(executionNode, getTDSColumn(executionNode, name, isColumnNameCaseSensitive).enumMapping);
    }
}
