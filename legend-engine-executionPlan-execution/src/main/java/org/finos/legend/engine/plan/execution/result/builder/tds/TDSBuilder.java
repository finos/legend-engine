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

package org.finos.legend.engine.plan.execution.result.builder.tds;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.stream.Collectors;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeTDSResultHelper;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSResultType;
import org.finos.legend.engine.shared.core.operational.Assert;

@JsonPropertyOrder(value = {"_type", "columns"})
public class TDSBuilder extends Builder
{
    public final List<TDSColumn> columns;

    public TDSBuilder(@JsonProperty("columns") List<TDSColumn> columns)
    {
        super("tdsBuilder");
        this.columns = columns;
    }

    public TDSBuilder(ExecutionNode node)
    {
        super("tdsBuilder");
        Assert.assertTrue(ExecutionNodeTDSResultHelper.isResultTDS(node),
                () -> "Cannot construct TDSBuilder from " + node.resultType.getClass().getSimpleName());
        this.columns = ((TDSResultType) node.resultType).tdsColumns.stream()
                .map(TDSColumn::copyWithoutEnumMapping)
                .collect(Collectors.toList());
    }

    public TDSBuilder(ExecutionNode node, List<String> columnLabels, boolean isColumnLabelCaseSensitive)
    {
        super("tdsBuilder");
        Assert.assertTrue(ExecutionNodeTDSResultHelper.isResultTDS(node),
                () -> "Cannot construct TDSBuilder from " + node.resultType.getClass().getSimpleName());
        this.columns = columnLabels.stream().map(label ->
        {
            TDSColumn c = ExecutionNodeTDSResultHelper.getTDSColumn(node, label, isColumnLabelCaseSensitive);
            return c.copyWithoutEnumMapping();
        }).collect(Collectors.toList());
    }
}
