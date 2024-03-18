// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.api.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Map;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;

public class ExecutionRequest
{
    private final ExecutionPlan executionPlan;
    private final Map<String, Object> executionParameters;

    public ExecutionRequest(ExecutionPlan executionPlan)
    {
        this(executionPlan, null);
    }

    @JsonCreator
    public ExecutionRequest(@JsonProperty("executionPlan") ExecutionPlan executionPlan, @JsonProperty("executionParameters") Map<String, Object> executionParameters)
    {
        this.executionPlan = executionPlan;
        this.executionParameters = executionParameters == null ? Collections.emptyMap() : executionParameters;
    }

    private static Map<String, Result> processParameters(Map<String, Object> executionParameters)
    {
        return MapIterate.collect(executionParameters,
                (key, obj) ->
                {
                    Result result;
                    if (obj instanceof Result)
                    {
                        result = (Result) obj;
                    }
                    else
                    {
                        result = new ConstantResult(obj);
                    }

                    return Tuples.pair(key, result);
                });
    }

    public ExecutionPlan getExecutionPlan()
    {
        return this.executionPlan;
    }

    public Map<String, Object> getExecutionParameters()
    {
        return this.executionParameters;
    }

    public Map<String, Result> getExecutionParametersAsResult()
    {
        return processParameters(this.executionParameters);
    }

    @JsonIgnore
    public SingleExecutionPlan getSingleExecutionPlan()
    {
        return this.executionPlan.getSingleExecutionPlan(this.executionParameters);
    }
}
