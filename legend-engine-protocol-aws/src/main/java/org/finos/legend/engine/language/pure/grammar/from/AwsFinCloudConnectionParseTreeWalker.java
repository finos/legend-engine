package org.finos.legend.engine.language.pure.grammar.from;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.AwsFinCloudConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.FinCloudDatasourceSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.RelationalDatabaseConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.postProcessors.PostProcessorSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudTargetSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class AwsFinCloudConnectionParseTreeWalker {

    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public AwsFinCloudConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation) {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    /**********
     * FinCloud Connection
     **********/

    public void visitAwsFinCloudConnectionValue(AwsFinCloudConnectionParserGrammar.DefinitionContext ctx, FinCloudConnection awsFinCloudConnection) {
        // store
        AwsFinCloudConnectionParserGrammar.ConnectionStoreContext storeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.connectionStore(), "store", awsFinCloudConnection.sourceInformation);
        if (storeContext != null) {
            awsFinCloudConnection.element = PureGrammarParserUtility.fromQualifiedName(storeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : storeContext.qualifiedName().packagePath().identifier(), storeContext.qualifiedName().identifier());
            awsFinCloudConnection.elementSourceInformation = this.walkerSourceInformation.getSourceInformation(storeContext.qualifiedName());
        }

        // datasetId
        AwsFinCloudConnectionParserGrammar.DatasetIdContext datasetIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.datasetId(), "datasetId", awsFinCloudConnection.sourceInformation);
        awsFinCloudConnection.datasetId = PureGrammarParserUtility.fromGrammarString(datasetIdContext.STRING().getText(), true);

        // authenticationStrategy
        AwsFinCloudConnectionParserGrammar.AuthenticationStrategyContext authenticationStrategyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.authenticationStrategy(), "authenticationStrategy", awsFinCloudConnection.sourceInformation);
        awsFinCloudConnection.authenticationStrategy = visitAuthenticationStrategy(authenticationStrategyContext);

        // targetSpecification
        AwsFinCloudConnectionParserGrammar.ApiUrlContext apiUrlContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.apiUrl(), "apiUrl", awsFinCloudConnection.sourceInformation);
        awsFinCloudConnection.apiUrl = PureGrammarParserUtility.fromGrammarString(apiUrlContext.STRING().getText(), true);

        // queryInfo
        AwsFinCloudConnectionParserGrammar.QueryInfoContext queryInfoContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.queryInfo(), "queryInfo", awsFinCloudConnection.sourceInformation);
        awsFinCloudConnection.queryInfo = PureGrammarParserUtility.fromGrammarString(queryInfoContext.STRING().getText(), true);

        // this.visit... ?
    }


    private AuthenticationStrategy visitAuthenticationStrategy(AwsFinCloudConnectionParserGrammar.AuthenticationStrategyContext ctx)
    {
        AwsFinCloudConnectionParserGrammar.SpecificationContext specification = ctx.specification();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        AuthenticationStrategySourceCode code = new AuthenticationStrategySourceCode(
                ctx.specification().getText(),
                specification.specificationType().getText(),
                sourceInformation,
                new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation)
                        .withLineOffset(sourceInformation.startLine - 1)
                        .withColumnOffset(sourceInformation.startColumn)
                        .build()
        );

        List<IAwsGrammarParserExtension> extensions = IAwsGrammarParserExtension.getExtensions();
        AuthenticationStrategy auth = IAwsGrammarParserExtension.process(code, ListIterate.flatCollect(extensions, IAwsGrammarParserExtension::getExtraAuthenticationStrategyParsers));

        if (auth == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }

        return auth;
    }

/*
    private FinCloudTargetSpecification visitTargetSpecification(AwsFinCloudConnectionParserGrammar.TargetSpecificationContext ctx)
    {
        AwsFinCloudConnectionParserGrammar.SpecificationContext specification = ctx.specification();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        DataSourceSpecificationSourceCode code = new DataSourceSpecificationSourceCode(
                ctx.specification().getText(),
                specification.specificationType().getText(),
                sourceInformation,
                new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation)
                        .withLineOffset(sourceInformation.startLine - 1)
                        .withColumnOffset(sourceInformation.startColumn)
                        .build()
        );

        List<IAwsGrammarParserExtension> extensions = IAwsGrammarParserExtension.getExtensions();
        FinCloudTargetSpecification ds = IAwsGrammarParserExtension.process(code, ListIterate.flatCollect(extensions, IAwsGrammarParserExtension::getExtraDataSourceSpecificationParsers));

        if (ds == null)
        {
            throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }

        return ds;
    }

   */
}
