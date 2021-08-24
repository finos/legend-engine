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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.UserPasswordAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.pac4j.core.profile.CommonProfile;

import javax.sql.DataSource;
import java.util.Properties;

public class SnowflakeDataSourceSpecification extends DataSourceSpecification {
    public static String SNOWFLAKE_ACCOUNT_NAME = "legend_snowflake_accountName";
    public static String SNOWFLAKE_REGION = "legend_snowflake_region";
    public static String SNOWFLAKE_WAREHOUSE_NAME = "legend_snowflake_warehouseName";
    public static String SNOWFLAKE_DATABASE_NAME = "legend_snowflake_databaseName";
    public static String SNOWFLAKE_CLOUD_TYPE = "legend_snowflake_cloudType";
    public static String SNOWFLAKE_QUOTE_IDENTIFIERS = "legend_snowflake_quoteIdentifiers";

    private Properties properties = new Properties();
    private String databaseName;
    private String accountName;
    private String region;
    private SnowflakeDataSourceSpecificationKey key;
    private String cloudType;
    private String warehouseName;
    private Boolean quoteIdentifiers;

    public SnowflakeDataSourceSpecification(SnowflakeDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, RelationalExecutorInfo relationalExecutorInfo) {
        super(key, databaseManager, authenticationStrategy, extraUserProperties, relationalExecutorInfo);

        this.key = key;
        this.warehouseName = updateSnowflakeIdentifiers(key.getWarehouseName(), key.getQuoteIdentifiers());
        this.databaseName = updateSnowflakeIdentifiers(key.getDatabaseName(), key.getQuoteIdentifiers());
        this.accountName = key.getAccountName();
        this.region = key.getRegion();
        this.cloudType = key.getCloudType();
        this.quoteIdentifiers = key.getQuoteIdentifiers();

        this.properties.put("account", key.getAccountName());
        this.properties.put("warehouse", warehouseName);
        this.properties.put("db", databaseName);
        this.properties.put("ocspFailOpen", true);
    }

    public SnowflakeDataSourceSpecification(SnowflakeDataSourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, RelationalExecutorInfo relationalExecutorInfo) {
        this(key, databaseManager, authenticationStrategy, new Properties(), relationalExecutorInfo);
    }

    @Override
    protected String buildJdbcURL() {
        // TODO : epsstan : refactor validation
        return "jdbc:snowflake://" + accountName + "." + region + "." + cloudType + ".snowflakecomputing.com";
    }

    @Override
    protected DataSource buildDataSource(MutableList<CommonProfile> profiles) {
        return super.buildDataSourceImpl(profiles);
    }

    // TODO : epsstan : do we need 2 data source properties ??
    @Override
    protected Properties buildExtraJdbcDatasourceProperties() {
        Properties properties = new Properties();
        properties.put(SNOWFLAKE_ACCOUNT_NAME, key.getAccountName());
        properties.put(SNOWFLAKE_REGION, key.getRegion());
        properties.put(SNOWFLAKE_WAREHOUSE_NAME, warehouseName);
        properties.put(SNOWFLAKE_DATABASE_NAME, databaseName);
        properties.put(SNOWFLAKE_CLOUD_TYPE, key.getCloudType());
        properties.put(SNOWFLAKE_QUOTE_IDENTIFIERS, key.getQuoteIdentifiers());
        properties.put("account", key.getAccountName());
        properties.put("warehouse", warehouseName);
        properties.put("db", databaseName);
        properties.put("ocspFailOpen", true);
        return properties;
    }

    @Override
    public boolean isDatabaseIdentifiersCaseSensitive() {
        return this.quoteIdentifiers == null || !this.quoteIdentifiers;
    }

    public static String updateSnowflakeIdentifiers(String identifier, boolean quoteIdentifiers) {
        if (quoteIdentifiers && identifier != null && !(identifier.startsWith("\"") && identifier.endsWith("\""))) {
            identifier = "\"" + identifier + "\"";
        }
        return identifier;
    }

    @Override
    public Pair<String, Properties> handleConnection(String url, Properties properties)
    {
        if (this.authenticationStrategy instanceof UserPasswordAuthenticationStrategy) {
            UserPasswordAuthenticationStrategy userPasswordAuthenticationStrategy = (UserPasswordAuthenticationStrategy) authenticationStrategy;
            Properties connectionProperties = new Properties();
            connectionProperties.putAll(properties);
            connectionProperties.put("user", "fred");
            connectionProperties.put("password", userPasswordAuthenticationStrategy.getPassword());
            return Tuples.pair(url, connectionProperties);
        }
        return Tuples.pair(url, properties);
    }
}

