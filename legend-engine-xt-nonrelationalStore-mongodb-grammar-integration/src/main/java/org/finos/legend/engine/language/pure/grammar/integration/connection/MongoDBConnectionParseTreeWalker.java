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

import org.antlr.v4.runtime.tree.ParseTree;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MongoDBConnectionParserGrammar;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MongoDBConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public MongoDBConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public void visitServiceStoreConnectionValue(MongoDBConnectionParserGrammar.DefinitionContext ctx, MongoDBConnection connectionValue, boolean isEmbedded)
    {
        // store (optional if the store is provided by embedding context, if not provided, it is required)
        MongoDBConnectionParserGrammar.ConnectionStoreContext connectionStoreContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.connectionStore(), "store", connectionValue.sourceInformation);
        if (connectionStoreContext != null)
        {
            connectionValue.element = PureGrammarParserUtility.fromQualifiedName(connectionStoreContext.qualifiedName().packagePath() == null ? Collections.emptyList() : connectionStoreContext.qualifiedName().packagePath().identifier(), connectionStoreContext.qualifiedName().identifier());
            connectionValue.elementSourceInformation = this.walkerSourceInformation.getSourceInformation(connectionStoreContext.qualifiedName());
        }
        else if (!isEmbedded)
        {
            // Copied from service store, do we need this??
            PureGrammarParserUtility.validateAndExtractRequiredField(ctx.connectionStore(), "store", connectionValue.sourceInformation);
        }
        // database type
        MongoDBConnectionParserGrammar.DatabaseContext dbContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.database(), "database", connectionValue.sourceInformation);
        MongoDBDatasourceSpecification dsSpecification = new MongoDBDatasourceSpecification();
        dsSpecification.databaseName = PureGrammarParserUtility.fromIdentifier(dbContext.identifier());

        MongoDBConnectionParserGrammar.AuthenticationContext authContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.authentication(), "authentication", connectionValue.sourceInformation);
        List<ParseTree> pTree = authContext.islandDefintion().children;
        List<String> islandValues = pTree.stream().map(p -> p.getText()).collect(Collectors.toList());
        AuthenticationSpecification authSpecification = new UserPasswordAuthenticationSpecification("mongo_user", new SystemPropertiesSecret("mongo_pwd"));
        connectionValue.dataSourceSpecification = dsSpecification;
        connectionValue.authenticationSpecification = authSpecification;
    }
}

