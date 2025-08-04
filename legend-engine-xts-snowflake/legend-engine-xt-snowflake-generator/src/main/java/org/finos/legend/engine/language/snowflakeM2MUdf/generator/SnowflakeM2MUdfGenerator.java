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


package org.finos.legend.engine.language.snowflakeM2MUdf.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.functionActivator.generation.FunctionActivatorGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.functionActivator.postDeployment.ActionContent;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.SDLC;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FunctionParametersValidationNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfArtifact;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfContent;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.deployment.SnowflakeM2MUdfDeploymentConfiguration;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdf;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_DeploymentOwnership;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_ConcreteFunctionDefinition_Impl;
import org.finos.legend.pure.generated.core_snowflake_core_snowflakem2mUdf_generation_generation;
import org.finos.legend.pure.generated.platform_pure_essential_meta_graph_elementToPath;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

public class SnowflakeM2MUdfGenerator
{
    private static  final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SnowflakeM2MUdfGenerator.class);
    public static final String EXECUTION_JAR_FILENAME = "legend-engine-xt-snowflake-m2mudf-plan-executor.jar";
    public static final String EXECUTION_PLAN_FILENAME = "executionPlan.json";
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static String database;
    private static String deploymentSchema;
    private static String deploymentStage;
    private static String udfName;
    private static String directoryLocation;

    public static SnowflakeM2MUdfArtifact generateArtifact(PureModel pureModel, Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf activator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        RelationalDatabaseConnection connection;
        AlloySDLC sdlc = null;
        if (((PureModelContextData)inputModel).getOrigin() != null)
        {
            SDLC sdlcInfo = ((PureModelContextData)inputModel).origin.sdlcInfo;
            if (sdlcInfo instanceof AlloySDLC)
            {
                sdlc = (AlloySDLC) sdlcInfo;
            }
        }
        //identify connection
        SnowflakeM2MUdf protocolActivator = Lists.mutable.withAll(((PureModelContextData) inputModel).getElementsOfType(SnowflakeM2MUdf.class))
                .select(c -> c.getPath().equals(platform_pure_essential_meta_graph_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_(activator, pureModel.getExecutionSupport())))
                .getFirst();
        connection   = (RelationalDatabaseConnection) Lists.mutable.withAll(((PureModelContextData) inputModel).getElementsOfType(PackageableConnection.class))
                .select(c -> c.getPath().equals(((org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdfDeploymentConfiguration)protocolActivator.activationConfiguration).activationConnection.connection)).getFirst().connectionValue;
        SnowflakeDatasourceSpecification ds = (SnowflakeDatasourceSpecification)connection.datasourceSpecification;

        MutableList<PlanGeneratorExtension> generatorExtensions = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        SingleExecutionPlan singleExecutionPlan = PlanGenerator.generateExecutionPlan(
                (FunctionDefinition<?>) activator._function(),
                null,
                null,
                null,
                pureModel,
                PureClientVersions.production,
                PlanPlatform.JAVA,
                null,
                routerExtensions.apply(pureModel),
                generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers)
        );

        database = ds.databaseName;
        deploymentSchema = activator._deploymentSchema();
        deploymentStage = activator._deploymentStage();
        udfName = activator._udfName().toUpperCase();
        directoryLocation = extractElementPath((Root_meta_pure_metamodel_function_ConcreteFunctionDefinition_Impl) activator._function());
        String inputStub = extractInputStub(singleExecutionPlan);
        String deployedLocation = String.format("https://app.%s.privatelink.snowflakecomputing.com/%s/%s/data/databases/%S/schemas/%S/user-function/%S(VARCHAR)", ds.region, ds.region, ds.accountName, ds.databaseName,deploymentSchema,udfName);

        List<String> sqlCommands = new ArrayList<>();

        //Add put command for execution jar
        sqlCommands.add(generatePutSqlCommand(EXECUTION_JAR_FILENAME));
        //Add put command for execution plan
        sqlCommands.add(generatePutSqlCommand(EXECUTION_PLAN_FILENAME));
        //Add udf create or replace statement
        sqlCommands.add(generateCreateFunctionQuery(inputStub));
        //Add grant statement
        sqlCommands.add(generateGrantStatement());

        String executionPlan;
        try
        {
            JavaHelper.compilePlan(singleExecutionPlan, Identity.getAnonymousIdentity());
            executionPlan = mapper.writeValueAsString(singleExecutionPlan);
        }
        catch (JavaCompileException e)
        {
            throw new RuntimeException(e);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }

        SnowflakeM2MUdfContent content = new SnowflakeM2MUdfContent(udfName, executionPlan, sqlCommands, getVersion(), activator._description(), database, deploymentSchema, deploymentStage, ((Root_meta_external_function_activator_DeploymentOwnership)activator._ownership())._id());
        List<ActionContent> actionContents = FunctionActivatorGenerator.generateActions(activator, pureModel, routerExtensions);

        return new SnowflakeM2MUdfArtifact(content, new SnowflakeM2MUdfDeploymentConfiguration(connection), deployedLocation, actionContents, sdlc);
    }

    private static String extractInputStub(SingleExecutionPlan singleExecutionPlan)
    {
        return "(\"" + (((FunctionParametersValidationNode) singleExecutionPlan.rootExecutionNode.executionNodes.get(0)).functionParameters).get(0).name + "\" VARCHAR)";
    }

    private static String extractElementPath(Root_meta_pure_metamodel_function_ConcreteFunctionDefinition_Impl function)
    {
        return "/" + String.join("/",function._functionName().split("::"));
    }

    private static String generateCreateFunctionQuery(String inputStub)
    {
        String query = "CREATE OR REPLACE FUNCTION " + database + "." + deploymentSchema + "." + udfName + inputStub + "\n" +
                "RETURNS VARCHAR\n" +
                "LANGUAGE JAVA\n" +
                "HANDLER = 'PlanExecutor.executeWithInputFromPlan'\n" +
                "IMPORTS = ('@" + deploymentStage + directoryLocation + "/" + EXECUTION_JAR_FILENAME + "')\n" +
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
                "    public static final String filename = \"@" + deploymentStage + directoryLocation + "/" + EXECUTION_PLAN_FILENAME + "\";\n" +
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

        return query;
    }

    private static String generatePutSqlCommand(String fileName)
    {
        return String.format("PUT file://%s/%s @%S%s AUTO_COMPRESS = FALSE OVERWRITE = TRUE",TEMP_DIR,fileName,deploymentStage,directoryLocation);
    }

    private static String generateGrantStatement()
    {
        return "GRANT USAGE ON FUNCTION " + database + "." + deploymentSchema + "." + udfName + "(VARCHAR) to role PUBLIC;";
    }

    private static String getVersion()
    {
        String version = null;
        try (InputStream input = SnowflakeM2MUdfGenerator.class.getClassLoader().getResourceAsStream("version.properties"))
        {
            if (input == null)
            {
                LOGGER.error("version.properties not found");
            }

            Properties props = new Properties();
            props.load(input);
            version = props.getProperty("version");
        }
        catch (Exception e)
        {
            LOGGER.error("Error capturing the version from application reason: " + e.getMessage());
        }

        return version;
    }

    public static String generateFunctionLineage(PureModel pureModel, Root_meta_external_function_activator_snowflakeM2MUdf_SnowflakeM2MUdf activator, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        return core_snowflake_core_snowflakem2mUdf_generation_generation.Root_meta_external_function_activator_snowflakeM2MUdf_generation_computeLineage_SnowflakeM2MUdf_1__Extension_MANY__String_1_(activator, routerExtensions.apply(pureModel), pureModel.getExecutionSupport());
    }
}
