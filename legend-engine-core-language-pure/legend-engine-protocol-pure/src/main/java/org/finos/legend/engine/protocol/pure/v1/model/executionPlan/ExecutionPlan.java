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

package org.finos.legend.engine.protocol.pure.v1.model.executionPlan;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;
import java.util.function.Function;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = SingleExecutionPlan.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SingleExecutionPlan.class, name = "simple"),
        @JsonSubTypes.Type(value = CompositeExecutionPlan.class, name = "composite")
})
public abstract class ExecutionPlan
{
    public SingleExecutionPlan getSingleExecutionPlan(Map<String, ?> params)
    {
        return getSingleExecutionPlan(params::get);
    }

    public abstract SingleExecutionPlan getSingleExecutionPlan(Function<? super String, ?> parameterValueAccessor);
}
