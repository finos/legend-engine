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

package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestDiagramGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testDiagram()
    {
        String formatted = "###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                // NOTE: here we try some UUID to see if those are picked up properly by the parser
                "  classView 1a0ac401-d443-4388-8ade-08a458f735e9\n" +
                "  {\n" +
                "    class: any::'thing goes';\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    hideProperties: true;\n" +
                "  }\n" +
                "  classView firstTimeIsNotAlwaysTheHardest\n" +
                "  {\n" +
                "    class: any::'thing goes';\n" +
                // NOTE: here we try passing different kinds of number to see if the parser handle this well
                // we also have to make sure it parses integer, however since this is round-trip test, we can
                // only try with decimals here
                "    position: (-123.123,23.0);\n" +
                "    rectangle: (12.23,-90.0);\n" +
                "    hideProperties: true;\n" +
                "    hideTaggedValue: true;\n" +
                "    hideStereotype: true;\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: any::'thing goes'.'on Monday';\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    target: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    points: [(0.3,2.23E-8),(2.23E10,23.23)];\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: any::'thing goes'.'on Monday';\n" +
                "    source: sourceId;\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "  generalizationView\n" +
                "  {\n" +
                "    source: sourceId;\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "Diagram meta::pure::AnotherDiagram\n" +
                "{\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: anything::goes.milk;\n" +
                "    source: sourceId;\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: anything::goes.milk;\n" +
                "    source: sourceId;\n" +
                "    target: ranOutOfCreativityHere;\n" +
                // line with only one point
                "    points: [(123.123,23.23)];\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: anything::goes.milk;\n" +
                "    source: sourceId;\n" +
                "    target: ranOutOfCreativityHere;\n" +
                // line with no point
                "    points: [];\n" +
                "  }\n" +
                "}\n";

        String unformatted = "###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView 1a0ac401-d443-4388-8ade-08a458f735e9\n" +
                "  {\n" +
                "    class: any::'thing goes';\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    hideProperties: true;\n" +
                "  }\n" +
                "  classView firstTimeIsNotAlwaysTheHardest\n" +
                "  {\n" +
                "    class: any::'thing goes';\n" +
                "    position: (-123.123,23);\n" + // intentionally try to throw off the number format
                "    rectangle: (12.23,-90);\n" + // intentionally try to throw off the number format
                "    hideProperties: true;\n" +
                "    hideTaggedValue: true;\n" +
                "    hideStereotype: true;\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: any::'thing goes'.'on Monday';\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    target: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    points: [(0.3,22.3E-9),(2.23E10,23.23)];\n" + // intentionally try to throw off the number format
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: any::'thing goes'.'on Monday';\n" +
                "    source: sourceId;\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "  generalizationView\n" +
                "  {\n" +
                "    source: sourceId;\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "Diagram meta::pure::AnotherDiagram\n" +
                "{\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: anything::goes.milk;\n" +
                "    source: sourceId;\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: anything::goes.milk;\n" +
                "    source: sourceId;\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23)];\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: anything::goes.milk;\n" +
                "    source: sourceId;\n" +
                "    target: ranOutOfCreativityHere;\n" +
                // line with no point
                "    points: [];\n" +
                "  }\n" +
                "}\n";
        testFormat(formatted, unformatted);
    }

    @Test
    public void testDiagramWithImport()
    {
        test("###Diagram\n" +
                "import anything::*;\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "}\n");
    }
}
