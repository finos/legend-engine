// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.AuroraDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Properties;

public abstract class AbstractAuroraFlow<A extends AuthenticationStrategy> implements DatabaseAuthenticationFlow<AuroraDatasourceSpecification, A>
{
    @Override
    public Class<AuroraDatasourceSpecification> getDatasourceClass()
    {
        return AuroraDatasourceSpecification.class;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Aurora;
    }

    @Override
    public Properties getDataSourceProperties(String dataSourceName, Identity identity, AuroraDatasourceSpecification datasourceSpecification, A authenticationStrategy, RuntimeContext runtimeContext)
    {
        Properties properties = new Properties();
        properties.put("clusterId", dataSourceName);
        properties.put("wrapperPlugins", "initialConnection,failover2,efm2");
        properties.put("wrapperDialect", "aurora-pg");

        if (datasourceSpecification.clusterInstanceHostPattern != null)
        {
            properties.put("clusterInstanceHostPattern", datasourceSpecification.clusterInstanceHostPattern);
        }

        return properties;
    }
}
