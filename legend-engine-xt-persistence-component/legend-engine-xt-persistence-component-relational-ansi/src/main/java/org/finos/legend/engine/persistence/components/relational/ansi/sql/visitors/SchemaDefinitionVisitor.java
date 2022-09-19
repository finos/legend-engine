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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Index;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.AnsiDatatypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.ColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NotNullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.PKColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.UniqueColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.PrimaryKeyTableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.TableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.TableIndexConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.UniqueTableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SchemaDefinitionVisitor implements LogicalPlanVisitor<SchemaDefinition>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, SchemaDefinition current, VisitorContext context)
    {
        List<Field> pkFields = current.fields().stream().filter(Field::primaryKey).collect(Collectors.toList());
        int pkNum = pkFields.size();

        for (Field f : current.fields())
        {
            DataType dataType = new AnsiDatatypeMapping().getDataType(f.type());
            List<ColumnConstraint> columnConstraints = new ArrayList<>();
            if (!f.nullable())
            {
                columnConstraints.add(new NotNullColumnConstraint());
            }
            if (f.primaryKey() && pkNum <= 1)
            {
                columnConstraints.add(new PKColumnConstraint());
            }
            if (f.unique())
            {
                columnConstraints.add(new UniqueColumnConstraint());
            }
            // todo: check if we want to print "NULL" if all above three are false (copy paste to the other modules too)
            Column column = new Column(f.name(), dataType, columnConstraints, context.quoteIdentifier());
            for (Optimizer optimizer : context.optimizers())
            {
                column = (Column) optimizer.optimize(column);
            }
            prev.push(column);
        }

        if (pkNum > 1)
        {
            TableConstraint constraint = new PrimaryKeyTableConstraint(pkFields.stream()
                .map(Field::name)
                .collect(Collectors.toList()), context.quoteIdentifier());
            for (Optimizer optimizer : context.optimizers())
            {
                constraint = (TableConstraint) optimizer.optimize(constraint);
            }
            prev.push(constraint);
        }

        for (Index idx : current.indexes())
        {
            TableConstraint constraint;
            List<String> columns = idx.columns().stream()
                .map(Field::name)
                .collect(Collectors.toList());
            if (idx.unique())
            {
                constraint = new UniqueTableConstraint(columns, context.quoteIdentifier());
            }
            else
            {
                constraint = new TableIndexConstraint(columns, idx.indexName(), context.quoteIdentifier());
            }
            for (Optimizer optimizer : context.optimizers())
            {
                constraint = (TableConstraint) optimizer.optimize(constraint);
            }
            prev.push(constraint);
        }

        return new VisitorResult(null);
    }
}