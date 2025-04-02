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
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.TrinoDatasourceSpecificationKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoSSLSpecification;
import org.finos.legend.engine.shared.core.vault.Vault;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class TrinoDatasourceSpecificationRuntime extends DataSourceSpecification
{

    public static final String CLIENT_TAGS = "clientTags";
    public static final String CATALOG = "catalog";
    public static final String SCHEMA = "schema";
    public static final String SSL = "SSL";
    public static final String SSL_TRUST_STORE_PATH = "SSLTrustStorePath";
    public static final String SSL_TRUST_STORE_PASSWORD = "SSLTrustStorePassword";
    public static final String KERBEROES_REMOTE_SERVICE_NAME = "KerberosRemoteServiceName";
    public static final String KERBEROS_USE_CANONICAL_HOSTNAME = "KerberosUseCanonicalHostname";
    public static final String KERBEROS_DELEGATION = "KerberosDelegation";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String DEFAULT_TRUST_STORE_PATH = "/gns/mw/security/ssl/ssl-certs-prod/cacerts.jks";
    private final TrinoDatasourceSpecificationKey key;

    public static final List<String> propertiesForDriver = Arrays.asList(CLIENT_TAGS, SSL, SSL_TRUST_STORE_PATH, SSL_TRUST_STORE_PASSWORD, KERBEROES_REMOTE_SERVICE_NAME, KERBEROS_USE_CANONICAL_HOSTNAME, KERBEROS_DELEGATION, USER, PASSWORD);

    public TrinoDatasourceSpecificationRuntime(TrinoDatasourceSpecificationKey key, DatabaseManager databaseManager,
                                               AuthenticationStrategy authenticationStrategy, Properties extraUserProperties)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties);
        this.key = key;

        initializeExtraDatasourceProperties();
    }

    public TrinoDatasourceSpecificationRuntime(TrinoDatasourceSpecificationKey key, DatabaseManager databaseManager,
                                               AuthenticationStrategy authenticationStrategy, Properties extraUserProperties,
                                               int maxPoolSize, int minPoolSize)
    {
        super(key, databaseManager, authenticationStrategy, extraUserProperties, maxPoolSize, minPoolSize);
        this.key = key;

        initializeExtraDatasourceProperties();
    }

    private void initializeExtraDatasourceProperties()
    {
        setExtraDSPropertyIfValueNotEmpty(CATALOG, key.getCatalog());
        setExtraDSPropertyIfValueNotEmpty(SCHEMA, key.getSchema());
        setExtraDSPropertyIfValueNotEmpty(CLIENT_TAGS, key.getClientTags());
        setSSLProperties();
    }

    private void setExtraDSPropertyIfValueNotEmpty(String driverProperty, String propertyValue)
    {
        Optional.ofNullable(propertyValue).ifPresent(value -> extraDatasourceProperties.setProperty(driverProperty, propertyValue));
    }

    private void setSSLProperties()
    {
        TrinoSSLSpecification sslSpec = key.getSslSpecification();
        if (sslSpec != null)
        {
            extraDatasourceProperties.setProperty(SSL, String.valueOf(sslSpec.ssl));
            setSSLTrustStorePath(sslSpec.trustStorePathVaultReference);
            setSSLTrustStorePassword(sslSpec.trustStorePasswordVaultReference);
        }
    }

    private void setSSLTrustStorePath(String vaultRef)
    {
        if (vaultRef == null)
        {
            setDefaultSSLTrustStorePath();
        }
        else
        {
            setCustomSSLTrustStorePath(vaultRef);
        }
    }

    private void setDefaultSSLTrustStorePath()
    {
        extraDatasourceProperties.setProperty(SSL_TRUST_STORE_PATH, DEFAULT_TRUST_STORE_PATH);
    }

    private void setCustomSSLTrustStorePath(String vaultRef)
    {
        String sslTrustStoreValue = Vault.INSTANCE.getValue(vaultRef);
        if (sslTrustStoreValue == null)
        {
            throw new RuntimeException("No valid SSL trust store value found for vault reference");
        }
        extraDatasourceProperties.setProperty(SSL_TRUST_STORE_PATH, createAndGetTrustStoreFile(vaultRef, sslTrustStoreValue));
    }

    private String createAndGetTrustStoreFile(String valueRef, String sslTrustStoreValue)
    {
        File tempTrustStoreFile = createEmptyFile(valueRef);
        populateTrustStoreFile(tempTrustStoreFile, sslTrustStoreValue);
        return tempTrustStoreFile.getAbsolutePath();
    }

    private static File createEmptyFile(String trustStorePathVaultReference)
    {
        File keystoreTempFile;
        try
        {
            keystoreTempFile = File.createTempFile("trino_keystore_" + trustStorePathVaultReference, "jks");
            keystoreTempFile.deleteOnExit();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to create trino trust store file!", e);
        }
        return keystoreTempFile;
    }

    private static void populateTrustStoreFile(File tempKeystoreFile, String sslTrustStoreValue)
    {
        try (FileOutputStream fos = new FileOutputStream(tempKeystoreFile))
        {
            fos.write(Base64.decodeBase64(sslTrustStoreValue.getBytes()));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to write to trino trust store file created!", e);
        }
    }

    private void setSSLTrustStorePassword(String vaultRef)
    {
        if (vaultRef == null)
        {
            // vault ref can be null for default trust store path;
            return;
        }

        String sslTrustStorePwdValue = Vault.INSTANCE.getValue(vaultRef);
        if (sslTrustStorePwdValue == null)
        {
            throw new RuntimeException("No valid SSL trust store password value found for vault reference");
        }
        extraDatasourceProperties.setProperty(SSL_TRUST_STORE_PASSWORD, sslTrustStorePwdValue);
    }

    /**
     * Usually defaults for host, port and databaseName are passed to this method in the original call.
     * This method is supposed to reset to correct values, if required, and construct the jdbc url by relaying to super class, which in turn relays to Driver.
     */
    @Override
    protected String getJdbcUrl(String host, int port, String databaseName, Properties properties)
    {
        return super.getJdbcUrl(key.getHost(), key.getPort(), null, properties);
    }
}
