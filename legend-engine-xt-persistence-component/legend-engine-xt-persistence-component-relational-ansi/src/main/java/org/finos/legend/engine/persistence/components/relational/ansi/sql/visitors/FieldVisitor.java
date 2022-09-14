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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.AnsiDatatypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.ColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NotNullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.PKColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.UniqueColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class FieldVisitor implements LogicalPlanVisitor<Field>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Field current, VisitorContext context)
    {
        DataType dataType = new AnsiDatatypeMapping().getDataType(current.type());
        List<ColumnConstraint> columnConstraints = new ArrayList<>();
        if (!current.nullable())
        {
            columnConstraints.add(new NotNullColumnConstraint());
        }
        if (current.primaryKey())
        {
            columnConstraints.add(new PKColumnConstraint());
        }
        if (current.unique())
        {
            columnConstraints.add(new UniqueColumnConstraint());
        }
        // todo: check if we want to print "NULL" if all above three are false (copy paste to the other modules too)
        Column column = new Column(current.name(), dataType, columnConstraints, context.quoteIdentifier());
        for (Optimizer optimizer : context.optimizers())
        {
            column = (Column) optimizer.optimize(column);
        }

        prev.push(column);

        return new VisitorResult(null);
    }
}