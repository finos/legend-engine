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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDBQueryBaseListener;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDBQueryLexer;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDBQueryListener;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDBQueryParser;
import org.finos.legend.engine.language.mongodb.schema.grammar.to.MongoDBQueryComposer;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.junit.Ignore;
import org.junit.Test;
import utils.CustomJSONPrettyPrinter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestQueryParseTreeWalker
{
    private final ObjectMapper mapper = new ObjectMapper().setDefaultPrettyPrinter(new CustomJSONPrettyPrinter())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            //.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);


    @Test
    public void testEmptyAggregate()
    {
        testRoundtrip("json/query/empty_pipeline_input.json",
                "json/query/empty_pipeline_output.json");
    }

    @Test
    public void testAggregateWithEmptyMatch()
    {
        testRoundtrip("json/query/empty_match_input.json",
                "json/query/empty_match_output.json");
    }

    @Test
    public void testAggregateWithMatchSimpleExpression()
    {
        testRoundtrip("json/query/match_simple_expression_input.json",
                "json/query/match_simple_expression_output.json");
    }

    @Test
    public void testAggregateWithMatchExpressionWithOperator()
    {
        testRoundtrip("json/query/match_with_operator_input.json",
                "json/query/match_with_operator_output.json");
    }


    // TODO: This is a valid query, rework antlr g4 to parse correctly
    @Ignore
    public void testAggregateWithMatchWithVariousComparisonOperatorFormats()
    {
        testRoundtrip("json/query/invalid_not_operator_expression_formats_input.json",
                "json/query/match_with_various_comparison_expression_formats_output.json");
    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithoutAndOperator()
    {
        testRoundtrip("json/query/two_match_with_operators_input.json",
                "json/query/two_match_with_operators_output.json");
    }

    @Test
    public void testAggregateWithMatchExpressionWithOrOperator()
    {
        testRoundtrip("json/query/match_with_or_operator_input.json",
                "json/query/match_with_or_operator_output.json");
    }

    @Test
    public void testAggregateWithMatchExpressionWithAndOperator()
    {
        testRoundtrip("json/query/match_with_and_operator_input.json",
                "json/query/match_with_and_operator_output.json");
    }


    // TODO: Fix match stage expressions for this query, we should have a matchExpression consisting of [*] queryExpression or logicalOperatorExpressions
    @Ignore
    public void testAggregateWithMatchExpressionWithLogicalExpressionAndRegularMatchExpressions()
    {
        testRoundtrip("json/query/multi_match_with_gt_with_without_operator_input.json",
                "json/query/multi_match_with_gt_with_without_operator_output.json");
    }

    @Test
    public void testInconsistFieldBasedMatchOperator() throws Exception
    {
        // String invalidQueryFile = "json/query/valid_match_with_eq_operator_input_as_object.json";

        String input = "";
        try
        {
            URL url1 = Objects.requireNonNull(getClass().getClassLoader().getResource("json/query/invalid_match_with_eq_operator_input.json"));
            input = new String(Files.readAllBytes(Paths.get(url1.toURI())), StandardCharsets.UTF_8);
        }
        catch (IOException | URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        final String inputJson = input;
        Exception exception = assertThrows(RuntimeException.class, () -> parseAndWalkDatabaseCommand(inputJson));
        assertEquals(exception.getMessage(), "line 6:47 missing '}' at ','");


//        URL url1 = Objects.requireNonNull(getClass().getClassLoader().getResource(invalidQueryFile));
//        String invalidQuery = new String(Files.readAllBytes(Paths.get(url1.toURI())), StandardCharsets.UTF_8);
//
//        DatabaseCommand invalidDBCommand = parseAndWalkDatabaseCommand(invalidQuery);
    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithEmptyArrayWithoutOperator()
    {
        testRoundtrip("json/query/match_with_empty_array_input.json",
                "json/query/match_with_empty_array_output.json");
    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithNonEmptyArraysWithAndWithoutOperators()
    {
        testRoundtrip("json/query/multi_match_non_empty_array_input.json",
                "json/query/multi_match_non_empty_array_output.json");
    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithManyValueTypes()
    {
        testRoundtrip("json/query/match_many_types_input.json",
                "json/query/match_many_types_output.json");
    }

    @Test
    public void testAggregateMatchWithNestedObjectStructure()
    {
        testRoundtrip("json/query/match_with_nested_object_input.json",
                "json/query/match_with_nested_object_output.json");
    }

    @Test
    public void testProjectWithSingleInclusionFilter()
    {
        testRoundtrip("json/query/project_with_single_inclusion_filter_input.json",
                "json/query/project_with_single_inclusion_filter_output.json");
    }

    @Test
    public void testProjectWithSingleComputedField()
    {
        testRoundtrip("json/query/project_with_single_computed_field_input.json",
                "json/query/project_with_single_computed_field_output.json");
    }

    @Test
    public void testProjectWithMultipleFilters()
    {
        testRoundtrip("json/query/project_with_multiple_filters_input.json",
                "json/query/project_with_multiple_filters_output.json");
    }

    @Test
    public void testProjectWithMultipleComplexFilters()
    {
        testRoundtrip("json/query/project_with_multiple_complex_filters_input.json",
                "json/query/project_with_multiple_complex_filters_output.json");
    }

    @Test
    @Ignore
    public void testProjectShouldThrowException()
    {
        String input = "";
        try
        {
            URL url1 = Objects.requireNonNull(getClass().getClassLoader().getResource("json/query/project_with_incl_and_excl_should_throw_input.json"));
            input = new String(Files.readAllBytes(Paths.get(url1.toURI())), StandardCharsets.UTF_8);
        }
        catch (IOException | URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        final String inputJson = input;
        Exception exception = assertThrows(RuntimeException.class, () -> parseAndWalkDatabaseCommand(inputJson));
        assertEquals(exception.getMessage(), "visitProjectFilterValue error");
    }

    private void test(String inputMongoQuery, String expectedParsedMongoQuery)
    {

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(inputMongoQuery);

        MongoDBQueryComposer composer = new MongoDBQueryComposer();
        String queryString = composer.parseDatabaseCommand(databaseCommand);

        try
        {
            assertEquals("Parsed MQL string (input) into java metamodel and" +
                            " composing back to MQL string is different to original MQL string input.",
                    mapper.readTree(inputMongoQuery).toPrettyString(), mapper.readTree(queryString).toPrettyString());
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
        try
        {
            assertEquals("Parsed MQL string (input) into java metamodel is different from expected.",
                    expectedParsedMongoQuery, mapper.writeValueAsString(databaseCommand));
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void testRoundtrip(String inputMongoQueryFileLoc, String expectedParsedMongoQueryFileLoc)
    {
        String inputMongoQuery = null;
        String expectedParsedMongoQuery = null;
        try
        {
            URL url1 = Objects.requireNonNull(getClass().getClassLoader().getResource(inputMongoQueryFileLoc));
            inputMongoQuery = new String(Files.readAllBytes(Paths.get(url1.toURI())), StandardCharsets.UTF_8);

            URL url2 = Objects.requireNonNull(getClass().getClassLoader().getResource(expectedParsedMongoQueryFileLoc));
            expectedParsedMongoQuery = new String(Files.readAllBytes(Paths.get(url2.toURI())), StandardCharsets.UTF_8);
        }
        catch (IOException | URISyntaxException e)
        {
            throw new RuntimeException(e);
        }

        // Test if input mongo query string can be walked into a java model representation and if we can compose this model back
        // into the exact initial input mongo query string
        test(inputMongoQuery, expectedParsedMongoQuery);

    }

    private MongoDBQueryParser getParser(CommonTokenStream tokens)
    {
        MongoDBQueryParser parser = new MongoDBQueryParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(AntlrThrowingErrorListener.INSTANCE);
        return parser;
    }


    private DatabaseCommand parseAndWalkDatabaseCommand(String input)
    {
        MongoDBQueryLexer programLexer = new MongoDBQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDBQueryParser parser = getParser(tokens);

        MongoDBQueryListener listener = new MongoDBQueryBaseListener();
        parser.addParseListener(listener);

        MongoDBQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDBQueryParseTreeWalker walker = MongoDBQueryParseTreeWalker.newInstance();
        walker.visit(commandContext);

        return walker.getCommand();
    }


}