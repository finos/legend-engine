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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DatabricksLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DatabricksParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatabricksDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class DatabricksGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Databricks");
    }

    @Override
    public List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers()
    {
        return Collections.singletonList(code -> null);
    }

    @Override
    public List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("Databricks".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, DatabricksLexerGrammar::new, DatabricksParserGrammar::new,
                        p -> visitDatabricksDatasourceSpecification(code, p.databricksDatasourceSpecification()));
            }
            return null;
        });
    }

    public DatabricksDatasourceSpecification visitDatabricksDatasourceSpecification(DataSourceSpecificationSourceCode code, DatabricksParserGrammar.DatabricksDatasourceSpecificationContext dbSpecCtx)
    {
        DatabricksDatasourceSpecification dsSpec = new DatabricksDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();

        DatabricksParserGrammar.HostnameContext hostnameCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.hostname(), "hostname", dsSpec.sourceInformation);
        dsSpec.hostname = PureGrammarParserUtility.fromGrammarString(hostnameCtx.STRING().getText(), true);

        DatabricksParserGrammar.PortContext portCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.port(), "port", dsSpec.sourceInformation);
        dsSpec.port = PureGrammarParserUtility.fromGrammarString(portCtx.STRING().getText(), true);

        DatabricksParserGrammar.ProtocolContext protocolCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.protocol(), "protocol", dsSpec.sourceInformation);
        dsSpec.protocol = PureGrammarParserUtility.fromGrammarString(protocolCtx.STRING().getText(), true);

        DatabricksParserGrammar.HttpPathContext httpCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.httpPath(), "httpPath", dsSpec.sourceInformation);
        dsSpec.httpPath = PureGrammarParserUtility.fromGrammarString(httpCtx.STRING().getText(), true);

        return dsSpec;
    }
}
