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

package org.finos.legend.engine.language.pure.grammar.integration.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.schema.MongoDBSchemaParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.deserializer.MongoDBSchemaDeserializer;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Collection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.SchemaValidationAction;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.SchemaValidationLevel;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Validator;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.JsonSchemaExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MongoDBSchemaParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;


    public MongoDBSchemaParseTreeWalker(ParseTreeWalkerSourceInformation sourceInformation)
    {
        this.walkerSourceInformation = sourceInformation;
        this.elementConsumer = null;
        this.section = null;
    }

    public MongoDBSchemaParseTreeWalker(ParseTreeWalkerSourceInformation sourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.walkerSourceInformation = sourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(MongoDBSchemaParserGrammar.DefinitionContext rootCtx)
    {
        rootCtx.mongodb().stream().map(this::visitMongoDBStore).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private MongoDatabase visitMongoDBStore(MongoDBSchemaParserGrammar.MongodbContext ctx)
    {
        MongoDatabase dbStore = new MongoDatabase();
        dbStore.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        dbStore._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        dbStore.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        dbStore.collections = ctx.collection().stream().map(this::visitCollection).collect(Collectors.toList());
        return dbStore;
    }

    private Collection visitCollection(MongoDBSchemaParserGrammar.CollectionContext ctx)
    {
        Collection colxn = new Collection();
        if (ctx.identifier().QUOTED_STRING() != null)
        {
            colxn.name = ctx.identifier().QUOTED_STRING().getText();
        }
        else
        {
            colxn.name = ctx.identifier().unquotedIdentifier().getText();
        }
        Validator validator = new Validator();
        if (ctx.validationLevel() != null)
        {
            validator.validationLevel = ctx.validationLevel().validationLevelValues().MODERATE() != null ? SchemaValidationLevel.moderate : SchemaValidationLevel.strict;
        }
        else
        {
            validator.validationLevel = SchemaValidationLevel.strict;
        }
        if (ctx.validationAction() != null)
        {
            validator.validationAction = ctx.validationAction().validationActionValues().WARN() != null ? SchemaValidationAction.warn : SchemaValidationAction.error;
        }
        else
        {
            validator.validationAction = SchemaValidationAction.error;
        }

        JsonSchemaExpression schemaExpression = this.getSchemaExpression(ctx);
        validator.validatorExpression = schemaExpression;
        colxn.validator = validator;
        return colxn;
    }

    private JsonSchemaExpression getSchemaExpression(MongoDBSchemaParserGrammar.CollectionContext ctx)
    {

        JsonFactory jFactory = new JsonFactory();
        try
        {
            JsonParser jParser = jFactory.createParser(ctx.jsonSchema().json().getText());
            jParser.setCodec(new ObjectMapper());
            MongoDBSchemaDeserializer schemaDeserializer = new MongoDBSchemaDeserializer();
            return schemaDeserializer.getSchemaExpression(jParser);
        }
        catch (IOException e)
        {
            throw new EngineException("Failed to parse the JSON Schema node", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
    }
}
