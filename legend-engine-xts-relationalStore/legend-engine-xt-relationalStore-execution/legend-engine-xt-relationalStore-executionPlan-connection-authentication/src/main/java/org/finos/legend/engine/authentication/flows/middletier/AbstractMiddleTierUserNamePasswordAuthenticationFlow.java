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

package org.finos.legend.engine.authentication.flows.middletier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.SortedMaps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierUserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.middletier.MiddleTierUserPasswordCredential;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.MAC_CONTEXT_PARAM;
import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.RESOURCE_CONTEXT_PARAM;
import static org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput.USAGE_CONTEXT_PARAM;

public abstract class AbstractMiddleTierUserNamePasswordAuthenticationFlow implements DatabaseAuthenticationFlow<StaticDatasourceSpecification, MiddleTierUserNamePasswordAuthenticationStrategy>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMiddleTierUserNamePasswordAuthenticationFlow.class);

    @Override
    public Class<StaticDatasourceSpecification> getDatasourceClass()
    {
        return StaticDatasourceSpecification.class;
    }

    @Override
    public Class<MiddleTierUserNamePasswordAuthenticationStrategy> getAuthenticationStrategyClass()
    {
        return MiddleTierUserNamePasswordAuthenticationStrategy.class;
    }

    @Override
    public Credential makeCredential(Identity identity, StaticDatasourceSpecification datasourceSpecification, MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy) throws Exception
    {
        return this.makeCredential(identity, datasourceSpecification, authenticationStrategy, RuntimeContext.empty());
    }

    @Override
    public Credential makeCredential(Identity identity, StaticDatasourceSpecification datasourceSpecification, MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy, RuntimeContext credentialAcquisitionContext) throws Exception
    {
        String logMessage = String.format("Acquiring middle tier credential. Context params ={}", credentialAcquisitionContext.getContextParams());
        LOGGER.info(new LogInfo(LoggingEventType.MIDDLETIER_CREDENTIAL_ACQUISITION, logMessage).toString());

        this.parseUsageContext(credentialAcquisitionContext);
        this.parseResourceContext(credentialAcquisitionContext);
        // TODO - remove
        this.verifyMAC(null);

        MiddleTierUserPasswordCredential credentialFromVault = this.getCredentialFromVault(authenticationStrategy.vaultReference);
        return credentialFromVault;
    }

    private void verifyMAC(String mac) throws Exception
    {
    }

    private PlanExecutionAuthorizerInput.ExecutionMode parseUsageContext(RuntimeContext credentialAcquisitionContext)
    {
        String contextValue = null;
        try
        {
            ImmutableMap<String, String> contextParams = credentialAcquisitionContext.getContextParams();
            if (!contextParams.containsKey(USAGE_CONTEXT_PARAM))
            {
                throw new RuntimeException(String.format("Credential acquisition context does not contain a parameter named '%s'. Supplied context values=%s", USAGE_CONTEXT_PARAM, contextParams));
            }
            contextValue = contextParams.get(USAGE_CONTEXT_PARAM);
            return PlanExecutionAuthorizerInput.ExecutionMode.valueOf(contextValue);
        }
        catch (IllegalArgumentException e)
        {
            String values = Lists.immutable.of(PlanExecutionAuthorizerInput.ExecutionMode.values()).collect(c -> c.name()).makeString(",");
            throw new RuntimeException(String.format("Invalid value for parameter '%s' . Supplied value=%s, Valid values=%s", USAGE_CONTEXT_PARAM, contextValue, values));
        }
    }

    private String parseResourceContext(RuntimeContext credentialAcquisitionContext)
    {
        return this.parseContext(credentialAcquisitionContext, RESOURCE_CONTEXT_PARAM);
    }

    private String parseMACContext(RuntimeContext credentialAcquisitionContext)
    {
        return this.parseContext(credentialAcquisitionContext, MAC_CONTEXT_PARAM);
    }

    private String parseContext(RuntimeContext credentialAcquisitionContext, String paramName)
    {
        ImmutableMap<String, String> contextParams = credentialAcquisitionContext.getContextParams();
        if (!contextParams.containsKey(paramName))
        {
            MutableSortedMap<String, String> sortedMap = SortedMaps.mutable.with(String::compareTo);
            sortedMap.putAll(contextParams.castToMap());
            throw new RuntimeException(String.format("Credential acquisition context does not contain a parameter named '%s'. Supplied context values=%s", paramName, sortedMap));
        }
        return contextParams.get(paramName);
    }

    protected MiddleTierUserPasswordCredential getCredentialFromVault(String vaultReference) throws Exception
    {
        String credentialAsString = Vault.INSTANCE.getValue(vaultReference);
        if (credentialAsString == null)
        {
            throw new Exception(String.format("Failed to locate credential using vault reference '%s'", vaultReference));
        }
        return new ObjectMapper().readValue(credentialAsString, MiddleTierUserPasswordCredential.class);
    }
}