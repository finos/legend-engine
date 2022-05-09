package org.finos.legend.engine.language.pure.grammar.to;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.*;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.*;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.RelationalDatabaseConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.RelationalDatabaseConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.AuthenticationStrategyLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.AuthenticationStrategyParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.datasource.DataSourceSpecificationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.datasource.DataSourceSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategyParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingElementParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingTestInputDataParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingElementSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.milestoning.MilestoningParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.milestoning.MilestoningSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.postProcessors.PostProcessorParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.postProcessors.PostProcessorSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RootRelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.ElementWithJoins;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.JoinPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class AwsFinCloudGrammarComposerExtension implements IAwsGrammarComposerExtension
{
    public static final String NAME = "Aws";
    public static final String AWS_FIN_CLOUD_CONNECTION_TYPE = "AwsFinCloudConnection";

    @Override
    public List<Function2<AuthenticationStrategy, PureGrammarComposerContext, String>> getExtraAuthenticationStrategyComposers()
    {
        return Lists.mutable.with((strategy, context) -> HelperAwsFinCloudGrammarComposer.visitAwsFinCloudConnectionAuthenticationStrategy(strategy, RelationalGrammarComposerContext.Builder.newInstance(context).build()));
    //look at relationalgrammarcomposercontext
    }

    @Override
    public List<Function2<FinCloudTargetSpecification, PureGrammarComposerContext, String>> getExtraDataSourceSpecificationComposers()
    {
        return Lists.mutable.with((specification, context) -> HelperAwsFinCloudGrammarComposer.visitAwsFinCloudConnectionDatasourceSpecification(specification, PureGrammarComposerContext.Builder.newInstance(context).build()));
    }

    @Override
    public List<Function2<Connection, PureGrammarComposerContext, Pair<String, String>>> getExtraConnectionValueComposers()
    {
        return Lists.mutable.with((connectionValue, context) ->
        {
            if (connectionValue instanceof FinCloudConnection) {
                PureGrammarComposerContext ctx = PureGrammarComposerContext.Builder.newInstance(context).build();
                FinCloudConnection finCloudConnection = (FinCloudConnection) connectionValue;
                int baseIndentation = 0;

                List<IAwsGrammarComposerExtension> extensions = IAwsGrammarComposerExtension.getExtensions(context);

                String authenticationStrategy = IAwsGrammarComposerExtension.process(finCloudConnection.authenticationStrategy,
                        ListIterate.flatCollect(extensions, IAwsGrammarComposerExtension::getExtraAuthenticationStrategyComposers),
                        context);

                //String specification = IAwsGrammarComposerExtension.process(finCloudConnection.targetSpecification,
                //        ListIterate.flatCollect(extensions, IAwsGrammarComposerExtension::getExtraDataSourceSpecificationComposers),
                //        context);

                return Tuples.pair(AwsFinCloudGrammarParserExtension.AWS_FIN_CLOUD_CONNECTION_TYPE, context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        (finCloudConnection.element != null ? (context.getIndentationString() + getTabString(baseIndentation + 1) + "store: " + finCloudConnection.element + ";\n") : "") +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "datasetId: " + finCloudConnection.datasetId + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "authenticationStrategy: '" + authenticationStrategy + "';\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "targetSpecification: '" + finCloudConnection.apiUrl + "';\n" +
                        context.getIndentationString() + "}");
            }
            return null;
        });
    }


    // might not require this -- check
    private AuthenticationStrategy parseAuthenticationStrategy(AuthenticationStrategySourceCode code, Function<AuthenticationStrategyParserGrammar, AuthenticationStrategy> func)
    {
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

    private FinCloudDatasourceSpecification parseDataSourceSpecification(DataSourceSpecificationSourceCode code, Function<FinCloudDatasourceSpecificationParserGrammar, FinCloudDatasourceSpecification> func)
    {
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
}

