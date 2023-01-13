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
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.ArgumentExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.ExpressionObject;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.FieldPathExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.LiteralExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.MatchStage;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.OperatorExpression;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.Operators;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.aggregation.Stage;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.model.bson.BaseType;
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
        return ctx.stringValue().getText();
    }

    private AggregationPipeline visitPipeline(MongoDbQueryParser.PipelinesContext ctx)
    {
        AggregationPipeline aggregationPipeline = new AggregationPipeline();
        if (ctx.stageList() == null)
        {
            return aggregationPipeline;
        }
        List<Stage> matchStageList = ctx.stageList().matchStage().stream().map(this::visitMatchStage).collect(Collectors.toList());
        aggregationPipeline.stages = matchStageList;
        return aggregationPipeline;
    }

    private Stage visitMatchStage(MongoDbQueryParser.MatchStageContext ctx)
    {
        MatchStage matchStage = new MatchStage();
        if (ctx == null)
        {
            return matchStage;
        }
        List<AggregateExpression> aggregateExpressions = ctx.queryFilter().filterExpression().stream().map(this::visitAggregateExpression).collect(Collectors.toList());
        matchStage.expression = aggregateExpressions;
        return matchStage;

    }

    private AggregateExpression visitAggregateExpression(MongoDbQueryParser.FilterExpressionContext ctx)
    {
        AggregateExpression aggregateExpression = new AggregateExpression();
        if (ctx.simpleFilterExpression() != null)
        {
            String field = ctx.simpleFilterExpression().WORD().getText();
            String value = ctx.simpleFilterExpression().STRING().getText();
            aggregateExpression.arguments = Arrays.asList(buildArgumentExpression(field, new StringType(value), Operators.$eq.toString()));
            return aggregateExpression;
        }
        else if (ctx.filterExpressionWithOperator() != null)
        {
            String field = ctx.filterExpressionWithOperator().WORD().getText();
            String value = ctx.filterExpressionWithOperator().STRING().getText();
            String operator = ctx.filterExpressionWithOperator().QUERY_SELECTOR().getText();
            aggregateExpression.arguments = Arrays.asList(buildArgumentExpression(field, new StringType(value), operator));
            return aggregateExpression;
        }
        throw new RuntimeException("Non of the expected expressions were found");
    }

    private static ArgumentExpression buildArgumentExpression(String field, BaseType value, String operator)
    {
        Operators currentOperator = operator == null ? Operators.$eq : Operators.valueOf(operator);
        FieldPathExpression fieldPathExpression = new FieldPathExpression(field);
        LiteralExpression literalExpression = new LiteralExpression(value);
        ExpressionObject expressionObject = new ExpressionObject(fieldPathExpression, literalExpression);
        OperatorExpression operatorExpression = new OperatorExpression(currentOperator, expressionObject);
        return operatorExpression;
    }
}
