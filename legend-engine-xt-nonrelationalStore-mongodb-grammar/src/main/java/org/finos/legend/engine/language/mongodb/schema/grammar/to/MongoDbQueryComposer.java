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

package org.finos.legend.engine.language.mongodb.schema.grammar.to;

import org.finos.legend.engine.language.mongodb.schema.grammar.from.ComparisonOperator;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.LogicalOperator;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AndOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArrayTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BaseTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BoolTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ComparisonOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ComputedFieldValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DecimalTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.EqOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GreaterThanEqualsOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.InOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.IntTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.KeyValueExpressionPair;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.KeyValuePair;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LiteralValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LogicalOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.MatchStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NinOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NorOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NotOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NullTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OrOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ProjectStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Stage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StringTypeValue;

import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.mongodb.schema.grammar.from.ComparisonOperator.EQ;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.ComparisonOperator.GT;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.ComparisonOperator.GTE;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.ComparisonOperator.IN;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.ComparisonOperator.LT;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.ComparisonOperator.LTE;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.ComparisonOperator.NE;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.ComparisonOperator.NIN;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.LogicalOperator.AND;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.LogicalOperator.NOR;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.LogicalOperator.NOT;
import static org.finos.legend.engine.language.mongodb.schema.grammar.from.LogicalOperator.OR;

public class MongoDbQueryComposer
{

    public String parseDatabaseCommand(DatabaseCommand databaseCommand)
    {
        String collectionName = databaseCommand.collectionName;
        return "{ \"aggregate\": " + collectionName + " , " + visitDatabaseCommand(databaseCommand) +
                ", \"cursor\": {} }";
    }

    private String visitDatabaseCommand(DatabaseCommand databaseCommand)
    {
        String pipelineStages = visitPipelineStages(databaseCommand.aggregationPipeline.stages);
        return "\"pipeline\" : [" + pipelineStages + "]";
    }

    private String visitPipelineStages(List<Stage> stages)
    {
        List<String> strings = stages.stream().map(stage ->
        {
            if (stage instanceof MatchStage)
            {
                return "{ \"$match\" : " + visitExpression(((MatchStage) stage).expression) + " }";
            }
            else if (stage instanceof ProjectStage)
            {
                return "{ \"$project\" : " + visitExpression(((ProjectStage) stage).filters) + " }";
            }
            throw new RuntimeException("Unknown Stage at visitPipelineStages");

        }).collect(Collectors.toList());

        return String.join(",", strings);
    }

    private String visitExpression(ArgumentExpression expression)
    {

        if (expression instanceof LogicalOperatorExpression)
        {

            List<String> expressionsString = ((LogicalOperatorExpression) expression).expressions.stream()
                    .map(this::visitExpression).collect(Collectors.toList());

            LogicalOperator op;

            if (expression instanceof OrOperatorExpression)
            {
                op = OR;
            }
            else if (expression instanceof AndOperatorExpression)
            {
                op = AND;
            }
            else if (expression instanceof NotOperatorExpression)
            {
                op = NOT;
            }
            else if (expression instanceof NorOperatorExpression)
            {
                op = NOR;
            }
            else
            {
                throw new RuntimeException("Unknown OperatorExpression expression");
            }

            return "{ \"" + op.label + "\" : [" + String.join(",", expressionsString) + "] }";

        }
        else if (expression instanceof ComparisonOperatorExpression)
        {

            String expString = visitExpression(((ComparisonOperatorExpression) expression).expression);
            ComparisonOperator op;
            if (expression instanceof EqOperatorExpression)
            {
                op = EQ;
            }
            else if (expression instanceof GTOperatorExpression)
            {
                op = GT;
            }
            else if (expression instanceof LTOperatorExpression)
            {
                op = LT;
            }
            else if (expression instanceof LTEOperatorExpression)
            {
                op = LTE;
            }
            else if (expression instanceof GreaterThanEqualsOperatorExpression)
            {
                op = GTE;
            }
            else if (expression instanceof NEOperatorExpression)
            {
                op = NE;
            }
            else if (expression instanceof NinOperatorExpression)
            {
                op = NIN;
            }
            else if (expression instanceof InOperatorExpression)
            {
                op = IN;
            }
            else
            {
                throw new RuntimeException("Unknown OperatorExpression expression");
            }

            return "{ \"" + op.label + "\" : " + expString + " }";
        }
        else if (expression instanceof ObjectExpression)
        {

            List<String> objPairString = ((ObjectExpression) expression).keyValues.stream()
                    .map(this::visitKeyValueExpressionPair).collect(Collectors.toList());

            return "{" + String.join(",", objPairString) + "}";
        }
        else if (expression instanceof ComputedFieldValue)
        {
            return ((ComputedFieldValue) expression).value;
        }
        else if (expression instanceof LiteralValue)
        {
            return visitBaseType(((LiteralValue) expression).value);
        }
        else
        {
            return "{}";
        }
    }

    private String visitBaseType(BaseTypeValue value)
    {

        if (value instanceof StringTypeValue)
        {
            return ((StringTypeValue) value).value;
        }
        else if (value instanceof IntTypeValue)
        {
            return String.valueOf(((IntTypeValue) value).value);
        }
        else if (value instanceof DecimalTypeValue)
        {
            return String.valueOf(((DecimalTypeValue) value).value);
        }
        else if (value instanceof BoolTypeValue)
        {
            return String.valueOf(((BoolTypeValue) value).value);
        }
        else if (value instanceof NullTypeValue)
        {
            return null;
        }
        else if (value instanceof ObjectTypeValue)
        {
            List<String> objPairString = ((ObjectTypeValue) value).keyValues.stream()
                    .map(this::visitKeyValuePair).collect(Collectors.toList());
            return "{" + String.join(",", objPairString) + "}";
        }
        else if (value instanceof ArrayTypeValue)
        {
            List<String> arrayItemString = ((ArrayTypeValue) value).items.stream().map(this::visitBaseType).collect(Collectors.toList());

            return "[" + String.join(",", arrayItemString) + "]";
        }
        throw new RuntimeException("Unknown expression at visitLiteralValue");
    }

    private String visitKeyValuePair(KeyValuePair pair)
    {
        String field = pair.key;
        String value = visitBaseType(pair.value);
        return field + " : " + value;
    }

    public String visitKeyValueExpressionPair(KeyValueExpressionPair pair)
    {
        String field = pair.field;
        String value = visitExpression(pair.argument);
        return field + " : " + value;
    }

}
