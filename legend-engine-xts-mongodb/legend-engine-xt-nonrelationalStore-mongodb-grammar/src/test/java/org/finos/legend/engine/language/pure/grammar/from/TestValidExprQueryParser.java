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

import org.finos.legend.engine.language.pure.grammar.to.MongoDBQueryJsonComposer;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@RunWith(Parameterized.class)
public class TestValidExprQueryParser
{

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TestValidExprQueryParser.class);
    private final String inputJsonFile;

    public TestValidExprQueryParser(String inputQueryFile)
    {
        inputJsonFile = inputQueryFile;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][]{
                {"json/exprquery/match_and_query.json"},
                {"json/exprquery/match_eq_query.json"},
                {"json/exprquery/match_empty_query.json"},
                {"json/exprquery/empty_pipeline.json"},
                {"json/exprquery/match_eq_object_query.json"},
                {"json/exprquery/match_eq_empty_array.json"},
                {"json/exprquery/match_eq_nested_object.json"},
                {"json/exprquery/match_or_query.json"},
                {"json/exprquery/match_eq_nonempty_array.json"},
                {"json/exprquery/match_duplicate_stage.json"},
                {"json/exprquery/match_and_multioperator_query.json"},
                {"json/exprquery/match_dates.json"},
                {"json/exprquery/project_multi_field.json"},
                {"json/exprquery/project_single_field.json"}
        });
    }


    @Test
    public void testSimpleMatchOperator() throws Exception
    {
        LOGGER.debug("Processing file: {}", this.inputJsonFile);
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(this.inputJsonFile));
        String inputQry = new String(Files.readAllBytes(Paths.get(url.toURI())), StandardCharsets.UTF_8);

        MongoDBQueryParser parser = MongoDBQueryParser.newInstance();

        // Pretty print input to canonical json format for test assertions
        String expected = parser.getObjectMapper().readTree(inputQry).toPrettyString();

        // Parse
        DatabaseCommand dbCommand = parser.parseQueryDocument(inputQry);
        //Compose back to String
        MongoDBQueryJsonComposer qryComposer = new MongoDBQueryJsonComposer();
        String qryComposerOutput = qryComposer.parseDatabaseCommand(dbCommand);

        // Pretty print output from composer for test assertions
        String actual = parser.getObjectMapper().readTree(qryComposerOutput).toPrettyString();

        Assert.assertEquals(expected, actual);

    }


}
