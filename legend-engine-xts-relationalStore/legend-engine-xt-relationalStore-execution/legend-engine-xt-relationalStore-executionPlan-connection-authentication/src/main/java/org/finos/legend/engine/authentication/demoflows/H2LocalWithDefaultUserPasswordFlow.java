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

package org.finos.legend.engine.authentication.demoflows;

import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DefaultH2AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

public class H2LocalWithDefaultUserPasswordFlow implements DatabaseAuthenticationFlow<LocalH2DatasourceSpecification, DefaultH2AuthenticationStrategy>
{
    @Override
    public Class<LocalH2DatasourceSpecification> getDatasourceClass()
    {
        return LocalH2DatasourceSpecification.class;
    }

    @Override
    public Class<DefaultH2AuthenticationStrategy> getAuthenticationStrategyClass()
    {
        return DefaultH2AuthenticationStrategy.class;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.H2;
    }

    @Override
    public Credential makeCredential(Identity identity, LocalH2DatasourceSpecification datasourceSpecification, DefaultH2AuthenticationStrategy authenticationStrategy)
    {
        PlaintextUserPasswordCredential credential = new PlaintextUserPasswordCredential("sa", "");
        return credential;
    }
}