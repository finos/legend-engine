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

package org.finos.legend.engine.execution.m2m.plan.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.execution.m2m.plan.SnowflakeM2MUdfPlanExecutor;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.pure.generated.platform_pure_essential_meta_graph_pathToElement.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_;

public class TestSnowflakeM2MUdfPlanExecutor
{
    private static  final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final PureModelContextData contextData;
    private final PureModel pureModel;
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = (PureModel pureModel) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));

    public TestSnowflakeM2MUdfPlanExecutor()
    {
        this.contextData = PureGrammarParser.newInstance().parseModel(readContentFromResource(resourcePath()));
        this.pureModel = Compiler.compile(contextData, null, Identity.getAnonymousIdentity().getName());
    }

    public String resourcePath()
    {
        return "/org/finos/legend/engine/execution/m2m/plan/executionPlanTestModel.pure";
    }

    private String readContentFromResource(String resourcePath)
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(Objects.requireNonNull(TestSnowflakeM2MUdfPlanExecutor.class.getResourceAsStream(resourcePath)))))
        {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected String generateAndCompilePlan(FunctionDefinition<?> function)
    {
        MutableList<PlanGeneratorExtension> generatorExtensions = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        SingleExecutionPlan singleExecutionPlan = PlanGenerator.generateExecutionPlan(
                (FunctionDefinition<?>) function,
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

        String executionPlan = null;
        try
        {
            JavaHelper.compilePlan(singleExecutionPlan, Identity.getAnonymousIdentity());
            executionPlan = mapper.writeValueAsString(singleExecutionPlan);
        }
        catch (JavaCompileException | JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }

        return executionPlan;
    }

    @Test
    public void testM2MJsonPlanExecutorWithValidArguments()
    {
        FunctionDefinition<?> function = (FunctionDefinition<?>) Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_("test::query::getFirmDetailsWithInput_String_1__String_1_", pureModel.getExecutionSupport());
        String actual = SnowflakeM2MUdfPlanExecutor.executeWithArgs(generateAndCompilePlan(function), "{\"firms\": [{\"employees\": [{\"firstName\": \"ABC\",\"lastName\": \"DEF\"},{\"firstName\": \"XYZ\",\"lastName\": \"PQR\"}],\"legalName\":\"IJK\"}]}");
        String expected = "{\"firms\":[{\"legalName\":\"IJK\",\"employees\":[{\"firstName\":\"ABC\",\"lastName\":\"DEF\"},{\"firstName\":\"XYZ\",\"lastName\":\"PQR\"}]}]}";
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testM2MJsonPlanExecutorWithInvalidArguments()
    {
        FunctionDefinition<?> function = (FunctionDefinition<?>) Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_("test::query::getFirmDetailsWithInput_String_1__String_1_", pureModel.getExecutionSupport());
        EngineException exception = Assert.assertThrows(EngineException.class, () -> SnowflakeM2MUdfPlanExecutor.executeWithArgs(generateAndCompilePlan(function), "{\"firms\": [{\"employees\": [{\"firstName\": \"ABC\",\"lastName\": \"DEF\"},{\"firstName\": \"XYZ\",\"lastName\": \"PQR\"}],\"legalName\":\"IJK\"}]}", "123"));
        Assert.assertEquals("Number of function parameters does not match number of arguments", exception.getMessage());
    }

}
