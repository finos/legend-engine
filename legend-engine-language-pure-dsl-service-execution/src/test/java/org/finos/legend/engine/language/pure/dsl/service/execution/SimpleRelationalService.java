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
