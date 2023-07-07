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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.collections.api.factory.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompositeExecutionPlan extends ExecutionPlan
{
    @JsonProperty(required = true)
    public String executionKeyName;
    public List<String> executionKeys = Collections.emptyList();
    public Map<String, SingleExecutionPlan> executionPlans = Maps.mutable.empty();

    public CompositeExecutionPlan()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public CompositeExecutionPlan(Map<String, SingleExecutionPlan> executionPlans, String executionKeyName, List<String> executionKeys)
    {
        this.executionPlans = executionPlans;
        this.executionKeyName = executionKeyName;
        this.executionKeys = executionKeys;
    }

    @Override
    public SingleExecutionPlan getSingleExecutionPlan(Function<? super String, ?> parameterValueAccessor)
    {
        Object planKey = parameterValueAccessor.apply(this.executionKeyName);
        SingleExecutionPlan singleExecutionPlan = this.executionPlans.get(planKey);
        if (singleExecutionPlan == null)
        {
            throw new RuntimeException("No plan exists for key: " + planKey + ". Available keys are : " + this.executionPlans.keySet().stream().sorted().collect(Collectors.joining(", ")) + ".");
        }
        return singleExecutionPlan;
    }
}
