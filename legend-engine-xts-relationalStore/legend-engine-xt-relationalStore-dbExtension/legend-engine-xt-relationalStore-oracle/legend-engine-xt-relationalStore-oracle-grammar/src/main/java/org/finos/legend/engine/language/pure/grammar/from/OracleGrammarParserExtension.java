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

import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.OracleLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.OracleParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.OracleDatasourceSpecification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class OracleGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("Oracle".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, OracleLexerGrammar::new, OracleParserGrammar::new,
                        p -> visitOracleDatasourceSpecification(code, p.oracleDatasourceSpecification()));
            }
            return null;
        });
    }

    private OracleDatasourceSpecification visitOracleDatasourceSpecification(DataSourceSpecificationSourceCode code, OracleParserGrammar.OracleDatasourceSpecificationContext dbSpecCtx)
    {
        OracleDatasourceSpecification dsSpec = new OracleDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();
        // host
        OracleParserGrammar.HostContext hostContext = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.host(), "host", dsSpec.sourceInformation);
        dsSpec.host = PureGrammarParserUtility.fromGrammarString(hostContext.STRING().getText(), true);
        // port
        OracleParserGrammar.PortContext portContext = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.port(), "port", dsSpec.sourceInformation);
        dsSpec.port = Integer.parseInt(portContext.INTEGER().getText());

        OracleParserGrammar.ServiceNameContext databaseNameContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.serviceName(), "serviceName", dsSpec.sourceInformation);
        Optional.ofNullable(databaseNameContext).ifPresent(hostCtx -> dsSpec.serviceName = PureGrammarParserUtility.fromGrammarString(hostCtx.STRING().getText(), true));


        return dsSpec;
    }
}
