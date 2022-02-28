package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;

import java.io.OutputStream;

public class SimpleRelationalServiceRunnerTDS extends AbstractServicePlanExecutor
{
    public SimpleRelationalServiceRunnerTDS()
    {
        this(new StoreExecutorConfiguration[0]);
    }

    public SimpleRelationalServiceRunnerTDS(StoreExecutorConfiguration... storeExecutorConfigurations)
    {
        super("service::RelationalService", "plans/org/finos/service/SimpleRelationalServiceRunnerTDS.json", storeExecutorConfigurations);
    }

    @Override
    public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream) {
        newExecutionBuilder()
                .withServiceRunnerInput(serviceRunnerInput)
                .executeToStream(outputStream);
    }
}