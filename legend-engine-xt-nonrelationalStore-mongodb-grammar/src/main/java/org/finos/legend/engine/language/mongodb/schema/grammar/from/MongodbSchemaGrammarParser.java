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

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongodbSchemaParser;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ArrayType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BaseType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BinaryType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BoolType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Collection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DecimalType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DoubleType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.IntType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.LongType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MaxKeyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MinKeyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoDatabase;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ObjectIdType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ObjectType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.PropertyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Schema;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.SchemaValidationAction;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.SchemaValidationLevel;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.StringType;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MongodbSchemaGrammarParser
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private boolean schemasProcessed = false;

    private MongodbSchemaGrammarParser()
    {
    }

    public static MongodbSchemaGrammarParser newInstance()
    {
        return new MongodbSchemaGrammarParser();
    }

    private static void visitBaseTypeAttributes(List<MongodbSchemaParser.PairContext> pair, BaseType bType)
    {
        for (MongodbSchemaParser.PairContext pairContext : pair)
        {
            if (pairContext.key().keywords().DESCRIPTION() != null)
            {
                bType.description = pairContext.value().getText();
            }
            else if (pairContext.key().keywords().TITLE() != null)
            {
                bType.title = pairContext.value().getText();
            }
        }
    }

    private static void visitOptionsNode(Map<String, Schema> schemas, Collection col, MongodbSchemaParser.PairContext pairContext)
    {
        MongodbSchemaParser.ValueContext options = pairContext.value();
        for (MongodbSchemaParser.PairContext optionsPair : options.obj().pair())
        {
            if (optionsPair.key().keywords().VALIDATOR() != null)
            {
                MongodbSchemaParser.ValueContext validator = optionsPair.value();
                for (MongodbSchemaParser.PairContext validatorPair : validator.obj().pair())
                {
                    if (validatorPair.key().keywords().REF() != null)
                    {
                        String schemaRef = validatorPair.value().STRING().getText();
                        // Check if we have schemas process - if so - look up in the Map and set.
                        if (schemas.get(schemaRef) != null)
                        {
                            col.schema = schemas.get(schemaRef);
                            break;
                        }
                        else
                        {
                            int line = validatorPair.key().getStart().getLine();
                            SourceInformation sourceInformation = new SourceInformation("", line, 1, line, 1);
                            throw new MongodbSchemaParserException("SchemaReference not found: " + schemaRef, sourceInformation);
                        }
                    }
                }
            }
            else
            {
                LOGGER.trace("Skipping key from collections/options object: " + optionsPair.getText());
            }
        }
        for (MongodbSchemaParser.PairContext optionsPair : options.obj().pair())
        {
            if (optionsPair.key().keywords().VALIDATION_LEVEL() != null)
            {
                col.schema.validationLevel = SchemaValidationLevel.valueOf(optionsPair.value().STRING().getText());
            }
            else if (optionsPair.key().keywords().VALIDATION_ACTION() != null)
            {
                col.schema.validationAction = SchemaValidationAction.valueOf(optionsPair.value().STRING().getText());
            }
        }
    }

    public MongoDatabase parseDocument(String code)
    {
        return this.parse(code);
    }

    private MongoDatabase parse(String code)
    {
        ANTLRErrorListener errorListener = new BaseErrorListener()
        {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
            {
                if (e != null && e.getOffendingToken() != null && e instanceof InputMismatchException)
                {
                    msg = "Unexpected token";
                }
                else if (e == null || e.getOffendingToken() == null)
                {
                    if (e == null && offendingSymbol instanceof Token && (msg.startsWith("extraneous input") || msg.startsWith("missing ")))
                    {
                        // when ANTLR detects unwanted symbol, it will not result in an error, but throw
                        // `null` with a message like "extraneous input ... expecting ..."
                        // NOTE: this is caused by us having INVALID catch-all symbol in the lexer
                        // so anytime, INVALID token is found, it should cause this error
                        // but because it is a catch-all rule, it only produces a lexer token, which is a symbol
                        // we have to construct the source information manually
                        SourceInformation sourceInformation = new SourceInformation("", line, charPositionInLine + 1, line, charPositionInLine + 1 + ((Token) offendingSymbol).getStopIndex() - ((Token) offendingSymbol).getStartIndex());
                        // NOTE: for some reason sometimes ANTLR report the end index of the token to be smaller than the start index so we must reprocess it here
                        sourceInformation.startColumn = Math.min(sourceInformation.endColumn, sourceInformation.startColumn);
                        msg = "Unexpected token";
                        throw new MongodbSchemaParserException(msg, sourceInformation);
                    }
                    SourceInformation sourceInformation = new SourceInformation("", line, charPositionInLine + 1, line, charPositionInLine + 1);
                    throw new MongodbSchemaParserException(msg, sourceInformation);
                }
                Token offendingToken = e.getOffendingToken();
                SourceInformation sourceInformation = new SourceInformation("", line, charPositionInLine + 1, offendingToken.getLine(), charPositionInLine + offendingToken.getText().length());
                throw new MongodbSchemaParserException(msg, sourceInformation);
            }

            @Override
            public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean b, BitSet bitSet, ATNConfigSet atnConfigSet)
            {
            }

            @Override
            public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitSet, ATNConfigSet atnConfigSet)
            {
            }

            @Override
            public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet)
            {
            }
        };
        org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongodbSchemaLexer lexer = new org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongodbSchemaLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongodbSchemaParser parser = new org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongodbSchemaParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return visitDatabase(parser.json());
    }

    private MongoDatabase visitDatabase(MongodbSchemaParser.JsonContext json)
    {
        MongoDatabase database = new MongoDatabase();
        MongodbSchemaParser.ValueContext top_element = json.value();
        if (top_element.obj() != null)
        {
            // Top element Should be an object - corresponding to database
            MongodbSchemaParser.ObjContext rootObj = top_element.obj();
            Iterator<MongodbSchemaParser.PairContext> iter = rootObj.pair().iterator();
            while (iter.hasNext())
            {
                MongodbSchemaParser.PairContext pairContext = iter.next();
                if (pairContext.key().keywords() != null && pairContext.key().keywords().DATABASE() != null)
                {
                    return visitDatabase(pairContext.value().obj());
                    // throw exception if obj() doesn't exist
                }
                else
                {
                    LOGGER.trace("Skipping keys we don't care: " + pairContext.getText());
                }
            }
        }
        int line = json.getStart().getLine();
        int colStart = json.getStart().getStartIndex() + 1;
        int colEnt = json.getStart().getStopIndex() + 1;
        SourceInformation sourceInformation = new SourceInformation("", line, 1, line, 1);
        throw new MongodbSchemaParserException("Top node not object type", sourceInformation);

    }

    private MongoDatabase visitDatabase(MongodbSchemaParser.ObjContext dbObject)
    {
        Iterator<MongodbSchemaParser.PairContext> iterPass1 = dbObject.pair().iterator();
        MongoDatabase db = new MongoDatabase();
        Map<String, Schema> schemas = Maps.mutable.empty();
        // pass 1 - skip processing collections as we want to process the schemas first.
        while (iterPass1.hasNext())
        {
            MongodbSchemaParser.PairContext pair = iterPass1.next();
            if (pair.key().keywords() != null && pair.key().keywords().SCHEMAS() != null && !schemasProcessed)
            {
                schemas = visitSchemas(pair.value());
                schemasProcessed = true;
            }
            else
            {
                //regular String - can be skipped at this level
                LOGGER.trace("Skipping key from top level context in pass 1: " + pair.key().getText() + " at line " + pair.start.getLine());
            }
        }

        // pass 2 = process all else - other than schemas
        Iterator<MongodbSchemaParser.PairContext> iterPass2 = dbObject.pair().iterator();
        while (iterPass2.hasNext())
        {
            MongodbSchemaParser.PairContext pair = iterPass2.next();
            if (pair.key().keywords() != null && (pair.key().keywords().DATABASE_NAME() != null))
            {
                db.name = pair.value().STRING().getText();
            }
            else if (pair.key().keywords() != null && pair.key().keywords().COLLECTIONS() != null && schemasProcessed)
            {
                db.collections = visitCollections(pair.value(), schemas);
            }
            else
            {
                //regular String - can be skipped at this level
                LOGGER.trace("Skipping key from top level context in pass 2: " + pair.key().getText() + " at line " + pair.start.getLine());
            }
        }

        return db;
    }

    private List<Collection> visitCollections(MongodbSchemaParser.ValueContext value, Map<String, Schema> schemas)
    {
        if (value.arr() != null)
        {
            return ListIterate.collect(value.arr().value(), item -> visitCollection(item, schemas));
        }
        else
        {
            LOGGER.debug("Collections value needs to be an array at line " + value.start.getLine());
        }
        return FastList.newList();
    }

    private Collection visitCollection(MongodbSchemaParser.ValueContext collectionContext, Map<String, Schema> schemas)
    {
        MongodbSchemaParser.PairContext pair;
        if (collectionContext.obj() != null)
        {
            Collection col = new Collection();
            for (MongodbSchemaParser.PairContext pairContext : collectionContext.obj().pair())
            {
                if (pairContext.key() != null && pairContext.key().keywords() != null)
                {
                    if (pairContext.key().keywords().COLLECTION_NAME() != null)
                    {
                        col.name = pairContext.value().STRING().getText();
                    }
                    else if (pairContext.key().keywords().OPTIONS() != null)
                    {
                        visitOptionsNode(schemas, col, pairContext);
                    }
                    else
                    {
                        LOGGER.trace("Skipping key (KEYWORD) from database value object: " + pairContext.getText());
                    }
                }
                else
                {
                    LOGGER.trace("Skipping key (STRING) from database value object: " + pairContext.getText());
                }
            }
            return col;
        }
        else
        {
            int line = collectionContext.getStart().getLine();
            SourceInformation sourceInformation = new SourceInformation("", line, 1, line, 1);
            throw new MongodbSchemaParserException("Collection node is not an Object type, but found: " + collectionContext.getText(), sourceInformation);
        }
    }

    private Map<String, Schema> visitSchemas(MongodbSchemaParser.ValueContext value)
    {
        if (value.arr() != null)
        {
            return value.arr().value().stream().map(this::visitSchema).collect(Collectors.toMap(s -> s.id, Function.identity()));
        }
        else
        {
            LOGGER.debug("Collections value needs to be an array at line " + value.start.getLine());
        }
        return Maps.mutable.empty();
    }

    /**
     * @param schemaContext {
     *                      "id": "base-uri",
     *                      "title": "Record of employee",
     *                      "description": "This document records the details of an employee",
     *                      "type": "object",
     *                      "properties": {
     *                      "id": {
     *                      "description": "A unique identifier for an employee",
     *                      "type": "number"
     *                      },...
     */
    private Schema visitSchema(MongodbSchemaParser.ValueContext schemaContext)
    {
        List<MongodbSchemaParser.PairContext> schemaPair = schemaContext.obj().pair();
        Schema schema = new Schema();
        visitSchemaProperties(schema, schemaPair);
        visitObjectProperties(schema, schemaPair);
        return schema;

    }

    private void visitSchemaProperties(Schema schema, List<MongodbSchemaParser.PairContext> pairContexts)
    {
        Iterator<MongodbSchemaParser.PairContext> schemaIter = pairContexts.iterator();
        while (schemaIter.hasNext())
        {
            MongodbSchemaParser.PairContext schemaPair = schemaIter.next();
            if (schemaPair.key().keywords() != null)
            {
                String key = schemaPair.key().keywords().getText();
                switch (key)
                {
                    case "\"$schema\"":
                        // Check schemaPair.value().STRING() is not null
                        schema.schemaVersion = schemaPair.value().getText();
                        break;
                    case "\"$id\"":
                        // Check schemaPair.value().STRING() is not null
                        schema.id = schemaPair.value().getText();
                        break;
                    default:
                        LOGGER.debug("Skipping key from object: " + key);
                }
            }
        }
    }

    private void visitObjectProperties(ObjectType objType, List<MongodbSchemaParser.PairContext> pairContexts)
    {
        Iterator<MongodbSchemaParser.PairContext> pairIter = pairContexts.iterator();
        while (pairIter.hasNext())
        {
            MongodbSchemaParser.PairContext objPair = pairIter.next();
            objType.additionalPropertiesAllowed = true; // this is default, else if the user doesn't set it to true, java will default it to false
            if (objPair.key().keywords() != null)
            {
                String key = objPair.key().keywords().getText();
                switch (key)
                {
                    case "\"properties\"":
                        objType.properties = visitProperties(objPair.value());
                        break;
                    case "\"title\"":
                        // Check schemaPair.value().STRING() is not null
                        objType.title = objPair.value().getText();
                        break;
                    case "\"description\"":
                        // Check schemaPair.value().STRING() is not null
                        objType.description = objPair.value().getText();
                        break;
                    case "\"maxProperties\"":
                        // Check schemaPair.value().STRING() is not null
                        objType.maxProperties = Long.parseLong(objPair.value().NUMBER().getText());
                        break;
                    case "\"minProperties\"":
                        // Check schemaPair.value().STRING() is not null
                        objType.minProperties = Long.parseLong(objPair.value().NUMBER().getText());
                        break;
                    case "\"required\"":
                        objType.required = visitRequiredFields(objPair.value().arr());
                        break;
                    case "\"allOf\"":
                        break;
                    case "\"anyOf\"":
                        break;
                    case "\"additionalProperties\"":
                        if (objPair.value().obj() != null)
                        {
                            // additional Properties is a schema - so handle appropriately
                            objType.additionalPropertiesAllowed = true;
                            ObjectType addProperties = new ObjectTypeImpl();
                            visitObjectProperties(addProperties, objPair.value().obj().pair());
                            objType.additionalProperties = addProperties;
                        }
                        else if (objPair.value().FALSE() != null || objPair.value().TRUE() != null)
                        {
                            objType.additionalPropertiesAllowed = objPair.value().TRUE() != null ? true : false;
                        }
                        // else objPair.value().TRUE()  & we have set the value = true as default at the top
                        break;
                    default:
                        LOGGER.trace("Skipping key from object: " + key);
                }
            }
        }
    }

    private List<PropertyType> visitProperties(MongodbSchemaParser.ValueContext value)
    {
        if (value.obj() != null)
        {
            Iterator<MongodbSchemaParser.PairContext> propertiesIter = value.obj().pair().iterator();
            List<PropertyType> propertyTypes = FastList.newList();
            while (propertiesIter.hasNext())
            {
                MongodbSchemaParser.PairContext propPair = propertiesIter.next();
                if (propPair.key().STRING() != null)
                {
                    PropertyType propType = new PropertyType();
                    propType.key = propPair.key().STRING().getText();
                    propType.value = visitPropertyType(propPair.value());
                    propertyTypes.add(propType);
                }
            }
            return propertyTypes;
        }
        else
        {
            LOGGER.debug("Properties node should be object type value, but found: " + value.getText());
        }
        return Lists.mutable.empty();
    }

    /**
     * @param value handles the value part here
     *              simple property "name"
     *              "name" : {
     *              "description": "name of the employee",
     *              "type": "string",
     *              "minLength": {
     *              "$numberInt": "2"
     *              }
     *              or  nested property "hobbies"
     *              "hobbies": {
     *              "description": "hobbies of the employee",
     *              "type": "object",
     *              "properties": {
     *              "indoor": {
     *              "items": {
     *              "description": "List of hobbies",
     *              "type": "string"
     *              },
     *              "minItems": 1,
     *              "uniqueItems": true,
     *              "type": "array"
     *              },
     *              "outdoor": {
     *              "items": {
     *              "description": "List of hobbies",
     *              "type": "string"
     *              },
     *              "minItems": 1,
     *              "uniqueItems": true,
     *              "type": "array"
     *              }
     *              },
     *              "required": [
     *              "indoor",
     *              "outdoor"
     *              ]
     *              }
     * @return
     */
    private BaseType visitPropertyType(MongodbSchemaParser.ValueContext value)
    {
        if (value.obj() != null)
        {
            BaseType bType = getPropertySchemaType(value.obj());
            return bType;
        }
        else
        {
            LOGGER.debug("Property type node should be object type value, but found: " + value.getText());

        }
        return null;
    }

    private BaseType createTypeReferenceFromType(String type, int lineNumber, List<MongodbSchemaParser.PairContext> pair)
    {
        switch (type)
        {
            case "string":
            {
                StringType stringType = new StringType();
                visitBaseTypeAttributes(pair, stringType);
                for (MongodbSchemaParser.PairContext pairContext : pair)
                {
                    if (pairContext.key().keywords().MIN_LENGTH() != null)
                    {
                        stringType.minLength = Long.parseLong(pairContext.value().getText());
                    }
                    else if (pairContext.key().keywords().MAX_LENGTH() != null)
                    {
                        stringType.maxLength = Long.parseLong(pairContext.value().getText());
                    }
                }

                return stringType;
            }
            case "number":
            case "long":
            {
                LongType longType = new LongType();
                visitBaseTypeAttributes(pair, longType);
                for (MongodbSchemaParser.PairContext pairContext : pair)
                {
                    if (pairContext.key().keywords().MINIMUM() != null)
                    {
                        longType.minimum = Long.parseLong(pairContext.value().getText());
                    }
                    else if (pairContext.key().keywords().MAX_LENGTH() != null)
                    {
                        longType.maximum = Long.parseLong(pairContext.value().getText());
                    }
                }
                return longType;
            }
            case "bool":
            {
                BoolType boolType = new BoolType();
                visitBaseTypeAttributes(pair, boolType);
                return boolType;
            }
            case "int":
            {
                IntType intType = new IntType();
                visitBaseTypeAttributes(pair, intType);
                for (MongodbSchemaParser.PairContext pairContext : pair)
                {
                    if (pairContext.key().keywords().MINIMUM() != null)
                    {
                        intType.minimum = Long.parseLong(pairContext.value().getText());
                    }
                    else if (pairContext.key().keywords().MAX_LENGTH() != null)
                    {
                        intType.maximum = Long.parseLong(pairContext.value().getText());
                    }
                }
                return intType;
            }
            case "double":
            {
                DoubleType doubleType = new DoubleType();
                visitBaseTypeAttributes(pair, doubleType);
                return doubleType;
            }
            case "objectId":
            {
                return new ObjectIdType();
            }
            case "binData":
            {
                return new BinaryType();
            }
            case "decimal":
            {
                DecimalType decimalType = new DecimalType();
                visitBaseTypeAttributes(pair, decimalType);
                return decimalType;
            }
            case "minKey":
            {
                return new MinKeyType();
            }
            case "maxKey":
            {
                return new MaxKeyType();
            }
            case "array":
            {
                ArrayType arrayType = new ArrayType();
                visitBaseTypeAttributes(pair, arrayType);
                visitArrayTypeAttributes(pair, arrayType);
                return arrayType;
            }
            case "object":
            {
                ObjectType objType = new ObjectTypeImpl();
                visitObjectProperties(objType, pair);
                return objType;
            }
            default:
            {
                SourceInformation sourceInformation = new SourceInformation("", lineNumber, 1, lineNumber, 1);
                throw new MongodbSchemaParserException("Un-supported data type: " + type, sourceInformation);
            }
            // Skipping Timestamp
        }

    }

    private void visitArrayTypeAttributes(List<MongodbSchemaParser.PairContext> pair, ArrayType arrayType)
    {

        for (MongodbSchemaParser.PairContext pairContext : pair)
        {
            if (pairContext.key().keywords().MAX_ITEMS() != null)
            {
                arrayType.maxItems = Long.parseLong(pairContext.value().NUMBER().getText());
            }
            else if (pairContext.key().keywords().MIN_ITEMS() != null)
            {
                arrayType.minItems = Long.parseLong(pairContext.value().NUMBER().getText());
            }
            else if (pairContext.key().keywords().UNIQUE_ITEMS() != null)
            {
                arrayType.uniqueItems = pairContext.value().TRUE() != null;
            }
            else if (pairContext.key().keywords().ITEMS() != null)
            {
                if (pairContext.value().obj() != null)
                {
                    BaseType bType = visitPropertyType(pairContext.value());
                    arrayType.items = FastList.newListWith(bType);
                }
                else if (pairContext.value().arr() != null)
                {
                    arrayType.items = pairContext.value().arr().value().stream().map(this::visitPropertyType).collect(Collectors.toList());
                }
            }
        }

    }

    private List<String> visitRequiredFields(MongodbSchemaParser.ArrContext arr)
    {
        return arr.value().stream().map(t -> t.STRING().getText()).collect(Collectors.toList());
    }

    private BaseType getPropertySchemaType(MongodbSchemaParser.ObjContext objContext)
    {

        Iterator<MongodbSchemaParser.PairContext> propTypeIter = objContext.pair().iterator();
        int lineNumber = objContext.getStart().getLine();
        for (MongodbSchemaParser.PairContext propType : objContext.pair())
        {
            if (propType.key().keywords().TYPE() != null || propType.key().keywords().BSONTYPE() != null)
            {
                return createTypeReferenceFromType(propType.value().getText(), lineNumber, objContext.pair());
            }
            else
            {
                LOGGER.trace("Skipping key (during SchemaType Identification): " + propType.key().getText());
            }
        }
        SourceInformation sourceInformation = new SourceInformation("", lineNumber, 1, lineNumber, 1);
        throw new MongodbSchemaParserException("Un-supported data type", sourceInformation);
    }


}
