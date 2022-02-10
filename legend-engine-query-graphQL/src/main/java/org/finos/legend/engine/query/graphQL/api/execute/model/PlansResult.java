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
