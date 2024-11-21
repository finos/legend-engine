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


}
