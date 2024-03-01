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

package org.finos.legend.engine.plan.execution.stores.relational;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.RelationalConnectionManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Ignore;

import java.sql.Connection;
import java.util.Optional;
import java.util.Properties;

public class TestRelationalConnectionManagerLocalMode
{
    // Test deliberately ignored - Test to be deleted when reverting the local mode feature
    @Ignore
    public void testLocalModeConnectionCreation() throws Exception
    {
        Properties vaultProperties = new Properties();
        vaultProperties.setProperty("legend-local-snowflake-accountName", "XXXX");
        vaultProperties.setProperty("legend-local-snowflake-region", "prod3.us-west-2");
        vaultProperties.setProperty("legend-local-snowflake-warehouseName", "demo_wh1");
        vaultProperties.setProperty("legend-local-snowflake-databaseName", "demo_db1");
        vaultProperties.setProperty("legend-local-snowflake-cloudType", "aws");
        vaultProperties.setProperty("legend-local-snowflake-role", "demo_role1");

        vaultProperties.setProperty("legend-local-snowflake-privateKeyVaultReference", "XXXX");
        vaultProperties.setProperty("legend-local-snowflake-passphraseVaultReference", "XXXX");
        vaultProperties.setProperty("legend-local-snowflake-publicuserName", "demo_user1");

        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(vaultProperties));

        RelationalConnectionManager manager = new RelationalConnectionManager(22, Lists.mutable.empty());
        String connectionStr =
                "{\n" +
                        "  \"_type\": \"RelationalDatabaseConnection\",\n" +
                        "  \"type\": \"Snowflake\"," +
                        "  \"localMode\": true" +
                        "}";

        RelationalDatabaseConnection connectionSpec = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(connectionStr, RelationalDatabaseConnection.class);
        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        snowflakeDatasourceSpecification.accountName = "legend-local-snowflake-accountName";
        snowflakeDatasourceSpecification.databaseName = "legend-local-snowflake-databaseName";
        snowflakeDatasourceSpecification.role = "legend-local-snowflake-role";
        snowflakeDatasourceSpecification.warehouseName = "legend-local-snowflake-warehouseName";
        snowflakeDatasourceSpecification.region = "legend-local-snowflake-region";
        snowflakeDatasourceSpecification.cloudType = "legend-local-snowflake-cloudType";
        connectionSpec.datasourceSpecification = snowflakeDatasourceSpecification;

        SnowflakePublicAuthenticationStrategy authenticationStrategy = new SnowflakePublicAuthenticationStrategy();
        authenticationStrategy.privateKeyVaultReference = "legend-local-snowflake-privateKeyVaultReference";
        authenticationStrategy.passPhraseVaultReference = "legend-local-snowflake-passphraseVaultReference";
        authenticationStrategy.publicUserName = "legend-local-snowflake-publicuserName";
        connectionSpec.authenticationStrategy = authenticationStrategy;

        Identity identity = DefaultIdentityFactory.INSTANCE.makeUnknownIdentity();
        DataSourceSpecification dataSourceSpecification = manager.getDataSourceSpecification(connectionSpec);

        Connection connection = manager.getDataSourceSpecification(connectionSpec).getConnectionUsingIdentity(identity, Optional.empty());
        System.out.println(connection.createStatement());
    }
}