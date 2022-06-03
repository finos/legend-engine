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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.MiddleTierKeytabAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;

import java.sql.Connection;
import java.util.Properties;

public class MiddleTierKeytabAuthenticationStrategy extends AuthenticationStrategy
{
    private final String principal;
    private final String keytabVaultReference;
    private final String keytabMetadataVaultReference;

    public MiddleTierKeytabAuthenticationStrategy(String principal, String keytabVaultReference, String keytabMetadataVaultReference)
    {
        this.principal = principal;
        this.keytabVaultReference = keytabVaultReference;
        this.keytabMetadataVaultReference = keytabMetadataVaultReference;
    }

    public MiddleTierKeytabAuthenticationStrategy()
    {
        this(null, null, null);
    }

    @Override
    public Connection getConnectionImpl(DataSourceWithStatistics ds, Identity identity) throws ConnectionException
    {
        try
        {
            LegendKerberosCredential legendKerberosCredential = this.resolveCredential(ds.getProperties());
            return getConnectionUsingKerberos(ds.getDataSource(), legendKerberosCredential.getSubject());
        }
        catch (Exception e)
        {
            throw new ConnectionException(e);
        }
    }

    @Override
    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        return Tuples.pair(url, properties);
    }

    private LegendKerberosCredential resolveCredential(Properties properties) throws Exception
    {
        IdentityState identityState = ConnectionStateManager.getInstance().getIdentityStateUsing(properties);
        CredentialSupplier credentialSupplier = identityState.getCredentialSupplier().get();
        // Note : we intentionally pass a null here. The getCredential API accepts an identity as input. In this case, we do not care about the current identity as we are using a keytab file
        return (LegendKerberosCredential)credentialSupplier.getCredential(null);
    }

    @Override
    public MiddleTierKeytabAuthenticationStrategyKey getKey()
    {
        return new MiddleTierKeytabAuthenticationStrategyKey(this.principal, this.keytabVaultReference, this.keytabMetadataVaultReference);
    }

    public String getKeytabVaultReference()
    {
        return this.keytabVaultReference;
    }

    public String getPrincipal()
    {
        return this.principal;
    }

    public String getkeytabMetadataVaultReference() {
        return keytabMetadataVaultReference;
    }

    public String getLogin() {
        return SubjectTools.getUsername(this.principal);
    }
}
