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

import org.finos.legend.engine.persistence.components.logicalplan.values.DatetimeValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.FunctionVisitor;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

public class DatetimeValueVisitor implements LogicalPlanVisitor<DatetimeValue>
{

    private static final String DATE_TIME_FORMAT = "%Y-%m-%d %H:%M:%E6S";

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, DatetimeValue current, VisitorContext context)
    {
        StringValue dateTimeFormat = StringValue.of(DATE_TIME_FORMAT);
        FunctionImpl parseDateTime = FunctionImpl.builder().functionName(FunctionName.PARSE_DATETIME).addValue(dateTimeFormat, StringValue.of(current.value())).build();
        return new FunctionVisitor().visit(prev, parseDateTime, context);
    }
}
