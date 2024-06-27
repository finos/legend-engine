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

package org.finos.legend.engine.language.dataquality.grammar.to;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestDataQualityRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{

    @Test
    public void testDataQuality_withModelAndStructuralConstraints()
    {
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "   context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "   validationTree: $[\n" +
                "      Person<ageMustBePositive, 'nameMust NotBeBlank'>{\n" +
                "        lastName,\n" +
                "        name,\n" +
                "        addresses<streetMustNotBeBlank>{\n" +
                "          addressId\n" +
                "        }\n" +
                "      }\n" +
                "    ]$;\n" +
                "}\n");
    }

    @Test
    public void testDataQuality_withFilter()
    {
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "   context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "   validationTree: $[\n" +
                "      Person<ageMustBePositive, 'nameMust NotBeBlank'>{\n" +
                "        lastName,\n" +
                "        name,\n" +
                "        age,\n" +
                "        addresses{\n" +
                "          addressId\n" +
                "        }\n" +
                "      }\n" +
                "    ]$;\n" +
                "   filter: p: Person[1]|$p.age >= 18;\n" +
                "}\n");
    }

    @Test
    public void testDataQuality_withMappingAndRuntime()
    {
        test("###DataQualityValidation\n" +
                "DataQualityValidation meta::external::dataquality::PersonDataQualityValidation\n" +
                "{\n" +
                "   context: fromDataSpace(meta::external::dataquality::PersonDataSpace, 'Local_Context');\n" +
                "   validationTree: $[\n" +
                "      Person<ageMustBePositive, 'nameMust NotBeBlank'>{\n" +
                "        lastName,\n" +
                "        name,\n" +
                "        age,\n" +
                "        addresses{\n" +
                "          addressId\n" +
                "        }\n" +
                "      }\n" +
                "    ]$;\n" +
                "}\n");
    }

    @Test
    public void testDataQuality_withOnlyStructuralConstraints()
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
                "}\n");
    }


}
