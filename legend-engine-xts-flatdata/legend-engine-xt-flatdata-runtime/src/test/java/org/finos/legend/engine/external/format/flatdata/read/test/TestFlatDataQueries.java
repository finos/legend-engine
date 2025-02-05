// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.read.test;

import net.javacrumbs.jsonunit.JsonMatchers;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.transformation.fromModel.ModelToFlatDataConfiguration;
import org.finos.legend.engine.external.format.flatdata.transformation.toModel.FlatDataToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ModelToSchemaGenerationTest;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.SchemaToModelGenerationTest;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.graphFetch.GraphFetchExecutionConfiguration;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_format_flatdata_externalFormatContract;
import org.finos.legend.pure.generated.core_external_format_flatdata_java_platform_binding_legendJavaPlatformBinding_descriptor;
import org.finos.legend.pure.generated.core_java_platform_binding_external_format_legendJavaPlatformBinding_externalFormat_bindingLegendJavaPlatformBindingExtension;
import org.finos.legend.pure.generated.core_pure_binding_extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

import static org.finos.legend.engine.external.shared.format.model.transformation.toModel.SchemaToModelGenerationTest.newExternalSchemaSetGrammarBuilder;

public class TestFlatDataQueries extends TestExternalFormatQueries
{
    @BeforeClass
    public static void setup()
    {
        ExecutionSupport executionSupport = Compiler.compile(PureModelContextData.newPureModelContextData(), null, Identity.getAnonymousIdentity().getName()).getExecutionSupport();
        formatExtensions = Collections.singletonList(core_external_format_flatdata_externalFormatContract.Root_meta_external_format_flatdata_extension_flatDataFormatExtension__Extension_1_(executionSupport));
        formatDescriptors = Collections.singletonList(core_external_format_flatdata_java_platform_binding_legendJavaPlatformBinding_descriptor.Root_meta_external_format_flatdata_executionPlan_platformBinding_legendJava_flatDataJavaBindingDescriptor__ExternalFormatLegendJavaPlatformBindingDescriptor_1_(executionSupport));
    }

    @Test
    public void testInternalizeWithGraphFetchAndDefects()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::GeographicPosition");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#"; // latitude property skipped on purpose to test graphFetch expands tree scope to include constraint on latitude

        try
        {
            runTest(generated,
                    "data:Byte[*]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetch(" + positionTree + ")->serialize(" + positionTree + ")",
                    Maps.mutable.with("data", resource("queries/positionWithExactHeadings.csv")));
            Assert.fail("Expected exception to be raised. Not found any");
        }
        catch (Exception e)
        {
            Assert.assertEquals("java.lang.IllegalStateException: Constraint :[validLatitude] violated in the Class GeographicPosition", e.getMessage());
        }
    }

    @Test
    public void testInternalizeWithGraphFetchUnexpandedAndDefects()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::GeographicPosition");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String positionTree = "#{test::firm::model::GeographicPosition{latitude, longitude}}#";

        try
        {
            runTest(generated,
                    "data:Byte[*]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetchUnexpanded(" + positionTree + ")->serialize(" + positionTree + ")",
                    Maps.mutable.with("data", resource("queries/positionWithExactHeadings.csv")));
            Assert.fail("Expected exception to be raised. Not found any");
        }
        catch (Exception e)
        {
            Assert.assertEquals("java.lang.IllegalStateException: Constraint :[validLatitude] violated in the Class GeographicPosition", e.getMessage());
        }
    }

    @Test
    public void testExternalizeWithCheckedTree()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        try
        {
            runTest(generated,
                    "data:Byte[*]|test::firm::model::Person->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + personTree() + ")->externalize(test::gen::TestBinding, checked(" + personTree() + ", test::gen::TestBinding))",
                    Maps.mutable.with("data", resource("queries/peopleWithExactHeadings.csv")));
            Assert.fail("Exception expected");
        }
        catch (Exception e)
        {
            Assert.assertEquals("Assert failure at (resource:/core_external_format_flatdata_java_platform_binding/legendJavaPlatformBinding/externalize.pure line:95 column:3), \"Multi Section serialization is not yet supported !!\"", e.getMessage());
        }
    }

    @Test
    public void testMemoryLimit()
    {
        FlatDataToModelConfiguration config = new FlatDataToModelConfiguration();
        config.targetPackage = "test::gen";
        config.purifyNames = true;
        config.schemaClassName = "PriceFile";
        config.format = "FlatData";

        String tree = "#{test::gen::PricesRecord{accountId,synonym,synonymType,currency,closePrice,priceFile{header{closeOfBusiness}}}}#";

        String schemaCode = multiSectionSchema();
        PureModelContextData generated = SchemaToModelGenerationTest.generateModel(schemaCode, config, true, "test::gen::TestBinding");
        PureModelContextData schemaData = PureGrammarParser.newInstance().parseModel(schemaCode);

        PureModel model = Compiler.compile(generated.combine(schemaData), DeploymentMode.TEST, Identity.getAnonymousIdentity().getName());
        PureGrammarParser parser = PureGrammarParser.newInstance();
        Lambda lambdaProtocol = parser.parseLambda("data:String[1]|test::gen::PricesRecord->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + tree + ")->serialize(" + tree + ")");
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(lambdaProtocol.body, lambdaProtocol.parameters, model.getContext());

        MutableList<Root_meta_pure_extension_Extension> extensions = Lists.mutable.with(core_pure_binding_extension.Root_meta_external_format_shared_externalFormatExtension__Extension_1_(model.getExecutionSupport()));
        extensions.addAll(formatExtensions);
        extensions.addAllIterable(core_java_platform_binding_external_format_legendJavaPlatformBinding_externalFormat_bindingLegendJavaPlatformBindingExtension.Root_meta_external_format_shared_executionPlan_platformBinding_legendJava_bindingExtensionsWithLegendJavaPlatformBinding_ExternalFormatLegendJavaPlatformBindingDescriptor_MANY__Extension_MANY_(Lists.mutable.withAll(formatDescriptors), model.getExecutionSupport()));

        SingleExecutionPlan plan = PlanGenerator.generateExecutionPlan(lambda, null, null, null, model, "vX_X_X", PlanPlatform.JAVA, "test", extensions, LegendPlanTransformers.transformers);

        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.ExecuteArgs.newArgs()
                .withPlan(plan)
                .withParams(Maps.mutable.with("data", resourceAsString("queries/prices.csv")))
                .build();

        try
        {
            PlanExecutor executor = PlanExecutor.newPlanExecutorBuilder().withGraphFetchExecutionConfiguration(new GraphFetchExecutionConfiguration(1)).withAvailableStoreExecutors().build();
            StreamingResult streamingResult = (StreamingResult) executor.executeWithArgs(executeArgs);
            String res = streamingResult.flush(streamingResult.getSerializer(SerializationFormat.DEFAULT));
            Assert.fail("Exception expected");
        }
        catch (Exception e)
        {
            Assert.assertEquals("Cannot access header, data too large for in memory operation", e.getMessage());
        }

        PlanExecutor executor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
        StreamingResult streamingResult = (StreamingResult) executor.executeWithArgs(executeArgs);
        String result = streamingResult.flush(streamingResult.getSerializer(SerializationFormat.DEFAULT));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals("{\"builder\":{\"_type\":\"json\"},\"values\":[{\"defects\":[],\"source\":{\"number\":1,\"lineNumber\":2,\"record\":\"123456789~ABC~123A4~USD~~~~~0.01\",\"recordValues\":[{\"address\":1,\"rawValue\":\"123456789\"},{\"address\":2,\"rawValue\":\"ABC\"},{\"address\":3,\"rawValue\":\"123A4\"},{\"address\":4,\"rawValue\":\"USD\"},{\"address\":5,\"rawValue\":\"\"},{\"address\":6,\"rawValue\":\"\"},{\"address\":7,\"rawValue\":\"\"},{\"address\":8,\"rawValue\":\"\"},{\"address\":9,\"rawValue\":\"0.01\"}]},\"value\":{\"accountId\":123456789,\"synonym\":\"123A4\",\"synonymType\":\"ABC\",\"currency\":\"USD\",\"closePrice\":0.01,\"priceFile\":{\"header\":{\"closeOfBusiness\":\"2021-06-08\"}}}},{\"defects\":[],\"source\":{\"number\":2,\"lineNumber\":3,\"record\":\"123456789~ABC~123A4~USD~~~~~0.02\",\"recordValues\":[{\"address\":1,\"rawValue\":\"123456789\"},{\"address\":2,\"rawValue\":\"ABC\"},{\"address\":3,\"rawValue\":\"123A4\"},{\"address\":4,\"rawValue\":\"USD\"},{\"address\":5,\"rawValue\":\"\"},{\"address\":6,\"rawValue\":\"\"},{\"address\":7,\"rawValue\":\"\"},{\"address\":8,\"rawValue\":\"\"},{\"address\":9,\"rawValue\":\"0.02\"}]},\"value\":{\"accountId\":123456789,\"synonym\":\"123A4\",\"synonymType\":\"ABC\",\"currency\":\"USD\",\"closePrice\":0.02,\"priceFile\":{\"header\":{\"closeOfBusiness\":\"2021-06-08\"}}}},{\"defects\":[],\"source\":{\"number\":3,\"lineNumber\":4,\"record\":\"123456789~ABC~123A4~USD~~~~~0.03\",\"recordValues\":[{\"address\":1,\"rawValue\":\"123456789\"},{\"address\":2,\"rawValue\":\"ABC\"},{\"address\":3,\"rawValue\":\"123A4\"},{\"address\":4,\"rawValue\":\"USD\"},{\"address\":5,\"rawValue\":\"\"},{\"address\":6,\"rawValue\":\"\"},{\"address\":7,\"rawValue\":\"\"},{\"address\":8,\"rawValue\":\"\"},{\"address\":9,\"rawValue\":\"0.03\"}]},\"value\":{\"accountId\":123456789,\"synonym\":\"123A4\",\"synonymType\":\"ABC\",\"currency\":\"USD\",\"closePrice\":0.03,\"priceFile\":{\"header\":{\"closeOfBusiness\":\"2021-06-08\"}}}},{\"defects\":[],\"source\":{\"number\":4,\"lineNumber\":5,\"record\":\"123456789~ABC~123A4~USD~~~~~0.04\",\"recordValues\":[{\"address\":1,\"rawValue\":\"123456789\"},{\"address\":2,\"rawValue\":\"ABC\"},{\"address\":3,\"rawValue\":\"123A4\"},{\"address\":4,\"rawValue\":\"USD\"},{\"address\":5,\"rawValue\":\"\"},{\"address\":6,\"rawValue\":\"\"},{\"address\":7,\"rawValue\":\"\"},{\"address\":8,\"rawValue\":\"\"},{\"address\":9,\"rawValue\":\"0.04\"}]},\"value\":{\"accountId\":123456789,\"synonym\":\"123A4\",\"synonymType\":\"ABC\",\"currency\":\"USD\",\"closePrice\":0.04,\"priceFile\":{\"header\":{\"closeOfBusiness\":\"2021-06-08\"}}}}]}"));
    }

    private String multiSectionSchema()
    {
        return newExternalSchemaSetGrammarBuilder("test::WholeLoanPriceFileSchema", "FlatData")
                .withSchemaText("section header: DelimitedWithoutHeadings\n" +
                        "{\n" +
                        "  delimiter: ' ';\n" +
                        "  scope.forNumberOfLines: 1;\n" +
                        "\n" +
                        "  Record\n" +
                        "  {\n" +
                        "    closeOfBusiness {3}: DATE(format='yyyyMMdd');\n" +
                        "  }\n" +
                        "}\n" +
                        "\n" +
                        "section prices: DelimitedWithoutHeadings\n" +
                        "{\n" +
                        "  scope.untilEof;\n" +
                        "  delimiter: '~';\n" +
                        "\n" +
                        "  Record\n" +
                        "  {\n" +
                        "    Account_ID   {1}: INTEGER;\n" +
                        "    Synonym_Type {2}: STRING;\n" +
                        "    Synonym      {3}: STRING;\n" +
                        "    Currency     {4}: STRING;\n" +
                        "    Close_Price  {9}: DECIMAL;\n" +
                        "  }\n" +
                        "}\n")
                .build();
    }

    private ModelToFlatDataConfiguration toFlatDataConfig()
    {
        ModelToFlatDataConfiguration config = new ModelToFlatDataConfiguration();
        config.targetSchemaSet = "test::gen::TestSchemaSet";
        config.format = "FlatData";
        return config;
    }
}
