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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

public class AbstractTestPlanExecution
{
    protected static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public static void registerDriver(String driver)
    {
        Connection conn = null;
        try
        {
            Class.forName(driver);

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

            found.forEach((Procedure<Driver>) c ->
            {
                try
                {
                    DriverManager.deregisterDriver(c);
                }
                catch (Exception ignore)
                {
                }
            });
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void exec(String sql, Connection connection) throws Exception
    {
        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);
        statement.close();
    }

    public void closeConnection(Connection conn) throws SQLException
    {
        if (conn != null)
        {
            conn.close();
        }
    }

    public String buildPlan(String connectionSpecJSON)
    {
        String template = getTemplatedPlanJSON();
        return template.replaceAll("____CONNECTION_SPEC_FRAGMENT____", connectionSpecJSON);
    }

    private String getTemplatedPlanJSON()
    {
        // executionPlan(|meta::relational::tests::mapping::enumeration::model::domain::Employee.all(), meta::relational::tests::mapping::enumeration::model::mapping::employeeTestMapping, meta::relational::tests::mapping::enumeration::enumTestRuntime())->meta::alloy::protocol::vX_X_X::transformation::fromPureGraph::executionPlan::transformPlan()->toJSON([], 1000, config(false, false, true, true));
        return "{\n" +
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
                "____CONNECTION_SPEC_FRAGMENT____" +
                "," +
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
    }
}