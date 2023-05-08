// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.elasticsearch.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ServiceLoader;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Configuration;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.TDSResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.elasticsearch.test.shared.ElasticsearchCommands;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.junit.*;
import org.testcontainers.DockerClientFactory;

public class TestElasticsearchExecutionPlanFromGrammarIntegration
{
    private static final String TEST_IMAGE_TAG = "7.8.0";
    private static PureModel PURE_MODEL;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        Assume.assumeTrue("Only run with docker", DockerClientFactory.instance().isDockerAvailable());
        String hostPort = initESContainer();
        compileGrammar(hostPort);
    }

    private static String initESContainer() throws Exception
    {
        System.setProperty("org.finos.legend.engine.plan.execution.stores.elasticsearch.test.password", "s3cret");
        Root_meta_pure_functions_io_http_URL url = ElasticsearchCommands.startServer(TEST_IMAGE_TAG);
        ElasticsearchCommands.request(TEST_IMAGE_TAG, IOUtils.toString(ClassLoader.getSystemResource("createIndexV7ProtocolForPlanIntegrationTesting.json"), StandardCharsets.UTF_8));
        return url._host() + ':' + url._port();
    }

    private static void compileGrammar(String hostPort) throws IOException
    {
        String grammar = IOUtils.toString(ClassLoader.getSystemResource("grammarForPlanIntegrationTesting.pure"), StandardCharsets.UTF_8);
        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(grammar.replace("_%_ELASTIC_HOST_%_", hostPort));
        PURE_MODEL = Compiler.compile(pmcd, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, null);
    }

    @AfterClass
    public static void afterClass()
    {
        ElasticsearchCommands.stopServer(TEST_IMAGE_TAG);
    }

    private Result getResultFromFunctionGrammar(String funcName)
    {
        ConcreteFunctionDefinition<?> concreteFxn = PURE_MODEL.getConcreteFunctionDefinition_safe(funcName);

        Assert.assertNotNull("Test function not found on model: " + funcName, concreteFxn);

        MutableList<PlanGeneratorExtension> extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = extensions.flatCollect(e -> e.getExtraExtensions(PURE_MODEL));
        MutableList<PlanTransformer> planTransformers = LegendPlanTransformers.transformers;

        SingleExecutionPlan plan = PlanGenerator.generateExecutionPlan(concreteFxn, null, null, null, PURE_MODEL, "vX_X_X", null, "id", routerExtensions, planTransformers);
        return PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build().execute(plan);
    }

    @Test
    public void testIndexToTdsPlanExecutionFromGrammar() throws IOException
    {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             TDSResult result = (TDSResult) getResultFromFunctionGrammar("abc::abc::indexToTdsFunction__TabularDataSet_1_")
        )
        {
            result.stream(outputStream, SerializationFormat.DEFAULT);
            String resultString = outputStream.toString(StandardCharsets.UTF_8.name());
            JsonAssert.assertJsonEquals(
                    "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"prop1\",\"type\":\"String\"}]},\"result\":{\"columns\":[\"prop1\"],\"rows\":[]},\"activities\":[{\"uri\":\"http://localhost:64794//index1/_search?typed_keys=true\",\"esRequest\":\"{\\\"_source\\\":{\\\"includes\\\":[\\\"prop1\\\"]}}\"}]}",
                    resultString,
                    Configuration.empty().whenIgnoringPaths("activities"));
        }
    }
}
