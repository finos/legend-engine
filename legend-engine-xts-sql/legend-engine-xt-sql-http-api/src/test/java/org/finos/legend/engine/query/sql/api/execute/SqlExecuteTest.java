// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api.execute;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.protocol.sql.schema.metamodel.Enum;
import org.finos.legend.engine.protocol.sql.schema.metamodel.EnumSchemaColumn;
import org.finos.legend.engine.protocol.sql.schema.metamodel.Parameter;
import org.finos.legend.engine.protocol.sql.schema.metamodel.PrimitiveSchemaColumn;
import org.finos.legend.engine.protocol.sql.schema.metamodel.PrimitiveType;
import org.finos.legend.engine.protocol.sql.schema.metamodel.Schema;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.query.sql.api.CatchAllExceptionMapper;
import org.finos.legend.engine.query.sql.api.MockPac4jFeature;
import org.finos.legend.engine.query.sql.api.TestSQLSourceProvider;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import java.util.List;
import java.util.ServiceLoader;

public class SqlExecuteTest
{
    @ClassRule
    public static final ResourceTestRule resources;
    private static final ObjectMapper OM = new ObjectMapper();
    private static final SQLGrammarParser PARSER = SQLGrammarParser.newInstance();

    static
    {
        Pair<PureModel, ResourceTestRule> pureModelAndResources = getPureModelResourceTestRulePair();

        resources =  pureModelAndResources.getTwo();
    }

    public static Pair<PureModel, ResourceTestRule> getPureModelResourceTestRulePair()
    {
        DeploymentMode deploymentMode = DeploymentMode.TEST;
        ModelManager modelManager = new ModelManager(deploymentMode);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();

        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        TestSQLSourceProvider testSQLSourceProvider = new TestSQLSourceProvider();
        SqlExecute sqlExecute = new SqlExecute(modelManager, executor, (pm) -> PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())), FastList.newListWith(testSQLSourceProvider), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));

        PureModel pureModel = modelManager.loadModel(testSQLSourceProvider.getPureModelContextData(), PureClientVersions.production, Identity.getAnonymousIdentity(), "");
        ResourceTestRule resources = ResourceTestRule.builder()
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addResource(sqlExecute)
                .addResource(new MockPac4jFeature())
                .addResource(new CatchAllExceptionMapper())
                .bootstrapLogging(false)
                .build();
        return Tuples.pair(pureModel,resources);
    }

    private Query parse(String sql)
    {
        return (Query) PARSER.parseStatement(sql);
    }

    private void lambdaTest(String api, Entity<?> entity, String expected) throws JsonProcessingException
    {
        String lambda = resources.target("sql/v1/execution/" + api)
                .request()
                .post(entity).readEntity(String.class);

        Lambda actual = new ObjectMapper().readValue(lambda, Lambda.class);
        String actualGrammar = actual.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build());

        Assert.assertEquals(expected, actualGrammar);
    }

    private void allLambdaTests(String sql, List<Object> arguments, String expected) throws JsonProcessingException
    {
        lambdaTest("lambda", Entity.json(new SQLQueryInput(null, sql, arguments)), expected);
        lambdaTest("lambda", Entity.json(new SQLQueryInput(parse(sql), null, arguments)), expected);
        lambdaTest("generateLambdaString", Entity.text(sql), expected);
        lambdaTest("generateLambda", Entity.json(parse(sql)), expected);
    }

    @Test
    public void testLambda() throws JsonProcessingException
    {
        String expectedCode = "{names: String[*], _1: String[1]|demo::employee.all()->filter(\n" +
                "  p: demo::employee[1]|$names->isEmpty() ||\n" +
                "    $p.name->in(\n" +
                "    $names\n" +
                "  )\n" +
                ")->project(\n" +
                "  [\n" +
                "    x: demo::employee[1]|$x.id,\n" +
                "    x: demo::employee[1]|$x.name,\n" +
                "    x: demo::employee[1]|$x.type\n" +
                "  ],\n" +
                "  [\n" +
                "    'Id',\n" +
                "    'Name',\n" +
                "    'Employee Type'\n" +
                "  ]\n" +
                ")->filter(\n" +
                "  row: meta::pure::tds::TDSRow[1]|$row.getString('Name') ==\n" +
                "    $_1\n" +
                ")->restrict('Name')}";

        allLambdaTests("SELECT Name FROM service('/personServiceForNames') where Name = ?", FastList.newList(), expectedCode);
    }

    private void executeTest(String api, Entity<?> entity, TDSExecuteResult expected) throws JsonProcessingException
    {
        String results = resources.target("sql/v1/execution/" + api)
                .request()
                .post(entity).readEntity(String.class);

        Assert.assertEquals(expected, OM.readValue(results, TDSExecuteResult.class));
    }

    private void allExecuteTests(String sql, List<Object> arguments, TDSExecuteResult expected) throws JsonProcessingException
    {
        allExecuteTests(sql, arguments, expected, false);
    }

    private void allExecuteTests(List<String> sqls, List<Object> arguments, TDSExecuteResult expected) throws JsonProcessingException
    {
        allExecuteTests(sqls, arguments, expected, false);
    }


    private void allExecuteTests(String sql, List<Object> arguments, TDSExecuteResult expected, boolean excludeDeprecated) throws JsonProcessingException
    {
        allExecuteTests(FastList.newListWith(sql), arguments, expected, excludeDeprecated);
    }

    private void allExecuteTests(List<String> sqls, List<Object> arguments, TDSExecuteResult expected, boolean excludeDeprecated) throws JsonProcessingException
    {
        for (String sql : sqls)
        {
            executeTest("execute", Entity.json(new SQLQueryInput(null, sql, arguments)), expected);
            executeTest("execute", Entity.json(new SQLQueryInput(parse(sql), null, arguments)), expected);

            if (!excludeDeprecated)
            {
                executeTest("executeQueryString", Entity.text(sql), expected);
                executeTest("executeQuery", Entity.json(parse(sql)), expected);
            }
        }
    }

    @Test
    public void testExecuteWithParameters() throws JsonProcessingException
    {
        allExecuteTests(FastList.newListWith(
                "SELECT Name FROM service('/personServiceForNames') ORDER BY Name",
                        "SELECT Name FROM service('/personServiceForNamesRelation') ORDER BY Name"), FastList.newList(), TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .addRow(FastList.newListWith("Bob"))
                .addRow(FastList.newListWith("Curtis"))
                .addRow(FastList.newListWith("Danielle"))
                .build());

        allExecuteTests(FastList.newListWith(
                "SELECT Name FROM service('/personServiceForNames', names => ['Alice', 'Danielle']) ORDER BY Name",
                "SELECT Name FROM service('/personServiceForNamesRelation', names => ['Alice', 'Danielle']) ORDER BY Name"
        ), FastList.newList(), TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .addRow(FastList.newListWith("Danielle"))
                .build());
    }

    @Test
    public void testExecuteWithDateParams() throws JsonProcessingException
    {
        allExecuteTests(FastList.newListWith(
                "SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24')",
                "SELECT Name FROM service('/personServiceForStartDateRelation/{date}', date =>'2023-08-24')"
        ), FastList.newList(), TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .build());
    }

    @Test
    public void testExecuteWithEnumParams() throws JsonProcessingException
    {
        allExecuteTests(FastList.newListWith(
                "SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24', type => 'Type1')",
                "SELECT Name FROM service('/personServiceForStartDateRelation/{date}', date =>'2023-08-24', type => 'Type1')"
        ), FastList.newList(), TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .build());
    }

    @Test
    public void testExecuteWithExpressionParams() throws JsonProcessingException
    {
        allExecuteTests(FastList.newListWith(
                "SELECT Name FROM service('/personServiceForStartDate/{date}', date => cast('2023-08-24' as DATE))",
                "SELECT Name FROM service('/personServiceForStartDateRelation/{date}', date => cast('2023-08-24' as DATE))"
        ), FastList.newList(), TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .build());
    }

    @Test
    public void testExecuteMultiUnionNoAliases() throws JsonProcessingException
    {
        //this is to test the query realiasing, can be moved to testTranspile one realiser moved to pure code

        allExecuteTests("SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24') " +
                "UNION SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24') " +
                "UNION SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24') " +
                "UNION SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24')", FastList.newList(), TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .addRow(FastList.newListWith("Alice"))
                .addRow(FastList.newListWith("Alice"))
                .addRow(FastList.newListWith("Alice"))
                .build());
    }

    @Test
    public void testExecuteWithPositionalParameters() throws JsonProcessingException
    {
        allExecuteTests("SELECT Name FROM service('/personServiceForNames') WHERE Name = ? ORDER BY Name", FastList.newListWith("Alice"), TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .build(), true);
    }

    @Test
    public void testExecuteWithCSVFormat()
    {
        String results = resources.target("sql/v1/execution/executeQueryString")
                .queryParam("serializationFormat", SerializationFormat.CSV)
                .request()
                .post(Entity.text("SELECT Name FROM service('/personServiceForNames') ORDER BY Name")).readEntity(String.class);

        Assert.assertEquals("Name\r\nAlice\r\nBob\r\nCurtis\r\nDanielle\r\n", results);
    }

    private void allSchemaTests(List<String> sqls, List<Object> arguments, Schema expected) throws JsonProcessingException
    {
        for (String sql: sqls)
        {
            schemaTest("schema", Entity.json(new SQLQueryInput(null, sql, arguments)), expected);
            schemaTest("schema", Entity.json(new SQLQueryInput(parse(sql), null, arguments)), expected);
            schemaTest("getSchemaFromQueryString", Entity.text(sql), expected);
            schemaTest("getSchemaFromQuery", Entity.json(parse(sql)), expected);
        }
    }

    private void schemaTest(String api, Entity<?> entity, Schema expected) throws JsonProcessingException
    {
        String schema = resources.target("sql/v1/execution/" + api)
                .request()
                .post(entity).readEntity(String.class);

        Assert.assertEquals(new ObjectMapper().writeValueAsString(expected), schema);
    }

    @Test
    public void testSchema() throws JsonProcessingException
    {
        Schema schema = new Schema();
        schema.columns = FastList.newListWith(
                primitiveColumn("Id", PrimitiveType.Integer),
                primitiveColumn("Name", PrimitiveType.String),
                enumColumn("Employee Type", "demo::employeeType")
        );

        schema.enums = FastList.newListWith(
                enumValue("demo::employeeType", "Type1", "Type2")
        );

        allSchemaTests(FastList.newListWith(
                "SELECT * FROM service.\"/testService\"",
                "SELECT * FROM service.\"/testServiceRelation\""
        ), FastList.newList(), schema);
    }

    @Test
    public void testSchemaParams() throws JsonProcessingException
    {
        Schema schema = new Schema();
        schema.columns = FastList.newListWith(
                primitiveColumn("Col", PrimitiveType.Integer)
        );

        schema.parameters = FastList.newListWith(parameter("_1", PrimitiveType.Integer));

        allSchemaTests(FastList.newListWith("SELECT 1 + ? AS \"Col\" FROM service.\"/testService\""), FastList.newList(), schema);
    }

    @Test
    public void testSchemaParamsProvided() throws JsonProcessingException
    {
        Schema schema = new Schema();
        schema.columns = FastList.newListWith(
                primitiveColumn("Col", PrimitiveType.Integer)
        );

        schema.parameters = FastList.newListWith(parameter("_1", PrimitiveType.Integer));

        allSchemaTests(FastList.newListWith("SELECT 1 + ? AS \"Col\" FROM service.\"/testService\""), FastList.newListWith(1), schema);
    }

    @Test
    public void testSchemaDuplicateSources() throws JsonProcessingException
    {
        Schema schema = new Schema();
        schema.columns = FastList.newListWith(
                primitiveColumn("Id", PrimitiveType.Integer)
        );

        allSchemaTests(FastList.newListWith(
                "SELECT Id FROM service.\"/testService\" UNION SELECT Id FROM service.\"/testService\"",
                "SELECT Id FROM service.\"/testServiceRelation\" UNION SELECT Id FROM service.\"/testServiceRelation\""
        ), FastList.newList(), schema);
    }

    private static PrimitiveSchemaColumn primitiveColumn(String name, PrimitiveType type)
    {
        PrimitiveSchemaColumn c = new PrimitiveSchemaColumn();
        c.name = name;
        c.type = type;

        return c;
    }

    private static EnumSchemaColumn enumColumn(String name, String type)
    {
        EnumSchemaColumn c = new EnumSchemaColumn();
        c.name = name;
        c.type = type;;

        return c;
    }

    private static Enum enumValue(String type, String... values)
    {
        Enum e = new Enum();
        e.type = type;
        e.values = FastList.newListWith(values);

        return e;
    }

    private static Parameter parameter(String name, PrimitiveType type)
    {
        Parameter param = new Parameter();
        param.name = name;
        param.type = type;

        return param;
    }
}
