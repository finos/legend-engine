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

import org.finos.legend.engine.language.mongodb.schema.grammar.roundtrip.OperatorUtility;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AndOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpressionVisitor;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArrayTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BaseTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BaseTypeValueVisitor;
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
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LongTypeValue;
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
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StageVisitor;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StringTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Operator;

import java.util.List;
import java.util.stream.Collectors;

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
        List<String> strings = stages.stream().map(this::visitPipelineStage).collect(Collectors.toList());
        return String.join(",", strings);
    }

    private String visitPipelineStage(Stage stage)
    {
        return stage.accept(new StageVisitor<String>()
        {

            @Override
            public String visit(MatchStage val)
            {
                return "{ \"$match\" : " + visitArgumentExpression(((MatchStage) stage).expression) + " }";
            }

            @Override
            public String visit(ProjectStage val)
            {
                return "{ \"$project\" : " + visitArgumentExpression(((ProjectStage) stage).filters) + " }";
            }
        });
    }

    private String visitArgumentExpression(ArgumentExpression argumentExpression)
    {

        if (argumentExpression == null)
        {
            return "{}";
        }

        return argumentExpression.accept(new ArgumentExpressionVisitor<String>()
        {

            @Override
            public String visit(AndOperatorExpression val)
            {
                List<String> expressionsString = val.expressions.stream()
                        .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.AND) + "\" : [" + String.join(",", expressionsString) + "] }";
            }

            @Override
            public String visit(ComparisonOperatorExpression val)
            {
                return null;
            }

            @Override
            public String visit(ComputedFieldValue val)
            {
                return val.value;
            }

            @Override
            public String visit(EqOperatorExpression val)
            {
                String expString = visit(val.expression);
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.EQ) + "\" : " + expString + " }";
            }

            @Override
            public String visit(GTOperatorExpression val)
            {
                String expString = visit(val.expression);
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.GT) + "\" : " + expString + " }";
            }

            @Override
            public String visit(GreaterThanEqualsOperatorExpression val)
            {
                String expString = visit(val.expression);
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.GTE) + "\" : " + expString + " }";
            }

            @Override
            public String visit(InOperatorExpression val)
            {
                String expString = visit(val.expression);
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.IN) + "\" : " + expString + " }";
            }

            @Override
            public String visit(LTEOperatorExpression val)
            {
                String expString = visit(val.expression);
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.LTE) + "\" : " + expString + " }";
            }

            @Override
            public String visit(LTOperatorExpression val)
            {
                String expString = visit(val.expression);
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.LT) + "\" : " + expString + " }";
            }

            @Override
            public String visit(LiteralValue val)
            {
                return visitBaseTypeValue(val.value);
            }

            @Override
            public String visit(LogicalOperatorExpression val)
            {
                return null;
            }

            @Override
            public String visit(NEOperatorExpression val)
            {
                String expString = visit(val.expression);
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.NE) + "\" : " + expString + " }";
            }

            @Override
            public String visit(NinOperatorExpression val)
            {
                String expString = visit(val.expression);
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.NIN) + "\" : " + expString + " }";
            }

            @Override
            public String visit(NorOperatorExpression val)
            {
                List<String> expressionsString = val.expressions.stream()
                        .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.NOR) + "\" : [" + String.join(",", expressionsString) + "] }";
            }

            @Override
            public String visit(NotOperatorExpression val)
            {
                List<String> expressionsString = val.expressions.stream()
                        .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.NOT) + "\" : [" + String.join(",", expressionsString) + "] }";
            }

            @Override
            public String visit(ObjectExpression val)
            {
                List<String> objPairString = val.keyValues.stream()
                        .map(x -> visitKeyValueExpressionPair(x)).collect(Collectors.toList());
                return "{" + String.join(",", objPairString) + "}";
            }

            @Override
            public String visit(OrOperatorExpression val)
            {
                List<String> expressionsString = val.expressions.stream()
                        .map(x -> visitArgumentExpression(x)).collect(Collectors.toList());
                return "{ \"" + OperatorUtility.lowerCaseOperatorAndAddDollar(Operator.OR) + "\" : [" + String.join(",", expressionsString) + "] }";
            }
        });
    }

    public String visitBaseTypeValue(BaseTypeValue value)
    {
        return value.accept(new BaseTypeValueVisitor<String>()
        {
            @Override
            public String visit(ArrayTypeValue val)
            {
                List<String> arrayItemString = ((ArrayTypeValue) value).items.stream().map(x -> visitBaseTypeValue(x)).collect(Collectors.toList());

                return "[" + String.join(",", arrayItemString) + "]";
            }

            @Override
            public String visit(BoolTypeValue val)
            {
                return String.valueOf(val.value);
            }

            @Override
            public String visit(DecimalTypeValue val)
            {
                return String.valueOf(val.value);
            }

            @Override
            public String visit(IntTypeValue val)
            {
                return String.valueOf(val.value);
            }

            @Override
            public String visit(LongTypeValue val)
            {
                // not used
                return String.valueOf(val);
            }

            @Override
            public String visit(NullTypeValue val)
            {
                return null;
            }

            @Override
            public String visit(ObjectTypeValue val)
            {
                List<String> objPairString = val.keyValues.stream()
                        .map(x -> visitKeyValuePair(x)).collect(Collectors.toList());
                return "{" + String.join(",", objPairString) + "}";
            }

            @Override
            public String visit(StringTypeValue val)
            {
                return String.valueOf(val.value);
            }
        });
    }

    private String visitKeyValuePair(KeyValuePair pair)
    {
        String field = pair.key;
        String value = visitBaseTypeValue(pair.value);
        return field + " : " + value;
    }

    public String visitKeyValueExpressionPair(KeyValueExpressionPair pair)
    {
        String field = pair.field;
        String value = visitArgumentExpression(pair.argument);
        return field + " : " + value;
    }

}
