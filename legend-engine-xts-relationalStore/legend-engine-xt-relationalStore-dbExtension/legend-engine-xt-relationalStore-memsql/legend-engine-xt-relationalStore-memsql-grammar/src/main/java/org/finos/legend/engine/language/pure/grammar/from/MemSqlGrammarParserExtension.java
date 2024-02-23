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

import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MemSqlLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MemSqlParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.MemSqlDatasourceSpecification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class MemSqlGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("MemSql".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, MemSqlLexerGrammar::new, MemSqlParserGrammar::new,
                        p -> visitMemSqlDatasourceSpecification(code, p.memSqlDatasourceSpecification()));
            }
            return null;
        });
    }

    private MemSqlDatasourceSpecification visitMemSqlDatasourceSpecification(DataSourceSpecificationSourceCode code, MemSqlParserGrammar.MemSqlDatasourceSpecificationContext dbSpecCtx)
    {
        MemSqlDatasourceSpecification dsSpec = new MemSqlDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();
        // project id
        MemSqlParserGrammar.HostContext hostContext = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.host(), "host", dsSpec.sourceInformation);
        dsSpec.host = PureGrammarParserUtility.fromGrammarString(hostContext.STRING().getText(), true);
        // default dataset
        MemSqlParserGrammar.PortContext portContext = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.port(), "port", dsSpec.sourceInformation);
        dsSpec.port = Integer.valueOf(PureGrammarParserUtility.fromGrammarString(portContext.STRING().getText(), true));
        // proxy host
        MemSqlParserGrammar.DatabaseNameContext databaseNameContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.databaseName(), "databaseName", dsSpec.sourceInformation);
        Optional.ofNullable(databaseNameContext).ifPresent(hostCtx -> dsSpec.databaseName = PureGrammarParserUtility.fromGrammarString(hostCtx.STRING().getText(), true));
        // proxy port
        MemSqlParserGrammar.UseSslContext useSslContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.useSsl(), "useSsl", dsSpec.sourceInformation);
        Optional.ofNullable(useSslContext).ifPresent(portCtx -> dsSpec.useSsl = Boolean.valueOf(PureGrammarParserUtility.fromGrammarString(useSslContext.STRING().getText(), true)));
        return dsSpec;
    }
}
