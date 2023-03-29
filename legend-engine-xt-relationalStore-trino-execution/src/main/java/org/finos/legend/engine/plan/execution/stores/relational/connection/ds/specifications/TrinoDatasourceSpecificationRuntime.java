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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import org.apache.commons.codec.binary.Base64;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.TrinoDatasourceSpecificationKey;
import org.finos.legend.engine.shared.core.vault.Vault;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.util.Optional.ofNullable;

public class TrinoDatasourceSpecificationRuntime extends org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification
{

    private static final String CLIENT_TAGS = "clientTags";
    private static final String CATALOG = "catalog";
    private static final String SCHEMA = "schema";
    private static final String SSL = "SSL";
    private static final String SSL_TRUST_STORE_PATH = "SSLTrustStorePath";
    private static final String SSL_TRUST_STORE_PASSWORD = "SSLTrustStorePassword";
    private static final String KERBEROES_REMOTE_SERVICE_NAME = "KerberosRemoteServiceName";
    private static final String KERBEROS_USE_CANONICAL_HOSTNAME = "KerberosUseCanonicalHostname";
    private static final String KERBEROS_DELEGATION = "KerberosDelegation";

    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private final TrinoDatasourceSpecificationKey key;

    public static final List<String> propertiesForDriver = Arrays.asList(CLIENT_TAGS, SSL, SSL_TRUST_STORE_PATH, SSL_TRUST_STORE_PASSWORD, KERBEROES_REMOTE_SERVICE_NAME, KERBEROS_USE_CANONICAL_HOSTNAME, KERBEROS_DELEGATION, USER, PASSWORD);

    public TrinoDatasourceSpecificationRuntime(TrinoDatasourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties);
        this.key = key;
        this.extraDatasourceProperties.putAll(getProperties());
    }

    public TrinoDatasourceSpecificationRuntime(TrinoDatasourceSpecificationKey key, DatabaseManager databaseManager, AuthenticationStrategy authenticationStrategy, Properties extraUserProperties, int maxPoolSize, int minPoolSize)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties, maxPoolSize, minPoolSize);
        this.key = key;
        this.extraDatasourceProperties.putAll(getProperties());
    }

    private Properties getProperties()
    {
        Properties properties = new Properties();

        ofNullable(key.getClientTags()).ifPresent(x -> properties.setProperty(CLIENT_TAGS, x));
        ofNullable(key.getCatalog()).ifPresent(x -> properties.setProperty(CATALOG, x));
        ofNullable(key.getSchema()).ifPresent(x -> properties.setProperty(SCHEMA, x));

        if (key.sslSpecification != null)
        {
            properties.setProperty(SSL, String.valueOf(key.sslSpecification.ssl));
            String trustStorePathVaultReference = key.getSslSpecification().trustStorePathVaultReference;
            String trustStorePasswordVaultReference = key.getSslSpecification().trustStorePasswordVaultReference;

            if (trustStorePathVaultReference != null && trustStorePasswordVaultReference != null)
            {
                String sslTrustStoreValue = Vault.INSTANCE.getValue(trustStorePathVaultReference);
                String sslTrustStorePassword = Vault.INSTANCE.getValue(trustStorePasswordVaultReference);

                if (sslTrustStoreValue == null || sslTrustStorePassword == null)
                {
                    throw new RuntimeException("No valid SSL trustStorePathVaultReference and trustStorePasswordVaultReference values found for references ");
                }
                properties.setProperty(SSL_TRUST_STORE_PATH, createTrinoTempDile(trustStorePathVaultReference, sslTrustStoreValue));
                properties.setProperty(SSL_TRUST_STORE_PASSWORD, sslTrustStorePassword);
            }
        }
        return properties;
    }

    private String createTrinoTempDile(String trustStorePathVaultReference, String sslTrustStoreValue)
    {
        File keystoreTempFile;
        try
        {
            keystoreTempFile = File.createTempFile("trino_keystore_" + trustStorePathVaultReference, "jks");
            keystoreTempFile.deleteOnExit();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to create trino keystore file.",e);
        }

        try (FileOutputStream fos = new FileOutputStream(keystoreTempFile))
        {
            fos.write(Base64.decodeBase64(sslTrustStoreValue.getBytes()));
            return keystoreTempFile.getAbsolutePath();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to write to Trino keystore file.",e);
        }
    }

    @Override
    protected String getJdbcUrl(String host, int port, String databaseName, Properties properties)
    {
        // usually defaults for host, port and databaseName are passed to this method in the original call.
        // This method is supposed to reset to correct values, if required, and construct the jdbc url by relaying to super class, which in turn relays to Driver.
        //TrinoDatasourceSpecificationKey key = (TrinoDatasourceSpecificationKey) this.datasourceKey;
        return super.getJdbcUrl(key.getHost(), key.getPort(), null, properties);
    }
}
