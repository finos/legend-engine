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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ArrayType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BaseType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BinaryType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BoolType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Collection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DateType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DecimalType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DoubleType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.IntType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.LongType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MaxKeyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MinKeyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoIndex;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ObjectIdType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ObjectType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.PropertyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Schema;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.SchemaValidationAction;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.SchemaValidationLevel;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.StringType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.TimeStampType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Validator;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.JsonSchemaExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MongoDBSchemaDeserializer extends StdDeserializer<MongoDatabase>
{
    public static final String BSON_TYPE = "bsonType";
    public static final String BSON_OBJECT = "object";
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MongoDBSchemaDeserializer.class);

    public MongoDBSchemaDeserializer()
    {
        this(null);
    }

    public MongoDBSchemaDeserializer(Class<?> vc)
    {
        super(vc);
    }

    // jsonParser is pointing to the open braces of the database objectnode. "database" : {  ....}
    @Override
    public MongoDatabase deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException
    {
        // Check the first token
        if (jsonParser.currentToken() == JsonToken.FIELD_NAME && jsonParser.getCurrentName().equals("database"))
        {
            if (jsonParser.nextToken() != JsonToken.START_OBJECT)
            {
                throw new IllegalStateException("Expected database value to be an object type");
            }
            return getMongoDatabase(jsonParser);
        }
        else
        {
            while (jsonParser.nextToken() != JsonToken.END_OBJECT)
            {
                if (jsonParser.currentToken() == JsonToken.FIELD_NAME && jsonParser.getCurrentName().equals("database"))
                {
                    if (jsonParser.nextToken() != JsonToken.START_OBJECT)
                    {
                        throw new IllegalStateException("Expected database value to be an object type");
                    }
                    return getMongoDatabase(jsonParser);
                }
                else
                {
                    LOGGER.debug("Skipping token in while parsing MongoDatabase type: {}", jsonParser.getText());
                }
            }
            throw new IllegalStateException("Expected database node to be an object while parsing MongoDatabase type");
        }
    }

    private MongoDatabase getMongoDatabase(JsonParser jsonParser) throws IOException
    {
        MongoDatabase db = new MongoDatabase();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT)
        {
            String propertyName = jsonParser.getCurrentName();
            jsonParser.nextToken();
            switch (propertyName)
            {
                case "databaseName":
                    db.name = jsonParser.getText();
                    break;
                case "collections":
                    db.collections = getCollections(jsonParser);
                    break;
                default:
                    LOGGER.trace("Ignoring unknown property: {}", propertyName);
            }
        }
        return db;
    }

    private List<Collection> getCollections(JsonParser jsonParser) throws IOException
    {
        // This node needs to be an array node
        if (jsonParser.currentToken() != JsonToken.START_ARRAY)
        {
            throw new IllegalStateException("Expected collection node to be an array");
        }
        List<Collection> mongoCollections = Lists.mutable.empty();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY)
        {
            Collection mongoCollection = getCollection(jsonParser);
            mongoCollections.add(mongoCollection);
        }
        return mongoCollections;
    }

    private Collection getCollection(JsonParser jsonParser) throws IOException
    {
        Collection mongoCollection = new Collection();
        if (jsonParser.currentToken() != JsonToken.START_OBJECT)
        {
            throw new IllegalStateException("Expected collection node to be an object");
        }
        while (jsonParser.nextToken() != JsonToken.END_OBJECT)
        {
            String propertyName = jsonParser.getCurrentName();
            jsonParser.nextToken();
            switch (propertyName)
            {
                case "collectionName":
                    mongoCollection.name = jsonParser.getText();
                    break;
                case "uuid":
                    mongoCollection.uuid = jsonParser.getText();
                    break;
                case "options":
                    mongoCollection.validator = getOptionsValidator(jsonParser);
                    // skip the options: close braces
                    break;
                case "indexes":
                    mongoCollection.indexes = getIndexes(jsonParser);
                    // skip the options: close braces
                    break;
                default:
                    LOGGER.trace("Ignoring unknown property: " + propertyName);
            }
        }
        return mongoCollection;
    }


    private Validator getOptionsValidator(JsonParser jsonParser) throws IOException
    {
        Validator mongoValidator = new Validator();
        if (jsonParser.currentToken() != JsonToken.START_OBJECT)
        {
            throw new IllegalStateException("Expected validator node to be an object");
        }
        while (jsonParser.nextToken() != JsonToken.END_OBJECT)
        {
            String propertyName = jsonParser.getCurrentName();
            jsonParser.nextToken();
            if (propertyName.equals("validator"))
            {
                mongoValidator = getValidator(jsonParser);
            }
            else
            {
                LOGGER.info("Expect only validator node within options, skipping {}", propertyName);
                jsonParser.skipChildren();
            }
        }
        return mongoValidator;
    }

    private Validator getValidator(JsonParser jsonParser) throws IOException
    {
        Validator validator = new Validator();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT)
        {
            String propertyName = jsonParser.getCurrentName();
            jsonParser.nextToken();
            switch (propertyName)
            {
                case "validationLevel":
                    validator.validationLevel = SchemaValidationLevel.valueOf(jsonParser.getText());
                    break;
                case "validationAction":
                    validator.validationAction = SchemaValidationAction.valueOf(jsonParser.getText());
                    break;
                case "$jsonSchema":
                    validator.validatorExpression = getSchemaExpression(jsonParser);
                    break;
                default:
                    LOGGER.debug("Skipping unknown element at validator node");
            }
        }
        return validator;
    }

    // This function is also used by legend engine/pure parser to convert the mongo-schema-string to schema object
    public JsonSchemaExpression getSchemaExpression(JsonParser jsonParser) throws IOException
    {
        JsonSchemaExpression schemaExpression = new JsonSchemaExpression();

        JsonNode schemaNode = jsonParser.getCodec().readTree(jsonParser);

        if (!schemaNode.isObject())
        {
            throw new IllegalStateException("Expected jsonSchema node to be an object");
        }
        schemaExpression.schemaExpression = getSchema(schemaNode);

        //jsonParser.skipChildren();
        return schemaExpression;
    }


    private Schema getSchema(JsonNode schemaNode) throws IOException
    {
        Schema schemaObject = new Schema();
        String type = schemaNode.get(BSON_TYPE).textValue();
        if (type != null && type.equals(BSON_OBJECT))
        {
            processObjectTypeFields(schemaNode, schemaObject);
        }
        else
        {
            throw new IllegalStateException("Expected jsonSchema bsonType should be of object type");
        }
        return schemaObject;
    }


    private void processObjectTypeFields(JsonNode schemaNode, ObjectType schemaObject) throws IOException
    {
        processBaseTypeFields(schemaNode, schemaObject);
        if (schemaNode.get("properties") != null && schemaNode.get("properties").isObject())
        {
            schemaObject.properties = processProperties(schemaNode.get("properties"));
        }
        if (schemaNode.get("maxProperties") != null)
        {
            schemaObject.maxProperties = schemaNode.get("maxProperties").longValue();
        }
        if (schemaNode.get("minProperties") != null)
        {
            schemaObject.minProperties = schemaNode.get("minProperties").longValue();
        }
        if (schemaNode.get("additionalProperties") != null)
        {
            if (schemaNode.get("additionalProperties").isBoolean())
            {
                schemaObject.additionalPropertiesAllowed = schemaNode.get("additionalProperties").booleanValue();
            }
            else if (schemaNode.get("additionalProperties").isObject())
            {
                schemaObject.additionalPropertiesAllowed = true;
                // else we expect a schema that needs to be comply
                ObjectType additionalPropertySchema = new ObjectTypeImpl();
                processObjectTypeFields(schemaNode.get("additionalProperties"), additionalPropertySchema);
                schemaObject.additionalProperties = additionalPropertySchema;
            }
        }
        if (schemaNode.get("required") != null && schemaNode.get("required").isArray())
        {
            List<String> reqProperties = Lists.mutable.empty();
            for (JsonNode item : schemaNode.get("required"))
            {
                reqProperties.add(item.textValue());
            }
            schemaObject.required = reqProperties;
        }
    }

    private void processBaseTypeFields(JsonNode schemaNode, BaseType schemaObject)
    {
        if (schemaNode.get("title") != null && schemaNode.get("title").isTextual())
        {
            schemaObject.title = schemaNode.get("title").textValue();
        }
        if (schemaNode.get("description") != null && schemaNode.get("description").isTextual())
        {
            schemaObject.description = schemaNode.get("description").textValue();
        }

    }

    private List<PropertyType> processProperties(JsonNode propertyNode) throws IOException
    {
        List<PropertyType> propertyTypes = Lists.mutable.empty();
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = propertyNode.fields();
        while (fieldsIterator.hasNext())
        {
            Map.Entry<String, JsonNode> entry = fieldsIterator.next();
            PropertyType propertyType = new PropertyType();
            propertyType.key = entry.getKey();
            propertyType.value = getBsonNodeType(entry.getValue());
            propertyTypes.add(propertyType);
        }
        return propertyTypes;
    }


    private BaseType getBsonNodeType(JsonNode objectNode) throws IOException
    {
        String type = objectNode.get("bsonType").textValue();
        if (type != null)
        {
            switch (type)
            {
                // ordered by mongo type number
                case "double":
                    DoubleType dblType = new DoubleType();
                    processDoubleTypeFields(objectNode, dblType);
                    return dblType;
                case "string":
                    StringType strType = new StringType();
                    processStringTypeFields(objectNode, strType);
                    return strType;
                case "object":
                    ObjectType objType = new ObjectType();
                    processObjectTypeFields(objectNode, objType);
                    return objType;
                case "array":
                    ArrayType arrType = new ArrayType();
                    processArrayTypeFields(objectNode, arrType);
                    return arrType;
                case "binData":
                    BinaryType binType = new BinaryType();
                    processBaseTypeFields(objectNode, binType);
                    return binType;
                case "objectId":
                    ObjectIdType objIdType = new ObjectIdType();
                    processBaseTypeFields(objectNode, objIdType);
                    return objIdType;
                case "bool":
                    BoolType boolType = new BoolType();
                    processBaseTypeFields(objectNode, boolType);
                    return boolType;
                case "date":
                    DateType dtType = new DateType();
                    processBaseTypeFields(objectNode, dtType);
                    return dtType;
                case "null":
                    LOGGER.info("Null Type not supported");
                    break;
                case "regex":
                    LOGGER.info("Regex Type not supported");
                    break;
                case "int":
                    IntType intType = new IntType();
                    processIntTypeFields(objectNode, intType);
                    return intType;
                case "timestamp":
                    TimeStampType tsType = new TimeStampType();
                    processBaseTypeFields(objectNode, tsType);
                    return tsType;
                case "long":
                    LongType longType = new LongType();
                    processLongTypeFields(objectNode, longType);
                    return longType;
                case "decimal":
                    DecimalType decimalType = new DecimalType();
                    processDecimalTypeFields(objectNode, decimalType);
                    return decimalType;
                case "minKey":
                    MinKeyType minKeyType = new MinKeyType();
                    processBaseTypeFields(objectNode, minKeyType);
                    return minKeyType;
                case "maxKey":
                    MaxKeyType maxKeyType = new MaxKeyType();
                    processBaseTypeFields(objectNode, maxKeyType);
                    return maxKeyType;
                default:
                    LOGGER.info("Bsontype not supported: {}", type);
            }
        }
        throw new IllegalStateException("Type not supported in jsonSchema model yet: " + type);

    }


    private void processDecimalTypeFields(JsonNode objectNode, DecimalType decimalType)
    {
//                   if(objectNode.get("exclusiveMaximum")!=null)
//                    {
//                    }
//                    if(objectNode.get("exclusiveMinimum")!=null)
//                    {
//                    }
        processBaseTypeFields(objectNode, decimalType);
        if (objectNode.get("maximum") != null)
        {
            decimalType.maximum = objectNode.get("maximum").decimalValue();
        }
        if (objectNode.get("minimum") != null)
        {
            decimalType.minimum = objectNode.get("minimum").decimalValue();
        }
    }

    private void processLongTypeFields(JsonNode objectNode, LongType longType)
    {
//                   if(objectNode.get("exclusiveMaximum")!=null)
//                    {
//                    }
//                    if(objectNode.get("exclusiveMinimum")!=null)
//                    {
//                    }
        processBaseTypeFields(objectNode, longType);
        if (objectNode.get("maximum") != null)
        {
            longType.maximum = objectNode.get("maximum").longValue();
        }
        if (objectNode.get("minimum") != null)
        {
            longType.minimum = objectNode.get("minimum").longValue();
        }
    }

    private void processIntTypeFields(JsonNode objectNode, IntType intType)
    {
//                   if(objectNode.get("exclusiveMaximum")!=null)
//                    {
//                    }
//                    if(objectNode.get("exclusiveMinimum")!=null)
//                    {
//                    }
        processBaseTypeFields(objectNode, intType);
        if (objectNode.get("maximum") != null)
        {
            intType.maximum = objectNode.get("maximum").longValue();
        }
        if (objectNode.get("minimum") != null)
        {
            intType.minimum = objectNode.get("minimum").longValue();
        }
    }

    private void processDoubleTypeFields(JsonNode objectNode, DoubleType dblType)
    {
        processBaseTypeFields(objectNode, dblType);
        if (objectNode.get("maximum") != null)
        {
            dblType.maximum = objectNode.get("maximum").doubleValue();
        }
        if (objectNode.get("minimum") != null)
        {
            dblType.minimum = objectNode.get("minimum").doubleValue();
        }
    }

    private void processStringTypeFields(JsonNode objectNode, StringType strType)
    {
        processBaseTypeFields(objectNode, strType);
        if (objectNode.get("maxLength") != null)
        {
            strType.maxLength = objectNode.get("maxLength").longValue();
        }
        if (objectNode.get("minLength") != null)
        {
            strType.minLength = objectNode.get("minLength").longValue();
        }
    }

    private void processArrayTypeFields(JsonNode objectNode, ArrayType arrType) throws IOException
    {
        processBaseTypeFields(objectNode, arrType);
        if (objectNode.get("maxItems") != null)
        {
            arrType.maxItems = objectNode.get("maxItems").longValue();
        }
        if (objectNode.get("minItems") != null)
        {
            arrType.minItems = objectNode.get("minItems").longValue();
        }
        if (objectNode.get("uniqueItems") != null)
        {
            arrType.uniqueItems = objectNode.get("uniqueItems").booleanValue();
        }
        if (objectNode.get("items") != null)
        {
            if (objectNode.get("items").isArray())
            {
                arrType.items = Lists.mutable.of();
                for (JsonNode arrItem : objectNode.get("items"))
                {
                    arrType.items.add(getBsonNodeType(arrItem));
                }
            }
            else
            {
                arrType.items = Lists.mutable.of(getBsonNodeType(objectNode.get("items")));
            }
        }


    }

    private MongoIndex getIndexes(JsonParser jsonParser) throws IOException
    {
        MongoIndex index = new MongoIndex();
        index.name = "_id_";
        jsonParser.skipChildren();
        return index;
    }
}
