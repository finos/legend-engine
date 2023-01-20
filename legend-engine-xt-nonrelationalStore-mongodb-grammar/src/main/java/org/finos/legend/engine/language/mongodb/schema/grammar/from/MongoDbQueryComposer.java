package org.finos.legend.engine.language.mongodb.schema.grammar.from;

import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Item;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StringTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AndExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArrayArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ExpressionObject;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.FieldPathExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LiteralValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.MatchStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OrExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Stage;
import java.util.List;
import java.util.stream.Collectors;

public class MongoDbQueryComposer {

    public String parser(DatabaseCommand databaseCommand)
    {
        String collectionName = databaseCommand.collectionName;
        return "{ \"aggregate\": " + collectionName + " , " + visitDatabaseCommand(databaseCommand) + ", \"cursor\": {} }";
    }

    public String visitDatabaseCommand(DatabaseCommand databaseCommand)
    {
        String pipelineStages = visitPipelineStages(databaseCommand.aggregationPipeline.stages).toString();
        return "\"pipeline\" : [" + pipelineStages + "]";

    }

    public StringBuilder visitPipelineStages(List<Stage> stages)
    {
        List<String> strings = stages.stream().map(x -> {
            if (x instanceof MatchStage)
            {
                return "{ \"$match\" : " + visitExpression(((MatchStage) x).expression).toString() + " }";
            }
            else
            {
                return new StringBuilder().toString();
            }
        }).collect(Collectors.toList());
        return new StringBuilder(String.join(",", strings));
    }

    public StringBuilder visitExpression(ArgumentExpression expression)
    {
        if (expression instanceof OrExpression || expression instanceof AndExpression)
        {
            if (expression instanceof OrExpression)
            {
                List<String> strings = ((OrExpression) expression).expressions.stream().map(x -> visitExpression(x).toString()).collect(Collectors.toList());
                return new StringBuilder("\"$or\": " + String.join("", strings));
            }
            else
            {
                List<String> strings = ((AndExpression) expression).expressions.stream().map(x -> visitExpression(x).toString()).collect(Collectors.toList());
                return new StringBuilder("\"$and\" : [" + String.join("", strings));
            }
        }
        else if (expression instanceof OperatorExpression)
        {
            String operator = String.valueOf(((OperatorExpression) expression).operator);
            String currentExpression = visitExpression(((OperatorExpression) expression).expression).toString();
            return new StringBuilder("{ \"" + operator + "\" : " + currentExpression + " }");
        }
        else if (expression instanceof ExpressionObject)
        {
            String field = visitExpression(((ExpressionObject) expression).field).toString();
            String argument = visitExpression(((ExpressionObject) expression).argument).toString();
            return new StringBuilder(field + " : " + argument);
        }
        else if (expression instanceof ArrayArgumentExpression)
        {
            List<String> strings = ((ArrayArgumentExpression) expression).items.stream().map(x -> visitExpression(x).toString()).collect(Collectors.toList());
            return new StringBuilder("[" + String.join(",", strings) + "]");
        }
        else if (expression instanceof Item)
        {
            List<String> strings = ((Item) expression).objects.stream().map(x -> visitExpression(x).toString()).collect(Collectors.toList());
            return new StringBuilder("{" + String.join(",", strings) + "}");
        }
        else if (expression instanceof FieldPathExpression)
        {
            return new StringBuilder(((FieldPathExpression) expression).path);
        }
        else if (expression instanceof LiteralValue)
        {
            return new StringBuilder(((StringTypeValue) ((LiteralValue) expression).value).value);
        }
        throw new RuntimeException("something went wrong");
    }
}
