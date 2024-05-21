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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreType;

public class MongoDBStoreExecutorBuilder implements StoreExecutorBuilder
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Mongo");
    }

    @Override
    public StoreType getStoreType()
    {
        return StoreType.NonRelational_MongoDB;
    }

    @Override
    public MongoDBStoreExecutor build()
    {
        return (MongoDBStoreExecutor) build(MongoDBStoreExecutorConfiguration.newInstance().build());
    }

    @Override
    public MongoDBStoreExecutor build(StoreExecutorConfiguration storeExecutorConfiguration)
    {
        if (!(storeExecutorConfiguration instanceof MongoDBStoreExecutorConfiguration))
        {
            throw new IllegalStateException("Incorrect store execution configuration, expected MongoDBStoreExecutorConfiguration. Please reach out to dev team");
        }
        return new MongoDBStoreExecutor(new MongoDBStoreState(), (MongoDBStoreExecutorConfiguration) storeExecutorConfiguration);
    }
}
