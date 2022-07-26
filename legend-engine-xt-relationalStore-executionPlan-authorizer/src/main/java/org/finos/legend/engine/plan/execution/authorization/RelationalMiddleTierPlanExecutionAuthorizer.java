//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierUserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.middletier.MiddleTierUserPasswordCredential;
import org.finos.legend.engine.shared.core.vault.Vault;

import java.util.List;

import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.ExecutionMode.INTERACTIVE_EXECUTION;
import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.ExecutionMode.SERVICE_EXECUTION;

public class RelationalMiddleTierPlanExecutionAuthorizer implements PlanExecutionAuthorizer
{
    public static final String DATABASE_PREFIX = "DB";

    private RelationalMiddleTierConnectionCredentialAuthorizer relationalMiddleTierConnectionCredentialAuthorizer;

    private static final String AUTHORIZER_NAME = RelationalMiddleTierPlanExecutionAuthorizer.class.getSimpleName();

    public RelationalMiddleTierPlanExecutionAuthorizer(RelationalMiddleTierConnectionCredentialAuthorizer relationalMiddleTierConnectionCredentialAuthorizer)
    {
        this.relationalMiddleTierConnectionCredentialAuthorizer = relationalMiddleTierConnectionCredentialAuthorizer;
    }

    @Override
    public PlanExecutionAuthorizerOutput evaluate(Identity identity, ExecutionPlan executionPlan, PlanExecutionAuthorizerInput authorizationInput) throws Exception
    {
        return this.evaluateImpl(identity, executionPlan, authorizationInput);
    }

    private PlanExecutionAuthorizerOutput evaluateImpl(Identity identity, ExecutionPlan executionPlan, PlanExecutionAuthorizerInput authorizationInput) throws Exception
    {
        if (executionPlan instanceof SingleExecutionPlan)
        {
            return evaluateSingleExecutionPlan(identity, (SingleExecutionPlan)executionPlan, authorizationInput);
        }
        else
        {
            CompositeExecutionPlan compositeExecutionPlan = (CompositeExecutionPlan) executionPlan;
            SingleExecutionPlan singleExecutionPlan = compositeExecutionPlan.executionPlans.get(compositeExecutionPlan.executionKeyName);
            return evaluateSingleExecutionPlan(identity, singleExecutionPlan, authorizationInput);
        }
    }

    private PlanExecutionAuthorizerOutput evaluateSingleExecutionPlan(Identity identity, SingleExecutionPlan executionPlan, PlanExecutionAuthorizerInput authorizationInput) throws Exception
    {
        MutableList<RelationalDatabaseConnection> nodesWithMiddleTierConnections = this.findNodesWithMiddleTierAuthConnections(executionPlan);

        MutableList<ExecutionAuthorization> authorizations = Lists.mutable.empty();

        PlanExecutionAuthorizerInput.ExecutionMode executionMode = authorizationInput.getExecutionMode();
        if (executionMode == SERVICE_EXECUTION)
        {
            authorizations.addAll(this.evaluateForServiceExecution(identity, authorizationInput.getContextParams(), nodesWithMiddleTierConnections));
        }
        else if (executionMode == INTERACTIVE_EXECUTION)
        {
            authorizations.addAll(this.evaluateForInteractiveExecution(identity, nodesWithMiddleTierConnections));
        }
        else
        {
            throw new UnsupportedOperationException("Unsupported execution mode : " + executionMode);
        }

        MutableListMultimap<ExecutionAuthorization.Status, ExecutionAuthorization> authorizationsByStatus = authorizations.groupBy(ExecutionAuthorization::getStatus);
        MutableList<ExecutionAuthorization> allowedAuthorizations = authorizationsByStatus.get(ExecutionAuthorization.Status.ALLOW);
        MutableList<ExecutionAuthorization> deniedAuthorizations = authorizationsByStatus.get(ExecutionAuthorization.Status.DENY);

        String summary = "";
        if (deniedAuthorizations.isEmpty())
        {
            summary = String.format("Overall authorization was successful. Authorizations granted=%d, Authorizations denied=%d", allowedAuthorizations.size(), deniedAuthorizations.size());
        }
        else
        {
            summary = String.format("Overall authorization was NOT successful. Authorizations granted=%d, Authorizations denied=%d", allowedAuthorizations.size(), deniedAuthorizations.size());
        }

        return new PlanExecutionAuthorizerOutput(AUTHORIZER_NAME, summary, authorizationInput, executionPlan, authorizations.toImmutable());
    }

    private MutableList<ExecutionAuthorization> evaluateForServiceExecution(Identity identity, ImmutableMap<String, String> resourceContexts, MutableList<RelationalDatabaseConnection> nodesWithMiddleTierConnections) throws Exception
    {
        MutableList<ExecutionAuthorization> authorizations = Lists.mutable.empty();

        MutableList<MiddleTierUserNamePasswordAuthenticationStrategy> middleTierAuthNodes =
                nodesWithMiddleTierConnections.collect(relationalDatabaseConnection -> (MiddleTierUserNamePasswordAuthenticationStrategy) relationalDatabaseConnection.authenticationStrategy);

        String servicePath = resourceContexts.get("legend.servicePath");
        String serviceUniqueId = resourceContexts.get("legend.serviceUniqueId");
        String normalizedResourceContext = normalizeServiceResourceContext(servicePath, serviceUniqueId);

        for (MiddleTierUserNamePasswordAuthenticationStrategy middleTierAuthNode : middleTierAuthNodes)
        {
            authorizations.add(this.evaluateCredentialAuthorization(identity, normalizedResourceContext, SERVICE_EXECUTION, middleTierAuthNode));
        }
        return authorizations;
    }

    public static String normalizeServiceResourceContext(String servicePath, String serviceUniqueId)
    {
        String normalizedResourceContext = String.format("id@%s@%s", serviceUniqueId, servicePath)
                 // ':' separates tokens in the id
                .replaceAll(":", "_")
                // '/' slashes in the service pattern
                .replaceAll("/", "_")
                // '{' service path paramemeters
                .replaceAll("\\{", "_")
                .replaceAll("\\}", "_");
        return normalizedResourceContext;
    }

    private MutableList<ExecutionAuthorization> evaluateForInteractiveExecution(Identity identity, MutableList<RelationalDatabaseConnection> nodesWithMiddleTierConnections) throws Exception
    {
        MutableList<ExecutionAuthorization> authorizations = Lists.mutable.empty();

        for (RelationalDatabaseConnection relationalDatabaseConnection : nodesWithMiddleTierConnections)
        {
            MiddleTierUserNamePasswordAuthenticationStrategy middleTierAuthNode = (MiddleTierUserNamePasswordAuthenticationStrategy)relationalDatabaseConnection.authenticationStrategy;
            String normalizedResourceContext = this.resolveDatabaseContext(relationalDatabaseConnection);
            authorizations.add(this.evaluateCredentialAuthorization(identity, normalizedResourceContext, INTERACTIVE_EXECUTION, middleTierAuthNode));
        }
        return authorizations;
    }

    public static String normalizeDatabaseResourceContext(String databaseName, String hostName, int port)
    {
        String normalizedResourceContext = String.format("%s@%s@%s@%d", DATABASE_PREFIX, databaseName, hostName, port)
                .replaceAll(":", "_");
        return normalizedResourceContext;
    }

    /*
        Note : The two resolveDatabaseContext methods are marked as protected so that they can be overridden to define rules for other database types.
        Not all database specifications have the same set of attributes.
        Care must be taken to choose a set of attributes that cannot be spoofed.
     */
    protected String resolveDatabaseContext(RelationalDatabaseConnection relationalDatabaseConnection)
    {
        return resolveDatabaseContext(relationalDatabaseConnection.databaseType, relationalDatabaseConnection.type, relationalDatabaseConnection.datasourceSpecification);
    }

    protected String resolveDatabaseContext(DatabaseType connectionType, DatabaseType databaseType, DatasourceSpecification datasourceSpecification)
    {
        if (databaseType == DatabaseType.MemSQL && datasourceSpecification instanceof StaticDatasourceSpecification)
        {
            StaticDatasourceSpecification staticDatasourceSpecification = (StaticDatasourceSpecification) datasourceSpecification;
            return normalizeDatabaseResourceContext(staticDatasourceSpecification.databaseName, staticDatasourceSpecification.host, staticDatasourceSpecification.port);
        }
        if (databaseType == DatabaseType.Postgres && datasourceSpecification instanceof StaticDatasourceSpecification)
        {
            StaticDatasourceSpecification staticDatasourceSpecification = (StaticDatasourceSpecification) datasourceSpecification;
            return normalizeDatabaseResourceContext(staticDatasourceSpecification.databaseName, staticDatasourceSpecification.host, staticDatasourceSpecification.port);
        }
        throw new UnsupportedOperationException(String.format("Unsupported combination. Connection type=%s, database type=%s, datasource spec=%s", connectionType, databaseType, datasourceSpecification.getClass().getCanonicalName()));
    }

    private ExecutionAuthorization evaluateCredentialAuthorization(Identity identity, String resourceContext, PlanExecutionAuthorizerInput.ExecutionMode executionMode, MiddleTierUserNamePasswordAuthenticationStrategy authNode) throws Exception
    {
        String vaultReference = authNode.vaultReference;
        String policyContext = this.resolvePolicyContext(authNode);

        RelationalMiddleTierConnectionCredentialAuthorizer.CredentialAuthorization credentialAuthorization = relationalMiddleTierConnectionCredentialAuthorizer.evaluate(identity, vaultReference, executionMode, resourceContext, policyContext);

        ExecutionAuthorization.Builder builder = ExecutionAuthorization.withSubject(identity.getName())
                .withResource(Maps.immutable.of("credential", vaultReference))
                .withAction("use")
                .withPolicyParams(Maps.immutable.of("resource", resourceContext, "policy", policyContext))
                .withDetails(credentialAuthorization.getDetails());

        if (credentialAuthorization.isAllowed())
        {
            return builder
                    .withStatus(ExecutionAuthorization.Status.ALLOW)
                    .withSummary("Use of credential allowed by policy")
                    .build();
        }
        else
        {
            return builder
                    .withStatus(ExecutionAuthorization.Status.DENY)
                    .withSummary("Use of credential denied by policy")
                    .build();
        }
    }

    private MutableList<RelationalDatabaseConnection> findNodesWithMiddleTierAuthConnections(SingleExecutionPlan executionPlan)
    {
        List<ExecutionNode> rootExecutionNodes = executionPlan.rootExecutionNode.executionNodes;
        MutableList<ExecutionNode> childExecutionNodes = ListIterate.flatCollect(rootExecutionNodes, executionNode -> executionNode.executionNodes);
        MutableList<ExecutionNode> allExecutionNodes = Lists.mutable.withAll(rootExecutionNodes);
        allExecutionNodes.addAll(childExecutionNodes);
        MutableList<RelationalDatabaseConnection> nodesWithMiddleTierConnections = allExecutionNodes
                .select(node -> node instanceof SQLExecutionNode)
                .collect(node -> (SQLExecutionNode) node)
                .select(sqlExecutionNode -> sqlExecutionNode.connection instanceof RelationalDatabaseConnection)
                .collect(sqlExecutionNode -> (RelationalDatabaseConnection) sqlExecutionNode.connection)
                .select(relationalDatabaseConnection -> relationalDatabaseConnection.authenticationStrategy instanceof MiddleTierUserNamePasswordAuthenticationStrategy);
        return nodesWithMiddleTierConnections;
    }

    // TODO - this code should not assume a credential type
    private String resolvePolicyContext(MiddleTierUserNamePasswordAuthenticationStrategy middleTierUserNamePasswordAuthenticationStrategy) throws Exception
    {
        String vaultReference = middleTierUserNamePasswordAuthenticationStrategy.vaultReference;
        String credentialAsString = Vault.INSTANCE.getValue(vaultReference);
        MiddleTierUserPasswordCredential credential = new ObjectMapper().readValue(credentialAsString, MiddleTierUserPasswordCredential.class);
        return credential.getUsagePolicyContext();
    }

    @Override
    public boolean isMiddleTierPlan(ExecutionPlan executionPlan)
    {
        if (executionPlan instanceof SingleExecutionPlan)
        {
            return isMiddleTierPlanImpl((SingleExecutionPlan) executionPlan);
        }
        else if (executionPlan instanceof CompositeExecutionPlan)
        {
            CompositeExecutionPlan compositeExecutionPlan = (CompositeExecutionPlan) executionPlan;
            SingleExecutionPlan singleExecutionPlan = compositeExecutionPlan.executionPlans.get(compositeExecutionPlan.executionKeyName);
            return this.isMiddleTierPlanImpl(singleExecutionPlan);
        }
        else
        {
            throw new UnsupportedOperationException(String.format("Unsupported execution plan type : %s", executionPlan.getClass().getCanonicalName()));
        }
    }

    private boolean isMiddleTierPlanImpl(SingleExecutionPlan executionPlan)
    {
        List<ExecutionNode> rootExecutionNodes = executionPlan.rootExecutionNode.executionNodes;
        MutableList<ExecutionNode> childExecutionNodes = ListIterate.flatCollect(rootExecutionNodes, executionNode -> executionNode.executionNodes);
        MutableList<ExecutionNode> allExecutionNodes = Lists.mutable.withAll(rootExecutionNodes);
        allExecutionNodes.addAll(childExecutionNodes);

        MutableList<MiddleTierUserNamePasswordAuthenticationStrategy> middleTierUserNamePasswordAuthenticationStrategies = allExecutionNodes
                .select(node -> node instanceof SQLExecutionNode)
                .collect(node -> (SQLExecutionNode) node)
                .select(sqlExecutionNode -> sqlExecutionNode.connection instanceof RelationalDatabaseConnection)
                .collect(sqlExecutionNode -> (RelationalDatabaseConnection) sqlExecutionNode.connection)
                .select(relationalDatabaseConnection -> relationalDatabaseConnection.authenticationStrategy instanceof MiddleTierUserNamePasswordAuthenticationStrategy)
                .collect(relationalDatabaseConnection -> (MiddleTierUserNamePasswordAuthenticationStrategy) relationalDatabaseConnection.authenticationStrategy);

        return !middleTierUserNamePasswordAuthenticationStrategies.isEmpty();
    }
}
