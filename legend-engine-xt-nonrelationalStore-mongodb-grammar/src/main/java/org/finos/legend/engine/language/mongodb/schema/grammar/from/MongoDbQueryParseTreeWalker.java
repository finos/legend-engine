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

import org.finos.legend.engine.language.mongodb.query.grammar.from.antlr4.MongoDbQueryParser;

import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.DatabaseCommand;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.AggregateExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.AggregationPipeline;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.AndExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.ArgumentExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.ExpressionObject;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.FieldPathExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.LiteralExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.MatchStage;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.OperatorExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.Operators;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.OrExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.Stage;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.bson.BaseType;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.bson.IntType;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.bson.StringType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MongoDbQueryParseTreeWalker
{
    public DatabaseCommand databaseCommand;

    public DatabaseCommand getCommand()
    {
        return this.databaseCommand;
    }


    public void visit(MongoDbQueryParser.DatabaseCommandContext ctx)
    {
        this.databaseCommand = new DatabaseCommand();
        this.databaseCommand.type = "aggregate";
        this.databaseCommand.collectionName = visitAggregate(ctx.command());
        this.databaseCommand.aggregationPipeline = visitPipeline(ctx.command().pipelines());
    }

    private String visitAggregate(MongoDbQueryParser.CommandContext ctx)
    {
        return ctx.STRING().getText();
    }

    private AggregationPipeline visitPipeline(MongoDbQueryParser.PipelinesContext ctx)
    {
        if (ctx.aggregationPipelineStage() == null)
        {
            return new AggregationPipeline();
        }
        List<Stage> matchStageList = ctx.aggregationPipelineStage().stream().map(this::visitAggregationPipelineStage).collect(Collectors.toList());
        return new AggregationPipeline(matchStageList);
    }

    private Stage visitAggregationPipelineStage(MongoDbQueryParser.AggregationPipelineStageContext ctx)
    {
        Stage stage;
        if (ctx.matchStage() != null)
        {
            stage = visitMatchStage(ctx.matchStage());
            return stage;
        }
        else
        {
            throw new RuntimeException("No stage was found");
        }

    }

    private MatchStage visitMatchStage(MongoDbQueryParser.MatchStageContext ctx)
    {
        MatchStage matchStage = new MatchStage(Arrays.asList(this.visitExpression(ctx)));
        return matchStage;
    }

    private AggregateExpression visitExpression(MongoDbQueryParser.MatchStageContext ctx)
    {
        AggregateExpression aggregateExpression;
        if (ctx.queryExpression() != null)
        {
            aggregateExpression = new AggregateExpression(this.visitQueryExpression(ctx.queryExpression()));
            return aggregateExpression;
        }
        else if (ctx.logicalOperatorExpression() != null)
        {
            aggregateExpression = new AggregateExpression(Arrays.asList(this.visitLogicalOperatorExpression(ctx.logicalOperatorExpression())));
            return aggregateExpression;
        }
        else
        {
            return new AggregateExpression();
        }
    }

    private List<ArgumentExpression> visitQueryExpression(MongoDbQueryParser.QueryExpressionContext ctx)
    {
        return ctx.expression().stream().map(x ->
        {
            if (x.expressionValue().complexExpressionValue() != null)
            {
                return visitExpressionValue(x.expressionValue(), x.STRING().getText(), null);
            }
            return buildExpression(x.STRING().getText(), visitLiteral(x.expressionValue()), null);
        }).collect(Collectors.toList());
    }

//    public ArgumentExpression visitExpression(MongoDbQueryParser.ExpressionContext ctx)
//    {
//        if (ctx.complexExpressionValue() != null)
//        {
//            return visitComplexExpressionValue(ctx.complexExpressionValue(), null);
//        }
//        else
//        {
//            return visitExpressionValue(ctx.expressionValue(), ctx.WORD().getText(), null);
//        }
//    }

    public ArgumentExpression visitExpressionValue(MongoDbQueryParser.ExpressionValueContext ctx, String field, String operator)
    {
        if (ctx.complexExpressionValue() != null)
        {
            return visitComplexExpressionValue(ctx.complexExpressionValue(), field);
        }
        else if (ctx.STRING() != null)
        {
            return buildExpression(field, visitLiteral(ctx), operator);
        }
        else if (ctx.NUMBER() != null)
        {
            return buildExpression(field, visitLiteral(ctx), operator);
        }
        throw new RuntimeException("visitExpressionValue error");

    }

    public ArgumentExpression visitComplexExpressionValue(MongoDbQueryParser.ComplexExpressionValueContext ctx, String field)
    {
        if (ctx.complexObjectExpressionValue() != null)
        {
            return visitComplexObjectExpressionValue(ctx.complexObjectExpressionValue(), field);
        }
        else
        {
            return visitComplexObjectExpressionValue(ctx.complexObjectExpressionValue(), field);
        }
    }

    private ArgumentExpression visitComplexObjectExpressionValue(MongoDbQueryParser.ComplexObjectExpressionValueContext ctx, String field)
    {
        String operator = ctx.COMPARISON_QUERY_OPERATOR().getText();
        return visitExpressionValue(ctx.expressionValue(), field, operator);
    }

    private List<ArgumentExpression> visitComplexArrayExpressionValue(MongoDbQueryParser.ComplexArrayExpressionValueContext ctx)
    {
        return ctx.complexExpressionValue().stream().map(x -> visitComplexExpressionValue(x, null)).collect(Collectors.toList());
    }

    private ArgumentExpression visitLogicalOperatorExpression(MongoDbQueryParser.LogicalOperatorExpressionContext ctx)
    {
        if (ctx.orAggregationExpression() != null)
        {
            List<ArgumentExpression> argumentExpressions = ctx.orAggregationExpression().queryExpression().stream().flatMap(x -> visitQueryExpression(x).stream()).collect(Collectors.toList());
            return new OrExpression(argumentExpressions);
        }
        else
        {
            List<ArgumentExpression> argumentExpressions = ctx.andAggregationExpression().queryExpression().stream().flatMap(x -> visitQueryExpression(x).stream()).collect(Collectors.toList());
            return new AndExpression(argumentExpressions);
        }
    }

    private BaseType visitLiteral(MongoDbQueryParser.ExpressionValueContext ctx)
    {
        if (ctx.NUMBER() != null)
        {
            return new IntType(Integer.parseInt(ctx.NUMBER().getText()));
        }
        else if (ctx.STRING() != null)
        {
            return new StringType(ctx.STRING().getText());
        }
        return new BaseType();
    }

    private static ArgumentExpression buildExpression(String field, BaseType value, String operator)
    {
        FieldPathExpression fieldPathExpression = new FieldPathExpression(field);
        LiteralExpression literalExpression = new LiteralExpression(value);
        ExpressionObject expressionObject = new ExpressionObject(fieldPathExpression, literalExpression);
        return operator == null
                ? new OperatorExpression(expressionObject)
                : new OperatorExpression(Operators.valueOf(operator), expressionObject);
    }
}
