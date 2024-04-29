//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;

import java.io.OutputStream;

public class SimpleRelationalServiceRunner extends AbstractServicePlanExecutor
{
    public SimpleRelationalServiceRunner()
    {
        super("test::Service",
                TestServiceRunner.buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleRelationalService.pure", "test::fetch_String_MANY__String_1_"), false);
    }

    public SimpleRelationalServiceRunner(StoreExecutorConfiguration... storeExecutorConfigurations)
    {
        super("test::Service",
                TestServiceRunner.buildPlanForFetchFunction("/org/finos/legend/engine/pure/dsl/service/execution/test/simpleRelationalService.pure", "test::fetch_String_MANY__String_1_"),
                PlanExecutor.newPlanExecutorWithConfigurations(storeExecutorConfigurations));
    }

    @Override
    public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
    {
        newExecutionBuilder()
                .withParameter("ip", serviceRunnerInput.getArgs().get(0))
                .withServiceRunnerInput(serviceRunnerInput)
                .executeToStream(outputStream);
    }
}
