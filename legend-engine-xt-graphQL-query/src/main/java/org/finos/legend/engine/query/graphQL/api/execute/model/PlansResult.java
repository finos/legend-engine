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

package org.finos.legend.engine.query.graphQL.api.execute.model;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PlansResult
{
    public List<PlanUnit> executionPlansByProperty = Collections.emptyList();

    public PlansResult(Collection<PlanUnit> plans)
    {
        this.executionPlansByProperty = Lists.mutable.withAll(plans);
    }

    public static class PlanUnit
    {
        public String property;
        public ExecutionPlan executionPlan;
        public String executionPlanAsText;

        public PlanUnit(String first, ExecutionPlan executionPlan, String executionPlanAsText)
        {
            this.property = first;
            this.executionPlan = executionPlan;
            this.executionPlanAsText = executionPlanAsText;
        }
    }
}
