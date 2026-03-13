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

package org.finos.legend.engine.language.dataquality.grammar.from;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataQualityParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestDataQualityParsing extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DataQualityParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###DataQualityValidation\n" +
                "DataQualityValidation " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "   context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "   validationTree: $[\n" +
                "      Person{\n" +
                "        lastName,\n" +
                "        name,\n" +
                "        age,\n" +
                "        addresses{\n" +
                "          addressId\n" +
                "        }\n" +
                "      }\n" +
                "    ]$;\n" +
                "   filter: p: Person[1]|$p.age >= 18;\n" +
                "}";
    }

    @Test
    public void testParserErrorForMandatoryFields()
    {
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "   validationTree: $[\n" +
                "      Person{\n" +
                "        lastName,\n" +
                "        name,\n" +
                "        age,\n" +
                "        addresses{\n" +
                "          addressId\n" +
                "        }\n" +
                "      }\n" +
                "    ]$;\n" +
                "   filter: p: Person[1]|$p.age >= 18;\n" +
                "}", "PARSER error at [2:1-15:1]: Field 'context' is required");

        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "   context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "   filter: p: Person[1]|$p.age >= 18;\n" +
                "}", "PARSER error at [2:1-6:1]: Field 'validationTree' is required");
    }

    @Test
    public void testParserForValidGrammar_withNoConstraints()
    {
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "   context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "   validationTree: $[\n" +
                "      Person{\n" +
                "        lastName,\n" +
                "        name,\n" +
                "        age,\n" +
                "        addresses{\n" +
                "          addressId\n" +
                "        }\n" +
                "      }\n" +
                "    ]$;\n" +
                "   filter: p: Person[1]|$p.age >= 18;\n" +
                "}");

    }

    @Test
    public void testParserForValidGrammar()
    {
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "    validationTree: $[\n" +
                "     Person<ageMustBePositive, 'nameMust NotBeBlank'>{\n" +
                "       lastName,\n" +
                "       name,\n" +
                "       addresses{\n" +
                "         addressId\n" +
                "       }\n" +
                "     }\n" +
                "    ]$;\n" +
                "    filter: p:Person[1] | p.age >= 18;\n" +
                "}");
        // whitespace chars in constraint names
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "    validationTree: $[\n" +
                "     Person<ageMustBePositive, 'nameMust \\r\\nNotBeBlank'>{\n" +
                "       lastName,\n" +
                "       name,\n" +
                "       addresses{\n" +
                "         addressId\n" +
                "       }\n" +
                "     }\n" +
                "    ]$;\n" +
                "    filter: p:Person[1] | p.age >= 18;\n" +
                "}");

    }

    @Test
    public void testEdgeScenarios()
    {
        // only model constraints
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "    validationTree: $[\n" +
                "     Person<ageMustBePositive>{\n" +
                "     }\n" +
                "    ]$;\n" +
                "}");
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "    validationTree: $[\n" +
                "     Person{\n" +
                "       addresses<idMustBeValid>{\n" +
                "       }\n" +
                "     }\n" +
                "    ]$;\n" +
                "}");
        // only structural constraints
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "    validationTree: $[\n" +
                "     Person{\n" +
                "        age\n" +
                "     }\n" +
                "    ]$;\n" +
                "}");
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "    validationTree: $[\n" +
                "     Person{\n" +
                "       addresses{\n" +
                "         id\n" +
                "       }\n" +
                "     }\n" +
                "    ]$;\n" +
                "}");
        // both model and structural constraints are absent
        // note: parser is lenient to allow empty trees but fails in compilation as constraints are added in tree pure model
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "    context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "    validationTree: $[\n" +
                "     Person{\n" +
                "     }\n" +
                "    ]$;\n" +
                "}");

    }

    @Test
    public void testParserForValidRelationValidationGrammar()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{my::Store.myTable}#->filter(c|$c.name == 'ok');\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'testValidation';\n" +
                "         description: 'test validation';\n" +
                "         assertion: row|$row.name != 'error';\n" +
                "      }\n" +
                "    ];\n" +
                "}");

        // with description as optional
        test("###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{my::Store.myTable}#->filter(c|$c.name == 'ok');\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'testValidation';\n" +
                "         assertion: row|$row.name != 'error';\n" +
                "      }\n" +
                "    ];\n" +
                "}");
    }

    @Test
    public void testParserForValidRelationValidationGrammar_withValidationTypes()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{my::Store.myTable}#->filter(c|$c.name == 'ok');\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'testValidation';\n" +
                "         description: 'test validation';\n" +
                "         assertion: row|$row.name != 'error';\n" +
                "         type: ROW_LEVEL;\n" +
                "      }\n" +
                "    ];\n" +
                "}");

        test("###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{my::Store.myTable}#->filter(c|$c.name == 'ok');\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'testValidation';\n" +
                "         assertion: rel|$rel->size()>0;\n" +
                "         type: AGGREGATE;" +
                "      }\n" +
                "    ];\n" +
                "}");
    }

    @Test
    public void testParserErrorForMandatoryFields_relationalValidations()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'testValidation';\n" +
                "         description: 'test validation';\n" +
                "         assertion: row|$row.name != 'error';\n" +
                "      }\n" +
                "    ];\n" +
                "}", "PARSER error at [2:1-11:1]: Field 'query' is required");

        test("###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{my::Store.myTable}#->filter(c|$c.name == 'ok');\n" +
                "}", "PARSER error at [2:1-5:1]: Field 'validations' is required");

        test("###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{my::Store.myTable}#->filter(c|$c.name == 'ok');\n" +
                "    validations: [\n" +
                "      {\n" +
                "         description: 'test validation';\n" +
                "         assertion: row|$row.name != 'error';\n" +
                "      }\n" +
                "    ];\n" +
                "}", "PARSER error at [2:1-11:1]: Field 'name' is required");

        test("###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::external::dataquality::testvalidation\n" +
                "{\n" +
                "    query: #>{my::Store.myTable}#->filter(c|$c.name == 'ok');\n" +
                "    validations: [\n" +
                "      {\n" +
                "         name: 'testValidation';\n" +
                "      }\n" +
                "    ];\n" +
                "}", "PARSER error at [2:1-10:1]: Field 'assertion' is required");
    }

    @Test
    public void testParserForValidRelationComparisonGrammar_allFields()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationComparison meta::external::dataquality::testRecon\n" +
                "{\n" +
                "    source: src|#>{my::Store.sourceTable}#->filter(c|$c.active == true);\n" +
                "    target: tgt|#>{my::Store.targetTable}#->filter(c|$c.active == true);\n" +
                "    keys: [id, name];\n" +
                "    columnsToCompare: [amount, quantity];\n" +
                "    strategy: MD5Hash\n" +
                "    {\n" +
                "        sourceHashColumn: srcHash;\n" +
                "        targetHashColumn: tgtHash;\n" +
                "        aggregatedHash: true;\n" +
                "    };\n" +
                "    expectedMatch: 0.99;\n" +
                "}");
    }

    @Test
    public void testParserForValidRelationComparisonGrammar_requiredFieldsOnly()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationComparison meta::external::dataquality::testRecon\n" +
                "{\n" +
                "    source: src|#>{my::Store.sourceTable}#->filter(c|$c.active == true);\n" +
                "    target: tgt|#>{my::Store.targetTable}#->filter(c|$c.active == true);\n" +
                "    keys: [id];\n" +
                "    strategy: MD5Hash;\n" +
                "}");
    }

    @Test
    public void testParserForValidRelationComparisonGrammar_withEmptyStrategyOptions()
    {
        // test with nothing - should fail
        test("###DataQualityValidation\n" +
                "DataQualityRelationComparison meta::external::dataquality::testRecon\n" +
                "{\n" +
                "    source: src|#>{my::Store.sourceTable}#->filter(c|$c.active == true);\n" +
                "    target: tgt|#>{my::Store.targetTable}#->filter(c|$c.active == true);\n" +
                "    keys: [id];\n" +
                "    strategy: MD5Hash\n" +
                "    {\n" +
                "    };\n" +
                "}", "PARSER error at [9:5]: Unexpected token '}'. Valid alternatives: ['sourceHashColumn', 'targetHashColumn', 'aggregatedHash']");
    }

    @Test
    public void testParserErrorForMandatoryFields_relationComparison_missingSource()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationComparison meta::external::dataquality::testRecon\n" +
                "{\n" +
                "    target: tgt|#>{my::Store.targetTable}#->filter(c|$c.active == true);\n" +
                "    keys: [id];\n" +
                "    strategy: MD5Hash;\n" +
                "}", "PARSER error at [2:1-7:1]: Field 'source' is required");
    }

    @Test
    public void testParserErrorForMandatoryFields_relationComparison_missingTarget()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationComparison meta::external::dataquality::testRecon\n" +
                "{\n" +
                "    source: src|#>{my::Store.sourceTable}#->filter(c|$c.active == true);\n" +
                "    keys: [id];\n" +
                "    strategy: MD5Hash;\n" +
                "}", "PARSER error at [2:1-7:1]: Field 'target' is required");
    }

    @Test
    public void testParserErrorForMandatoryFields_relationComparison_missingKeys()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationComparison meta::external::dataquality::testRecon\n" +
                "{\n" +
                "    source: src|#>{my::Store.sourceTable}#->filter(c|$c.active == true);\n" +
                "    target: tgt|#>{my::Store.targetTable}#->filter(c|$c.active == true);\n" +
                "    strategy: MD5Hash;\n" +
                "}", "PARSER error at [2:1-7:1]: Field 'keys' is required");
    }

    @Test
    public void testParserErrorForMandatoryFields_relationComparison_missingStrategy()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationComparison meta::external::dataquality::testRecon\n" +
                "{\n" +
                "    source: src|#>{my::Store.sourceTable}#->filter(c|$c.active == true);\n" +
                "    target: tgt|#>{my::Store.targetTable}#->filter(c|$c.active == true);\n" +
                "    keys: [id];\n" +
                "}", "PARSER error at [2:1-7:1]: Field 'strategy' is required");
    }

    @Test
    public void testParserErrorForValidRelationComparisonGrammar_emptyColumnsToCompare()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationComparison meta::external::dataquality::testRecon\n" +
                "{\n" +
                "    source: src|#>{my::Store.sourceTable}#->filter(c|$c.active == true);\n" +
                "    target: tgt|#>{my::Store.targetTable}#->filter(c|$c.active == true);\n" +
                "    keys: [id];\n" +
                "    columnsToCompare: [];\n" +
                "    strategy: MD5Hash;\n" +
                "}", "PARSER error at [7:24]: Unexpected token");
    }

        @Test
    public void testParserErrorForValidRelationComparisonGrammar_emptyKeys()
    {
        test("###DataQualityValidation\n" +
                "DataQualityRelationComparison meta::external::dataquality::testRecon\n" +
                "{\n" +
                "    source: src|#>{my::Store.sourceTable}#->filter(c|$c.active == true);\n" +
                "    target: tgt|#>{my::Store.targetTable}#->filter(c|$c.active == true);\n" +
                "    keys: [];\n" +
                "    strategy: MD5Hash;\n" +
                "}", "PARSER error at [6:12]: Unexpected token");
    }

}
