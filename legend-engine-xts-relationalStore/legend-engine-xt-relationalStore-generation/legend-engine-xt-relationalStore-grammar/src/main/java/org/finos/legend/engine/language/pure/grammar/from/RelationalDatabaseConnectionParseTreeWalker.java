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

package org.finos.legend.engine.language.pure.grammar.from;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.RelationalDatabaseConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.postProcessors.PostProcessorSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.queryGenerationConfigs.QueryGenerationConfigSourceCode;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalQueryGenerationConfig;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class RelationalDatabaseConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public RelationalDatabaseConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public void visitRelationalDatabaseConnectionValue(RelationalDatabaseConnectionParserGrammar.DefinitionContext ctx, RelationalDatabaseConnection connectionValue, boolean isEmbedded)
    {
        // store (optional if the store is provided by embedding context, if not provided, it is required)
        RelationalDatabaseConnectionParserGrammar.ConnectionStoreContext storeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.connectionStore(), "store", connectionValue.sourceInformation);
        if (storeContext != null)
        {
            connectionValue.element = PureGrammarParserUtility.fromQualifiedName(storeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : storeContext.qualifiedName().packagePath().identifier(), storeContext.qualifiedName().identifier());
            connectionValue.elementSourceInformation = this.walkerSourceInformation.getSourceInformation(storeContext.qualifiedName());
        }
        // database type
        RelationalDatabaseConnectionParserGrammar.DbTypeContext dbTypeCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dbType(), "type", connectionValue.sourceInformation);
        try
        {
            connectionValue.type = DatabaseType.valueOf(PureGrammarParserUtility.fromIdentifier(dbTypeCtx.identifier()));
            connectionValue.databaseType = DatabaseType.valueOf(PureGrammarParserUtility.fromIdentifier(dbTypeCtx.identifier()));
        }
        catch (Exception e)
        {
            throw new EngineException("Unknown database type '" + PureGrammarParserUtility.fromIdentifier(dbTypeCtx.identifier()) + "'", this.walkerSourceInformation.getSourceInformation(dbTypeCtx), EngineErrorType.PARSER);
        }
        //queryTimeoutInSeconds (optional)
        RelationalDatabaseConnectionParserGrammar.QueryTimeOutInSecondsContext queryTimeOutInSecondsCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.queryTimeOutInSeconds(), "queryTimeOutInSeconds", connectionValue.sourceInformation);
        connectionValue.queryTimeOutInSeconds = queryTimeOutInSecondsCtx != null ? Integer.parseInt(queryTimeOutInSecondsCtx.INTEGER().getText()) : null;
        // timezone (optional)
        RelationalDatabaseConnectionParserGrammar.DbConnectionTimezoneContext timezoneCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.dbConnectionTimezone(), "timezone", connectionValue.sourceInformation);
        connectionValue.timeZone = timezoneCtx != null ? timezoneCtx.TIMEZONE().getText() : null;
        // quoteIdentifiers (optional)
        RelationalDatabaseConnectionParserGrammar.DbQuoteIdentifiersContext quoteIdentifiersContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.dbQuoteIdentifiers(), "quoteIdentifiers", connectionValue.sourceInformation);
        connectionValue.quoteIdentifiers = quoteIdentifiersContext != null ? Boolean.parseBoolean(quoteIdentifiersContext.BOOLEAN().getText()) : null;
        //post processors
        RelationalDatabaseConnectionParserGrammar.RelationalPostProcessorsContext postProcessorsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.relationalPostProcessors(), "postProcessors", connectionValue.sourceInformation);
        connectionValue.postProcessors = postProcessorsContext != null ? this.visitRelationalPostProcessors(postProcessorsContext) : null;
        // queryGenerationConfigs
        RelationalDatabaseConnectionParserGrammar.QueryGenConfigsContext queryGenConfigsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.queryGenConfigs(), "queryGenerationConfigs", connectionValue.sourceInformation);
        connectionValue.queryGenerationConfigs = queryGenConfigsContext != null ? this.visitQueryGenConfigs(queryGenConfigsContext) : null;

        RelationalDatabaseConnectionParserGrammar.ConnectionModeContext connectionModeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.connectionMode(), "mode", connectionValue.sourceInformation);
        String localMode = connectionModeContext != null ? PureGrammarParserUtility.fromIdentifier(connectionModeContext.identifier()) : null;
        if ("local".equals(localMode))
        {
            this.handleLocalMode(connectionValue);
        }
        else
        {
            // datasource specification
            RelationalDatabaseConnectionParserGrammar.RelationalDBDatasourceSpecContext dspCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.relationalDBDatasourceSpec(), "specification", connectionValue.sourceInformation);
            connectionValue.datasourceSpecification = this.visitRelationalDatabaseConnectionDatasourceSpecification(dspCtx);
            // authentication strategy
            RelationalDatabaseConnectionParserGrammar.RelationalDBAuthContext authCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.relationalDBAuth(), "auth", connectionValue.sourceInformation);
            connectionValue.authenticationStrategy = this.visitRelationalDatabaseConnectionAuthenticationStrategy(authCtx);
        }
    }

    private void handleLocalMode(RelationalDatabaseConnection connectionValue)
    {
        connectionValue.localMode = true;
        List<IRelationalGrammarParserExtension> extensions = IRelationalGrammarParserExtension.getExtensions();
        try
        {
            connectionValue.datasourceSpecification = IRelationalGrammarParserExtension.process(
                    connectionValue,
                    ListIterate.flatCollect(extensions, IRelationalGrammarParserExtension::getExtraLocalModeDataSourceSpecification));

            connectionValue.authenticationStrategy = IRelationalGrammarParserExtension.process(
                    connectionValue,
                    ListIterate.flatCollect(extensions, IRelationalGrammarParserExtension::getExtraLocalModeAuthenticationStrategy));
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException("'local' mode not supported " + e.getMessage());
        }
    }

    private List<PostProcessor> visitRelationalPostProcessors(RelationalDatabaseConnectionParserGrammar.RelationalPostProcessorsContext postProcessorsContext)
    {
        List<RelationalDatabaseConnectionParserGrammar.SpecificationContext> specifications = postProcessorsContext.specification();
        List<IRelationalGrammarParserExtension> extensions = IRelationalGrammarParserExtension.getExtensions();
        List<Function<PostProcessorSpecificationSourceCode, PostProcessor>> parsers = ListIterate.flatCollect(extensions, IRelationalGrammarParserExtension::getExtraPostProcessorParsers);
        return ListIterate.collect(specifications, spec -> visitRelationalPostProcessor(spec, parsers));
    }

    private PostProcessor visitRelationalPostProcessor(RelationalDatabaseConnectionParserGrammar.SpecificationContext spec, List<Function<PostProcessorSpecificationSourceCode, PostProcessor>> parsers)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(spec);

        PostProcessorSpecificationSourceCode code = new PostProcessorSpecificationSourceCode(
                spec.getText(),
                spec.specificationType().getText(),
                sourceInformation,
                ParseTreeWalkerSourceInformation.offset(walkerSourceInformation, spec.getStart())
        );

        PostProcessor processor = IRelationalGrammarParserExtension.process(code, parsers);

        if (processor == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(spec), EngineErrorType.PARSER);
        }

        return processor;
    }

    private List<RelationalQueryGenerationConfig> visitQueryGenConfigs(RelationalDatabaseConnectionParserGrammar.QueryGenConfigsContext queryGenConfigsContext)
    {
        List<RelationalDatabaseConnectionParserGrammar.SpecificationContext> specifications = queryGenConfigsContext.specification();
        List<IRelationalGrammarParserExtension> extensions = IRelationalGrammarParserExtension.getExtensions();
        List<Function<QueryGenerationConfigSourceCode, RelationalQueryGenerationConfig>> parsers = ListIterate.flatCollect(extensions, IRelationalGrammarParserExtension::getExtraQueryGenerationConfigParsers);
        return ListIterate.collect(specifications, spec -> visitQueryGenConfig(spec, parsers));
    }

    private RelationalQueryGenerationConfig visitQueryGenConfig(RelationalDatabaseConnectionParserGrammar.SpecificationContext spec, List<Function<QueryGenerationConfigSourceCode, RelationalQueryGenerationConfig>> parsers)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(spec);
        ParseTreeWalkerSourceInformation subParseTreeWalkerSourceInfo = ParseTreeWalkerSourceInformation.offset(walkerSourceInformation, spec.getStart());

        QueryGenerationConfigSourceCode code = new QueryGenerationConfigSourceCode(
                spec.getText(),
                spec.specificationType().getText(),
                sourceInformation,
                new ParseTreeWalkerSourceInformation.Builder(subParseTreeWalkerSourceInfo).withLineOffset(subParseTreeWalkerSourceInfo.getLineOffset() + 1).build() // Update line offset
        );

        RelationalQueryGenerationConfig relationalQueryGenerationConfig = IRelationalGrammarParserExtension.process(code, parsers);

        if (relationalQueryGenerationConfig == null)
        {
            throw new EngineException("Unsupported syntax", sourceInformation, EngineErrorType.PARSER);
        }

        relationalQueryGenerationConfig.sourceInformation = sourceInformation;
        return relationalQueryGenerationConfig;
    }

    private DatasourceSpecification visitRelationalDatabaseConnectionDatasourceSpecification(RelationalDatabaseConnectionParserGrammar.RelationalDBDatasourceSpecContext ctx)
    {
        RelationalDatabaseConnectionParserGrammar.SpecificationContext specification = ctx.specification();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        DataSourceSpecificationSourceCode code = new DataSourceSpecificationSourceCode(
                ctx.specification().getText(),
                specification.specificationType().getText(),
                sourceInformation,
                ParseTreeWalkerSourceInformation.offset(walkerSourceInformation, ctx.getStart())
        );

        List<IRelationalGrammarParserExtension> extensions = IRelationalGrammarParserExtension.getExtensions();
        DatasourceSpecification ds = IRelationalGrammarParserExtension.process(code, ListIterate.flatCollect(extensions, IRelationalGrammarParserExtension::getExtraDataSourceSpecificationParsers));

        if (ds == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }

        return ds;
    }


    public AuthenticationStrategy visitRelationalDatabaseConnectionAuthenticationStrategy(RelationalDatabaseConnectionParserGrammar.RelationalDBAuthContext ctx)
    {
        RelationalDatabaseConnectionParserGrammar.SpecificationContext specification = ctx.specification();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        AuthenticationStrategySourceCode code = new AuthenticationStrategySourceCode(
                ctx.specification().getText(),
                specification.specificationType().getText(),
                sourceInformation,
                ParseTreeWalkerSourceInformation.offset(walkerSourceInformation, ctx.getStart())
        );

        List<IRelationalGrammarParserExtension> extensions = IRelationalGrammarParserExtension.getExtensions();
        AuthenticationStrategy auth = IRelationalGrammarParserExtension.process(code, ListIterate.flatCollect(extensions, IRelationalGrammarParserExtension::getExtraAuthenticationStrategyParsers));

        if (auth == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }

        return auth;
    }
}