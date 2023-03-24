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

package org.finos.legend.engine.authentication.flows;

import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TrinoDelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;

import java.util.Optional;

public class TrinoWithDelegatedKerberosFlow
        implements DatabaseAuthenticationFlow<TrinoDatasourceSpecification, TrinoDelegatedKerberosAuthenticationStrategy>
{
    @Override
    public Class<TrinoDatasourceSpecification> getDatasourceClass()
    {
        return TrinoDatasourceSpecification.class;
    }

    @Override
    public Class<TrinoDelegatedKerberosAuthenticationStrategy> getAuthenticationStrategyClass()
    {
        return TrinoDelegatedKerberosAuthenticationStrategy.class;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Trino;
    }

    @Override
    public Credential makeCredential(Identity identity, TrinoDatasourceSpecification trinoDatasourceSpecification, TrinoDelegatedKerberosAuthenticationStrategy delegatedKerberosAuthenticationStrategy)
            throws Exception
    {
        Optional<LegendKerberosCredential> credentialHolder = identity.getCredential(LegendKerberosCredential.class);
        if (!credentialHolder.isPresent())
        {
            throw new Exception("Identity does not contain expected credential of type : " + LegendKerberosCredential.class);
        }
        return credentialHolder.get();
    }
}
