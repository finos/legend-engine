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
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.MemSqlDatasourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.StaticDataSourceSpecificationKey;
import org.finos.legend.engine.shared.core.vault.Vault;

import java.util.Optional;
import java.util.Properties;

import static java.util.Optional.ofNullable;

public class MemSqlDataSourceSpecification extends DataSourceSpecification
{
    public static String MEMSQL_HOST = "host";
    public static String MEMSQL_PORT = "port";
    public static String MEMSQL_DATABASE_NAME = "databaseName";
    public static String MEMSQL_USE_SSL = "useSsl";

    private final MemSqlDatasourceSpecificationKey key;

    public MemSqlDataSourceSpecification(MemSqlDatasourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties);
        this.key = key;
        this.extraDatasourceProperties.putAll(getProperties());
    }

    public MemSqlDataSourceSpecification(MemSqlDatasourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, int maxPoolSize, int minPoolSize)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties, maxPoolSize, minPoolSize);
        this.key = key;
        this.extraDatasourceProperties.putAll(getProperties());
    }

    private Properties getProperties()
    {
        Properties properties = new Properties();

        ofNullable(key.getDatabaseName()).ifPresent(x -> properties.setProperty(MEMSQL_DATABASE_NAME, x));
        ofNullable(key.isUseSsl()).ifPresent(x -> properties.setProperty(MEMSQL_USE_SSL, String.valueOf(key.useSsl)));

        return properties;
    }

    private void putIfNotEmpty(Properties connectionProperties, String propName, String propValue)
    {
        Optional.ofNullable(propValue).ifPresent(x -> connectionProperties.put(propName, propValue));
    }

    public MemSqlDataSourceSpecification(MemSqlDatasourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy)
    {
        this(key, databaseManager, authenticationStrategy, new Properties());
    }

    @Override
    protected String getJdbcUrl(String host, int port, String databaseName, Properties properties)
    {
        return super.getJdbcUrl(
                ((MemSqlDatasourceSpecificationKey) this.datasourceKey).getHost(),
                ((MemSqlDatasourceSpecificationKey) this.datasourceKey).getPort(),
                ((MemSqlDatasourceSpecificationKey) this.datasourceKey).getDatabaseName(),
                properties);
    }
}
