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

package org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors;

import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.ParseJsonFunction;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

public class ParseJsonFunctionVisitor implements LogicalPlanVisitor<ParseJsonFunction>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, ParseJsonFunction current, VisitorContext context)
    {
        FunctionImpl function = FunctionImpl.builder()
                .functionName(org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName.PARSE_JSON)
                .addValue(current.jsonString())
                .build();
        return new FunctionVisitor().visit(prev, function, context);
    }
}
