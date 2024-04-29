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

package org.finos.legend.engine.plan.execution.stores.relational.connection.test;

import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Ignore;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.Properties;

public class ExternalIntegration_TestConnectionObjectProtocol_SnowflakeDemo extends DbSpecificTests
{
    @Override
    protected Subject getSubject()
    {
        return null;
    }

    // Deliberately being ignored - Test to be reverted along with local mode
    @Ignore
    public void testUsingKeyPairWithSFWest() throws Exception
    {
        Class.forName("net.snowflake.client.jdbc.SnowflakeDriver");
        testSnowflakePublicConnectionWithSFWest(c -> c.getConnectionUsingIdentity(Identity.getAnonymousIdentity()));
    }

    public void testSnowflakePublicConnectionWithSFWest(Function<DataSourceSpecification, Connection> toDBConnection) throws Exception
    {
        Properties properties = new Properties();
        properties.put("privateKeyRef", "XXXXXX");
        properties.put("passphraseRef", "YYYYYY");

        PropertiesVaultImplementation propertiesVaultImplementation = new PropertiesVaultImplementation(properties);

        Vault.INSTANCE.registerImplementation(propertiesVaultImplementation);

        SnowflakeDataSourceSpecification ds =
                new SnowflakeDataSourceSpecification(
                        new SnowflakeDataSourceSpecificationKey(
                                "pkb24938",
                                "prod3.us-west-2",
                                "demo_wh1",
                                "demo_db1",
                                "aws",
                                null,
                                "demo_role1"),
                        new SnowflakeManager(),
                        new SnowflakePublicAuthenticationStrategy(
                                "privateKeyRef",
                                "passphraseRef",
                                "DEMO_USER"));
        try (Connection connection = toDBConnection.valueOf(ds))
        {
            testConnection(connection, "select * from demo_db1.demo_schema1.FIRM");
        }
    }


    // Deliberately being ignored - Test to be reverted along with local mode
    @Ignore
    public void testUsingKeyPairWithSFEast() throws Exception
    {
        Class.forName("net.snowflake.client.jdbc.SnowflakeDriver");
        testSnowflakePublicConnectionWithSFEast(c -> c.getConnectionUsingIdentity(Identity.getAnonymousIdentity()));
    }

    public void testSnowflakePublicConnectionWithSFEast(Function<DataSourceSpecification, Connection> toDBConnection) throws Exception
    {
        Properties properties = new Properties();
        properties.put("privateKeyRef", "AAAAAA");
        properties.put("passphraseRef", "BBBBBB");

        PropertiesVaultImplementation propertiesVaultImplementation = new PropertiesVaultImplementation(properties);

        Vault.INSTANCE.registerImplementation(propertiesVaultImplementation);

        SnowflakeDataSourceSpecification ds =
                new SnowflakeDataSourceSpecification(
                        new SnowflakeDataSourceSpecificationKey(
                                "ki79827",
                                "us-east-2",
                                "SUMMIT_DEV",
                                "SUMMIT_DEV",
                                "aws",
                                null,
                                "SUMMIT_DEV"),
                        new SnowflakeManager(),
                        new SnowflakePublicAuthenticationStrategy(
                                "privateKeyRef",
                                "passphraseRef",
                                "SUMMIT_DEV1"));
        try (Connection connection = toDBConnection.valueOf(ds))
        {
            testConnection(connection, "select * from SUMMIT_DEV.SCHEMA1.FIRM");
        }
    }
}
