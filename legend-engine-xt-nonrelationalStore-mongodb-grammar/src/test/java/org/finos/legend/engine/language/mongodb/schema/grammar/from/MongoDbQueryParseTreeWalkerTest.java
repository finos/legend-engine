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

package org.finos.legend.engine.language.mongodb.schema.grammar.from;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryBaseListener;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryLexer;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryListener;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryParser;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.DatabaseCommand;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class MongoDbQueryParseTreeWalkerTest
{

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    @Test
    public void testEmptyAggregate() throws JsonProcessingException
    {
        String input = resourceAsString("input_empty_pipeline.json");
        String expectedOutput = resourceAsString("output_empty_pipeline.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithEmptyMatch() throws Exception
    {
        String input = resourceAsString("input_empty_match.json");
        String expectedOutput = resourceAsString("output_empty_match.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMatchSimpleExpression() throws Exception
    {
        String input = resourceAsString("input_match_simple_expression.json");
        String expectedOutput = resourceAsString("output_match_simple_expression.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMatchExpressionWithOperator() throws Exception
    {
        String input = resourceAsString("input_match_with_operator.json");
        String expectedOutput = resourceAsString("output_match_with_operator.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithoutAndOperator() throws Exception
    {
        String input = resourceAsString("input_two_match_with_operators.json");
        String expectedOutput = resourceAsString("output_two_match_with_operators.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithOrOperator() throws Exception
    {
        String input = resourceAsString("input_two_or_match_with_and_without_operator.json");
        String expectedOutput = resourceAsString("output_two_or_match_with_and_without_operator.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithAndOperator() throws Exception
    {
        String input = resourceAsString("input_two_and_match_with_and_without_operator.json");
        String expectedOutput = resourceAsString("output_two_and_match_with_and_without_operator.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateMatchWithEmptyAndOperator() throws Exception
    {
        String input = resourceAsString("input_match_empty_and.json");
        String expectedOutput = resourceAsString("output_match_empty_and.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithNumbersAndWithWithoutOperator() throws Exception
    {
        String input = resourceAsString("input_multi_match_with_gt_with_without_operator.json");
        String expectedOutput = resourceAsString("output_multi_match_with_gt_with_without_operator.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithEmptyArrayWithoutOperator() throws Exception
    {
        // TODO: expressions for the empty array should be an empty array
        String input = resourceAsString("input_match_with_empty_array.json");
        String expectedOutput = resourceAsString("output_match_with_empty_array.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithNonEmptyArraysWithAndWithoutOperators() throws Exception
    {
        //String input = "{ aggregate: 'firms', pipeline: [ { $match: { test : { $eq: ['ABC', 'DEF'] }, test2: [5, 6], test3: ['one', 'two'] } } ] }";
        String input = resourceAsString("input_multi_match_non_empty_arrays_with_and_without_operators.json");
        String expectedOutput = resourceAsString("output_multi_match_non_empty_arrays_with_and_without_operators.json");

        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);
        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        DatabaseCommand databaseCommand = walker.getCommand();

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    protected String resourceAsString(String path)
    {
        byte[] bytes;
        try
        {
            bytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(path), "Failed to get resource " + path).toURI()));
        }
        catch (IOException | URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        String string = new String(bytes, StandardCharsets.UTF_8);
        return string;
    }
}