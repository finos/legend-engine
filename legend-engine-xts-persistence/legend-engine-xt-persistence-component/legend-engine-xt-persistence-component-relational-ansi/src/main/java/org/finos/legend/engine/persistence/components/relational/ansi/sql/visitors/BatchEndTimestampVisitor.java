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

import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.FunctionName;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Function;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.Optional;

public class BatchEndTimestampVisitor implements LogicalPlanVisitor<BatchEndTimestamp>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, BatchEndTimestamp current, VisitorContext context)
    {

        Optional<String> batchEndTimestampPattern = context.batchEndTimestampPattern();
        if (batchEndTimestampPattern.isPresent())
        {
            prev.push(new org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue(batchEndTimestampPattern.get(), context.quoteIdentifier()));
        }
        else
        {
            prev.push(new Function(FunctionName.CURRENT_TIMESTAMP, null, context.quoteIdentifier()));
        }
        return new VisitorResult();
    }
}
