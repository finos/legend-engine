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
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ViewPipeline;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Stage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.MatchStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ProjectStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Item;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ComputedFieldValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StringTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BoolTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LiteralValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.IntTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArrayTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ComparisonOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.EqOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GreaterThanEqualsOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.InOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NinOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LogicalOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AndOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OrOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NorOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NotOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.KeyValuePair;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BaseTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DecimalTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NullTypeValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;

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
        }
        else if (ctx.projectStage() != null)
        {
            stage = visitProjectStage(ctx.projectStage());
        }
        else
        {
            throw new RuntimeException("No stage was found in visitAggregationPipelineStage");
        }

        return stage;
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

    private ProjectStage visitProjectStage(MongoDbQueryParser.ProjectStageContext ctx)
    {
        ProjectStage stage = new ProjectStage();
        if (ctx.projectFilterExpression() != null)
        {
            stage.filters = visitProjectFilterExpression(ctx.projectFilterExpression());
        }
        return stage;
    }

    private ArgumentExpression visitProjectFilterExpression(MongoDbQueryParser.ProjectFilterExpressionContext ctx)
    {
        List<ObjectExpression> expressions = new ArrayList<>();
        if (ctx.projectFilter().size() > 0)
        {
            expressions = ctx.projectFilter().stream().map(this::visitProjectFilter).collect(Collectors.toList());
        }
        Item expression = new Item();
        expression.objects = expressions;
        return expression;
    }

    private ObjectExpression visitProjectFilter(MongoDbQueryParser.ProjectFilterContext ctx)
    {
        ArgumentExpression argument = visitProjectFilterValue(ctx.projectFilterValue());
        ObjectExpression ObjectExpression = new ObjectExpression();
        ObjectExpression.field = ctx.STRING().getText();
        ObjectExpression.argument = argument;

        return ObjectExpression;
    }


    private ArgumentExpression visitProjectFilterValue(MongoDbQueryParser.ProjectFilterValueContext ctx)
    {

        ArgumentExpression val;
        if (ctx.projectComputedFieldValue() != null)
        {
            ComputedFieldValue computedFieldValue = new ComputedFieldValue();
            computedFieldValue.value = ctx.projectComputedFieldValue().getText();
            val = computedFieldValue;
        }
        else if (ctx.projectFilterExpression() != null)
        {
            val = visitProjectFilterExpression(ctx.projectFilterExpression());
        }
        else if (ctx.BOOLEAN() != null)
        {
            BoolTypeValue boolTypeValue = new BoolTypeValue();
            boolTypeValue.value = parseBoolean(ctx.getText());

            LiteralValue literalValue = new LiteralValue();
            literalValue.value = boolTypeValue;
            val = literalValue;
        }
        else if (ctx.NUMBER() != null && ctx.NUMBER().getText().equals("1") || ctx.NUMBER().getText().equals("0"))
        {
            IntTypeValue intType = new IntTypeValue();
            intType.value = Integer.parseInt(ctx.getText());

            LiteralValue literalValue = new LiteralValue();
            literalValue.value = intType;
            val = literalValue;
        }
        else
        {
            throw new RuntimeException("visitProjectFilterValue error");
        }

        return val;
    }


    private ObjectExpression visitExpression(MongoDbQueryParser.ExpressionContext ctx)
    {
        ObjectExpression ObjectExpression = new ObjectExpression();
        ObjectExpression.field = ctx.STRING().getText();
        ObjectExpression.argument = visitExpressionValue(ctx.expressionValue());
        return ObjectExpression;
    }

    private ArgumentExpression visitQueryExpression(MongoDbQueryParser.QueryExpressionContext ctx)
    {
        List<ObjectExpression> expressions = new ArrayList<>();
        if (ctx.expression().size() > 0)
        {
            expressions = ctx.expression().stream().map(this::visitExpression).collect(Collectors.toList());
        }
        Item expression = new Item();
        expression.objects = expressions;
        return expression;
    }

    private ArgumentExpression visitExpressionValue(MongoDbQueryParser.ExpressionValueContext ctx)
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

    private LiteralValue visitValue(MongoDbQueryParser.ValueContext ctx)
    {

        if (ctx.STRING() != null || ctx.NUMBER() != null || ctx.BOOLEAN() != null || ctx.NULL() != null)
        {
            LiteralValue literalValue = new LiteralValue();
            literalValue.value = visitLiteral(ctx);
            return literalValue;
        }
        else if (ctx.obj() != null)
        {
            LiteralValue literalValue = new LiteralValue();
            literalValue.value = visitObj(ctx.obj());
            return literalValue;
        }
        else if (ctx.arr() != null)
        {
            LiteralValue literalValue = new LiteralValue();
            literalValue.value = visitArray(ctx.arr());
            return literalValue;
        }

        throw new RuntimeException("visitExpressionValue error");

    }

    private ArrayTypeValue visitArray(MongoDbQueryParser.ArrContext ctx)
    {
        ArrayTypeValue array = new ArrayTypeValue();
        array.items = ctx.value().stream().map(e -> visitValue(e).value).collect(Collectors.toList());
        return array;
    }

    private ArgumentExpression visitOperatorExpression(MongoDbQueryParser.OperatorExpressionContext ctx)
    {
        String operator = ctx.COMPARISON_QUERY_OPERATOR().getText().substring(1,
                ctx.COMPARISON_QUERY_OPERATOR().getText().length() - 1);

        ArgumentExpression expression = buildComparisonOperatorExpression(operator, ctx.value());

        return expression;
    }

    private ComparisonOperatorExpression buildComparisonOperatorExpression(String operator, MongoDbQueryParser.ValueContext valueCtx)
    {

        LiteralValue value = visitValue(valueCtx);
        ComparisonOperatorExpression comparisonOpEx = null;

        if (operator.equals(ComparisonOperator.EQ.label))
        {
            comparisonOpEx = new EqOperatorExpression();
            comparisonOpEx.expression = value;
        }
        else if (operator.equals(ComparisonOperator.GT.label))
        {
            comparisonOpEx = new GTOperatorExpression();
            comparisonOpEx.expression = value;
        }
        else if (operator.equals(ComparisonOperator.GTE.label))
        {
            comparisonOpEx = new GreaterThanEqualsOperatorExpression();
            comparisonOpEx.expression = value;
        }
        else if (operator.equals(ComparisonOperator.IN.label))
        {
            comparisonOpEx = new InOperatorExpression();
            comparisonOpEx.expression = value;
        }
        else if (operator.equals(ComparisonOperator.LT.label))
        {
            comparisonOpEx = new LTOperatorExpression();
            comparisonOpEx.expression = value;
        }
        else if (operator.equals(ComparisonOperator.LTE.label))
        {
            comparisonOpEx = new LTEOperatorExpression();
            comparisonOpEx.expression = value;
        }
        else if (operator.equals(ComparisonOperator.NE.label))
        {
            comparisonOpEx = new NEOperatorExpression();
            comparisonOpEx.expression = value;
        }
        else if (operator.equals(ComparisonOperator.NIN.label))
        {
            comparisonOpEx = new NinOperatorExpression();
            comparisonOpEx.expression = value;
        }

        return comparisonOpEx;
    }


    private LogicalOperatorExpression visitLogicalOperatorExpression(MongoDbQueryParser.LogicalOperatorExpressionContext ctx)
    {
        LogicalOperatorExpression logicalOpEx = null;
        List<ArgumentExpression> expressions = ctx.queryExpression().stream()
                    .map(this::visitQueryExpression).collect(Collectors.toList());

        String operator = ctx.LOGICAL_QUERY_OPERATOR().getText().substring(1,
                ctx.LOGICAL_QUERY_OPERATOR().getText().length() - 1);

        if (operator.equals(LogicalOperator.AND.label))
        {
            logicalOpEx = new AndOperatorExpression();
        }
        else if (operator.equals(LogicalOperator.OR.label))
        {
            logicalOpEx = new OrOperatorExpression();
        }
        else if (operator.equals(LogicalOperator.NOR.label))
        {
            logicalOpEx = new NorOperatorExpression();
        }
        else if (operator.equals(LogicalOperator.NOT.label))
        {
            logicalOpEx = new NotOperatorExpression();
        }

        logicalOpEx.expressions = expressions;
        return logicalOpEx;
    }

    private ObjectTypeValue visitObj(MongoDbQueryParser.ObjContext ctx)
    {
        ObjectTypeValue objVal = new ObjectTypeValue();
        if (ctx.pair() != null)
        {
            objVal.keyValues = ctx.pair().stream().map(this::visitPair).collect(Collectors.toList());
        }

        return objVal;
    }

    private KeyValuePair visitPair(MongoDbQueryParser.PairContext ctx)
    {

        KeyValuePair pair = new KeyValuePair();
        pair.key = ctx.STRING().getText();

        LiteralValue literalValue = visitValue(ctx.value());
        pair.value = literalValue.value;

        return pair;
    }

    private BaseTypeValue visitLiteral(MongoDbQueryParser.ValueContext ctx)
    {
        if (ctx.NUMBER() != null)
        {
            if (checkIfNumberIsInteger(ctx.NUMBER().getText()))
            {
                IntTypeValue intTypeValue = new IntTypeValue();
                intTypeValue.value = Integer.parseInt(ctx.NUMBER().getText());
                return intTypeValue;
            }
            else
            {
                DecimalTypeValue decimalTypeValue = new DecimalTypeValue();
                decimalTypeValue.value = Double.parseDouble(ctx.NUMBER().getText());
                return decimalTypeValue;
            }
        }
        else if (ctx.STRING() != null)
        {
            StringTypeValue stringType = new StringTypeValue();
            stringType.value = ctx.STRING().getText();
            return stringType;
        }
        else if (ctx.BOOLEAN() != null)
        {
            BoolTypeValue boolTypeValue = new BoolTypeValue();
            boolTypeValue.value = Boolean.parseBoolean(ctx.BOOLEAN().getText());
            return boolTypeValue;
        }
        else if (ctx.NULL() != null)
        {
            return new NullTypeValue();
        }

        return new StringTypeValue();
    }

    private static boolean checkIfNumberIsInteger(String number)
    {
        try
        {
            Integer.parseInt(number);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }
}
