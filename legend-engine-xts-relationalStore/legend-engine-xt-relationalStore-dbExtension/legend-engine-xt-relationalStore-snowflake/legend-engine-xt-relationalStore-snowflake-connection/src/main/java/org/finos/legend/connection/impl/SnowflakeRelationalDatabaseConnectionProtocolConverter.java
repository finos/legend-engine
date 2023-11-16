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

package org.finos.legend.connection.impl;

import org.finos.legend.engine.protocol.pure.v1.connection.SnowflakeConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnectionProtocolConverter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationConfigurationWrapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.ConnectionSpecificationWrapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.EncryptedPrivateKeyPairAuthenticationConfiguration;

public class SnowflakeRelationalDatabaseConnectionProtocolConverter
{
    public static class ForSnowflakeWithKeyPairFlow extends RelationalDatabaseConnectionProtocolConverter<SnowflakeDatasourceSpecification, SnowflakePublicAuthenticationStrategy>
    {
        @Override
        protected DatabaseType getDatabaseType()
        {
            return DatabaseType.Snowflake;
        }

        @Override
        protected RelationalDatabaseConnection convert(SnowflakeDatasourceSpecification datasourceSpecification, SnowflakePublicAuthenticationStrategy authenticationStrategy, RelationalDatabaseConnection relationalDatabaseConnection)
        {
            SnowflakeConnectionSpecification connectionSpecification = new SnowflakeConnectionSpecification();
            connectionSpecification.accountName = datasourceSpecification.accountName;
            connectionSpecification.region = datasourceSpecification.region;
            connectionSpecification.warehouseName = datasourceSpecification.warehouseName;
            connectionSpecification.databaseName = datasourceSpecification.databaseName;
            connectionSpecification.cloudType = datasourceSpecification.cloudType;
            connectionSpecification.quotedIdentifiersIgnoreCase = datasourceSpecification.quotedIdentifiersIgnoreCase;
            connectionSpecification.enableQueryTags = datasourceSpecification.enableQueryTags;
            connectionSpecification.proxyHost = datasourceSpecification.proxyHost;
            connectionSpecification.proxyPort = datasourceSpecification.proxyPort;
            connectionSpecification.nonProxyHosts = datasourceSpecification.nonProxyHosts;
            connectionSpecification.organization = datasourceSpecification.organization;
            connectionSpecification.accountType = datasourceSpecification.accountType;
            connectionSpecification.role = datasourceSpecification.role;
            ConnectionSpecificationWrapper connectionSpecificationWrapper = new ConnectionSpecificationWrapper();
            connectionSpecificationWrapper.value = connectionSpecification;
            relationalDatabaseConnection.datasourceSpecification = connectionSpecificationWrapper;

            EncryptedPrivateKeyPairAuthenticationConfiguration authenticationConfiguration = new EncryptedPrivateKeyPairAuthenticationConfiguration();
            authenticationConfiguration.userName = authenticationStrategy.publicUserName;
            authenticationConfiguration.privateKey = new PropertiesFileSecret(authenticationStrategy.privateKeyVaultReference);
            authenticationConfiguration.passphrase = new PropertiesFileSecret(authenticationStrategy.passPhraseVaultReference);
            AuthenticationConfigurationWrapper authenticationConfigurationWrapper = new AuthenticationConfigurationWrapper();
            authenticationConfigurationWrapper.value = authenticationConfiguration;
            relationalDatabaseConnection.authenticationStrategy = authenticationConfigurationWrapper;

            return relationalDatabaseConnection;
        }
    }
}
