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

package org.finos.legend.engine.language.pure.grammar.from.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AggregationPipeline;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.AndOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArrayTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BaseTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.BoolTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DatabaseCommand;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.DecimalTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.EqOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ExprQueryExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.FieldPathExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GTEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.GTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.InOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.IntTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.KeyValuePair;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LTOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LiteralValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.LongTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.MatchStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NEOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NinOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NotOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.NullTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectQueryExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ObjectTypeValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.OrOperatorExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ProjectStage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.QueryExprKeyValue;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.Stage;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.StringTypeValue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class MongoDBQueryDeserializer extends StdDeserializer<DatabaseCommand>
{

    public static final String OPERATOR_EXPRESSION = "OperatorExpression";
    public static final String FIELD_NAME = "fieldName";
    public static final String EXPR = "$expr";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MongoDBQueryDeserializer.class);
    static Set<String> comparisonOperators = Sets.mutable.of("$eq", "$gt", "$gte", "$lt", "$lte", "$ne", "$in", "$nin");
    static Set<String> logicalOperators = Sets.mutable.of("$and", "$or", "$nor", "$not");

    public MongoDBQueryDeserializer()
    {
        this(null);
    }

    protected MongoDBQueryDeserializer(Class<?> vc)
    {
        super(vc);
    }

    private static LiteralValue getLiteralValueFromEntry(Map.Entry<String, JsonNode> entry)
    {

        return getLiteralValueFromNode(entry.getValue());

    }

    private static LiteralValue getLiteralValueFromNode(JsonNode jsonNode)
    {
        LiteralValue literalValue = new LiteralValue();

        if (jsonNode.isValueNode())
        {
            if (jsonNode.isTextual())
            {
                StringTypeValue typeValue = new StringTypeValue();
                typeValue.value = jsonNode.textValue();
                literalValue.value = typeValue;
            }
            else if (jsonNode.isBoolean())
            {
                BoolTypeValue typeValue = new BoolTypeValue();
                typeValue.value = jsonNode.booleanValue();
                literalValue.value = typeValue;
            }
            else if (jsonNode.isInt())
            {
                IntTypeValue typeValue = new IntTypeValue();
                typeValue.value = jsonNode.longValue();
                literalValue.value = typeValue;
            }
            else if (jsonNode.isLong())
            {
                LongTypeValue typeValue = new LongTypeValue();
                typeValue.value = jsonNode.longValue();
                literalValue.value = typeValue;
            }
            else if (jsonNode.isDouble() || jsonNode.isFloat() || jsonNode.isBigDecimal())
            {
                DecimalTypeValue typeValue = new DecimalTypeValue();
                typeValue.value = new BigDecimal(jsonNode.asText());
                literalValue.value = typeValue;
            }
            else if (jsonNode.isNull())
            {
                literalValue.value = new NullTypeValue();
            }
        }
        else if (jsonNode.isObject())
        {
            ObjectTypeValue objTypeValue = new ObjectTypeValue();
            List<KeyValuePair> kvPairs = getKeyValuePairs(jsonNode);
            objTypeValue.keyValues = kvPairs;
            literalValue.value = objTypeValue;
        }
        else if (jsonNode.isArray())
        {
            ArrayTypeValue arrayTypeValue = new ArrayTypeValue();
            List<BaseTypeValue> items = Lists.mutable.of();
            for (JsonNode item : jsonNode)
            {
                BaseTypeValue itemValue = getLiteralValueFromNode(item).value;
                items.add(itemValue);
            }
            arrayTypeValue.items = items;
            literalValue.value = arrayTypeValue;
        }
        return literalValue;


    }

    private static List<KeyValuePair> getKeyValuePairs(JsonNode jsonNode)
    {
        Iterator<Map.Entry<String, JsonNode>> literalObjFields = jsonNode.fields();
        List<KeyValuePair> kvPairs = Lists.mutable.empty();
        while (literalObjFields.hasNext())
        {
            Map.Entry<String, JsonNode> literalObjEntry = literalObjFields.next();
            KeyValuePair kvPair = new KeyValuePair();
            if (isSupportedOperation(literalObjEntry.getKey()))
            {
                throw new IllegalStateException("Literal Object value has key that is a operator key word " + literalObjEntry.getKey());
            }
            kvPair.key = literalObjEntry.getKey();
            kvPair.value = getLiteralValueFromEntry(literalObjEntry).value;
            kvPairs.add(kvPair);
        }
        return kvPairs;
    }

    private static boolean isSupportedOperation(String key)
    {
        return comparisonOperators.contains(key) || logicalOperators.contains(key);
    }


    @Override
    public DatabaseCommand deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
    {

        DatabaseCommand dbCommand = new DatabaseCommand();

        while (jsonParser.currentToken() != JsonToken.END_OBJECT)
        {
            if (jsonParser.currentToken() == JsonToken.FIELD_NAME)
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
            jsonParser.nextToken();
        }
        return dbCommand;
    }

    private AggregationPipeline getAggPipeline(JsonParser jsonParser) throws IOException
    {
        AggregationPipeline aggPipeline = new AggregationPipeline();

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
        if (!stageNode.isObject() || stageNode.size() > 1)
        {
            throw new IllegalStateException("Expected Stage command node to be an object, with just 1 key(stage)");
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

    private Stage getMatchStage(JsonNode matchNode)
    {
        MatchStage matchStage = new MatchStage();

        if (!matchNode.isObject())
        {
            throw new IllegalStateException("Expected match node to be an object");
        }
        if (!matchNode.isEmpty())
        {
            // We support only $expr style syntax in match stage
            if (matchNode.get(EXPR) != null)
            {
                matchStage.expression = getMatchExpression(matchNode);
            }
            else
            {
                throw new IllegalStateException("Match stage supports only  $expr style syntax");
            }
        }
        else
        {
            matchStage.expression = new ArgumentExpression();
        }

        return matchStage;
    }

    private ArgumentExpression getProjectExpression(JsonNode matchNode)
    {
        Iterator<Map.Entry<String, JsonNode>> matchNodeFields = matchNode.fields();
        ObjectQueryExpression objQueryExpr = new ObjectQueryExpression();

        List<ArgumentExpression> argumentExpressions = Lists.mutable.empty();
        while (matchNodeFields.hasNext())
        {
            Map.Entry<String, JsonNode> entry = matchNodeFields.next();
            String keyType = getOperatorType(entry.getKey());
            switch (keyType)
            {
                case FIELD_NAME:
                    // Match stage defined with a field name as starting point "name" : {....
                    argumentExpressions.add(getFieldBasedProjectOperation(entry));
                    break;
                case OPERATOR_EXPRESSION:
                    throw new IllegalStateException("Project syntax does not support Operator Expression");
                case EXPR:
                    throw new IllegalStateException("Project syntax does not support Expr Expression");
            }
        }
        objQueryExpr.keyValues = argumentExpressions;
        return objQueryExpr;
    }

    private ArgumentExpression getMatchExpression(JsonNode matchNode)
    {
        if (!matchNode.isObject() || matchNode.size() != 1)
        {
            throw new IllegalStateException("Match Expression should contain a single $expr key : Object");
        }

        Iterator<Map.Entry<String, JsonNode>> matchNodeFields = matchNode.fields();
        while (matchNodeFields.hasNext())
        {
            Map.Entry<String, JsonNode> entry = matchNodeFields.next();
            String keyType = getOperatorType(entry.getKey());

            switch (keyType)
            {
                case EXPR:
                    // Match stage defined with expr as starting point EXPR : { "$eq"....
                    // Needs to be single expression (or last one wins)
                    ExprQueryExpression exprOperation = new ExprQueryExpression();
                    ArgumentExpression operationExpression = getOperationExpression(entry.getValue());
                    exprOperation.expression = operationExpression;
                    return exprOperation;
                case OPERATOR_EXPRESSION:
                    return getOperationExpression(matchNode);
            }
        }
        throw new IllegalStateException("Match Expression cannot be empty");
    }


    private ArgumentExpression getOperationExpression(JsonNode matchNode)
    {
        Iterator<Map.Entry<String, JsonNode>> matchNodeFields = matchNode.fields();
        // Check for single key
        boolean isFirstElementOperator = false;

        while (matchNodeFields.hasNext())
        {
            Map.Entry<String, JsonNode> entry = matchNodeFields.next();
            String keyType = getOperatorType(entry.getKey());
            switch (keyType)
            {
                case OPERATOR_EXPRESSION:
                    isFirstElementOperator = true;
                    Optional<ArgumentExpression> operatorExpression = getOperatorExpression(entry);
                    if (operatorExpression.isPresent())
                    {
                        return operatorExpression.get();
                    }
                    break;
                case FIELD_NAME:
                    if (isFirstElementOperator)
                    {
                        throw new IllegalStateException("An object representing an expression must have exactly one field");
                    }
                    return getLiteralValueFromNode(matchNode);
                default:
                    LOGGER.info("Get Operation invoked with a non-operator expression");

            }
        }
        throw new IllegalStateException("No operator found within $expr");
    }

    private NotOperatorExpression getNotOperatorExpression(Map.Entry<String, JsonNode> entry)
    {
        NotOperatorExpression notOpExpression = new NotOperatorExpression();
        // TODO: $not should be a single operation
        notOpExpression.expressions = Lists.fixedSize.of(getObjectQueryExpression(entry.getValue()));
        return notOpExpression;
    }

    private OrOperatorExpression getOrOperatorExpression(Map.Entry<String, JsonNode> entry)
    {
        OrOperatorExpression orOpExpression = new OrOperatorExpression();
        orOpExpression.expressions = getQueryExpressions(entry.getValue());
        return orOpExpression;
    }

    private AndOperatorExpression getAndOperatorExpression(Map.Entry<String, JsonNode> entry)
    {
        AndOperatorExpression andOpExpression = new AndOperatorExpression();
        andOpExpression.expressions = getQueryExpressions(entry.getValue());
        return andOpExpression;
    }

    private List<ArgumentExpression> getQueryExpressions(JsonNode value)
    {
        if (value.isArray() && value.size() > 0)
        {
            List<ArgumentExpression> objExpressions = Lists.mutable.of();

            for (JsonNode item : value)
            {
                if (item.isValueNode())
                {
                    throw new IllegalStateException("Logical Operators need object node or pattern as argument");
                }
                ArgumentExpression objExpression = getMatchExpression(item);
                objExpressions.add(objExpression);
            }
            return objExpressions;
        }
        throw new IllegalStateException("Logical Operators need non-zero array of Object Expressions ($and, $or, $nor)");
    }

    private ArgumentExpression getObjectQueryExpression(JsonNode value)
    {
        if (value.isObject())
        {
            ArgumentExpression objExpression = getMatchExpression(value);
            return objExpression;
        }
        else
        {
            throw new IllegalStateException("Operator need object node (eg., $not)");
        }
    }


    private QueryExprKeyValue getFieldBasedProjectOperation(Map.Entry<String, JsonNode> entry)
    {
        QueryExprKeyValue qryExprKeyValue = new QueryExprKeyValue();
        FieldPathExpression fieldPathExpr = new FieldPathExpression();
        fieldPathExpr.fieldPath = entry.getKey();
        qryExprKeyValue.key = fieldPathExpr;
        if (entry.getValue().isValueNode())
        {
            // We are looking at something like { "name" : 1 } or {"name" : true }
            // TODO : Add support for computed values
            LiteralValue literalValue = getLiteralValueFromEntry(entry);
            qryExprKeyValue.value = literalValue;
        }
        else
        {
            throw new IllegalStateException("Project syntax supports only field: 1 / bool");
        }
        return qryExprKeyValue;
    }


    private Optional<ArgumentExpression> getOperatorExpression(Map.Entry<String, JsonNode> opExprEntry)
    {
        switch (opExprEntry.getKey())
        {
            case EXPR:
                return Optional.of(getMatchExpression(opExprEntry.getValue()));
            case "$eq":
                EqOperatorExpression eqOpExpression = new EqOperatorExpression();
                if (!opExprEntry.getValue().isArray() && opExprEntry.getValue().size() != 2)
                {
                    throw new IllegalStateException("$eq operator accepts only array of 2 expressions");
                }
                else
                {
                    List<ArgumentExpression> argExprs = getExpressionParams(opExprEntry);
                    eqOpExpression.expressions = argExprs;
                }
                return Optional.of(eqOpExpression);
            case "$ne":
                NEOperatorExpression neOpExpression = new NEOperatorExpression();
                if (!opExprEntry.getValue().isArray() && opExprEntry.getValue().size() != 2)
                {
                    throw new IllegalStateException("$ne operator accepts only array of 2 expressions");
                }
                else
                {
                    List<ArgumentExpression> argExprs = getExpressionParams(opExprEntry);
                    neOpExpression.expressions = argExprs;
                }
                return Optional.of(neOpExpression);
            case "$gt":
                GTOperatorExpression gtOpExpression = new GTOperatorExpression();
                if (!opExprEntry.getValue().isArray() && opExprEntry.getValue().size() != 2)
                {
                    throw new IllegalStateException("$ne operator accepts only array of 2 expressions");
                }
                else
                {
                    List<ArgumentExpression> argExprs = getExpressionParams(opExprEntry);
                    gtOpExpression.expressions = argExprs;
                }
                return Optional.of(gtOpExpression);
            case "$gte":
                GTEOperatorExpression gteOpExpression = new GTEOperatorExpression();
                if (!opExprEntry.getValue().isArray() && opExprEntry.getValue().size() != 2)
                {
                    throw new IllegalStateException("$ne operator accepts only array of 2 expressions");
                }
                else
                {
                    List<ArgumentExpression> argExprs = getExpressionParams(opExprEntry);
                    gteOpExpression.expressions = argExprs;
                }
                return Optional.of(gteOpExpression);
            case "$lt":
                LTOperatorExpression ltOpExpression = new LTOperatorExpression();
                if (!opExprEntry.getValue().isArray() && opExprEntry.getValue().size() != 2)
                {
                    throw new IllegalStateException("$ne operator accepts only array of 2 expressions");
                }
                else
                {
                    List<ArgumentExpression> argExprs = getExpressionParams(opExprEntry);
                    ltOpExpression.expressions = argExprs;
                }
                return Optional.of(ltOpExpression);
            case "$lte":
                LTEOperatorExpression lteOpExpression = new LTEOperatorExpression();
                if (!opExprEntry.getValue().isArray() && opExprEntry.getValue().size() != 2)
                {
                    throw new IllegalStateException("$ne operator accepts only array of 2 expressions");
                }
                else
                {
                    List<ArgumentExpression> argExprs = getExpressionParams(opExprEntry);
                    lteOpExpression.expressions = argExprs;
                }
                return Optional.of(lteOpExpression);
            case "$in":
                InOperatorExpression inOpExpression = new InOperatorExpression();
                if (!opExprEntry.getValue().isArray() && opExprEntry.getValue().size() != 2)
                {
                    throw new IllegalStateException("$ne operator accepts only array of 2 expressions");
                }
                else
                {
                    List<ArgumentExpression> argExprs = getExpressionParams(opExprEntry);
                    inOpExpression.expressions = argExprs;
                }
                return Optional.of(inOpExpression);
            case "$nin":
                NinOperatorExpression ninOpExpression = new NinOperatorExpression();
                LiteralValue ninliteralValue = getLiteralValueFromEntry(opExprEntry);
                ninOpExpression.expressions = Lists.fixedSize.of(ninliteralValue);
                return Optional.of(ninOpExpression);
            case "$and":
                return Optional.of(getAndOperatorExpression(opExprEntry));
            case "$or":
                return Optional.of(getOrOperatorExpression(opExprEntry));
            case "$not":
                return Optional.of(getNotOperatorExpression(opExprEntry));
            default:
                throw new IllegalStateException("Operator not supported yet: " + opExprEntry.getKey());
        }
    }

    private List<ArgumentExpression> getExpressionParams(Map.Entry<String, JsonNode> opExprEntry)
    {
        List<ArgumentExpression> argExprs = Lists.mutable.of();
        for (JsonNode item : opExprEntry.getValue())
        {
            if (item.isValueNode())
            {
                if (item.isTextual())
                {
                    if (item.asText().startsWith("$"))
                    {
                        FieldPathExpression fpExpression = new FieldPathExpression();
                        fpExpression.fieldPath = item.asText();
                        argExprs.add(fpExpression);
                    }
                    else
                    {
                        LiteralValue stringLiteral = new LiteralValue();
                        StringTypeValue stringTypeValue = new StringTypeValue();
                        stringTypeValue.value = item.asText();
                        stringLiteral.value = stringTypeValue;
                        argExprs.add(stringLiteral);
                    }
                }
                else
                {
                    // This is a LiteralValue
                    argExprs.add(getLiteralValueFromNode(item));
                }
            }
            else if (item.isObject())
            {
                argExprs.add(getOperationExpression(item));
            }
            else if (item.isArray())
            {
                argExprs.add(getLiteralValueFromNode(item));
            }
        }
        return argExprs;
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

    private Stage getProjectStage(JsonNode projectNode)
    {
        ProjectStage projectStage = new ProjectStage();

        if (!projectNode.isObject())
        {
            throw new IllegalStateException("Expected Project node to be an object");
        }
        projectStage.projections = getProjectExpression(projectNode);

        return projectStage;

    }


}
