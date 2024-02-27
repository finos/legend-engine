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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.TrinoLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.TrinoParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TrinoDelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoSSLSpecification;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class TrinoGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Trino");
    }

    @Override
    public List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("TrinoDelegatedKerberos".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, TrinoLexerGrammar::new, TrinoParserGrammar::new,
                        p -> visitTrinoAuth(code, p.trinoDelegatedKerberosAuth()));
            }
            return null;
        });
    }

    private TrinoDelegatedKerberosAuthenticationStrategy visitTrinoAuth(AuthenticationStrategySourceCode code, TrinoParserGrammar.TrinoDelegatedKerberosAuthContext authCtx)
    {
        TrinoDelegatedKerberosAuthenticationStrategy authStrategy = new TrinoDelegatedKerberosAuthenticationStrategy();
        authStrategy.sourceInformation = code.getSourceInformation();

        // Kerberos Related Parameters
        TrinoParserGrammar.TrinoServerPrincipalContext serverPrincipalCtx = PureGrammarParserUtility.validateAndExtractOptionalField(authCtx.trinoServerPrincipal(), "serverPrincipal", authStrategy.sourceInformation);
        authStrategy.serverPrincipal = serverPrincipalCtx == null ? null : PureGrammarParserUtility.fromGrammarString(serverPrincipalCtx.STRING().getText(), true);
        TrinoParserGrammar.TrinoKerberosRemoteServiceNameContext remoteSvcNameCtx = PureGrammarParserUtility.validateAndExtractRequiredField(authCtx.trinoKerberosRemoteServiceName(), "kerberosRemoteServiceName", authStrategy.sourceInformation);
        authStrategy.kerberosRemoteServiceName = PureGrammarParserUtility.fromGrammarString(remoteSvcNameCtx.STRING().getText(), true);
        TrinoParserGrammar.TrinoKerberosUseCanonicalHostnameContext kerberosUseCanonicalHostNameCtx = PureGrammarParserUtility.validateAndExtractOptionalField(authCtx.trinoKerberosUseCanonicalHostname(), "kerberosUseCanonicalHostname", authStrategy.sourceInformation);
        authStrategy.kerberosUseCanonicalHostname = kerberosUseCanonicalHostNameCtx == null ? null : Boolean.parseBoolean(kerberosUseCanonicalHostNameCtx.BOOLEAN().getText());

        return authStrategy;
    }
    
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
        TrinoParserGrammar.TrinoCatalogContext catalogCtx = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.trinoCatalog(), "catalog", dsSpec.sourceInformation);
        dsSpec.catalog =  catalogCtx == null ? null : PureGrammarParserUtility.fromGrammarString(catalogCtx.STRING().getText(), true);
        // Schema name
        TrinoParserGrammar.TrinoSchemaContext schemaCtx = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.trinoSchema(), "schema", dsSpec.sourceInformation);
        dsSpec.schema =  schemaCtx == null ? null : PureGrammarParserUtility.fromGrammarString(schemaCtx.STRING().getText(), true);
        // clientTags
        TrinoParserGrammar.TrinoClientTagsContext ClientTagCtx = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.trinoClientTags(), "clientTags", dsSpec.sourceInformation);
        dsSpec.clientTags =  ClientTagCtx == null ? null : PureGrammarParserUtility.fromGrammarString(ClientTagCtx.STRING().getText(), true);


        // SSL related parameters

        TrinoParserGrammar.TrinoSSLSpecificationContext trinoSSLSpecificationContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.trinoSSLSpecification(), "sslSpecification", dsSpec.sourceInformation);
        dsSpec.sslSpecification = trinoSSLSpecificationContext == null ? null : parseTrinoSSLSpecification(trinoSSLSpecificationContext, dsSpec);

        return dsSpec;
    }

    private static TrinoSSLSpecification parseTrinoSSLSpecification(TrinoParserGrammar.TrinoSSLSpecificationContext trinoSSLSpecificationContext, TrinoDatasourceSpecification dsSpec)
    {
        TrinoSSLSpecification sslSpecification = new TrinoSSLSpecification();
        TrinoParserGrammar.TrinoSSLContext sslCtx = PureGrammarParserUtility.validateAndExtractRequiredField(trinoSSLSpecificationContext.trinoSSL(), "ssl", dsSpec.sourceInformation);
        sslSpecification.ssl = Boolean.parseBoolean(sslCtx.BOOLEAN().getText());

        TrinoParserGrammar.TrinoTrustStorePathVaultReferenceContext trustPathCtx = PureGrammarParserUtility.validateAndExtractOptionalField(trinoSSLSpecificationContext.trinoTrustStorePathVaultReference(), "trustStorePathVaultReference", dsSpec.sourceInformation);
        sslSpecification.trustStorePathVaultReference = trustPathCtx == null ? null : PureGrammarParserUtility.fromGrammarString(trustPathCtx.STRING().getText(), true);

        TrinoParserGrammar.TrinoTrustStorePasswordVaultReferenceContext trustPasswordCtx = PureGrammarParserUtility.validateAndExtractOptionalField(trinoSSLSpecificationContext.trinoTrustStorePasswordVaultReference(), "trustStorePasswordVaultReference", dsSpec.sourceInformation);
        sslSpecification.trustStorePasswordVaultReference = trustPasswordCtx == null ? null : PureGrammarParserUtility.fromGrammarString(trustPasswordCtx.STRING().getText(), true);

        return sslSpecification;
    }
}
