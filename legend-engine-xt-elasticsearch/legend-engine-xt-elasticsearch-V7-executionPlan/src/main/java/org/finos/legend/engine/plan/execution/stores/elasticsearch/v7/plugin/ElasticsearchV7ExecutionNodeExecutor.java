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
//

package org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.plugin;

import org.apache.http.client.protocol.HttpClientContext;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.connection.ElasticsearchHttpContextUtil;
import org.finos.legend.engine.plan.execution.stores.elasticsearch.v7.result.ExecutionRequestVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNodeVisitor;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.executionPlan.Elasticsearch7RequestExecutionNode;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreConnection;
import org.finos.legend.engine.shared.core.identity.Identity;

public class ElasticsearchV7ExecutionNodeExecutor implements ExecutionNodeVisitor<Result>
{
    private final Identity identity;
    private final ExecutionState executionState;
    private final ElasticsearchV7StoreState state;

    public ElasticsearchV7ExecutionNodeExecutor(Identity identity, ExecutionState executionState, ElasticsearchV7StoreState state)
    {
        this.identity = identity;
        this.executionState = executionState;
        this.state = state;
    }

    @Override
    public Result visit(ExecutionNode executionNode)
    {
        if (executionNode instanceof Elasticsearch7RequestExecutionNode)
        {
            Elasticsearch7RequestExecutionNode esNode = (Elasticsearch7RequestExecutionNode) executionNode;
            Elasticsearch7StoreConnection connection = (Elasticsearch7StoreConnection) esNode.connection.connection;

            HttpClientContext httpClientContext = ElasticsearchHttpContextUtil.authToHttpContext(this.identity, this.executionState.getCredentialProviderProvider(), connection.authSpec, this.state.getProviders());

            return esNode.request.accept(new ExecutionRequestVisitor(this.state.getClient(), httpClientContext, connection.sourceSpec.url, esNode, this.executionState));
        }

        throw new IllegalStateException("should not get here");
    }
}
