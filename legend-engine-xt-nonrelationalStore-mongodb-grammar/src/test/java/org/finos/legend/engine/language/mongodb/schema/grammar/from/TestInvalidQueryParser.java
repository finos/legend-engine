// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.mongodb.schema.grammar.from;

import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@RunWith(Parameterized.class)
public class TestInvalidQueryParser
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final String inputJsonFile;

    private final String expectedErrorMessage;
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    public TestInvalidQueryParser(String inputQueryFile, String errMessage)
    {
        this.inputJsonFile = inputQueryFile;
        this.expectedErrorMessage = errMessage;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][]{
//                {"json/query/project_with_incl_and_excl_should_throw_input.json"},
                {"json/query/stage_should_have_1_field.json", "Expected Stage command node to be an object, with just 1 key(stage)"},
                {"json/query/invalid_not_operator_expression_formats_input.json", "Operator need object node (eg., $not)"},
                {"json/query/invalid_match_with_and_operator_input.json", "Logical Operators need non-zero array of Object Expressions ($and, $or, $nor)"},
                {"json/query/invalid_match_with_and_operator_input_empty_array.json", "Logical Operators need non-zero array of Object Expressions ($and, $or, $nor)"},
                {"json/query/invalid_match_with_eq_operator_input.json", "Field Based operation cannot mix  exprOperation & {field : value} syntax"},
                {"json/query/invalid_project_with_single_inclusion_filter_input.json", "Project syntax supports only field: 1 / bool"},
        });
    }

    @Test
    public void testExceptionForInvalidQuery()
    {
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage(this.expectedErrorMessage);
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(this.inputJsonFile));
        String inputQry = null;
        try
        {
            inputQry = new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
        }
        catch (IOException | URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        // Parse
        MongoDBQueryParseTreeWalker parser = MongoDBQueryParseTreeWalker.newInstance();
        DatabaseCommand dbCommand = parser.parseQueryDocument(inputQry);

    }
}
