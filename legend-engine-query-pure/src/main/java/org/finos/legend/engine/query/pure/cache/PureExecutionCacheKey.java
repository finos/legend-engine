// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.query.pure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.plan.execution.cache.executionPlan.PlanCacheKey;
import org.finos.legend.engine.protocol.pure.v1.model.context.SDLC;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.util.List;

public class PureExecutionCacheKey implements PlanCacheKey
{
    private final String pureFunction;
    private final String runtime;
    private final String mapping;
    private final String modelSource;
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();


    public PureExecutionCacheKey(List<ValueSpecification> body, Runtime runtime, String mapping, SDLC sdlcInfo) throws JsonProcessingException
    {
        this.pureFunction = objectMapper.writeValueAsString(body);
        this.runtime = objectMapper.writeValueAsString(runtime);
        this.mapping = mapping;
        this.modelSource = objectMapper.writeValueAsString(sdlcInfo);

    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        PureExecutionCacheKey that = (PureExecutionCacheKey) o;

        if (!pureFunction.equals(that.pureFunction))
        {
            return false;
        }
        if (!runtime.equals(that.runtime))
        {
            return false;
        }
        if (!mapping.equals(that.mapping))
        {
            return false;
        }
        return modelSource.equals(that.modelSource);
    }

    @Override
    public int hashCode()
    {
        int result = pureFunction.hashCode();
        result = 31 * result + runtime.hashCode();
        result = 31 * result + mapping.hashCode();
        result = 31 * result + modelSource.hashCode();
        return result;
    }
}
