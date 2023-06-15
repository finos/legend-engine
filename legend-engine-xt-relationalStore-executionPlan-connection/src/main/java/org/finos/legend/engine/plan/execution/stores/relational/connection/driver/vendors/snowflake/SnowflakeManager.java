//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeAccountType;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SnowflakeManager extends DatabaseManager
{

    public static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeManager.class);

    public static final String PRIVATELINK_SNOWFLAKECOMPUTING_COM = ".privatelink.snowflakecomputing.com";
    public static final String SNOWFLAKECOMPUTING_COM = ".snowflakecomputing.com";

    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("Snowflake");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        Assert.assertTrue(extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME) != null, () -> SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME + " is not set");
        Assert.assertTrue(extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION) != null, () -> SnowflakeDataSourceSpecification.SNOWFLAKE_REGION + " is not set");
        Assert.assertTrue(extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME) != null, () -> SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME + " is not set");

        String accountName = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME);
        String region = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION);
        String cloudType = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE);
        String organisation = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ORGANIZATION_NAME);

        String accountTypeName = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_TYPE_NAME);
        SnowflakeAccountType accountType = accountTypeName != null ? SnowflakeAccountType.valueOf(accountTypeName) : null;


        StringBuilder URL = new StringBuilder().append("jdbc:snowflake://");
        if (accountType != null)
        {
            if (SnowflakeAccountType.VPS.equals(accountType))
            {
                URL.append(accountName)
                        .append(".").append(organisation)
                        .append(".").append(region)
                        .append(".").append(cloudType);
                URL.append(PRIVATELINK_SNOWFLAKECOMPUTING_COM);
            }
            else if (SnowflakeAccountType.MultiTenant.equals(accountType))
            {
                this.buildMultiTenantHostname(accountName, region, URL);
                URL.append(PRIVATELINK_SNOWFLAKECOMPUTING_COM);
            }
        }
        else
        {
            URL.append(accountName)
                    .append(".").append(region)
                    .append(".").append(cloudType);
            URL.append(SNOWFLAKECOMPUTING_COM);
        }
        return URL.toString();
    }


    public void buildMultiTenantHostname(String accountName, String region, StringBuilder url)
    {
        url.append(accountName).append(".").append(region);
    }


    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new SnowflakeCommands();
    }

    @Override
    public DataSourceSpecification getLocalDataSourceSpecification(DatasourceSpecification protocolDataSourceSpecification, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy protocolAuthenticationStrategy)
    {
        return this.loadLocalDataSourceSpecification(protocolDataSourceSpecification, protocolAuthenticationStrategy);
    }

    private SnowflakeDataSourceSpecification loadLocalDataSourceSpecification(DatasourceSpecification protocolDataSourceSpecification, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy protocolAuthenticationStrategy)
    {
        try
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy
                    templateAuthStrategy = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy) protocolAuthenticationStrategy;

            SnowflakeDatasourceSpecification templateDataSource = (SnowflakeDatasourceSpecification) protocolDataSourceSpecification;
            
            String accountName = this.lookupRequiredPropertyInVault(templateDataSource.accountName);
            String region = this.lookupRequiredPropertyInVault(templateDataSource.region);
            String warehouse = this.lookupRequiredPropertyInVault(templateDataSource.warehouseName);
            String databaseName = this.lookupRequiredPropertyInVault(templateDataSource.databaseName);
            String cloudType = this.lookupRequiredPropertyInVault(templateDataSource.cloudType);
            String role = this.lookupRequiredPropertyInVault(templateDataSource.role);
            SnowflakeDataSourceSpecificationKey key = new SnowflakeDataSourceSpecificationKey(accountName, region, warehouse, databaseName, cloudType, false, role);

            String userName = this.lookupRequiredPropertyInVault(templateAuthStrategy.publicUserName);

            SnowflakePublicAuthenticationStrategy authenticationStrategy = new SnowflakePublicAuthenticationStrategy(templateAuthStrategy.privateKeyVaultReference, templateAuthStrategy.passPhraseVaultReference, userName);
            SnowflakeDataSourceSpecification snowflakeDataSourceSpecification = new SnowflakeDataSourceSpecification(key, this, authenticationStrategy, new Properties());
            return snowflakeDataSourceSpecification;
        }
        catch (Exception e)
        {
            String message = String.format("Cannot create a local Snowflake datasource specification. Exception = %s", e);
            throw new UnsupportedOperationException(message, e);
        }
    }

    private String lookupRequiredPropertyInVault(String property)
    {
        Vault instance = Vault.INSTANCE;
        if (!instance.hasValue(property))
        {
            throw new NullPointerException("Failed to find property '" + property + "' in vault");
        }
        return instance.getValue(property).trim();
    }
}
