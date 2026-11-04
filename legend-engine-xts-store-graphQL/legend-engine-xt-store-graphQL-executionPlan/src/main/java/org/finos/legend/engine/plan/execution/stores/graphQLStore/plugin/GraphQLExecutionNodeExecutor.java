// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.graphQLStore.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.InputStreamResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.graphQLStore.GraphQLHttpConnectionBuilder;
import org.finos.legend.engine.plan.execution.stores.graphQLStore.auth.GraphQLConnectionProvider;
import org.finos.legend.engine.plan.execution.stores.graphQLStore.auth.GraphQLStoreConnectionSpecification;
import org.finos.legend.engine.protocol.graphQLStore.executionPlan.GraphQLExecutionNode;
import org.finos.legend.engine.protocol.graphQLStore.executionPlan.GraphQLVariableBinding;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AggregationAwareExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.AllocationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ConstantExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ErrorExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FreeMarkerConditionalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FunctionParametersValidationNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.GraphFetchM2MExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.MultiResultSequenceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.PureExpressionPlatformExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SequenceExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.GraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.LocalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.StoreMappingGlobalGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryCrossStoreGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryPropertyGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.InMemoryRootGraphFetchExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch.store.inMemory.StoreStreamReadingExecutionNode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class GraphQLExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Identity identity;
    private final ExecutionState executionState;

    public GraphQLExecutionNodeExecutor(Identity identity, ExecutionState executionState)
    {
        this.identity = identity;
        this.executionState = executionState;
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        if (executionNode instanceof GraphQLExecutionNode)
        {
            return executeGraphQLNode((GraphQLExecutionNode) executionNode);
        }
        return null;
    }

    private Result executeGraphQLNode(GraphQLExecutionNode node)
    {
        try
        {
            Map<String, Object> variables = resolveVariables(node);
            JsonNode data = executeSingleRequest(node, variables);

            if (node.unwrapAlias != null && data != null)
            {
                if (!data.has(node.unwrapAlias))
                {
                    throw new EngineException("GraphQL request [" + node.url + "] response is missing the expected top-level field '" + node.unwrapAlias + "'");
                }
                data = data.get(node.unwrapAlias);
            }

            byte[] dataBytes = (data == null ? "null" : data.toString()).getBytes(StandardCharsets.UTF_8);
            return new InputStreamResult(new ByteArrayInputStream(dataBytes));
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    // Caller is responsible for unwrapping whatever shape "data" holds.
    private JsonNode executeSingleRequest(GraphQLExecutionNode node, Map<String, Object> variables) throws Exception
    {
        String requestBody = buildRequestBody(node.query, variables);

        URI uri = new URIBuilder(node.url).build();
        StringEntity bodyEntity = new StringEntity(requestBody, ContentType.APPLICATION_JSON);
        GraphQLStoreConnectionSpecification connectionSpecification = new GraphQLStoreConnectionSpecification(uri, bodyEntity);

        CredentialProviderProvider credentialProviderProvider = ((GraphQLStoreExecutionState) this.executionState.getStoreExecutionState(StoreType.GraphQL)).getCredentialProviderProvider();
        GraphQLConnectionProvider connectionProvider = new GraphQLConnectionProvider(credentialProviderProvider);
        GraphQLHttpConnectionBuilder connectionBuilder = connectionProvider.makeConnection(connectionSpecification, node.authenticationSpecification, this.identity);

        return connectionBuilder.execute(httpResponse ->
        {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            String responseBody = httpResponse.getEntity() == null ? "" : EntityUtils.toString(httpResponse.getEntity());

            if (statusCode != HttpStatus.SC_OK)
            {
                throw new EngineException("GraphQL request [" + node.url + "] failed with status " + statusCode + " - " + responseBody);
            }

            JsonNode responseJson = OBJECT_MAPPER.readTree(responseBody);
            JsonNode errors = responseJson.get("errors");
            if (errors != null && errors.isArray() && errors.size() > 0)
            {
                List<String> errorMessages = Lists.mutable.empty();
                errors.forEach(e -> errorMessages.add(e.has("message") ? e.get("message").asText() : e.toString()));
                throw new EngineException("GraphQL request [" + node.url + "] returned errors: " + String.join("; ", errorMessages));
            }

            return responseJson.get("data");
        });
    }

    private Map<String, Object> resolveVariables(GraphQLExecutionNode node) throws Exception
    {
        Map<String, Object> variables = Maps.mutable.empty();

        if (node.variables != null)
        {
            for (GraphQLVariableBinding binding : node.variables)
            {
                variables.put(binding.name, OBJECT_MAPPER.readTree(binding.value));
            }
        }

        if (node.requiredVariableInputs != null)
        {
            node.requiredVariableInputs.forEach(v ->
            {
                Result result = this.executionState.getResult(v.name);
                if (result != null)
                {
                    if (!(result instanceof ConstantResult))
                    {
                        throw new RuntimeException("GraphQL store: expected a constant value for variable '" + v.name + "', got " + result.getClass().getSimpleName());
                    }
                    variables.put(v.name, ((ConstantResult) result).getValue());
                }
                else if (v.multiplicity != null && v.multiplicity.lowerBound >= 1)
                {
                    throw new EngineException("GraphQL store: unable to resolve required variable '" + v.name + "' - no value available for the parent row's join key/argument");
                }
            });
        }
        return variables;
    }

    private static String buildRequestBody(String query, Map<String, Object> variables) throws Exception
    {
        Map<String, Object> body = Maps.mutable.with("query", query, "variables", variables);
        return OBJECT_MAPPER.writeValueAsString(body);
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
    public Result visit(InMemoryCrossStoreGraphFetchExecutionNode inMemoryCrossStoreGraphFetchExecutionNode)
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
    public Result visit(FreeMarkerConditionalExecutionNode freeMarkerConditionalExecutionNode)
    {
        throw new RuntimeException("Not implemented!");
    }
}
