package org.finos.legend.engine.language.pure.grammar.from;

import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.datasource.DataSourceSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class BigQueryGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("BigQuery".equals(code.getType()))
            {
                return parse(code, BigQueryLexerGrammar::new, BigQueryParserGrammar::new,
                        p -> visitBigQueryDatasourceSpecification(code, p.bigQueryDatasourceSpecification()));
            }
            return null;
        });
    }

    private BigQueryDatasourceSpecification visitBigQueryDatasourceSpecification(DataSourceSpecificationSourceCode code, BigQueryParserGrammar.BigQueryDatasourceSpecificationContext dbSpecCtx)
    {
        BigQueryDatasourceSpecification dsSpec = new BigQueryDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();
        // project id
        BigQueryParserGrammar.ProjectIdContext projectIdCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.projectId(), "projectId", dsSpec.sourceInformation);
        dsSpec.projectId = PureGrammarParserUtility.fromGrammarString(projectIdCtx.STRING().getText(), true);
        // default dataset
        BigQueryParserGrammar.DefaultDatasetContext defaultDatasetCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.defaultDataset(), "defaultDataset", dsSpec.sourceInformation);
        dsSpec.defaultDataset = PureGrammarParserUtility.fromGrammarString(defaultDatasetCtx.STRING().getText(), true);
        // proxy host
        BigQueryParserGrammar.DbProxyHostContext proxyHostContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.dbProxyHost(), "proxyHost", dsSpec.sourceInformation);
        Optional.ofNullable(proxyHostContext).ifPresent(hostCtx -> dsSpec.proxyHost = PureGrammarParserUtility.fromGrammarString(hostCtx.STRING().getText(), true));
        // proxy port
        BigQueryParserGrammar.DbProxyPortContext proxyPortContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.dbProxyPort(), "proxyPort", dsSpec.sourceInformation);
        Optional.ofNullable(proxyPortContext).ifPresent(portCtx -> dsSpec.proxyPort = PureGrammarParserUtility.fromGrammarString(portCtx.STRING().getText(), true));
        return dsSpec;
    }
}
