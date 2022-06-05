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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.StaticDataSourceSpecificationKey;

import java.util.Properties;

public class StaticDataSourceSpecification extends DataSourceSpecification
{
    public StaticDataSourceSpecification(StaticDataSourceSpecificationKey key, DatabaseManager driver, AuthenticationStrategy authenticationStrategy)
    {
        this(key, driver, authenticationStrategy, new Properties());
    }

    public StaticDataSourceSpecification(StaticDataSourceSpecificationKey key, DatabaseManager driver, AuthenticationStrategy authenticationStrategy, int maxPoolSize, int minPoolSize)
    {
        this(key, driver, authenticationStrategy, new Properties(), maxPoolSize, minPoolSize);
    }

    public StaticDataSourceSpecification(StaticDataSourceSpecificationKey key, DatabaseManager driver, AuthenticationStrategy authenticationStrategy, Properties extraUserDataSourceProperties, int maxPoolSize, int minPoolSize)
    {
        super(key, driver, authenticationStrategy, extraUserDataSourceProperties, maxPoolSize, minPoolSize);
    }

    public StaticDataSourceSpecification(StaticDataSourceSpecificationKey key, DatabaseManager driver, AuthenticationStrategy authenticationStrategy, Properties extraUserDataSourceProperties)
    {
        super(key, driver, authenticationStrategy, extraUserDataSourceProperties);
    }

    @Override
    protected String getJdbcUrl(String host, int port, String databaseName, Properties properties)
    {
        return super.getJdbcUrl(
                ((StaticDataSourceSpecificationKey) this.datasourceKey).getHost(),
                ((StaticDataSourceSpecificationKey) this.datasourceKey).getPort(),
                ((StaticDataSourceSpecificationKey) this.datasourceKey).getDatabaseName(),
                properties);
    }
}
