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

package org.finos.legend.engine.external.format.json.specifications;

import org.finos.legend.engine.external.shared.format.model.test.ExternalSchemaCompilationTest;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.junit.Test;

public class TestJsonSchemaCompilation extends ExternalSchemaCompilationTest
{
    protected void testJsonSchema(String jsonSchema, String location)
    {
        testJsonSchema(jsonSchema, location, null);
    }

    protected void testJsonSchema(String jsonSchema, String location, String expectedError)
    {
        test("###ExternalFormat\n" +
                        "SchemaSet test::Example1\n" +
                        "{\n" +
                        "  format: JSON;\n" +
                        "  schemas: [ { location: '" + location + "';\n" +
                        "               content: " + PureGrammarComposerUtility.convertString(jsonSchema, true) + "; } ];\n" +
                        "}\n",
                expectedError
        );
    }
}
