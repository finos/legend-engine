package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.finos.legend.engine.language.pure.dsl.service.execution.AbstractServicePlanExecutor;
import org.finos.legend.engine.language.pure.dsl.service.execution.ServiceRunnerInput;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;

import java.io.OutputStream;

public class SimpleRelationalServiceRunner extends AbstractServicePlanExecutor
{
    public SimpleRelationalServiceRunner()
    {
        super("test::Service",
                TestServiceRunner.buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleRelationalService.pure", "test::fetch"), false);
    }

    public SimpleRelationalServiceRunner(StoreExecutorConfiguration ...storeExecutorConfigurations)
    {
        super("test::Service",
                TestServiceRunner.buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleRelationalService.pure", "test::fetch"),
                PlanExecutor.newPlanExecutorWithConfigurations(storeExecutorConfigurations));
    }

    @Override
    public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream) {
        newExecutionBuilder()
                .withParameter("ip", serviceRunnerInput.getArgs().get(0))
                .withServiceRunnerInput(serviceRunnerInput)
                .executeToStream(outputStream);
    }
}
