package org.finos.legend.engine.language.pure.grammar.from;

import org.finos.legend.engine.language.pure.grammar.from.antlr4.AwsFinCloudConnectionParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudTargetSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AwsOAuthAuthenticationStrategy;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;

public class AwsFinCloudConnectionParseTreeWalker {

    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public AwsFinCloudConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation) {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    /**********
     * FinCloud Connection
     **********/

    public void visitAwsFinCloudConnectionValue(AwsFinCloudConnectionParserGrammar.DefinitionContext ctx, FinCloudConnection awsFinCloudConnection) {
        // store (to change to not applicable?)
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
        awsFinCloudConnection.authenticationStrategy = (AwsOAuthAuthenticationStrategy) visitAuthenticationStrategy(authenticationStrategyContext);

        // targetSpecification
        AwsFinCloudConnectionParserGrammar.TargetSpecificationContext targetSpecificationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetSpecification(), "targetSpecification", awsFinCloudConnection.sourceInformation);
        awsFinCloudConnection.targetSpecification = visitTargetSpecification(targetSpecificationContext);

    }

    private AuthenticationStrategy visitAuthenticationStrategy(AwsFinCloudConnectionParserGrammar.AuthenticationStrategyContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.FINCLOUD_AUTHENTICATION_STRATEGY() != null)
        {
            AwsOAuthAuthenticationStrategy awsOAuthAuthenticationStrategy = new AwsOAuthAuthenticationStrategy();
            awsOAuthAuthenticationStrategy.sourceInformation = sourceInformation;

            return awsOAuthAuthenticationStrategy;
        }
        throw new EngineException("Unrecognized auth", sourceInformation, EngineErrorType.PARSER);
    }


    private FinCloudTargetSpecification visitTargetSpecification(AwsFinCloudConnectionParserGrammar.TargetSpecificationContext ctx)
    {
        if (ctx.datasourceSpecification()!=null)
        {
            return visitDataSource(ctx.datasourceSpecification());
        }
        throw new UnsupportedOperationException();
    }
    private FinCloudDatasourceSpecification visitDataSource(AwsFinCloudConnectionParserGrammar.DatasourceSpecificationContext ctx)
    {
        FinCloudDatasourceSpecification datasource = new FinCloudDatasourceSpecification();
        datasource.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // apiUrl
        AwsFinCloudConnectionParserGrammar.ApiUrlContext apiUrlContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.apiUrl(), "apiUrl", datasource.sourceInformation);
        datasource.apiUrl = PureGrammarParserUtility.fromGrammarString(apiUrlContext.STRING().getText(), true);

        return datasource;
    }
}
