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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierKeytabAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.identity.credential.middletier.MiddleTierKeytabCredential;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.util.Arrays;
import java.util.Optional;

public abstract class AbstractMiddletierKeytabAuthenticationFlow implements DatabaseAuthenticationFlow<StaticDatasourceSpecification, MiddleTierKeytabAuthenticationStrategy>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMiddletierKeytabAuthenticationFlow.class);

    @Override
    public Class<StaticDatasourceSpecification> getDatasourceClass()
    {
        return StaticDatasourceSpecification.class;
    }

    @Override
    public Class<MiddleTierKeytabAuthenticationStrategy> getAuthenticationStrategyClass()
    {
        return MiddleTierKeytabAuthenticationStrategy.class;
    }

    @Override
    public abstract DatabaseType getDatabaseType();

    @Override
    public Credential makeCredential(Identity identity, StaticDatasourceSpecification datasourceSpecification, MiddleTierKeytabAuthenticationStrategy authenticationStrategy) throws Exception
    {
        throw new UnsupportedOperationException("Unsafe attempt to make a credential without a runtime context");
    }

    @Override
    public Credential makeCredential(Identity identity, StaticDatasourceSpecification datasourceSpecification, MiddleTierKeytabAuthenticationStrategy authenticationStrategy, RuntimeContext credentialAcquisitionContext) throws Exception
    {
        String runtimeContext = credentialAcquisitionContext.getContextParams().get("context");
        if (runtimeContext == null)
        {
            throw new UnsupportedOperationException("Unsafe attempt to make a credential with a (Java) null runtime context");
        }

        MiddleTierKeytabCredential middleTierKeytabCredential = this.getKeytabMetadataFromVault(authenticationStrategy.keytabMetadataVaultReference);
        this.validateKeytabMetadataPointsToKeytab(authenticationStrategy.keytabVaultReference, middleTierKeytabCredential);
        this.validateKeytabAgainstRuntimeContext(authenticationStrategy.keytabVaultReference, middleTierKeytabCredential, runtimeContext);

        String keytabFileLocation = resolveKeytabFileLocation(authenticationStrategy);
        Subject subject = SubjectTools.getSubjectFromKeytab(keytabFileLocation, authenticationStrategy.principal, true);
        return new LegendKerberosCredential(subject);
    }

    // Note - This is an extension method to allow vault implementations to implement custom behavior
    protected String resolveKeytabFileLocation(MiddleTierKeytabAuthenticationStrategy authenticationStrategy)
    {
        return Vault.INSTANCE.getValue(authenticationStrategy.keytabVaultReference);
    }

    protected MiddleTierKeytabCredential getKeytabMetadataFromVault(String keytabMetadataVaultReference) throws Exception
    {
        String keytabVaultMetatadataAsString = Vault.INSTANCE.getValue(keytabMetadataVaultReference);
        if (keytabVaultMetatadataAsString == null)
        {
            throw new Exception(String.format("Failed to locate keytab metadata using vault reference '%s'", keytabMetadataVaultReference));
        }
        return new ObjectMapper().readValue(keytabVaultMetatadataAsString, MiddleTierKeytabCredential.class);
    }

    protected MiddleTierKeytabCredential validateKeytabMetadataPointsToKeytab(String keytabVaultReference, MiddleTierKeytabCredential middleTierKeytabCredential) throws Exception
    {
        // Does the metadata refer to the keytab
        if (!middleTierKeytabCredential.getKeytabReference().equals(keytabVaultReference))
        {
            LOGGER.warn(String.format("Use of keytab with reference '%s' not authorized. Mismatch between keytab vault reference and it's associated vault metadata", keytabVaultReference));
            throw new Exception(String.format("Use of keytab with reference '%s' not authorized. Mismatch between keytab vault reference and it's associated vault metadata", keytabVaultReference));
        }
        return middleTierKeytabCredential;
    }

    // Note - This is an extension method to allow additional organization specific validation logic
    protected abstract void runAdditionalKeytabMetadataValidations(String keytabVaultReference, String keytabMetadataReference);

    protected void validateKeytabAgainstRuntimeContext(String keyTabReference, MiddleTierKeytabCredential middleTierKeytabCredential, String runtimeContext) throws Exception
    {
        Optional<String> matches = Arrays.stream(middleTierKeytabCredential.getUsageContexts()).filter(usageContext -> usageContext.equals(runtimeContext)).findAny();
        if (!matches.isPresent())
        {
            LOGGER.warn(String.format("Use of keytab with reference '%s' not authorized. Mismatch between runtime context '%s' and keytab metadata contexts '%s'", keyTabReference, runtimeContext, Arrays.toString(middleTierKeytabCredential.getUsageContexts())));
            throw new Exception(String.format("Use of keytab with reference '%s' not authorized. Mismatch between runtime context and keytab metadata context", keyTabReference));
        }
    }
}