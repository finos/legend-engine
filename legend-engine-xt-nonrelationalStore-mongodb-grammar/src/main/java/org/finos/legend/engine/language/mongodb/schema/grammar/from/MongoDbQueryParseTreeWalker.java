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
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.ObjectExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.AggregationPipeline;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.AndExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.ArgumentExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.ArrayArgumentExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.ExpressionObject;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.FieldPathExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.LiteralValue;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.MatchStage;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.OperatorExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.Operators;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.OrExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.Stage;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.bson.BaseType;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.bson.IntType;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.bson.StringType;

import java.util.ArrayList;
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
        MatchStage matchStage = new MatchStage();
        if (ctx.queryExpression() != null)
        {
            matchStage.expression = visitQueryExpression(ctx.queryExpression());
        }
        else if (ctx.logicalOperatorExpression() != null)
        {
            matchStage.expression = visitLogicalOperatorExpression(ctx.logicalOperatorExpression());
        }
        return matchStage;
    }

    private ArgumentExpression visitExpression(MongoDbQueryParser.ExpressionContext ctx)
    {
        ArgumentExpression expression = visitExpressionValue(ctx.expressionValue());
        return new ExpressionObject(new FieldPathExpression(ctx.STRING().getText()), expression);
    }

    private ArgumentExpression visitQueryExpression(MongoDbQueryParser.QueryExpressionContext ctx)
    {
        List<ArgumentExpression> expressions = new ArrayList<>();
        if (ctx.expression().size() > 0)
        {
            expressions = ctx.expression().stream().map(this::visitExpression).collect(Collectors.toList());
        }
        return new ObjectExpression(expressions);
    }

    public ArgumentExpression visitExpressionValue(MongoDbQueryParser.ExpressionValueContext ctx)
    {
        if (ctx.operatorExpression() != null)
        {
            return visitOperatorExpression(ctx.operatorExpression());
        }
        else if (ctx.value() != null)
        {
            return visitValue(ctx.value());
        }
        throw new RuntimeException("visitExpressionValue error");

    }

    public ArgumentExpression visitValue(MongoDbQueryParser.ValueContext ctx)
    {
        if (ctx.STRING() != null || ctx.NUMBER() != null)
        {
            return new LiteralValue(visitLiteral(ctx));
        }
        else if (ctx.obj() != null)
        {
            return visitObj(ctx.obj());
        }
        else if (ctx.arr() != null)
        {
            return visitArray(ctx.arr());
        }

        throw new RuntimeException("visitExpressionValue error");

    }

    public ArgumentExpression visitArray(MongoDbQueryParser.ArrContext ctx)
    {
        List<ArgumentExpression> expressions = ctx.value().stream().map(x -> visitValue(x)).collect(Collectors.toList());
        return new ArrayArgumentExpression(expressions);
    }

    public ArgumentExpression visitOperatorExpression(MongoDbQueryParser.OperatorExpressionContext ctx)
    {
        String operator = ctx.COMPARISON_QUERY_OPERATOR().getText();
        ArgumentExpression expression = visitOperatorExpressionValue(ctx.operatorExpressionValue());
        return new OperatorExpression(Operators.valueOf(operator.substring(1, operator.length() - 1)), expression);
    }

    private ArgumentExpression visitOperatorExpressionValue(MongoDbQueryParser.OperatorExpressionValueContext ctx)
    {

        if (ctx.operatorExpression() != null)
        {
            return visitOperatorExpression(ctx.operatorExpression());
        }
        else if (ctx.expressionValue() != null)
        {
            return visitExpressionValue(ctx.expressionValue());
        }

        throw new RuntimeException("visitOperatorExpressionValue exception");

    }

    private ArgumentExpression visitLogicalOperatorExpression(MongoDbQueryParser.LogicalOperatorExpressionContext ctx)
    {
        if (ctx.orAggregationExpression() != null)
        {
            List<ArgumentExpression> argumentExpressions = ctx.orAggregationExpression().queryExpression().stream().map(x -> visitQueryExpression(x)).collect(Collectors.toList());
            return new OrExpression(argumentExpressions);
        }
        else
        {
            List<ArgumentExpression> argumentExpressions = ctx.andAggregationExpression().queryExpression().stream().map(x -> visitQueryExpression(x)).collect(Collectors.toList());
            return new AndExpression(argumentExpressions);
        }
    }

    private ArgumentExpression visitObj(MongoDbQueryParser.ObjContext ctx)
    {

        ObjectExpression result = new ObjectExpression();
        if (ctx.pair() != null)
        {
            result.arguments = ctx.pair().stream().map(this::visitPair).collect(Collectors.toList());
        }

       return result;
    }

    private ArgumentExpression visitPair(MongoDbQueryParser.PairContext ctx)
    {
        ArgumentExpression expression = visitValue(ctx.value());
        return new ExpressionObject(new FieldPathExpression(ctx.STRING().getText()), expression);
    }

    private BaseType visitLiteral(MongoDbQueryParser.ValueContext ctx)
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
}
