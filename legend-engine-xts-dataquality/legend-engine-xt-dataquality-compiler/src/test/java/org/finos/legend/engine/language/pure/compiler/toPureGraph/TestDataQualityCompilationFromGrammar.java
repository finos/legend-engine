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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestDataQualityCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityValidation meta::dataquality::Person\n" + // duplicated the validation name, same name as the class
                "{\n" +
                "    context: fromMappingAndRuntime(meta::dataquality::dataquality::Mapping, meta::dataquality::DataQualityRuntime);\n" +
                "    filter: p:meta::dataquality::Person[1] | $p.name=='John';\n" +
                "    validationTree: $[\n" +
                "      meta::dataquality::Person<mustBeOfLegalAge>{\n" +
                "        name\n" +
                "      }\n" +
                "    ]$;\n" +
                "}";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [104:1-113:1]: Duplicated element 'meta::dataquality::Person'";
    }

    @Test
    public void testHappyPath()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityValidation meta::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromMappingAndRuntime(meta::dataquality::dataquality::Mapping, meta::dataquality::DataQualityRuntime);\n" +
                "    filter: p:meta::dataquality::Person[1] | $p.name=='John';\n" +
                "    validationTree: $[\n" +
                "      meta::dataquality::Person<mustBeOfLegalAge>{\n" +
                "        name\n" +
                "      }\n" +
                "    ]$;\n" +
                "}");
    }

    @Test
    public void testHappyPath_withDataspace()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityValidation meta::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromDataSpace(meta::dataquality::PersonDataspace, 'default');\n" +
                "    filter: p:meta::dataquality::Person[1] | $p.name=='John';\n" +
                "    validationTree: $[\n" +
                "      meta::dataquality::Person<mustBeOfLegalAge>{\n" +
                "        name\n" +
                "      }\n" +
                "    ]$;\n" +
                "}");
    }

    @Test
    public void testRootClassNoModelConstraints()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityValidation meta::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromMappingAndRuntime(meta::dataquality::dataquality::Mapping, meta::dataquality::DataQualityRuntime);\n" +
                "    filter: p:meta::dataquality::Person[1] | $p.name=='John';\n" +
                "    validationTree: $[\n" +
                "      meta::dataquality::Person{\n" +
                "        name\n" +
                "      }\n" +
                "    ]$;\n" +
                "}");
    }

    @Test
    public void testRootClassNoStructuralConstraints()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityValidation meta::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromMappingAndRuntime(meta::dataquality::dataquality::Mapping, meta::dataquality::DataQualityRuntime);\n" +
                "    filter: p:meta::dataquality::Person[1] | $p.name=='John';\n" +
                "    validationTree: $[\n" +
                "      meta::dataquality::Person<mustBeOfLegalAge>{\n" +
                "      }\n" +
                "    ]$;\n" +
                "}");
    }

    @Test
    public void testRootClassEmptyTree()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityValidation meta::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromMappingAndRuntime(meta::dataquality::dataquality::Mapping, meta::dataquality::DataQualityRuntime);\n" +
                "    filter: p:meta::dataquality::Person[1] | $p.name=='John';\n" +
                "    validationTree: $[\n" +
                "      meta::dataquality::Person{\n" +
                "      }\n" +
                "    ]$;\n" +
                "}", " at [104:1-112:1]: Error in 'meta::dataquality::PersonDataQualityValidation': Execution error at (resource: lines:104c1-112c1), \"Constraint :[mustHaveAtLeastOnePropertyOrConstraint] violated in the Class DataQualityRootGraphFetchTree\"");
    }


    @Test
    public void testRelationValidation_row_level() // row level tests for backward compatibility
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->from(meta::dataquality::DataQualityRuntime);\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'validFirstName';\n" +
                "         description: 'First name cannot be empty';\n" +
                "         assertion: row|$row.FIRSTNAME->isNotEmpty();\n" +
                "         type: ROW_LEVEL;\n" +
                "      }\n" +
                "    ];\n" +
                "}");
    }

    @Test
    public void testRelationValidation_forClass_fromMappingRuntime()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: meta::dataquality::Person.all()->project(~[theName: x|$x.name])" +
                "       ->from(meta::dataquality::dataquality::Mapping, meta::dataquality::DataQualityRuntime);\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'validTheName';\n" +
                "         description: 'theName cannot be empty';\n" +
                "         assertion: row|$row.theName->isNotEmpty();\n" +
                "         type: ROW_LEVEL;\n" +
                "      }\n" +
                "    ];\n" +
                "}");
    }

    @Test
    public void testRelationValidation_absentRuntime()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::Validation\n" +
                "{\n" +
                "    query: #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME);\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'validFirstName';\n" +
                "         description: 'First name cannot be empty';\n" +
                "         assertion: row|$row.FIRSTNAME->isNotEmpty();\n" +
                "         type: ROW_LEVEL;\n" +
                "      }\n" +
                "    ];\n" +
                "}", " at [104:1-115:1]: Error in 'meta::external::dataquality::Validation': Execution error at (resource: lines:104c1-115c1), \"Constraint :[mustEndWithRuntime] violated in the Class DataQualityRelationValidation\"");
    }

    @Test
    public void testRelationValidation()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->from(meta::dataquality::DataQualityRuntime);\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: rel|$rel->assertRelationNotEmpty();\n" +
                "      }\n" +
                "    ];\n" +
                "}");

        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->from(meta::dataquality::DataQualityRuntime);\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: rel|$rel->assertRelationNotEmpty();\n" +
                "      }\n" +
                "    ];\n" +
                "}");

        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->from(meta::dataquality::DataQualityRuntime);\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: rel|$rel->assertRelationNotEmpty();\n" +
                "         type: AGGREGATE;\n" +
                "      }\n" +
                "    ];\n" +
                "}");

        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: {firstName:String[1]| #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->filter(r| $r.FIRSTNAME == $firstName)->from(meta::dataquality::DataQualityRuntime)};\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: {firstName:String[1], rel| $rel->assertRelationNotEmpty()};\n" +
                "         type: AGGREGATE;\n" +
                "      }\n" +
                "    ];\n" +
                "}");
    }

    @Test
    public void testRelationValidation_MultilineQuery()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: {|let l = #>{meta::dataquality::db.personTable}#->select(~[FIRSTNAME, LASTNAME])->from(meta::dataquality::DataQualityRuntime); $l->select(~FIRSTNAME);};\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: rel|$rel->assertRelationNotEmpty();\n" +
                "      }\n" +
                "    ];\n" +
                "}", "COMPILATION error at [106:13-161]: Multiline lambda is not supported.");
    }

    @Test
    public void testRelationValidation_MultilineAssertion()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: {|let l = #>{meta::dataquality::db.personTable}#->select(~[FIRSTNAME, LASTNAME])->from(meta::dataquality::DataQualityRuntime); $l->select(~FIRSTNAME);};\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: {rel|let isEmpty = $rel->assertRelationNotEmpty(); isEmpty;};\n" +
                "      }\n" +
                "    ];\n" +
                "}", "COMPILATION error at [106:13-161]: Multiline lambda is not supported.");
    }

    @Test
    public void testRelationValidation_query_params()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: {firstName:String[1]| #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->filter(r| $r.FIRSTNAME == $firstName)->from(meta::dataquality::DataQualityRuntime)};\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: {firstName:String[1], rel| $rel->assertRelationNotEmpty()};\n" +
                "         type: AGGREGATE;\n" +
                "      }\n" +
                "    ];\n" +
                "}");

        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: {firstName:String[1], businessDate:String[1]| #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->filter(r| $r.FIRSTNAME == $firstName)->from(meta::dataquality::DataQualityRuntime)};\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: {firstName, rel| $rel->assertRelationNotEmpty()};\n" +
                "      }\n" +
                "    ];\n" +
                "}", "COMPILATION error at [111:36-67]: Invalid parameter specified on assertion - firstName. Please validate that it is exactly the same as query parameters(name/type/multiplicity)");

        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: {firstName:String[1], businessDate:String[1]| #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->filter(r| $r.FIRSTNAME == $firstName)->from(meta::dataquality::DataQualityRuntime)};\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: {lastName:String[1], rel| $rel->assertRelationNotEmpty()};\n" +
                "      }\n" +
                "    ];\n" +
                "}", "COMPILATION error at [111:22-39]: Invalid parameter specified on assertion - lastName. Please validate that it is exactly the same as query parameters(name/type/multiplicity)");

        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: {firstName:String[1], businessDate:String[1]| #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->filter(r| $r.FIRSTNAME == $firstName)->from(meta::dataquality::DataQualityRuntime)};\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: {firstName:String[1] | $rel->assertRelationNotEmpty()};\n" +
                "      }\n" +
                "    ];\n" +
                "}", "COMPILATION error at [111:42-73]: Assertion param 'rel' is missing!");
    }

    @Test
    public void testRelationValidation_assertionValidation()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->from(meta::dataquality::DataQualityRuntime);\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'idLength';\n" +
                "         description: 'id length';\n" +
                "         assertion: row|$row.FIRSTNAME->length();\n" +
                "         type: ROW_LEVEL;\n" +
                "      }\n" +
                "    ];\n" +
                "}", "COMPILATION error at [111:24-48]: Assertion should return Boolean");

        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(COMPILATION_PREREQUISITE_CODE +
                "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{meta::dataquality::db.personTable}#->select(~FIRSTNAME)->from(meta::dataquality::DataQualityRuntime);\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'nonEmptyDataset';\n" +
                "         description: 'dataset cannot be empty';\n" +
                "         assertion: rel|$rel->filter(r|$r.FIRSTNAME->isNotEmpty());\n" +
                "      }\n" +
                "    ];\n" +
                "}", "COMPILATION error at [111:24-66]: Assertion should end in either assertRelationEmpty or assertRelationNotEmpty functions");
    }



    private static final String COMPILATION_PREREQUISITE_CODE = "###Connection\n" +
            "RelationalDatabaseConnection meta::dataquality::H2\n" +
            "{\n" +
            "  store: meta::dataquality::db;\n" +
            "  type: H2;\n" +
            "  specification: LocalH2\n" +
            "  { \n" +
            "    testDataSetupSqls: [];\n" +
            "  };\n" +
            "  auth: DefaultH2;\n" +
            "}\n" +
            "\n" +
            "###Relational\n" +
            "Database meta::dataquality::db\n" +
            "(\n" +
            "   Table personTable (ID INT PRIMARY KEY, FIRSTNAME VARCHAR(200), LASTNAME VARCHAR(200), AGE INT, ADDRESSID INT, FIRMID INT)\n" +
            "   Table addressTable (ID INT PRIMARY KEY, LOCATIONID INT, TYPE INT)\n" +
            "   Table locationTable (ID INT PRIMARY KEY, STREET VARCHAR(200), LOCALITY VARCHAR(200))\n" +
            "\n" +
            "   Join Address_Person(addressTable.ID = personTable.ADDRESSID)\n" +
            "   Join Address_Location(addressTable.LOCATIONID = locationTable.ID)\n" +
            ")\n" +
            "\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping meta::dataquality::dataquality::Mapping\n" +
            "(\n" +
            "\n" +
            "   meta::dataquality::Person : Relational\n" +
            "   {\n" +
            "      name : [meta::dataquality::db]personTable.FIRSTNAME,\n" +
            "      age : [meta::dataquality::db]personTable.AGE,\n" +
            "      addresses : [meta::dataquality::db]@Address_Person\n" +
            "   }\n" +
            "\n" +
            "   meta::dataquality::Address : Relational\n" +
            "   {\n" +
            "      addressId : [meta::dataquality::db]addressTable.ID,\n" +
            "      location : [meta::dataquality::db]@Address_Location\n" +
            "   }\n" +
            "\n" +
            "   meta::dataquality::Location : Relational\n" +
            "   {\n" +
            "      street : [meta::dataquality::db]locationTable.STREET,\n" +
            "      locality : [meta::dataquality::db]locationTable.LOCALITY\n" +
            "   }\n" +
            ")\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime meta::dataquality::DataQualityRuntime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    meta::dataquality::dataquality::Mapping\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    meta::dataquality::db:\n" +
            "    [\n" +
            "      connection_1: meta::dataquality::H2\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n" +
            "\n" +
            "\n" +
            "\n" +
            "###Pure\n" +
            "Class meta::dataquality::Person\n" +
            "[\n" +
            "  mustBeOfLegalAge: $this.age >= 18\n" +
            "]\n" +
            "{\n" +
            "   name : String[1];\n" +
            "   age : Integer[1];\n" +
            "   addresses : meta::dataquality::Address[*];\n" +
            "}\n" +
            "\n" +
            "Class meta::dataquality::Address\n" +
            "{\n" +
            "   location: meta::dataquality::Location[1];\n" +
            "   locationStreet: String[1];\n" +
            "   addressId: String[1];\n" +
            "}\n" +
            "\n" +
            "Class meta::dataquality::Location\n" +
            "{\n" +
            "   street: String[1];\n" +
            "   locality: String[1];\n" +
            "}\n" +
            "###DataSpace\n" +
            "DataSpace meta::dataquality::PersonDataspace\n" +
            "{\n" +
            "  executionContexts:\n" +
            "  [\n" +
            "    {\n" +
            "      name: 'default';\n" +
            "      mapping: meta::dataquality::dataquality::Mapping;\n" +
            "      defaultRuntime: meta::dataquality::DataQualityRuntime;\n" +
            "    }\n" +
            "  ];\n" +
            "  defaultExecutionContext: 'default';\n" +
            "}\n";

}
