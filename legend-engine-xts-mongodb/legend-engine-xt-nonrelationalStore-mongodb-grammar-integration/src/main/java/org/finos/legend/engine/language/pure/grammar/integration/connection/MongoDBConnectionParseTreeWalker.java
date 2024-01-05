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

package org.finos.legend.engine.language.pure.grammar.integration.connection;

import org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.IAuthenticationGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MongoDBConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MongoDBConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.DatabaseType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBDatasourceSpecification;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBURL;

import java.util.Collections;
import java.util.stream.Collectors;

public class MongoDBConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserExtensions extension;

    public MongoDBConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserExtensions extension)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.extension = extension;
    }

    public void visitMongoDBConnectionValue(MongoDBConnectionParserGrammar.DefinitionContext ctx, MongoDBConnection connectionValue, boolean isEmbedded)
    {
        // store (optional if the store is provided by embedding context, if not provided, it is required)
        MongoDBConnectionParserGrammar.ConnectionStoreContext connectionStoreContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.connectionStore(), "store", connectionValue.sourceInformation);
        if (connectionStoreContext != null)
        {
            connectionValue.element = PureGrammarParserUtility.fromQualifiedName(connectionStoreContext.qualifiedName().packagePath() == null ? Collections.emptyList() : connectionStoreContext.qualifiedName().packagePath().identifier(), connectionStoreContext.qualifiedName().identifier());
            connectionValue.elementSourceInformation = this.walkerSourceInformation.getSourceInformation(connectionStoreContext.qualifiedName());
        }
        // database type
        connectionValue.type = DatabaseType.MongoDb;
        MongoDBDatasourceSpecification dsSpecification = getMongoDBDatasourceSpecification(ctx, connectionValue);
        connectionValue.dataSourceSpecification = dsSpecification;

        MongoDBConnectionParserGrammar.AuthenticationContext authenticationContext = PureGrammarParserUtility.validateAndExtractRequiredField(
                ctx.authentication(),
                MongoDBConnectionLexerGrammar.VOCABULARY.getLiteralName(MongoDBConnectionLexerGrammar.AUTHENTICATION),
                connectionValue.sourceInformation
        );
        connectionValue.authenticationSpecification = IAuthenticationGrammarParserExtension.parseAuthentication(authenticationContext.islandDefinition(), this.walkerSourceInformation, this.extension);

    }

    private static MongoDBDatasourceSpecification getMongoDBDatasourceSpecification(MongoDBConnectionParserGrammar.DefinitionContext ctx, MongoDBConnection connectionValue)
    {
        MongoDBDatasourceSpecification dsSpecification = new MongoDBDatasourceSpecification();
        MongoDBConnectionParserGrammar.DatabaseContext dbContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.database(), "database", connectionValue.sourceInformation);
        dsSpecification.databaseName = PureGrammarParserUtility.fromIdentifier(dbContext.identifier());

        MongoDBConnectionParserGrammar.ServerDetailsContext serverDetailsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serverDetails(), "serverURLs", connectionValue.sourceInformation);
        dsSpecification.serverURLs = serverDetailsContext.serverURLDef().stream().map(i ->
        {
            MongoDBURL url = new MongoDBURL();
            url.baseUrl = i.HOST_STRING().getText();
            url.port = Long.parseLong(i.INTEGER().getText());
            return url;
        }).collect(Collectors.toList());
        return dsSpecification;
    }
}

