//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json.read.test;

import net.javacrumbs.jsonunit.JsonMatchers;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.external.format.json.fromModel.ModelToJsonSchemaConfiguration;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ModelToSchemaGenerationTest;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.pure.generated.core_external_format_json_externalFormatContract;
import org.finos.legend.pure.generated.core_external_format_json_java_platform_binding_legendJavaPlatformBinding_descriptor;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

public class TestJsonSchemaQueries extends TestExternalFormatQueries
{
    @BeforeClass
    public static void setup()
    {
        ExecutionSupport executionSupport = Compiler.compile(PureModelContextData.newPureModelContextData(), null, IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName()).getExecutionSupport();
        formatExtensions = Collections.singletonList(core_external_format_json_externalFormatContract.Root_meta_external_format_json_extension_jsonSchemaFormatExtension__Extension_1_(executionSupport));
        formatDescriptors = Collections.singletonList(core_external_format_json_java_platform_binding_legendJavaPlatformBinding_descriptor.Root_meta_external_format_json_executionPlan_platformBinding_legendJava_jsonSchemaJavaBindingDescriptor__ExternalFormatLegendJavaPlatformBindingDescriptor_1_(executionSupport));
    }

    @Test
    public void testInternalizeWithGraphFetchAndDefects()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Lists.mutable.with("test::firm::model::Person", "test::firm::model::Address", "test::firm::model::AddressUse", "test::firm::model::GeographicPosition");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toJsonSchemaConfig(), true, "test::gen::TestBinding");

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#"; // latitude property skipped on purpose to test graphFetch expands tree scope to include constraint on latitude

        try
        {
            runTest(generated,
                    "data:Byte[*]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetch(" + positionTree + ")->externalize(test::gen::TestBinding, " + positionTree + ")",
                    Maps.mutable.with("data", resource("queries/positions.json")));
            Assert.fail("Expected exception to be raised. Not found any");
        }
        catch (Exception e)
        {
            Assert.assertEquals("java.lang.IllegalStateException: Constraint :[validLatitude] violated in the Class GeographicPosition", e.getMessage());
        }
    }

    @Test
    public void testInternalizeWithGraphFetchChecked()
    {
        String modelGrammar = firmModel() + "\n\n" +
                "###ExternalFormat\n" +
                "Binding test::gen::TestBinding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    test::firm::model::Person,\n" +
                "    test::firm::model::Address,\n" +
                "    test::firm::model::AddressUse,\n" +
                "    test::firm::model::GeographicPosition,\n" +
                "    meta::pure::dataQuality::Checked\n," +
                "    meta::pure::dataQuality::Defect\n," +
                "    meta::pure::dataQuality::RelativePathNode" +
                "  ];\n" +
                "}";

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#"; // latitude property skipped on purpose to test graphFetch expands tree scope to include constraint on latitude

        String result = runTest(PureGrammarParser.newInstance().parseModel(modelGrammar),
                "data:Byte[*]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + positionTree + ")->externalize(test::gen::TestBinding, checked(" + positionTree + ", test::gen::TestBinding))",
                Maps.mutable.with("data", resource("queries/positions.json")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/positionGraphFetchCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithGraphFetchUnexpanded()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Lists.mutable.with("test::firm::model::Person", "test::firm::model::Address", "test::firm::model::AddressUse", "test::firm::model::GeographicPosition");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toJsonSchemaConfig(), true, "test::gen::TestBinding");

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#";

        String result = runTest(generated,
                "data:Byte[*]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetchUnexpanded(" + positionTree + ")->externalize(test::gen::TestBinding, " + positionTree + ")",
                Maps.mutable.with("data", resource("queries/positions.json")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/positionGraphFetchUnexpandedResult.json")));
    }

    @Test
    public void testInternalizeWithGraphFetchUnexpandedChecked()
    {
        String modelGrammar = firmModel() + "\n\n" +
                "###ExternalFormat\n" +
                "Binding test::gen::TestBinding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    test::firm::model::Person,\n" +
                "    test::firm::model::Address,\n" +
                "    test::firm::model::AddressUse,\n" +
                "    test::firm::model::GeographicPosition,\n" +
                "    meta::pure::dataQuality::Checked\n," +
                "    meta::pure::dataQuality::Defect\n," +
                "    meta::pure::dataQuality::RelativePathNode" +
                "  ];\n" +
                "}";

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#";

        String result = runTest(PureGrammarParser.newInstance().parseModel(modelGrammar),
                "data:Byte[*]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetchCheckedUnexpanded(" + positionTree + ")->externalize(test::gen::TestBinding, checked(" + positionTree + ", test::gen::TestBinding))",
                Maps.mutable.with("data", resource("queries/positions.json")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/positionGraphFetchCheckedUnexpandedResult.json")));
    }

    @Test
    public void testToAndFromJson()
    {
        String grammar = firmModel();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String resultWithByte = runTest(modelData,
                "data:Byte[*]|test::firm::model::Firm->fromJson($data)->toJson(" + firmTree() + ")",
                Maps.mutable.with("data", resource("queries/firmTreeTestData.json")));

        MatcherAssert.assertThat(resultWithByte, JsonMatchers.jsonEquals("[{\"name\":\"Firm A\",\"ranking\":1},{\"name\":\"Firm B\",\"ranking\":null}]"));

        String resultWithString = runTest(modelData,
                "data:String[1]|test::firm::model::Firm->fromJson($data)->toJson(" + firmTree() + ")",
                Maps.mutable.with("data", resourceAsString("queries/firmTreeTestData.json")));

        MatcherAssert.assertThat(resultWithString, JsonMatchers.jsonEquals("[{\"name\":\"Firm A\",\"ranking\":1},{\"name\":\"Firm B\",\"ranking\":null}]"));
    }

    @Test
    public void testM2MChainingWithContentType()
    {
        String modelGrammar = firmModel() + "\n\n###Pure\n" +
                "Class test::firm::model::TargetPerson\n" +
                "{\n" +
                "  fullName : String[1];\n" +
                "}" +
                "\n\n" +
                "###Mapping\n" +
                "Mapping test::firm::model::M2MMapping\n" +
                "(\n" +
                "  test::firm::model::TargetPerson: Pure\n" +
                "  {\n" +
                "    ~src test::firm::model::Person\n" +
                "    fullName : $src.firstName + ' ' + $src.lastName\n" +
                "  }\n" +
                ")" +
                "\n\n" +
                "###ExternalFormat\n" +
                "Binding test::gen::TestBinding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    test::firm::model::Person,\n" +
                "    test::firm::model::Address,\n" +
                "    test::firm::model::AddressUse,\n" +
                "    test::firm::model::GeographicPosition,\n" +
                "    test::firm::model::TargetPerson" +
                "  ];\n" +
                "}";

        String targetPersonTree = "#{test::firm::model::TargetPerson {fullName}}#";
        String result = runTest(PureGrammarParser.newInstance().parseModel(modelGrammar),
                "{data:Byte[*]|test::firm::model::TargetPerson.all()->graphFetch(" + targetPersonTree + ")->from(test::firm::model::M2MMapping, getRuntimeWithModelQueryConnection(test::firm::model::Person, 'application/json', $data))->externalize(test::gen::TestBinding, " + targetPersonTree + ");}",
                Maps.mutable.with("data", resource("queries/peopleTestData.json")));
        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/targetPersonResult.json")));
    }

    @Test
    public void testMultiM2MChaining()
    {
        String modelGrammar = firmModel() + "\n\n###Pure\n" +
                "Class test::firm::model::TargetPerson2\n" +
                "{\n" +
                "  fullName : String[1];\n" +
                "}\n" +
                "Class test::firm::model::TargetPerson\n" +
                "{\n" +
                "  fullName : String[1];\n" +
                "}\n" +
                "\n\n" +
                "###Connection\n" +
                "ModelChainConnection test::firm::connection::modelChainConnection\n" +
                "{\n" +
                "   mappings: [test::firm::model::M2MMapping];\n" +
                "}\n\n" +
                "###Runtime\n" +
                "Runtime test::firm::runtime::ModelChainConnectionRuntime\n" +
                "{\n" +
                "   mappings : [test::firm::model::M2MMapping];\n" +
                "   connections : \n" +
                "   [\n" +
                "       ModelStore:\n" +
                "       [\n" +
                "           c1: test::firm::connection::modelChainConnection\n" +
                "       ]\n" +
                "   ];\n" +
                "}\n" +
                "\n\n" +
                "###Mapping\n" +
                "Mapping test::firm::model::M2MMapping\n" +
                "(\n" +
                "  test::firm::model::TargetPerson2: Pure\n" +
                "  {\n" +
                "    ~src test::firm::model::TargetPerson\n" +
                "    fullName : $src.fullName + '_gen'\n" +
                "  }\n" +
                "  test::firm::model::TargetPerson: Pure\n" +
                "  {\n" +
                "    ~src test::firm::model::Person\n" +
                "    fullName : $src.firstName + ' ' + $src.lastName\n" +
                "  }\n" +
                ")" +
                "\n\n" +
                "###ExternalFormat\n" +
                "Binding test::gen::TestBinding\n" +
                "{\n" +
                "  contentType: 'application/json';\n" +
                "  modelIncludes: [\n" +
                "    test::firm::model::Person,\n" +
                "    test::firm::model::Address,\n" +
                "    test::firm::model::AddressUse,\n" +
                "    test::firm::model::GeographicPosition,\n" +
                "    test::firm::model::TargetPerson2\n" +
                "  ];\n" +
                "}";
        String targetPersonTree = "#{test::firm::model::TargetPerson2 {fullName}}#";
        String result = runTest(PureGrammarParser.newInstance().parseModel(modelGrammar),
                "{data:Byte[*]|test::firm::model::TargetPerson2.all()->graphFetch(" + targetPersonTree + ")->from(test::firm::model::M2MMapping, mergeRuntimes([test::firm::runtime::ModelChainConnectionRuntime, getRuntimeWithModelQueryConnection(test::firm::model::Person, test::gen::TestBinding, $data)]))->externalize(test::gen::TestBinding, " + targetPersonTree + ");}",
                Maps.mutable.with("data", resource("queries/peopleTestData.json")));
        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/targetPerson2Result.json")));
    }

    private ModelToJsonSchemaConfiguration toJsonSchemaConfig()
    {
        ModelToJsonSchemaConfiguration config = new ModelToJsonSchemaConfiguration();
        config.targetSchemaSet = "test::gen::TestSchemaSet";
        config.format = "JSON";
        return config;
    }
}
