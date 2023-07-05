// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.plugin;

import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;

public class Relational
{
    public static StoreExecutor build(RelationalExecutionConfiguration relationalExecutionConfiguration)
    {
        return new RelationalStoreExecutorBuilder().build(relationalExecutionConfiguration);
    }

    public static StoreExecutor build(int port)
    {
        RelationalExecutionConfiguration relationalExecutionConfiguration = RelationalExecutionConfiguration.newInstance()
                .withTemporaryTestDbConfiguration(new TemporaryTestDbConfiguration(port))
                .build();

        return new RelationalStoreExecutorBuilder().build(relationalExecutionConfiguration);
    }

    public static StoreExecutor build()
    {
        return new RelationalStoreExecutorBuilder().build();
    }
}
