// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.datapush.server.test;

import org.finos.legend.connection.AuthenticationMechanismConfiguration;
import org.finos.legend.connection.AuthenticationMechanismType;
import org.finos.legend.connection.PostgresTestContainerWrapper;
import org.finos.legend.connection.StoreInstance;
import org.finos.legend.engine.protocol.pure.v1.connection.ConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.connection.UserPasswordAuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.connection.StaticJDBCConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

public class TestDataPushServer extends AbstractDataPushServerResourceTest
{
    private MinIOS3TestContainerWrapper minioContainer;

    private PostgresTestContainerWrapper postgresContainer;

    @Before
    public void setUp()
    {
        try
        {
            // TODO: use @ClassRule
            this.minioContainer = MinIOS3TestContainerWrapper.build();
            this.minioContainer.start();
        }
        catch (Exception e)
        {
            Assume.assumeTrue("Can't start MinIO", false);
        }

        try
        {
            // TODO: use @ClassRule
            this.postgresContainer = PostgresTestContainerWrapper.build();
            this.postgresContainer.start();
        }
        catch (Exception e)
        {
            Assume.assumeTrue("Can't start PostgreSQLContainer", false);
        }

        System.setProperty("passwordRef", this.postgresContainer.getPassword());

        DataPushServerForTest application = this.getApplicationInstance();
        ConnectionSpecification connectionSpecification = new StaticJDBCConnectionSpecification(
                this.postgresContainer.getHost(),
                this.postgresContainer.getPort(),
                this.postgresContainer.getDatabaseName()
        );
        application.getStoreInstanceProvider().injectStoreInstance(new StoreInstance.Builder(application.getEnvironment().getStoreSupport("Postgres"))
                .withIdentifier("test-store")
                .withAuthenticationMechanismConfigurations(
                        new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD).build()
                )
                .withConnectionSpecification(connectionSpecification)
                .build()
        );
        application.getAuthenticationConfigurationProvider()
                .injectAuthenticationConfiguration("test-store",
                        new UserPasswordAuthenticationConfiguration(this.postgresContainer.getUser(), new SystemPropertiesSecret("passwordRef")));
    }

    @After
    public void tearDown() throws Exception
    {
        if (this.minioContainer != null)
        {
            this.minioContainer.stop();
        }

        if (this.postgresContainer != null)
        {
            this.postgresContainer.stop();
        }

        System.clearProperty("passwordRef");
    }

    @Test
    @Ignore
    public void test()
    {
        Response response = this.clientFor("/api/data-push/stage").request().post(Entity.entity("{\n" +
                "  \"_type\": \"sql\",\n" +
                "  \"statements\": [\"Drop table if exists FirmTable;\n" +
                "Create Table FirmTable(id INT, Legal_Name VARCHAR(200));\n" +
                "Insert into FirmTable (id, Legal_Name) values (1, 'FINOS');\"]\n" +
                "}", MediaType.APPLICATION_JSON_TYPE));
        String responseText = response.readEntity(String.class);

        assertEquals("ok", responseText);
    }
}
