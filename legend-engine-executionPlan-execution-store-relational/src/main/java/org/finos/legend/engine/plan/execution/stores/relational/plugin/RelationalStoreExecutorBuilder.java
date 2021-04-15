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
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreType;

public class RelationalStoreExecutorBuilder implements StoreExecutorBuilder
{
    private static final int DEFAULT_PORT = -1;
    private static final String DEFAULT_TEMP_PATH = "/tmp/";

    private TemporaryTestDbConfiguration testDbConfig;
    private RelationalExecutionConfiguration relationalExecutionConfig;

    @Override
    public StoreType getStoreType()
    {
        return StoreType.Relational;
    }

    @Override
    public RelationalStoreExecutor build()
    {
        TemporaryTestDbConfiguration resolvedTestDbConfig = (this.testDbConfig == null) ? new TemporaryTestDbConfiguration(DEFAULT_PORT) : this.testDbConfig;
        RelationalExecutionConfiguration resolvedRelationalExecutionConfig = (this.relationalExecutionConfig == null) ? new RelationalExecutionConfiguration(DEFAULT_TEMP_PATH) : this.relationalExecutionConfig;
        RelationalStoreState state = new RelationalStoreState(resolvedTestDbConfig, resolvedRelationalExecutionConfig);
        return new RelationalStoreExecutor(state);
    }

    public void setTemporaryTestDbConfiguration(TemporaryTestDbConfiguration testDbConfiguration)
    {
        this.testDbConfig = testDbConfiguration;
    }

    public RelationalStoreExecutorBuilder withTemporaryTestDbConfiguration(TemporaryTestDbConfiguration testDbConfiguration)
    {
        setTemporaryTestDbConfiguration(testDbConfiguration);
        return this;
    }

    public void setRelationalExecutionConfiguration(RelationalExecutionConfiguration relationalExecutionConfig)
    {
        this.relationalExecutionConfig = relationalExecutionConfig;
    }

    public RelationalStoreExecutorBuilder withRelationalExecutionConfiguration(RelationalExecutionConfiguration relationalExecutionConfig)
    {
        setRelationalExecutionConfiguration(relationalExecutionConfig);
        return this;
    }
}
