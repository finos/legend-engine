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
import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.dependencies.store.serviceStore.IServiceParametersResolutionExecutionNodeSpecifics;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionNodeJavaPlatformHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.stores.service.ServiceExecutor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AggregationAwareExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AllocationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ConstantExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ErrorExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FreeMarkerConditionalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FunctionParametersValidationNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.GraphFetchM2MExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.LimitExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.MultiResultSequenceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.PureExpressionPlatformExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RestServiceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SequenceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ServiceParametersResolutionExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.LocalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.StoreMappingGlobalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryCrossStoreGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryPropertyGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.StoreStreamReadingExecutionNode;
import org.pac4j.core.profile.CommonProfile;

import java.util.Collections;
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

                List<String> mappedParameters;
                if (node.requiredVariableInputs == null)
                {
                    mappedParameters = Collections.emptyList();
                }
                else
                {
                    mappedParameters = ListIterate.collect(node.requiredVariableInputs, v -> v.name);
                }

                String processedUrl = ServiceExecutor.getProcessedUrl(node.url, node.params, mappedParameters, this.executionState);
                List<Header> headers = ServiceExecutor.getProcessedHeaders(node.params, mappedParameters, this.executionState);
                StringEntity requestBodyEntity = ServiceExecutor.getRequestBodyEntity(node.requestBodyDescription, this.executionState);
                return ServiceExecutor.executeHttpService(processedUrl, headers, requestBodyEntity, node.method, node.mimeType, node.securitySchemes, this.profiles);
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

                Map<String, Object> outputMap = nodeSpecifics.resolveServiceParameters(getInputMapForSources(requiredSources));
                outputMap.forEach((key, value) -> this.executionState.addParameterValue(key, value));
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error resolving service store parameters.\n" + e);
            }
            return new ConstantResult("success");
        }
        else if (executionNode instanceof LimitExecutionNode)
        {
            LimitExecutionNode limitExecutionNode = (LimitExecutionNode) executionNode;

            Result childResult = limitExecutionNode.executionNodes.get(0).accept(new ExecutionNodeExecutor(this.profiles, this.executionState));

            Result result;
            if (childResult instanceof StreamingObjectResult)
            {
                StreamingObjectResult<?> streamingChildResult = (StreamingObjectResult<?>) childResult;
                result = new StreamingObjectResult<>(streamingChildResult.getObjectStream().limit(limitExecutionNode.limit), streamingChildResult.getResultBuilder(), streamingChildResult.getChildResult());
            }
            else
            {
                childResult.close();
                throw new UnsupportedOperationException("Expected StreamingObjectResult. Found - " + childResult.getClass());
            }

            return result;
        }
        else
        {
            return null;
        }
    }

    private Map<String, Object> getInputMapForSources(List<String> sources)
    {
        Map<String, Object> inputMap = Maps.mutable.empty();
        sources.forEach(v ->
        {
            Result res = this.executionState.getResult(v);
            if (res == null)
            {
                return;
            }

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
    public Result visit(StoreMappingGlobalGraphFetchExecutionNode storeMappingGlobalGraphFetchExecutionNode)
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
