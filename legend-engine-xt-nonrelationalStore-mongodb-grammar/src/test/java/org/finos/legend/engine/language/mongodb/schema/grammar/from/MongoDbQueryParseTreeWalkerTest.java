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
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArrayArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ExpressionObject;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.FieldPathExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LiteralValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.MatchStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Operator;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OrExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StringTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ViewPipeline;
import org.junit.Test;
import utils.CustomJSONPrettyPrinter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class MongoDbQueryParseTreeWalkerTest
{
    private final ObjectMapper mapper = new ObjectMapper().setDefaultPrettyPrinter(new CustomJSONPrettyPrinter())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    @Test
    public void testEmptyAggregate() throws JsonProcessingException
    {
        String input = resourceAsString("input_empty_pipeline.json");
        String expectedOutput = resourceAsString("output_empty_pipeline.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithEmptyMatch() throws Exception
    {
        String input = resourceAsString("input_empty_match.json");
        String expectedOutput = resourceAsString("output_empty_match.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMatchSimpleExpression() throws Exception
    {
        String input = resourceAsString("input_match_simple_expression.json");
        String expectedOutput = resourceAsString("output_match_simple_expression.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMatchExpressionWithOperator() throws Exception
    {
        String input = resourceAsString("input_match_with_operator.json");
        String expectedOutput = resourceAsString("output_match_with_operator.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithoutAndOperator() throws Exception
    {
        String input = resourceAsString("input_two_match_with_operators.json");
        String expectedOutput = resourceAsString("output_two_match_with_operators.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithOrOperator() throws Exception
    {
        String input = resourceAsString("input_two_or_match_with_and_without_operator.json");
        String expectedOutput = resourceAsString("output_two_or_match_with_and_without_operator.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithAndOperator() throws Exception
    {
        String input = resourceAsString("input_two_and_match_with_and_without_operator.json");
        String expectedOutput = resourceAsString("output_two_and_match_with_and_without_operator.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateMatchWithEmptyAndOperator() throws Exception
    {
        String input = resourceAsString("input_match_empty_and.json");
        String expectedOutput = resourceAsString("output_match_empty_and.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithGtWithAndWithoutOperator() throws Exception
    {
        String input = resourceAsString("input_multi_match_with_gt_with_without_operator.json");
        String expectedOutput = resourceAsString("output_multi_match_with_gt_with_without_operator.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithEmptyArrayWithoutOperator() throws JsonProcessingException
    {
        String input = resourceAsString("input_match_with_empty_array.json");
        String expectedOutput = resourceAsString("output_match_with_empty_array.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithMultiMatchExpressionWithNonEmptyArraysWithAndWithoutOperators() throws Exception
    {
        String input = resourceAsString("input_multi_match_non_empty_arrays_with_and_without_operators.json");
        String expectedOutput = resourceAsString("output_multi_match_non_empty_arrays_with_and_without_operators.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testAggregateWithComplicatedNestedStructure() throws Exception
    {
        String input = resourceAsString("input_match_with_nested_object.json");
        String expectedOutput = resourceAsString("output_match_with_nested_object.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));
    }

    @Test
    public void testProjectWithSingleFilter() throws Exception
    {
        String input = resourceAsString("input_project_with_single_filter.json");
        String expectedOutput = resourceAsString("output_project_with_single_filter.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testProjectWithMultipleFilters() throws Exception
    {
        String input = resourceAsString("input_project_with_multiple_filters.json");
        String expectedOutput = resourceAsString("output_project_with_multiple_filters.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }


    @Test
    public void testProjectWithMultipleComplexFilters() throws Exception
    {
        String input = resourceAsString("input_project_with_multiple_complex_filters.json");
        String expectedOutput = resourceAsString("output_project_with_multiple_complex_filters.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void testProjectWithSingleComputedField() throws Exception
    {
        String input = resourceAsString("input_project_with_single_computed_field.json");
        String expectedOutput = resourceAsString("output_project_with_single_computed_field.json");

        DatabaseCommand databaseCommand = parseAndWalkDatabaseCommand(input);

        assertEquals(expectedOutput, mapper.writeValueAsString(databaseCommand));

    }

    @Test
    public void goBackwards() throws Exception
    {
        StringTypeValue stringTypeValue1 = new StringTypeValue();
        stringTypeValue1.value = "Peter";

        LiteralValue literalValue1 = new LiteralValue();
        literalValue1.value = stringTypeValue1;

        OperatorExpression operatorExpression = new OperatorExpression();
        operatorExpression.operator = Operator.EQ;
        operatorExpression.expression = literalValue1;

        StringTypeValue stringTypeValue2 = new StringTypeValue();
        stringTypeValue2.value = "Smith";

        LiteralValue literalValue2 = new LiteralValue();
        literalValue2.value = stringTypeValue2;

        FieldPathExpression fieldPathExpression1 = new FieldPathExpression();
        fieldPathExpression1.path = "fName";

        FieldPathExpression fieldPathExpression2 = new FieldPathExpression();
        fieldPathExpression2.path = "lName";

        ExpressionObject expressionObject1 = new ExpressionObject();
        expressionObject1.field = fieldPathExpression1;
        expressionObject1.argument = operatorExpression;

        ExpressionObject expressionObject2 = new ExpressionObject();
        expressionObject2.field = fieldPathExpression2;
        expressionObject2.argument = literalValue2;

        ArrayArgumentExpression argumentExpression = new ArrayArgumentExpression();
        argumentExpression.items = Arrays.asList(expressionObject1, expressionObject2);

        OrExpression orExpression = new OrExpression();
        orExpression.operator = Operator.OR;
        orExpression.expressions = Arrays.asList(argumentExpression);

        MatchStage matchStage = new MatchStage();
        matchStage.expression = orExpression;

        ViewPipeline viewPipeline = new ViewPipeline();
        viewPipeline.stages = Arrays.asList(matchStage);

        DatabaseCommand databaseCommand = new DatabaseCommand();
        databaseCommand.aggregationPipeline = viewPipeline;

        MongoDbQueryComposer mongoDbQueryComposer = new MongoDbQueryComposer();
        mongoDbQueryComposer.parser(databaseCommand);
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

    private String resourceAsString(String path)
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
        return new String(bytes, StandardCharsets.UTF_8);
    }
}