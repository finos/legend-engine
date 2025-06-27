//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import org.apache.commons.lang.SystemUtils;
import org.finos.legend.engine.authentication.vaults.InMemoryVaultForTesting;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TrinoDelegatedKerberosAuthenticationStrategyRuntime;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.trino.TrinoManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.TrinoDatasourceSpecificationKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoSSLSpecification;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TrinoDatasourceSpecificationRuntimeTest extends TrinoDatasourceSpecificationRuntime
{
    private final InMemoryVaultForTesting inMemoryVault = new InMemoryVaultForTesting();

    public TrinoDatasourceSpecificationRuntimeTest()
    {
        super(new TrinoDatasourceSpecificationKey("dummy", 100, "dummy", "dummy", "dummy", null),
                new TrinoManager(),
                new TrinoDelegatedKerberosAuthenticationStrategyRuntime("dummy", "dummy", true),
                new Properties());
    }

    @Before
    public void setup()
    {
        inMemoryVault.setValue("testPathRef", "testTrustStoreVal");
        inMemoryVault.setValue("testPwdRef", "changeme");

        Vault.INSTANCE.registerImplementation(inMemoryVault);
    }

    @Test
    public void testTrinoDatasourceSpecificationProperties_WithValidCustomTrustStore()
    {
        Assume.assumeFalse("Trust store path is unix specific", SystemUtils.IS_OS_WINDOWS);

        TrinoSSLSpecification trinoSSLSpecification = buildSSLSpecWith(true, "testPathRef", "testPwdRef");
        TrinoDatasourceSpecificationRuntime ds = buildDatasourceSpecificationRuntime(trinoSSLSpecification);

        Properties properties = ds.getExtraDatasourceProperties();
        assertEquals("catalog", properties.getProperty(CATALOG));
        assertEquals("schema", properties.getProperty(SCHEMA));
        assertEquals("cg:test", properties.getProperty(CLIENT_TAGS));
        assertEquals("true", properties.getProperty(SSL));
        assertEquals("test_user", properties.getProperty(USER));
        assertEquals("changeme", properties.getProperty(SSL_TRUST_STORE_PASSWORD));
        assertTrue(properties.getProperty(SSL_TRUST_STORE_PATH).matches(".*trino_keystore_testPathRef.*jks"));
    }

    @Test
    public void testTrinoDatasourceSpecificationProperties_WithEmptyTrustStoreAndPwd_AddNothing()
    {
        TrinoSSLSpecification trinoSSLSpecification = buildSSLSpecWith(true, null, null);
        TrinoDatasourceSpecificationRuntime ds = buildDatasourceSpecificationRuntime(trinoSSLSpecification);

        Properties properties = ds.getExtraDatasourceProperties();
        assertEquals("catalog", properties.getProperty(CATALOG));
        assertEquals("schema", properties.getProperty(SCHEMA));
        assertEquals("cg:test", properties.getProperty(CLIENT_TAGS));
        assertEquals("true", properties.getProperty(SSL));
        assertEquals("test_user", properties.getProperty(USER));
        assertNull(properties.getProperty(SSL_TRUST_STORE_PATH));
        assertNull(properties.getProperty(SSL_TRUST_STORE_PASSWORD));
    }

    @Test
    public void testTrinoDatasourceSpecificationProperties_WithInvalidTrustStorePwdRef_ThrowException()
    {

        TrinoSSLSpecification trinoSSLSpecification = buildSSLSpecWith(true, "testPathRef", "invalidTestPwdRef");
        try
        {
            buildDatasourceSpecificationRuntime(trinoSSLSpecification);
        }
        catch (RuntimeException re)
        {
            assertEquals("No valid SSL trust store password value found for vault reference", re.getMessage());
        }
    }

    @Test
    public void testTrinoDatasourceSpecificationProperties_WithInvalidTrustStoreRef_ThrowException()
    {
        TrinoSSLSpecification trinoSSLSpecification = buildSSLSpecWith(true, "InvalidTestPathRef", "testPwdRef");
        try
        {
            buildDatasourceSpecificationRuntime(trinoSSLSpecification);
        }
        catch (RuntimeException re)
        {
            assertEquals("No valid SSL trust store value found for vault reference", re.getMessage());
        }
    }

    @Test
    public void test_getJdbcUrl_givenCatalog_noSchema()
    {
        TrinoSSLSpecification trinoSSLSpecification = buildSSLSpecWith(true, null, null);
        TrinoDatasourceSpecificationRuntime ds = new TrinoDatasourceSpecificationRuntime(
                new TrinoDatasourceSpecificationKey("host", 8000, "catalog", null, "cg:test", trinoSSLSpecification),
                new TrinoManager(),
                new TrinoDelegatedKerberosAuthenticationStrategyRuntime("test", "HTTP", false),
                new Properties()
        );

        Properties properties = ds.getExtraDatasourceProperties();
        String jdbcUrl = ds.getJdbcUrl("unused_host", 9090, null, properties);
        assertEquals("jdbc:trino://host:8000/catalog", jdbcUrl);
    }

    @Test
    public void test_getJdbcUrl_givenCatalogAndSchema()
    {
        TrinoSSLSpecification trinoSSLSpecification = buildSSLSpecWith(true, null, null);
        TrinoDatasourceSpecificationRuntime ds = buildDatasourceSpecificationRuntime(trinoSSLSpecification);

        Properties properties = ds.getExtraDatasourceProperties();
        String jdbcUrl = ds.getJdbcUrl("unused_host", 9090, null, properties);
        assertEquals("jdbc:trino://host:8000/catalog/schema", jdbcUrl);
    }

    private static TrinoSSLSpecification buildSSLSpecWith(boolean ssl, String trustStorePathVaultRef, String trustStorePwdVaultRef)
    {
        TrinoSSLSpecification sslSpec = new TrinoSSLSpecification();
        sslSpec.ssl = ssl;
        sslSpec.trustStorePathVaultReference = trustStorePathVaultRef;
        sslSpec.trustStorePasswordVaultReference = trustStorePwdVaultRef;
        return sslSpec;
    }

    private static TrinoDatasourceSpecificationRuntime buildDatasourceSpecificationRuntime(TrinoSSLSpecification trinoSSLSpecification)
    {
        Properties properties = new Properties();
        properties.setProperty("user", "test_user");
        return new TrinoDatasourceSpecificationRuntime(
                new TrinoDatasourceSpecificationKey("host", 8000, "catalog", "schema", "cg:test", trinoSSLSpecification),
                new TrinoManager(),
                new TrinoDelegatedKerberosAuthenticationStrategyRuntime("test", "HTTP", false),
                properties
        );
    }
}
