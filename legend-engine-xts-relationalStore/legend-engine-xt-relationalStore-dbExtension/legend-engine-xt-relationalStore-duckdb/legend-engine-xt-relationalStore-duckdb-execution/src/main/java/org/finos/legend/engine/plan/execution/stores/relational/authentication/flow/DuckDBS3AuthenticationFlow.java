// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.authentication.flow;

import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.authentication.DuckDBS3AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DuckDBDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;

public class DuckDBS3AuthenticationFlow implements DatabaseAuthenticationFlow<DuckDBDatasourceSpecification, DuckDBS3AuthenticationStrategy>
{
    @Override
    public Class<DuckDBDatasourceSpecification> getDatasourceClass()
    {
        return DuckDBDatasourceSpecification.class;
    }

    @Override
    public Class<DuckDBS3AuthenticationStrategy> getAuthenticationStrategyClass()
    {
        return DuckDBS3AuthenticationStrategy.class;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.DuckDB;
    }

    @Override
    public Credential makeCredential(Identity identity, DuckDBDatasourceSpecification datasourceSpecification, DuckDBS3AuthenticationStrategy authenticationStrategy) throws Exception
    {
        return new AnonymousCredential();
    }
}
