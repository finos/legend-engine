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

import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.StoreState;
import org.finos.legend.engine.plan.execution.stores.StoreType;

public class RelationalStoreState implements StoreState
{
    private final RelationalExecutor relationalExecutor;
    private final RelationalExecutorInfo relationalExecutorInfo = new RelationalExecutorInfo();

    public RelationalStoreState(TemporaryTestDbConfiguration temporarytestdb, RelationalExecutionConfiguration relationalExecutionConfiguration)
    {
        this.relationalExecutor = new RelationalExecutor(temporarytestdb, relationalExecutionConfiguration);
    }

    public RelationalStoreState(int port)
    {
        this(new TemporaryTestDbConfiguration(port), new RelationalExecutionConfiguration("/tmp/"));
    }

    @Override
    public StoreType getStoreType()
    {
        return StoreType.Relational;
    }

    @Override
    public RelationalExecutorInfo getStoreExecutionInfo()
    {
        return this.relationalExecutorInfo;
    }

    public RelationalExecutor getRelationalExecutor()
    {
        return this.relationalExecutor;
    }
}
