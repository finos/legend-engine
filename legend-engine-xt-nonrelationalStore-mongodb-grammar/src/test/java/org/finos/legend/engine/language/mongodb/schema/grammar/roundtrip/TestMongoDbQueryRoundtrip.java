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

package org.finos.legend.engine.language.mongodb.schema.grammar.roundtrip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryBaseListener;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryLexer;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryListener;
import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryParser;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.AntlrThrowingErrorListener;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.MongoDbQueryParseTreeWalker;
import org.finos.legend.engine.language.mongodb.schema.grammar.to.MongoDbQueryComposer;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.junit.Ignore;
import org.junit.Test;
import utils.CustomJSONPrettyPrinter;

import static utils.TestUtils.resourceAsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestMongoDbQueryRoundtrip
{
    private final ObjectMapper mapper = new ObjectMapper().setDefaultPrettyPrinter(new CustomJSONPrettyPrinter())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);


    @Test
    public void testEmptyAggregate()
    {
        testRoundtrip("empty_pipeline_input.json",
                "empty_pipeline_output.json");
    }

    @Test
    public void testAggregateWithEmptyMatch()
    {
        testRoundtrip("empty_match_input.json",
                "empty_match_output.json");
    }

    @Test
    public void testAggregateWithMatchSimpleExpression()
    {
        testRoundtrip("match_simple_expression_input.json",
                "match_simple_expression_output.json");
    }

    @Test
    public void testAggregateWithMatchExpressionWithOperator()
    {
        testRoundtrip("match_with_operator_input.json",
                "match_with_operator_output.json");
    }


    // TODO: This is a valid query, rework antlr g4 to parse correctly
    @Ignore
    public void testAggregateWithMatchWithVariousComparisonOperatorFormats()
    {
        testRoundtrip("match_with_various_comparison_expression_formats_input.json",
                "match_with_various_comparison_expression_formats_output.json");
    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithoutAndOperator()
    {
        testRoundtrip("two_match_with_operators_input.json",
                "two_match_with_operators_output.json");
    }

    @Test
    public void testAggregateWithMatchExpressionWithOrOperator() throws Exception
    {
        testRoundtrip("match_with_or_operator_input.json",
                "match_with_or_operator_output.json");
    }

    @Test
    public void testAggregateWithMatchExpressionWithAndOperator()
    {
        testRoundtrip("match_with_and_operator_input.json",
                "match_with_and_operator_output.json");
    }


    // TODO: Fix match stage expressions for this query, we should have a matchExpression consisting of [*] queryExpression or logicalOperatorExpressions
    @Ignore
    public void testAggregateWithMatchExpressionWithLogicalExpressionAndRegularMatchExpressions()
    {
        testRoundtrip("multi_match_with_gt_with_without_operator_input.json",
                "multi_match_with_gt_with_without_operator_output.json");
    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithEmptyArrayWithoutOperator()
    {
        testRoundtrip("match_with_empty_array_input.json",
                "match_with_empty_array_output.json");
    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithNonEmptyArraysWithAndWithoutOperators()
    {
        testRoundtrip("multi_match_non_empty_array_input.json",
                "multi_match_non_empty_array_output.json");
    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithManyValueTypes() throws Exception
    {
        testRoundtrip("match_many_types_input.json",
                "match_many_types_output.json");
    }

    @Test
    public void testAggregateMatchWithNestedObjectStructure() throws Exception
    {
        testRoundtrip("match_with_nested_object_input.json",
                "match_with_nested_object_output.json");
    }

    @Test
    public void testProjectWithSingleInclusionFilter() throws Exception
    {
        testRoundtrip("project_with_single_inclusion_filter_input.json",
                "project_with_single_inclusion_filter_output.json");
    }

    @Test
    public void testProjectWithSingleComputedField() throws Exception
    {
        testRoundtrip("project_with_single_computed_field_input.json",
                "project_with_single_computed_field_output.json");
    }

    @Test
    public void testProjectWithMultipleFilters() throws Exception
    {
        testRoundtrip("project_with_multiple_filters_input.json",
                "project_with_multiple_filters_output.json");
    }

    @Test
    public void testProjectWithMultipleComplexFilters() throws Exception
    {
        testRoundtrip("project_with_multiple_complex_filters_input.json",
                "project_with_multiple_complex_filters_output.json");
    }

    @Test
    public void testProjectShouldThrowException() throws Exception
    {
        String input = resourceAsString("project_with_wrong_number_should_throw_input.json");
        Exception exception = assertThrows(RuntimeException.class, () -> parseAndWalkDatabaseCommand(input));
        assertEquals(exception.getMessage(), "visitProjectFilterValue error");
    }

    private void test(String inputMongoQuery, String expectedParsedMongoQuery)
    {

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(inputMongoQuery);

        MongoDbQueryComposer composer = new MongoDbQueryComposer();
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

        String inputMongoQuery = resourceAsString(inputMongoQueryFileLoc);
        String expectedParsedMongoQuery = resourceAsString(expectedParsedMongoQueryFileLoc);

        // Test if input mongo query string can be walked into a java model representation and if we can compose this model back
        // into the exact initial input mongo query string
        test(inputMongoQuery, expectedParsedMongoQuery);

    }

    private MongoDbQueryParser getParser(CommonTokenStream tokens)
    {
        MongoDbQueryParser parser = new MongoDbQueryParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(AntlrThrowingErrorListener.INSTANCE);
        return parser;
    }


    private DatabaseCommand parseAndWalkDatabaseCommand(String input)
    {
        MongoDbQueryLexer programLexer = new MongoDbQueryLexer(CharStreams.fromString(input));

        CommonTokenStream tokens = new CommonTokenStream(programLexer);
        MongoDbQueryParser parser = getParser(tokens);

        MongoDbQueryListener listener = new MongoDbQueryBaseListener();
        parser.addParseListener(listener);

        MongoDbQueryParser.DatabaseCommandContext commandContext = parser.databaseCommand();

        MongoDbQueryParseTreeWalker walker = new MongoDbQueryParseTreeWalker();
        walker.visit(commandContext);

        return walker.getCommand();
    }


}