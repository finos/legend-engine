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
    public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
    {
        newExecutionBuilder()
                .withServiceRunnerInput(serviceRunnerInput)
                .executeToStream(outputStream);
    }
}