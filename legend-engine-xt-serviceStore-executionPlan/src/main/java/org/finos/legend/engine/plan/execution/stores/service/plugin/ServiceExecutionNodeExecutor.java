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

package org.finos.legend.engine.plan.execution.stores.service.plugin;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.dependencies.store.serviceStore.IServiceParametersResolutionExecutionNodeSpecifics;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.service.ServiceExecutor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.*;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GlobalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.LocalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryCrossStoreGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryPropertyGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.StoreStreamReadingExecutionNode;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;
import java.util.Map;

public class ServiceExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    MutableList<CommonProfile> profiles;
    ExecutionState executionState;

    public ServiceExecutionNodeExecutor(MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        this.profiles = profiles;
        this.executionState = executionState;
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        if (executionNode instanceof RestServiceExecutionNode)
        {
            try (Scope scope = GlobalTracer.get().buildSpan("Service Store Execution").startActive(true))
            {
                scope.span().setTag("raw url", ((RestServiceExecutionNode) executionNode).url);
                scope.span().setTag("method", ((RestServiceExecutionNode) executionNode).method.toString());
                RestServiceExecutionNode node = (RestServiceExecutionNode) executionNode;
                return ServiceExecutor.executeHttpService(node.url, node.params, node.requestBodyDescription, node.method, node.mimeType, node.securitySchemes, this.executionState, this.profiles);
            }
        }
        else if (executionNode instanceof ServiceParametersResolutionExecutionNode)
        {
            ServiceParametersResolutionExecutionNode node = (ServiceParametersResolutionExecutionNode) executionNode;

            IServiceParametersResolutionExecutionNodeSpecifics nodeSpecifics = ExecutionNodeJavaPlatformHelper.getNodeSpecificsInstance(node, this.executionState, this.profiles);
            try
            {
                List<String> requiredSources = Lists.mutable.empty();
                if (node.requiredVariableInputs != null)
                {
                    requiredSources.addAll(ListIterate.collect(node.requiredVariableInputs, s -> s.name));
                }
                if (node.propertyInputMap != null)
                {
                    requiredSources.addAll(node.propertyInputMap.values()); //TODO: TO BE REMOVED
                }

                Map<String, Object> outputMap = nodeSpecifics.resolveServiceParameters(getInputMapForSources(requiredSources));
                outputMap.forEach((key, value) -> this.executionState.addParameterValue(key, value));
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error resolving service store parameters.\n" + e);
            }
            return new ConstantResult("success");
        }
        else
        {
            return null;
        }
    }

    private Map<String, Object> getInputMapForSources(List<String> sources)
    {
        Map<String, Object> inputMap = Maps.mutable.empty();
        sources.forEach(v -> {
            Result res = this.executionState.getResult(v);
            if (res instanceof ConstantResult)
            {
                inputMap.put(v, ((ConstantResult) res).getValue());
            }
            else
            {
                throw new RuntimeException(String.format("Expected Constant Result for %s. Found - %s", v, res.getClass().getSimpleName()));
            }
        });

        return inputMap;
    }

    @Deprecated
    @Override
    public Result visit(GraphFetchM2MExecutionNode graphFetchM2MExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(StoreStreamReadingExecutionNode storeStreamReadingExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(InMemoryRootGraphFetchExecutionNode inMemoryRootGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(InMemoryCrossStoreGraphFetchExecutionNode inMemoryRootGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(InMemoryPropertyGraphFetchExecutionNode inMemoryPropertyGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Deprecated
    @Override
    public Result visit(GraphFetchExecutionNode graphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(GlobalGraphFetchExecutionNode globalGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(ErrorExecutionNode errorExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(AggregationAwareExecutionNode aggregationAwareExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(MultiResultSequenceExecutionNode multiResultSequenceExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(SequenceExecutionNode sequenceExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(FunctionParametersValidationNode functionParametersValidationNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(AllocationExecutionNode allocationExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(PureExpressionPlatformExecutionNode pureExpressionPlatformExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(ConstantExecutionNode constantExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(LocalGraphFetchExecutionNode localGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public Result visit(FreeMarkerConditionalExecutionNode localGraphFetchExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }
}
