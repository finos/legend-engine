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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.SpannerLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.SpannerParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SpannerDatasourceSpecification;

public class SpannerGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("Spanner".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, SpannerLexerGrammar::new, SpannerParserGrammar::new,
                        p -> visitSpannerDatasourceSpecification(code, p.spannerDatasourceSpecification()));
            }
            return null;
        });
    }

    public SpannerDatasourceSpecification visitSpannerDatasourceSpecification(DataSourceSpecificationSourceCode code, SpannerParserGrammar.SpannerDatasourceSpecificationContext dbSpecCtx)
    {
        SpannerDatasourceSpecification dsSpec = new SpannerDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();
        // project id
        SpannerParserGrammar.ProjectIdContext projectIdCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.projectId(), "projectId", dsSpec.sourceInformation);
        dsSpec.projectId = PureGrammarParserUtility.fromGrammarString(projectIdCtx.STRING().getText(), true);
        // instanceId
        SpannerParserGrammar.InstanceIdContext instanceIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.instanceId(), "instanceId", dsSpec.sourceInformation);
        dsSpec.instanceId = PureGrammarParserUtility.fromGrammarString(instanceIdContext.STRING().getText(), true);
        // databaseId
        SpannerParserGrammar.DatabaseIdContext databaseIdCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.databaseId(), "databaseId", dsSpec.sourceInformation);
        dsSpec.databaseId = PureGrammarParserUtility.fromGrammarString(databaseIdCtx.STRING().getText(), true);
        // proxy host
        SpannerParserGrammar.DbHostContext proxyHostContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.dbHost(), "host", dsSpec.sourceInformation);
        Optional.ofNullable(proxyHostContext).ifPresent(hostCtx -> dsSpec.proxyHost = PureGrammarParserUtility.fromGrammarString(hostCtx.STRING().getText(), true));
        // proxy port
        SpannerParserGrammar.DbPortContext proxyPortContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.dbPort(), "port", dsSpec.sourceInformation);
        Optional.ofNullable(proxyPortContext).ifPresent(portCtx -> dsSpec.proxyPort = Integer.parseInt(portCtx.INTEGER().getText()));
        return dsSpec;
    }
}
