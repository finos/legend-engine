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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.RedshiftDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;
import org.finos.legend.engine.shared.core.vault.Vault;

public class RedshiftWithUserPasswordFlow implements DatabaseAuthenticationFlow<RedshiftDatasourceSpecification, UserNamePasswordAuthenticationStrategy>
{
    @Override
    public Class<RedshiftDatasourceSpecification> getDatasourceClass()
    {
        return RedshiftDatasourceSpecification.class;
    }

    @Override
    public Class<UserNamePasswordAuthenticationStrategy> getAuthenticationStrategyClass()
    {
        return UserNamePasswordAuthenticationStrategy.class;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Redshift;
    }

    @Override
    public Credential makeCredential(Identity identity, RedshiftDatasourceSpecification datasourceSpecification, UserNamePasswordAuthenticationStrategy authStrategy) throws Exception
    {
        String userNameVaultKey = authStrategy.baseVaultReference == null ? authStrategy.userNameVaultReference : authStrategy.baseVaultReference + authStrategy.userNameVaultReference;
        String passwordVaultKey = authStrategy.baseVaultReference == null ? authStrategy.passwordVaultReference : authStrategy.baseVaultReference + authStrategy.passwordVaultReference;
        String userName = Vault.INSTANCE.getValue(userNameVaultKey);
        String password = Vault.INSTANCE.getValue(passwordVaultKey);
        return new PlaintextUserPasswordCredential(userName, password);
    }
}
