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
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.NullType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AggregateExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BaseTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.IntTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StringTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ViewPipeline;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AndExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArrayArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ExpressionObject;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.FieldPathExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LiteralValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.MatchStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Operator;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OrExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Stage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BaseType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.IntType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.StringType;
import java.util.ArrayList;
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

    private ViewPipeline visitPipeline(MongoDbQueryParser.PipelinesContext ctx)
    {
        ViewPipeline viewPipeline = new ViewPipeline();
        if (ctx.aggregationPipelineStage() != null)
        {
            viewPipeline.stages = ctx.aggregationPipelineStage().stream().map(this::visitAggregationPipelineStage).collect(Collectors.toList());
        }
        else
        {
            viewPipeline.stages = Arrays.asList();
        }
        return viewPipeline;
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
        FieldPathExpression field = new FieldPathExpression();
        field.path = ctx.STRING().getText();
        ExpressionObject expressionObject = new ExpressionObject();
        expressionObject.field = field;
        expressionObject.argument = visitExpressionValue(ctx.expressionValue());
        return expressionObject;
    }

    private ArgumentExpression visitQueryExpression(MongoDbQueryParser.QueryExpressionContext ctx)
    {
        List<ArgumentExpression> expressions = new ArrayList<>();
        if (ctx.expression().size() > 0)
        {
            expressions = ctx.expression().stream().map(this::visitExpression).collect(Collectors.toList());
        }
        ArrayArgumentExpression expression = new ArrayArgumentExpression();
        expression.expressions = expressions;
        return expression;
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
            LiteralValue literalValue = new LiteralValue();
            literalValue.value = visitLiteral(ctx);
            return literalValue;
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
        ArrayArgumentExpression expression = new ArrayArgumentExpression();
        expression.expressions = ctx.value().stream().map(x -> visitValue(x)).collect(Collectors.toList());;
        return expression;
    }

    public ArgumentExpression visitOperatorExpression(MongoDbQueryParser.OperatorExpressionContext ctx)
    {
        String operator = ctx.COMPARISON_QUERY_OPERATOR().getText();
        OperatorExpression operatorExpression = new OperatorExpression();
        operatorExpression.expression = visitOperatorExpressionValue(ctx.operatorExpressionValue());;
        operatorExpression.operator = Operator.valueOf(operator.toUpperCase().substring(2, operator.length() - 1));
        return operatorExpression;
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
            OrExpression orExpression = new OrExpression();
            orExpression.expressions = ctx.orAggregationExpression().queryExpression().stream().map(x -> visitQueryExpression(x)).collect(Collectors.toList());
            orExpression.operator = Operator.OR;
            return orExpression;
        }
        else
        {
            AndExpression andExpression = new AndExpression();
            andExpression.expressions = ctx.andAggregationExpression().queryExpression().stream().map(x -> visitQueryExpression(x)).collect(Collectors.toList());
            andExpression.operator = Operator.AND;
            return andExpression;
        }
    }

    private ArgumentExpression visitObj(MongoDbQueryParser.ObjContext ctx)
    {

        ArrayArgumentExpression result = new ArrayArgumentExpression();
        if (ctx.pair() != null)
        {
            result.expressions = ctx.pair().stream().map(this::visitPair).collect(Collectors.toList());
        }

       return result;
    }

    private ArgumentExpression visitPair(MongoDbQueryParser.PairContext ctx)
    {
        FieldPathExpression field = new FieldPathExpression();
        field.path = ctx.STRING().getText();
        ExpressionObject expressionObject = new ExpressionObject();
        expressionObject.field = field;
        expressionObject.argument = visitValue(ctx.value());;
        return expressionObject;
    }

    private BaseTypeValue visitLiteral(MongoDbQueryParser.ValueContext ctx)
    {
        if (ctx.NUMBER() != null)
        {
            IntTypeValue intType = new IntTypeValue();
            intType.value = Integer.parseInt(ctx.NUMBER().getText());
            return intType;
        }
        else if (ctx.STRING() != null)
        {
            StringTypeValue stringType = new StringTypeValue();
            stringType.value = ctx.STRING().getText();
            return stringType;
        }

        StringTypeValue stringType = new StringTypeValue();
        return stringType;
    }
}
