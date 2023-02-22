// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.mongodb.schema.grammar.from;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AggregationPipeline;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AndOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpressionVisitor;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BoolTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DecimalTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.EqOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GTEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.InOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.IntTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.KeyValueExpressionPair;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.KeyValuePair;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LiteralValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LongTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.MatchStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NinOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NullTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ProjectStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Stage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StringTypeValue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MongoDBQueryDeserializer extends StdDeserializer<DatabaseCommand>
{

    public static final String OPERATOR_EXPRESSION = "OperatorExpression";
    public static final String FIELD_NAME = "fieldName";
    public static final String EXPR = "$expr";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    Set<String> comparisonOperators = Sets.mutable.of(
            "$eq", "$gt", "$gte", "$lt", "$lte", "$ne", "$in", "$nin");
    Set<String> logicalOperators = Sets.mutable.of("$and", "$or", "$nor", "$not");

    public MongoDBQueryDeserializer()
    {
        this(null);
    }

    protected MongoDBQueryDeserializer(Class<?> vc)
    {
        super(vc);
    }

    private static LiteralValue getLiteralValue(Map.Entry<String, JsonNode> entry)
    {
        LiteralValue literalValue = new LiteralValue();
        if (entry.getValue().isValueNode())
        {
            if (entry.getValue().isTextual())
            {
                StringTypeValue typeValue = new StringTypeValue();
                typeValue.value = entry.getValue().textValue();
                literalValue.value = typeValue;
            }
            else if (entry.getValue().isBoolean())
            {
                BoolTypeValue typeValue = new BoolTypeValue();
                typeValue.value = entry.getValue().booleanValue();
                literalValue.value = typeValue;
            }
            else if (entry.getValue().isInt())
            {
                IntTypeValue typeValue = new IntTypeValue();
                typeValue.value = entry.getValue().intValue();
                literalValue.value = typeValue;
            }
            else if (entry.getValue().isLong())
            {
                LongTypeValue typeValue = new LongTypeValue();
                typeValue.value = entry.getValue().longValue();
                literalValue.value = typeValue;
            }
            else if (entry.getValue().isDouble() || entry.getValue().isFloat() || entry.getValue().isBigDecimal())
            {
                DecimalTypeValue typeValue = new DecimalTypeValue();
                typeValue.value = new BigDecimal(entry.getValue().asText());
                literalValue.value = typeValue;
            }
            else if (entry.getValue().isNull())
            {
                NullTypeValue typeValue = new NullTypeValue();
                literalValue.value = typeValue;
            }
        }
        else if (entry.getValue().isObject())
        {
            ObjectTypeValue objTypeValue = new ObjectTypeValue();
            List<KeyValuePair> kvPairs = getKeyValuePairs(entry);
            objTypeValue.keyValues = kvPairs;
            literalValue.value = objTypeValue;
        }
        return literalValue;
    }

    private static List<KeyValuePair> getKeyValuePairs(Map.Entry<String, JsonNode> entry)
    {
        Iterator<Map.Entry<String, JsonNode>> literalObjFields = entry.getValue().fields();
        List<KeyValuePair> kvPairs = Lists.mutable.empty();
        while (literalObjFields.hasNext())
        {
            Map.Entry<String, JsonNode> literalObjEntry = literalObjFields.next();
            KeyValuePair kvPair = new KeyValuePair();
            kvPair.key = literalObjEntry.getKey();
            kvPair.value = getLiteralValue(literalObjEntry).value;
            kvPairs.add(kvPair);
        }
        return kvPairs;
    }

    /*
    {
  "aggregate": "firm",
  "pipeline": [
    {
      "$match": {
        "name": {
          "$eq": "Zemlak-Hegmann"
        }
      }
    }
  ],
  "cursor": {}
}
     */
    @Override
    public DatabaseCommand deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException
    {

        DatabaseCommand dbCommand = new DatabaseCommand();

        // Check the first token
        if (jsonParser.currentToken() != JsonToken.START_OBJECT)
        {
            throw new IllegalStateException("Expected database command node to be an object");
        }
        while (jsonParser.nextToken() != JsonToken.END_OBJECT)
        {
            String propertyName = jsonParser.getCurrentName();
            jsonParser.nextToken();
            switch (propertyName)
            {
                case "aggregate":
                    dbCommand.type = "aggregate";
                    dbCommand.collectionName = jsonParser.getText();
                    break;
                case "pipeline":
                    dbCommand.aggregationPipeline = getAggPipeline(jsonParser);
                    break;
                case "cursor":
                    LOGGER.info("Output = cursor, default format for output");
                    break;
                default:
                    LOGGER.trace("Ignoring unknown property: {}", propertyName);
            }
        }
        return dbCommand;
    }

    private AggregationPipeline getAggPipeline(JsonParser jsonParser) throws IOException
    {
        AggregationPipeline aggPipeline = new AggregationPipelineImpl();

        if (jsonParser.currentToken() != JsonToken.START_ARRAY)
        {
            throw new IllegalStateException("Expected aggregationPipeline node to be an array");
        }
        List<Stage> aggStages = Lists.mutable.empty();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY)
        {
            Stage aggStage = getStage(jsonParser);
            aggStages.add(aggStage);
        }
        aggPipeline.stages = aggStages;
        return aggPipeline;
    }

    private Stage getStage(JsonParser jsonParser) throws IOException
    {

        JsonNode stageNode = jsonParser.getCodec().readTree(jsonParser);
        if (!stageNode.isObject())
        {
            throw new IllegalStateException("Expected Stage command node to be an object");
        }
        // Each Stage can have one of the following

        if (stageNode.has("$match"))
        {
            return getMatchStage(stageNode.get("$match"));
        }
        else if (stageNode.has("$project"))
        {
            return getProjectStage(stageNode.get("$project"));
        }
        else
        {
            LOGGER.error("Ignoring stage - at {}", jsonParser.getCurrentLocation().getLineNr());

        }
        throw new IllegalStateException("Unsupported stages provided in pipeline, refer above");
    }

    private Stage getMatchStage(JsonNode matchNode) throws IOException
    {
        // Parse this as an object instead of streaming
        // We will want to support both $expr : {}  semantics & field : expr operations

        MatchStage matchStage = new MatchStage();

        if (!matchNode.isObject())
        {
            throw new IllegalStateException("Expected jsonSchema node to be an object");
        }
        matchStage.expression = getMatchExpression(matchNode);

        return matchStage;
    }

    private ArgumentExpression getMatchExpression(JsonNode matchNode)
    {
        Iterator<Map.Entry<String, JsonNode>> matchNodeFields = matchNode.fields();
        while (matchNodeFields.hasNext())
        {
            Map.Entry<String, JsonNode> entry = matchNodeFields.next();
            String keyType = getOperatorType(entry.getKey());
            switch (keyType)
            {
                case FIELD_NAME:
                    // Match stage defined with a field name as starting point "name" : {....
                    //objExprValues.add(getFieldBasedOperation(entry));
                    break;
                case OPERATOR_EXPRESSION:
                    // Match stage defined with operator as starting point "$eq" : {....
                    //objExprValues.add(getOperatorBasedOperation(entry));
                    break;
                case EXPR:
                    // Match stage defined with expr as starting point "$expr" : { "$eq"....
                    break;
            }
        }

        //return objExpression;
        return null;

    }

    private KeyValueExpressionPair getOperatorBasedOperation(Map.Entry<String, JsonNode> entry)
    {
        KeyValueExpressionPair kvExpr = new KeyValueExpressionPair();
        kvExpr.field = entry.getKey();
        switch (kvExpr.field)
        {
            case "$and":
                //List<ObjectExpression> expr = getObjectExpressions(entry.getValue());
                AndOperatorExpression andOpExpression = new AndOperatorExpression();
                andOpExpression.expressions = getObjectExpressions(entry.getValue());
                kvExpr.argument = andOpExpression;
                break;
            case "$or":
                break;
            case "$not":
                break;
        }
        return kvExpr;
    }

    private List<ArgumentExpression> getObjectExpressions(JsonNode value)
    {
        List<ArgumentExpression> objExpressions = Lists.mutable.of();
        if (value.isArray())
        {
            for (JsonNode item : value)
            {
                ArgumentExpression objExpression = getMatchExpression(value);
                objExpressions.add(objExpression);
            }
        }
        else if (value.isObject())
        {
            ArgumentExpression objExpression = getMatchExpression(value);
            objExpressions.add(objExpression);
        }
        return objExpressions;
    }

    private ArgumentExpression getObjectExpression(Map.Entry<String, JsonNode> entry)
    {
        ObjectExpression objExpression = new ObjectExpression();
        List<KeyValueExpressionPair> objExprValues = Lists.mutable.empty();
        objExprValues.add(getFieldBasedOperation(entry));
        objExpression.keyValues = objExprValues;
        return objExpression;
    }

    private KeyValueExpressionPair getFieldBasedOperation(Map.Entry<String, JsonNode> entry)
    {
        KeyValueExpressionPair kvExpr = new KeyValueExpressionPair();
        kvExpr.field = entry.getKey();
        if (entry.getValue().isValueNode())
        {
            //something like { "name" : "joe"  }
            LiteralValue literalValue = getLiteralValue(entry);
            kvExpr.argument = literalValue;
        }
        else if (entry.getValue().isObject())
        {
            // something like: { "name" : {$eq : "joe" } }
            ArgumentExpression singleOpExpression = getSingleOperatorExpression(entry);
            kvExpr.argument = singleOpExpression;
        }
        return kvExpr;
    }

    private ArgumentExpression getSingleOperatorExpression(Map.Entry<String, JsonNode> entry)
    {
        Iterator<Map.Entry<String, JsonNode>> opExprNodeFields = entry.getValue().fields();
        while (opExprNodeFields.hasNext())
        {
            Map.Entry<String, JsonNode> opExprEntry = opExprNodeFields.next();
            switch (opExprEntry.getKey())
            {
                case "$eq":
                    EqOperatorExpression eqOpExpression = new EqOperatorExpression();
                    if (opExprEntry.getValue().isValueNode())
                    {
                        LiteralValue eqOpliteralValue = getLiteralValue(opExprEntry);
                        eqOpExpression.expression = eqOpliteralValue;
                    }
                    else if (opExprEntry.getValue().isObject())
                    {
                        // Todo : Is there difference between value node vs object here?
                        LiteralValue eqOpliteralValue = getLiteralValue(opExprEntry);
                        eqOpExpression.expression = eqOpliteralValue;
                    }
                    return eqOpExpression;
                case "$ne":
                    NEOperatorExpression neOpExpression = new NEOperatorExpression();
                    LiteralValue neOpliteralValue = getLiteralValue(opExprEntry);
                    neOpExpression.expression = neOpliteralValue;
                    return neOpExpression;
                case "$gt":
                    GTOperatorExpression gtOpExpression = new GTOperatorExpression();
                    LiteralValue gtOpliteralValue = getLiteralValue(opExprEntry);
                    gtOpExpression.expression = gtOpliteralValue;
                    return gtOpExpression;
                case "$gte":
                    GTEOperatorExpression gteOpExpression = new GTEOperatorExpression();
                    LiteralValue gteliteralValue = getLiteralValue(opExprEntry);
                    gteOpExpression.expression = gteliteralValue;
                    return gteOpExpression;
                case "$lt":
                    LTOperatorExpression ltOpExpression = new LTOperatorExpression();
                    LiteralValue ltliteralValue = getLiteralValue(opExprEntry);
                    ltOpExpression.expression = ltliteralValue;
                    return ltOpExpression;
                case "$lte":
                    LTEOperatorExpression lteOpExpression = new LTEOperatorExpression();
                    LiteralValue lteliteralValue = getLiteralValue(opExprEntry);
                    lteOpExpression.expression = lteliteralValue;
                    return lteOpExpression;
                case "$in":
                    InOperatorExpression inOpExpression = new InOperatorExpression();
                    LiteralValue inliteralValue = getLiteralValue(opExprEntry);
                    inOpExpression.expression = inliteralValue;
                    return inOpExpression;
                case "$nin":
                    NinOperatorExpression ninOpExpression = new NinOperatorExpression();
                    LiteralValue ninliteralValue = getLiteralValue(opExprEntry);
                    ninOpExpression.expression = ninliteralValue;
                    return ninOpExpression;
            }

        }
        throw new IllegalStateException("Failed to match any supported Operator");
    }

    private String getOperatorType(String key)
    {
        if (EXPR.equals(key))
        {
            return EXPR;
        }
        else if (isSupportedOperation(key))
        {
            return OPERATOR_EXPRESSION;
        }
        else
        {
            return FIELD_NAME;
        }
    }

    private boolean isSupportedOperation(String key)
    {

        return comparisonOperators.contains(key) || logicalOperators.contains(key);

    }

    private Stage getProjectStage(JsonNode projectNode) throws IOException
    {
        return new ProjectStage();
    }


}
