// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.api.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.JsonToGrammarInput;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.LambdaInput;
import org.finos.legend.engine.language.pure.grammar.api.jsonToGrammar.TransformJsonToGrammar;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class TestJsonToGrammarApi
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final TransformJsonToGrammar grammarComposerApi = new TransformJsonToGrammar();

    @Test
    public void testIsolatedLambdas()
    {
        testIsolatedLambdaFromProtocol(
                "{\"isolatedLambdas\":{\"testLambda\":\"src: myClass[1]|if($src.property == 'abc', |'abc', |'123')\"}}",
                getJsonString("pureGenerationLambdaTest.json"),
                PureGrammarComposerContext.RenderStyle.STANDARD
        );
    }

    @Test
    public void testRenderingElementsWithoutSection()
    {
        testModelFromProtocol("###Diagram\n" +
                "import diag::a::b::*;\n" +
                "import diag::a::e::*;\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Association myAssociation\n" +
                "{\n" +
                "  a: String[1];\n" +
                "  b: a::c::A[1];\n" +
                "}\n" +
                "\n" +
                "Enum A\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Profile meta::pure::profiles::doc\n" +
                "{\n" +
                "  stereotypes: [deprecated];\n" +
                "  tags: [doc, todo];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Diagram\n" +
                "Diagram meta::pure::AnotherDiagram\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Class A2\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Measure NewMeasure\n" +
                "{\n" +
                "  *UnitOne: x -> $x;\n" +
                "}\n", "modelElementsWithoutSection.json"
        );
    }

    @Test
    public void testRenderingElementsWithoutMultipleSections()
    {
        testModelFromProtocol(
                "###Diagram\n" +
                        "import diag::a::b::*;\n" +
                        "import diag::a::e::*;\n" +
                        "Diagram meta::pure::MyDiagram\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Pure\n" +
                        "Association myAssociation\n" +
                        "{\n" +
                        "  a: String[1];\n" +
                        "  b: a::c::A[1];\n" +
                        "}\n" +
                        "\n" +
                        "Profile meta::pure::profiles::doc\n" +
                        "{\n" +
                        "  stereotypes: [deprecated];\n" +
                        "  tags: [doc, todo];\n" +
                        "}\n" +
                        "\n" +
                        "Enum A\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Pure\n" +
                        "\n" +
                        "\n" +
                        "###Diagram\n" +
                        "import diag::a::b::*;\n" +
                        "import diag::a::e::*;\n" +
                        "\n" +
                        "\n" +
                        "###Pure\n" +
                        "Enum A\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Diagram\n" +
                        "Diagram meta::pure::AnotherDiagram\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Pure\n" +
                        "Class A2\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Measure NewMeasure\n" +
                        "{\n" +
                        "  *UnitOne: x -> $x;\n" +
                        "}\n", "modelElementsWithoutMultiplesections.json");
    }

    @Test
    public void testGroupByAggStructure()
    {
        testIsolatedLambdaFromProtocol(
                "{\"isolatedLambdas\":{\"testLambda\":\"|meta::datasetMetadata::domain::physicalCatalogPURE.all()->groupBy(\\n  x: meta::datasetMetadata::domain::physicalCatalogPURE[1]|$x.owningBU,\\n  agg(x: meta::datasetMetadata::domain::physicalCatalogPURE[1]|$x.classPackage->toOne() + '::' + $x.className->toOne(), y: String[*]|$y->distinct()->count()),\\n  [\\n    'Owning BU',\\n    'fullClass Distinct Count'\\n  ]\\n)\"}}",
                getJsonString("testLambdaObj.json"),
                PureGrammarComposerContext.RenderStyle.PRETTY
        );
    }

    @Test
    public void testGroupByAggStructureHTML()
    {
        testIsolatedLambdaFromProtocol(
                "{\"isolatedLambdas\":{\"testLambda\":\"|<span class='pureGrammar-package'>meta::datasetMetadata::domain::</span><span class='pureGrammar-packageableElement'>physicalCatalogPURE</span>.<span class='pureGrammar-function'>all</span>()<span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>groupBy</span>(</BR>\\n<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-var'>x</span>: <span class='pureGrammar-package'>meta::datasetMetadata::domain::</span><span class='pureGrammar-packageableElement'>physicalCatalogPURE</span>[1]|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>owningBU</span>,</BR>\\n<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-function'>agg</span>(<span class='pureGrammar-var'>x</span>: <span class='pureGrammar-package'>meta::datasetMetadata::domain::</span><span class='pureGrammar-packageableElement'>physicalCatalogPURE</span>[1]|<span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>classPackage</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>toOne</span>() + <span class='pureGrammar-string'>'::'</span> + <span class='pureGrammar-var'>$x</span>.<span class=pureGrammar-property>className</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>toOne</span>(), <span class='pureGrammar-var'>y</span>: <span class='pureGrammar-packageableElement'>String</span>[*]|<span class='pureGrammar-var'>$y</span><span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>distinct</span>()<span class='pureGrammar-arrow'>-></span><span class='pureGrammar-function'>count</span>()),</BR>\\n<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>[</BR>\\n<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'Owning BU'</span>,</BR>\\n<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-space'></span><span class='pureGrammar-string'>'fullClass Distinct Count'</span></BR>\\n<span class='pureGrammar-space'></span><span class='pureGrammar-space'></span>]</BR>\\n)\"}}",
                getJsonString("testLambdaObj.json"),
                PureGrammarComposerContext.RenderStyle.PRETTY_HTML
        );
    }

    @Test
    public void testSimpleModelMapping()
    {
        String expected = "###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_S_Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "  }\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Product2Simple[meta_pure_mapping_modelToModel_test_shared_dest_Product2Simple]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Product2\n" +
                "    name: $src.name,\n" +
                "    region: $src.region\n" +
                "  }\n" +
                ")\n";
        testMappingFromProtocol(expected, "simpleModelMapping.json");
    }


    @Test
    public void testDeepFetchTreeWithFormatting()
    {
        testIsolatedLambdaFromProtocol(
                "{\"isolatedLambdas\":{\"testLambdas\":\"|model::complex::L1.all()->graphFetchChecked(\\n  #{\\n    model::complex::L1{\\n      name1,\\n      l14{\\n        name4,\\n        l42{\\n          name2,\\n          l23_1{\\n            name3\\n          },\\n          l23{\\n            name3\\n          }\\n        },\\n        l45{\\n          name3\\n        }\\n      },\\n      l12{\\n        name2,\\n        l23_1{\\n          name3\\n        },\\n        l23{\\n          name3\\n        }\\n      },\\n      l13{\\n        name3\\n      }\\n    }\\n  }#\\n)->serialize(\\n  #{\\n    model::complex::L1{\\n      name1,\\n      l14{\\n        name4,\\n        l42{\\n          name2,\\n          l23_1{\\n            name3\\n          },\\n          l23{\\n            name3\\n          }\\n        },\\n        l45{\\n          name3\\n        }\\n      },\\n      l12{\\n        name2,\\n        l23_1{\\n          name3\\n        },\\n        l23{\\n          name3\\n        }\\n      },\\n      l13{\\n        name3\\n      }\\n    }\\n  }#\\n)\"}}",
                getJsonString("testGraphFetchTreeLambda.json"),
                PureGrammarComposerContext.RenderStyle.PRETTY
        );
    }

    @Test
    public void testLambdaWithCast()
    {
        testIsolatedLambdaFromProtocol("{\"isolatedLambdas\":{\"testLambda\":\"src: OldClass[1]|$src->cast(@newClass)\"}}",
                "{\"lambdas\":{\"testLambda\":{\"_type\":\"lambda\",\"body\":[{\"_type\":\"func\",\"function\":\"cast\",\"parameters\":[{\"_type\":\"var\",\"name\":\"src\",\"sourceInformation\":{\"endColumn\":20,\"endLine\":1,\"sourceId\":\"testLambda\",\"startColumn\":17,\"startLine\":1}},{\"_type\":\"hackedClass\",\"fullPath\":\"newClass\",\"sourceInformation\":{\"endColumn\":36,\"endLine\":1,\"sourceId\":\"testLambda\",\"startColumn\":29,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":26,\"endLine\":1,\"sourceId\":\"testLambda\",\"startColumn\":23,\"startLine\":1}}],\"parameters\":[{\"_type\":\"var\",\"class\":\"OldClass\",\"multiplicity\":{\"lowerBound\":1,\"upperBound\":1},\"name\":\"src\",\"sourceInformation\":{\"endColumn\":15,\"endLine\":1,\"sourceId\":\"testLambda\",\"startColumn\":4,\"startLine\":1}}],\"sourceInformation\":{\"endColumn\":37,\"endLine\":1,\"sourceId\":\"testLambda\",\"startColumn\":16,\"startLine\":1}}}}",
                PureGrammarComposerContext.RenderStyle.STANDARD);
    }

    @Test
    public void testEnumerationMappingWithNoSourceValueType()
    {
        String expected = "Enum test::model::E1\n" +
                "{\n" +
                "  TP1,\n" +
                "  TP2\n" +
                "}\n" +
                "\n" +
                "Enum test::model::E2\n" +
                "{\n" +
                "  MP1,\n" +
                "  MP2,\n" +
                "  MP3\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::mapping::enumerationMapping\n" +
                "(\n" +
                "  test::model::E1: EnumerationMapping E1Mapping\n" +
                "  {\n" +
                "    TP1: ['MP1'],\n" +
                "    TP2: ['MP2', 'MP3']\n" +
                "  }\n" +
                "  test::model::E2: EnumerationMapping E2Mapping\n" +
                "  {\n" +
                "    MP1: [10],\n" +
                "    MP2: [20],\n" +
                "    MP3: [30, 40]\n" +
                "  }\n" +
                ")\n";
        testModelFromProtocol(expected, "enumerationMappingWithNoSourceValueType.json");
    }

    @Test
    public void testEnumerationMapping()
    {
        String expected = "Enum test::model::E1\n" +
                "{\n" +
                "  TP1,\n" +
                "  TP2\n" +
                "}\n" +
                "\n" +
                "Enum test::model::E2\n" +
                "{\n" +
                "  MP1,\n" +
                "  MP2,\n" +
                "  MP3\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::mapping::enumerationMapping\n" +
                "(\n" +
                "  test::model::E1: EnumerationMapping E1Mapping\n" +
                "  {\n" +
                "    TP1: ['MP1'],\n" +
                "    TP2: ['MP2', 'MP3']\n" +
                "  }\n" +
                "  test::model::E1: EnumerationMapping E1Mapping2\n" +
                "  {\n" +
                "    TP1: [test::model::E2.MP1],\n" +
                "    TP2: [test::model::E2.MP2, test::model::E2.MP3]\n" +
                "  }\n" +
                "  test::model::E2: EnumerationMapping E2Mapping\n" +
                "  {\n" +
                "    MP1: [10],\n" +
                "    MP2: [20],\n" +
                "    MP3: [30, 40]\n" +
                "  }\n" +
                ")\n";
        testModelFromProtocol(expected, "enumerationMapping.json");
    }

    @Test
    public void testUnionModelMapping()
    {
        String expected = "###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::union::unionModelMapping\n" +
                "(\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Person[meta_pure_mapping_modelToModel_test_shared_dest_Person]: Operation\n" +
                "  {\n" +
                "    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(p1,p2)\n" +
                "  }\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Firm[meta_pure_mapping_modelToModel_test_shared_dest_Firm]: Operation\n" +
                "  {\n" +
                "    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(f1,f2)\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::shared::dest::Firm[f1]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Firm\n" +
                "    legalName: 'f1 / ' + $src.name\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::shared::dest::Firm[f2]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Firm\n" +
                "    legalName: 'f2 / ' + $src.name\n" +
                "  }\n" +
                "  *meta::pure::mapping::modelToModel::test::shared::dest::Address[meta_pure_mapping_modelToModel_test_shared_dest_Address]: Pure\n" +
                "  {\n" +
                "    street: 'streetConstant'\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::shared::dest::Person[p1]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Person\n" +
                "    ~filter $src.fullName->startsWith('Johny')\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    addresses[meta_pure_mapping_modelToModel_test_shared_dest_Address]: $src.addresses,\n" +
                "    firm[f1]: $src.firm\n" +
                "  }\n" +
                "  meta::pure::mapping::modelToModel::test::shared::dest::Person[p2]: Pure\n" +
                "  {\n" +
                "    ~src meta::pure::mapping::modelToModel::test::shared::src::_Person\n" +
                "    ~filter $src.fullName->startsWith('_')\n" +
                "    firstName: 'N/A',\n" +
                "    lastName: 'N/A',\n" +
                "    firm[f2]: $src.firm\n" +
                "  }\n" +
                ")\n";
        testMappingFromProtocol(expected, "unionModelMapping.json");
    }


    @Test
    public void testFunctionWithUnderscores()
    {
        String expected = "Class my::underscore_package::ClassA\n" +
                "[\n" +
                "  c: $this.prop->my::underscore_package::constraint_fn()\n" +
                "]\n" +
                "{\n" +
                "  prop: String[1];\n" +
                "}\n" +
                "\n" +
                "function my::underscore_package::constraint_fn(value: String[1]): Boolean[1]\n" +
                "{\n" +
                "   $value->startsWith('A')\n" +
                "}\n";
        testModelFromProtocol(expected, "functionWithUnderscores.json");

    }

    @Test
    public void testDateFromProtocol()
    {
        String expected =
                "function my::example::compareDate(): Boolean[1]\n" +
                        "{\n" +
                        "   %2020-01-01 < %2020-01-02\n" +
                        "}\n";
        testModelFromProtocol(expected, "functionWithDate.json");
        testModelFromProtocol(expected, "functionWithDateContainingPercent.json");
    }

    @Test
    public void testFunctionNameWithoutParameters()
    {
        String expected =
                "function domainModel::migration::test::account::getRowsResult(): meta::pure::metamodel::type::Any[*]\n" +
                        "{\n" +
                        "   1\n" +
                        "}\n";
        testModelFromProtocol(expected, "simpleFunctionWithoutParameters.json");
    }

    @Test
    public void testFunctionNameWithParameters()
    {
        String expected ="function domainModel::migration::test::account::getRowsResult(a: String[1]): meta::pure::metamodel::type::Any[*]\n" +
                "{\n" +
                "   1\n" +
                "}\n";
        testModelFromProtocol(expected, "simpleFunctionWithParameters.json");
    }

    private void testMappingFromProtocol(String expected, String protocolPath)
    {
        try
        {
            Mapping map = objectMapper.readValue(getJsonString(protocolPath), Mapping.class);
            PureModelContextData pureModelContextData = new PureModelContextData.Builder().withElement(map).build();
            PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
            String pureCode = grammarTransformer.renderPureModelContextData(pureModelContextData);
            assertEquals(expected, pureCode);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void testModelFromProtocol(String expected, String protocolPath)
    {
        try
        {
            PureModelContextData pureModelContextData = objectMapper.readValue(getJsonString(protocolPath), PureModelContextData.class);
            PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
            String pureCode = grammarTransformer.renderPureModelContextData(pureModelContextData);
            assertEquals(expected, pureCode);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void testIsolatedLambdaFromProtocol(String expectedOutput, String protocolData, PureGrammarComposerContext.RenderStyle renderStyle)
    {
        try
        {
            LambdaInput lambda = objectMapper.readValue(protocolData, LambdaInput.class);
            JsonToGrammarInput input = new JsonToGrammarInput(lambda);
            input.renderStyle = renderStyle;
            String actual = grammarComposerApi.transformJsonToGrammar(input, null).getEntity().toString();
            assertEquals(expectedOutput, actual);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private String getJsonString(String path)
    {
        return new java.util.Scanner(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path), "Can't find resource '" + path + "'")).useDelimiter("\\A").next();
    }
}