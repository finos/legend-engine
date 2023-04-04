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

package org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.Optional;

public class FieldValueVisitor implements LogicalPlanVisitor<FieldValue>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, FieldValue current, VisitorContext context)
    {
        Field field = new Field(
            getDatasetReferenceAlias(current.datasetRef(), current.datasetRefAlias()),
            current.fieldName(),
            context.quoteIdentifier(),
            current.alias().orElse(null));

        for (Optimizer optimizer : context.optimizers())
        {
            field = (Field) optimizer.optimize(field);
        }

        prev.push(field);

        return new VisitorResult(null);
    }

    public static String getDatasetReferenceAlias(Optional<DatasetReference> datasetRef, Optional<String> datasetRefAlias)
    {
        String datasetReferenceAlias = null;
        if (datasetRef.isPresent())
        {
            datasetReferenceAlias = datasetRef.get().alias().orElse(null);
        }
        else if (datasetRefAlias.isPresent())
        {
            datasetReferenceAlias = datasetRefAlias.get();
        }
        return datasetReferenceAlias;
    }
}
