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


package org.finos.legend.engine.plan.execution.stores.relational.test;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutionContext;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.graphFetch.AdaptiveGraphBatchStats;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToPureFormatSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import static org.finos.legend.pure.generated.core_relational_relational_extensions_extension.Root_meta_relational_extension_relationalExtensions__Extension_MANY_;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestPlanExecutionWithAdaptiveBatching extends AlloyTestServer
{

    private static final String LOGICAL_MODEL = "###Pure\n" +
            "Class test::Person\n" +
            "{\n" +
            "  fullName: String[1];\n" +
            "}\n" +
            "\n" +
            "Class test::Firm\n" +
            "{\n" +
            "  name: String[1];\n" +
            "}\n" +
            "\n" +
            "Class test::Address\n" +
            "{\n" +
            "  name: String[1];\n" +
            "}\n" +
            "\n" +
            "Association test::Person_Firm\n" +
            "{\n" +
            "  employees: test::Person[*];\n" +
            "  firm: test::Firm[0..1];\n" +
            "}\n" +
            "\n" +
            "Association test::Person_Address\n" +
            "{\n" +
            "  persons: test::Person[*];\n" +
            "  address: test::Address[0..1];  \n" +
            "}\n" +
            "\n" +
            "Association test::Firm_Address\n" +
            "{\n" +
            "  firms: test::Firm[*];\n" +
            "  address: test::Address[0..1];  \n" +
            "}\n\n\n";

    private static final String STORE_MODEL = "###Relational\n" +
            "Database test::DB1\n" +
            "(\n" +
            "  Table personTable (\n" +
            "    fullName VARCHAR(100) PRIMARY KEY,\n" +
            "    firmName VARCHAR(100),\n" +
            "    addressName VARCHAR(100)\n" +
            "  )\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "Database test::DB2\n" +
            "(\n" +
            "  Table firmTable (\n" +
            "    name VARCHAR(100) PRIMARY KEY,\n" +
            "    addressName VARCHAR(100)\n" +
            "  )\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "Database test::DB3\n" +
            "(\n" +
            "  Table addressTable (\n" +
            "    name VARCHAR(100) PRIMARY KEY\n" +
            "  )\n" +
            ")\n\n\n";

    private static final String MAPPING = "###Mapping\n" +
            "Mapping test::Map\n" +
            "(\n" +
            "  test::Person : Relational {\n" +
            "    +firmName : String[0..1] : [test::DB1]personTable.firmName,\n" +
            "    +addressName : String[0..1] : [test::DB1]personTable.addressName, \n" +
            "    fullName: [test::DB1]personTable.fullName\n" +
            "  }\n" +
            "\n" +
            "  test::Firm : Relational {\n" +
            "    +addressName : String[0..1] : [test::DB2]firmTable.addressName, \n" +
            "    name: [test::DB2]firmTable.name\n" +
            "  }\n" +
            "\n" +
            "  test::Address : Relational {\n" +
            "    name: [test::DB3]addressTable.name\n" +
            "  }\n" +
            "\n" +
            "  test::Person_Firm : XStore {\n" +
            "    employees[test_Firm, test_Person]: $this.name == $that.firmName,\n" +
            "    firm[test_Person, test_Firm]: $this.firmName == $that.name\n" +
            "  }\n" +
            "\n" +
            "  test::Person_Address : XStore {\n" +
            "    persons[test_Address, test_Person]: $this.name == $that.addressName,\n" +
            "    address[test_Person, test_Address]: $this.addressName == $that.name\n" +
            "  }\n" +
            "\n" +
            "  test::Firm_Address : XStore {\n" +
            "    firms[test_Address, test_Firm]: $this.name == $that.addressName,\n" +
            "    address[test_Firm, test_Address]: $this.addressName == $that.name\n" +
            "  }\n" +
            ")\n\n\n";

    private static final String RUNTIME = "###Runtime\n" +
            "Runtime test::Runtime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    test::Map\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    test::DB1:\n" +
            "    [\n" +
            "      c1: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: H2;\n" +
            "          specification: LocalH2 {};\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ],\n" +
            "    test::DB2:\n" +
            "    [\n" +
            "      c2: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: H2;\n" +
            "          specification: LocalH2 {};\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ],\n" +
            "    test::DB3:\n" +
            "    [\n" +
            "      c3: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: H2;\n" +
            "          specification: LocalH2 {};\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n";


    // Tests the return value when previous batch stats are null
    @Test
    public void testAdaptiveBatchingWithNoStats() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "}";

        SingleExecutionPlan executionPlan = buildPlanForFetchFunction(fetchFunction);
        ExecutionState fakeExecutionState = new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))));
        RelationalExecutionNodeExecutor relationalExecutionNodeExecutor = new RelationalExecutionNodeExecutor(fakeExecutionState, Lists.mutable.empty());
        Assert.assertEquals(1, relationalExecutionNodeExecutor.getAdaptiveBatchSize());
    }

    // Tests the increase in batch size when far off from the soft limit
    @Test
    public void testIncreaseInBatchSizeFarFromSoftLimit() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "}";

        SingleExecutionPlan executionPlan = buildPlanForFetchFunction(fetchFunction);
        ExecutionState fakeExecutionState = new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))));
        fakeExecutionState.adaptiveGraphBatchStats = new AdaptiveGraphBatchStats(121, 2);

        RelationalExecutionNodeExecutor relationalExecutionNodeExecutor = new RelationalExecutionNodeExecutor(fakeExecutionState, Lists.mutable.empty());

        Assert.assertEquals(3, relationalExecutionNodeExecutor.getAdaptiveBatchSize());
        Assert.assertEquals(5, relationalExecutionNodeExecutor.getAdaptiveBatchSize());
        Assert.assertEquals(9, relationalExecutionNodeExecutor.getAdaptiveBatchSize());
        Assert.assertEquals(17, relationalExecutionNodeExecutor.getAdaptiveBatchSize());
    }


    // Tests the increase in batch size when near the soft limit
    @Test
    public void testIncreaseInBatchSizeNearSoftLimit() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "}";

        SingleExecutionPlan executionPlan = buildPlanForFetchFunction(fetchFunction);
        ExecutionState fakeExecutionState = new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))), true, 200, PlanExecutor.DEFAULT_GRAPH_FETCH_SOFT_MEMORY_LIMIT_PERCENTAGE, true);
        fakeExecutionState.adaptiveGraphBatchStats = new AdaptiveGraphBatchStats(49, 1);

        RelationalExecutionNodeExecutor relationalExecutionNodeExecutor = new RelationalExecutionNodeExecutor(fakeExecutionState, Lists.mutable.empty());

        Assert.assertEquals(1, relationalExecutionNodeExecutor.getAdaptiveBatchSize());
    }

    // Tests exponential decrease in batch size
    @Test
    public void testExponentialDecreaseInBatchSize() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "}";

        SingleExecutionPlan executionPlan = buildPlanForFetchFunction(fetchFunction);
        ExecutionState fakeExecutionState = new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))), true, 200, PlanExecutor.DEFAULT_GRAPH_FETCH_SOFT_MEMORY_LIMIT_PERCENTAGE, true);
        fakeExecutionState.adaptiveGraphBatchStats = new AdaptiveGraphBatchStats(400, 4);

        RelationalExecutionNodeExecutor relationalExecutionNodeExecutor = new RelationalExecutionNodeExecutor(fakeExecutionState, Lists.mutable.empty());

        Assert.assertEquals(2, relationalExecutionNodeExecutor.getAdaptiveBatchSize());
    }

    // Tests ideal decrease in batch size
    @Test
    public void testIdealDecreaseInBatchSize() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "}";

        SingleExecutionPlan executionPlan = buildPlanForFetchFunction(fetchFunction);
        ExecutionState fakeExecutionState = new ExecutionState(Maps.mutable.empty(), Lists.mutable.withAll(executionPlan.templateFunctions), Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(serverPort))), true, 200, PlanExecutor.DEFAULT_GRAPH_FETCH_SOFT_MEMORY_LIMIT_PERCENTAGE, true);
        fakeExecutionState.adaptiveGraphBatchStats = new AdaptiveGraphBatchStats(120, 4);

        RelationalExecutionNodeExecutor relationalExecutionNodeExecutor = new RelationalExecutionNodeExecutor(fakeExecutionState, Lists.mutable.empty());

        Assert.assertEquals(3, relationalExecutionNodeExecutor.getAdaptiveBatchSize());
    }

    // Tests if adaptive batching prevents crossing the hard limit
    @Test
    public void testAdaptiveBatchingWithMemoryLimits() throws JavaCompileException
    {
        String fetchFunctionWithBatchSize = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#, 10)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#)\n" +
                "}";

        String fetchFunctionWithoutBatchSize = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#)\n" +
                "}";

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\"}}" +
                "]";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunctionWithBatchSize);
        PlanExecutionContext context = new PlanExecutionContext(plan);

        Exception e = assertThrows(RuntimeException.class, () ->
        {
            Assert.assertEquals(expectedRes, executePlan(plan, context, 900));
        });
        Assert.assertEquals("Maximum memory reached when processing the graphFetch. Try reducing batch size of graphFetch fetch operation.", e.getMessage());

        SingleExecutionPlan planWithoutBatchSize = buildPlanForFetchFunction(fetchFunctionWithoutBatchSize);
        PlanExecutionContext contextWithoutBatchSize = new PlanExecutionContext(planWithoutBatchSize);

        Assert.assertEquals(expectedRes, executePlan(planWithoutBatchSize, contextWithoutBatchSize, 500));
    }

    // Tests the result with and without adaptive batching
    @Test
    public void testAdaptiveBatchingResult() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name,\n" +
                "          address {\n" +
                "            name\n" +
                "          }\n" +
                "        },\n" +
                "        address {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name,\n" +
                "          address {\n" +
                "            name\n" +
                "          }\n" +
                "        },\n" +
                "        address {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#)\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction);
        PlanExecutionContext context = new PlanExecutionContext(plan);

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\",\"address\":{\"name\":\"A3\"}},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}" +
                "]";

        Assert.assertEquals(expectedRes, executePlan(plan, context, PlanExecutor.DEFAULT_GRAPH_FETCH_BATCH_MEMORY_LIMIT));
    }

    private String executePlan(SingleExecutionPlan plan, PlanExecutionContext context, long graphFetchBatchMemoryLimit)
    {
        planExecutor.setGraphFetchBatchMemoryLimit(graphFetchBatchMemoryLimit);
        JsonStreamingResult result = (JsonStreamingResult) planExecutor.execute(plan, Collections.emptyMap(), null, context);
        return result.flush(new JsonStreamToPureFormatSerializer(result));
    }

    private SingleExecutionPlan buildPlanForFetchFunction(String fetchFunction)
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + fetchFunction);
        PureModel pureModel = Compiler.compile(contextData, null, null);

        List<ValueSpecification> fetchFunctionExpressions = contextData.getElementsOfType(Function.class).get(0).body;

        return PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(fetchFunctionExpressions, Collections.emptyList(), new CompileContext.Builder(pureModel).build()),
                pureModel.getMapping("test::Map"),
                pureModel.getRuntime("test::Runtime"),
                null,
                pureModel,
                "vX_X_X",
                PlanPlatform.JAVA,
                null,
                Root_meta_relational_extension_relationalExtensions__Extension_MANY_(pureModel.getExecutionSupport()),
                LegendPlanTransformers.transformers
        );
    }

    protected void insertTestData(Statement s) throws SQLException
    {
        s.execute("Create Schema default;");
        s.execute("Drop table if exists personTable;");
        s.execute("Create Table personTable(fullName VARCHAR(100) NOT NULL,firmName VARCHAR(100) NULL,addressName VARCHAR(100) NULL, PRIMARY KEY(fullName));");
        s.execute("Drop table if exists firmTable;");
        s.execute("Create Table firmTable(name VARCHAR(100) NOT NULL,addressName VARCHAR(100) NULL, PRIMARY KEY(name));");
        s.execute("Drop table if exists addressTable;");
        s.execute("Create Table addressTable(name VARCHAR(100) NOT NULL, PRIMARY KEY(name));");
        s.execute("insert into personTable (fullName,firmName,addressName) values ('P1','F1','A1');");
        s.execute("insert into personTable (fullName,firmName,addressName) values ('P2','F2','A2');");
        s.execute("insert into personTable (fullName,firmName,addressName) values ('P3',null,null);");
        s.execute("insert into personTable (fullName,firmName,addressName) values ('P4',null,'A3');");
        s.execute("insert into personTable (fullName,firmName,addressName) values ('P5','F1','A1');");
        s.execute("insert into firmTable (name,addressName) values ('F1','A4');");
        s.execute("insert into firmTable (name,addressName) values ('F2','A3');");
        s.execute("insert into firmTable (name,addressName) values ('F3','A3');");
        s.execute("insert into firmTable (name,addressName) values ('F4',null);");
        s.execute("insert into addressTable (name) values ('A1');");
        s.execute("insert into addressTable (name) values ('A2');");
        s.execute("insert into addressTable (name) values ('A3');");
        s.execute("insert into addressTable (name) values ('A4');");
        s.execute("insert into addressTable (name) values ('A5');");
    }
}
