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

import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.TrinoLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.TrinoParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoDatasourceSpecification;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class TrinoGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("Trino".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, TrinoLexerGrammar::new, TrinoParserGrammar::new,
                        p -> visitTrinoDsp(code, p.trinoDatasourceSpecification()));
            }
            return null;
        });
    }

    private TrinoDatasourceSpecification visitTrinoDsp(DataSourceSpecificationSourceCode code, TrinoParserGrammar.TrinoDatasourceSpecificationContext dbSpecCtx)
    {
        TrinoDatasourceSpecification dsSpec = new TrinoDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();
        // host
        TrinoParserGrammar.TrinoHostContext hostCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.trinoHost(), "host", dsSpec.sourceInformation);
        dsSpec.host = PureGrammarParserUtility.fromGrammarString(hostCtx.STRING().getText(), true);
        // port
        TrinoParserGrammar.TrinoPortContext portCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.trinoPort(), "port", dsSpec.sourceInformation);
        dsSpec.port = Integer.parseInt(portCtx.INTEGER().getText());
        // Catalog name
        TrinoParserGrammar.TrinoCatalogContext catalogCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.trinoCatalog(), "catalog", dsSpec.sourceInformation);
        dsSpec.catalog = PureGrammarParserUtility.fromGrammarString(catalogCtx.STRING().getText(), true);
        // Schema name
        TrinoParserGrammar.TrinoSchemaContext schemaCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.trinoSchema(), "schema", dsSpec.sourceInformation);
        dsSpec.schema = PureGrammarParserUtility.fromGrammarString(schemaCtx.STRING().getText(), true);
        // clientTags
        TrinoParserGrammar.TrinoClientTagsContext ClientTagCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.trinoClientTags(), "clientTags", dsSpec.sourceInformation);
        dsSpec.clientTags = PureGrammarParserUtility.fromGrammarString(ClientTagCtx.STRING().getText(), true);


        // SSL related parameters
        TrinoParserGrammar.TrinoSSLContext sslCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.trinoSSL(), "ssl", dsSpec.sourceInformation);
        dsSpec.ssl = Boolean.parseBoolean(PureGrammarParserUtility.fromGrammarString(sslCtx.BOOLEAN().getText(), true));
        // trustStorePathVaultReference
        TrinoParserGrammar.TrinoTrustStorePathVaultReferenceContext pathVaultCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.trinoTrustStorePathVaultReference(), "trustStorePathVaultReference", dsSpec.sourceInformation);
        dsSpec.trustStorePathVaultReference = PureGrammarParserUtility.fromGrammarString(pathVaultCtx.STRING().getText(), true);
        // trustStorePasswordVaultReference
        TrinoParserGrammar.TrinoTrustStorePasswordVaultReferenceContext passwordCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.trinoTrustStorePasswordVaultReference(), "trustStorePasswordVaultReference", dsSpec.sourceInformation);
        dsSpec.trustStorePasswordVaultReference = PureGrammarParserUtility.fromGrammarString(passwordCtx.STRING().getText(), true);

        // Kerberose Related Parameters
        TrinoParserGrammar.TrinoKerberosRemoteServiceNameContext remoteSvcNameCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.trinoKerberosRemoteServiceName(), "kerberosRemoteServiceName", dsSpec.sourceInformation);
        dsSpec.kerberosRemoteServiceName = PureGrammarParserUtility.fromGrammarString(remoteSvcNameCtx.STRING().getText(), true);
        TrinoParserGrammar.TrinoKerberosUseCanonicalHostnameContext cnhCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.trinoKerberosUseCanonicalHostname(), "kerberosUseCanonicalHostname", dsSpec.sourceInformation);
        dsSpec.kerberosUseCanonicalHostname = Boolean.parseBoolean(PureGrammarParserUtility.fromGrammarString(cnhCtx.BOOLEAN().getText(), true));

        return dsSpec;
    }
}
