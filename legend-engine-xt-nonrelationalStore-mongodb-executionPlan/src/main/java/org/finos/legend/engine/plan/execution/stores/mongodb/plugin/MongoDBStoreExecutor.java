// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.mongodb.plugin;

import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreState;

public class MongoDBStoreExecutor  implements StoreExecutor
{
    private final MongoDBStoreState state;

    private final MongoDBStoreExecutorConfiguration storeExecutionConfiguration;

    public MongoDBStoreExecutor(MongoDBStoreState state, MongoDBStoreExecutorConfiguration storeExecutionConfiguration)
    {
        this.state = state;
        this.storeExecutionConfiguration = storeExecutionConfiguration;
    }



    @Override
    public StoreExecutionState buildStoreExecutionState()
    {
        return new MongoDBStoreExecutionState(this.state, this.storeExecutionConfiguration.getCredentialProviderProvider());
    }

    @Override
    public StoreState getStoreState()
    {
        return this.state;
    }


}
