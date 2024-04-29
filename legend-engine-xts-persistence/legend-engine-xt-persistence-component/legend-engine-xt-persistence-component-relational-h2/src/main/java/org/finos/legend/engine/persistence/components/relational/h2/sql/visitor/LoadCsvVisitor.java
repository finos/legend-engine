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

package org.finos.legend.engine.persistence.components.relational.h2.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.LoadCsv;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.InsertStatement;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoadCsvVisitor implements LogicalPlanVisitor<LoadCsv>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, LoadCsv current, VisitorContext context)
    {
        InsertStatement insertStatement = new InsertStatement();
        prev.push(insertStatement);
        List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>();

        logicalPlanNodeList.add(current.targetDataset());
        logicalPlanNodeList.add(current.targetDataset().schemaReference());

        List<Value> fieldsToSelect = current.targetDataset().schemaReference().fieldValues()
            .stream()
            .map(fieldValue -> FieldValue.builder().fieldName(fieldValue.fieldName()).build())
            .collect(Collectors.toList());

        Dataset selectStage = Selection.builder().source(current.csvExternalDatasetReference()).addAllFields(fieldsToSelect).build();
        logicalPlanNodeList.add(selectStage);

        return new VisitorResult(insertStatement, logicalPlanNodeList);
    }
}
