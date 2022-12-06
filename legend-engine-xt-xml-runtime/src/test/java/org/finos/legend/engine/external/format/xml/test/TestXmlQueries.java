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

package org.finos.legend.engine.external.format.xml.test;

import net.javacrumbs.jsonunit.JsonMatchers;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.core_external_format_xml_externalFormatContract;
import org.finos.legend.pure.generated.core_external_format_xml_java_platform_binding_legendJavaPlatformBinding_descriptor;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

public class TestXmlQueries extends TestExternalFormatQueries
{
    @BeforeClass
    public static void setup()
    {
        ExecutionSupport executionSupport = Compiler.compile(PureModelContextData.newPureModelContextData(), null, null).getExecutionSupport();
        formatExtensions = Collections.singletonList(core_external_format_xml_externalFormatContract.Root_meta_external_format_xml_extension_xsdFormatExtension__Extension_1_(executionSupport));
        formatDescriptors = Collections.singletonList(core_external_format_xml_java_platform_binding_legendJavaPlatformBinding_descriptor.Root_meta_external_format_xml_executionPlan_platformBinding_legendJava_xsdJavaBindingDescriptor__ExternalFormatLegendJavaPlatformBindingDescriptor_1_(executionSupport));
    }

    @Test
    public void testInternalizeWithDynamicByteStream()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->checked()->serialize(" + firmTree() + ")",
                Maps.mutable.with("data", resource("queries/oneFirm.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/oneFirmCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithDynamicString()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result = runTest(modelData,
                "data:String[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->checked()->serialize(" + firmTree() + ")",
                Maps.mutable.with("data", resourceAsString("queries/oneFirm.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/oneFirmCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithStaticString()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String data = resourceAsString("queries/oneFirm.xml").replace("\n", "\\n").replace("'", "\\'");
        String result = runTest(modelData,
                "|test::firm::model::Firm->internalize(test::firm::Binding, '" + data + "')->checked()->serialize(" + firmTree() + ")");

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/oneFirmCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithGraphFetch()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->graphFetch(" + firmTree() + ")->serialize(" + firmTree() + ")",
                Maps.mutable.with("data", resource("queries/manyFirmsElements.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/manyFirmsElementsObjectResult.json")));
    }

    @Test
    public void testInternalizeWithGraphFetchAndDefects()
    {
        String grammar = firmModel() + positionBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#"; // latitude property skipped on purpose to test graphFetch expands tree scope to include constraint on latitude

        try
        {
            runTest(modelData,
                    "data:ByteStream[1]|test::firm::model::GeographicPosition->internalize(test::firm::Binding, $data)->graphFetch(" + positionTree + ")->serialize(" + positionTree + ")",
                    Maps.mutable.with("data", resource("queries/positions.xml")));
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
        String grammar = firmModel() + positionBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#"; // latitude property skipped on purpose to test graphFetch expands tree scope to include constraint on latitude


        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::GeographicPosition->internalize(test::firm::Binding, $data)->graphFetchChecked(" + positionTree + ")->serialize(" + positionTree + ")",
                Maps.mutable.with("data", resource("queries/positions.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/positionGraphFetchCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithGraphFetchUnexpanded()
    {
        String grammar = firmModel() + positionBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#";


        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::GeographicPosition->internalize(test::firm::Binding, $data)->graphFetchUnexpanded(" + positionTree + ")->serialize(" + positionTree + ")",
                Maps.mutable.with("data", resource("queries/positions.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/positionGraphFetchUnexpandedResult.json")));
    }

    @Test
    public void testInternalizeWithGraphFetchUnexpandedChecked()
    {
        String grammar = firmModel() + positionBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#";


        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::GeographicPosition->internalize(test::firm::Binding, $data)->graphFetchCheckedUnexpanded(" + positionTree + ")->serialize(" + positionTree + ")",
                Maps.mutable.with("data", resource("queries/positions.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/positionGraphFetchCheckedUnexpandedResult.json")));
    }

    @Test
    public void testInternalizeMultipleFirmsUsingAttributesWithoutSchema()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->graphFetchChecked(" + firmTree() + ")->serialize(" + firmTree() + ")",
                Maps.mutable.with("data", resource("queries/manyFirmsAttributes.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/manyFirmsAttributesCheckedResult.json")));
    }

    @Test
    public void testInternalizeMultipleFirmsUsingElementsWithoutSchema()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->graphFetchChecked(" + firmTree() + ")->serialize(" + firmTree() + ")",
                Maps.mutable.with("data", resource("queries/manyFirmsElements.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/manyFirmsElementsCheckedResult.json")));
    }

    @Test
    public void testInternalizeMultipleFirmsUsingElementsWithoutSchemaUnwrapped()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->graphFetch(" + firmTree() + ")->serialize(" + firmTree() + ")",
                Maps.mutable.with("data", resource("queries/manyFirmsElements.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/manyFirmsElementsObjectResult.json")));
    }

    @Test
    public void testInternalizeFullGraphWithoutSchema()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                Maps.mutable.with("data", resource("queries/fullFirm.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/fullFirmCheckedResult.json")));
    }

    @Test
    public void testInternalizeFullGraphWithoutSchemaUnwrapped()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->graphFetch(" + fullTree() + ")->serialize(" + fullTree() + ")",
                Maps.mutable.with("data", resource("queries/fullFirm.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/fullFirmUnwrappedResult.json")));
    }

    @Test
    public void testInternalizeInvalidData()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result1 = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                Maps.mutable.with("data", resource("queries/firmInvalidRanking.xml")));

        MatcherAssert.assertThat(result1, JsonMatchers.jsonEquals(resourceReader("queries/firmInvalidRankingCheckedResult.json")));

        String result2 = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                Maps.mutable.with("data", resource("queries/firmInvalidLongitude.xml")));

        MatcherAssert.assertThat(result2, JsonMatchers.jsonEquals(resourceReader("queries/firmInvalidLongitudeCheckedResult.json")));
    }

    @Test
    public void testInternalizeConstraintViolation()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                Maps.mutable.with("data", resource("queries/firmLongitudeConstraintViolation.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/firmLongitudeConstraintViolationCheckedResult.json")));
    }

    @Test
    public void testInternalizeWithXsiNil()
    {
        String grammar = firmModel() + schemalessBinding();
        PureModelContextData modelData = PureGrammarParser.newInstance().parseModel(grammar);

        String result = runTest(modelData,
                "data:ByteStream[1]|test::firm::model::Firm->internalize(test::firm::Binding, $data)->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                Maps.mutable.with("data", resource("queries/firmWithXsiNil.xml")));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/firmWithXsiNilCheckedResult.json")));
    }

    private String schemalessBinding()
    {
        return "###ExternalFormat\n" +
                "Binding test::firm::Binding\n" +
                "{\n" +
                "  contentType: 'application/xml';\n" +
                "  modelIncludes: [ test::firm::model ];\n" +
                "}\n";
    }

    private String positionBinding()
    {
        return "###ExternalFormat\n" +
                "Binding test::firm::Binding\n" +
                "{\n" +
                "  contentType: 'application/xml';\n" +
                "  modelIncludes: [ test::firm::model::GeographicPosition ];\n" +
                "}\n";
    }
}
