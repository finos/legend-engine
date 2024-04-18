// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreState;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.util.Objects;

public class PlanExecutorInfo
{
    private final ImmutableList<Object> storeExecutionInfos;

    private PlanExecutorInfo(ImmutableList<Object> storeExecutionInfos)
    {
        this.storeExecutionInfos = storeExecutionInfos;
    }

    public ImmutableList<Object> getStoreExecutionInfos()
    {
        return this.storeExecutionInfos;
    }

    public String toJSON()
    {
        try
        {
            return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(this);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static PlanExecutorInfo fromStoreExecutors(Iterable<? extends StoreExecutor> storeExecutors)
    {
        return fromStoreExecutionInfos(LazyIterate.collect(storeExecutors, StoreExecutor::getStoreState).collect(StoreState::getStoreExecutionInfo));
    }

    @JsonCreator
    public static PlanExecutorInfo fromStoreExecutionInfos(@JsonProperty("storeExecutionInfos") Iterable<?> storeExecutionInfos)
    {
        return new PlanExecutorInfo(Lists.immutable.withAll(LazyIterate.select(storeExecutionInfos, Objects::nonNull)));
    }
}
