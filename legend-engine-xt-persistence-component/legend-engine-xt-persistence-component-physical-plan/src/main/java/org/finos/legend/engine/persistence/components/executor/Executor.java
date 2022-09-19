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

package org.finos.legend.engine.persistence.components.executor;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlan;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;

import java.util.List;
import java.util.Map;

public interface Executor<C extends PhysicalPlanNode, R extends ResultData, P extends PhysicalPlan<C>>
{
    void executePhysicalPlan(P physicalPlan);

    void executePhysicalPlan(P physicalPlan, Map<String, String> placeholderKeyValues);

    List<R> executePhysicalPlanAndGetResults(P physicalPlan);

    List<R> executePhysicalPlanAndGetResults(P physicalPlan, Map<String, String> placeholderKeyValues);

    boolean datasetExists(Dataset dataset);

    void validateMainDatasetSchema(Dataset dataset);

    void begin();

    void commit();

    void revert();

    void close();
}
