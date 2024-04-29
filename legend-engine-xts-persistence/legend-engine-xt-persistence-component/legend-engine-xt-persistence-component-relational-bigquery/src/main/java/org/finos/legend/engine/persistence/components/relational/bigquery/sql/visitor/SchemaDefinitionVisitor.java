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

package org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.bigquery.sql.BigQueryDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.constraints.columns.PKColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.ColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NotNullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.PrimaryKeyTableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.TableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SchemaDefinitionVisitor implements LogicalPlanVisitor<SchemaDefinition>
{
    /*
    IN BigQuery World
    Project => Database
    Dataset => Schema
    Table Name => Table
     */

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, SchemaDefinition current, VisitorContext context)
    {

        List<Field> pkFields = current.fields().stream().filter(Field::primaryKey).collect(Collectors.toList());
        int pkNum = pkFields.size();

        for (Field f : current.fields())
        {
            DataType dataType = new BigQueryDataTypeMapping().getDataType(f.type());
            List<ColumnConstraint> columnConstraints = new ArrayList<>();
            if (!f.nullable())
            {
                columnConstraints.add(new NotNullColumnConstraint());
            }
            if (f.primaryKey() && pkNum <= 1)
            {
                columnConstraints.add(new PKColumnConstraint());
            }
            Column column = new Column(f.name(), dataType, columnConstraints, context.quoteIdentifier());
            for (Optimizer optimizer : context.optimizers())
            {
                column = (Column) optimizer.optimize(column);
            }
            prev.push(column);
        }

        if (pkNum > 1)
        {
            TableConstraint constraint = new PrimaryKeyTableConstraint(pkFields.stream().map(Field::name).collect(Collectors.toList()), context.quoteIdentifier(), true);
            for (Optimizer optimizer : context.optimizers())
            {
                constraint = (TableConstraint) optimizer.optimize(constraint);
            }
            prev.push(constraint);
        }

        return new VisitorResult(null);
    }
}