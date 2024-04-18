// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.postgres.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.operations.Merge;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SQLMergeVisitor extends org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SQLMergeVisitor
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Merge current, VisitorContext context)
    {
        List<Pair<FieldValue, Value>> keyValuePairsWithoutDatasetRef = new ArrayList<>();
        for (Pair<FieldValue, Value> pair : current.matchedKeyValuePairs())
        {
            FieldValue fieldValueWithoutDatasetRef = pair.key().withDatasetRef(Optional.empty());
            keyValuePairsWithoutDatasetRef.add(Pair.of(fieldValueWithoutDatasetRef, pair.value()));
        }
        return super.visit(prev, current.withMatchedKeyValuePairs(keyValuePairsWithoutDatasetRef), context);
    }
}
