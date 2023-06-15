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

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestSnowflakeManager
{
    @Test
    public void testCreateLocalDataSourceSpecificationFromVault_PropertyNotFound() throws IOException
    {
        Properties snowflakeLocalDataSourceSpecFileProperties = new Properties();
        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(snowflakeLocalDataSourceSpecFileProperties));

        try
        {
            this.testCreateLocalDataSourceSpecificationFromVault_Impl();
            fail("Failed to throw exception");
        }
        catch (Exception e)
        {
            String message = "Cannot create a local Snowflake datasource specification. Exception = java.lang.NullPointerException: Failed to find property 'legend-local-snowflake-accountName' in vault";
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void testCreateLocalDataSourceSpecificationFromVault() throws IOException
    {
        Properties snowflakeLocalDataSourceSpecFileProperties = new Properties();
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-accountName", "accountNameValue");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-region", "us-east-2");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-warehouseName", "warehouse1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-databaseName", "database1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-cloudType", "aws");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-role", "role1");

        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-privateKeyVaultReference", "ref1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-passphraseVaultReference", "phrase1");
        snowflakeLocalDataSourceSpecFileProperties.setProperty("legend-local-snowflake-publicuserName", "user1");

        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(snowflakeLocalDataSourceSpecFileProperties));

        this.testCreateLocalDataSourceSpecificationFromVault_Impl();
    }

    private void testCreateLocalDataSourceSpecificationFromVault_Impl()
    {
        SnowflakeManager snowflakeManager = new SnowflakeManager();

        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        snowflakeDatasourceSpecification.accountName = "legend-local-snowflake-accountName";
        snowflakeDatasourceSpecification.databaseName = "legend-local-snowflake-databaseName";
        snowflakeDatasourceSpecification.role = "legend-local-snowflake-role";
        snowflakeDatasourceSpecification.warehouseName = "legend-local-snowflake-warehouseName";
        snowflakeDatasourceSpecification.region = "legend-local-snowflake-region";
        snowflakeDatasourceSpecification.cloudType = "legend-local-snowflake-cloudType";

        SnowflakePublicAuthenticationStrategy authenticationStrategy = new SnowflakePublicAuthenticationStrategy();
        authenticationStrategy.privateKeyVaultReference = "legend-local-snowflake-privateKeyVaultReference";
        authenticationStrategy.passPhraseVaultReference = "legend-local-snowflake-passphraseVaultReference";
        authenticationStrategy.publicUserName = "legend-local-snowflake-publicuserName";

        DataSourceSpecification localDataSourceSpecification = snowflakeManager.getLocalDataSourceSpecification(snowflakeDatasourceSpecification, authenticationStrategy);
        assertTrue(localDataSourceSpecification instanceof SnowflakeDataSourceSpecification);
    }
}
