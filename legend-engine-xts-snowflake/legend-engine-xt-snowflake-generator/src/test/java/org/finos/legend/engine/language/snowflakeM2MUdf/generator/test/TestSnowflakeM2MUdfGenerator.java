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

package org.finos.legend.engine.language.snowflakeM2MUdf.generator.test;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.snowflakeM2MUdf.generator.SnowflakeM2MUdfGenerator;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfArtifact;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfContent;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.finos.legend.pure.generated.platform_pure_essential_meta_graph_pathToElement.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_;

public class TestSnowflakeM2MUdfGenerator
{
    private final PureModelContextData contextData;
    private final PureModel pureModel;
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel pureModel) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));

    public TestSnowflakeM2MUdfGenerator()
    {
        this.contextData = PureGrammarParser.newInstance().parseModel(readModelContentFromResource(this.modelResourcePath()));
        this.pureModel = Compiler.compile(contextData, null, Identity.getAnonymousIdentity().getName());
    }

    public String modelResourcePath()
    {
        return "/org/finos/legend/engine/language/snowflakeM2MUdf/generator/snowflakeM2MUdfTestModels.pure";
    }

    private String readModelContentFromResource(String resourcePath)
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(Objects.requireNonNull(TestSnowflakeM2MUdfGenerator.class.getResourceAsStream(resourcePath)))))
        {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private SnowflakeM2MUdfArtifact generateForActivator(String activatorPath, PureModel pureModel)
    {
        Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf app = (Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf) Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_(activatorPath, pureModel.getExecutionSupport());
        return SnowflakeM2MUdfGenerator.generateArtifact(pureModel, app, this.contextData, routerExtensions);
    }

    @Test
    public void testSnowflakeM2MUdfActivator()
    {
        SnowflakeM2MUdfArtifact artifact = generateForActivator("test::udf::MyUDF", this.pureModel);
        String expectedExecutionJarPutCommand = "PUT file://" + SnowflakeM2MUdfGenerator.TEMP_DIR + "/legend-engine-xt-snowflake-m2mudf-plan-executor.jar @DEMO_SCHEMA.SNOWFLAKESTAGE/test/query/getFirmDetailsWithInput AUTO_COMPRESS = FALSE OVERWRITE = TRUE";
        String expectedExecutionPlanPutCommand = "PUT file://" + SnowflakeM2MUdfGenerator.TEMP_DIR + "/executionPlan.json @DEMO_SCHEMA.SNOWFLAKESTAGE/test/query/getFirmDetailsWithInput AUTO_COMPRESS = FALSE OVERWRITE = TRUE";
        String expectedCreateCommand = "CREATE OR REPLACE FUNCTION dbName.legend_native_apps_1.MYUDF(\"input\" VARCHAR)\n" +
                "RETURNS VARCHAR\n" +
                "LANGUAGE JAVA\n" +
                "HANDLER = 'PlanExecutor.executeWithInputFromPlan'\n" +
                "IMPORTS = ('@demo_schema.snowflakeStage/test/query/getFirmDetailsWithInput/legend-engine-xt-snowflake-m2mudf-plan-executor.jar')\n" +
                "AS '\n" +
                "import java.io.IOException;\n" +
                "import java.io.InputStream;\n" +
                "import java.nio.charset.StandardCharsets;\n" +
                "import com.snowflake.snowpark_java.types.SnowflakeFile;\n" +
                "import org.finos.legend.engine.execution.m2m.plan.SnowflakeM2MUdfPlanExecutor;\n" +
                "import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;\n" +
                "import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;\n" +
                "import org.finos.legend.engine.shared.core.identity.Identity;\n" +
                "import com.fasterxml.jackson.databind.ObjectMapper;\n" +
                "import org.finos.legend.engine.shared.core.ObjectMapperFactory;\n" +
                "import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;\n" +
                "import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;\n" +
                "import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FunctionParametersValidationNode;\n" +
                "import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;\n" +
                "\n" +
                "class PlanExecutor {\n" +
                "    public static final String filename = \"@demo_schema.snowflakeStage/test/query/getFirmDetailsWithInput/executionPlan.json\";\n" +
                "    private static ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();\n" +
                "    public static EngineJavaCompiler engineJavaCompiler;\n" +
                "    public static SingleExecutionPlan singleExecutionPlan;\n" +
                "    public static String parameter;\n" +
                "    \n" +
                "    public static String executeWithInputFromPlan(String input) {\n" +
                "        try \n" +
                "        {\n" +
                "             if(singleExecutionPlan == null){\n" +
                "                 SnowflakeFile sfFile = SnowflakeFile.newInstance(filename, false);\n" +
                "                 InputStream stream = sfFile.getInputStream();\n" +
                "                 String plan = new String(stream.readAllBytes(), StandardCharsets.UTF_8);\n" +
                "                 singleExecutionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);\n" +
                "                 parameter = ((FunctionParametersValidationNode) singleExecutionPlan.rootExecutionNode.executionNodes.get(0)).functionParameters.get(0).name;\n" +
                "                 try {\n" +
                "                     engineJavaCompiler = JavaHelper.compilePlan(singleExecutionPlan, Identity.getAnonymousIdentity());\n" +
                "                 } catch (JavaCompileException e) {\n" +
                "                     throw new RuntimeException(e);\n" +
                "                 }\n" +
                "             }\n" +
                "             return SnowflakeM2MUdfPlanExecutor.executeSnowflakeM2MUdfPlanWithArg(singleExecutionPlan, engineJavaCompiler, parameter, input);\n" +
                "        }\n" +
                "        catch (IOException e)\n" +
                "        {\n" +
                "            throw new RuntimeException(e);\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "';";
        String expectedDeployedLocation = "https://app.region.privatelink.snowflakecomputing.com/region/account/data/databases/DBNAME/schemas/LEGEND_NATIVE_APPS_1/user-function/MYUDF(VARCHAR)";
        Assert.assertEquals(expectedExecutionJarPutCommand, ((SnowflakeM2MUdfContent)artifact.content).sqlCommands.get(0));
        Assert.assertEquals(expectedExecutionPlanPutCommand, ((SnowflakeM2MUdfContent)artifact.content).sqlCommands.get(1));
        Assert.assertEquals(expectedCreateCommand, ((SnowflakeM2MUdfContent)artifact.content).sqlCommands.get(2));
        Assert.assertEquals(expectedDeployedLocation, artifact.deployedLocation);
    }

}
