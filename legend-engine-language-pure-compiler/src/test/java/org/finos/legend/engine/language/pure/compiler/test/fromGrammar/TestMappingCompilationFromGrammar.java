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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_modelToModel_PureInstanceSetImplementation_Impl;
import org.junit.Assert;
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
                "import test::*;\n" +
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
                "   include M1\n" +
                "   include test::M2\n" +
                "   test::A: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n", "COMPILATION error at [16:1-22:1]: Cycle detected in mapping include hierarchy: test::M1 -> " +
                "test::M2 -> test::M3 -> test::M1");
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
                "import test::*;\n" +
                "Mapping test::M1 (\n" +
                "   test::A[1]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "Mapping test::M2 (\n" +
                "   include M1\n" +
                "   include test::M1\n" +
                "   *test::A[2]: Pure {\n" +
                "      ~src test::S_A\n" +
                "      prop1: $src.prop1\n" +
                "   }\n" +
                ")\n" +
                "\n", "COMPILATION error at [23:1-30:1]: Duplicated mapping include 'test::M1' in mapping " +
                "'test::M2'"
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
                ")\n", "COMPILATION error at [9:18-38]: Can't find the packageable element 'model::domain::Target'");
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
                ")\n", "PARSER error at [13:10-67]: Mapping test object 'input type' is missing. Possible values: JSON, XML");
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
                        "    dog: $src.name\n" +
                        "  }\n" +
                        ")\n",
                "COMPILATION error at [20:5-18]: Can't find class mapping 'ui_Dog'");
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
                ")", "COMPILATION error at [7:28]: Error in class mapping 'a::mapping' for property 'lastName' - Type error: 'Integer' is not in the class hierarchy of 'String'");
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
                ")", "COMPILATION error at [8:24]: Error in class mapping 'a::mapping' for property 'lastName' - Type error: 'Integer' is not in the class hierarchy of 'String'");
        // check walker source information processing for mapping element
        test("Class test::Person{lastName:String[1];}\n" +
                "###Mapping\n" +
                "Mapping a::mapping\n" +
                "(\n" +
                "   test::Person : Pure\n" +
                // intentionally mess up spacing to check walker source information processing for mapping element
                "              {    lastName :              1 }\n" +
                ")", "COMPILATION error at [6:44]: Error in class mapping 'a::mapping' for property 'lastName' - Type error: 'Integer' is not in the class hierarchy of 'String'");
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
                ")", "COMPILATION error at [7:19-24]: Error in class mapping 'a::mapping' for property 'lastName' - Type error: 'Integer' is not in the class hierarchy of 'String'");
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
                ")", "COMPILATION error at [7:19-28]: Error in class mapping 'a::mapping' for property 'lastName' - Multiplicity error: [1] doesn't subsume [2]");
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
                ")", "COMPILATION error at [8:25-28]: Error in class mapping 'a::mapping' for property 'employees' - Type error: 'test::SrcPerson' is not in the class hierarchy of 'test::OtherPerson'");
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
                ")", "COMPILATION error at [7:41-43]: Error in class mapping 'a::mapping' for property 'incType' - Type error: 'String' is not in the class hierarchy of 'Integer'");
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
                ")\n", "COMPILATION error at [7:41-43]: Error in class mapping 'a::mapping' for property 'incType' - Type error: 'String' is not in the class hierarchy of 'test::Other'");
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
                ")\n", "COMPILATION error at [7:55-58]: Error in class mapping 'a::mapping' for property 'incType' - Type error: 'test::IncType' is not in the class hierarchy of 'test::Other'");
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

    @Test
    public void testModelToModelMappingWithMilestonedProperties()
    {
        test("Class <<meta::pure::profiles::temporal.businesstemporal>> model::person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  employees: model::person[*];\n" +
                "}\n" +
                "\n" +
                "Class <<meta::pure::profiles::temporal.businesstemporal>> model::_person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::_firm\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  employees: model::_person[*];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping model::test\n" +
                "(\n" +
                "  *model::firm: Pure\n" +
                "  {\n" +
                "    ~src model::_firm\n" +
                "    legalName: $src.legalName,\n" +
                "    employees[model_person]: $src.employeesAllVersions\n" +
                "  }\n" +
                "  *model::person: Pure\n" +
                "  {\n" +
                "    ~src model::_person\n" +
                "    name: $src.name\n" +
                "  }\n" +
                ")");
    }

    @Test
    public void testPropertyMappingWithTargetId()
    {
        String models = "Class test::A {\n" +
                "  prop1: String[1];\n" +
                "  prop2: test::B[1];\n" +
                "}\n" +
                "\n" +
                "Class test::_A {\n" +
                "  prop1: String[1];\n" +
                "  prop2: test::_B[1];\n" +
                "}\n" +
                "\n" +
                "Class test::B\n" +
                "{\n" +
                "  prop: String[1];\n" +
                "}\n" +
                "Class test::_B\n" +
                "{\n" +
                "  prop: String[1];\n" +
                "}\n" +
                "\n";
        PureModel model = test(models +
                "###Mapping\n" +
                "Mapping test::M1 (\n" +
                "   test::A: Pure {\n" +
                "      ~src test::_A\n" +
                "      prop1: $src.prop1,\n" +
                "      prop2: $src.prop2\n" +
                "   }\n" +
                "   test::B: Pure {\n" +
                "      ~src test::_B\n" +
                "      prop: $src.prop\n" +
                "   }\n" +
                ")\n" +
                "\n" +
                "\n").getTwo();

        Root_meta_pure_mapping_modelToModel_PureInstanceSetImplementation_Impl classA = (Root_meta_pure_mapping_modelToModel_PureInstanceSetImplementation_Impl) model.getMapping("test::M1")._classMappings().getFirst();
        Assert.assertEquals(classA._propertyMappings().getLast()._targetSetImplementationId(), "test_B");
    }

    @Test
    public void testModelMappingWithLocalProperties()
    {
        test("Class test::Firm\n" +
                "{" +
                "   name : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping a::localPropertyMapping\n" +
                "(\n" +
                "   \n" +
                "   test::Firm : Pure\n" +
                "   {\n" +
                "       ~src test::Firm\n" +
                "       +prop1: String[1]: $src.name,\n" +
                "       name : $src.name\n" +
                "   }\n" +
                ")");
    }

    @Test
    public void testCrossStoreMappingWithLocalProperties()
    {
        String mapping = "###Pure\n" +
                "Class test::Person\n" +
                "{\n" +
                "   name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Firm\n" +
                "{\n" +
                "   id: Integer[1];\n" +
                "   legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Association test::Firm_Person\n" +
                "{\n" +
                "   employer: test::Firm[1];\n" +
                "   employees: test::Person[*];\n" +
                "}\n\n\n" +
                "###Mapping\n" +
                "Mapping test::crossPropertyMappingWithLocalProperties\n" +
                "(\n" +
                "   test::Person[p]: Pure {\n" +
                "      ~src test::Person\n" +
                "      +firmId: Integer[1]: 1,\n" +
                "      name: $src.name\n" +
                "   }\n" +
                "   \n" +
                "   test::Firm[f]: Pure {\n" +
                "      ~src test::Firm\n" +
                "      id: $src.id,\n" +
                "      legalName: $src.legalName\n" +
                "   }\n\n" +
                "%s\n" +
                ")\n";

        test(
                String.format(
                        mapping,
                        "   test::Firm_Person: XStore {\n" +
                                "      employer[p1, f]: $this.firmId + $that.id\n" +
                                "   }"),
                "COMPILATION error at [36:7-46]: Can't find class mapping 'p1' in mapping 'test::crossPropertyMappingWithLocalProperties'"
        );

        test(
                String.format(
                        mapping,
                        "   test::Firm_Person: XStore {\n" +
                                "      employer[p, f1]: $this.firmId + $that.id\n" +
                                "   }"),
                "COMPILATION error at [36:7-46]: Can't find class mapping 'f1' in mapping 'test::crossPropertyMappingWithLocalProperties'"
        );

        test(
                String.format(
                        mapping,
                        "   test::Firm_Person: XStore {\n" +
                                "      employer[p, f]: $this.firmId + $that.id\n" +
                                "   }"),
                "COMPILATION error at [36:36-45]: XStore property mapping function should return 'Boolean[1]'"
        );

        test(
                String.format(
                        mapping,
                        "   test::Firm_Person: XStore {\n" +
                                "      employer[p, f]: [true, true]\n" +
                                "   }"),
                "COMPILATION error at [36:23-34]: XStore property mapping function should return 'Boolean[1]'"
        );

        test(
                String.format(
                        mapping,
                        "   test::Firm_Person: XStore {\n" +
                                "      employer[p, f]: $this.firmId == $that.id\n" +
                                "   }")
        );
    }

    @Test
    public void testCrossStoreMappingWithMilestoning()
    {
        test("###Pure\n" +
                "Class <<temporal.businesstemporal>> test::Firm_Milestoned\n" +
                "{\n" +
                "  id: Integer[1];\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class <<temporal.businesstemporal>> test::Person_Milestoned\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Association test::Firm_Person_Milestoned\n" +
                "{\n" +
                "  employer: test::Firm_Milestoned[1];\n" +
                "  employees: test::Person_Milestoned[*];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::crossPropertyMappingWithLocalProperties_Milestoned\n" +
                "(\n" +
                "  test::Person_Milestoned[p]: Pure\n" +
                "  {\n" +
                "    ~src test::Person_Milestoned\n" +
                "    +firmId: Integer[1]: 1,\n" +
                "    name: $src.name\n" +
                "  }\n" +
                "  test::Firm_Milestoned[f]: Pure\n" +
                "  {\n" +
                "    ~src test::Firm_Milestoned\n" +
                "    id: $src.id,\n" +
                "    legalName: $src.legalName\n" +
                "  }\n" +
                "\n" +
                "  test::Firm_Person_Milestoned: XStore\n" +
                "  {\n" +
                "    employer[p, f]: $this.firmId == $that.id\n" +
                "  }\n" +
                ")");
    }

    @Test
    public void testOperationWithSubtype()


    {


        String model = "###Pure\n" +
                "Class example::Person\n" +
                "{\n" +
                "  firstName:String[0..1];\n" +
                "  lastName: String[0..1];\n" +
                "}\n" +
                "\n" +
                "Class example::_S_PersonA extends example::_S_Person\n" +
                "{\n" +
                "   aName    : String[1];\n" +
                "}\n" +
                "\n" +
                "Class example::_S_PersonB extends example::_S_Person\n" +
                "{\n" +
                "   bName    : String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class example::_S_Person\n" +
                "{\n" +
                "   fullName      : String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class example::_S_Cat\n" +
                "{\n" +
                "   fullName      : String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "Class example::_S_Firm\n" +
                "{\n" +
                "  name:String[1];\n" +
                "  sourceEmployees:example::_S_Person[*];\n" +
                "  sourceEmployeesA:example::_S_PersonA[*];\n" +

                "}\n" +
                "\n" +
                "Class example::Firm\n" +
                "{\n" +
                "\n" +
                "  legalName:String[1];\n" +
                "  employees:example::Person[*];\n" +
                "}\n";

        test(model + "\n###Mapping\n" +
                "Mapping example::MappingwithNill\n" +
                "   (\n" +
                " example::Firm : Pure\n" +
                "            {\n" +
                "               ~src example::_S_Firm\n" +
                "\n" +
                "               legalName        : $src.name,\n" +
                "               employees     : []\n" +
                "            }\n" +
                "\n" +
                "   example::Person  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_PersonA\n" +
                "     ~filter $src.fullName  == 'A1 Person'\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')) + $src.aName,\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                "\n" +

                "\n" +

                ")\n");

        test(model + "\n###Mapping\n" +
                "Mapping example::MappingwithSubtype\n" +
                "   (\n" +
                " example::Firm : Pure\n" +
                "            {\n" +
                "               ~src example::_S_Firm\n" +
                "\n" +
                "               legalName        : $src.name,\n" +
                "               employees     : $src.sourceEmployeesA\n" +
                "            }\n" +
                "\n" +

                "   example::Person  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_Person\n" +
                "     ~filter $src.fullName  == 'A1 Person'\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')) + $src->cast(@example::_S_PersonA).aName,\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                "\n" +

                "\n" +

                ")\n");

        test(model + "\n###Mapping\n" +
                "Mapping example::UnionOnSubTypeinclude\n" +
                "   (\n" +
                " example::Firm : Pure\n" +
                "            {\n" +
                "               ~src example::_S_Firm\n" +
                "\n" +
                "               legalName        : $src.name,\n" +
                "               employees[personUnion]     : $src.sourceEmployees\n" +
                "            }\n" +
                "\n" +
                "   *example::Person[personUnion] : Operation {\n" +
                "      meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(A1,Cat)\n" +
                "   }\n" +
                "   example::Person[A1]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_PersonA\n" +
                "     ~filter $src.fullName  == 'A1 Person'\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')) + $src.aName,\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                "\n" +
                "\n" +
                "     example::Person[Cat]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_Cat\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                "\n" +
                ")\n", "COMPILATION error at [53:50-64]: Error in class mapping 'example::UnionOnSubTypeinclude' for property 'employees' - Type error: 'example::_S_Person' is not in the class hierarchy of 'example::_S_Cat'");

        test(model +

                "\n###Mapping\n" +
                "Mapping example::UnionOnSubType\n" +
                "   (\n" +
                "include example::UnionOnSubTypeinclude " +
                "example::Firm : Pure\n" +
                "            {\n" +
                "               ~src example::_S_Firm\n" +
                "\n" +
                "               legalName        : $src.name,\n" +
                "               employees[personUnion]     : $src.sourceEmployees\n" +
                "            })\n" +


                "Mapping example::UnionOnSubTypeinclude\n" +
                "   (\n" +
                "   *example::Person[personUnion] : Operation {\n" +
                "      meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(A1,Cat)\n" +
                "   }\n" +
                "   example::Person[A1]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_PersonA\n" +
                "     ~filter $src.fullName  == 'A1 Person'\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')) + $src.aName,\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                "\n" +
                "\n" +
                "     example::Person[Cat]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_Cat\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                "\n" +
                ")\n", "COMPILATION error at [53:50-64]: Error in class mapping 'example::UnionOnSubType' for property 'employees' - Type error: 'example::_S_Person' is not in the class hierarchy of 'example::_S_Cat'");


        test(model + "\n###Mapping\n" +
                "Mapping example::UnionOnSubTypeinclude\n" +
                "   (\n" +
                " example::Firm : Pure\n" +
                "            {\n" +
                "               ~src example::_S_Firm\n" +
                "\n" +
                "               legalName        : $src.name,\n" +
                "               employees[personUnion]     : $src.sourceEmployees\n" +
                "            }\n" +
                "\n" +
                "   *example::Person[personUnion] : Operation {\n" +
                "      meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(R,A1,B)\n" +
                "   }\n" +
                "   example::Person[R]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_Person\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' '))+' Super',\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }" +
                "   example::Person[A1]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_PersonA\n" +
                "     ~filter $src.fullName  == 'A1 Person'\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')) + $src.aName,\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                "\n" +

                "\n" +
                "    example::Person[B]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_PersonB\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')) + $src.bName,\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                ")\n");

        test(model + "\n###Mapping\n" +
                "Mapping example::UnionOnSubTypeinclude\n" +
                "   (\n" +
                "   *example::Person[personUnion] : Operation {\n" +
                "      meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(R,A1,B)\n" +
                "   }\n" +

                " example::Firm : Pure\n" +
                "            {\n" +
                "               ~src example::_S_Firm\n" +
                "\n" +
                "               legalName        : $src.name,\n" +
                "               employees[personUnion]     : $src.sourceEmployees\n" +
                "            }\n" +
                "\n" +
                "   example::Person[R]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_Person\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' '))+' Super',\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }" +

                "   example::Person[A1]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_PersonA\n" +
                "     ~filter $src.fullName  == 'A1 Person'\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')) + $src.aName,\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                "\n" +

                "\n" +
                "    example::Person[B]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_PersonB\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')) + $src.bName,\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                ")\n");


        test(model + "\n###Mapping\n" +
                "Mapping example::UnionOnSubTypeinclude\n" +
                "   (\n" +
                "   *example::Person[personUnion] : Operation {\n" +
                "      meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(A1,Cat)\n" +
                "   }\n" +

                " example::Firm : Pure\n" +
                "            {\n" +
                "               ~src example::_S_Firm\n" +
                "\n" +
                "               legalName        : $src.name,\n" +
                "               employees[personUnion]     : $src.sourceEmployees\n" +
                "            }\n" +
                "\n" +

                "   example::Person[A1]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_PersonA\n" +
                "     ~filter $src.fullName  == 'A1 Person'\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')) + $src.aName,\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                "\n" +
                "\n" +
                "     example::Person[Cat]  : Pure\n" +
                "   {\n" +
                "      ~src example::_S_Cat\n" +
                "      firstName : $src.fullName->substring(0, $src.fullName->indexOf(' ')) ,\n" +
                "      lastName : $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length())\n" +
                "\n" +
                "   }\n" +
                "\n" +
                ")\n", "COMPILATION error at [56:50-64]: Error in class mapping 'example::UnionOnSubTypeinclude' for property 'employees' - Type error: 'example::_S_Person' is not in the class hierarchy of 'example::_S_Cat'");


    }

    @Test
    public void testComplexPropertyMappedToComplexSourceWithoutClassMapping()
    {
        test("###Pure\n" +
                "Class test::dest::Person\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "   firm: test::shared::Firm[1];\n" +
                "}\n" +
                "Class test::src::Person\n" +
                "{\n" +
                "   fullName: String[1];\n" +
                "   firm: test::shared::Firm[1];\n" +
                "}\n" +
                "Class test::shared::Firm\n" +
                "{\n" +
                "   id: String[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Mapping\n" +
                "(\n" +
                "  *test::dest::Person: Pure\n" +
                "  {\n" +
                "    ~src test::src::Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    firm: $src.firm\n" +
                "  }\n" +
                ")\n");

        test("###Pure\n" +
                "Class test::dest::Person\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "   firm: test::shared::Firm[1];\n" +
                "}\n" +
                "Class test::src::Person\n" +
                "{\n" +
                "   fullName: String[1];\n" +
                "   firm: test::shared::BigFirm[1];\n" +
                "}\n" +
                "Class test::shared::Firm\n" +
                "{\n" +
                "   id: String[1];\n" +
                "}\n" +
                "Class test::shared::BigFirm extends test::shared::Firm\n" +
                "{\n" +
                "   size: Integer[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Mapping\n" +
                "(\n" +
                "  *test::dest::Person: Pure\n" +
                "  {\n" +
                "    ~src test::src::Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    firm: $src.firm\n" +
                "  }\n" +
                ")\n");

        test("###Pure\n" +
                "Class test::dest::Person\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "   firm: test::shared::Firm[1];\n" +
                "}\n" +
                "Class test::src::Person\n" +
                "{\n" +
                "   fullName: String[1];\n" +
                "   firm: test::shared::Corporation[1];\n" +
                "}\n" +
                "Class test::shared::Firm\n" +
                "{\n" +
                "   id: String[1];\n" +
                "}\n" +
                "Class test::shared::BigFirm extends test::shared::Firm\n" +
                "{\n" +
                "   size: Integer[1];\n" +
                "}\n" +
                "Class test::shared::Corporation extends test::shared::BigFirm\n" +
                "{\n" +
                "   legalName: Integer[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Mapping\n" +
                "(\n" +
                "  *test::dest::Person: Pure\n" +
                "  {\n" +
                "    ~src test::src::Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    firm: $src.firm\n" +
                "  }\n" +
                ")\n");

        test("###Pure\n" +
                "Class test::dest::Person\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "   firm: test::shared::BigFirm[1];\n" +
                "}\n" +
                "Class test::src::Person\n" +
                "{\n" +
                "   fullName: String[1];\n" +
                "   firm: test::shared::Corporation[1];\n" +
                "}\n" +
                "Class test::shared::Firm\n" +
                "{\n" +
                "   id: String[1];\n" +
                "}\n" +
                "Class test::shared::BigFirm extends test::shared::Firm\n" +
                "{\n" +
                "   size: Integer[1];\n" +
                "}\n" +
                "Class test::shared::Corporation extends test::shared::BigFirm\n" +
                "{\n" +
                "   legalName: Integer[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Mapping\n" +
                "(\n" +
                "  *test::dest::Person: Pure\n" +
                "  {\n" +
                "    ~src test::src::Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    firm: $src.firm\n" +
                "  }\n" +
                ")\n");

        test("###Pure\n" +
                "Class test::dest::Person\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "   firm: test::shared::Firm[1];\n" +
                "}\n" +
                "Class test::src::Person\n" +
                "{\n" +
                "   fullName: String[1];\n" +
                "   firm: test::shared::Firm[*];\n" +
                "}\n" +
                "Class test::shared::Firm\n" +
                "{\n" +
                "   id: String[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Mapping\n" +
                "(\n" +
                "  *test::dest::Person: Pure\n" +
                "  {\n" +
                "    ~src test::src::Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    firm: $src.firm\n" +
                "  }\n" +
                ")\n", "COMPILATION error at [26:16-19]: Error in class mapping 'test::Mapping' for property 'firm' - Multiplicity error: [1] doesn't subsume [*]");

        test("###Pure\n" +
                "Class test::dest::Person\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "   firm: test::shared::Firm[*];\n" +
                "}\n" +
                "Class test::src::Person\n" +
                "{\n" +
                "   fullName: String[1];\n" +
                "   firm: test::shared::Firm[1];\n" +
                "}\n" +
                "Class test::shared::Firm\n" +
                "{\n" +
                "   id: String[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Mapping\n" +
                "(\n" +
                "  *test::dest::Person: Pure\n" +
                "  {\n" +
                "    ~src test::src::Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    firm: $src.firm\n" +
                "  }\n" +
                ")\n");

        test("###Pure\n" +
                "Class test::dest::Person\n" +
                "{\n" +
                "   firstName: String[1];\n" +
                "   lastName: String[1];\n" +
                "   firm: test::shared::Firm[*];\n" +
                "}\n" +
                "Class test::src::Person\n" +
                "{\n" +
                "   fullName: String[1];\n" +
                "   firm: test::shared::Firm[*];\n" +
                "}\n" +
                "Class test::shared::Firm\n" +
                "{\n" +
                "   id: String[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Mapping\n" +
                "(\n" +
                "  *test::dest::Person: Pure\n" +
                "  {\n" +
                "    ~src test::src::Person\n" +
                "    firstName: $src.fullName->substring(0, $src.fullName->indexOf(' ')),\n" +
                "    lastName: $src.fullName->substring($src.fullName->indexOf(' ') + 1, $src.fullName->length()),\n" +
                "    firm: $src.firm\n" +
                "  }\n" +
                ")\n");
    }

    @Test
    public void testMappingTestSuite()
    {
        test("###Pure\n" +
                "Class test::model\n" +
                "{\n" +
                "    name: String[1];\n" +
                "    id: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::changedModel{    name: String[1];    id: Integer[1];}\n" +
                "###Data\n" +
                "Data test::data::MyData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/json';\n" +
                "    data: '{\"name\":\"john doe\",\"id\":\"77\"}';\n" +
                "  }#\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::modelToModelMapping\n" +
                "(\n" +
                "    *test::changedModel: Pure\n" +
                "{\n" +
                "    ~src test::model\n" +
                "    name: $src.name,\n" +
                "    id: $src.id->parseInteger()\n" +
                "}\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      function: |test::changedModel.all()->graphFetch(#{test::changedModel{id,name}}#)->serialize(#{test::changedModel{id,name}}#);\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          data:\n" +
                "          [\n" +
                "            ModelStore: ModelStore\n" +
                "            #{\n" +
                "               test::model:\n" +
                "                Reference \n" +
                "                #{ \n" +
                "                  test::data::MyData\n" +
                "                }#\n" +
                "            }#\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                ")\n" +
                "\n");

        test("###Pure\n" +
                "Class test::model\n" +
                "{\n" +
                "    name: String[1];\n" +
                "    id: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::changedModel{    name: String[1];    id: Integer[1];}\n" +
                "###Data\n" +
                "Data test::data::MyData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/json';\n" +
                "    data: '{\"name\":\"john doe\",\"id\":\"77\"}';\n" +
                "  }#\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::modelToModelMapping\n" +
                "(\n" +
                "    *test::changedModel: Pure\n" +
                "{\n" +
                "    ~src test::model\n" +
                "    name: $src.name,\n" +
                "    id: $src.id->parseInteger()\n" +
                "}\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      function: |test::changedModel.all();\n" +
                "      tests:\n" +
                "      [];\n" +
                "    }\n" +
                "  ]\n" +
                ")\n" +
                "\n",
                "COMPILATION error at [30:5-35:5]: Mapping TestSuites should have at least 1 test");
        test("###Pure\n" +
                "Class test::model\n" +
                "{\n" +
                "    name: String[1];\n" +
                "    id: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::changedModel{    name: String[1];    id: Integer[1];}\n" +
                "###Data\n" +
                "Data test::data::MyData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/json';\n" +
                "    data: '{\"name\":\"john doe\",\"id\":\"77\"}';\n" +
                "  }#\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::modelToModelMapping\n" +
                "(\n" +
                "    *test::changedModel: Pure\n" +
                "{\n" +
                "    ~src test::model\n" +
                "    name: $src.name,\n" +
                "    id: $src.id->parseInteger()\n" +
                "}\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      function: |test::changedModel.all()->graphFetch(#{test::changedModel{id,name}}#)->serialize(#{test::changedModel{id,name}}#);\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          data:\n" +
                "          [\n" +
                "            ModelStore: ModelStore\n" +
                "            #{\n" +
                "               test::model:\n" +
                "                Reference \n" +
                "                #{ \n" +
                "                  test::data::MyData\n" +
                "                }#\n" +
                "            }#\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{}';\n" +
                "                  }#;\n" +
                "              }#,\n" +
                "            assert2:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                ")\n" +
                "\n", "COMPILATION error at [35:9-71:9]: Mapping Tests can only have one assertion");

    }

    @Test
    public void testMappingSuiteDataReference()
    {
        test("###Data\n" +
                "Data model::PersonData\n" +
                "{\n" +
                "  ExternalFormat\n" +
                "  #{\n" +
                "    contentType: 'application/json';\n" +
                "    data: '';\n" +
                "  }#\n" +
                "}\n" +
                "\n" +
                "Data model::ModelStoreRef\n" +
                "{\n" +
                "  ModelStore\n" +
                "  #{\n" +
                "    model::Person:\n" +
                "      Reference\n" +
                "      #{\n" +
                "        model::PersonData\n" +
                "      }#\n" +
                "  }#\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Pure\n" +
                "Class model::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class model::TargetPerson\n" +
                "{\n" +
                "  fullName: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping model::M2MSimpleMapping\n" +
                "(\n" +
                "  *model::TargetPerson: Pure\n" +
                "  {\n" +
                "    ~src model::Person\n" +
                "    fullName: $src.firstName + ' ' + $src.lastName\n" +
                "  }\n" +
                "\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    graphFetchSuite:\n" +
                "    {\n" +
                "      function: |model::TargetPerson.all()->graphFetch(\n" +
                "  #{\n" +
                "    model::TargetPerson{\n" +
                "      fullName\n" +
                "    }\n" +
                "  }#\n" +
                ")->serialize(\n" +
                "  #{\n" +
                "    model::TargetPerson{\n" +
                "      fullName\n" +
                "    }\n" +
                "  }#\n" +
                ");\n" +
                "      tests:\n" +
                "      [\n" +
                "        dataReferenceOnPerson:\n" +
                "        {\n" +
                "          data:\n" +
                "          [\n" +
                "            ModelStore:\n" +
                "              ModelStore\n" +
                "              #{\n" +
                "                model::Person:\n" +
                "                  Reference\n" +
                "                  #{\n" +
                "                    model::PersonData\n" +
                "                  }#\n" +
                "              }#\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            expectedAssertion:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected:\n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        },\n" +
                "        dataReference:\n" +
                "        {\n" +
                "          data:\n" +
                "          [\n" +
                "            ModelStore:\n" +
                "              Reference\n" +
                "              #{\n" +
                "                model::ModelStoreRef\n" +
                "              }#\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            expectedAssertion:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected:\n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                ")\n");
    }
}
