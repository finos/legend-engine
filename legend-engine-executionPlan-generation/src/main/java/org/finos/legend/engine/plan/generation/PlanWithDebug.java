package org.finos.legend.engine.plan.generation;

import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;

public class PlanWithDebug
{
    public SingleExecutionPlan plan;
    public String[] debug;

    public PlanWithDebug(SingleExecutionPlan plan, String debug)
    {
        this.plan = plan;
        this.debug = debug.split("\\n");
    }
}
