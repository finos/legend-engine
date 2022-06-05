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

package org.finos.legend.engine.plan.execution.stores.relational.plugin;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreState;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.pac4j.core.profile.CommonProfile;

public class FakeRelationalStoreExecutorBuilder implements StoreExecutorBuilder
{

    @Override
    public StoreType getStoreType()
    {
        return StoreType.Relational;
    }

    public static class Configuration implements StoreExecutorConfiguration
    {
        @Override
        public StoreType getStoreType()
        {
            return StoreType.Relational;
        }
    }

    public static class Executor implements StoreExecutor
    {
        private StoreExecutorConfiguration configuration;

        public Executor(StoreExecutorConfiguration configuration)
        {
            this.configuration = configuration;
        }

        public StoreExecutorConfiguration getConfiguration()
        {
            return configuration;
        }

        @Override
        public StoreExecutionState buildStoreExecutionState()
        {
            return new StoreExecutionState()
            {
                @Override
                public StoreState getStoreState()
                {
                    return null;
                }

                @Override
                public ExecutionNodeVisitor<Result> getVisitor(MutableList<CommonProfile> profiles, ExecutionState executionState)
                {
                    return null;
                }

                @Override
                public StoreExecutionState copy()
                {
                    return null;
                }
            };
        }

        @Override
        public StoreState getStoreState()
        {
            return new StoreState()
            {
                @Override
                public StoreType getStoreType()
                {
                    return StoreType.Relational;
                }

                @Override
                public Object getStoreExecutionInfo()
                {
                    return new Object();
                }
            };
        }
    }

    @Override
    public StoreExecutor build()
    {
        return new Executor(null);
    }

    @Override
    public StoreExecutor build(StoreExecutorConfiguration storeExecutorConfiguration)
    {
        return new Executor(storeExecutorConfiguration);
    }
}
