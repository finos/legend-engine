package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.AuthenticationStrategyLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.AuthenticationStrategyParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.FinCloudDatasourceSpecificationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.FinCloudDatasourceSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.datasource.DataSourceSpecificationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.datasource.DataSourceSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategyParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudTargetSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
//import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;

public interface IAwsGrammarParserExtension extends PureGrammarParserExtension {
    static List<IAwsGrammarParserExtension> getExtensions() {
        return Lists.mutable.withAll(ServiceLoader.load(IAwsGrammarParserExtension.class));
    }

    static AuthenticationStrategy process(AuthenticationStrategySourceCode code, List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> processors) {
        return process(code, processors, "Authentication Strategy");
    }

    static FinCloudTargetSpecification process(DataSourceSpecificationSourceCode code, List<Function<DataSourceSpecificationSourceCode, FinCloudTargetSpecification>> processors) {
        return process(code, processors, "Data Source Specification");
    }

    static <T extends SpecificationSourceCode, U> U process(T code, List<Function<T, U>> processors, String type) {
        return ListIterate
                .collect(processors, processor -> processor.apply(code))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + code.getType() + "'", code.getSourceInformation(), EngineErrorType.PARSER));
    }

    static AuthenticationStrategy parseAuthenticationStrategy(AuthenticationStrategySourceCode code, Function<AuthenticationStrategyParserGrammar, AuthenticationStrategy> func) {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        AuthenticationStrategyLexerGrammar lexer = new AuthenticationStrategyLexerGrammar(input);
        AuthenticationStrategyParserGrammar parser = new AuthenticationStrategyParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }

    default List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers() {
        return Collections.singletonList(code ->
        {
            AuthenticationStrategyParseTreeWalker walker = new AuthenticationStrategyParseTreeWalker();

            switch (code.getType()) {
                case "awsOAuth":
                    return parseAuthenticationStrategy(code, p -> walker.visitAwsOAuthAuthenticationStrategy(code, p.awsOAuth()));
                default:
                    return null;
            }
        });
    }

    static FinCloudTargetSpecification parseDataSourceSpecification(DataSourceSpecificationSourceCode code, Function<FinCloudDatasourceSpecificationParserGrammar, FinCloudTargetSpecification> func) {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        FinCloudDatasourceSpecificationLexerGrammar lexer = new FinCloudDatasourceSpecificationLexerGrammar(input);
        FinCloudDatasourceSpecificationParserGrammar parser = new FinCloudDatasourceSpecificationParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }

    default List<Function<DataSourceSpecificationSourceCode, FinCloudTargetSpecification>> getExtraDataSourceSpecificationParsers() {
        return Collections.singletonList(code ->
        {
            FinCloudDatasourceSpecificationParseTreeWalker walker = new FinCloudDatasourceSpecificationParseTreeWalker();

            switch (code.getType()) {
                case "AwsFinCloudDatasourceSpecification":
                    return parseDataSourceSpecification(code, p -> walker.visitFinCloudDatasourceSpecification(code, p.finCloudDatasourceSpec()));
                default:
                    return parseDataSourceSpecification(code, p -> walker.visitFinCloudDatasourceSpecification(code, p.finCloudDatasourceSpec()));
            }
        });

        }

    }
