// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.h2.tools.Server;
import org.junit.*;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.finos.legend.engine.plan.execution.stores.relational.TestExecutionScope.buildTestExecutor;

@Ignore
public class TestSemiStructuredWrite
{
    protected static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TestSemiStructuredWrite.class);
    private static final String DRIVER_CLASS_NAME = "org.h2.Driver";
    protected static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    protected static int serverPort;
    protected Server server;
    protected PlanExecutor planExecutor;
    protected RelationalExecutor testRelationalExecutor;


    @BeforeClass
    public static void setUpDriver()
    {
        try
        {
            Class.forName(DRIVER_CLASS_NAME);

            Enumeration<Driver> e = DriverManager.getDrivers();
            while (e.hasMoreElements())
            {
                Driver d = e.nextElement();
                if (!d.getClass().getName().equals(DRIVER_CLASS_NAME))
                {
                    try
                    {
                        DriverManager.deregisterDriver(d);
                    }
                    catch (Exception ignored)
                    {
                    }
                }
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }


    @Before
    public void setupServer() throws Exception
    {
        boolean successful = false;
        for (int attempts = 0; !successful && attempts < 3; attempts++)
        {
            try
            {
                serverPort = DynamicPortGenerator.generatePort();
                server = AlloyH2Server.startServer(serverPort);
                successful = true;
            }
            catch (Exception e)
            {
                // Maybe try again
            }
        }
        if (!successful)
        {
            throw new IllegalStateException("Unable to create H2 Server");
        }
        insertTestData(serverPort);
        planExecutor = buildPlanExecutor();
        System.out.println("Finished setup");
    }

    protected PlanExecutor buildPlanExecutor()
    {
        return PlanExecutor.newPlanExecutor(Relational.build(serverPort), InMemory.build());
    }

    protected String executePlan(SingleExecutionPlan plan, Map<String, ?> params)
    {
        RelationalResult result = (RelationalResult) planExecutor.execute(plan, params, null);
        return result.flush(new RelationalResultToJsonDefaultSerializer(result));
    }

    @After
    public void tearDownServer() throws Exception
    {
        server.shutdown();
        server.stop();
        System.out.println("Teardown complete");
    }

    public static int getServerPort()
    {
        return serverPort;
    }

    protected void insertTestData(int serverPort) throws SQLException
    {
        testRelationalExecutor = buildTestExecutor(serverPort);
        Connection testDBConnection = testRelationalExecutor.getConnectionManager().getTestDatabaseConnection();
        try (Statement statement = testDBConnection.createStatement())
        {
            insertTestData(statement);
        }
    }

    protected void insertTestData(Statement statement) throws SQLException
    {
        statement.execute("Drop table if exists Person_Table;");
        statement.execute("Create Table Person_Table(FIRM VARCHAR(100) , FIRST_NAME VARCHAR(100) , KERBEROS VARCHAR(100) , LAST_NAME VARCHAR(100), IN_Z TIMESTAMP, OUT_Z TIMESTAMP)");
    }

    @Test
    public  void testSemiStructuredWrite() throws Exception
    {
        String input = "{\n" +
                "  \"kerberos\": \"kerberos 98\",\n" +
                "  \"firstName\": \"firstName 98\",\n" +
                "  \"lastName\": \"lastName 56\",\n" +
                "  \"firm\": {\n" +
                "    \"name\": \"name 71\",\n" +
                "    \"numberOfEmployees\": 54\n" +
                "  }\n" +
                "}";
        String plan = readContent(modelResourcePathSemiStructured());
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        Map inputMap = Maps.mutable.empty();
        inputMap.put("jsonString",input);
        Result result = planExecutor.execute(executionPlan, inputMap);
        Assert.assertTrue("Success - 1 rows updated!".equals(((ConstantResult)result).getValue()), () -> String.format("Write Failed. Result: %s", ((ConstantResult)result).getValue()));

    }

    @Test
    public  void testSemiStructuredWriteWithProcessingDate() throws Exception
    {
        String input = "{\n" +
                "  \"kerberos\": \"kerberos 98\",\n" +
                "  \"firstName\": \"firstName 98\",\n" +
                "  \"lastName\": \"lastName 56\",\n" +
                "  \"firm\": {\n" +
                "    \"name\": \"name 71\",\n" +
                "    \"numberOfEmployees\": 54\n" +
                "  }\n" +
                "}";
        String plan = readContent(modelResourcePathSemiStructuredWithProcessingDate());
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        Map inputMap = Maps.mutable.empty();
        inputMap.put("jsonString",input);
        Result result = planExecutor.execute(executionPlan, inputMap);
        Assert.assertTrue("Success - 1 rows updated!".equals(((ConstantResult)result).getValue()), () -> String.format("Write Failed. Result: %s", ((ConstantResult)result).getValue()));
    }

    public void testStatement(String s)
    {
        testRelationalExecutor = buildTestExecutor(serverPort);
        Connection testDBConnection = testRelationalExecutor.getConnectionManager().getTestDatabaseConnection();
        try (Statement statement = testDBConnection.createStatement())
        {
           ResultSet r =  statement.executeQuery(s);
           while (r.next())
           {
//               r.next();
               System.out.println(r.getString(1) + " " + r.getString(2) + " "  + r.getString(3) + " "  + r.getString(4));
           }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String modelResourcePathSemiStructured()
    {
        return "/org/finos/legend/engine/server/testSemiStructured.json";
    }

    public String modelResourcePathSemiStructuredWithProcessingDate()
    {
        return "/org/finos/legend/engine/server/testSemiStructuredWithProcessingDate.json";
    }

    private String readContent(String resourcePath)
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(Objects.requireNonNull(TestSemiStructuredWrite.class.getResourceAsStream(resourcePath)))))
        {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
