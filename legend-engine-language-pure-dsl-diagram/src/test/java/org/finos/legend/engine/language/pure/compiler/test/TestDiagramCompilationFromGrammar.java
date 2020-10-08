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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

public class TestDiagramCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Diagram\n" +
                "Diagram anything::class\n" +
                "{\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::class'";
    }

    @Test
    public void testFaultyDiagramClassViewReferringUnknownClass()
    {
        test("###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView firstTimeIsNotAlwaysTheHardest\n" +
                "  {\n" +
                "    class: anything::goes;\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    hideProperties: true;\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [6:12-25]: Can't find class 'anything::goes'"
        );
    }

    @Test
    public void testFaultyDiagramPropertyViewReferringUnknownProperty()
    {
        test("Class anything::goes\n" +
                "{\n" +
                "   s:String[1];\n" +
                "}\n" +
                "###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView ola\n" +
                "  {\n" +
                "    class: anything::goes;\n" +
                "    position: (-123.123,233);\n" +
                "    rectangle: (+12,-0);\n" +
                "    hideProperties: true;\n" +
                "  }\n" +
                "  classView otis\n" +
                "  {\n" +
                "    class: anything::goes;\n" +
                "    position: (.123,2.2e2);\n" +
                "    rectangle: (-123,23.23);\n" +
                "    hideProperties: true;\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: anything::goes.milk;\n" +
                "    source: ola;\n" +
                "    target: otis;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [24:15-28]: Can't find property 'milk' in class 'anything::goes'"
        );
    }

    @Test
    public void testFaultyDiagramGeneralizationViewReferringUnknownClassViewAsSource()
    {
        test("Class anything::goes\n" +
                "{\n" +
                "   s:String[1];\n" +
                "}\n" +
                "###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView ola\n" +
                "  {\n" +
                "    class: anything::goes;\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    hideProperties: true;\n" +
                "  }\n" +
                "  classView otis\n" +
                "  {\n" +
                "    class: anything::goes;\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    hideProperties: true;\n" +
                "  }\n" +
                "  generalizationView\n" +
                "  {\n" +
                "    source: 86a8a6a1-5353-48f1-b058-199ddd7cbe5a;\n" +
                "    target: otis;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [24:13-48]: Can't find source class view '86a8a6a1-5353-48f1-b058-199ddd7cbe5a'"
        );
    }

    @Test
    public void testFaultyDiagramGeneralizationViewReferringUnknownClassViewAsTarget()
    {
        test("Class anything::goes\n" +
                "{\n" +
                "   s:String[1];\n" +
                "}\n" +
                "###Diagram\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView ola\n" +
                "  {\n" +
                "    class: anything::goes;\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    hideProperties: true;\n" +
                "  }\n" +
                "  classView otis\n" +
                "  {\n" +
                "    class: anything::goes;\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    hideProperties: true;\n" +
                "  }\n" +
                "  generalizationView\n" +
                "  {\n" +
                "    source: ola;\n" +
                "    target: otiss;\n" +
                "    points: [(.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [25:13-17]: Can't find target class view 'otiss'"
        );
    }

    @Test
    public void testDiagramWithQualifiedPropertyView()
    {
        test("Class animal::Animal\n" +
                "{\n" +
                "  owner: user::User[1];\n" +
                "  a(){$this.owner}:user::User[1];\n" +
                "}\n" +
                "\n" +
                "Class user::User\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "###Diagram\n" +
                "Diagram animal::test\n" +
                "{\n" +
                "  classView 51a08024-0583-457f-81f8-c028c4cff247\n" +
                "  {\n" +
                "    class: user::User;\n" +
                "    position: (817.156494140625,602.2708129882812);\n" +
                "    rectangle: (135.53759765625,103.0);\n" +
                "  }\n" +
                "  classView 464a4e74-60b8-4dd5-875e-e983aedf933a\n" +
                "  {\n" +
                "    class: animal::Animal;\n" +
                "    position: (399.0,251.90884399414062);\n" +
                "    rectangle: (233.31298828125,211.0);\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: animal::Animal.owner;\n" +
                "    source: 464a4e74-60b8-4dd5-875e-e983aedf933a;\n" +
                "    target: 51a08024-0583-457f-81f8-c028c4cff247;\n" +
                "    points: [];\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: animal::Animal.a;\n" +
                "    source: 464a4e74-60b8-4dd5-875e-e983aedf933a;\n" +
                "    target: 51a08024-0583-457f-81f8-c028c4cff247;\n" +
                "    points: [];\n" +
                "  }\n" +
                "}\n"
        );
    }

    @Test
    public void testDiagramWithImport()
    {
        test("Class anything::goes\n" +
                "{\n" +
                "  name: String[*];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Diagram\n" +
                "import anything::*;\n" +
                "Diagram meta::pure::MyDiagram\n" +
                "{\n" +
                "  classView 1a0ac401-d443-4388-8ade-08a458f735e9\n" +
                "  {\n" +
                // class view class
                "    class: goes;\n" +
                "    position: (123.123,23.23);\n" +
                "    rectangle: (123.123,23.23);\n" +
                "    hideProperties: true;\n" +
                "  }\n" +
                "  propertyView\n" +
                "  {\n" +
                "    property: goes.name;\n" +
                "    source: 1a0ac401-d443-4388-8ade-08a458f735e9;\n" +
                "    target: 1a0ac401-d443-4388-8ade-08a458f735e9;\n" +
                "    points: [(123.123,23.23),(123.123,23.23)];\n" +
                "  }\n" +
                "}\n");
    }
}
