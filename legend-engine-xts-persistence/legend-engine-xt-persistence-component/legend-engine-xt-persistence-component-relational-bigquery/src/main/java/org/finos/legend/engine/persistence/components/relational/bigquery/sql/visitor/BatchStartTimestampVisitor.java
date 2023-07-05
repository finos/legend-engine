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

import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.FunctionVisitor;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.Optional;

public class BatchStartTimestampVisitor implements LogicalPlanVisitor<BatchStartTimestamp>
{

    private static final String DATE_TIME_FORMAT = "%Y-%m-%d %H:%M:%S";

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, BatchStartTimestamp current, VisitorContext context)
    {
        Optional<String> batchStartTimestampPattern = context.batchStartTimestampPattern();
        StringValue dateTimeFormat = StringValue.of(DATE_TIME_FORMAT);
        StringValue datetimeValue;
        datetimeValue = StringValue.of(batchStartTimestampPattern.orElse(context.batchStartTimestamp()));
        FunctionImpl parseDateTime = FunctionImpl.builder().functionName(FunctionName.PARSE_DATETIME).addValue(dateTimeFormat, datetimeValue).build();
        return new FunctionVisitor().visit(prev, parseDateTime, context);
    }
}
