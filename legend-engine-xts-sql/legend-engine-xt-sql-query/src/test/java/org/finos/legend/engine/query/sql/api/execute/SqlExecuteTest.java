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
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.sql.schema.metamodel.Enum;
import org.finos.legend.engine.protocol.sql.schema.metamodel.EnumSchemaColumn;
import org.finos.legend.engine.protocol.sql.schema.metamodel.PrimitiveSchemaColumn;
import org.finos.legend.engine.protocol.sql.schema.metamodel.PrimitiveType;
import org.finos.legend.engine.protocol.sql.schema.metamodel.Schema;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.query.sql.api.CatchAllExceptionMapper;
import org.finos.legend.engine.query.sql.api.MockPac4jFeature;
import org.finos.legend.engine.query.sql.api.TestSQLSourceProvider;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import java.util.ServiceLoader;

public class SqlExecuteTest
{
    @ClassRule
    public static final ResourceTestRule resources;
    private static final PureModel pureModel;
    private static final ObjectMapper OM = new ObjectMapper();

    static
    {
        Pair<PureModel, ResourceTestRule> pureModelAndResources = getPureModelResourceTestRulePair();

        pureModel = pureModelAndResources.getOne();
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

        PureModel pureModel = modelManager.loadModel(testSQLSourceProvider.getPureModelContextData(), PureClientVersions.production, null, "");
        ResourceTestRule resources = ResourceTestRule.builder()
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addResource(sqlExecute)
                .addResource(new MockPac4jFeature())
                .addResource(new CatchAllExceptionMapper())
                .bootstrapLogging(false)
                .build();
        return Tuples.pair(pureModel,resources);
    }

    @Test
    public void testLambda() throws JsonProcessingException
    {
        String lambda = resources.target("sql/v1/execution/generateLambdaString")
                .request()
                .post(Entity.text("SELECT Name FROM service('/personServiceForNames')")).readEntity(String.class);

        String expectedCode = "names: String[*]|demo::employee.all()->filter(\n" +
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
                ")->restrict('Name')";

        Lambda actual = new ObjectMapper().readValue(lambda, Lambda.class);
        String actualGrammar = actual.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build());

        Assert.assertEquals(expectedCode, actualGrammar);
    }

    @Test
    public void testExecuteWithParameters() throws JsonProcessingException
    {
        String all = resources.target("sql/v1/execution/executeQueryString")
                .request()
                .post(Entity.text("SELECT Name FROM service('/personServiceForNames') ORDER BY Name")).readEntity(String.class);

        String filtered = resources.target("sql/v1/execution/executeQueryString")
                .request()
                .post(Entity.text("SELECT Name FROM service('/personServiceForNames', names => ['Alice', 'Danielle']) ORDER BY Name")).readEntity(String.class);

        TDSExecuteResult allExpected = TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .addRow(FastList.newListWith("Bob"))
                .addRow(FastList.newListWith("Curtis"))
                .addRow(FastList.newListWith("Danielle"))
                .build();

        TDSExecuteResult filteredExpected = TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .addRow(FastList.newListWith("Danielle"))
                .build();

        Assert.assertEquals(allExpected, OM.readValue(all, TDSExecuteResult.class));
        Assert.assertEquals(filteredExpected, OM.readValue(filtered, TDSExecuteResult.class));
    }

    @Test
    public void testExecuteWithDateParams() throws JsonProcessingException
    {
        String all = resources.target("sql/v1/execution/executeQueryString")
                .request()
                .post(Entity.text("SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24')")).readEntity(String.class);

        TDSExecuteResult allExpected = TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .build();

        Assert.assertEquals(allExpected, OM.readValue(all, TDSExecuteResult.class));
    }

    @Test
    public void testExecuteWithEnumParams() throws JsonProcessingException
    {
        String all = resources.target("sql/v1/execution/executeQueryString")
                .request()
                .post(Entity.text("SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24', type => 'Type1')")).readEntity(String.class);

        TDSExecuteResult allExpected = TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .build();

        Assert.assertEquals(allExpected, OM.readValue(all, TDSExecuteResult.class));
    }

    @Test
    public void testExecuteWithExpressionParams() throws JsonProcessingException
    {
        String all = resources.target("sql/v1/execution/executeQueryString")
                .request()
                .post(Entity.text("SELECT Name FROM service('/personServiceForStartDate/{date}', date => cast('2023-08-24' as DATE))")).readEntity(String.class);

        TDSExecuteResult allExpected = TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .build();

        Assert.assertEquals(allExpected, OM.readValue(all, TDSExecuteResult.class));
    }

    @Test
    public void testExecuteMultiUnionNoAliases() throws JsonProcessingException
    {
        //this is to test the query realiasing, can be moved to testTranspile one realiser moved to pure code
        String all = resources.target("sql/v1/execution/executeQueryString")
                .request()
                .post(Entity.text("SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24') " +
                        "UNION SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24') " +
                        "UNION SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24') " +
                        "UNION SELECT Name FROM service('/personServiceForStartDate/{date}', date =>'2023-08-24')")).readEntity(String.class);

        TDSExecuteResult allExpected = TDSExecuteResult.builder(FastList.newListWith("Name"))
                .addRow(FastList.newListWith("Alice"))
                .addRow(FastList.newListWith("Alice"))
                .addRow(FastList.newListWith("Alice"))
                .addRow(FastList.newListWith("Alice"))
                .build();

        Assert.assertEquals(allExpected, OM.readValue(all, TDSExecuteResult.class));
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

    @Test
    public void getSchemaForQueryWithDuplicateSources() throws JsonProcessingException
    {
        String actualSchema = resources.target("sql/v1/execution/getSchemaFromQueryString")
                .request()
                .post(Entity.text("SELECT Id FROM service.\"/testService\" UNION SELECT Id FROM service.\"/testService\"")).readEntity(String.class);

        Schema schema = new Schema();
        schema.columns = FastList.newListWith(
                primitiveColumn("Id", PrimitiveType.Integer)
        );

        Assert.assertEquals(new ObjectMapper().writeValueAsString(schema), actualSchema);
    }


    @Test
    public void getSchemaFromQueryString() throws JsonProcessingException
    {
        String actualSchema = resources.target("sql/v1/execution/getSchemaFromQueryString")
                .request()
                .post(Entity.text("SELECT * FROM service.\"/testService\"")).readEntity(String.class);

        Schema schema = new Schema();
        schema.columns = FastList.newListWith(
                primitiveColumn("Id", PrimitiveType.Integer),
                primitiveColumn("Name", PrimitiveType.String),
                enumColumn("Employee Type", "demo::employeeType")
        );

        schema.enums = FastList.newListWith(
                enumValue("demo::employeeType", "Type1", "Type2")
        );

        Assert.assertEquals(new ObjectMapper().writeValueAsString(schema), actualSchema);
    }

    @Test
    public void getSchemaFromQuery() throws JsonProcessingException
    {
        String actualSchema = resources.target("sql/v1/execution/getSchemaFromQuery")
                .request()
                .post(Entity.json("{ \"_type\": \"query\", \"orderBy\": [], \"queryBody\": { \"_type\": \"querySpecification\", \"from\": [ { \"_type\": \"table\", \"name\": { \"parts\": [ \"service\", \"/testService\" ] } } ], \"groupBy\": [], \"orderBy\": [], \"select\": { \"_type\": \"select\", \"distinct\": false, \"selectItems\": [ { \"_type\": \"allColumns\" } ] } } }")).readEntity(String.class);

        Schema schema = new Schema();
        schema.columns = FastList.newListWith(
                primitiveColumn("Id", PrimitiveType.Integer),
                primitiveColumn("Name", PrimitiveType.String),
                enumColumn("Employee Type", "demo::employeeType")
        );

        schema.enums = FastList.newListWith(
                enumValue("demo::employeeType", "Type1", "Type2")
        );

        Assert.assertEquals(new ObjectMapper().writeValueAsString(schema), actualSchema);
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

}
