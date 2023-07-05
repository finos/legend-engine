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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestValidExprQueryParserWithoutTypeIdentifier
{
    @Test
    public void testSimpleMatchOperator() throws Exception
    {
        String inputQry = new String(Files.readAllBytes(Paths.get("src/test/resources/json/exprquery/match_and_query.json")), StandardCharsets.UTF_8);
        String expectedJson = new String(Files.readAllBytes(Paths.get("src/test/resources/json/exprquery/match_and_query_without_type_output.json")), StandardCharsets.UTF_8);

        MongoDBQueryParser parser = MongoDBQueryParser.newInstance();

        // Pretty print input to canonical json format for test assertions
        String expected = parser.getObjectMapper().readTree(expectedJson).toPrettyString();

        // Parse
        DatabaseCommand dbCommand = parser.parseQueryDocument(inputQry);
        //Compose back to String
        MongoDBQueryJsonComposer qryComposer = new MongoDBQueryJsonComposer(false);
        String qryComposerOutput = qryComposer.parseDatabaseCommand(dbCommand);

        // Pretty print output from composer for test assertions
        String actual = parser.getObjectMapper().readTree(qryComposerOutput).toPrettyString();

        Assert.assertEquals(expected, actual);
    }
}