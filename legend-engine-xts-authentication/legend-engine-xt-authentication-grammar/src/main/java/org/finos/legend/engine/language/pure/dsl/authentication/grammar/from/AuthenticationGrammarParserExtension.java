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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureIslandGrammarSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;

import java.util.Collections;
import java.util.List;

public class AuthenticationGrammarParserExtension implements IAuthenticationGrammarParserExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Authentication");
    }

    public List<Function<PureIslandGrammarSourceCode, AuthenticationSpecification>> getExtraAuthenticationParsers()
    {
        return Collections.singletonList(code ->
        {
            AuthenticationParseTreeWalker walker = new AuthenticationParseTreeWalker(code.walkerSourceInformation);
            switch (code.type)
            {
                case "ApiKey":
                    return parse(code, p -> walker.visitApiKeyAuthentication(p.apiKeyAuthentication()));
                case "UserPassword":
                    return parse(code, p -> walker.visitUserPasswordAuthentication(p.userPasswordAuthentication()));
                case "Kerberos":
                    return parse(code, p -> walker.visitKerberosAuthentication(p.kerberosAuthentication()));
                case "EncryptedPrivateKey":
                    return parse(code, p -> walker.visitEncryptedKeyPairAuthentication(p.encryptedPrivateKeyAuthentication()));
                case "GCPWIFWithAWSIdP":
                    return parse(code, p -> walker.visitGcpWIFWithAWSIdPAuthenticationContext(p.gcpWIFWithAWSIdPAuthentication()));
                default:
                    return null;
            }
        });
    }

    public List<Function<PureIslandGrammarSourceCode, CredentialVaultSecret>> getExtraCredentialVaultSecretParsers()
    {
        return Collections.singletonList(code ->
        {
            CredentialVaultSecretParseTreeWalker walker = new CredentialVaultSecretParseTreeWalker(code.walkerSourceInformation);
            switch (code.type)
            {
                case "PropertiesFileSecret":
                    return parse(code, p -> walker.visitPropertiesFileSecret(p.propertiesSecret()));
                case "EnvironmentSecret":
                    return parse(code, p -> walker.visitEnvironmentSecret(p.environmentSecret()));
                case "SystemPropertiesSecret":
                    return parse(code, p -> walker.visitSystemPropertiesSecret(p.systemPropertiesSecret()));
                case "AWSSecretsManagerSecret":
                    return parse(code, p -> walker.visitAwsSecretsManagerCredentialVaultSecret(p.awsSecretsManagerSecret()));
                default:
                    return null;
            }
        });
    }

    private <T> T parse(PureIslandGrammarSourceCode code, Function<AuthenticationParserGrammar, T> func)
    {
        CharStream input = CharStreams.fromString(code.code);
        ParserErrorListener errorListener = new ParserErrorListener(code.walkerSourceInformation, AuthenticationLexerGrammar.VOCABULARY);
        AuthenticationLexerGrammar lexer = new AuthenticationLexerGrammar(input);
        AuthenticationParserGrammar parser = new AuthenticationParserGrammar(new CommonTokenStream(lexer));

        lexer.addErrorListener(errorListener);
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }
}