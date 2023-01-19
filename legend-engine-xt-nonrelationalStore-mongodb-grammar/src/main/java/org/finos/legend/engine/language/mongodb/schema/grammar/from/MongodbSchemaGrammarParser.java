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
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BaseTypeVisitor;
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
        return visitDocument(parser.json());
    }

    private MongoDatabase visitDocument(MongodbSchemaParser.JsonContext json)
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
//                    int line = dbKey.getStart().getLine();
//                    int colStart = dbKey.getStart().getStartIndex() + 1;
//                    int colEnt = dbKey.getStart().getStopIndex() + 1;
//                    SourceInformation sourceInformation = new SourceInformation(
//                            "",
//                            line,
//                            1,
//                            line,
//                            1);
//                    throw new MongodbSchemaParserException("Top level node should have key = database", sourceInformation);

                    LOGGER.debug("Skipping keys we don't care: " + pairContext.getText());
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
                LOGGER.debug("Skipping key from top level context in pass 1: " + pair.key().getText() + " at line " + pair.start.getLine());
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
                LOGGER.debug("Skipping key from top level context in pass 2: " + pair.key().getText() + " at line " + pair.start.getLine());
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
            Iterator<MongodbSchemaParser.PairContext> iter = collectionContext.obj().pair().iterator();
            while (iter.hasNext())
            {
                pair = iter.next();

                if (pair.key() != null && pair.key().keywords() != null)
                {
                    if (pair.key().keywords().COLLECTION_NAME() != null)
                    {
                        col.name = pair.value().STRING().getText();
                    }
                    else if (pair.key().keywords().OPTIONS() != null)
                    {
                        MongodbSchemaParser.ValueContext options = pair.value();
                        Iterator<MongodbSchemaParser.PairContext> optionsIter = options.obj().pair().iterator();
                        while (optionsIter.hasNext())
                        {
                            MongodbSchemaParser.PairContext optionsPair = optionsIter.next();
                            if (optionsPair.key().keywords().VALIDATOR() != null)
                            {
                                MongodbSchemaParser.ValueContext validator = optionsPair.value();
                                Iterator<MongodbSchemaParser.PairContext> validatorIter = validator.obj().pair().iterator();
                                while (validatorIter.hasNext())
                                {
                                    MongodbSchemaParser.PairContext validatorPair = validatorIter.next();
                                    if (validatorPair.key().keywords().REF() != null)
                                    {
                                        String schemaRef = validatorPair.value().STRING().getText();
                                        // Check if we have schemas process - if so - look up in the Map and set.
                                        if (schemas.get(schemaRef) != null)
                                        {
                                            col.schema = schemas.get(schemaRef);
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
                                LOGGER.debug("Skipping key from collections/options object: " + optionsPair.getText());
                            }
                        }
                    }
                    else
                    {
                        LOGGER.debug("Skipping key (KEYWORD) from database value object: " + pair.getText());
                    }
                }
                else
                {
                    LOGGER.debug("Skipping key (STRING) from database value object: " + pair.getText());
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
            return value.arr().value()
                    .stream()
                    .map(this::visitSchema)
                    .collect(Collectors.toMap(s -> s.id, Function.identity()));
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
        Iterator<MongodbSchemaParser.PairContext> schemaIter = schemaContext.obj().pair().iterator();
        Schema schema = new Schema();
        while (schemaIter.hasNext())
        {
            MongodbSchemaParser.PairContext schemaPair = schemaIter.next();

            if (schemaPair.key().keywords() != null)
            {
                String key = schemaPair.key().keywords().getText();
                switch (key)
                {
                    case "\"properties\"":
                        schema.properties = visitProperties(schemaPair.value());
                        break;
                    case "\"$schema\"":
                        // Check schemaPair.value().STRING() is not null
                        schema.schemaVersion = schemaPair.value().getText();
                        break;
                    case "\"$id\"":
                        // Check schemaPair.value().STRING() is not null
                        schema.id = schemaPair.value().getText();
                        break;
                    case "\"title\"":
                        // Check schemaPair.value().STRING() is not null
                        schema.title = schemaPair.value().getText();
                        break;
                    case "\"description\"":
                        // Check schemaPair.value().STRING() is not null
                        schema.description = schemaPair.value().getText();
                        break;
                    case "\"required\"":
                        schema.required = visitRequiredFields(schemaPair.value().arr());
                        break;
                    case "\"allOf\"":
                        break;
                    case "\"anyOf\"":
                        break;
                    case "\"bsonType\"":
                    case "\"type\"":
                        schema.bsonType = createTypeReferenceFromType(schemaPair.value().getText(), schemaPair.value().getStart().getLine());
                        break;

                }
            }
        }
        return schema;

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
            return visitSchema(value);
        }
        else
        {
            LOGGER.debug("Property type node should be object type value, but found: " + value.getText());

        }
        return null;
    }

    private BaseType createTypeReferenceFromType(String type, int lineNumber)
    {
        switch (type)
        {
            case "string":
            {
                return new StringType();
            }
            case "number":
            case "long":
            {
                return new LongType();
            }
            case "bool":
            {
                return new BoolType();
            }
            case "int":
            {
                return new IntType();
            }
            case "double":
            {
                return new DoubleType();
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
                return new DecimalType();
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
                return new ArrayType();
            }
            case "object":
            {
                return new ObjectType()
                {
                    @Override
                    public <T> T accept(BaseTypeVisitor<T> visitor)
                    {
                        return (visitor.visit(this));
                    }
                };
            }
            default:
            {
                SourceInformation sourceInformation = new SourceInformation("", lineNumber, 1, lineNumber, 1);
                throw new MongodbSchemaParserException("Un-supported data type: " + type, sourceInformation);
            }
            // Skipping Timestamp
        }
    }

    private List<String> visitRequiredFields(MongodbSchemaParser.ArrContext arr)
    {
        return arr.value().stream().map(t -> t.STRING().getText()).collect(Collectors.toList());
    }

    private SchemaType getPropertySchemaType(MongodbSchemaParser.ObjContext objContext)
    {

        Iterator<MongodbSchemaParser.PairContext> propTypeIter = objContext.pair().iterator();
        while (propTypeIter.hasNext())
        {
            MongodbSchemaParser.PairContext propType = propTypeIter.next();
            if (propType.key().keywords().TYPE() != null || propType.key().keywords().BSONTYPE() != null)
            {
                if ("object".equals(propType.value().getText()))
                {
                    return SchemaType.OBJECT;
                }
                else if ("array".equals(propType.value().getText()))
                {
                    return SchemaType.ARRAY;
                }
                else
                {
                    return SchemaType.PRIMITIVE;
                }
            }
            else
            {
                LOGGER.debug("Skipping key (during SchemaType Identification): " + propType.key().getText());
            }
        }
        int lineNumber = objContext.getStart().getLine();
        SourceInformation sourceInformation = new SourceInformation("", lineNumber, 1, lineNumber, 1);
        throw new MongodbSchemaParserException("Un-supported data type", sourceInformation);
    }

    private enum SchemaType
    {
        PRIMITIVE, ARRAY, OBJECT;
    }

}
