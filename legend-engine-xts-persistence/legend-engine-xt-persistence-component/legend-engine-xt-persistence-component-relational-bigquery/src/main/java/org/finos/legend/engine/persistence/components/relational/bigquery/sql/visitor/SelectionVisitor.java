// Copyright 2025 Goldman Sachs
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

import java.util.List;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;

public class SelectionVisitor extends org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SelectionVisitor
{
    /*
    SELECTION with WHERE always needs A FROM CLAUSE IN BigQuery
    If the source is not provided, default source is SELECT 1 as X
    */
    @Override
    public void visitSource(Selection current, List<LogicalPlanNode> logicalPlanNodeList, List<Condition> whereConditions)
    {
        if (current.source().isPresent() || whereConditions.isEmpty())
        {
            super.visitSource(current, logicalPlanNodeList, whereConditions);
        }
        else
        {
            Dataset dataset = Selection.builder().addFields(NumericalValue.of(1L)).alias("X").build();
            logicalPlanNodeList.add(dataset);
        }
    }
}
