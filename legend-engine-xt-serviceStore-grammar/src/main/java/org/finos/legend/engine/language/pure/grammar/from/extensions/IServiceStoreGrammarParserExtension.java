// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.extensions;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.SpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.connection.authentication.AuthenticationSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.connection.authentication.SecuritySchemeSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;

public interface IServiceStoreGrammarParserExtension extends PureGrammarParserExtension
{
    static List<IServiceStoreGrammarParserExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IServiceStoreGrammarParserExtension.class));
    }

    static SecurityScheme process(SecuritySchemeSourceCode code, List<Function<SecuritySchemeSourceCode, SecurityScheme>> processors)
    {
        return process(code, processors, "Security Scheme");
    }

    static AuthenticationSpecification process(AuthenticationSpecificationSourceCode code, List<Function<AuthenticationSpecificationSourceCode, AuthenticationSpecification>> processors)
    {
        return process(code, processors, "Auth Token Generation Specification");
    }

    default List<Function<SecuritySchemeSourceCode, SecurityScheme>> getExtraSecuritySchemesParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<AuthenticationSpecificationSourceCode, AuthenticationSpecification>> getExtraAuthenticationGenerationSpecificationParsers()
    {
        return Collections.emptyList();
    }

    static <T extends SpecificationSourceCode, U> U process(T code, List<Function<T, U>> processors, String type)
    {
        return ListIterate
                .collect(processors, processor -> processor.apply(code))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + code.getType() + "'", code.getSourceInformation(), EngineErrorType.PARSER));
    }

    static <P extends Parser, V> V parse(SpecificationSourceCode code, Function<CharStream, Lexer> lexerFunc, Function<TokenStream, P> parserFunc, Function<P, V> transformer)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        Lexer lexer = lexerFunc.apply(input);
        P parser = parserFunc.apply(new CommonTokenStream(lexer));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return transformer.apply(parser);
    }



}
