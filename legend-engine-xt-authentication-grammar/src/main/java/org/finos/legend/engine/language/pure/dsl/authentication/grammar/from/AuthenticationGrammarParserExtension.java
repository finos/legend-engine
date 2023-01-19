// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.authentication.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;

import java.util.Collections;
import java.util.List;

public class AuthenticationGrammarParserExtension implements IAuthenticationGrammarParserExtension
{
    public static final String NAME = "Authentication";

    public List<Function<SpecificationSourceCode, AuthenticationSpecification>> getExtraAuthenticationParsers()
    {
        return Collections.singletonList(code ->
        {
            SourceCodeParserInfo parserInfo = getParserInfo(code);
            AuthenticationParseTreeWalker walker = new AuthenticationParseTreeWalker(parserInfo.walkerSourceInformation);
            switch (code.getType())
            {
                case "ApiKey":
                    return parseAuthentication(code, p -> walker.visitApiKeyAuthentication(p.apiKeyAuthentication()));
                case "UserPassword":
                    return parseAuthentication(code, p -> walker.visitUserPasswordAuthentication(p.userPasswordAuthentication()));
                case "EncryptedPrivateKey":
                    return parseAuthentication(code, p -> walker.visitEncryptedKeyPairAuthentication(p.encryptedPrivateKeyAuthentication()));
                case "GCPWIFWithAWSIdP":
                    return parseAuthentication(code, p -> walker.visitGcpWIFWithAWSIdPAuthenticationContext(p.gcpWIFWithAWSIdPAuthentication()));
                default:
                    return null;
            }
        });
    }

    public List<Function<SpecificationSourceCode, CredentialVaultSecret>> getExtraCredentialVaultSecretParsers()
    {
        return Collections.singletonList(code ->
        {
            SourceCodeParserInfo parserInfo = getParserInfo(code);
            CredentialVaultSecretParseTreeWalker walker = new CredentialVaultSecretParseTreeWalker(parserInfo.walkerSourceInformation);
            switch (code.getType())
            {
                case "PropertiesFileSecret":
                    return parseSecret(code, p -> walker.visitPropertiesFileSecret(p.propertiesSecret()));
                case "EnvironmentSecret":
                    return parseSecret(code, p -> walker.visitEnvironmentSecret(p.environmentSecret()));
                case "SystemPropertiesSecret":
                    return parseSecret(code, p -> walker.visitSystemPropertiesSecret(p.systemPropertiesSecret()));
                case "AWSSecretsManagerSecret":
                    return parseSecret(code, p -> walker.visitAwsSecretsManagerCredentialVaultSecret(p.awsSecretsManagerSecret()));
                default:
                    return null;
            }
        });
    }

    private AuthenticationSpecification parseAuthentication(SpecificationSourceCode code, Function<AuthenticationParserGrammar, AuthenticationSpecification> func)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        AuthenticationLexerGrammar lexer = new AuthenticationLexerGrammar(input);
        AuthenticationParserGrammar parser = new AuthenticationParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }

    private CredentialVaultSecret parseSecret(SpecificationSourceCode code, Function<AuthenticationParserGrammar, CredentialVaultSecret> func)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        AuthenticationLexerGrammar lexer = new AuthenticationLexerGrammar(input);
        AuthenticationParserGrammar parser = new AuthenticationParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }

    private static SourceCodeParserInfo getParserInfo(SpecificationSourceCode specificationSourceCode)
    {
        CharStream input = CharStreams.fromString(specificationSourceCode.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(specificationSourceCode.getWalkerSourceInformation());
        AuthenticationLexerGrammar lexer = new AuthenticationLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        AuthenticationParserGrammar parser = new AuthenticationParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(specificationSourceCode.getCode(), input, specificationSourceCode.getSourceInformation(), specificationSourceCode.getWalkerSourceInformation(), lexer, parser, null);
    }

}