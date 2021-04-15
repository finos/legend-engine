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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.RelationResultType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.RelationType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Column;

public class ExecutionNodeRelationalResultHelper
{
    @JsonIgnore
    public static boolean isRelationResult(ExecutionNode executionNode)
    {
        return executionNode.resultType instanceof RelationResultType;
    }

    @JsonIgnore
    public static String getRelationNameFromRelationResult(ExecutionNode executionNode)
    {
        RelationResultType relationResultType = (RelationResultType) executionNode.resultType;
        return relationResultType.relationName;
    }

    @JsonIgnore
    public static RelationType getRelationTypeFromRelationResult(ExecutionNode executionNode)
    {
        RelationResultType relationResultType = (RelationResultType) executionNode.resultType;
        return relationResultType.relationType;
    }

    @JsonIgnore
    public static String getSchemaNameFromRelationResult(ExecutionNode executionNode)
    {
        RelationResultType relationResultType = (RelationResultType) executionNode.resultType;
        return relationResultType.schemaName;
    }

    @JsonIgnore
    public static String getDatabaseFromRelationResult(ExecutionNode executionNode)
    {
        RelationResultType relationResultType = (RelationResultType) executionNode.resultType;
        return relationResultType.database;
    }

    @JsonIgnore
    public static MutableList<Column> getColumnInfoFromRelationResult(ExecutionNode executionNode)
    {
        RelationResultType relationResultType = (RelationResultType) executionNode.resultType;
        return Lists.mutable.withAll(relationResultType.columns);
    }
}
