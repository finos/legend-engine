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

package org.finos.legend.engine.plan.execution.stores.relational.test.write;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.plan.execution.stores.relational.test.semiStructured.AbstractTestSemiStructured;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestExecutionPlanWithWrite extends AlloyTestServer
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Override
    protected void insertTestData(Statement statement) throws SQLException
    {
        statement.execute("Drop table if exists personTable;");
        statement.execute("Create Table personTable(firstName VARCHAR(100) , lastName VARCHAR(100));");
        statement.execute("Drop table if exists personTable2;");
        statement.execute("Create Table personTable2(firstName VARCHAR(100) , lastName VARCHAR(100));");
        statement.execute("Drop table if exists personTable3;");
        statement.execute("Create Table personTable3(firstName VARCHAR(100));");

        statement.execute("insert into personTable (firstName, lastName) values ('David', 'harte');");
        statement.execute("insert into personTable (firstName, lastName) values ('Pierre', 'debelen');");
        statement.execute("insert into personTable (firstName, lastName) values ('Mohammed', 'ibrahim');");
    }

//    public insertSingle(Statement statement)

    @Test
    @ToFix
    public void testSimpleWrite() throws  IOException
    {
        String plan = readContent(modelResourcePath());
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        String result = executePlanWrite(executionPlan, Maps.mutable.empty());
        Assert.assertTrue("3".equals(result), () -> String.format("Results do not match. Expected: 3, Actual: %s",result));
    }

    @Test
    public void testSimpleWriteMultiColumn() throws  IOException
    {
        String plan = readContent(modelResourcePath2());
        SingleExecutionPlan executionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
        String result = executePlanWrite(executionPlan, Maps.mutable.empty());
        Assert.assertTrue("3".equals(result), () -> String.format("Results do not match. Expected: 3, Actual: %s",result));

    }

    protected String executePlanWrite(SingleExecutionPlan plan, Map<String, ?> params)
    {
        ConstantResult result = (ConstantResult) planExecutor.execute(plan, params, null);
        return result.getValue().toString();
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/write/writePersonExecutionPlan.json";
    }


    public String modelResourcePath2()
    {
        return "/org/finos/legend/engine/plan/execution/stores/relational/test/write/writeMultiColumnPlan.json";
    }

    private String readContent(String resourcePath)
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(Objects.requireNonNull(AbstractTestSemiStructured.class.getResourceAsStream(resourcePath)))))
        {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
