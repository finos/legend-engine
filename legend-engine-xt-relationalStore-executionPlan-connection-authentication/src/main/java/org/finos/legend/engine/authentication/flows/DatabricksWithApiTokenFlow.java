// Copyright 2022 Databricks
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.ApiTokenAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatabricksDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.ApiTokenCredential;
import org.finos.legend.engine.shared.core.vault.Vault;

public class DatabricksWithApiTokenFlow implements DatabaseAuthenticationFlow<DatabricksDatasourceSpecification, ApiTokenAuthenticationStrategy>
{
    @Override
    public Class<DatabricksDatasourceSpecification> getDatasourceClass()
    {
        return DatabricksDatasourceSpecification.class;
    }

    @Override
    public Class<ApiTokenAuthenticationStrategy> getAuthenticationStrategyClass()
    {
        return ApiTokenAuthenticationStrategy.class;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Databricks;
    }

    @Override
    public Credential makeCredential(Identity identity, DatabricksDatasourceSpecification datasourceSpecification, ApiTokenAuthenticationStrategy authStrategy) throws Exception
    {
        String apiToken = Vault.INSTANCE.getValue(authStrategy.apiToken);
        if (apiToken == null || apiToken.length() == 0)
        {
            throw new Exception("Could not retrieve API token from default vault");
        }
        return new ApiTokenCredential(apiToken);
    }
}
