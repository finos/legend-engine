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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.milestoning.MilestoningSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.postProcessors.PostProcessorSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;

public interface IRelationalGrammarParserExtension extends PureGrammarParserExtension
{
    static List<IRelationalGrammarParserExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IRelationalGrammarParserExtension.class));
    }

    static DatasourceSpecification process(DataSourceSpecificationSourceCode code, List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> processors)
    {
        return process(code, processors, "Data Source Specification");
    }

    static AuthenticationStrategy process(AuthenticationStrategySourceCode code, List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> processors)
    {
        return process(code, processors, "Authentication Strategy");
    }

    static PostProcessor process(PostProcessorSpecificationSourceCode code, List<Function<PostProcessorSpecificationSourceCode, PostProcessor>> processors)
    {
        return process(code, processors, "Post Processor");
    }

    static Milestoning process(MilestoningSpecificationSourceCode code, List<Function<MilestoningSpecificationSourceCode, Milestoning>> processors)
    {
        return process(code, processors, "Milestoning");
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

    default List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<PostProcessorSpecificationSourceCode, PostProcessor>> getExtraPostProcessorParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<MilestoningSpecificationSourceCode, Milestoning>> getExtraMilestoningParsers()
    {
        return Collections.emptyList();
    }
}