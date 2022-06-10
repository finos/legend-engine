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

package org.finos.legend.engine.authentication.middletier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierUserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.MiddleTierUserPasswordCredential;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

public class MemSqlStaticWithMiddletierUserNamePasswordAuthenticationFlow implements DatabaseAuthenticationFlow<StaticDatasourceSpecification, MiddleTierUserNamePasswordAuthenticationStrategy>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MemSqlStaticWithMiddletierUserNamePasswordAuthenticationFlow.class);

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
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.MemSQL;
    }

    @Override
    public Credential makeCredential(Identity identity, StaticDatasourceSpecification datasourceSpecification, MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy) throws Exception
    {
        throw new UnsupportedOperationException("Unsafe attempt to make a credential without a runtime context");
    }

    @Override
    public Credential makeCredential(Identity identity, StaticDatasourceSpecification datasourceSpecification, MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy, RuntimeContext credentialAcquisitionContext) throws Exception
    {
        String runtimeContext = credentialAcquisitionContext.getContextParams().get("context");
        if (runtimeContext == null)
        {
            throw new UnsupportedOperationException("Unsafe attempt to make a credential with a (Java) null runtime context");
        }

        MiddleTierUserPasswordCredential middleTierUserPasswordCredential = this.getCredentialFromVault(authenticationStrategy.vaultReference);
        this.validateCredentialAgainstRuntimeContext(authenticationStrategy.vaultReference, middleTierUserPasswordCredential, runtimeContext);
        return middleTierUserPasswordCredential;
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

    protected void validateCredentialAgainstRuntimeContext(String vaultReference, MiddleTierUserPasswordCredential middleTierUserPasswordCredential, String runtimeContext) throws Exception
    {
        Optional<String> matches = Arrays.stream(middleTierUserPasswordCredential.getUsageContexts()).filter(usageContext -> usageContext.equals(runtimeContext)).findAny();
        if (!matches.isPresent())
        {
            LOGGER.warn(String.format("Use of credential with reference '%s' not authorized. Mismatch between runtime context '%s' and credential contexts '%s'", vaultReference, runtimeContext, Arrays.toString(middleTierUserPasswordCredential.getUsageContexts())));
            throw new Exception(String.format("Use of credential with reference '%s' not authorized. Mismatch between runtime context and credential contexts", vaultReference));
        }
    }

}