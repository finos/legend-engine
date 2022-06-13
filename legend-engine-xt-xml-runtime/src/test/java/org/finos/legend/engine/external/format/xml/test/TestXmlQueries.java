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
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class TestXmlQueries extends TestExternalFormatQueries
{
    @Test
    public void testDeserializeSingleFirmUsingAttributesWithoutSchema()
    {
        String grammar = firmModel() + firmSelfMapping() + schemalessBinding() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::firm::Binding");
        String result = runTest(grammar,
                "|test::firm::model::Firm.all()->graphFetchChecked(" + firmTree() + ")->serialize(" + firmTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/oneFirm.xml"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/oneFirmCheckedResult.json")));
    }

    @Test
    public void testDeserializeMultipleFirmsUsingAttributesWithoutSchema()
    {
        String grammar = firmModel() + firmSelfMapping() + schemalessBinding() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::firm::Binding");
        String result = runTest(grammar,
                "|test::firm::model::Firm.all()->graphFetchChecked(" + firmTree() + ")->serialize(" + firmTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/manyFirmsAttributes.xml"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/manyFirmsAttributesCheckedResult.json")));
    }

    @Test
    public void testDeserializeMultipleFirmsUsingElementsWithoutSchema()
    {
        String grammar = firmModel() + firmSelfMapping() + schemalessBinding() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::firm::Binding");
        String result = runTest(grammar,
                "|test::firm::model::Firm.all()->graphFetchChecked(" + firmTree() + ")->serialize(" + firmTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/manyFirmsElements.xml"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/manyFirmsElementsCheckedResult.json")));
    }

    @Test
    public void testDeserializeMultipleFirmsUsingElementsWithoutSchemaUnwrapped()
    {
        String grammar = firmModel() + firmSelfMapping() + schemalessBinding() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::firm::Binding");
        String result = runTest(grammar,
                "|test::firm::model::Firm.all()->graphFetch(" + firmTree() + ")->serialize(" + firmTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/manyFirmsElements.xml"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/manyFirmsElementsObjectResult.json")));
    }

    @Test
    public void testDeserializeFullGraphWithoutSchema()
    {
        String grammar = firmModel() + firmSelfMapping() + schemalessBinding() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::firm::Binding");
        String result = runTest(grammar,
                "|test::firm::model::Firm.all()->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/fullFirm.xml"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/fullFirmCheckedResult.json")));
    }

    @Test
    public void testDeserializeFullGraphWithoutSchemaUnwrapped()
    {
        String grammar = firmModel() + firmSelfMapping() + schemalessBinding() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::firm::Binding");
        String result = runTest(grammar,
                "|test::firm::model::Firm.all()->graphFetch(" + fullTree() + ")->serialize(" + fullTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/fullFirm.xml"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/fullFirmUnwrappedResult.json")));
    }

    @Test
    public void testDeserializeInvalidData()
    {
        String grammar = firmModel() + firmSelfMapping() + schemalessBinding() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::firm::Binding");
        String result1 = runTest(grammar,
                "|test::firm::model::Firm.all()->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/firmInvalidRanking.xml"));

        MatcherAssert.assertThat(result1, JsonMatchers.jsonEquals(resourceReader("queries/firmInvalidRankingCheckedResult.json")));

        String result2 = runTest(grammar,
                "|test::firm::model::Firm.all()->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/firmInvalidLongitude.xml"));

        MatcherAssert.assertThat(result2, JsonMatchers.jsonEquals(resourceReader("queries/firmInvalidLongitudeCheckedResult.json")));
    }

    @Test
    public void testDeserializeConstraintViolation()
    {
        String grammar = firmModel() + firmSelfMapping() + schemalessBinding() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::firm::Binding");
        String result = runTest(grammar,
                "|test::firm::model::Firm.all()->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/firmLongitudeConstraintViolation.xml"));

        MatcherAssert.assertThat(result, JsonMatchers.jsonEquals(resourceReader("queries/firmLongitudeConstraintViolationCheckedResult.json")));
    }

    @Test
    public void testDeserializeWithXsiNil()
    {
        String grammar = firmModel() + firmSelfMapping() + schemalessBinding() + urlStreamRuntime("test::firm::mapping::SelfMapping", "test::firm::Binding");
        String result = runTest(grammar,
                "|test::firm::model::Firm.all()->graphFetchChecked(" + fullTree() + ")->serialize(" + fullTree() + ")",
                "test::firm::mapping::SelfMapping",
                "test::runtime",
                resource("queries/firmWithXsiNil.xml"));

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
}
