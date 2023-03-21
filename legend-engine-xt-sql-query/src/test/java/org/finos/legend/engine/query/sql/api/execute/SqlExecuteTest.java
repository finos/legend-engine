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

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.query.sql.api.MockPac4jFeature;
import org.finos.legend.engine.query.sql.model.Schema;
import org.finos.legend.engine.query.sql.model.SchemaColumn;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.security.PrivilegedActionException;
import java.util.ServiceLoader;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class SqlExecuteTest
{
    @ClassRule
    public static final ResourceTestRule resources;


    static
    {
        DeploymentMode deploymentMode = DeploymentMode.TEST;
        ModelManager modelManager = new ModelManager(deploymentMode);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithConfigurations();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        SqlExecute sqlExecute = new SqlExecute(modelManager, executor, (pm) -> generatorExtensions.flatCollect(g -> g.getExtraExtensions(pm)), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers), null, deploymentMode);

        SqlExecute resource = spy(sqlExecute);

        try
        {
            String pureProject1 = ResourceHelpers.resourceFilePath("proj-1.pure");
            String pureProject1Contents = FileUtils.readFileToString(Paths.get(pureProject1).toFile(), Charset.defaultCharset());

            PureModelContextData pureModelContextData2 = PureModelContextData.newBuilder().withOrigin(null).withSerializer(null).withPureModelContextData(PureGrammarParser.newInstance().parseModel(pureProject1Contents)).build();

            doReturn(pureModelContextData2).when(resource).loadModelContextData(any(), eq(null), eq("SAMPLE-123"));
        }
        catch (PrivilegedActionException | IOException e)
        {
            throw new RuntimeException(e);
        }
        resources = ResourceTestRule.builder()
                .addResource(resource)
                .addResource(new MockPac4jFeature())
                .build();
    }


    @Test
    public void getSchemaFromQueryString()
    {
        Schema actualSchema = resources.target("sql/v1/execution/getSchemaFromQueryString/SAMPLE-123")
                .request()
                .post(Entity.text("SELECT * FROM service.\"/testService\"")).readEntity(Schema.class);
        SchemaColumn col1 = new SchemaColumn("Id", "Integer");
        SchemaColumn col2 = new SchemaColumn("Name", "String");
        Schema expectedSchema = new Schema(FastList.newListWith(col1, col2));
        Assert.assertEquals(expectedSchema, actualSchema);
    }

    @Test
    public void getSchemaFromQuery()
    {
        Schema actualSchema = resources.target("sql/v1/execution/getSchemaFromQuery/SAMPLE-123")
                .request()
                .post(Entity.json("{ \"_type\": \"query\", \"orderBy\": [], \"queryBody\": { \"_type\": \"querySpecification\", \"from\": [ { \"_type\": \"table\", \"name\": { \"parts\": [ \"service\", \"/testService\" ] } } ], \"groupBy\": [], \"orderBy\": [], \"select\": { \"_type\": \"select\", \"distinct\": false, \"selectItems\": [ { \"_type\": \"allColumns\" } ] } } }")).readEntity(Schema.class);
        SchemaColumn col1 = new SchemaColumn("Id", "Integer");
        SchemaColumn col2 = new SchemaColumn("Name", "String");
        Schema expectedSchema = new Schema(FastList.newListWith(col1, col2));
        Assert.assertEquals(expectedSchema, actualSchema);
    }

}