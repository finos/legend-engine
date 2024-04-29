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

package org.finos.legend.engine.persistence.components.relational.transformer;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNodes;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.transformer.AbstractTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;

public class RelationalTransformer extends AbstractTransformer<SqlGen, SqlPlan>
{
    public RelationalTransformer(RelationalSink relationalSink)
    {
        super(relationalSink, TransformOptions.builder().build());
    }

    public RelationalTransformer(RelationalSink relationalSink, TransformOptions options)
    {
        super(relationalSink, options);
    }

    @Override
    protected SqlPlan createPhysicalPlan(PhysicalPlanNodes<SqlGen> nodes)
    {
        return SqlPlan.builder().addAllOps(nodes.nodes()).build();
    }

    @Override
    public SqlPlan generatePhysicalPlan(LogicalPlan plan)
    {
        return super.generatePhysicalPlan(plan);
    }
}
