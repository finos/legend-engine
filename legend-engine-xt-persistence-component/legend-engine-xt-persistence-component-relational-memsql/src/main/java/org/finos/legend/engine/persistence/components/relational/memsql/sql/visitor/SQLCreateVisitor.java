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

package org.finos.legend.engine.persistence.components.relational.memsql.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.modifiers.IfNotExistsTableModifier;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.memsql.sqldom.tabletypes.ReferenceTableType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.CreateTable;
import org.finos.legend.engine.persistence.components.relational.sqldom.tabletypes.TableType;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SQLCreateVisitor implements LogicalPlanVisitor<Create>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Create current, VisitorContext context)
    {
        CreateTable createTable = new CreateTable();
        if (!current.dataset().schema().shardSpecification().isPresent() || current.dataset().schema().shardSpecification().get().shardKeys().size() == 0)
        {
            List<TableType> tableTypeList = Arrays.asList(new ReferenceTableType());
            createTable = new CreateTable(tableTypeList);
        }
        prev.push(createTable);

        List<LogicalPlanNode> logicalPlanNodes = new ArrayList<>();
        logicalPlanNodes.add(current.dataset());
        logicalPlanNodes.add(current.dataset().schema());

        if (current.ifNotExists())
        {
            logicalPlanNodes.add(IfNotExistsTableModifier.INSTANCE);
        }

        return new VisitorResult(createTable, logicalPlanNodes);
    }
}
