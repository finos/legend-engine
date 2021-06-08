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

package org.finos.legend.engine.plan.execution.stores.relational.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class TestExecutionPlan
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final int port = DynamicPortGenerator.generatePort();

    @Before
    public void setUp()
    {
        Connection conn = null;
        try
        {
            Class.forName("org.h2.Driver");

            Enumeration<Driver> e = DriverManager.getDrivers();
            MutableList<Driver> found = Lists.mutable.empty();
            while (e.hasMoreElements())
            {
                Driver d = e.nextElement();
                if (!d.getClass().getName().equals("org.h2.Driver"))
                {
                    found.add(d);
                }
            }

            found.forEach((Procedure<Driver>) c -> {
                try
                {
                    DriverManager.deregisterDriver(c);
                }
                catch (Exception ignore)
                {
                }
            });

            AlloyH2Server.startServer(port);

            conn = new RelationalStoreState(port).getRelationalExecutor().getConnectionManager().getTestDatabaseConnection();

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

            System.out.println("finished inserts");
        }
        catch (Exception e)
        {
        }
        finally
        {
            try
            {
                if (conn != null)
                {
                    conn.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void exec(String sql, Connection connection) throws Exception
    {
        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
        statement.close();
    }


    @Test
    public void testPlatform() throws Exception
    {
        String plan = "{\n" +
                "  \"serializer\": {\n" +
                "    \"name\": \"pure\",\n" +
                "    \"version\": \"vX_X_X\"\n" +
                "  },\n" +
                "  \"rootExecutionNode\": {\n" +
                "    \"varName\": \"businessDate\",\n" +
                "    \"_type\": \"allocation\",\n" +
                "    \"resultType\": {\n" +
                "      \"dataType\": \"DateTime\",\n" +
                "      \"_type\": \"dataType\"\n" +
                "    },\n" +
                "    \"executionNodes\": [\n" +
                "      {\n" +
                "        \"pure\": {\n" +
                "          \"function\": \"now\",\n" +
                "          \"fControl\": \"now__DateTime_1_\",\n" +
                "          \"_type\": \"func\"\n" +
                "        },\n" +
                "        \"implementation\": {\n" +
                "          \"executionClassFullName\": \"org.finos.legend.engine.plan.dependencies.store.platform.PredefinedExpressions\",\n" +
                "          \"executionMethodName\": \"now\",\n" +
                "          \"_type\": \"java\"\n" +
                "        },\n" +
                "        \"_type\": \"platform\",\n" +
                "        \"resultType\": {\n" +
                "          \"dataType\": \"DateTime\",\n" +
                "          \"_type\": \"dataType\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        ConstantResult result = runJavaExecutionPlan(executionPlan);
        Assert.assertTrue(result.getValue() instanceof PureDate);
    }

    @Test
    public void testPlatformCompile() throws Exception
    {
        String plan = "{\n" +
                "  \"serializer\": {\n" +
                "    \"name\": \"pure\",\n" +
                "    \"version\": \"vX_X_X\"\n" +
                "  },\n" +
                "  \"rootExecutionNode\": {\n" +
                "    \"varName\": \"businessDate\",\n" +
                "    \"_type\": \"allocation\",\n" +
                "    \"resultType\": {\n" +
                "      \"dataType\": \"Date\",\n" +
                "      \"_type\": \"dataType\"\n" +
                "    },\n" +
                "    \"executionNodes\": [\n" +
                "      {\n" +
                "        \"pure\": {\n" +
                "          \"function\": \"adjust\",\n" +
                "          \"fControl\": \"adjust_Date_1__Integer_1__DurationUnit_1__Date_1_\",\n" +
                "          \"parameters\": [\n" +
                "            {\n" +
                "              \"function\": \"now\",\n" +
                "              \"fControl\": \"now__DateTime_1_\",\n" +
                "              \"_type\": \"func\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"values\": [\n" +
                "                1\n" +
                "              ],\n" +
                "              \"multiplicity\": {\n" +
                "                \"lowerBound\": 1,\n" +
                "                \"upperBound\": 1\n" +
                "              },\n" +
                "              \"_type\": \"integer\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"property\": \"DAYS\",\n" +
                "              \"parameters\": [\n" +
                "                {\n" +
                "                  \"fullPath\": \"meta::pure::functions::date::DurationUnit\",\n" +
                "                  \"_type\": \"enum\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"_type\": \"property\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"_type\": \"func\"\n" +
                "        },\n" +
                "        \"implementation\": {\n" +
                "          \"executionClassFullName\": \"_pure.plan.root.n1.n1.Execute\",\n" +
                "          \"executionMethodName\": \"execute\",\n" +
                "          \"_type\": \"java\"\n" +
                "        },\n" +
                "        \"_type\": \"platform\",\n" +
                "        \"resultType\": {\n" +
                "          \"dataType\": \"Date\",\n" +
                "          \"_type\": \"dataType\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"globalImplementationSupport\": {\n" +
                "    \"classes\": [\n" +
                "      {\n" +
                "         \"package\": \"_pure.plan.root.n1.n1\",\n" +
                "         \"name\": \"Execute\",\n" +
                "         \"source\": \"package _pure.plan.root.n1.n1;\\nimport java.util.Date;\\nimport org.finos.legend.engine.plan.dependencies.domain.date.DurationUnit;\\nimport org.finos.legend.engine.plan.dependencies.domain.date.PureDate;\\nimport org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;\\nimport org.finos.legend.engine.plan.dependencies.util.Library;\\n\\npublic class Execute {\\n\\n    public static Object execute(IExecutionNodeContext context) {\\n        try\\n        {\\n            return Library.adjustDate(PureDate.fromDate(new Date()), 1L, DurationUnit.DAYS);\\n        }\\n        catch (Exception e)\\n        {\\n            throw new RuntimeException(\\\"Failed in node: root.n1.n1\\\", e);\\n        }\\n    }\\n}\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"_type\": \"java\"\n" +
                "  }\n" +
                "}";
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        ConstantResult result = runJavaExecutionPlan(executionPlan);
        Assert.assertTrue(result.getValue() instanceof PureDate);
    }

    @Test
    public void testPlatformCompileConst() throws Exception
    {
        String plan = "{\n" +
                "  \"serializer\": {\n" +
                "    \"name\": \"pure\",\n" +
                "    \"version\": \"vX_X_X\"\n" +
                "  },\n" +
                "  \"rootExecutionNode\": {\n" +
                "    \"varName\": \"businessDate\",\n" +
                "    \"_type\": \"allocation\",\n" +
                "    \"resultType\": {\n" +
                "      \"dataType\": \"Date\",\n" +
                "      \"_type\": \"dataType\"\n" +
                "    },\n" +
                "    \"executionNodes\": [\n" +
                "      {\n" +
                "        \"pure\": {\n" +
                "          \"function\": \"adjust\",\n" +
                "          \"fControl\": \"adjust_Date_1__Integer_1__DurationUnit_1__Date_1_\",\n" +
                "          \"parameters\": [\n" +
                "            {\n" +
                "              \"values\": [\n" +
                "                \"2005-10-10\"\n" +
                "              ],\n" +
                "              \"multiplicity\": {\n" +
                "                \"lowerBound\": 1,\n" +
                "                \"upperBound\": 1\n" +
                "              },\n" +
                "              \"_type\": \"strictDate\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"values\": [\n" +
                "                1\n" +
                "              ],\n" +
                "              \"multiplicity\": {\n" +
                "                \"lowerBound\": 1,\n" +
                "                \"upperBound\": 1\n" +
                "              },\n" +
                "              \"_type\": \"integer\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"property\": \"DAYS\",\n" +
                "              \"parameters\": [\n" +
                "                {\n" +
                "                  \"fullPath\": \"meta::pure::functions::date::DurationUnit\",\n" +
                "                  \"_type\": \"enum\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"_type\": \"property\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"_type\": \"func\"\n" +
                "        },\n" +
                "        \"implementation\": {\n" +
                "          \"executionClassFullName\": \"_pure.plan.root.n1.n1.Execute\",\n" +
                "          \"executionMethodName\": \"execute\",\n" +
                "          \"_type\": \"java\"\n" +
                "        },\n" +
                "        \"_type\": \"platform\",\n" +
                "        \"resultType\": {\n" +
                "          \"dataType\": \"Date\",\n" +
                "          \"_type\": \"dataType\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"globalImplementationSupport\": {\n" +
                "    \"classes\": [\n" +
                "      {\n" +
                "         \"package\": \"_pure.plan.root.n1.n1\",\n" +
                "         \"name\": \"Execute\",\n" +
                "         \"source\": \"package _pure.plan.root.n1.n1;\\nimport org.finos.legend.engine.plan.dependencies.domain.date.DurationUnit;\\nimport org.finos.legend.engine.plan.dependencies.domain.date.PureDate;\\nimport org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;\\nimport org.finos.legend.engine.plan.dependencies.util.Library;\\n\\npublic class Execute {\\n\\n    public static Object execute(IExecutionNodeContext context) {\\n        try\\n        {\\n            return Library.adjustDate(PureDate.parsePureDate(\\\"2005-10-10\\\"), 1L, DurationUnit.DAYS);\\n        }\\n        catch (Exception e)\\n        {\\n            throw new RuntimeException(\\\"Failed in node: root.n1.n1\\\", e);\\n        }\\n    }\\n}\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"_type\": \"java\"\n" +
                "  }\n" +
                "}";
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        ConstantResult result = runJavaExecutionPlan(executionPlan);
        Assert.assertTrue(result.getValue() instanceof PureDate);
    }

    private ConstantResult runJavaExecutionPlan(SingleExecutionPlan executionPlan) throws JavaCompileException
    {
        ExecutionState state = new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(port))));
        EngineJavaCompiler engineJavaCompiler = JavaHelper.compilePlan(executionPlan, null);
        if (engineJavaCompiler != null)
        {
            state.setJavaCompiler(engineJavaCompiler);
        }
        return  (ConstantResult) executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, state));
    }

    @Test
    public void testClassWithEnum() throws Exception
    {
        // executionPlan(|meta::relational::tests::mapping::enumeration::model::domain::Employee.all(), meta::relational::tests::mapping::enumeration::model::mapping::employeeTestMapping, meta::relational::tests::mapping::enumeration::enumTestRuntime())->meta::alloy::protocol::vX_X_X::transformation::fromPureGraph::executionPlan::transformPlan()->toJSON([], 1000, config(false, false, true, true));
        String plan = "{\n" +
                "  \"rootExecutionNode\": {\n" +
                "    \"sqlQuery\": \"select \\\"root\\\".id as \\\"pk_0\\\", \\\"root\\\".id as \\\"id\\\", \\\"root\\\".name as \\\"name\\\", \\\"root\\\".doh as \\\"dateOfHire\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".active as \\\"active\\\" from employeeTable as \\\"root\\\"\",\n" +
                "    \"resultColumns\": [\n" +
                "      {\n" +
                "        \"label\": \"\\\"pk_0\\\"\",\n" +
                "        \"dataType\": \"INTEGER\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"id\\\"\",\n" +
                "        \"dataType\": \"INTEGER\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"name\\\"\",\n" +
                "        \"dataType\": \"VARCHAR(200)\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"dateOfHire\\\"\",\n" +
                "        \"dataType\": \"DATE\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"type\\\"\",\n" +
                "        \"dataType\": \"VARCHAR(20)\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"active\\\"\",\n" +
                "        \"dataType\": \"INTEGER\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"connection\": {\n" +
                "       \"_type\": \"RelationalDatabaseConnection\",\n" +
                "       \"type\": \"H2\",\n" +
                "       \"authenticationStrategy\" : {\n" +
                "           \"_type\" : \"test\"\n" +
                "       },\n" +
                "       \"datasourceSpecification\" : {\n" +
                "           \"_type\" : \"static\",\n" +
                "           \"databaseName\" : \"testDB\",\n" +
                "           \"host\":\"127.0.0.1\",\n" +
                "           \"port\" : \""+port+"\"\n" +
                "       }\n" +
                "    }," +
                "    \"_type\": \"relational\",\n" +
                "    \"resultType\": {\n" +
                "      \"class\": \"meta::relational::tests::mapping::enumeration::model::domain::Employee\",\n" +
                "      \"setImplementations\": [\n" +
                "        {\n" +
                "          \"class\": \"meta::relational::tests::mapping::enumeration::model::domain::Employee\",\n" +
                "          \"mapping\": \"meta::relational::tests::mapping::enumeration::model::mapping::employeeTestMapping\",\n" +
                "          \"id\": \"meta_relational_tests_mapping_enumeration_model_domain_Employee\",\n" +
                "          \"propertyMappings\": [\n" +
                "            {\n" +
                "              \"property\": \"id\",\n" +
                "              \"type\": \"Integer\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"property\": \"name\",\n" +
                "              \"type\": \"String\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"property\": \"dateOfHire\",\n" +
                "              \"type\": \"Date\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"property\": \"type\",\n" +
                "              \"type\": \"meta::relational::tests::mapping::enumeration::model::domain::EmployeeType\",\n" +
                "              \"enumMapping\": {\n" +
                "                \"CONTRACT\": [\n" +
                "                  \"FTC\",\n" +
                "                  \"FTO\"\n" +
                "                ],\n" +
                "                \"FULL_TIME\": [\n" +
                "                  \"FTE\"\n" +
                "                ]\n" +
                "              }\n" +
                "            },\n" +
                "            {\n" +
                "              \"property\": \"active\",\n" +
                "              \"type\": \"meta::relational::tests::mapping::enumeration::model::domain::YesNo\",\n" +
                "              \"enumMapping\": {\n" +
                "                \"YES\": [\n" +
                "                  \"1\"\n" +
                "                ],\n" +
                "                \"NO\": [\n" +
                "                  \"0\"\n" +
                "                ]\n" +
                "              }\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ],\n" +
                "      \"_type\": \"class\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult) executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(port))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::mapping::enumeration::model::mapping::employeeTestMapping\",\"classMappings\":[{\"setImplementationId\":\"meta_relational_tests_mapping_enumeration_model_domain_Employee\",\"properties\":[{\"property\":\"id\",\"type\":\"Integer\"},{\"property\":\"name\",\"type\":\"String\"},{\"property\":\"dateOfHire\",\"type\":\"Date\"},{\"property\":\"type\",\"type\":\"meta::relational::tests::mapping::enumeration::model::domain::EmployeeType\"},{\"property\":\"active\",\"type\":\"meta::relational::tests::mapping::enumeration::model::domain::YesNo\"}],\"class\":\"meta::relational::tests::mapping::enumeration::model::domain::Employee\"}],\"class\":\"meta::relational::tests::mapping::enumeration::model::domain::Employee\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".id as \\\"pk_0\\\", \\\"root\\\".id as \\\"id\\\", \\\"root\\\".name as \\\"name\\\", \\\"root\\\".doh as \\\"dateOfHire\\\", \\\"root\\\".type as \\\"type\\\", \\\"root\\\".active as \\\"active\\\" from employeeTable as \\\"root\\\"\"}], \"result\" : {\"columns\" : [\"pk_0\",\"id\",\"name\",\"dateOfHire\",\"type\",\"active\"], \"rows\" : [{\"values\": [1,1,\"Alice\",\"1983-03-15T00:00:00.000000000+0000\",\"CONTRACT\",\"YES\"]},{\"values\": [2,2,\"Bob\",\"2003-07-19T00:00:00.000000000+0000\",\"FULL_TIME\",\"NO\"]},{\"values\": [3,3,\"Curtis\",\"2012-08-25T00:00:00.000000000+0000\",\"CONTRACT\",null]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }

    @Test
    public void testTDSWithEnum() throws Exception
    {
        // executionPlan(|meta::relational::tests::mapping::enumeration::model::domain::Employee.all()->project(a|$a.type, 'xx'), meta::relational::tests::mapping::enumeration::model::mapping::employeeTestMapping, meta::relational::tests::mapping::enumeration::enumTestRuntime())->meta::alloy::protocol::vX_X_X::transformation::fromPureGraph::executionPlan::transformPlan()->toJSON([], 1000, config(false, false, true, true));
        String plan = "{\n" +
                "  \"rootExecutionNode\": {\n" +
                "    \"sqlQuery\": \"select \\\"root\\\".type as \\\"xx\\\" from employeeTable as \\\"root\\\"\",\n" +
                "    \"resultColumns\": [\n" +
                "      {\n" +
                "        \"label\": \"\\\"xx\\\"\",\n" +
                "        \"dataType\": \"VARCHAR(20)\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"connection\": {\n" +
                "       \"_type\": \"RelationalDatabaseConnection\",\n" +
                "       \"type\": \"H2\",\n" +
                "       \"authenticationStrategy\" : {\n" +
                "           \"_type\" : \"test\"\n" +
                "       },\n" +
                "       \"datasourceSpecification\" : {\n" +
                "           \"_type\" : \"static\",\n" +
                "           \"databaseName\" : \"testDB\",\n" +
                "           \"host\":\"127.0.0.1\",\n" +
                "           \"port\" : \""+port+"\"\n" +
                "       }\n" +
                "    }," +
                "    \"_type\": \"relational\",\n" +
                "    \"resultType\": {\n" +
                "      \"tdsColumns\": [\n" +
                "        {\n" +
                "          \"name\": \"xx\",\n" +
                "          \"type\": \"EmployeeType\",\n" +
                "          \"relationalType\": \"VARCHAR(20)\",\n" +
                "          \"enumMapping\": {\n" +
                "            \"CONTRACT\": [\n" +
                "              \"FTC\",\n" +
                "              \"FTO\"\n" +
                "            ],\n" +
                "            \"FULL_TIME\": [\n" +
                "              \"FTE\"\n" +
                "            ]\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"_type\": \"tds\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult) executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(port))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"xx\",\"type\":\"EmployeeType\",\"relationalType\":\"VARCHAR(20)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".type as \\\"xx\\\" from employeeTable as \\\"root\\\"\"}], \"result\" : {\"columns\" : [\"xx\"], \"rows\" : [{\"values\": [\"CONTRACT\"]},{\"values\": [\"FULL_TIME\"]},{\"values\": [\"CONTRACT\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }


    @Test
    public void testUnion() throws Exception
    {
        // executionPlan(|meta::pure::tests::model::simple::Person.all(), meta::relational::tests::mapping::union::unionMapping, meta::relational::tests::testRuntime())->meta::alloy::protocol::vX_X_X::transformation::fromPureGraph::executionPlan::transformPlan()->toJSON([], 1000, config(false, false, true, true));
        String plan = "{\n" +
                "  \"rootExecutionNode\": {\n" +
                "    \"sqlQuery\": \"select \\\"unionBase\\\".u_type as u_type, \\\"unionBase\\\".\\\"pk_0_0\\\" as \\\"pk_0_0\\\", \\\"unionBase\\\".\\\"pk_0_1\\\" as \\\"pk_0_1\\\", \\\"unionBase\\\".\\\"lastName\\\" as \\\"lastName\\\" from (select '0' as u_type, \\\"root\\\".ID as \\\"pk_0_0\\\", null as \\\"pk_0_1\\\", \\\"root\\\".lastName_s1 as \\\"lastName\\\" from PersonSet1 as \\\"root\\\" UNION ALL select '1' as u_type, null as \\\"pk_0_0\\\", \\\"root\\\".ID as \\\"pk_0_1\\\", \\\"root\\\".lastName_s2 as \\\"lastName\\\" from PersonSet2 as \\\"root\\\") as \\\"unionBase\\\"\",\n" +
                "    \"resultColumns\": [\n" +
                "      {\n" +
                "        \"label\": \"u_type\",\n" +
                "        \"dataType\": \"INTEGER\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"pk_0_0\\\"\",\n" +
                "        \"dataType\": \"INTEGER\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"pk_0_1\\\"\",\n" +
                "        \"dataType\": \"INTEGER\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"label\": \"\\\"lastName\\\"\",\n" +
                "        \"dataType\": \"INTEGER\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"connection\": {\n" +
                "       \"_type\": \"RelationalDatabaseConnection\",\n" +
                "       \"type\": \"H2\",\n" +
                "       \"authenticationStrategy\" : {\n" +
                "           \"_type\" : \"test\"\n" +
                "       },\n" +
                "       \"datasourceSpecification\" : {\n" +
                "           \"_type\" : \"static\",\n" +
                "           \"databaseName\" : \"testDB\",\n" +
                "           \"host\":\"127.0.0.1\",\n" +
                "           \"port\" : \""+port+"\"\n" +
                "       }\n" +
                "    }," +
                "    \"_type\": \"relational\",\n" +
                "    \"resultType\": {\n" +
                "      \"class\": \"meta::pure::tests::model::simple::Person\",\n" +
                "      \"setImplementations\": [\n" +
                "        {\n" +
                "          \"class\": \"meta::pure::tests::model::simple::Person\",\n" +
                "          \"mapping\": \"meta::relational::tests::mapping::union::unionMapping\",\n" +
                "          \"id\": \"set1\",\n" +
                "          \"propertyMappings\": [\n" +
                "            {\n" +
                "              \"property\": \"lastName\",\n" +
                "              \"type\": \"String\"\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"class\": \"meta::pure::tests::model::simple::Person\",\n" +
                "          \"mapping\": \"meta::relational::tests::mapping::union::unionMapping\",\n" +
                "          \"id\": \"set2\",\n" +
                "          \"propertyMappings\": [\n" +
                "            {\n" +
                "              \"property\": \"lastName\",\n" +
                "              \"type\": \"String\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ],\n" +
                "      \"_type\": \"class\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult) executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(port))))));
        Assert.assertEquals("{\"builder\": {\"_type\":\"classBuilder\",\"mapping\":\"meta::relational::tests::mapping::union::unionMapping\",\"classMappings\":[{\"setImplementationId\":\"set1\",\"properties\":[{\"property\":\"lastName\",\"type\":\"String\"}],\"class\":\"meta::pure::tests::model::simple::Person\"},{\"setImplementationId\":\"set2\",\"properties\":[{\"property\":\"lastName\",\"type\":\"String\"}],\"class\":\"meta::pure::tests::model::simple::Person\"}],\"class\":\"meta::pure::tests::model::simple::Person\"}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"unionBase\\\".u_type as u_type, \\\"unionBase\\\".\\\"pk_0_0\\\" as \\\"pk_0_0\\\", \\\"unionBase\\\".\\\"pk_0_1\\\" as \\\"pk_0_1\\\", \\\"unionBase\\\".\\\"lastName\\\" as \\\"lastName\\\" from (select '0' as u_type, \\\"root\\\".ID as \\\"pk_0_0\\\", null as \\\"pk_0_1\\\", \\\"root\\\".lastName_s1 as \\\"lastName\\\" from PersonSet1 as \\\"root\\\" UNION ALL select '1' as u_type, null as \\\"pk_0_0\\\", \\\"root\\\".ID as \\\"pk_0_1\\\", \\\"root\\\".lastName_s2 as \\\"lastName\\\" from PersonSet2 as \\\"root\\\") as \\\"unionBase\\\"\"}], \"result\" : {\"columns\" : [\"U_TYPE\",\"pk_0_0\",\"pk_0_1\",\"lastName\"], \"rows\" : [{\"values\": [\"0\",1,null,\"Doe\"]},{\"values\": [\"0\",2,null,\"Jones\"]},{\"values\": [\"0\",3,null,\"Evans\"]},{\"values\": [\"1\",null,1,\"Smith\"]},{\"values\": [\"1\",null,2,\"Johnson\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }

    @Test
    public void testRelationalBlockExecutionNode() throws Exception
    {
        String plan = "{\n" +
                "  \"rootExecutionNode\": {\n" +
                "    \"executionNodes\": [\n" +
                "      {\n" +
                "        \"onConnectionCloseCommitQuery\": \"drop table Firm_temp_123\",\n" +
                "        \"sqlQuery\": \"Create LOCAL TEMPORARY TABLE Firm_temp_123 (name VARCHAR(200));\",\n" +
                "        \"_type\": \"relational\",\n" +
                "        \"connection\": {\n" +
                "            \"_type\": \"RelationalDatabaseConnection\",\n" +
                "            \"type\": \"H2\",\n" +
                "            \"authenticationStrategy\" : {\n" +
                "              \"_type\" : \"test\"\n" +
                "          },\n" +
                "           \"datasourceSpecification\" : {\n" +
                "              \"_type\" : \"static\",\n" +
                "              \"databaseName\" : \"testDB\",\n" +
                "               \"host\":\"127.0.0.1\",\n" +
                "               \"port\" : \""+port+"\"\n" +
                "           }\n" +
                "         }," +
                "        \"resultType\": {\n" +
                "          \"_type\": \"void\"\n" +
                "        },\n" +
                "        \"onConnectionCloseRollbackQuery\": \"drop table Firm_temp_123\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"onConnectionCloseCommitQuery\": \"drop table Person_temp_123\",\n" +
                "        \"sqlQuery\": \"Create LOCAL TEMPORARY TABLE Person_temp_123 (fullname VARCHAR(1000), firmName VARCHAR(1000));\",\n" +
                "        \"_type\": \"relational\",\n" +
                "        \"connection\": {\n" +
                "          \"_type\": \"RelationalDatabaseConnection\",\n" +
                "        \"type\": \"H2\",\n" +
                "        \"authenticationStrategy\" : {\n" +
                "            \"_type\" : \"test\"\n" +
                "        },\n" +
                "           \"datasourceSpecification\" : {\n" +
                "               \"_type\" : \"static\",\n" +
                "            \"databaseName\" : \"testDB\",\n" +
                "            \"host\":\"127.0.0.1\",\n" +
                "            \"port\" : \""+port+"\"\n" +
                "        }\n" +
                "        }," +
                "        \"resultType\": {\n" +
                "          \"_type\": \"void\"\n" +
                "        },\n" +
                "        \"onConnectionCloseRollbackQuery\": \"drop table Person_temp_123\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"sqlQuery\": \"insert into Firm_temp_123(name) values ('FA'), ('FirmA')\",\n" +
                "        \"_type\": \"relational\",\n" +
                "      \"connection\": {\n" +
                "            \"_type\": \"RelationalDatabaseConnection\",\n" +
                "            \"type\": \"H2\",\n" +
                "         \"authenticationStrategy\" : {\n" +
                "             \"_type\" : \"test\"\n" +
                "         },\n" +
                "         \"datasourceSpecification\" : {\n" +
                "               \"_type\" : \"static\",\n" +
                "               \"databaseName\" : \"testDB\",\n" +
                "               \"host\":\"127.0.0.1\",\n" +
                "               \"port\" : \""+port+"\"\n" +
                "           }\n" +
                "       }," +
                "        \"resultType\": {\n" +
                "          \"_type\": \"void\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"sqlQuery\": \"insert into Person_temp_123 (fullname, firmName) values ('abc', 'FA'), ('xyz', 'FA')\",\n" +
                "        \"_type\": \"relational\",\n" +
                "    \"connection\": {\n" +
                "       \"_type\": \"RelationalDatabaseConnection\",\n" +
                "       \"type\": \"H2\",\n" +
                "       \"authenticationStrategy\" : {\n" +
                "           \"_type\" : \"test\"\n" +
                "       },\n" +
                "       \"datasourceSpecification\" : {\n" +
                "           \"_type\" : \"static\",\n" +
                "           \"databaseName\" : \"testDB\",\n" +
                "           \"host\":\"127.0.0.1\",\n" +
                "           \"port\" : \""+port+"\"\n" +
                "       }\n" +
                "    }," +
                "        \"resultType\": {\n" +
                "          \"_type\": \"void\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"sqlQuery\": \"select \\\\\\\"root\\\\\\\".name as \\\\\\\"pk_0\\\\\\\", \\\\\\\"root\\\\\\\".name as \\\\\\\"firmName\\\\\\\", \\\\\\\"personTable\\\\\\\".fullname as \\\\\\\"employee_name\\\\\\\" from Person_temp_123 as \\\\\\\"personTable\\\\\\\" left outer join Firm_temp_123 as \\\\\\\"root\\\\\\\" on (\\\\\\\"root\\\\\\\".name = \\\\\\\"personTable\\\\\\\".firmName)\",\n" +
                "        \"resultColumns\": [\n" +
                "          {\n" +
                "            \"dataType\": \"VARCHAR(200)\",\n" +
                "            \"label\": \"pk_0\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"dataType\": \"VARCHAR(200)\",\n" +
                "            \"label\": \"firmName\"\n" +
                "          },\n" +
                "          {\n" +
                "            \"dataType\": \"VARCHAR(200)\",\n" +
                "            \"label\": \"employee_name\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"_type\": \"relational\",\n" +
                "    \"connection\": {\n" +
                "       \"_type\": \"RelationalDatabaseConnection\",\n" +
                "       \"type\": \"H2\",\n" +
                "       \"authenticationStrategy\" : {\n" +
                "           \"_type\" : \"test\"\n" +
                "       },\n" +
                "       \"datasourceSpecification\" : {\n" +
                "           \"_type\" : \"static\",\n" +
                "           \"databaseName\" : \"testDB\",\n" +
                "           \"host\":\"127.0.0.1\",\n" +
                "           \"port\" : \""+port+"\"\n" +
                "       }\n" +
                "    }," +
                "        \"resultSizeRange\": {\n" +
                "          \"lowerBound\": 0\n" +
                "        },\n" +
                "        \"resultType\": {\n" +
                "          \"_type\": \"tds\",\n" +
                "          \"tdsColumns\": [\n" +
                "            {\n" +
                "              \"type\": \"VARCHAR(200)\",\n" +
                "              \"name\": \"pk_0\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"type\": \"VARCHAR(200)\",\n" +
                "              \"name\": \"firmName\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"type\": \"VARCHAR(200)\",\n" +
                "              \"name\": \"employee_name\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"_type\": \"relationalBlock\",\n" +
                "    \"resultType\": {\n" +
                "      \"dataType\": \"meta::pure::metamodel::type::Any\",\n" +
                "      \"_type\": \"dataType\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"serializer\": {\n" +
                "    \"name\": \"pure\",\n" +
                "    \"version\": \"vX_X_X\"\n" +
                "  }\n" +
                "}";
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        RelationalResult result = (RelationalResult) executionPlan.rootExecutionNode.accept(new ExecutionNodeExecutor(null, new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(port))))));

        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"pk_0\",\"type\":\"VARCHAR(200)\"},{\"name\":\"firmName\",\"type\":\"VARCHAR(200)\"},{\"name\":\"employee_name\",\"type\":\"VARCHAR(200)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"Create LOCAL TEMPORARY TABLE Firm_temp_123 (name VARCHAR(200));\"},{\"_type\":\"relational\",\"sql\":\"Create LOCAL TEMPORARY TABLE Person_temp_123 (fullname VARCHAR(1000), firmName VARCHAR(1000));\"},{\"_type\":\"relational\",\"sql\":\"insert into Firm_temp_123(name) values ('FA'), ('FirmA')\"},{\"_type\":\"relational\",\"sql\":\"insert into Person_temp_123 (fullname, firmName) values ('abc', 'FA'), ('xyz', 'FA')\"},{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"pk_0\\\", \\\"root\\\".name as \\\"firmName\\\", \\\"personTable\\\".fullname as \\\"employee_name\\\" from Person_temp_123 as \\\"personTable\\\" left outer join Firm_temp_123 as \\\"root\\\" on (\\\"root\\\".name = \\\"personTable\\\".firmName)\"}], \"result\" : {\"columns\" : [\"pk_0\",\"firmName\",\"employee_name\"], \"rows\" : [{\"values\": [\"FA\",\"FA\",\"abc\"]},{\"values\": [\"FA\",\"FA\",\"xyz\"]}]}}", result.flush(new RelationalResultToJsonDefaultSerializer(result)));
    }

    @Test
    public void testFreeMarkerConditionalExecutionNode() throws Exception
    {
        String plan = "{\n" +
                "  \"serializer\": {\n" +
                "    \"name\": \"pure\",\n" +
                "    \"version\": \"vX_X_X\"\n" +
                "  },\n" +
                "  \"templateFunctions\": [\n" +
                "    \"<#function renderCollection collection separator><#return collection?join(separator)><\\/#function>\",\n" +
                "    \"<#function collectionSize collection> <#return collection?size> <\\/#function>\"\n" +
                "  ],\n" +
                "  \"rootExecutionNode\": {\n" +
                "    \"_type\": \"relationalBlock\",\n" +
                "    \"resultType\": {\n" +
                "      \"tdsColumns\": [\n" +
                "        {\n" +
                "          \"name\": \"name\",\n" +
                "          \"type\": \"String\",\n" +
                "          \"relationalType\": \"VARCHAR(1000)\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"_type\": \"tds\"\n" +
                "    },\n" +
                "    \"executionNodes\": [\n" +
                "      {\n" +
                "        \"functionParameters\": [\n" +
                "          {\n" +
                "            \"name\": \"name\",\n" +
                "            \"supportsStream\": true,\n" +
                "            \"multiplicity\": {\n" +
                "              \"lowerBound\": 0\n" +
                "            },\n" +
                "            \"class\": \"String\",\n" +
                "            \"_type\": \"var\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"_type\": \"function-parameters-validation\",\n" +
                "        \"resultType\": {\n" +
                "          \"dataType\": \"Boolean\",\n" +
                "          \"_type\": \"dataType\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"varName\": \"inFilterClause_name\",\n" +
                "        \"_type\": \"allocation\",\n" +
                "        \"resultType\": {\n" +
                "          \"dataType\": \"String\",\n" +
                "          \"_type\": \"dataType\"\n" +
                "        },\n" +
                "        \"executionNodes\": [\n" +
                "          {\n" +
                "            \"freeMarkerBooleanExpression\": \"${(instanceOf(name, \\\"Stream\\\") || ((collectionSize(name)) > 50))?c}\",\n" +
                "            \"trueBlock\": {\n" +
                "              \"_type\": \"sequence\",\n" +
                "              \"resultType\": {\n" +
                "                \"dataType\": \"String\",\n" +
                "                \"_type\": \"dataType\"\n" +
                "              },\n" +
                "              \"executionNodes\": [\n" +
                "                {\n" +
                "                  \"inputVarNames\": [\n" +
                "                    \"name\"\n" +
                "                  ],\n" +
                "                  \"tempTableName\": \"tempTableForIn_name\",\n" +
                "                  \"tempTableColumnMetaData\": [\n" +
                "                    {\n" +
                "                      \"column\": {\n" +
                "                        \"label\": \"ColumnForStoringInCollection\",\n" +
                "                        \"dataType\": \"VARCHAR(200)\"\n" +
                "                      }\n" +
                "                    }\n" +
                "                  ],\n" +
                "    \"connection\": {\n" +
                "       \"_type\": \"RelationalDatabaseConnection\",\n" +
                "       \"type\": \"H2\",\n" +
                "       \"authenticationStrategy\" : {\n" +
                "           \"_type\" : \"test\"\n" +
                "       },\n" +
                "       \"datasourceSpecification\" : {\n" +
                "           \"_type\" : \"static\",\n" +
                "           \"databaseName\" : \"testDB\",\n" +
                "           \"host\":\"127.0.0.1\",\n" +
                "           \"port\" : \""+port+"\"\n" +
                "       }\n" +
                "    }," +
                "                  \"_type\": \"createAndPopulateTempTable\",\n" +
                "                  \"resultType\": {\n" +
                "                    \"_type\": \"void\"\n" +
                "                  },\n" +
                "                  \"implementation\": {\n" +
                "                    \"executionClassFullName\": \"_pure.plan.root.n2.n1.trueBlock.n1.CreateAndPopulateTempTable\",\n" +
                "                    \"executionMethodName\": \"getGetterNameForProperty\",\n" +
                "                    \"_type\": \"java\"\n" +
                "                  }\n" +
                "                },\n" +
                "                {\n" +
                "                  \"values\": {\n" +
                "                    \"values\": [\n" +
                "                      \"select \\\"temptableforin_name_0\\\".ColumnForStoringInCollection as ColumnForStoringInCollection from tempTableForIn_name as \\\"temptableforin_name_0\\\"\"\n" +
                "                    ],\n" +
                "                    \"multiplicity\": {\n" +
                "                      \"lowerBound\": 1,\n" +
                "                      \"upperBound\": 1\n" +
                "                    },\n" +
                "                    \"_type\": \"string\"\n" +
                "                  },\n" +
                "                  \"_type\": \"constant\",\n" +
                "                  \"resultType\": {\n" +
                "                    \"dataType\": \"String\",\n" +
                "                    \"_type\": \"dataType\"\n" +
                "                  }\n" +
                "                }\n" +
                "              ]\n" +
                "            },\n" +
                "            \"falseBlock\": {\n" +
                "              \"values\": {\n" +
                "                \"values\": [\n" +
                "                  \"'${renderCollection(name \\\"','\\\")}'\"\n" +
                "                ],\n" +
                "                \"multiplicity\": {\n" +
                "                  \"lowerBound\": 1,\n" +
                "                  \"upperBound\": 1\n" +
                "                },\n" +
                "                \"_type\": \"string\"\n" +
                "              },\n" +
                "              \"_type\": \"constant\",\n" +
                "              \"resultType\": {\n" +
                "                \"dataType\": \"String\",\n" +
                "                \"_type\": \"dataType\"\n" +
                "              }\n" +
                "            },\n" +
                "            \"_type\": \"freeMarkerConditionalExecutionNode\",\n" +
                "            \"resultType\": {\n" +
                "              \"dataType\": \"String\",\n" +
                "              \"_type\": \"dataType\"\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"_type\": \"relationalTdsInstantiation\",\n" +
                "        \"resultType\": {\n" +
                "          \"tdsColumns\": [\n" +
                "            {\n" +
                "              \"name\": \"name\",\n" +
                "              \"type\": \"String\",\n" +
                "              \"relationalType\": \"VARCHAR(1000)\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"_type\": \"tds\"\n" +
                "        },\n" +
                "        \"executionNodes\": [\n" +
                "          {\n" +
                "            \"sqlQuery\": \"select \\\"root\\\".name as \\\"name\\\" from employeeTable as \\\"root\\\" where \\\"root\\\".name in (${inFilterClause_name})\",\n" +
                "            \"resultColumns\": [\n" +
                "              {\n" +
                "                \"label\": \"\\\"name\\\"\",\n" +
                "                \"dataType\": \"VARCHAR(1000)\"\n" +
                "              }\n" +
                "            ],\n" +
                "    \"connection\": {\n" +
                "       \"_type\": \"RelationalDatabaseConnection\",\n" +
                "       \"type\": \"H2\",\n" +
                "       \"authenticationStrategy\" : {\n" +
                "           \"_type\" : \"test\"\n" +
                "       },\n" +
                "       \"datasourceSpecification\" : {\n" +
                "           \"_type\" : \"static\",\n" +
                "           \"databaseName\" : \"testDB\",\n" +
                "           \"host\":\"127.0.0.1\",\n" +
                "           \"port\" : \""+port+"\"\n" +
                "       }\n" +
                "    }," +
                "            \"_type\": \"sql\",\n" +
                "            \"resultType\": {\n" +
                "              \"dataType\": \"meta::pure::metamodel::type::Any\",\n" +
                "              \"_type\": \"dataType\"\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"authDependent\": false,\n" +
                "  \"globalImplementationSupport\": {\n" +
                "    \"classes\": [\n" +
                "      {\n" +
                "        \"package\": \"_pure.plan.root.n2.n1.trueBlock.n1\",\n" +
                "        \"name\": \"CreateAndPopulateTempTable\",\n" +
                "        \"source\": \"package _pure.plan.root.n2.n1.trueBlock.n1;\\n\\nimport org.finos.legend.engine.plan.dependencies.store.relational.IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics;\\nimport org.finos.legend.engine.plan.dependencies.util.Library;\\nimport java.util.Arrays;\\nimport java.util.List;\\nimport java.util.Optional;\\nimport java.util.stream.Collector;\\nimport java.util.stream.Collectors;\\nimport java.util.stream.Stream;\\n\\npublic class CreateAndPopulateTempTable implements IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics {\\n\\n    public String getGetterNameForProperty(String p) {\\n        return p == null ? null : Arrays.asList(\\\"get\\\", Library.toOne(Optional.ofNullable(p).map(Stream::of).orElseGet(Stream::empty).map(Library::toUpperFirstCharacter).collect(Collectors.toList()))).stream().collect(Collectors.joining(\\\"\\\"));\\n    }\\n}\\n\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"_type\": \"java\"\n" +
                "  }\n" +
                "}";

        PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(Relational.build(this.port));

        Map<String, ?> inputWithList = Maps.mutable.with("name", Lists.mutable.with("Alice", "Bob", "Curtis"));
        RelationalResult resultWithList = (RelationalResult) planExecutor.execute(plan, inputWithList);
        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"name\\\" from employeeTable as \\\"root\\\" where \\\"root\\\".name in ('Alice','Bob','Curtis')\"}], \"result\" : {\"columns\" : [\"name\"], \"rows\" : [{\"values\": [\"Alice\"]},{\"values\": [\"Bob\"]},{\"values\": [\"Curtis\"]}]}}", resultWithList.flush(new RelationalResultToJsonDefaultSerializer(resultWithList)));

        Map<String, ?> inputWithListExceedingThreshold = Maps.mutable.with("name", Lists.mutable.with("Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis", "Alice", "Bob", "Curtis"));
        RelationalResult resultWithListExceedingThreshold = (RelationalResult) planExecutor.execute(plan, inputWithListExceedingThreshold);
        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"name\\\" from employeeTable as \\\"root\\\" where \\\"root\\\".name in (select \\\"temptableforin_name_0\\\".ColumnForStoringInCollection as ColumnForStoringInCollection from tempTableForIn_name as \\\"temptableforin_name_0\\\")\"}], \"result\" : {\"columns\" : [\"name\"], \"rows\" : [{\"values\": [\"Alice\"]},{\"values\": [\"Bob\"]},{\"values\": [\"Curtis\"]}]}}", resultWithListExceedingThreshold.flush(new RelationalResultToJsonDefaultSerializer(resultWithListExceedingThreshold)));

        Map<String, ?> streamInput = Maps.mutable.with("name", Lists.mutable.with("Alice", "Bob", "Curtis").stream());
        RelationalResult resultWithStream = (RelationalResult) planExecutor.execute(plan, streamInput);
        Assert.assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"name\",\"type\":\"String\",\"relationalType\":\"VARCHAR(1000)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".name as \\\"name\\\" from employeeTable as \\\"root\\\" where \\\"root\\\".name in (select \\\"temptableforin_name_0\\\".ColumnForStoringInCollection as ColumnForStoringInCollection from tempTableForIn_name as \\\"temptableforin_name_0\\\")\"}], \"result\" : {\"columns\" : [\"name\"], \"rows\" : [{\"values\": [\"Alice\"]},{\"values\": [\"Bob\"]},{\"values\": [\"Curtis\"]}]}}", resultWithStream.flush(new RelationalResultToJsonDefaultSerializer(resultWithStream)));

        String poolInfo = ((Map) ((List) ((Map) ((List) ((Map) ((List) objectMapper.readValue(planExecutor.getPlanExecutorInfo().toJSON(), Map.class).get("storeExecutionInfos")).get(0)).get("databases")).get(0)).get("pools")).get(0)).get("dynamic").toString();
        Assert.assertEquals("{activeConnections=0, idleConnections=1, threadsAwaitingConnection=0, totalConnections=1}", poolInfo);
    }
}
