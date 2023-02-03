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
import org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongoDBSchemaParser;
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

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MongoDBSchemaParseTreeWalker
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private boolean schemasProcessed = false;

    private MongoDBSchemaParseTreeWalker()
    {
    }

    public static MongoDBSchemaParseTreeWalker newInstance()
    {
        return new MongoDBSchemaParseTreeWalker();
    }

    private static void visitBaseTypeAttributes(List<MongoDBSchemaParser.PairContext> pair, BaseType bType)
    {
        for (MongoDBSchemaParser.PairContext pairContext : pair)
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

    private static void visitOptionsNode(Map<String, Schema> schemas, Collection col, MongoDBSchemaParser.PairContext pairContext)
    {
        MongoDBSchemaParser.ValueContext options = pairContext.value();
        for (MongoDBSchemaParser.PairContext optionsPair : options.obj().pair())
        {
            if (optionsPair.key().keywords().VALIDATOR() != null)
            {
                MongoDBSchemaParser.ValueContext validator = optionsPair.value();
                for (MongoDBSchemaParser.PairContext validatorPair : validator.obj().pair())
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
                            throw new MongoDBSchemaParserException("SchemaReference not found: " + schemaRef, sourceInformation);
                        }
                    }
                }
            }
            else
            {
                LOGGER.trace("Skipping key from collections/options object: " + optionsPair.getText());
            }
        }
        for (MongoDBSchemaParser.PairContext optionsPair : options.obj().pair())
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

    private static MongoDBSchemaParserException raiseException(int line, int startColumn, String errMessage)
    {
        SourceInformation sourceInformation = new SourceInformation("", line, startColumn, line, startColumn);
        return new MongoDBSchemaParserException(errMessage, sourceInformation);
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
                        // NOTE: for some reason sometimes ANTLR report the end index of the token to be smaller than the start index, so we must reprocess it here
                        sourceInformation.startColumn = Math.min(sourceInformation.endColumn, sourceInformation.startColumn);
                        msg = "Unexpected token";
                        throw new MongoDBSchemaParserException(msg, sourceInformation);
                    }
                    SourceInformation sourceInformation = new SourceInformation("", line, charPositionInLine + 1, line, charPositionInLine + 1);
                    throw new MongoDBSchemaParserException(msg, sourceInformation);
                }
                Token offendingToken = e.getOffendingToken();
                SourceInformation sourceInformation = new SourceInformation("", line, charPositionInLine + 1, offendingToken.getLine(), charPositionInLine + offendingToken.getText().length());
                throw new MongoDBSchemaParserException(msg, sourceInformation);
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
        org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongoDBSchemaLexer lexer = new org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongoDBSchemaLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongoDBSchemaParser parser = new org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongoDBSchemaParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return visitDatabase(parser.json());
    }

    private MongoDatabase visitDatabase(MongoDBSchemaParser.JsonContext json)
    {
        MongoDBSchemaParser.ValueContext top_element = json.value();
        if (top_element.obj() != null)
        {
            // Top element Should be an object - corresponding to database
            MongoDBSchemaParser.ObjContext rootObj = top_element.obj();
            for (MongoDBSchemaParser.PairContext pairContext : rootObj.pair())
            {
                if (pairContext.key().keywords() != null && pairContext.key().keywords().DATABASE() != null)
                {
                    return visitDatabase(pairContext.value().obj());
                }
                else
                {
                    LOGGER.trace("Skipping keys we don't care: " + pairContext.getText());
                }
            }
        }
        int line = json.getStart().getLine();
        int colStart = json.getStart().getStartIndex() + 1;
        int colEnd = json.getStart().getStopIndex() + 1;
        SourceInformation sourceInformation = new SourceInformation("", line, colStart, line, colEnd);
        throw new MongoDBSchemaParserException("Top node not object type", sourceInformation);

    }

    private MongoDatabase visitDatabase(MongoDBSchemaParser.ObjContext dbObject)
    {
        Iterator<MongoDBSchemaParser.PairContext> iterPass1 = dbObject.pair().iterator();
        MongoDatabase db = new MongoDatabase();
        Map<String, Schema> schemas = Maps.mutable.empty();
        // pass 1 - skip processing collections as we want to process the schemas first.
        while (iterPass1.hasNext())
        {
            MongoDBSchemaParser.PairContext pair = iterPass1.next();
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
        for (MongoDBSchemaParser.PairContext pair : dbObject.pair())
        {
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

    private List<Collection> visitCollections(MongoDBSchemaParser.ValueContext value, Map<String, Schema> schemas)
    {
        if (value.arr() != null)
        {
            return ListIterate.collect(value.arr().value(), item -> visitCollection(item, schemas));
        }
        else
        {
            int line = value.getStart().getLine();
            int col = value.getStart().getStartIndex();
            String errorMessage = "Collections value needs to be an array, but found: " + value.getText();
            throw raiseException(line, col, errorMessage);
        }
    }

    private Collection visitCollection(MongoDBSchemaParser.ValueContext collectionContext, Map<String, Schema> schemas)
    {
        if (collectionContext.obj() != null)
        {
            Collection col = new Collection();
            for (MongoDBSchemaParser.PairContext pairContext : collectionContext.obj().pair())
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
            int col = collectionContext.getStart().getStartIndex();
            String errorMessage = "Collection node is not an Object type, but found: " + collectionContext.getText();
            throw raiseException(line, col, errorMessage);
        }
    }

    private Map<String, Schema> visitSchemas(MongoDBSchemaParser.ValueContext value)
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
    private Schema visitSchema(MongoDBSchemaParser.ValueContext schemaContext)
    {
        List<MongoDBSchemaParser.PairContext> schemaPair = schemaContext.obj().pair();
        Schema schema = new Schema();
        visitSchemaProperties(schema, schemaPair);
        visitObjectProperties(schema, schemaPair);
        return schema;

    }

    private void visitSchemaProperties(Schema schema, List<MongoDBSchemaParser.PairContext> pairContexts)
    {
        Iterator<MongoDBSchemaParser.PairContext> schemaIter = pairContexts.iterator();
        while (schemaIter.hasNext())
        {
            MongoDBSchemaParser.PairContext schemaPair = schemaIter.next();
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

    private void visitObjectProperties(ObjectType objType, List<MongoDBSchemaParser.PairContext> pairContexts)
    {
        Iterator<MongoDBSchemaParser.PairContext> pairIter = pairContexts.iterator();
        while (pairIter.hasNext())
        {
            MongoDBSchemaParser.PairContext objPair = pairIter.next();
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
                            objType.additionalPropertiesAllowed = objPair.value().TRUE() != null;
                        }
                        // else objPair.value().TRUE()  & we have set the value = true as default at the top
                        break;
                    default:
                        LOGGER.trace("Skipping key from object: " + key);
                }
            }
        }
    }

    private List<PropertyType> visitProperties(MongoDBSchemaParser.ValueContext value)
    {
        if (value.obj() != null)
        {
            Iterator<MongoDBSchemaParser.PairContext> propertiesIter = value.obj().pair().iterator();
            List<PropertyType> propertyTypes = FastList.newList();
            while (propertiesIter.hasNext())
            {
                MongoDBSchemaParser.PairContext propPair = propertiesIter.next();
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

    private BaseType visitPropertyType(MongoDBSchemaParser.ValueContext value)
    {
        if (value.obj() != null)
        {
            return getPropertySchemaType(value.obj());
        }
        else
        {
            LOGGER.debug("Property type node should be object type value, but found: " + value.getText());

        }
        return null;
    }

    private BaseType createTypeReferenceFromType(String type, int lineNumber, List<MongoDBSchemaParser.PairContext> pair)
    {
        switch (type)
        {
            case "string":
            {
                StringType stringType = new StringType();
                visitBaseTypeAttributes(pair, stringType);
                for (MongoDBSchemaParser.PairContext pairContext : pair)
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
                for (MongoDBSchemaParser.PairContext pairContext : pair)
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
                for (MongoDBSchemaParser.PairContext pairContext : pair)
                {
                    if (pairContext.key().keywords().MINIMUM() != null)
                    {
                        intType.minimum = Long.parseLong(pairContext.value().getText());
                    }
                    else if (pairContext.key().keywords().MAXIMUM() != null)
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
                for (MongoDBSchemaParser.PairContext pairContext : pair)
                {
                    if (pairContext.key().keywords().MINIMUM() != null)
                    {
                        decimalType.minimum = new BigDecimal(pairContext.value().getText());
                    }
                    else if (pairContext.key().keywords().MAXIMUM() != null)
                    {
                        decimalType.maximum = new BigDecimal(pairContext.value().getText());
                    }
                }
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
                throw new MongoDBSchemaParserException("Un-supported data type: " + type, sourceInformation);
            }
            // Skipping Timestamp
        }

    }

    private void visitArrayTypeAttributes(List<MongoDBSchemaParser.PairContext> pair, ArrayType arrayType)
    {

        for (MongoDBSchemaParser.PairContext pairContext : pair)
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

    private List<String> visitRequiredFields(MongoDBSchemaParser.ArrContext arr)
    {
        return arr.value().stream().map(t -> t.STRING().getText()).collect(Collectors.toList());
    }

    private BaseType getPropertySchemaType(MongoDBSchemaParser.ObjContext objContext)
    {

        int lineNumber = objContext.getStart().getLine();
        for (MongoDBSchemaParser.PairContext propType : objContext.pair())
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
        throw new MongoDBSchemaParserException("Un-supported data type", sourceInformation);
    }


}
