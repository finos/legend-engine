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

package org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor;

import org.finos.legend.engine.persistence.components.common.LoadOptions;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Copy;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.statements.CopyStatement;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CopyVisitor implements LogicalPlanVisitor<Copy>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Copy current, VisitorContext context)
    {
        Map<String, Object> loadOptionsMap = new HashMap<>();
        current.loadOptions().ifPresent(options -> retrieveLoadOptions(options, loadOptionsMap));
        CopyStatement copyStatement = new CopyStatement(loadOptionsMap);
        prev.push(copyStatement);

        List<LogicalPlanNode> logicalPlanNodes = new ArrayList<>();
        logicalPlanNodes.add(current.sourceDataset());
        logicalPlanNodes.add(current.targetDataset());
        logicalPlanNodes.addAll(current.fields());

        return new VisitorResult(copyStatement, logicalPlanNodes);
    }

    private void retrieveLoadOptions(LoadOptions loadOptions, Map<String, Object> loadOptionsMap)
    {
        loadOptions.onError().ifPresent(property -> loadOptionsMap.put("ON_ERROR", property));
        loadOptions.force().ifPresent(property -> loadOptionsMap.put("FORCE", property));
    }
}
