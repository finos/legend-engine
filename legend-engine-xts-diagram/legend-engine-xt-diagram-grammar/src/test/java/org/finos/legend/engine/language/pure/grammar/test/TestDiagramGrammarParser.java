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

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DiagramParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestDiagramGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DiagramParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Diagram\n" +
                "Diagram " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "}\n";
    }

    @Test
    public void testDiagramClassView()
    {
        // Missing fields
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView 1a0ac401-d443-4388-8ade-08a458f735e9\n" +
                "  {\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-8:3]: Field 'class' is required");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView 1a0ac401-d443-4388-8ade-08a458f735e9\n" +
                "  {\n" +
                "    position: (123.123,23.23);\n" +
                "    class: anything::goes;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-8:3]: Field 'rectangle' is required");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView 1a0ac401-d443-4388-8ade-08a458f735e9\n" +
                "  {\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    class: anything::goes;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-8:3]: Field 'position' is required");
        // Duplicated fields
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView 1a0ac401-d443-4388-8ade-08a458f735e9\n" +
                "  {\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    class: anything::goes;\n" +
                "    class: anything::goes;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-10:3]: Field 'class' should be specified only once");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView 1a0ac401-d443-4388-8ade-08a458f735e9\n" +
                "  {\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    class: anything::goes;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-10:3]: Field 'rectangle' should be specified only once");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView 1a0ac401-d443-4388-8ade-08a458f735e9\n" +
                "  {\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    position: (123.123,23.23);\n" +
                "    class: anything::goes;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-10:3]: Field 'position' should be specified only once");
    }

    @Test
    public void testDiagramPropertyView()
    {
        // Missing fields
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  propertyView\n" +
                "  {\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    points: [(0.3,2.23E-8),(2.23E10,23.23)];\n" +
                "    target: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-9:3]: Field 'property' is required");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  propertyView\n" +
                "  {\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    points: [(0.3,2.23E-8),(2.23E10,23.23)];\n" +
                "    property: anything::goes.milk;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-9:3]: Field 'target' is required");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  propertyView\n" +
                "  {\n" +
                "    points: [(0.3,2.23E-8),(2.23E10,23.23)];\n" +
                "    target: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    property: anything::goes.milk;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-9:3]: Field 'source' is required");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  propertyView\n" +
                "  {\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    target: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    property: anything::goes.milk;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-9:3]: Field 'points' is required");
        // Duplicated fields
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  propertyView\n" +
                "  {\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    target: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    property: anything::goes.milk;\n" +
                "    property: anything::goes.milk;\n" +
                "    points: [(0.3,2.23E-8),(2.23E10,23.23)];\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-11:3]: Field 'property' should be specified only once");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  propertyView\n" +
                "  {\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    target: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    target: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    property: anything::goes.milk;\n" +
                "    points: [(0.3,2.23E-8),(2.23E10,23.23)];\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-11:3]: Field 'target' should be specified only once");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  propertyView\n" +
                "  {\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    target: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    property: anything::goes.milk;\n" +
                "    points: [(0.3,2.23E-8),(2.23E10,23.23)];\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-11:3]: Field 'source' should be specified only once");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  propertyView\n" +
                "  {\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    points: [(0.3,2.23E-8),(2.23E10,23.23)];\n" +
                "    target: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    property: anything::goes.milk;\n" +
                "    points: [(0.3,2.23E-8),(2.23E10,23.23)];\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-11:3]: Field 'points' should be specified only once");
    }

    @Test
    public void testDiagramGeneralizationView()
    {
        // Missing fields
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  generalizationView\n" +
                "  {\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "    source: sourceId;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-8:3]: Field 'target' is required");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  generalizationView\n" +
                "  {\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    source: sourceId;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-8:3]: Field 'points' is required");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  generalizationView\n" +
                "  {\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-8:3]: Field 'source' is required");
        // Duplicated fields
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  generalizationView\n" +
                "  {\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "    source: sourceId;\n" +
                "    source: sourceId;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-10:3]: Field 'source' should be specified only once");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  generalizationView\n" +
                "  {\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    source: sourceId;\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-10:3]: Field 'target' should be specified only once");
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  generalizationView\n" +
                "  {\n" +
                "    target: ranOutOfCreativityHere;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "    source: sourceId;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "}\n", "PARSER error at [4:3-10:3]: Field 'points' should be specified only once");
    }
}
