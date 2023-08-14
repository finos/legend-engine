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

package org.finos.legend.engine.plan.execution.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.IOException;

@JsonPropertyOrder({"contextParams"})
public class PlanExecutionAuthorizerInput
{
    public static String USAGE_CONTEXT_PARAM = "legend.usageContext";
    public static String RESOURCE_CONTEXT_PARAM = "legend.resourceContext";
    public static String MAC_CONTEXT_PARAM = "legend.macContext";

    public enum ExecutionMode
    {
        INTERACTIVE_EXECUTION,
        SERVICE_EXECUTION
    }

    public enum ExecutionAction
    {
        EXECUTE_SERVICE,
        EXPLORE_DATA
    }

    @JsonIgnore
    private ExecutionMode executionMode;
    private ImmutableMap<String, String> contextParams;

    public PlanExecutionAuthorizerInput()
    {
        // jackson
    }

    public PlanExecutionAuthorizerInput(ExecutionMode executionMode, ImmutableMap<String, String> contextParams)
    {
        this.executionMode = executionMode;
        this.contextParams = contextParams;
    }

    public ExecutionMode getExecutionMode()
    {
        return executionMode;
    }

    public ImmutableMap<String, String> getContextParams()
    {
        return contextParams;
    }

    public String toJSON()
    {
        try
        {
            return ObjectMapperFactory
                    .getNewStandardObjectMapperWithPureProtocolExtensionSupports()
                    .writeValueAsString(this);
        }
        catch (IOException e)
        {
            throw new RuntimeException("JSON serialization exception", e);
        }
    }

    public String toPrettyJSON()
    {
        try
        {
            return ObjectMapperFactory
                    .getNewStandardObjectMapperWithPureProtocolExtensionSupports()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(this);
        }
        catch (IOException e)
        {
            throw new RuntimeException("JSON serialization exception", e);
        }
    }

    public static ExecutionAuthorizationInputBuilder with(ExecutionMode executionMode)
    {
        return new ExecutionAuthorizationInputBuilder(executionMode);
    }

    public static class ExecutionAuthorizationInputBuilder
    {
        private ExecutionMode executionMode;
        private MutableMap<String, String> contextParams = Maps.mutable.empty();

        public ExecutionAuthorizationInputBuilder(ExecutionMode executionMode)
        {
            this.executionMode = executionMode;
            this.contextParams.put(USAGE_CONTEXT_PARAM, executionMode.name());
        }

        public ExecutionAuthorizationInputBuilder withResourceContext(String key, String value)
        {
            this.contextParams.put(key, value);
            return this;
        }

        public ExecutionAuthorizationInputBuilder withResourceContext(ImmutableMap<String, String> resourceContexts)
        {
            this.contextParams.putAll(resourceContexts.castToMap());
            return this;
        }

        public PlanExecutionAuthorizerInput build()
        {
            return new PlanExecutionAuthorizerInput(executionMode, contextParams.toImmutable());
        }
    }
}

