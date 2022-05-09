package org.finos.legend.engine.language.pure.grammar.from;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.FinCloudDatasourceSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.datasource.DataSourceSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.FinCloudTargetSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;

public class FinCloudDatasourceSpecificationParseTreeWalker {

    public FinCloudTargetSpecification visitFinCloudDatasourceSpecification(DataSourceSpecificationSourceCode code, FinCloudDatasourceSpecificationParserGrammar.FinCloudDatasourceSpecContext fcSpecCtx)
    {
        FinCloudDatasourceSpecification fcSpec = new FinCloudDatasourceSpecification();
        fcSpec.sourceInformation = code.getSourceInformation();
        // apiUrl
        FinCloudDatasourceSpecificationParserGrammar.ApiUrlContext apiUrlContext = PureGrammarParserUtility.validateAndExtractOptionalField(fcSpecCtx.apiUrl(), "apiUrl", fcSpec.sourceInformation);
        fcSpec.apiUrl = PureGrammarParserUtility.fromGrammarString(apiUrlContext.STRING().getText(), true);


        return fcSpec;
    }
}
