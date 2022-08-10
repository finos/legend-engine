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
