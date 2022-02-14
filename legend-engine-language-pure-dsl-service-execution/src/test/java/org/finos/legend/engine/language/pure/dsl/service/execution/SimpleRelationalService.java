package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.finos.legend.engine.language.pure.dsl.service.execution.AbstractServicePlanExecutor;
import org.finos.legend.engine.language.pure.dsl.service.execution.ServiceRunnerInput;
import org.finos.legend.engine.plan.execution.PlanExecutor;

import java.io.OutputStream;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.shared.core.url.StreamProvider;

import java.io.OutputStream;
import java.util.List;

public class SimpleRelationalService extends AbstractServicePlanExecutor
{
    public SimpleRelationalService()
    {
        super("service::RelationalService", "plans/org/finos/service/SimpleRelationalService.json", false);
    }

    public SimpleRelationalService(StoreExecutorConfiguration... storeExecutorConfigurations)
    {
        super("service::RelationalService", "plans/org/finos/service/SimpleRelationalService.json", storeExecutorConfigurations);
    }

    public Result execute()
    {
        return execute(null);
    }

    public Result execute(StreamProvider streamProvider)
    {
        return newNoParameterExecutionBuilder().withStreamProvider(streamProvider).execute();
    }

    @Override
    public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
    {
        List<Object> args = serviceRunnerInput.getArgs();
        if (args.size() != 0)
        {
            throw new IllegalArgumentException("Unexpected number of parameters. Expected parameter size: 0, Passed parameter size: " + args.size());
        }

        newExecutionBuilder(0)
                .withServiceRunnerInput(serviceRunnerInput)
                .executeToStream(outputStream);
    }
}
