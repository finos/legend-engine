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

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestMappingCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Mapping\n" +
                "Mapping anything::class\n" +
                "(\n" +
                ")\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::class'";
    }

    @Test
    public void testCycleMappingInclude()
    {
        String models = "Class test::A extends test::B {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  currency: String[*];\n" +
                "}\n" +
                "\n";
        test(models +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   include test::M1\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n", "COMPILATION error at [15:1-21:1]: Cycle detected in mapping include hierarchy: test::M1 -> test::M1");
        test(models +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   include test::M2\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M2 (\n" +
                "   include test::M1\n" +
                "   *test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n", "COMPILATION error at [15:1-21:1]: Cycle detected in mapping include hierarchy: test::M1 -> test::M2 -> test::M1");
        test(models +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   include test::M2\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M2 (\n" +
                "   include test::M3\n" +
                "   *test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n", "COMPILATION error at [15:1-21:1]: Cycle detected in mapping include hierarchy: test::M1 -> test::M2 -> test::M3 -> test::M1");
    }

    @Test
    public void testDuplicatedMappingInclude()
    {
        test("Class test::A extends test::B {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  currency: String[*];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M2 (\n" +
                "   include test::M1\n" +
                "   include test::M1\n" +
                "   *test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n", "COMPILATION error at [22:1-29:1]: Duplicated mapping include 'test::M1' in mapping 'test::M2'"
        );
    }

    @Test
    public void testFaultyMappingInclude()
    {
        test("###Mapping\n" +
                "Mapping test::MyMapping (\n" +
                "   include test::MissingMapping\n" +
                ")\n" +
                "\n", "COMPILATION error at [3:4-31]: Can't find mapping 'test::MissingMapping'");
    }

    @Test
    public void testMappingExplosion()
    {
        test("Class test::Firm\n" +
                "{" +
                "   name : String[1];\n" +
                "   employees : test::Person[*];\n" +
                "}" +
                "Class test::Person\n" +
                "{\n" +
                "   fullName : String[1];\n" +
                "}\n" +
                "Class test::FirmEmployee\n" +
                "{\n" +
                "   \n" +
                "   firmName : String[1];\n" +
                "   fullName : String[1];\n" +
                "   \n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping a::createInstancesModelMappingOneMany\n" +
                "(\n" +
                "   \n" +
                "   test::FirmEmployee : Pure\n" +
                "            {\n" +
                "               ~src test::Firm\n" +
                "               firmName : $src.name,\n" +
                "               fullName* : $src.employees.fullName\n" +
                "            }\n" +
                "   \n" +
                ")");
    }

    @Test
    public void testClassMappingsRootsCount()
    {
        String models = "Class test::A  {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   *test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   *test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n";
        test(models + "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                "   test::A[f]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")", "COMPILATION error at [23:1-34:1]: Class 'test::A' is mapped by 2 set implementations and has 0 roots. There should be exactly one root set implementation for the class, and it should be marked with a '*'");
        test(models + "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   *test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                "   *test::A[f]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")", "COMPILATION error at [23:1-34:1]: Class 'test::A' is mapped by 2 set implementations and has 2 roots. There should be exactly one root set implementation for the class, and it should be marked with a '*'");
        test(models + "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   *test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                "   test::A[f]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")");
        test(models + "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   *test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")");
    }

    @Test
    public void testDuplicatedClassMappingIds()
    {
        String models = "Class test::A  {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_A {\n" +
                "  prop1: String[1];\n" +
                "}\n" +
                "\n";
        // duplicated IDs within the same mapping
        test(models +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   *test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   *test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                "   test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")", "COMPILATION error at [23:1-34:1]: Duplicated class mappings found with ID 'test_A' in mapping 'test::M3'");
        // duplicated IDs within the included mappings
        test(models +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   *test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   *test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                "   test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")", "COMPILATION error at [23:1-34:1]: Duplicated class mappings found with ID 'test_A' in mapping 'test::M3'");
        // duplicated IDs in the included mappings and mapping
        test(models +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   *test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   *test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                "   test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")", "COMPILATION error at [23:1-34:1]: Duplicated class mappings found with ID '2' in mapping 'test::M3'");
        // duplicated IDs in nested included mappings
        test(models +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   *test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   include test::M1\n" +
                "   *test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M3 (\n" +
                "   include test::M2\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                "   test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")", "COMPILATION error at [24:1-34:1]: Duplicated class mappings found with ID '1' in mapping 'test::M3'");
        // duplicated IDs with a mapping included multiple times included mapping
        test(models +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   *test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   include test::M1\n" +
                "   *test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                "   test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")", "COMPILATION error at [24:1-35:1]: Duplicated class mappings found with ID '1' in mapping 'test::M3'");
    }

    @Test
    public void testDuplicatedEnumerationMappingIds()
    {
        String models = "Enum test::B {\n" +
                "   a,b\n" +
                "}\n";
        // duplicated IDs within the same mapping
        test(models + "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   test::B: EnumerationMapping 1 {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   test::B: EnumerationMapping 2 {\n" +
                "      a : 'a',\n" +
                "      b : 'b'\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   test::B: EnumerationMapping {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                "   test::B: EnumerationMapping {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                ")", "COMPILATION error at [18:1-29:1]: Duplicated enumeration mappings found with ID 'test_B' in mapping 'test::M3'");
        // duplicated IDs within the included mappings
        test(models + "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   test::B: EnumerationMapping {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   test::B: EnumerationMapping {\n" +
                "      a : 'a',\n" +
                "      b : 'b'\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   test::B: EnumerationMapping 1 {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                "   test::B: EnumerationMapping 2 {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                ")", "COMPILATION error at [18:1-29:1]: Duplicated enumeration mappings found with ID 'test_B' in mapping 'test::M3'");
        // duplicated IDs in the included mappings and mapping
        test(models + "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   test::B: EnumerationMapping 1 {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   test::B: EnumerationMapping 2 {\n" +
                "      a : 'a',\n" +
                "      b : 'b'\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   test::B: EnumerationMapping 2 {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                "   test::B: EnumerationMapping {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                ")", "COMPILATION error at [18:1-29:1]: Duplicated enumeration mappings found with ID '2' in mapping 'test::M3'");
        // duplicated IDs in nested included mappings
        test(models + "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   test::B: EnumerationMapping 1 {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   include test::M1\n" +
                "   test::B: EnumerationMapping 2 {\n" +
                "      a : 'a',\n" +
                "      b : 'b'\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M3 (\n" +
                "   include test::M2\n" +
                "   test::B: EnumerationMapping 1 {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                "   test::B: EnumerationMapping {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                ")", "COMPILATION error at [19:1-29:1]: Duplicated enumeration mappings found with ID '1' in mapping 'test::M3'");
        // duplicated IDs with a mapping included multiple times included mapping
        test(models + "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   test::B: EnumerationMapping 1 {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                ")\n" +
                "Mapping test::M2 (\n" +
                "   include test::M1\n" +
                "   test::B: EnumerationMapping 2 {\n" +
                "      a : 'a',\n" +
                "      b : 'b'\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M3 (\n" +
                "   include test::M1\n" +
                "   include test::M2\n" +
                "   test::B: EnumerationMapping 1 {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                "   test::B: EnumerationMapping {\n" +
                "      a : ['a'],\n" +
                "      b : ['b']\n" +
                "   }\n" +
                ")", "COMPILATION error at [19:1-30:1]: Duplicated enumeration mappings found with ID '1' in mapping 'test::M3'");
    }

    @Test
    public void testMappingIncludes()
    {
        String shared = "Class model::Firm\n" +
                "{\n" +
                "  type: model::IncType[1];\n" +
                "  employees: model::Person[1];\n" +
                "}\n" +
                "\n" +
                "Class model::Person\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Class model::_Firm\n" +
                "{\n" +
                "  _type: String[1];\n" +
                "  em: model::_Person[1];\n" +
                "}\n" +
                "\n" +
                "Class model::_Person\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Enum model::IncType\n" +
                "{\n" +
                "  CORP\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping model::mapping2\n" +
                "(\n" +
                "  model::Person[model_Person_1]: Pure\n" +
                "  {\n" +
                "    ~src model::_Person\n" +
                "  }\n" +
                "\n" +
                "  model::IncType: EnumerationMapping model_IncType2\n" +
                "  {\n" +
                "    CORP: ['corporation']\n" +
                "  }\n" +
                ")\n";
        test(shared + "###Mapping\n" +
                "Mapping model::mapping1\n" +
                "(\n" +
                "  include model::mapping2\n" +
                "  *model::Firm: Pure\n" +
                "  {\n" +
                "    ~src model::_Firm\n" +
                "    employees[model_Person_1]: $src.em,\n" +
                "    type: EnumerationMapping model_IncType2: $src._type\n" +
                "  }\n" +
                ")\n" +
                "\n");
        test(shared + "###Mapping\n" +
                "Mapping model::mapping1\n" +
                "(\n" +
                "  *model::Firm: Pure\n" +
                "  {\n" +
                "    ~src model::_Firm\n" +
                "    type: EnumerationMapping model_IncType2: $src._type\n" +
                "  }\n" +
                ")\n" +
                "\n", "COMPILATION error at [46:5-55]: Can't find enumeration mapping 'model_IncType2'");
        test(shared + "###Mapping\n" +
                "Mapping model::mapping1\n" +
                "(\n" +
                "  *model::Firm: Pure\n" +
                "  {\n" +
                "    ~src model::_Firm\n" +
                "    employees[model_Person_1]: $src.em\n" +
                "  }\n" +
                ")\n" +
                "\n", "COMPILATION error at [46:5-38]: Can't find class mapping 'model_Person_1'");
    }

    @Test
    public void mappingTestFaultyLambda()
    {
        test("Class model::domain::Source {}\n" +
                "###Mapping\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      test2\n" +
                "      (\n" +
                // intentionally mess up the spacing so we can test if we send the full query string with white space to the M3 parser
                "         query: |model::domain::Target.all()-> graphFetchChecked(#{model::domain::Target{name}}#)-> serialize( #{model::domain::Target{name}}# );\n" +
                "         data: [" +
                "               <Object, JSON, model::domain::Source, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>" +
                "                   ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "      )\n" +
                "   ]" +
                "\n" +
                ")\n", "COMPILATION error at [9:18-38]: Can't find type 'model::domain::Target'");
    }

    @Test
    public void mappingTestFaultyGraphFetch()
    {
        // check faulty graph fetch (on the same line - check column offset processing)
        test("Class model::domain::Source {}" +
                "Class model::domain::Target\n" +
                "{\n" +
                "    name : String[1];\n" +
                "    date : StrictDate[0..1];\n" +
                "    number : Integer[0..1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping model::mapping::SourceToTargetM2M\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      defaultTest\n" +
                "      (\n" +
                // intentionally mess up the spacing to check walker source information processing (single line so we can test column offset)
                "         query:      |  model::domain::Target.all()->graphFetchChecked(#{       ClassNotHere {name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "         data: [" +
                "               <Object, XML, model::domain::Source, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>" +
                "                   ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 2\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 2\"},\"source\":{\"number\":1,\"record\":\"{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}\"}}}';\n" +
                "      )\n" +
                "   ]\n" +
                ")\n", "COMPILATION error at [15:81-92]: Can't find class 'ClassNotHere'");
        // check faulty graph fetch (on multiple lines - check column offset processing)
        test("Class model::domain::Source {}" +
                "Class model::domain::Target\n" +
                "{\n" +
                "    name : String[1];\n" +
                "    date : StrictDate[0..1];\n" +
                "    number : Integer[0..1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping model::mapping::SourceToTargetM2M\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      defaultTest\n" +
                "      (\n" +
                // intentionally mess up the spacing to check walker source information processing (multiple line)
                "         query: |  model::domain::Target.all()->graphFetchChecked(#{       \n" +
                "                                                                 ClassNotHere {name}}#)->serialize(#{model::domain::Target{name}}#);\n" +
                "         data: [" +
                "               <Object, XML, model::domain::Source, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>" +
                "                   ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 2\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 2\"},\"source\":{\"number\":1,\"record\":\"{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}\"}}}';\n" +
                "      )\n" +
                "   ]\n" +
                ")\n", "COMPILATION error at [16:66-77]: Can't find class 'ClassNotHere'");
    }

    @Test
    public void mappingTestObjectInputData()
    {
        // success
        test("Class model::domain::Target\n" +
                "{\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping model::mapping::SourceToTargetM2M\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      defaultTest\n" +
                "      (\n" +
                "         query: |model::domain::Target.all();\n" +
                "         data: [\n" +
                "         <Object, JSON, model::domain::Target, '{\"oneName\":\"oneName 2\"}'>" +
                "                   ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 2\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 2\"},\"source\":{\"number\":1,\"record\":\"{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}\"}}}';\n" +
                "      )\n" +
                "   ]\n" +
                ")\n");
        // missing format type
        test("Class model::domain::Target\n" +
                "{\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping model::mapping::SourceToTargetM2M\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      defaultTest\n" +
                "      (\n" +
                "         query: |model::domain::Target.all();\n" +
                "         data: [\n" +
                "         <Object, model::domain::Target, '{\"oneName\":\"oneName 2\"}'>" +
                "                   ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 2\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 2\"},\"source\":{\"number\":1,\"record\":\"{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}\"}}}';\n" +
                "      )\n" +
                "   ]\n" +
                ")\n", "PARSER error at [13:10-67]: Mapping test object input data format type is missing");
        // missing class
        test("Class model::domain::Target\n" +
                "{\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping model::mapping::SourceToTargetM2M\n" +
                "(\n" +
                "   MappingTests\n" +
                "   [\n" +
                "      defaultTest\n" +
                "      (\n" +
                "         query: |model::domain::Target.all();\n" +
                "         data: [\n" +
                "         <Object, JSON, model::domain::Source, '{\"oneName\":\"oneName 2\"}'>" +
                "                   ];\n" +
                "         assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 2\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 2\"},\"source\":{\"number\":1,\"record\":\"{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}\"}}}';\n" +
                "      )\n" +
                "   ]\n" +
                ")\n", "COMPILATION error at [13:10-73]: Can't find class 'model::domain::Source'");
    }

    @Test
    public void testFaultyPureInstanceClassMapping()
    {
        // check target class
        test("###Mapping\n" +
                "Mapping model::mapping::SourceToTargetM2M\n" +
                "(\n" +
                "   *model::domain::Target[model_domain_Target] : Pure\n" +
                "    {\n" +
                "     ~src model::domain::Source\n" +
                "     name: $src.oneName,\n" +
                "     date: $src.anotherDate,\n" +
                "     number: $src.oneNumber\n" +
                "    }\n" +
                ")\n", "COMPILATION error at [4:4-10:5]: Can't find class 'model::domain::Target'");
        // check source class
        test("Class ui::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping ui::a\n" +
                "(\n" +
                "  *ui::Person[ui_Person]: Pure\n" +
                "  {\n" +
                "    ~src ui::Person2\n" +
                "    name: 'aa'\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [10:10-20]: Can't find class 'ui::Person2'");
        // check set implementation root resolution
        test("Class ui::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  dog: ui::Dog[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class ui::Dog\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  breed: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping ui::a\n" +
                        "(\n" +
                        "  *ui::Person[ui_Person]: Pure\n" +
                        "  {\n" +
                        "    ~src ui::Person\n" +
                        "    name: 'aa',\n" +
                        "    dog: $src.dog\n" +
                        "  }\n" +
                        ")\n",
                "COMPILATION error at [20:5-17]: Can't find class mapping for 'ui::Dog'");
    }

    @Test
    public void testMissingClassMappingId()
    {
        test("Class ui::Person\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  dog: ui::Dog[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class ui::Dog\n" +
                        "{\n" +
                        "  name: String[1];\n" +
                        "  breed: String[1];\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "Mapping ui::a\n" +
                        "(\n" +
                        "  *ui::Person[ui_Person]: Pure\n" +
                        "  {\n" +
                        "    ~src ui::Person\n" +
                        "    name: 'aa',\n" +
                        "    dog[imMissing]: $src.dog\n" +
                        "  }\n" +
                        "  *ui::Dog[ui_Dog]: Pure\n" +
                        "  {\n" +
                        "    ~src ui::Dog\n" +
                        "    name: 'dogName',\n" +
                        "    breed: 'dogBreed'\n" +
                        "  }\n" +
                        ")\n",
                "COMPILATION error at [19:5-28]: Can't find class mapping 'imMissing'"
        );
    }

    @Test
    public void testCompileMapping()
    {
        // also check the same test with different spacing scheme for transform lambda to check walker source information processing
        test("Class test::Person{lastName:String[1];}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Person : Pure\n" +
                "   {\n" +
                // intentionally mess up spacing to check walker source information processing
                "       lastName :          1\n" +
                "   }\n" +
                ")", "COMPILATION error at [7:28]: Error in class mapping 'a::mapping' for property 'lastName' - Type error: 'Integer' is not a subtype of 'String'");
        test("Class test::Person{lastName:String[1];}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Person : Pure\n" +
                "   {\n" +
                // intentionally mess up spacing to check walker source information processing
                "       lastName :          \n" +
                "                       1\n" +
                "   }\n" +
                ")", "COMPILATION error at [8:24]: Error in class mapping 'a::mapping' for property 'lastName' - Type error: 'Integer' is not a subtype of 'String'");
        // check walker source information processing for mapping element
        test("Class test::Person{lastName:String[1];}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Person : Pure\n" +
                // intentionally mess up spacing to check walker source information processing for mapping element
                "              {    lastName :              1 }\n" +
                ")", "COMPILATION error at [6:44]: Error in class mapping 'a::mapping' for property 'lastName' - Type error: 'Integer' is not a subtype of 'String'");
    }

    @Test
    public void testMappingWithMatch()
    {
        test("Class example::Report\n" +
                "{\n" +
                "   productSide:String[1];\n" +
                "   hasOption:Boolean[1];\n" +
                "}\n" +
                "\n" +
                "Class example::Trade\n" +
                "{\n" +
                "   product:example::Product[1];\n" +
                "   \n" +
                "}\n" +
                "Class  example::Product\n" +
                "{\n" +
                "   name:String[1];\n" +
                "   \n" +
                "}\n" +
                "Class example::Option extends example::Product\n" +
                "{\n" +
                "   side1:String[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "Class example::Swap extends example::Product\n" +
                "{\n" +
                "   side2:String[1];\n" +
                "   \n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping example::mappingWithMatch\n" +
                "(\n" +
                "   example::Report : Pure\n" +
                "            {\n" +
                "               ~src example::Trade\n" +
                "                  hasOption: $src.product->instanceOf(example::Option),\n" +
                "                  productSide: $src.product->match([p:example::Option[1]|$p.side1,\n" +
                "                                                    s:example::Swap[1]|$s.side2])\n" +
                "            }\n" +
                "\n" +
                ")\n");
    }

    @Test
    public void testSuperTypePropertyMapped()
    {
        test("Class ui::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class ui::Employee extends ui::Person\n" +
                "[\n" +
                "  a: $this.name == 'a'\n" +
                "]\n" +
                "{\n" +
                "  companyName: String[0..1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping ui::myMapping\n" +
                "(\n" +
                "  *ui::Employee[ui_Employee]: Pure\n" +
                "  {\n" +
                "    name: 'test name'\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testCollectionCompileMappingError()
    {
        test("Class test::Person{lastName:String[1];}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Person : Pure\n" +
                "   {\n" +
                "       lastName : [1, 2]\n" +
                "   }\n" +
                ")", "COMPILATION error at [7:19-24]: Error in class mapping 'a::mapping' for property 'lastName' - Type error: 'Integer' is not a subtype of 'String'");
    }

    @Test
    public void testCollection2CompileMappingError()
    {
        test("Class test::Person{lastName:String[1];}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Person : Pure\n" +
                "   {\n" +
                "       lastName : ['1', '2']\n" +
                "   }\n" +
                ")", "COMPILATION error at [7:19-28]: Error in class mapping 'a::mapping' for property 'lastName' - Multiplicity error: [1] doesn't subsumes [2]");
    }

    @Test
    public void testCompileSrcTypeMapping()
    {
        test("Class test::Person{lastName:String[1];} Class test::Firm{employees:test::Person[*];} Class test::SrcPerson{} Class test::SrcFirm{empl:test::SrcPerson[*];}\n" +
                "###Mapping\n" +
                "Mapping a::mapping" +
                "(" +
                "   test::Firm : Pure" +
                "   {" +
                "       ~src test::SrcFirm" +
                "       employees : $src.empl" +
                "   }" +
                "   test::Person : Pure" +
                "   {" +
                "       ~src test::SrcPerson" +
                "       lastName : '1'" +
                "   }" +
                ")");
    }

    @Test
    public void testCompileSrcTypeMappingError()
    {
        test("Class test::Person{lastName:String[1];} Class test::OtherPerson{lastName:String[1];} Class test::Firm{employees: test::Person[*];} Class test::SrcPerson{} Class test::SrcFirm{empl: test::SrcPerson[*];}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Firm : Pure\n" +
                "   {\n" +
                "       ~src test::SrcFirm\n" +
                "       employees : $src.empl\n" +
                "   }\n" +
                "   test::Person : Pure\n" +
                "   {\n" +
                "       ~src test::OtherPerson\n" +
                "       lastName : '1'\n" +
                "   }\n" +
                ")", "COMPILATION error at [8:25-28]: Error in class mapping 'a::mapping' for property 'employees' - Type error: 'test::SrcPerson' is not a subtype of 'test::OtherPerson'");
    }

    @Test
    public void testEnumMappingInteger()
    {
        test("Class test::Firm{incType:test::IncType[1];} Enum test::IncType{CORP, LLC}\n" +
                "###Mapping\n" +
                "Mapping a::mapping" +
                "(" +
                "   test::Firm : Pure" +
                "   {" +
                "       incType : EnumerationMapping a : 1" +
                "   }" +
                "   test::IncType : EnumerationMapping a" +
                "   {" +
                "       CORP : [1]," +
                "       LLC : [2]" +
                "   }" +
                ")");
    }

    @Test
    public void testEnumMappingError()
    {
        test("Class test::Firm{incType:test::IncType[1];} Enum test::IncType{CORP, LLC}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Firm : Pure\n" +
                "   {\n" +
                "       incType : EnumerationMapping a : '1'\n" +
                "   }\n" +
                "   test::IncType : EnumerationMapping a\n" +
                "   {\n" +
                "       CORP : [1],\n" +
                "       LLC : [2]\n" +
                "   }\n" +
                ")", "COMPILATION error at [7:41-43]: Error in class mapping 'a::mapping' for property 'incType' - Type error: 'String' is not a subtype of 'Integer'");
    }

    @Test
    public void testEnumMappingString()
    {
        test("Class test::Firm{incType:test::IncType[1];} Enum test::IncType{CORP, LLC}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Firm : Pure\n" +
                "   {\n" +
                "       incType : EnumerationMapping a : '1'\n" +
                "   }\n" +
                "   test::IncType : EnumerationMapping a\n" +
                "   {" +
                "       CORP : ['1'],\n" +
                "       LLC : ['2', '3']\n" +
                "   }" +
                ")\n");
    }

    @Test
    public void testEnumMappingEnumError()
    {
        test("Class test::Firm{incType:test::IncType[1];} Enum test::IncType{CORP, LLC} Enum test::Other{bla}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Firm : Pure\n" +
                "   {\n" +
                "       incType : EnumerationMapping a : '1'\n" +
                "   }\n" +
                "   test::IncType : EnumerationMapping a\n" +
                "   {\n" +
                "       CORP : [test::Other.bla],\n" +
                "       LLC : [test::Other.bla]\n" +
                "   }\n" +
                ")\n", "COMPILATION error at [7:41-43]: Error in class mapping 'a::mapping' for property 'incType' - Type error: 'String' is not a subtype of 'test::Other'");
    }

    @Test
    public void testEnumMappingEnumError2()
    {
        test("Class test::Firm{incType:test::IncType[1];} Enum test::IncType{CORP, LLC} Enum test::Other{bla}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Firm : Pure\n" +
                "   {\n" +
                "       incType : EnumerationMapping a : test::IncType.CORP\n" +
                "   }\n" +
                "   test::IncType : EnumerationMapping a\n" +
                "   {\n" +
                "       CORP : [test::Other.bla],\n" +
                "       LLC : [test::Other.bla]\n" +
                "   }\n" +
                ")\n", "COMPILATION error at [7:55-58]: Error in class mapping 'a::mapping' for property 'incType' - Type error: 'test::IncType' is not a subtype of 'test::Other'");
    }

    @Test
    public void testEnumMappingEnum()
    {
        test("Class test::Firm{incType:test::IncType[1];} Enum test::IncType{CORP, LLC} Enum test::Other{bla}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Firm : Pure\n" +
                "   {\n" +
                "       incType : EnumerationMapping a : test::Other.bla\n" +
                "   }\n" +
                "   test::IncType : EnumerationMapping a\n" +
                "   {\n" +
                "       CORP : [test::Other.bla],\n" +
                "       LLC : [test::Other.bla]\n" +
                "   }\n" +
                ")\n");
    }

    @Test
    public void testEnumerationMappingMixedSourceValueType()
    {
        test("Class test::Firm{incType:test::IncType[1];} Enum test::IncType{CORP, LLC, LLC2} Enum test::Other{bla}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::IncType : EnumerationMapping a\n" +
                "   {\n" +
                "       CORP : [test::Other.bla,test::Other.bla,test::Other.bla,test::Other.bla,test::Other.bla,test::Other.bla],\n" +
                "       LLC : [test::Other.bla],\n" +
                "       LLC2 : ['asd']\n" +
                "   }\n" +
                ")", "COMPILATION error at [5:4-10:4]: Only one type of source value (integer, string or an enum) is allowed for enumeration mapping");

        test("Class test::Firm{incType:test::IncType[1];} Enum test::IncType{CORP, LLC, LLC2} Enum test::Other{bla}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::IncType : EnumerationMapping a\n" +
                "   {\n" +
                "       CORP : ['4'],\n" +
                "       LLC : ['4', 4],\n" +
                "       LLC2 : ['123']\n" +
                "   }\n" +
                ")", "COMPILATION error at [5:4-10:4]: Only one type of source value (integer, string or an enum) is allowed for enumeration mapping");

        test("Class test::Firm{incType:test::IncType[1];} Enum test::IncType{CORP, LLC, LLC2} Enum test::Other{bla}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::IncType : EnumerationMapping a\n" +
                "   {\n" +
                "       CORP : [4],\n" +
                "       LLC : [7,7,7,7,7,test::Other.bla],\n" +
                "       LLC2 : [3]\n" +
                "   }\n" +
                ")", "COMPILATION error at [5:4-10:4]: Only one type of source value (integer, string or an enum) is allowed for enumeration mapping");
    }

    @Test
    public void testMappingWithFaultyOperationClassMapping()
    {
        test("Class anything::goes\n" +
                "{\n" +
                "  name: String[*];\n" +
                "}\n" +
                "\n\n" +
                "###Mapping\n" +
                "import anything::*;\n" +
                "import anything::else::*;\n" +
                "Mapping anything::A\n" +
                "(\n" +
                "  *goes[anything_goes]: Operation\n" +
                "  {\n" +
                "    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(p1,p2)\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [12:3-15:3]: Can't find class mapping 'p1' in mapping 'anything::A'");
    }

    @Test
    public void testMappingWithImport()
    {
        test("Class anything::goes\n" +
                "{\n" +
                "  fullName: String[1];\n" +
                "  name: String[*];\n" +
                "}\n" +
                "\n" +
                "Enum anything::else::goes2\n" +
                "{\n" +
                "  b\n" +
                "}\n" +
                "\n\n" +
                "###Mapping\n" +
                "import anything::*;\n" +
                "import anything::else::*;\n" +
                "Mapping anything::A\n" +
                "(\n" +
                "  *goes[tiec]: Operation\n" +
                "  {\n" +
                "    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(tiec,tiec)\n" +
                "  }\n" +
                ")\n" +
                "Mapping meta::pure::mapping::modelToModel::test::simple::simpleModelMapping\n" +
                "(\n" +
                // Mapping Include mapping
                "  include A\n" +
                // OPERATION CLASS MAPPING
                // Operation Class mapping target class
                "  *goes[anything_goes]: Operation\n" +
                "  {\n" +
                "    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(anything_goes,tiec)\n" +
                "  }\n" +
                // PURE INSTANCE CLASS MAPPING
                // Pure Instance Class mapping target class
                "  goes[cay]: Pure\n" +
                "  {\n" +
                // Pure Instance class mapping source class
                "    ~src goes\n" +
                "    fullName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                // class mapping transform
                "    name: $src->cast(@goes).fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "  }\n" +
                "\n" +
                // ENUMERATION MAPPING
                // Enumeration mapping target enumeration
                "  goes2: EnumerationMapping TargetTradeTypeMapping2\n" +
                "  {\n" +
                // Enumeration mapping source value enumeration type
                "    b: [goes2.b]\n" +
                "  }\n" +
                "\n" +
                "  MappingTests\n" +
                "  [\n" +
                "    test2\n" +
                "    (\n" +
                // graph fetch tree target class
                "      query: |goes.all()->graphFetchChecked(#{goes{name}}#)->serialize(#{goes{name}}#);\n" +
                "      data:\n" +
                "      [\n" +
                // Object input data source class
                "        <Object, JSON, goes, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>,\n" +
                "        <Object, XML, goes, '{\"oneName\":\"oneName 2\",\"anotherName\":\"anotherName 16\",\"oneDate\":\"2020-02-05\",\"anotherDate\":\"2020-04-13\",\"oneNumber\":24,\"anotherNumber\":29}'>\n" +
                "      ];\n" +
                "      assert: '{\"defects\":[],\"value\":{\"name\":\"oneName 99\"},\"source\":{\"defects\":[],\"value\":{\"oneName\":\"oneName 99\"},\"source\":{\"number\":1,\"record\":\"{\\\"oneName\\\":\\\"oneName 99\\\",\\\"anotherName\\\":\\\"anotherName 17\\\",\\\"oneDate\\\":\\\"2020-04-13\\\",\\\"anotherDate\\\":\\\"2020-02-25\\\",\\\"oneNumber\\\":27,\\\"anotherNumber\\\":28}\"}}}';\n" +
                "    )\n" +
                "  ]" +
                ")\n");
    }

    @Test
    public void testModelToModelMappingWithUnitTypeProperties()
    {
        String models = "Measure test::Mass\n" +
                "{\n" +
                "   *Gram: x -> $x;\n" +
                "   Kilogram: x -> $x * 1000;\n" +
                "   Pound: x -> $x * 453.59;\n" +
                "}" +
                "Class test::Person\n" +
                "{\n" +
                "   weight : test::Mass~Kilogram[1];\n" +
                "}\n" +
                "Class test::_Person\n" +
                "{\n" +
                "   weightUnit : String[1];\n" +
                "   weightValue : Number[1];\n" +
                "}\n" +
                "Class test::PersonWithPound\n" +
                "{\n" +
                "   weight : test::Mass~Pound[1];\n" +
                "}\n";
        test(models +
                "###Mapping\n" +
                "Mapping test::decomposeMapping\n" +
                "(\n" +
                "  *test::_Person: Pure\n" +
                "  {\n" +
                "    ~src test::Person\n" +
                "    weightUnit: $src.weight->unitType(),\n" +
                "    weightValue: $src.weight->unitValue()\n" +
                "  }\n" +
                ")");
        test(models +
                "###Mapping\n" +
                "Mapping test::composeMapping\n" +
                "(\n" +
                "  *test::Person: Pure\n" +
                "  {\n" +
                "    ~src test::_Person\n" +
                "    weight: newUnit(test::Mass~Kilogram, $src.weightValue)->cast(@test::Mass~Kilogram)\n" +
                "  }\n" +
                ")");
        test(models +
                "###Mapping\n" +
                "Mapping test::convertMapping\n" +
                "(\n" +
                "  *test::PersonWithPound: Pure\n" +
                "  {\n" +
                "    ~src test::Person\n" +
                "    weight: $src.weight->convert(test::Mass~Pound)->cast(@test::Mass~Pound)\n" +
                "  }\n" +
                ")");
    }
}
