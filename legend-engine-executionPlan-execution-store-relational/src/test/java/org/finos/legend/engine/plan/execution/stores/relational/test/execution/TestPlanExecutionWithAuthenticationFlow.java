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

package org.finos.legend.engine.plan.execution.stores.relational.test.execution;

import java.sql.Connection;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.authentication.vaults.InMemoryVaultForTesting;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class TestPlanExecutionWithAuthenticationFlow extends AbstractTestPlanExecution
{
    private static final int port = DynamicPortGenerator.generatePort();

    @BeforeClass
    public static void setupClass() throws Exception
    {
        registerDriver("org.h2.Driver");
        AlloyH2Server.startServer(port);
    }

    public Connection setupH2Connection() throws Exception
    {
        Connection conn = new RelationalStoreState(port).getRelationalExecutor().getConnectionManager().getTestDatabaseConnection();

        exec("drop table if exists employeeTable", conn);
        exec("create table employeeTable(id INT, name VARCHAR(200), firmId INT, doh TIMESTAMP, type VARCHAR(200), active INT , skills VARCHAR(200))", conn);
        exec("insert into employeeTable (id, name, firmId, doh, type, active,skills) values (1, 'Alice',  0, '1983-03-15', 'FTC', 1, null)", conn);
        exec("insert into employeeTable (id, name, firmId, doh, type, active,skills) values (2, 'Bob',    0, '2003-07-19', 'FTE', 0, ',1,2,')", conn);
        exec("insert into employeeTable (id, name, firmId, doh, type, active,skills) values (3, 'Curtis', 0, '2012-08-25', 'FTO', null, ',3,2,')", conn);

        exec("Drop table if exists PersonSet1;", conn);
        exec("Create Table PersonSet1 (ID INT, lastName_s1 VARCHAR(200), FirmID INT, ADDRESSID INT, NICKNAME VARCHAR(200));", conn);
        exec("insert into PersonSet1 (ID, lastName_s1, FirmID, ADDRESSID) values (1, 'Doe', 1, 1);", conn);
        exec("insert into PersonSet1 (ID, lastName_s1, FirmID, ADDRESSID) values (2, 'Jones', 1, 1);", conn);
        exec("insert into PersonSet1 (ID, lastName_s1, FirmID, ADDRESSID) values (3, 'Evans', 2, 2);", conn);

        exec("Drop table if exists PersonSet2;", conn);
        exec("Create Table PersonSet2 (ID INT, lastName_s2 VARCHAR(200), FirmID INT, ADDRESSID INT);", conn);
        exec("insert into PersonSet2 (ID, lastName_s2, FirmID, ADDRESSID) values (1, 'Smith', 1, 1);", conn);
        exec("insert into PersonSet2 (ID, lastName_s2, FirmID, ADDRESSID) values (2, 'Johnson', 1, 1);", conn);

        return conn;
    }

    @Test
    public void testPlanExecutionWithLocalH2() throws Exception
    {
        Connection connection = setupH2Connection();

        String connectionSpec = "\"connection\": {\n" +
                "       \"_type\": \"RelationalDatabaseConnection\",\n" +
                "       \"type\": \"H2\",\n" +
                "       \"authenticationStrategy\" : {\n" +
                "           \"_type\" : \"test\"\n" +
                "       },\n" +
                "       \"datasourceSpecification\" : {\n" +
                "           \"_type\" : \"h2Local\"\n" +
                "       }\n" +
                "    }";
        String plan = this.buildPlan(connectionSpec);
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult) executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(port))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::mapping::enumeration::model::mapping::employeeTestMapping\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_mapping_enumeration_model_domain_Employee\",\"properties\":[{\"property\":\"id\",\"type\":\"Integer\"},{\"property\":\"name\",\"type\":\"String\"},{\"property\":\"dateOfHire\",\"type\":\"Date\"},{\"property\":\"type\",\"type\":\"meta::relational::tests::mapping::enumeration::model::domain::EmployeeType\"},{\"property\":\"active\",\"type\":\"meta::relational::tests::mapping::enumeration::model::domain::YesNo\"}],\"class\":\"meta::relational::tests::mapping::enumeration::model::domain::Employee\"}],\"class\":\"meta::relational::tests::mapping::enumeration::model::domain::Employee\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".id as \\\"pk_0\\\", \\\"root\\\".id as \\\"id\\\", \\\"root\\\".name as \\\"name\\\", \\\"root\\\".doh as \\\"dateOfHire\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".active as \\\"active\\\" from employeeTable as \\\"root\\\"\"}], \"result\" : {\"columns\" : [\"pk_0\",\"id\",\"name\",\"dateOfHire\",\"type\",\"active\"], \"rows\" : [{\"values\": [1,1,\"Alice\",\"1983-03-15T00:00:00.000000000+0000\",\"CONTRACT\",\"YES\"]},{\"values\": [2,2,\"Bob\",\"2003-07-19T00:00:00.000000000+0000\",\"FULL_TIME\",\"NO\"]},{\"values\": [3,3,\"Curtis\",\"2012-08-25T00:00:00.000000000+0000\",\"CONTRACT\",null]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));

        closeConnection(connection);
    }

    @Ignore
    // TODO - fix this test. For now it registers a fake key just to verify just to verify credential resolution
    public void testPlanExecutionWithSnowflakeKeyPair() throws Exception
    {
        String connectionSpec = "\"connection\": {\n" +
                "       \"_type\": \"RelationalDatabaseConnection\",\n" +
                "       \"type\": \"Snowflake\",\n" +
                "       \"authenticationStrategy\" : {\n" +
                "           \"_type\" : \"snowflakePublic\",\n" +
                "           \"publicUserName\" : \"fakeUser1\",\n" +
                "           \"privateKeyVaultReference\" : \"fakePrivateKeyRef1\",\n" +
                "           \"passPhraseVaultReference\" : \"fakePassphraseRef1\"\n" +
                "       },\n" +
                "       \"datasourceSpecification\" : {\n" +
                "           \"_type\" : \"snowflake\",\n" +
                "           \"accountName\" : \"ki79827\",\n" +
                "           \"region\" : \"us-east-2\",\n" +
                "           \"warehouseName\" : \"LEGENDRO_WH\",\n" +
                "           \"databaseName\" : \"KNOEMA_RENEWABLES_DATA_ATLAS\",\n" +
                "           \"cloudType\" : \"aws\"\n" +
                "       }\n" +
                "    }";

        InMemoryVaultForTesting inMemoryVault = new InMemoryVaultForTesting();
        inMemoryVault.setValue("fakePrivateKeyRef1", "notused");
        inMemoryVault.setValue("fakePassphraseRef1", "notused");
        Vault.INSTANCE.registerImplementation(inMemoryVault);
        String plan = this.buildPlan(connectionSpec);

        try
        {
            SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
            RelationalResult result = (RelationalResult) executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(port))))));
            Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::mapping::enumeration::model::mapping::employeeTestMapping\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_mapping_enumeration_model_domain_Employee\",\"properties\":[{\"property\":\"id\",\"type\":\"Integer\"},{\"property\":\"name\",\"type\":\"String\"},{\"property\":\"dateOfHire\",\"type\":\"Date\"},{\"property\":\"type\",\"type\":\"meta::relational::tests::mapping::enumeration::model::domain::EmployeeType\"},{\"property\":\"active\",\"type\":\"meta::relational::tests::mapping::enumeration::model::domain::YesNo\"}],\"class\":\"meta::relational::tests::mapping::enumeration::model::domain::Employee\"}],\"class\":\"meta::relational::tests::mapping::enumeration::model::domain::Employee\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".id as \\\"pk_0\\\", \\\"root\\\".id as \\\"id\\\", \\\"root\\\".name as \\\"name\\\", \\\"root\\\".doh as \\\"dateOfHire\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".active as \\\"active\\\" from employeeTable as \\\"root\\\"\"}], \"result\" : {\"columns\" : [\"pk_0\",\"id\",\"name\",\"dateOfHire\",\"type\",\"active\"], \"rows\" : [{\"values\": [1,1,\"Alice\",\"1983-03-15T00:00:00.000000000+0000\",\"CONTRACT\",\"YES\"]},{\"values\": [2,2,\"Bob\",\"2003-07-19T00:00:00.000000000+0000\",\"FULL_TIME\",\"NO\"]},{\"values\": [3,3,\"Curtis\",\"2012-08-25T00:00:00.000000000+0000\",\"CONTRACT\",null]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("Failed to initialize pool: net.snowflake.client.jdbc.SnowflakeSQLLoggedException: Private key provided is invalid or not supported: Use java.security.interfaces.RSAPrivateCrtKey.class for the private key"));
        }
    }
}