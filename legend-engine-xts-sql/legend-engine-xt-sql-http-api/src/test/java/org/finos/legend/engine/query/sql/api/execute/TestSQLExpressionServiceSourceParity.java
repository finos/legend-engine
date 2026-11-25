// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.query.sql.api.execute;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.client.Entity;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.sql.metamodel.Query;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.query.sql.api.TestSQLSourceProvider;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.glassfish.jersey.test.TestProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ServiceLoader;

@ExtendWith(DropwizardExtensionsSupport.class)
public class TestSQLExpressionServiceSourceParity
{
    static
    {
        System.setProperty(TestProperties.CONTAINER_PORT, "0");
    }

    public static final ResourceExtension resources = SqlExecuteTest.getResourceTestRule();
    private static final ObjectMapper OM = new ObjectMapper();
    private static final PureModelContextData PROJECT = GrammarParseTestUtils.loadPureModelContextFromResource("proj-1.pure", TestSQLSourceProvider.class);

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT Id, Name, \"Employee Type\" FROM service('/personServiceForNames') ORDER BY Id",
            "SELECT Id, Name, \"Employee Type\" FROM service('/personServiceForNamesRelation') ORDER BY Id",
            "SELECT Name FROM service('/personServiceForNames', names => ['Alice', 'Danielle']) ORDER BY Name",
            "SELECT Name FROM service('/personServiceForNamesRelation', names => ['Alice', 'Danielle']) ORDER BY Name",
            "SELECT Name, Ratings FROM service('/personRatings') WHERE Ratings > 9.2 ORDER BY Name",
            "SELECT Name, Ratings FROM service('/personRatingsRelation') WHERE Ratings > 9.2 ORDER BY Name",
            "SELECT Id, CASE WHEN Id > 102 THEN Name END AS later FROM service('/personService') ORDER BY Id",
            "SELECT Id, CASE WHEN Id > 102 THEN Name END AS later FROM service('/personServiceRelation') ORDER BY Id",
            "SELECT Id, \"Employee Type\" FROM service('/personServiceMultiExec/{env}', env => 'env2', minId => 103) ORDER BY Id",
            "SELECT Name FROM service('/personTop2ByRating') ORDER BY Name",
            "SELECT Name FROM service('/personTop2ByRatingRelation') ORDER BY Name",
            "SELECT count(*) AS n FROM service('/personTop2ByRating')",
            "SELECT count(*) AS n FROM service('/personTop2ByRatingRelation')",
            "SELECT Name FROM service('/personTop2ByRating') WHERE Ratings < 9.4 ORDER BY Name",
            "SELECT Name FROM service('/personTop2ByRatingRelation') WHERE Ratings < 9.4 ORDER BY Name"
    })
    public void testSQLExpressionReturnsTheSameRowsAsThePostgresPath(String sql) throws Exception
    {
        TDSExecuteResult postgres = OM.readValue(resources.target("sql/v1/execution/execute")
                .request()
                .post(Entity.json(new SQLQueryInput((Query) null, sql, Lists.mutable.empty()))).readEntity(String.class), TDSExecuteResult.class);

        TDSExecuteResult sqlExpression = executeSQLExpression(sql);

        Assertions.assertFalse(postgres.result.rows.isEmpty(), postgres::toString);
        Assertions.assertEquals(postgres, sqlExpression);
    }

    private static TDSExecuteResult executeSQLExpression(String sql) throws Exception
    {
        PureModelContextData model = PureModelContextData.newBuilder()
                .withPureModelContextData(PROJECT)
                .withPureModelContextData(PureGrammarParser.newInstance().parseModel(
                        "function demo::sqlExpression():meta::pure::metamodel::relation::Relation<Any>[1]\n" +
                        "{\n" +
                        "  #SQL{" + sql + "}#\n" +
                        "}\n"))
                .withSectionIndexesMerged()
                .build();
        PureModel pureModel = new PureModel(model, null, DeploymentMode.TEST);
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(PureGrammarParser.newInstance().parseLambda("|demo::sqlExpression()"), pureModel.getContext());

        SingleExecutionPlan plan = PlanGenerator.generateExecutionPlan(lambda, null, null, null, pureModel, PureClientVersions.production, PlanPlatform.JAVA, null, extensions,
                Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class)).flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));
        StreamingResult result = (StreamingResult) PlanExecutor.newPlanExecutorWithAvailableStoreExecutors().execute(plan);

        return OM.readValue(result.flush(result.getSerializer(SerializationFormat.DEFAULT)), TDSExecuteResult.class);
    }
}
