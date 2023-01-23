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

import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BoolTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DecimalTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.IntTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Item;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NullTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StringTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AndExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArrayExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.FieldPathExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LiteralValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.MatchStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OrExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Stage;
import java.util.List;
import java.util.stream.Collectors;

public class MongoDbQueryComposer
{

    public String parser(DatabaseCommand databaseCommand)
    {
        String collectionName = databaseCommand.collectionName;
        return "{ \"aggregate\": " + collectionName + " , " + visitDatabaseCommand(databaseCommand) + ", \"cursor\": {} }";
    }

    public String visitDatabaseCommand(DatabaseCommand databaseCommand)
    {
        String pipelineStages = visitPipelineStages(databaseCommand.aggregationPipeline.stages);
        return "\"pipeline\" : [" + pipelineStages + "]";

    }

    public String visitPipelineStages(List<Stage> stages)
    {
        List<String> strings = stages.stream().map(x ->
        {
            if (x instanceof MatchStage)
            {
                return "{ \"$match\" : " + visitExpression(((MatchStage) x).expression) + " }";
            }
            else
            {
                return "";
            }
        }).collect(Collectors.toList());
        return String.join(",", strings);
    }

    public String visitExpression(ArgumentExpression expression)
    {
        if (expression instanceof OrExpression || expression instanceof AndExpression)
        {
            if (expression instanceof OrExpression)
            {
                List<String> strings = ((OrExpression) expression).expressions.stream().map(x -> visitExpression(x)).collect(Collectors.toList());
                return "\"$or\": " + String.join("", strings);
            }
            else
            {
                List<String> strings = ((AndExpression) expression).expressions.stream().map(x -> visitExpression(x)).collect(Collectors.toList());
                return "\"$and\" : [" + String.join("", strings);
            }
        }
        else if (expression instanceof OperatorExpression)
        {
            String operator = String.valueOf(((OperatorExpression) expression).operator);
            String currentExpression = visitExpression(((OperatorExpression) expression).expression);
            return "{ \"" + operator + "\" : " + currentExpression + " }";
        }
        else if (expression instanceof ObjectExpression)
        {
            String field = visitExpression(((ObjectExpression) expression).field);
            String argument = visitExpression(((ObjectExpression) expression).argument);
            return field + " : " + argument;
        }
        else if (expression instanceof ArrayExpression)
        {
            List<String> strings = ((ArrayExpression) expression).items.stream().map(x -> visitExpression(x)).collect(Collectors.toList());
            return "[" + String.join(",", strings) + "]";
        }
        else if (expression instanceof Item)
        {
            List<String> strings = ((Item) expression).objects.stream().map(x -> visitExpression(x)).collect(Collectors.toList());
            return "{" + String.join(",", strings) + "}";
        }
        else if (expression instanceof FieldPathExpression)
        {
            return ((FieldPathExpression) expression).path;
        }
        else if (expression instanceof LiteralValue)
        {
            if (((LiteralValue) expression).value instanceof StringTypeValue)
            {
                return ((StringTypeValue) ((LiteralValue) expression).value).value;
            }
            else if (((LiteralValue) expression).value instanceof IntTypeValue)
            {
                return String.valueOf(((IntTypeValue) ((LiteralValue) expression).value).value);
            }
            else if (((LiteralValue) expression).value instanceof DecimalTypeValue)
            {
                return String.valueOf(((DecimalTypeValue) ((LiteralValue) expression).value).value);
            }
            else if (((LiteralValue) expression).value instanceof BoolTypeValue)
            {
                return String.valueOf(((BoolTypeValue) ((LiteralValue) expression).value).value);
            }
            else if (((LiteralValue) expression).value instanceof NullTypeValue)
            {
                return null;
            }
        }
        throw new RuntimeException("something went wrong");
    }
}
