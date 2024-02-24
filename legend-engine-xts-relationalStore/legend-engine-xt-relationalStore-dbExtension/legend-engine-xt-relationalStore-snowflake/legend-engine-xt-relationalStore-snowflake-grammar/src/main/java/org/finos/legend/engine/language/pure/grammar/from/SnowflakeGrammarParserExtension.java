// Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.SnowflakeLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.SnowflakeParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SnowflakeGrammarParserExtension implements IRelationalGrammarParserExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Snowflake");
    }

    private String normalizeName(String elementName, String localPrefix)
    {
        String normalized = elementName.replaceAll("::", "-");
        return localPrefix + "-" + normalized;
    }

    @Override
    public List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("SnowflakePublic".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, SnowflakeLexerGrammar::new, SnowflakeParserGrammar::new,
                        p -> visitSnowflakeAuth(code, p.snowflakePublicAuth()));
            }
            return null;
        });
    }

    @Override
    public List<Function<RelationalDatabaseConnection, AuthenticationStrategy>> getExtraLocalModeAuthenticationStrategy()
    {
        return Collections.singletonList(dbConn ->
        {
            DatabaseType databaseType = dbConn.type != null ? dbConn.type : dbConn.databaseType;
            if (databaseType == DatabaseType.Snowflake)
            {
                String elementName = dbConn.element;
                SnowflakePublicAuthenticationStrategy authenticationStrategy = new SnowflakePublicAuthenticationStrategy();
                authenticationStrategy.privateKeyVaultReference = this.normalizeName(elementName, "legend-local-snowflake-privateKeyVaultReference");
                authenticationStrategy.passPhraseVaultReference = this.normalizeName(elementName, "legend-local-snowflake-passphraseVaultReference");
                authenticationStrategy.publicUserName = this.normalizeName(elementName, "legend-local-snowflake-publicuserName");
                return authenticationStrategy;
            }
            return null;
        });
    }

    public SnowflakePublicAuthenticationStrategy visitSnowflakeAuth(AuthenticationStrategySourceCode code, SnowflakeParserGrammar.SnowflakePublicAuthContext snowflakePublicAuth)
    {
        SnowflakePublicAuthenticationStrategy authStrategy = new SnowflakePublicAuthenticationStrategy();
        authStrategy.sourceInformation = code.getSourceInformation();
        SnowflakeParserGrammar.SnowflakePublicAuthUserNameContext publicUserName = PureGrammarParserUtility.validateAndExtractRequiredField(snowflakePublicAuth.snowflakePublicAuthUserName(), "publicUserName", code.getSourceInformation());
        authStrategy.publicUserName = PureGrammarParserUtility.fromGrammarString(publicUserName.STRING().getText(), true);
        SnowflakeParserGrammar.SnowflakePublicAuthKeyVaultRefContext snowflakePublicAuthKeyVaultRef = PureGrammarParserUtility.validateAndExtractRequiredField(snowflakePublicAuth.snowflakePublicAuthKeyVaultRef(), "privateKeyVaultReference", code.getSourceInformation());
        authStrategy.privateKeyVaultReference = PureGrammarParserUtility.fromGrammarString(snowflakePublicAuthKeyVaultRef.STRING().getText(), true);
        SnowflakeParserGrammar.SnowflakePublicAuthPassPhraseVaultRefContext snowflakePublicAuthPassPhraseVaultRef = PureGrammarParserUtility.validateAndExtractRequiredField(snowflakePublicAuth.snowflakePublicAuthPassPhraseVaultRef(), "passPhraseVaultReference", code.getSourceInformation());
        authStrategy.passPhraseVaultReference = PureGrammarParserUtility.fromGrammarString(snowflakePublicAuthPassPhraseVaultRef.STRING().getText(), true);
        return authStrategy;
    }

    @Override
    public List<Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            if ("Snowflake".equals(code.getType()))
            {
                return IRelationalGrammarParserExtension.parse(code, SnowflakeLexerGrammar::new, SnowflakeParserGrammar::new,
                        p -> visitSnowflakeDsp(code, p.snowflakeDatasourceSpecification()));
            }
            return null;
        });
    }

    @Override
    public List<Function<RelationalDatabaseConnection, DatasourceSpecification>> getExtraLocalModeDataSourceSpecification()
    {
        return Collections.singletonList(dbConn ->
        {
            DatabaseType databaseType = dbConn.type != null ? dbConn.type : dbConn.databaseType;
            if (databaseType == DatabaseType.Snowflake)
            {
                String elementName = dbConn.element;
                SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
                snowflakeDatasourceSpecification.accountName = this.normalizeName(elementName, "legend-local-snowflake-accountName");
                snowflakeDatasourceSpecification.databaseName = this.normalizeName(elementName, "legend-local-snowflake-databaseName");
                snowflakeDatasourceSpecification.role = this.normalizeName(elementName, "legend-local-snowflake-role");
                snowflakeDatasourceSpecification.warehouseName = this.normalizeName(elementName, "legend-local-snowflake-warehouseName");
                snowflakeDatasourceSpecification.region = this.normalizeName(elementName, "legend-local-snowflake-region");
                snowflakeDatasourceSpecification.cloudType = this.normalizeName(elementName, "legend-local-snowflake-cloudType");
                return snowflakeDatasourceSpecification;
            }
            return null;
        });
    }

    public SnowflakeDatasourceSpecification visitSnowflakeDsp(DataSourceSpecificationSourceCode code, SnowflakeParserGrammar.SnowflakeDatasourceSpecificationContext dbSpecCtx)
    {
        SnowflakeDatasourceSpecification dsSpec = new SnowflakeDatasourceSpecification();
        dsSpec.sourceInformation = code.getSourceInformation();
        // databaseName
        SnowflakeParserGrammar.DbNameContext databaseNameCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.dbName(), "name", dsSpec.sourceInformation);
        dsSpec.databaseName = PureGrammarParserUtility.fromGrammarString(databaseNameCtx.STRING().getText(), true);
        // account
        SnowflakeParserGrammar.DbAccountContext accountCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.dbAccount(), "account", dsSpec.sourceInformation);
        dsSpec.accountName = PureGrammarParserUtility.fromGrammarString(accountCtx.STRING().getText(), true);
        // warehouse
        SnowflakeParserGrammar.DbWarehouseContext warehouseCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.dbWarehouse(), "warehouse", dsSpec.sourceInformation);
        dsSpec.warehouseName = PureGrammarParserUtility.fromGrammarString(warehouseCtx.STRING().getText(), true);
        // region
        SnowflakeParserGrammar.SnowflakeRegionContext regionCtx = PureGrammarParserUtility.validateAndExtractRequiredField(dbSpecCtx.snowflakeRegion(), "region", dsSpec.sourceInformation);
        dsSpec.region = PureGrammarParserUtility.fromGrammarString(regionCtx.STRING().getText(), true);
        // cloudType
        SnowflakeParserGrammar.CloudTypeContext cloudTypeCtx = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.cloudType(), "cloudType", dsSpec.sourceInformation);
        if (cloudTypeCtx != null)
        {
            dsSpec.cloudType = PureGrammarParserUtility.fromGrammarString(cloudTypeCtx.STRING().getText(), true);
        }
        //quotedIdentifiersIgnoreCase
        SnowflakeParserGrammar.SnowflakeQuotedIdentifiersIgnoreCaseContext snowflakeQuotedIdentifiersIgnoreCaseCtx = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.snowflakeQuotedIdentifiersIgnoreCase(), "quotedIdentifiersIgnoreCase", dsSpec.sourceInformation);
        if (snowflakeQuotedIdentifiersIgnoreCaseCtx != null)
        {
            dsSpec.quotedIdentifiersIgnoreCase = Boolean.parseBoolean(snowflakeQuotedIdentifiersIgnoreCaseCtx.BOOLEAN().getText());
        }
        // enableQueryTags
        SnowflakeParserGrammar.EnableQueryTagsContext snowflakeQueryTagsCtx = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.enableQueryTags(), "enableQueryTags", dsSpec.sourceInformation);
        if (snowflakeQueryTagsCtx != null)
        {
            dsSpec.enableQueryTags = Boolean.parseBoolean(snowflakeQueryTagsCtx.BOOLEAN().getText());
        }
        // proxyHost
        SnowflakeParserGrammar.DbProxyHostContext proxyHostContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.dbProxyHost(), "proxyHost", dsSpec.sourceInformation);
        Optional.ofNullable(proxyHostContext).ifPresent(hostCtx -> dsSpec.proxyHost = PureGrammarParserUtility.fromGrammarString(hostCtx.STRING().getText(), true));
        // proxyPort
        SnowflakeParserGrammar.DbProxyPortContext proxyPortContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.dbProxyPort(), "proxyPort", dsSpec.sourceInformation);
        Optional.ofNullable(proxyPortContext).ifPresent(portCtx -> dsSpec.proxyPort = PureGrammarParserUtility.fromGrammarString(portCtx.STRING().getText(), true));
        // nonProxyHosts
        SnowflakeParserGrammar.DbNonProxyHostsContext nonProxyHostsContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.dbNonProxyHosts(), "nonProxyHosts", dsSpec.sourceInformation);
        Optional.ofNullable(nonProxyHostsContext).ifPresent(nonProxyHostsCtx -> dsSpec.nonProxyHosts = PureGrammarParserUtility.fromGrammarString(nonProxyHostsCtx.STRING().getText(), true));
        // accountType
        SnowflakeParserGrammar.DbAccountTypeContext accountTypeContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.dbAccountType(), "accountType", dsSpec.sourceInformation);
        Optional.ofNullable(accountTypeContext).ifPresent(accountTypeCtx -> dsSpec.accountType = PureGrammarParserUtility.fromIdentifier(accountTypeCtx.identifier()));
        // organization
        SnowflakeParserGrammar.DbOrganizationContext organizationContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.dbOrganization(), "organization", dsSpec.sourceInformation);
        Optional.ofNullable(organizationContext).ifPresent(organizationCtx -> dsSpec.organization = PureGrammarParserUtility.fromGrammarString(organizationCtx.STRING().getText(), true));

        // role
        SnowflakeParserGrammar.DbRoleContext roleContext = PureGrammarParserUtility.validateAndExtractOptionalField(dbSpecCtx.dbRole(), "role", dsSpec.sourceInformation);
        Optional.ofNullable(roleContext).ifPresent(roleCtx -> dsSpec.role = PureGrammarParserUtility.fromGrammarString(roleCtx.STRING().getText(), true));

        return dsSpec;
    }
}
