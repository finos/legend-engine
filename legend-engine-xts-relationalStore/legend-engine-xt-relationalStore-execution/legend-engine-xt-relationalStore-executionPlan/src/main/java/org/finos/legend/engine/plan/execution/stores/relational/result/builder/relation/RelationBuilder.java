// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.result.builder.relation;

import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.stores.relational.result.ExecutionNodeRelationalResultHelper;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.RelationType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.TablePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Column;

import java.util.List;

public class RelationBuilder extends Builder
{
    public String relationName;
    public RelationType relationType;
    public String schemaName;
    public String database;
    public List<Column> columns;

    public RelationBuilder(ExecutionNode node)
    {
        this.relationName = ExecutionNodeRelationalResultHelper.getRelationNameFromRelationResult(node);
        this.relationType = ExecutionNodeRelationalResultHelper.getRelationTypeFromRelationResult(node);
        this.schemaName = ExecutionNodeRelationalResultHelper.getSchemaNameFromRelationResult(node);
        this.database = ExecutionNodeRelationalResultHelper.getDatabaseFromRelationResult(node);
        this.columns = ExecutionNodeRelationalResultHelper.getColumnInfoFromRelationResult(node);
    }

    public TablePtr getTablePointer()
    {
        TablePtr tablePtr = new TablePtr();
        tablePtr.table = this.relationName;
        tablePtr.schema = this.schemaName;
        tablePtr.database = this.database;
        return tablePtr;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof RelationBuilder)
        {
            RelationBuilder other = (RelationBuilder) obj;
            return this.relationName.equals(other.relationName) && this.relationType == other.relationType &&
                    this.schemaName.equals(other.schemaName) && this.database.equals(other.database);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + relationName.hashCode();
        result = prime * result + relationType.hashCode();
        result = schemaName == null ? result : prime * result + schemaName.hashCode();
        result = database == null ? result : prime * result + database.hashCode();
        return result;
    }
}

