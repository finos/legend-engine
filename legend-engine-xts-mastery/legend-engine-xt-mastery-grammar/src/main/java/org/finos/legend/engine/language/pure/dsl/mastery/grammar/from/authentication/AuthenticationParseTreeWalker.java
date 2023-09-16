
// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.authentication;

import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.IMasteryParserExtension;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.SpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MasteryParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationStrategyParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MasteryConnectionParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.CredentialSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.NTLMAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.TokenAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;

import java.util.List;
import java.util.function.Function;

public class AuthenticationParseTreeWalker
{

    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final List<Function<SpecificationSourceCode, CredentialSecret>> credentialSecretProcessors;

    public AuthenticationParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, List<Function<SpecificationSourceCode, CredentialSecret>> credentialSecretProcessors)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.credentialSecretProcessors = credentialSecretProcessors;
    }

    public AuthenticationStrategy visitAuthentication(AuthenticationStrategyParserGrammar ctx)
    {
        AuthenticationStrategyParserGrammar.DefinitionContext definitionContext =  ctx.definition();

        if (definitionContext.tokenAuthentication() != null)
        {
            return visitTokenAuthentication(definitionContext.tokenAuthentication());
        }

        if (definitionContext.ntlmAuthentication() != null)
        {
            return visitNTLMAuthentication(definitionContext.ntlmAuthentication());
        }

        return null;
    }

    public AuthenticationStrategy visitNTLMAuthentication(AuthenticationStrategyParserGrammar.NtlmAuthenticationContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        NTLMAuthenticationStrategy authenticationStrategy = new NTLMAuthenticationStrategy();

        // credential
        AuthenticationStrategyParserGrammar.CredentialContext credentialContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.credential(), "credential", sourceInformation);
        authenticationStrategy.credential = IMasteryParserExtension.process(extraSpecificationCode(credentialContext.islandSpecification(), walkerSourceInformation), credentialSecretProcessors, "credential secret");

        return authenticationStrategy;
    }

    public AuthenticationStrategy visitTokenAuthentication(AuthenticationStrategyParserGrammar.TokenAuthenticationContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        TokenAuthenticationStrategy authenticationStrategy = new TokenAuthenticationStrategy();

        // token url
        AuthenticationStrategyParserGrammar.TokenUrlContext tokenUrlContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.tokenUrl(), "tokenUrl", sourceInformation);
        authenticationStrategy.tokenUrl = PureGrammarParserUtility.fromGrammarString(tokenUrlContext.STRING().getText(), true);

        // credential
        AuthenticationStrategyParserGrammar.CredentialContext credentialContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.credential(), "credential", sourceInformation);
        authenticationStrategy.credential = IMasteryParserExtension.process(extraSpecificationCode(credentialContext.islandSpecification(), walkerSourceInformation), credentialSecretProcessors, "credential secret");

        return authenticationStrategy;
    }

    static SpecificationSourceCode extraSpecificationCode(AuthenticationStrategyParserGrammar.IslandSpecificationContext ctx, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        StringBuilder text = new StringBuilder();
        AuthenticationStrategyParserGrammar.IslandValueContext islandValueContext = ctx.islandValue();
        if (islandValueContext != null)
        {
            for (AuthenticationStrategyParserGrammar.IslandValueContentContext fragment : islandValueContext.islandValueContent())
            {
                text.append(fragment.getText());
            }

            // prepare island grammar walker source information
            int startLine = islandValueContext.ISLAND_OPEN().getSymbol().getLine();
            int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
            // only add current walker source information column offset if this is the first line
            int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + islandValueContext.ISLAND_OPEN().getSymbol().getCharPositionInLine() + islandValueContext.ISLAND_OPEN().getSymbol().getText().length();
            ParseTreeWalkerSourceInformation triggerValueWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(walkerSourceInformation.getReturnSourceInfo()).build();
            SourceInformation triggerValueSourceInformation = walkerSourceInformation.getSourceInformation(ctx);

            return new SpecificationSourceCode(text.toString(), ctx.islandType().getText(), triggerValueSourceInformation, triggerValueWalkerSourceInformation);
        }
        else
        {
            SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
            return new SpecificationSourceCode(text.toString(), ctx.islandType().getText(), sourceInformation, walkerSourceInformation);
        }
    }
}
