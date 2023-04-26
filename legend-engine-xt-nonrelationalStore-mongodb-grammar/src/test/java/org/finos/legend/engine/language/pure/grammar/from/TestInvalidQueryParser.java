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

package org.finos.legend.engine.language.pure.grammar.from;

import org.junit.Assert;
import org.junit.Test;
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
    private final String inputJsonFile;

    private final String expectedErrorMessage;

    public TestInvalidQueryParser(String inputQueryFile, String errMessage)
    {
        this.inputJsonFile = inputQueryFile;
        this.expectedErrorMessage = errMessage;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][]{
                //TODO:     {"json/query/invalid_project_mixed_selection.json"},
                {"json/query/invalid_multi_stage.json", "Expected Stage command node to be an object, with just 1 key(stage)"},
                {"json/query/invalid_match_non_expr.json", "Match stage supports only  $expr style syntax"},
                {"json/query/invalid_project_syntax.json", "Project syntax supports only field: 1 / bool"},
        });
    }

    @Test
    public void testExceptionForInvalidQuery()
    {
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(this.inputJsonFile));
        String inputQry;
        try
        {
            inputQry = new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);
        }
        catch (IOException | URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        // Parse
        MongoDBQueryParser parser = MongoDBQueryParser.newInstance();
        IllegalStateException exception = Assert.assertThrows(IllegalStateException.class, () -> parser.parseQueryDocument(inputQry));
        Assert.assertEquals(this.expectedErrorMessage, exception.getMessage());

    }
}
