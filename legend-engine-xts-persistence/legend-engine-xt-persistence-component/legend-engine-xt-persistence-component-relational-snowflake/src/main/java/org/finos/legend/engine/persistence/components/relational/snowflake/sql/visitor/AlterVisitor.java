// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetAdditionalProperties;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.TableOrigin;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.statements.AlterTable;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.tabletypes.IcebergTableType;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.AlterOperation;
import org.finos.legend.engine.persistence.components.relational.sqldom.tabletypes.TableType;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AlterVisitor implements LogicalPlanVisitor<Alter>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Alter current, VisitorContext context)
    {
        List<TableType> types = new ArrayList<>();
        Optional<DatasetAdditionalProperties> datasetAdditionalProperties = current.dataset().datasetAdditionalProperties();
        if (datasetAdditionalProperties.isPresent())
        {
            if (datasetAdditionalProperties.get().tableOrigin().isPresent())
            {
                if (datasetAdditionalProperties.get().tableOrigin().get().equals(TableOrigin.ICEBERG))
                {
                    types.add(new IcebergTableType());
                }
            }
        }
        AlterTable alterTable = new AlterTable(AlterOperation.valueOf(current.operation().name()), types);
        for (Optimizer optimizer : context.optimizers())
        {
            alterTable = (AlterTable) optimizer.optimize(alterTable);
        }

        prev.push(alterTable);

        return new VisitorResult(alterTable, Arrays.asList(current.dataset(), current.columnDetails()));
    }
}
