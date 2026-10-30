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

package org.finos.legend.engine.plan.execution.stores.graphQLStore;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.protocol.graphQLStore.executionPlan.GraphQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.OAuthCredential;

import java.util.Collections;
import java.util.List;

public class GraphQLStoreExecutionExtension implements IGraphQLStoreExecutionExtension
{
    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("Store", "GraphQL");
    }

    @Override
    public List<Function3<ExecutionNode, Identity, ExecutionState, Result>> getExtraNodeExecutors()
    {
        return Collections.singletonList((executionNode, identity, executionState) ->
        {
            if (executionNode instanceof GraphQLExecutionNode)
            {
                return executionNode.accept(executionState.getStoreExecutionState(StoreType.GraphQL).getVisitor(identity, executionState));
            }
            return null;
        });
    }

    @Override
    public List<Function3<AuthenticationSpecification, Credential, GraphQLHttpConnectionBuilder, Boolean>> getExtraAuthenticationProcessors()
    {
        return Lists.mutable.with((authenticationSpecification, credential, connectionBuilder) ->
        {
            if (credential instanceof OAuthCredential)
            {
                connectionBuilder.requestBuilder.addHeader("Authorization", "Bearer " + ((OAuthCredential) credential).getAccessToken());
                return true;
            }
            return null;
        });
    }
}
