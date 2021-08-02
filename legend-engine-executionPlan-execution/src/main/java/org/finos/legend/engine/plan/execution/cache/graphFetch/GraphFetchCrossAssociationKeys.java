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

package org.finos.legend.engine.plan.execution.cache.graphFetch;

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;

import java.util.List;
import java.util.Objects;

/* Work in progress, do not use */

public class GraphFetchCrossAssociationKeys
{
    private final SingleExecutionPlan plan;
    private final String planIdentifier;
    private final String propertyPath;
    private final String sourceMappingId;
    private final String sourceSetId;
    private final String targetMappingId;
    private final String targetSetId;
    private final List<String> targetPropertiesOrdered;
    private final String subTree;

    public GraphFetchCrossAssociationKeys(SingleExecutionPlan plan, String planIdentifier, String propertyPath, String sourceMappingId, String sourceSetId, String targetMappingId, String targetSetId, List<String> targetPropertiesOrdered, String subTree)
    {
        this.plan = Objects.requireNonNull(plan, "plan must not be null");
        this.planIdentifier = Objects.requireNonNull(planIdentifier, "planIdentifier must not be null");
        this.propertyPath = Objects.requireNonNull(propertyPath, "propertyPath must not be null");
        this.sourceMappingId = Objects.requireNonNull(sourceMappingId, "sourceMappingId must not be null");
        this.sourceSetId = Objects.requireNonNull(sourceSetId, "sourceSetId must not be null");
        this.targetMappingId = Objects.requireNonNull(targetMappingId, "targetMappingId must not be null");
        this.targetSetId = Objects.requireNonNull(targetSetId, "targetSetId must not be null");
        this.targetPropertiesOrdered = Objects.requireNonNull(targetPropertiesOrdered, "targetPropertiesOrdered must not be null");
        this.subTree = Objects.requireNonNull(subTree, "subTree must not be null");
    }

    public boolean isCompatible(GraphFetchCrossAssociationKeys other)
    {
        return this.plan.equals(other.plan) &&
                this.targetMappingId.equals(other.targetMappingId) &&
                this.targetSetId.equals(other.targetSetId) &&
                this.targetPropertiesOrdered.equals(other.targetPropertiesOrdered) &&
                this.subTree.equals(other.subTree);
    }

    public String getName()
    {
        return "<" + this.planIdentifier + " " + this.propertyPath + '>';
    }

    public SingleExecutionPlan getPlan()
    {
        return this.plan;
    }

    public String getPlanIdentifier()
    {
        return this.planIdentifier;
    }

    public String getPropertyPath()
    {
        return this.propertyPath;
    }

    public String getSourceMappingId()
    {
        return this.sourceMappingId;
    }

    public String getSourceSetId()
    {
        return this.sourceSetId;
    }

    public String getTargetMappingId()
    {
        return this.targetMappingId;
    }

    public String getTargetSetId()
    {
        return this.targetSetId;
    }

    public List<String> getTargetPropertiesOrdered()
    {
        return this.targetPropertiesOrdered;
    }

    public String getSubTree()
    {
        return this.subTree;
    }
}
